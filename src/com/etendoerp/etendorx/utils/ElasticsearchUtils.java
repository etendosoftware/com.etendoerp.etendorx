package com.etendoerp.etendorx.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Utility class for Elasticsearch operations using REST API.
 * Provides methods to index JSON documents and perform searches.
 */
public class ElasticsearchUtils {
  private static final Logger log = LogManager.getLogger();
  private static final String ELASTICSEARCH_HOST_PROPERTY = "elasticsearch.host";
  private static final String ELASTICSEARCH_PORT_PROPERTY = "elasticsearch.port";
  private static final String ELASTICSEARCH_SCHEME_PROPERTY = "elasticsearch.scheme";
  private static final String ELASTICSEARCH_USERNAME_PROPERTY = "elasticsearch.username";
  private static final String ELASTICSEARCH_PASSWORD_PROPERTY = "elasticsearch.password";
  
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 9200;
  private static final String DEFAULT_SCHEME = "http";
  
  private static final String FIELD_ID = "id";

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private ElasticsearchUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Gets the base URL for Elasticsearch from properties.
   *
   * @return the base URL (e.g., "http://localhost:9200")
   */
  private static String getBaseUrl() {
    Properties properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    
    String host = properties.getProperty(ELASTICSEARCH_HOST_PROPERTY, DEFAULT_HOST);
    int port = Integer.parseInt(properties.getProperty(ELASTICSEARCH_PORT_PROPERTY, String.valueOf(DEFAULT_PORT)));
    String scheme = properties.getProperty(ELASTICSEARCH_SCHEME_PROPERTY, DEFAULT_SCHEME);
    
    return String.format("%s://%s:%d", scheme, host, port);
  }

  /**
   * Gets the Basic Authentication header if credentials are configured.
   *
   * @return the Authorization header value or null if no credentials
   */
  private static String getAuthHeader() {
    Properties properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    
    String username = properties.getProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    String password = properties.getProperty(ELASTICSEARCH_PASSWORD_PROPERTY);
    
    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
      String credentials = username + ":" + password;
      return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
    
    return null;
  }

  /**
   * Sends an HTTP request to Elasticsearch.
   *
   * @param method the HTTP method (GET, POST, PUT, DELETE)
   * @param endpoint the API endpoint (e.g., "/myindex/_doc/123")
   * @param body the request body (null for GET/DELETE)
   * @return the response as a String
   * @throws OBException if the request fails
   */
  private static String sendRequest(String method, String endpoint, String body) {
    try {
      URL url = new URL(getBaseUrl() + endpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod(method);
      conn.setRequestProperty("Content-Type", "application/json");
      
      String authHeader = getAuthHeader();
      if (authHeader != null) {
        conn.setRequestProperty("Authorization", authHeader);
      }
      
      if (body != null && (method.equals("POST") || method.equals("PUT"))) {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
          os.write(body.getBytes(StandardCharsets.UTF_8));
          os.flush();
        }
      }
      
      int responseCode = conn.getResponseCode();
      
      BufferedReader reader;
      if (responseCode >= 200 && responseCode < 300) {
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
      } else {
        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
      }
      
      String response = reader.lines().collect(Collectors.joining("\n"));
      reader.close();
      
      if (responseCode < 200 || responseCode >= 300) {
        log.error("Elasticsearch request failed with code {}: {}", responseCode, response);
        throw new OBException("Elasticsearch request failed: " + response);
      }
      
      return response;
      
    } catch (Exception e) {
      log.error("Error sending request to Elasticsearch: {}", e.getMessage(), e);
      throw new OBException("Failed to communicate with Elasticsearch: " + e.getMessage(), e);
    }
  }

