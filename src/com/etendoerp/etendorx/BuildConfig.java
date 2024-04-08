package com.etendoerp.etendorx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.base.session.OBPropertiesProvider;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

public class BuildConfig extends HttpBaseServlet {

  private static Logger log = LogManager.getLogger();
  public static final String OAUTH_CLIENT_REG = "spring.security.oauth2.client.registration.";
  public static final String OAUTH_CLIENT_PROV = "spring.security.oauth2.client.provider.";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    JSONObject result = new JSONObject();
    try {
      result = getDefaultConfigToJsonObject();
      result.getJSONArray("propertySources").getJSONObject(0).getJSONObject("source");
      JSONObject sourceJSON = result.getJSONArray("propertySources").getJSONObject(0).getJSONObject("source");
      OBContext.setAdminMode();
      OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
          .setFilterOnReadableOrganization(false)
          .setFilterOnReadableClients(false)
          .list().forEach(provider -> {
        try {
          String providerName = provider.getValue();
          sourceJSON.put(providerName + "-api", provider.getAPIUrl());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".provider", providerName);
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".client-id", provider.getIdforclient());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".scope", provider.getScope());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".client-name", provider.getClientname());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".authorization-grant-type", provider.getAuthorizationGrantType());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".redirectUri", provider.getRedirecturi());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".code_challenge_method", provider.getCodechallengemethod());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".client-authentication-method", provider.getClientauthenticationmethod());
          sourceJSON.put(OAUTH_CLIENT_REG + providerName + ".token-uri", provider.getAPIUrl() + provider.getTokenuri());
          sourceJSON.put(OAUTH_CLIENT_PROV + providerName + ".authorization-uri", provider.getAPIUrl() + provider.getAuthorizationuri());
          sourceJSON.put(OAUTH_CLIENT_PROV + providerName + ".token-uri", provider.getAPIUrl() + provider.getTokenuri());
          sourceJSON.put(OAUTH_CLIENT_PROV + providerName + ".user-info-uri",  provider.getAPIUrl() + provider.getUserinfouri());
          sourceJSON.put(OAUTH_CLIENT_PROV + providerName + ".user-name-attribute", provider.getUsernameattribute());
        } catch (JSONException e) {
          log.error(e.getMessage(), e);
        }
      });

      response.setContentType("application/json");
      response.setCharacterEncoding("utf-8");
      final Writer w = response.getWriter();
      result.getJSONArray("propertySources").getJSONObject(0).put("source", sourceJSON);
      w.write(result.toString());
      w.close();
    } catch (OBException | JSONException e) {
      log.error(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get a JSON object from the default configs of RX.
   * It sends a GET request to the URL and reads the response line by line.
   * The response is then converted into a JSON object which is returned by the method.
   *
   * @return JSONObject This returns the JSON object from the response.
   * @throws IOException On input error.
   * @throws JSONException On error parsing the response to a JSON object.
   */
  private static JSONObject getDefaultConfigToJsonObject() throws IOException, JSONException {
    JSONObject result;
    URL url = new URL((String) OBPropertiesProvider.getInstance().getOpenbravoProperties().get("config.url"));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    final InputStream inputStream = conn.getInputStream();
    BufferedReader bR = new BufferedReader(new InputStreamReader(inputStream));
    String line = "";
    StringBuilder responseStrBuilder = new StringBuilder();
    while ((line =  bR.readLine()) != null){
      responseStrBuilder.append(line);
    }
    inputStream.close();
    result = new JSONObject(responseStrBuilder.toString());

    return result;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    throw new OBException("POST not supported");
  }
}
