package com.etendoerp.etendorx.actionhandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ConfigServiceParam;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.utils.RXConfigUtils;

/**
 * Action handler for initializing RX services.
 */
public class InitializeRXServices extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();
  private static final String MESSAGE = "message";
  private static final String MESSAGE_SEVERITY = "severity";
  private static final String MESSAGE_TEXT = "text";
  private static final String MESSAGE_SUCCESS = "success";
  private static final String MESSAGE_ERROR = "error";
  private static final String ERROR_TITLE = "ERROR";
  private static final String PROPERTY_RX = "docker_com.etendoerp.etendorx";
  private static final String PROPERTY_TOMCAT = "docker_com.etendoerp.tomcat";
  private static final String PROPERTY_ASYNC = "docker_com.etendoerp.etendorx_async";
  private static final String PROPERTY_CONNECTOR = "docker_com.etendoerp.etendorx_connector";
  private static final String PROPERTY_TOMCAT_PORT = "tomcat.port";
  private static final String LOCALHOST_URL = "http://localhost:%d";
  private static final String DAS_URL = "http://das:8092";
  private static final String DOCKER_TOMCAT_URL = "http://tomcat:8080";

  /**
   * Groups the four docker-enable flags to avoid long boolean parameter lists.
   */
  static class ServiceFlags {
    final boolean rxEnable;
    final boolean tomcatEnable;
    final boolean asyncEnable;
    final boolean connectorEnable;

    ServiceFlags(boolean rxEnable, boolean tomcatEnable, boolean asyncEnable, boolean connectorEnable) {
      this.rxEnable = rxEnable;
      this.tomcatEnable = tomcatEnable;
      this.asyncEnable = asyncEnable;
      this.connectorEnable = connectorEnable;
    }
  }

  /**
   * Returns the Tomcat port from properties, or the default if not set.
   */
  private static String getTomcatPort() {
    String port = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(PROPERTY_TOMCAT_PORT);
    return !StringUtils.isBlank(port) ? port : "8080";
  }

  /**
   * Builds the Host Docker Internal Tomcat URL using the current Tomcat port.
   */
  private static String getHostDockerInternalTomcatUrl() {
    return "http://host.docker.internal:" + getTomcatPort();
  }

  private static Set<String> parseExcludedServices(String excludedServicesCsv) {
    if (excludedServicesCsv == null || excludedServicesCsv.isBlank()) {
      return Collections.emptySet();
    }
    return Arrays.stream(excludedServicesCsv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static boolean parseBoolProperty(Properties props, String key) {
    return Boolean.parseBoolean(props.getProperty(key));
  }

  /**
   * Executes the action to initialize RX services.
   *
   * @param parameters
   *     the parameters for the action
   * @param content
   *     the content for the action
   * @return the result of the action as a JSONObject
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject actionResult = new JSONObject();
    try {
      Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();

      ServiceFlags flags = new ServiceFlags(
          parseBoolProperty(props, PROPERTY_RX),
          parseBoolProperty(props, PROPERTY_TOMCAT),
          parseBoolProperty(props, PROPERTY_ASYNC),
          parseBoolProperty(props, PROPERTY_CONNECTOR));

      String rxProp = props.getProperty(PROPERTY_RX);
      String asyncProp = props.getProperty(PROPERTY_ASYNC);
      String connectorProp = props.getProperty(PROPERTY_CONNECTOR);

      if (rxProp == null && asyncProp == null && connectorProp == null) {
        actionResult.put(MESSAGE_SEVERITY, "warning");
        actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD("ETRX_NoServicePropertiesFound"));
        return actionResult;
      }

      Set<String> excludedServices = parseExcludedServices(props.getProperty("docker.exclude"));

      List<String> names = OBDal.getInstance()
          .getSession()
          .createQuery("select e.serviceName from ETRX_Config e", String.class)
          .list();
      Set<String> existingServiceNames = new HashSet<>(names);

      initializeServices(rxProp, asyncProp, connectorProp, existingServiceNames, excludedServices, flags);
      OBDal.getInstance().flush();

      actionResult.put(MESSAGE_SEVERITY, MESSAGE_SUCCESS);
      actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD(MESSAGE_SUCCESS) + "<br/>" +
          OBMessageUtils.messageBD("ETRX_SERVICES_INITIALIZED"));
      actionResult.put("refreshGrid", true);
    } catch (Exception e) {
      return handleException(e, actionResult);
    }
    return actionResult;
  }

  /**
   * Initializes the RX services based on the provided parameters.
   *
   * @param rxProp
   *     the RX docker property value (null means not configured)
   * @param asyncProp
   *     the async docker property value (null means not configured)
   * @param connectorProp
   *     the connector docker property value (null means not configured)
   * @param existingServiceNames
   *     the set of existing service names
   * @param excludedServices
   *     set of service names to skip
   * @param flags
   *     grouped enable/disable flags for the services
   */
  private void initializeServices(String rxProp, String asyncProp, String connectorProp,
      Set<String> existingServiceNames, Set<String> excludedServices, ServiceFlags flags) {

    Object[][] serviceTypes = {
        { rxProp, RXConfigUtils.ServiceConfigType.ETENDORX },
        { asyncProp, RXConfigUtils.ServiceConfigType.ASYNC },
        { connectorProp, RXConfigUtils.ServiceConfigType.CONNECTOR },
    };
    for (Object[] entry : serviceTypes) {
      if (entry[0] != null) {
        Map<String, Integer> services = RXConfigUtils.getServicesByType(
            (RXConfigUtils.ServiceConfigType) entry[1]);
        manageServices(services, existingServiceNames, excludedServices, flags);
      }
    }
  }

  /**
   * Manages the services by saving their configurations if they do not already exist.
   *
   * @param services
   *     the map of service names to their ports
   * @param existingServiceNames
   *     the set of existing service names
   * @param excludedServices
   *     set of service names to skip
   * @param flags
   *     grouped enable/disable flags for the services
   */
  private void manageServices(Map<String, Integer> services, Set<String> existingServiceNames,
      Set<String> excludedServices, ServiceFlags flags) {
    services.forEach((name, port) -> {
      if (excludedServices.contains(name)) {
        log.debug("Skipping excluded service: {}", name);
      } else if (!existingServiceNames.contains(name)) {
        String serviceUrl = RXConfigUtils.buildServiceUrl(
            name, flags.rxEnable, flags.tomcatEnable, flags.asyncEnable, flags.connectorEnable);
        ETRXConfig newRXConfig = saveServiceConfig(name, serviceUrl, port);
        addStaticServiceParamConfig(newRXConfig, "das.url", DAS_URL);
        addStaticServiceParamConfig(newRXConfig, "classic.url",
            flags.tomcatEnable ? DOCKER_TOMCAT_URL : getHostDockerInternalTomcatUrl());
      }
    });
  }

  /**
   * Adds a static service parameter configuration to the RX configuration.
   *
   * @param rxConfig
   *     the RX configuration
   * @param keyProperty
   *     the key property for the parameter
   * @param valueProperty
   *     the value property for the parameter
   */
  private void addStaticServiceParamConfig(ETRXConfig rxConfig, String keyProperty, String valueProperty) {
    ConfigServiceParam newParam = OBProvider.getInstance().get(ConfigServiceParam.class);
    newParam.setParameterKey(keyProperty);
    newParam.setParameterValue(valueProperty);
    newParam.setRXConfig(rxConfig);
    OBDal.getInstance().save(newParam);
  }

  /**
   * Saves the configuration for a service.
   *
   * @param name
   *     the name of the service
   * @param serviceUrl
   *     the URL of the service
   * @param port
   *     the port number of the service
   */
  protected ETRXConfig saveServiceConfig(String name, String serviceUrl, int port) {
    ETRXConfig newServiceConfig = OBProvider.getInstance().get(ETRXConfig.class);
    newServiceConfig.setServiceName(name);
    newServiceConfig.setUpdateableConfigs(!StringUtils.equals("config", name));
    newServiceConfig.setServiceURL(serviceUrl);
    newServiceConfig.setPublicURL(String.format(LOCALHOST_URL, port));
    OBDal.getInstance().save(newServiceConfig);
    return newServiceConfig;
  }

  /**
   * Handles exceptions that occur during the execution of the action.
   *
   * @param e
   *     the exception that occurred
   * @param actionResult
   *     the result of the action as a JSONObject
   * @return the updated action result with error information
   */
  private JSONObject handleException(Exception e, JSONObject actionResult) {
    log.error("Error during RX Service Initialization", e);
    log.error("RXConfigUtils Debug Info:\n{}", RXConfigUtils.getDebugInfo());
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put(MESSAGE_SEVERITY, MESSAGE_ERROR);
      errorMessage.put("title", ERROR_TITLE);
      errorMessage.put("text", e.getMessage());
      actionResult.put(MESSAGE, errorMessage);
    } catch (JSONException jsonException) {
      log.error("Error while creating error message JSON", jsonException);
    }
    return actionResult;
  }
}
