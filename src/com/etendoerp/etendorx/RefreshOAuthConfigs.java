package com.etendoerp.etendorx;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

/**
 * This class extends the Action class and is used to refresh OAuth configurations.
 */
public class RefreshOAuthConfigs extends Action {

  private static final Logger log = LogManager.getLogger();
  private static final String ACTUATOR_RESTART = "/actuator/restart";
  private static final String POST = "POST";

  /**
   * This method is used to perform the action of refreshing OAuth configurations.
   * It first gets the ETRXConfig instance, then creates a connection to the authentication server,
   * sends a POST request, and checks the response. If any error occurs during the process, it logs the error and sets the ActionResult type to ERROR.
   *
   * @param parameters a JSONObject containing the parameters for the action
   * @param isStopped a MutableBoolean that indicates whether the action should be stopped
   * @return an ActionResult that indicates the result of the action
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    actionResult.setMessage("OAuth configurations are being refreshed.");
    ETRXConfig rxConfig = getETRXConfig();

    try {
      HttpURLConnection connection = createConnection(rxConfig);
      sendPostRequest(connection);
      checkResponse(connection, actionResult);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage(e.getMessage());
    }
    return actionResult;
  }

  /**
   * This method is used to get the ETRXConfig instance.
   *
   * @return an ETRXConfig instance
   */
  private ETRXConfig getETRXConfig() {
    return (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class).setMaxResults(1).uniqueResult();
  }

  /**
   * This method is used to create a connection to the authentication server.
   *
   * @param rxConfig the ETRXConfig instance
   * @return a HttpURLConnection to the authentication server
   * @throws IOException if an I/O error occurs while creating the connection
   */
  private HttpURLConnection createConnection(ETRXConfig rxConfig) throws IOException {
    URL url = new URL(rxConfig.getAuthURL() + ACTUATOR_RESTART);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(POST);
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json");
    return connection;
  }

  /**
   * This method is used to send a POST request to the authentication server.
   *
   * @param connection the HttpURLConnection to the authentication server
   * @throws IOException if an I/O error occurs while sending the request
   */
  private void sendPostRequest(HttpURLConnection connection) throws IOException {
    try (OutputStream os = connection.getOutputStream()) {
      byte[] input = "{}".getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }
  }

  /**
   * This method is used to check the response from the authentication server.
   * If the response code is not HTTP_OK, it sets the ActionResult type to ERROR.
   *
   * @param connection the HttpURLConnection to the authentication server
   * @param actionResult the ActionResult for the action
   * @throws IOException if an I/O error occurs while getting the response code
   */
  private void checkResponse(HttpURLConnection connection, ActionResult actionResult) throws IOException {
    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage("Restart failed! Try again.");
    }
  }

  /**
   * This method is used to get the input class for the action.
   *
   * @return the Class object for ETRXConfig
   */
  @Override
  protected Class<?> getInputClass() {
    return ETRXConfig.class;
  }
}