package com.etendoerp.etendorx.webhooks;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.utils.ElasticsearchUtils;
import com.etendoerp.webhookevents.services.BaseWebhookService;
import com.smf.securewebservices.utils.WSResult;
import com.smf.securewebservices.utils.WSResult.Status;

/**
 * Webhook service for performing Elasticsearch searches.
 *
 * <p>This webhook allows searching documents in Elasticsearch indices using the
 * ElasticsearchUtils utility. It supports searching in specific indices or across
 * all indices using Lucene query syntax.
 *
 * <p>Example usage:
 * <pre>
 * GET /sws/com.etendoerp.etendorx/ElasticSearch?query=name:john&indexName=users
 * GET /sws/com.etendoerp.etendorx/ElasticSearch?query=email:*@example.com
 * </pre>
 */
public class ElasticSearch extends BaseWebhookService {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearch.class);
  private static final String PARAM_QUERY = "query";
  private static final String PARAM_INDEX_NAME = "indexName";
  private static final String MESSAGE = "message";

  /**
   * Handles the GET request for the webhook.
   * This method retrieves the query and optional indexName from the parameters,
   * performs an Elasticsearch search, and constructs a JSON response with the results.
   *
   * @param parameter
   *     A map of request parameters containing 'searchQuery' (required) and 'indexName' (optional).
   * @param responseVars
   *     A map to store the response variables.
   */
  @Override
  public void get(Map<String, String> parameter, Map<String, String> responseVars) {
    LOG.debug("Executing WebHook: ElasticSearch");
    for (Map.Entry<String, String> entry : parameter.entrySet()) {
      LOG.debug("Parameter: {} = {}", entry.getKey(), entry.getValue());
    }

    String searchQueryStr = parameter.get("searchQuery");
    String indexName = parameter.get(PARAM_INDEX_NAME);

    if (StringUtils.isEmpty(searchQueryStr)) {
      responseVars.put("error", "Missing required parameter: searchQuery");
      return;
    }

    try {
      WSResult result = handleElasticsearchSearch(searchQueryStr, indexName);
      responseVars.put(MESSAGE, result.getJSONResponse().toString());
    } catch (Exception e) {
      LOG.error("Error processing Elasticsearch search", e);
      responseVars.put("error", e.getMessage());
    }
  }

  /**
   * Handles the Elasticsearch search based on the provided request parameters.
   *
   * @param searchBodyStr
   *     The search query as JSON string (Elasticsearch query DSL).
   * @param indexName
   *     The name of the index to search in. If null or empty, searches across all indices.
   * @return A WSResult object containing the search results.
   * @throws JSONException
   *     If an error occurs while processing JSON data.
   */
  public static WSResult handleElasticsearchSearch(String searchBodyStr, String indexName) throws JSONException {
    WSResult wsResult = new WSResult();

    try {
      JSONArray searchResults;

      if (StringUtils.isNotBlank(indexName)) {
        LOG.info("Performing Elasticsearch search in index '{}' with query: {}", indexName, searchBodyStr);
        searchResults = ElasticsearchUtils.search(indexName, searchBodyStr);
      } else {
        LOG.info("Performing Elasticsearch search across all indices with query: {}", searchBodyStr);
        searchResults = ElasticsearchUtils.searchAll(searchBodyStr);
      }

      // Build response with results
      JSONObject response = new JSONObject();
      response.put("count", searchResults.length());
      response.put("results", searchResults);

      wsResult.setStatus(Status.OK);
      wsResult.setData(response);

      LOG.info("Search completed successfully. Found {} results", searchResults.length());

    } catch (Exception e) {
      LOG.error("Error during Elasticsearch search: {}", e.getMessage(), e);
      wsResult.setStatus(Status.INTERNAL_SERVER_ERROR);
      wsResult.setMessage("Elasticsearch error: " + e.getMessage());
    }

    return wsResult;
  }
}

