package com.etendoerp.etendorx.services;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.service.web.WebService;

import com.etendoerp.etendorx.utils.ElasticsearchUtils;

/**
 * Service that handles Elasticsearch search requests.
 *
 * <p>This service provides search functionality using the Elasticsearch utility.
 * It supports searching in specific indices or across all indices.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>Search in a specific index: POST to /elasticsearch with body:
 *       {"indexName": "myindex", "query": "search text"}</li>
 *   <li>Search across all indices: POST to /elasticsearch with body:
 *       {"query": "search text"}</li>
 * </ul>
 */
public class ElasticSearchService implements WebService {

  private static final Logger log = LogManager.getLogger();

  private static final String PARAM_INDEX_NAME = "indexName";
  private static final String PARAM_QUERY = "query";
  private static final String RESPONSE_STATUS = "status";
  private static final String RESPONSE_DATA = "data";
  private static final String RESPONSE_ERROR = "error";
  private static final String STATUS_SUCCESS = "success";
  private static final String STATUS_ERROR = "error";

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // GET method not supported for searches, use POST instead
    sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "GET method not supported. Use POST to perform searches.");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException, JSONException {
    try {
      // Read request body
      BufferedReader reader = request.getReader();
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }

      String requestBody = sb.toString();
      if (requestBody.trim().isEmpty()) {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
            "Request body cannot be empty");
        return;
      }

      JSONObject body = new JSONObject(requestBody);

      // Validate query parameter
      if (!body.has(PARAM_QUERY)) {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
            "Missing required parameter: 'query'");
        return;
      }
      
      // Get the search body - can be JSONObject or String
      JSONObject searchBody = null;
      try {
        searchBody = body.getJSONObject(PARAM_QUERY);
      } catch (JSONException e) {
        // If it's not a JSONObject, try to get it as a String
        String queryStr = body.getString(PARAM_QUERY);
        if (queryStr == null || queryStr.trim().isEmpty()) {
          sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
              "Query parameter cannot be empty");
          return;
        }
        searchBody = new JSONObject(queryStr);
      }

      // Perform search
      JSONArray results;
      String indexName = body.optString(PARAM_INDEX_NAME, null);

      if (indexName != null && !indexName.trim().isEmpty()) {
        log.info("Performing search in index '{}' with query: {}", indexName, searchBody.toString());
        results = ElasticsearchUtils.search(indexName, searchBody);
      } else {
        log.info("Performing search across all indices with query: {}", searchBody.toString());
        results = ElasticsearchUtils.searchAll(searchBody);
      }

      // Send success response
      sendSuccessResponse(response, results);

    } catch (OBException e) {
      log.error("Elasticsearch error: {}", e.getMessage(), e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Elasticsearch error: " + e.getMessage());
    } catch (JSONException e) {
      log.error("Invalid JSON in request: {}", e.getMessage(), e);
      sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
          "Invalid JSON format: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error during search: {}", e.getMessage(), e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unexpected error: " + e.getMessage());
    }
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // DELETE method not supported
    sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "DELETE method not supported for search service.");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // PUT method not supported
    sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "PUT method not supported for search service.");
  }

  /**
   * Sends a success response with search results.
   *
   * @param response
   *     the HTTP response object
   * @param results
   *     the search results as JSONArray
   * @throws IOException
   *     if writing to response fails
   * @throws JSONException
   *     if JSON creation fails
   */
  private void sendSuccessResponse(HttpServletResponse response, JSONArray results)
      throws IOException, JSONException {
    JSONObject responseJson = new JSONObject();
    responseJson.put(RESPONSE_STATUS, STATUS_SUCCESS);
    responseJson.put(RESPONSE_DATA, results);
    responseJson.put("count", results.length());

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json; charset=UTF-8");
    response.getWriter().write(responseJson.toString());
  }

  /**
   * Sends an error response.
   *
   * @param response
   *     the HTTP response object
   * @param statusCode
   *     the HTTP status code
   * @param errorMessage
   *     the error message
   * @throws IOException
   *     if writing to response fails
   * @throws JSONException
   *     if JSON creation fails
   */
  private void sendErrorResponse(HttpServletResponse response, int statusCode, String errorMessage)
      throws IOException, JSONException {
    JSONObject responseJson = new JSONObject();
    responseJson.put(RESPONSE_STATUS, STATUS_ERROR);
    responseJson.put(RESPONSE_ERROR, errorMessage);

    response.setStatus(statusCode);
    response.setContentType("application/json; charset=UTF-8");
    response.getWriter().write(responseJson.toString());
  }
}