  /**
   * Indexes a JSON document into the specified Elasticsearch index.
   * The JSON document MUST contain an "id" field which will be used as the document ID.
   * If a document with the same ID exists, it will be overwritten.
   *
   * @param indexName the name of the index where the document will be stored
   * @param jsonDocument the JSON document to index as a String (must contain "id" field)
   * @return the document ID from the JSON
   * @throws OBException if indexing fails or if "id" field is missing
   */
  public static String indexDocument(String indexName, String jsonDocument) {
    if (indexName == null || indexName.trim().isEmpty()) {
      throw new OBException("Index name cannot be null or empty");
    }
    
    if (jsonDocument == null || jsonDocument.trim().isEmpty()) {
      throw new OBException("JSON document cannot be null or empty");
    }
    
    try {
      // Parse JSON to extract the "id" field
      JSONObject jsonObj = new JSONObject(jsonDocument);
      
      if (!jsonObj.has(FIELD_ID)) {
        throw new OBException("JSON document must contain an 'id' field");
      }
      
      String documentId = jsonObj.getString(FIELD_ID);
      
      if (documentId == null || documentId.trim().isEmpty()) {
        throw new OBException("The 'id' field cannot be null or empty");
      }
      
      // Index the document using PUT /{index}/_doc/{id}
      // Elasticsearch requires index names to be lowercase
      String normalizedIndexName = indexName.toLowerCase();
      String endpoint = String.format("/%s/_doc/%s", normalizedIndexName, documentId);
      String response = sendRequest("PUT", endpoint, jsonDocument);
      
      log.info("Document indexed successfully in index '{}' with ID: {}", indexName, documentId);
      return documentId;
      
    } catch (JSONException e) {
      log.error("Error parsing JSON document: {}", e.getMessage(), e);
      throw new OBException("Invalid JSON document: " + e.getMessage(), e);
    }
  }

  /**
   * Indexes a JSONObject into the specified Elasticsearch index.
   * The JSONObject MUST contain an "id" field which will be used as the document ID.
   * If a document with the same ID exists, it will be overwritten.
   *
   * @param indexName the name of the index where the document will be stored
   * @param jsonObject the JSONObject to index (must contain "id" field)
   * @return the document ID from the JSON
   * @throws OBException if indexing fails or if "id" field is missing
   */
  public static String indexDocument(String indexName, JSONObject jsonObject) {
    return indexDocument(indexName, jsonObject.toString());
  }

  /**
   * Searches for documents in a specific Elasticsearch index.
   *
   * @param indexName the name of the index to search in
   * @param searchBody the search query as JSONObject (Elasticsearch query DSL)
   * @return JSONArray containing the search results
   * @throws OBException if search fails
   */
  public static JSONArray search(String indexName, JSONObject searchBody) {
    if (indexName == null || indexName.trim().isEmpty()) {
      throw new OBException("Index name cannot be null or empty");
    }
    
    // Elasticsearch requires index names to be lowercase
    String normalizedIndexName = indexName.toLowerCase();
    return executeSearch(normalizedIndexName, searchBody);
  }

  /**
   * Searches for documents in a specific Elasticsearch index.
   *
   * @param indexName the name of the index to search in
   * @param searchBodyStr the search query as String (Elasticsearch query DSL JSON)
   * @return JSONArray containing the search results
   * @throws OBException if search fails or JSON parsing fails
   */
  public static JSONArray search(String indexName, String searchBodyStr) {
    if (searchBodyStr == null || searchBodyStr.trim().isEmpty()) {
      throw new OBException("Search body cannot be null or empty");
    }
    
    try {
      JSONObject searchBody = new JSONObject(searchBodyStr);
      return search(indexName, searchBody);
    } catch (JSONException e) {
      log.error("Error parsing search body string to JSON: {}", e.getMessage(), e);
      throw new OBException("Invalid JSON in search body: " + e.getMessage(), e);
    }
  }

  /**
   * Searches for documents across all Elasticsearch indices.
   *
   * @param searchBody the search query as JSONObject (Elasticsearch query DSL)
   * @return JSONArray containing the search results
   * @throws OBException if search fails
   */
  public static JSONArray searchAll(JSONObject searchBody) {
    return executeSearch("_all", searchBody);
  }

