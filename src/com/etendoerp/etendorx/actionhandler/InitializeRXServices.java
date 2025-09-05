package com.etendoerp.etendorx.actionhandler;

import java.util.Arrays;
import java.util.Collections;
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
  private static final String ASYNCPROCESS = "asyncprocess";
  private static final int ASYNCSERVICE_PORT = 9092;
  private static final String DAS_URL = "http://das:8092";
  private static final String DOCKER_TOMCAT_URL = "http://tomcat:8080";

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
      Properties obPropertiesProvider = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      boolean rxEnable = Boolean.parseBoolean(obPropertiesProvider.getProperty(PROPERTY_RX));
      boolean tomcatEnable = Boolean.parseBoolean(obPropertiesProvider.getProperty(PROPERTY_TOMCAT));
      boolean asyncEnable = Boolean.parseBoolean(obPropertiesProvider.getProperty(PROPERTY_ASYNC));
      boolean connectorEnable = Boolean.parseBoolean(obPropertiesProvider.getProperty(PROPERTY_CONNECTOR));
      String excludedServicesStr = obPropertiesProvider.getProperty("docker.exclude");
      Set<String> excludedServices = parseExcludedServices(excludedServicesStr);

      Set<String> existingServiceNames = OBDal.getInstance().createCriteria(ETRXConfig.class).list().stream()
          .map(ETRXConfig::getServiceName)
          .collect(Collectors.toSet());

      initializeServices(rxEnable, tomcatEnable, asyncEnable, connectorEnable, existingServiceNames, excludedServices);
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
   * @param rxEnable
   *     flag indicating if RX is enabled
   * @param tomcatEnable
   *     flag indicating if Tomcat is enabled
   * @param asyncEnable
   *     flag indicating if async processing is enabled
   * @param connectorEnable
   *     flag indicating if connector services are enabled
   * @param existingServiceNames
   *     the set of existing service names
   * @param excludedServices
   *     set of service names to skip
   */
  private void initializeServices(boolean rxEnable, boolean tomcatEnable, boolean asyncEnable,
      boolean connectorEnable, Set<String> existingServiceNames, Set<String> excludedServices) {
    if (rxEnable) {
      manageServices(RXConfigUtils.SERVICE_PORTS, existingServiceNames, excludedServices, rxEnable, tomcatEnable,
          asyncEnable,
          connectorEnable);
    }
    if (asyncEnable) {
      manageServices(Map.of(ASYNCPROCESS, ASYNCSERVICE_PORT), existingServiceNames, excludedServices, rxEnable,
          tomcatEnable, asyncEnable,
          connectorEnable);
    }
    if (connectorEnable) {
      manageServices(RXConfigUtils.CONNECTOR_SERVICES, existingServiceNames, excludedServices, rxEnable, tomcatEnable,
          asyncEnable,
          connectorEnable);
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
   * @param rxEnable
   *     flag indicating if RX is enabled
   * @param tomcatEnable
   *     flag indicating if Tomcat is enabled
   * @param asyncEnable
   *     flag indicating if async processing is enabled
   * @param connectorEnable
   *     flag indicating if connector services are enabled
   */
  private void manageServices(Map<String, Integer> services, Set<String> existingServiceNames,
      Set<String> excludedServices, boolean rxEnable, boolean tomcatEnable, boolean asyncEnable,
      boolean connectorEnable) {
    services.forEach((name, port) -> {
      boolean isExcluded = excludedServices.contains(name);
      if (!isExcluded && !existingServiceNames.contains(name)) {
        String serviceUrl = RXConfigUtils.buildServiceUrl(
            name, port, rxEnable, tomcatEnable, asyncEnable, connectorEnable);
        ETRXConfig newRXConfig = saveServiceConfig(name, serviceUrl, port);
        addStaticServiceParamConfig(newRXConfig, "das.url", DAS_URL);
        addStaticServiceParamConfig(newRXConfig, "classic.url",
            tomcatEnable ? DOCKER_TOMCAT_URL : getHostDockerInternalTomcatUrl());
      }
      log.debug("Skipping excluded service: {}", name);
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
