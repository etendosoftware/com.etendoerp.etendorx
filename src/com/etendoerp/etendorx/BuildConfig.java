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

public class BuildConfig extends HttpBaseServlet {

  private static final Logger log = LogManager.getLogger();
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION = "spring.security.oauth2.client.registration.";
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER = "spring.security.oauth2.client.provider.";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      OBContext.setOBContext("0");
      JSONObject result = getDefaultConfigToJsonObject();
      SimpleEntry<Integer, JSONObject> sourceEntry = findSource(result);
      updateSourceWithOAuthProviders(sourceEntry.getValue());
      sendResponse(response, result, sourceEntry.getValue(), sourceEntry.getKey());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method gets the default configuration from the ETRXConfig entity and converts it to a JSON object.
   *
   * @return The default configuration as a JSON object.
   * @throws JSONException If there is an error parsing the JSON.
   */
  private JSONObject getDefaultConfigToJsonObject() throws JSONException, IOException {
    ETRXConfig rxConfig = (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class)
        .setMaxResults(1)
        .uniqueResult();
    URL url = new URL(rxConfig.getConfigUrl() + "/auth/default");
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
   * This method finds the source with the server port 8094 from the result JSON object.
   *
   * @param result The result JSON object.
   * @return The source JSON object.
   * @throws JSONException If there is an error parsing the JSON.
   */
  private SimpleEntry<Integer, JSONObject> findSource(JSONObject result) throws JSONException {
    JSONArray propSource = result.getJSONArray("propertySources");
    for (int i = 0; i < propSource.length(); i++) {
      JSONObject source = propSource.getJSONObject(i).getJSONObject("source");
      if (StringUtils.equals(source.getString("server.port"), "8094")) {
        return new SimpleEntry<>(i, source);
      }
    }
    return new SimpleEntry<>(-1, new JSONObject());
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
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

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
}