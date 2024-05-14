package com.etendoerp.etendorx;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import java.util.AbstractMap.SimpleEntry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * This class is the base class for the build configuration servlets. It provides the basic
 * functionality for retrieving the default configuration and updating it with the OAuth providers
 * details.
 */
public abstract class BuildConfig extends HttpBaseServlet {

  private static final Logger log = LogManager.getLogger();
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION = "spring.security.oauth2.client.registration.";
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER = "spring.security.oauth2.client.provider.";

  /**
   * This method handles the GET request. It fetches the default configuration, updates it with the OAuth providers details and sends the response.
   * It also handles the creation of a new source if no default configuration is found for the specific service.
   *
   * @param request The HttpServletRequest object.
   * @param response The HttpServletResponse object.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      OBContext.setAdminMode();
      final String serviceURI = getURIFromRequest(request);
      final String service = serviceURI.split("/")[1];
      final JSONObject result = getDefaultConfigToJsonObject(serviceURI);
      SimpleEntry<Integer, JSONObject> sourceEntry = findSource(result.getJSONArray("propertySources"), service);
      // TODO: Improve the way to check if the service needs the oAuthProvider configuration.
      if (StringUtils.equals("auth", service) || StringUtils.equals("psd2", service)) {
        updateSourceWithOAuthProviders(sourceEntry.getValue());
      }
      sendResponse(response, result, sourceEntry.getValue(), sourceEntry.getKey());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method extracts the URI from the given HTTP request.
   *
   * @param request The HTTP request from which the URI is to be extracted.
   * @return The URI as a string. It is the part of the request URL that comes after the servlet path.
   */
  private static String getURIFromRequest(HttpServletRequest request) {
    // TODO: Improve the way to get the URI
    return request.getRequestURL().toString().split(request.getServletPath())[1];
  }

  /**
   * This method gets the default configuration from the ETRXConfig entity and converts it to a JSON object.
   *
   * @return The default configuration as a JSON object.
   * @throws JSONException If there is an error parsing the JSON.
   */
  private JSONObject getDefaultConfigToJsonObject(String serviceURI) throws JSONException, IOException {
    ETRXConfig rxConfig = (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class)
        .setMaxResults(1)
        .uniqueResult();
    URL url = new URL(rxConfig.getConfigURL() + serviceURI);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    final InputStream inputStream = conn.getInputStream();
    BufferedReader bR = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    StringBuilder responseStrBuilder = new StringBuilder();
    while ((line =  bR.readLine()) != null){
      responseStrBuilder.append(line);
    }
    inputStream.close();
    return new JSONObject(responseStrBuilder.toString());
  }

  /**
   * This method finds the source JSON object in the property sources array.
   * If no default config is found for the specific service, it retrieves the application default config.
   *
   * @param propSource The property sources array.
   * @param service The service name.
   * @return The index and the source JSON object.
   * @throws JSONException If there is an error parsing the JSON.
   */
  private SimpleEntry<Integer, JSONObject> findSource(JSONArray propSource, String service) throws JSONException {
    final int length = propSource.length();
    for (int i = 0; i < length; i++) {
      if (propSource.getJSONObject(i).getString("name").contains(service)) {
        return new SimpleEntry<>(i, propSource.getJSONObject(i).getJSONObject("source"));
      }
    }
    // If no default config is found for the specific service, retrieve the application default config.
    // That it's always load at the last index of the JSONArray
    return new SimpleEntry<>(length - 1, propSource.getJSONObject(length - 1).getJSONObject("source"));
  }

  /**
   * This method updates the source JSON object with the OAuth providers details.
   *
   * @param sourceJSON The source JSON object.
   */
  private void updateSourceWithOAuthProviders(JSONObject sourceJSON) {
    OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
        .setFilterOnReadableOrganization(false)
        .setFilterOnReadableClients(false)
        .list().forEach(provider -> updateSourceWithOAuthProvider(sourceJSON, provider));
  }

  /**
   * This method updates the source JSON object with the details of a single OAuth provider.
   *
   * @param sourceJSON The source JSON object.
   * @param provider The OAuth provider.
   */
  private void updateSourceWithOAuthProvider(JSONObject sourceJSON, ETRXoAuthProvider provider) {
    try {
      String providerName = provider.getValue();
      String apiUrl = provider.getAPIUrl();
      sourceJSON.put(providerName + "-api", apiUrl);
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".provider", providerName);
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".client-id", provider.getIdforclient());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".scope", provider.getScope());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".client-name", provider.getClientname());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".authorization-grant-type", provider.getAuthorizationGrantType());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".redirectUri", provider.getRedirecturi());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".code_challenge_method", provider.getCodechallengemethod());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".client-authentication-method", provider.getClientauthenticationmethod());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName + ".token-uri", apiUrl + provider.getTokenuri());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName + ".authorization-uri", apiUrl + provider.getAuthorizationuri());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName + ".token-uri", apiUrl + provider.getTokenuri());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName + ".user-info-uri",  apiUrl + provider.getUserinfouri());
      sourceJSON.put(SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName + ".user-name-attribute", provider.getUsernameattribute());
      // Here the abstract method is called to fill custom configurations.
      fillCustomConfigs(sourceJSON, provider);
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  /**
   * This method sends the response to the client.
   *
   * @param response The HTTP response.
   * @param result The result JSON object.
   * @param sourceJSON The source JSON object.
   * @param indexFound The index of the source JSON object in the property sources array.
   * @throws IOException If there is an error writing the response.
   */
  private void sendResponse(HttpServletResponse response, JSONObject result, JSONObject sourceJSON, Integer indexFound) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try (Writer w = response.getWriter()) {
      result.getJSONArray("propertySources").getJSONObject(indexFound).put("source", sourceJSON);
      w.write(result.toString());
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    throw new OBException("POST not supported");
  }

  /**
   * This abstract method is used to fill custom configurations in the source JSON object.
   * Implementing classes should add configurations that follow the same structure as a YAML file.
   * <p>
   * For example, to add a client-id for a specific OAuth provider, the following code can be used:
   * <code>sourceJSON.put("spring.security.oauth2.client.registration." + providerName +
   * ".client-id", provider.getIdforclient());</code>
   * <p>
   * This method should be implemented by any class that extends BuildConfig.
   *
   * @param sourceJSON The source JSON object where the custom configurations should be added.
   * @param provider The OAuth provider for which the custom configurations are being added.
   */
  public abstract void fillCustomConfigs(JSONObject sourceJSON, ETRXoAuthProvider provider);
}