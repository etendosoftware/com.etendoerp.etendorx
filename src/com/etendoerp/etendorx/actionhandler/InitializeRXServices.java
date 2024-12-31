package com.etendoerp.etendorx.actionhandler;

import java.util.Map;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

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

      SERVICE_PORTS.forEach((name, port) -> {
        StringBuilder serviceUrlBuilder = new StringBuilder("http://");

        if (rxEnable && tomcatEnable) {
          serviceUrlBuilder.append(name).append(":").append(port);
        } else if (tomcatEnable) {
          serviceUrlBuilder.append("host.docker.internal:").append(port);
        } else {
          serviceUrlBuilder.append("localhost:").append(port);
        }
        ETRXConfig newServiceConfig = OBProvider.getInstance().get(ETRXConfig.class);
        newServiceConfig.setServiceName(name);
        newServiceConfig.setUpdateableConfigs(false);
        newServiceConfig.setServiceURL(serviceUrlBuilder.toString());
        newServiceConfig.setPublicURL("http://localhost:" + port);
        OBDal.getInstance().save(newServiceConfig);
      });
      OBDal.getInstance().flush();
      actionResult.put("refreshGrid", true);
    } catch (JSONException e) {
      log.error("Error in InitializeRXServices Action Handler", e);
      handleErrorMessage(e.getMessage(), actionResult);
    }
    return actionResult;
  }

  /**
   * This method is used to handle error messages.
   * It creates a JSONObject containing the error message and adds it to the result.
   *
   * @param message a String containing the error message
   * @param result a JSONObject containing the result
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
