/*
 * Copyright 2022-2024  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendoerp.etendorx.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Utility class for handling asynchronous processes.
 */
public class AsyncProcessUtil {

  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private AsyncProcessUtil() {
  }

  /**
   * Creates an HTTP request for the given URI.
   *
   * @param uri the URI to send the request to
   * @return the created HTTP request
   */
  public static HttpRequest getRequest(String uri) {
    ETRXConfig eTRXConfig = RXConfigUtils.getRXConfig("asyncprocess");
    String asyncUrl = eTRXConfig.getServiceURL();
    try {
      String asyncToken = SecureWebServicesUtils.generateToken(OBContext.getOBContext().getUser());
      return HttpRequest.newBuilder()
          .uri(URI.create(
              asyncUrl + uri))
          .header("Content-Type", "application/json")
          .header("Authorization",
              "Bearer " + asyncToken)
          .GET()
          .build();
    } catch (Exception e) {
      throw new OBException(e.getMessage(), true);
    }
  }

  /**
   * Formats a date string from "dd-MM-yyyy HH:mm:ss:SSS" format to "yyyy-MM-dd HH:mm:ss:SSS" format.
   *
   * @param dateStr the date string to format
   * @return the formatted date string
   * @throws ParseException if the input date string cannot be parsed
   */
  public static String fmtDate(String dateStr) throws ParseException {
    SimpleDateFormat fmtToDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS");
    SimpleDateFormat fmtToStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    return fmtToStr.format(fmtToDate.parse(dateStr));
  }

  /**
   * Parses a JSON string into a list of maps.
   *
   * @param body the JSON string to parse
   * @return the parsed list of maps
   * @throws JsonProcessingException if the input string cannot be parsed
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> getRows(String body) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(body, List.class);
  }

  /**
   * Sends an HTTP request to the given URI, parses the response body into a list of maps,
   * applies a transformation to each map, and returns the resulting list.
   *
   * @param uri the URI to send the request to
   * @return the list of transformed maps
   * @throws OBException if an error occurs during the HTTP request or the parsing of the response
   */
  public static List<Map<String, Object>> getList(String uri) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      var response = client.send(AsyncProcessUtil.getRequest(uri),
          HttpResponse.BodyHandlers.ofString());
      return getRows(response.body());
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OBException(e);
    }
  }
}