  /**
   * Searches for documents across all Elasticsearch indices.
   *
   * @param searchBodyStr the search query as String (Elasticsearch query DSL JSON)
   * @return JSONArray containing the search results
   * @throws OBException if search fails or JSON parsing fails
   */
  public static JSONArray searchAll(String searchBodyStr) {
    if (searchBodyStr == null || searchBodyStr.trim().isEmpty()) {
      throw new OBException("Search body cannot be null or empty");
    }
    
    try {
      JSONObject searchBody = new JSONObject(searchBodyStr);
      return searchAll(searchBody);
    } catch (JSONException e) {
      log.error("Error parsing search body string to JSON: {}", e.getMessage(), e);
      throw new OBException("Invalid JSON in search body: " + e.getMessage(), e);
    }
  }

  /**
   * Executes a search query against specified index or all indices.
   *
   * @param indexName the index name or "_all" for all indices
   * @param searchBody the search query as JSONObject (Elasticsearch query DSL)
   * @return JSONArray containing the search results
   * @throws OBException if search fails
   */
  private static JSONArray executeSearch(String indexName, JSONObject searchBody) {
    if (searchBody == null) {
      throw new OBException("Search body cannot be null");
    }
    
    try {
      // Send search request using POST /{index}/_search
      String endpoint = String.format("/%s/_search", indexName);
      String response = sendRequest("POST", endpoint, searchBody.toString());
      
      // Parse response
      JSONObject responseObj = new JSONObject(response);
      JSONObject hits = responseObj.getJSONObject("hits");
      JSONArray hitsArray = hits.getJSONArray("hits");
      
      // Convert results to simplified format
      JSONArray results = new JSONArray();
      for (int i = 0; i < hitsArray.length(); i++) {
        JSONObject hit = hitsArray.getJSONObject(i);
        JSONObject result = new JSONObject();
        result.put("index", hit.getString("_index"));
        result.put("id", hit.getString("_id"));
        result.put("score", hit.getDouble("_score"));
        result.put("source", hit.getJSONObject("_source"));
        results.put(result);
      }
      
      String searchLocation = indexName.equals("_all") ? "all indices" : "index: " + indexName;
      log.info("Search completed in {}. Found {} results", searchLocation, results.length());
      
      return results;
      
    } catch (Exception e) {
      log.error("Error executing search: {}", e.getMessage(), e);
      throw new OBException("Failed to execute search: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes an Elasticsearch index.
   *
   * @param indexName the name of the index to delete
   * @return true if deletion was successful
   * @throws OBException if deletion fails
   */
  public static boolean deleteIndex(String indexName) {
    if (indexName == null || indexName.trim().isEmpty()) {
      throw new OBException("Index name cannot be null or empty");
    }
    
    try {
      // Elasticsearch requires index names to be lowercase
      String normalizedIndexName = indexName.toLowerCase();
      
      // Delete index using DELETE /{index}
      String endpoint = String.format("/%s", normalizedIndexName);
      sendRequest("DELETE", endpoint, null);
      
      log.info("Index '{}' deleted successfully", indexName);
      return true;
      
    } catch (Exception e) {
      log.error("Error deleting index '{}': {}", indexName, e.getMessage(), e);
      throw new OBException("Failed to delete index: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if an Elasticsearch index exists.
   *
   * @param indexName the name of the index to check
   * @return true if the index exists, false otherwise
   * @throws OBException if the check fails
   */
  public static boolean indexExists(String indexName) {
    if (indexName == null || indexName.trim().isEmpty()) {
      throw new OBException("Index name cannot be null or empty");
    }
    
    try {
      // Elasticsearch requires index names to be lowercase
      String normalizedIndexName = indexName.toLowerCase();
      
      // Check index existence using HEAD /{index}
      URL url = new URL(getBaseUrl() + "/" + normalizedIndexName);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("HEAD");
      
      String authHeader = getAuthHeader();
      if (authHeader != null) {
        conn.setRequestProperty("Authorization", authHeader);
      }
      
      int responseCode = conn.getResponseCode();
      conn.disconnect();
      
      return responseCode == 200;
      
    } catch (Exception e) {
      log.error("Error checking if index '{}' exists: {}", indexName, e.getMessage(), e);
      throw new OBException("Failed to check index existence: " + e.getMessage(), e);
    }
  }
}
