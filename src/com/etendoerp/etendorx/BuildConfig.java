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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etendoerp.etendorx.data.ConfigServiceParam;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.RXConfigUtils;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjector;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjectorRegistry;
import com.smf.securewebservices.SWSConfig;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * This class is the base class for the build configuration servlets. It provides the basic
 * functionality for retrieving the default configuration and updating it with the OAuth providers
 * details.
 */
public class BuildConfig extends HttpBaseServlet {

  private static final Logger log = LogManager.getLogger();
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION = "spring.security.oauth2.client.registration.";
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER = "spring.security.oauth2.client.provider.";
  private static final String MANAGEMENT_ENDPOINT_RESTART_ENABLED = "management.endpoint.restart.enabled";
  private static final String CONFIG_URL = "http://config:8888";
  private static final String SOURCE = "source";
  private static final String AUTH_SERVICE = "auth";
  private static final String CONFIG_SERVICE = "config";
  private static final String ES256_ALGORITHM = "ES256";
  private static final String PRIVATE_KEY = "private-key";
  private static final String PUBLIC_KEY = "public-key";
  private static final String SYS_USER_ID = "0";
  private static final String APPLICATION = "application";
  private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
  private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

  /**
   * This method handles the GET request. It fetches the default configuration,
   * updates it with the OAuth providers details and sends the response.
   * It also handles the creation of a new source if no default configuration is found for the specific service.
   *
   * @param request The HttpServletRequest object.
   * @param response The HttpServletResponse object.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      OBContext.setAdminMode();
      SWSConfig swsConfig = SWSConfig.getInstance();
      if (swsConfig == null || swsConfig.getPrivateKey() == null) {
        throw new OBException(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_Misconfigured",
            OBContext.getOBContext().getLanguage().getLanguage()));
      }
      final String serviceURI = getURIFromRequest(request);
      final String service = StringUtils.equals(APPLICATION, serviceURI.split("/")[1]) ?
          CONFIG_SERVICE : serviceURI.split("/")[1];
      final JSONObject defaultConfig = getDefaultConfigToJsonObject(serviceURI);
      SimpleEntry<Integer, JSONObject> sourceEntry = findSource(defaultConfig.getJSONArray("propertySources"), service);
      ETRXConfig rxConfig = RXConfigUtils.getRXConfig(service);
      if (rxConfig == null) {
        String dbMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_NoConfigFound",
            OBContext.getOBContext().getLanguage().getLanguage());
        throw new OBException(StringUtils.replace(dbMessage, "%s" , service));
      }
      JSONObject sourceJSON = sourceEntry.getValue();
      addRXParams(sourceJSON, rxConfig);
      // No need (for now) to check the services to be updated due to only those who needs the config
      // will change the url to get the config from config server to this endpoint.
      List<OAuthProviderConfigInjector> allInjectors = OAuthProviderConfigInjectorRegistry.getInjectors();
      for (OAuthProviderConfigInjector injector : allInjectors) {
        injector.injectConfig(defaultConfig);
      }
      String algorithmPref = Preferences.getPreferenceValue("SMFSWS_EncryptionAlgorithm", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(),
          null);
      if (!StringUtils.equals(ES256_ALGORITHM, algorithmPref)) {
        String errorMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_WrongAlgorithm",
            OBContext.getOBContext().getLanguage().getLanguage()) + algorithmPref;
        throw new UnsupportedOperationException(errorMessage);
      }
      JSONObject keys = new JSONObject(swsConfig.getPrivateKey());
      if (StringUtils.equals(AUTH_SERVICE, service)) {
        User tokenUser = OBDal.getInstance().get(User.class, SYS_USER_ID);
        String sysToken = SecureWebServicesUtils.generateToken(tokenUser);
        tokenUser = OBDal.getInstance().get(User.class, "100");
        String adminToken = SecureWebServicesUtils.generateToken(tokenUser);
        sourceJSON.put("token", sysToken);
        sourceJSON.put("adminToken", adminToken);
        sourceJSON.put(PRIVATE_KEY, keys.getString(PRIVATE_KEY));
        updateSourceWithOAuthProviders(sourceEntry.getValue(), allInjectors);
      }
      var publicKey = StringUtils.replace(keys.getString(PUBLIC_KEY), BEGIN_PUBLIC_KEY,"");
      publicKey = StringUtils.replace(publicKey, END_PUBLIC_KEY,"");
      sourceJSON.put(PUBLIC_KEY, publicKey);
      sendResponse(response, defaultConfig, sourceEntry.getValue(), sourceEntry.getKey());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      sendErrorResponse(response, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method adds the RX parameters to the services JSON Object for configs.
   *
   * @param sourceJSON The source JSON object.
   * @param rxConfig The RX configuration.
   */
  private void addRXParams(JSONObject sourceJSON, ETRXConfig rxConfig) {
    List<ConfigServiceParam> paramList = rxConfig.getETRXServiceParamList();
    for (ConfigServiceParam param : paramList) {
      try {
        sourceJSON.put(param.getParameterKey(), param.getParameterValue());
      } catch (JSONException e) {
        log.error(e.getMessage(), e);
        throw new OBException(e);
      }
    }
  }

