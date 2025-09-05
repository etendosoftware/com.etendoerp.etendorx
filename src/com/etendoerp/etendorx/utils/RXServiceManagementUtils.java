package com.etendoerp.etendorx.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Utility class for managing interactions with RX services.
 * Provides methods to create connections, send requests, and handle responses.
 */
public class RXServiceManagementUtils {
  public static final String ACTUATOR_HEALTH = "/actuator/health";
  private static final Logger log = LogManager.getLogger();
  private static final String POST = "POST";
  private static final String GET = "GET";

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private RXServiceManagementUtils() {
    throw new IllegalStateException("Utility class shouldn't be instantiated");
  }

  /**
   * This method is used to create a connection to the RX server and perform a restart.
   *
   * @param urlStr
   *     the URL string to make the connection
   * @param actionResult
   *     the ActionResult to update the result
   * @throws IOException
   *     if an I/O error occurs
   */
  public static void performRestart(String urlStr, ActionResult actionResult) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection connection = createPOSTConnection(url, null);
    try {
      checkResponse(connection, actionResult);
    } finally {
      connection.disconnect();
    }
  }

  /**
   * This method is used to create a POST connection to an RX service, optionally including a body.
   *
   * @param url
   *     the URL to make the connection
   * @param body
   *     the body to send in the POST request (optional, can be null)
   * @return a HttpURLConnection to an RX service
   * @throws IOException
   *     if an I/O error occurs while creating the connection
   */
  public static HttpURLConnection createPOSTConnection(URL url, String body) throws IOException {
    log.debug("Creating POST connection to URL: {}", url);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(POST);
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json");

    if (body != null) {
      log.debug("Sending body: {}", body);
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = body.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }
    }

    return connection;
  }

  /**
   * This method is used to check the response from the RX Service.
   * If the response code is not HTTP_OK, it sets the ActionResult type to ERROR.
   *
   * @param connection
   *     the HttpURLConnection to the RX service
   * @param actionResult
   *     the ActionResult for the action
   * @throws IOException
   *     if an I/O error occurs while getting the response code
   */
  private static void checkResponse(HttpURLConnection connection, ActionResult actionResult) throws IOException {
    int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      String responseMessage = connection.getResponseMessage();
      log.error("Response Code: {}, Message: {}", responseCode, responseMessage);
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage(responseMessage);
    }
  }

  /**
   * Checks if the service at the given URL is running by calling its health endpoint.
   * <p>
   * The method performs a GET request to the URL concatenated with {@code ACTUATOR_HEALTH}.
   * The result of the check is stored in the provided {@link ActionResult} object.
   * If the connection fails, {@code actionResult} will be marked as {@link Result.Type#ERROR}
   * and an appropriate message will be set.
   *
   * @param urlStr
   *     the base URL of the service to check (without the health path)
   * @param actionResult
   *     the object where the result of the check will be stored
   */
  public static void checkRunning(String urlStr, ActionResult actionResult) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(urlStr + ACTUATOR_HEALTH);
      connection = createGETConnection(url);
      checkResponse(connection, actionResult);
    } catch (IOException e) {
      actionResult.setType(Result.Type.ERROR);
      if (e instanceof ConnectException) {
        actionResult.setMessage(OBMessageUtils.messageBD("ETRX_ServiceNotRunning") + " ");
      } else {
        actionResult.setMessage(e.getMessage());
      }
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private static HttpURLConnection createGETConnection(URL url) throws IOException {
    log.debug("Creating GET connection to URL: {}", url);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(GET);
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json");
    return connection;
  }
}
