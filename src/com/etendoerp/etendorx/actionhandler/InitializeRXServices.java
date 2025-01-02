package com.etendoerp.etendorx.actionhandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXConfig;

/**
 * Action handler to initialize RX services.
 */
public class InitializeRXServices extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();
  private static final String MESSAGE = "message";
  private static final String MESSAGE_SEVERITY = "severity";
  private static final String MESSAGE_TEXT = "text";
  private static final String MESSAGE_SUCCESS = "success";
  private static final String MESSAGE_ERROR = "error";

  private static final Map<String, Integer> SERVICE_PORTS = Map.of(
      "config", 8888,
      "auth", 8094,
      "das", 8092,
      "edge", 8096
  );
  private static final String ASYNC_SERVICE_NAME = "asyncprocess";
  private static final int ASYNC_SERVICE_PORT = 9092;
  private static final Map<String, Integer> CONNECTOR_SERVICES = Map.of(
      "worker", 0,
      "obconnsrv", 8101
  );


  /**
   * Executes the action to initialize RX services.
   *
   * @param parameters the parameters for the action
   * @param content the content for the action
   * @return a JSONObject containing the result of the action
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject actionResult = new JSONObject();
    try {
      actionResult.put(MESSAGE_SEVERITY, MESSAGE_SUCCESS);
      actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD(MESSAGE_SUCCESS) + "<br/>"
          + "RX services have been initialized.");

      OBPropertiesProvider obPropertiesProvider = OBPropertiesProvider.getInstance();
      boolean rxEnable = Boolean.parseBoolean(
          obPropertiesProvider.getOpenbravoProperties().getProperty("docker_com.etendoerp.etendorx"));
      boolean tomcatEnable = Boolean.parseBoolean(
          obPropertiesProvider.getOpenbravoProperties().getProperty("docker_com.etendoerp.tomcat"));
      boolean asyncEnable = Boolean.parseBoolean(
          obPropertiesProvider.getOpenbravoProperties().getProperty("docker_com.etendoerp.etendorx_async"));
      boolean connectorEnable = Boolean.parseBoolean(
          obPropertiesProvider.getOpenbravoProperties().getProperty("docker_com.etendoerp.etendorx_connector"));

      SERVICE_PORTS.forEach((name, port) -> {
        String serviceUrl = buildServiceUrl(name, port, rxEnable, tomcatEnable);
        saveServiceConfig(name, serviceUrl, port);
      });

      if (asyncEnable) {
        String asyncServiceUrl = buildServiceUrl(ASYNC_SERVICE_NAME, ASYNC_SERVICE_PORT, rxEnable, tomcatEnable);
        saveServiceConfig(ASYNC_SERVICE_NAME, asyncServiceUrl, ASYNC_SERVICE_PORT);
      }

      if (connectorEnable) {
        CONNECTOR_SERVICES.forEach((name, port) -> {
          String connectorServiceUrl = buildServiceUrl(name, port, rxEnable, tomcatEnable);
          saveServiceConfig(name, connectorServiceUrl, port);
        });
      }

      OBDal.getInstance().flush();
      actionResult.put("refreshGrid", true);
    } catch (JSONException e) {
      log.error("Error in InitializeRXServices Action Handler", e);
      handleErrorMessage(e.getMessage(), actionResult);
    }
    return actionResult;
  }

  /**
   * Builds the service URL based on the service name, port, and enable flags.
   *
   * @param name the name of the service
   * @param port the port of the service
   * @param rxEnable flag indicating if RX is enabled
   * @param tomcatEnable flag indicating if Tomcat is enabled
   * @return the constructed service URL
   */
  private String buildServiceUrl(String name, int port, boolean rxEnable, boolean tomcatEnable) {
    StringBuilder serviceUrlBuilder = new StringBuilder("http://");

    if (rxEnable && tomcatEnable) {
      serviceUrlBuilder.append(name).append(":").append(port);
    } else if (tomcatEnable) {
      serviceUrlBuilder.append("host.docker.internal:").append(port);
    } else {
      serviceUrlBuilder.append("localhost:").append(port);
    }

    return serviceUrlBuilder.toString();
  }

  /**
   * Saves the service configuration to the database.
   *
   * @param name the name of the service
   * @param serviceUrl the URL of the service
   * @param port the port of the service
   */
  private void saveServiceConfig(String name, String serviceUrl, int port) {
    ETRXConfig newServiceConfig = OBProvider.getInstance().get(ETRXConfig.class);
    newServiceConfig.setServiceName(name);
    newServiceConfig.setUpdateableConfigs(!StringUtils.equals("config", name));
    newServiceConfig.setServiceURL(serviceUrl);
    newServiceConfig.setPublicURL("http://localhost:" + port);
    OBDal.getInstance().save(newServiceConfig);
  }

  /**
   * Handles an error message by adding it to the result.
   *
   * @param message the error message
   * @param result the result to add the error message to
   */
  private static void handleErrorMessage(String message, JSONObject result) {
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put(MESSAGE_SEVERITY, MESSAGE_ERROR);
      errorMessage.put("title", "ERROR");
      errorMessage.put("text", message);
      result.put(MESSAGE, errorMessage);
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
    }
  }
}