  /**
   * This method extracts the URI from the given HTTP request.
   *
   * @param request The HTTP request from which the URI is to be extracted.
   * @return The URI as a string. It is the part of the request URL that comes after the servlet path.
   */
  private static String getURIFromRequest(HttpServletRequest request) {
    return request.getRequestURL().toString().split(request.getServletPath())[1];
  }

  /**
   * This method gets the default configuration from the ETRXConfig entity and converts it to a JSON object.
   *
   * @return The default configuration as a JSON object.
   * @throws JSONException If there is an error parsing the JSON.
   */
  private JSONObject getDefaultConfigToJsonObject(String serviceURI) throws JSONException, IOException {
    ETRXConfig rxConfig = RXConfigUtils.getRXConfig(CONFIG_SERVICE);
    String serverURL = rxConfig == null ? CONFIG_URL : rxConfig.getServiceURL();
    URL url = new URL(serverURL + serviceURI);
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
        return new SimpleEntry<>(i, propSource.getJSONObject(i).getJSONObject(SOURCE));
      }
    }
    // If no default config is found for the specific service, retrieve the application default config.
    // That it's always load at the last index of the JSONArray
    return new SimpleEntry<>(length - 1, propSource.getJSONObject(length - 1).getJSONObject(SOURCE));
  }

  /**
   * This method updates the source JSON object with the oAuth providers details.
   *
   * @param sourceJSON The source JSON object.
   */
  private void updateSourceWithOAuthProviders(JSONObject sourceJSON, List<OAuthProviderConfigInjector> allInjectors) {
    OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
        .setFilterOnReadableOrganization(false)
        .setFilterOnReadableClients(false)
        .list().forEach(provider -> updateSourceWithOAuthProvider(sourceJSON, provider, allInjectors));
  }

  /**
   * This method updates the source JSON object with the details of a single oAuth provider.
   *
   * @param sourceJSON The source JSON object.
   * @param provider The oAuth provider.
   */
  private void updateSourceWithOAuthProvider(JSONObject sourceJSON, ETRXoAuthProvider provider,
      List<OAuthProviderConfigInjector> allInjectors) {
    try {
      String providerName = provider.getValue();
      String apiUrl = provider.getOAuthAPIURL();
      final String providerRegistration = SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName;
      final String providerProv = SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName;
      sourceJSON.put(providerName + "-api", apiUrl);
      Map<String, String> values = new HashMap<>();
      values.put(providerRegistration + ".provider", providerName);
      values.put(providerRegistration + ".client-id", provider.getIDForClient());
      values.put(providerRegistration + ".client-secret", provider.getClientSecret());
      values.put(providerRegistration + ".scope", provider.getScope());
      values.put(providerRegistration + ".client-name", provider.getClientName());
      values.put(providerRegistration + ".authorization-grant-type", provider.getAuthorizationGrantType());
      values.put(providerRegistration + ".redirect-uri", provider.getRedirectURI());
      values.put(providerRegistration + ".code_challenge_method", provider.getCodeChallengeMethod());
      values.put(providerRegistration + ".client-authentication-method", provider.getClientAuthenticationMethod());
      values.put(providerRegistration + ".token-uri", provider.getTokenURI());
      values.put(providerProv + ".authorization-uri", provider.getAuthorizationURI());
      values.put(providerProv + ".jwk-set-uri", provider.getJwkseturi());
      values.put(providerProv + ".token-uri", provider.getTokenURI());
      values.put(providerProv + ".user-info-uri", provider.getUserInfoURI());
      values.put(providerProv + ".user-name-attribute", provider.getUserNameAttribute());

      values.forEach((key, value) -> {
        if (value != null) {
          try {
            sourceJSON.put(key, value);
          } catch (JSONException e) {
            throw new OBException("Error inserting Data on PropertySource JSON: " ,e);
          }
        }
      });

      for (OAuthProviderConfigInjector injector : allInjectors) {
        injector.injectConfig(sourceJSON, provider);
      }
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
  private void sendResponse(HttpServletResponse response, JSONObject result, JSONObject sourceJSON, Integer indexFound)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try (Writer w = response.getWriter()) {
      sourceJSON.put(MANAGEMENT_ENDPOINT_RESTART_ENABLED, true);
      result.getJSONArray("propertySources").getJSONObject(indexFound).put(SOURCE, sourceJSON);
      w.write(result.toString());
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  /**
   * This method sends an error response in JSON format.
   *
   * @param response The HttpServletResponse object.
   * @param errorMessage The error message to be sent to the client.
   */
  private void sendErrorResponse(HttpServletResponse response, String errorMessage) {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);  // HTTP status code 500
    JSONObject errorResponse = new JSONObject();
    try (Writer writer = response.getWriter()) {
      errorResponse.put("error", true);
      errorResponse.put("message", errorMessage);
      writer.write(errorResponse.toString());
    } catch (IOException | JSONException ex) {
      log.error(String.format("Error sending the error response: %s", ex.getMessage()), ex);
    }
  }
}
