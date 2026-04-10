package com.etendoerp.etendorx;

/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

// Standard Java imports

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.etendorx.data.ConfigServiceParam;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjector;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjectorRegistry;
import com.etendoerp.etendorx.utils.RXConfigUtils;
import com.smf.securewebservices.SWSConfig;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * This class is the base class for the build configuration servlets. It provides the basic
 * functionality for retrieving the default configuration and updating it with the OAuth providers
 * details.
 */
public class BuildConfig extends HttpBaseServlet {

  // OAuth Configuration Constants
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION = "spring.security.oauth2.client.registration.";
  public static final String SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER = "spring.security.oauth2.client.provider.";
  private static final Logger log = LogManager.getLogger();
  // Service Names
  private static final String AUTH_SERVICE = "auth";
  private static final String CONFIG_SERVICE = "config";
  private static final String APPLICATION = "application";

  // Security & Keys
  private static final String ES256_ALGORITHM = "ES256";
  private static final String PRIVATE_KEY = "private-key";
  private static final String PUBLIC_KEY = "public-key";
  private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
  private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

  // User IDs
  private static final String SYS_USER_ID = "0";
  private static final String ADMIN_USER_ID = "100";

  // Configuration Keys
  private static final String MANAGEMENT_ENDPOINT_RESTART_ENABLED = "management.endpoint.restart.enabled";
  private static final String SOURCE = "source";
  private static final String TOKEN = "token";
  private static final String ADMIN_TOKEN = "admin.token";

  // Message Keys
  private static final String ENCRYPTION_ALGORITHM_PREF = "SMFSWS_EncryptionAlgorithm";
  private static final String ETENDO_MIDDLEWARE = "EtendoMiddleware";
  private static final String API_SUFFIX = "-api";

  /**
   * This method extracts the URI from the given HTTP request.
   *
   * @param request
   *     The HTTP request from which the URI is to be extracted.
   * @return The URI as a string. It is the part of the request URL that comes after the servlet path.
   */
  private static String getURIFromRequest(HttpServletRequest request) {
    return request.getRequestURL().toString().split(request.getServletPath())[1];
  }

  // ========== Helper Methods for doGet ==========

  /**
   * Handles the GET request by fetching configuration, updating with OAuth providers,
   * and sending the response.
   *
   * @param request
   *     The HttpServletRequest object.
   * @param response
   *     The HttpServletResponse object.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      OBContext.setAdminMode();

      // Validate SWS configuration and get instance
      SWSConfig swsConfig = validateSWSConfiguration();

      // Process request and get service configuration
      final String serviceURI = getURIFromRequest(request);
      final String serviceName = determineServiceName(serviceURI);
      final JSONObject defaultConfig = getDefaultConfigToJsonObject(serviceURI);

      // Find and configure source
      SimpleEntry<Integer, JSONObject> sourceEntry = findSource(defaultConfig.getJSONArray("propertySources"),
          serviceName);
      ETRXConfig rxConfig = getRXConfigForService(serviceName);
      JSONObject sourceJSON = sourceEntry.getValue();
      addRXParams(sourceJSON, rxConfig);

      // Apply OAuth provider configurations
      List<OAuthProviderConfigInjector> allInjectors = OAuthProviderConfigInjectorRegistry.getInjectors();
      applyOAuthConfigurations(defaultConfig, allInjectors);

      // Validate encryption algorithm
      validateEncryptionAlgorithm();

      // Configure security settings
      JSONObject keys = new JSONObject(swsConfig.getPrivateKey());

      if (StringUtils.equals(AUTH_SERVICE, serviceName)) {
        configureAuthService(sourceJSON, keys, allInjectors);
      }

      // Add public key (required for all services)
      addPublicKey(sourceJSON, keys);

      // Send response
      sendResponse(response, defaultConfig, sourceJSON, sourceEntry.getKey());

    } catch (Exception e) {
      log.error("Error processing build configuration request", e);
      sendErrorResponse(response, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Validates SWS configuration and returns the instance.
   */
  private SWSConfig validateSWSConfiguration() {
    SWSConfig swsConfig = SWSConfig.getInstance();
    if (swsConfig == null || swsConfig.getPrivateKey() == null) {
      throw new OBException(getLocalizedMessage("SMFSWS_Misconfigured"));
    }
    return swsConfig;
  }

  /**
   * Determines the service name from the URI.
   */
  private String determineServiceName(String serviceURI) {
    String[] uriParts = serviceURI.split("/");
    return StringUtils.equals(APPLICATION, uriParts[1]) ? CONFIG_SERVICE : uriParts[1];
  }

  /**
   * Gets RX configuration for the specified service.
   */
  private ETRXConfig getRXConfigForService(String serviceName) {
    ETRXConfig rxConfig = RXConfigUtils.getRXConfig(serviceName);
    if (rxConfig == null) {
      String errorMessage = getLocalizedMessage("ETRX_NoConfigFound");
      throw new OBException(String.format(errorMessage, serviceName));
    }
    return rxConfig;
  }

  /**
   * Applies OAuth provider configurations.
   */
  private void applyOAuthConfigurations(JSONObject defaultConfig,
      List<OAuthProviderConfigInjector> allInjectors) throws JSONException {
    for (OAuthProviderConfigInjector injector : allInjectors) {
      injector.injectConfig(defaultConfig);
    }
  }

  /**
   * Validates the encryption algorithm.
   */
  private void validateEncryptionAlgorithm() throws PropertyException {
    final String algorithmPref = getAlgorithmPref(ENCRYPTION_ALGORITHM_PREF);
    if (!StringUtils.equals(ES256_ALGORITHM, algorithmPref)) {
      String errorMessage = getLocalizedMessage("ETRX_WrongAlgorithm") + algorithmPref;
      throw new UnsupportedOperationException(errorMessage);
    }
  }

  /**
   * Configures authentication service specific settings.
   */
  private void configureAuthService(JSONObject sourceJSON, JSONObject keys,
      List<OAuthProviderConfigInjector> allInjectors) throws Exception {
    // Generate tokens for system and admin users
    User sysUser = OBDal.getInstance().get(User.class, SYS_USER_ID);
    String sysToken = SecureWebServicesUtils.generateToken(sysUser);

    User adminUser = OBDal.getInstance().get(User.class, ADMIN_USER_ID);
    String adminToken = SecureWebServicesUtils.generateToken(adminUser);

    // Add tokens and private key to configuration
    sourceJSON.put(TOKEN, sysToken);
    sourceJSON.put(ADMIN_TOKEN, adminToken);
    sourceJSON.put(PRIVATE_KEY, keys.getString(PRIVATE_KEY));

    // Update with OAuth providers
    updateSourceWithOAuthProviders(sourceJSON, allInjectors);
  }

  /**
   * Adds the public key to the configuration.
   */
  private void addPublicKey(JSONObject sourceJSON, JSONObject keys) throws JSONException {
    String publicKey = keys.getString(PUBLIC_KEY)
        .replace(BEGIN_PUBLIC_KEY, "")
        .replace(END_PUBLIC_KEY, "");
    sourceJSON.put(PUBLIC_KEY, publicKey);
  }

  // ========== Core Business Logic Methods ==========

  /**
   * Gets a localized message from the database.
   */
  private String getLocalizedMessage(String messageKey) {
    return Utility.messageBD(new DalConnectionProvider(), messageKey,
        OBContext.getOBContext().getLanguage().getLanguage());
  }

  /**
   * This method retrieves the algorithm preference from the database.
   *
   * @param preferenceString
   *     The preference string to be retrieved.
   * @return The algorithm preference as a string.
   * @throws PropertyException
   *     If there is an error retrieving the preference.
   */
  protected String getAlgorithmPref(String preferenceString) throws PropertyException {
    return Preferences.getPreferenceValue(preferenceString, true,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(),
        OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(),
        null);
  }

  /**
   * This method adds the RX parameters to the services JSON Object for configs.
   *
   * @param sourceJSON
   *     The source JSON object.
   * @param rxConfig
   *     The RX configuration.
   */
  private void addRXParams(JSONObject sourceJSON, ETRXConfig rxConfig) {
    try {
      for (ConfigServiceParam param : rxConfig.getETRXServiceParamList()) {
        sourceJSON.put(param.getParameterKey(), param.getParameterValue());
      }
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  /**
   * This method gets the default configuration from the ETRXConfig entity and converts it to a JSON object.
   *
   * @return The default configuration as a JSON object.
   * @throws JSONException
   *     If there is an error parsing the JSON.
   */
  protected JSONObject getDefaultConfigToJsonObject(String serviceURI) throws JSONException, IOException {
    ETRXConfig rxConfig = RXConfigUtils.getRXConfig(CONFIG_SERVICE);
    if (rxConfig == null) {
      throw new OBException(String.format(getLocalizedMessage("ETRX_NoConfigFound"), CONFIG_SERVICE));
    }
    HttpURLConnection conn = (HttpURLConnection) URI.create(
        rxConfig.getServiceURL() + serviceURI).toURL().openConnection();
    try {
      conn.setRequestMethod("GET");
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
        return new JSONObject(reader.lines().collect(Collectors.joining()));
      }
    } finally {
      conn.disconnect();
    }
  }

  /**
   * This method finds the source JSON object in the property sources array.
   * If no default config is found for the specific service, it retrieves the application default config.
   *
   * @param propSource
   *     The property sources array.
   * @param service
   *     The service name.
   * @return The index and the source JSON object.
   * @throws JSONException
   *     If there is an error parsing the JSON.
   */
  private SimpleEntry<Integer, JSONObject> findSource(JSONArray propSource, String service) throws JSONException {
    final int length = propSource.length();
    for (int i = 0; i < length; i++) {
      JSONObject entry = propSource.getJSONObject(i);
      if (entry.getString("name").contains(service)) {
        return new SimpleEntry<>(i, entry.getJSONObject(SOURCE));
      }
    }
    // No service-specific config found; fall back to the application default at the last index
    return new SimpleEntry<>(length - 1, propSource.getJSONObject(length - 1).getJSONObject(SOURCE));
  }

  /**
   * This method updates the source JSON object with the oAuth providers details.
   *
   * @param sourceJSON
   *     The source JSON object.
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
   * @param sourceJSON
   *     The source JSON object.
   * @param provider
   *     The oAuth provider.
   */
  private void updateSourceWithOAuthProvider(JSONObject sourceJSON, ETRXoAuthProvider provider,
      List<OAuthProviderConfigInjector> allInjectors) {
    try {
      // Skip EtendoMiddleware provider
      if (StringUtils.equals(ETENDO_MIDDLEWARE, provider.getValue())) {
        return;
      }

      String providerName = provider.getValue();

      // Add API URL if available
      String apiUrl = provider.getOAuthAPIURL();
      if (apiUrl != null) {
        sourceJSON.put(providerName + API_SUFFIX, apiUrl);
      }

      // Configure OAuth registration and provider settings
      configureOAuthRegistration(sourceJSON, provider, providerName);
      configureOAuthProvider(sourceJSON, provider, providerName);

      // Apply custom injectors
      for (OAuthProviderConfigInjector injector : allInjectors) {
        injector.injectConfig(sourceJSON, provider);
      }

    } catch (JSONException e) {
      log.error("Error updating source with OAuth provider: {}", provider.getValue(), e);
      throw new OBException("Failed to configure OAuth provider: " + provider.getValue(), e);
    }
  }

  /**
   * Configures OAuth registration settings.
   */
  private void configureOAuthRegistration(JSONObject sourceJSON, ETRXoAuthProvider provider,
      String providerName) throws JSONException {
    final String p = SPRING_SECURITY_OAUTH_2_CLIENT_REGISTRATION + providerName;
    putIfNotNull(sourceJSON, p + ".provider", providerName);
    putIfNotNull(sourceJSON, p + ".client-id", provider.getIDForClient());
    putIfNotNull(sourceJSON, p + ".client-secret", provider.getClientSecret());
    putIfNotNull(sourceJSON, p + ".scope", provider.getScope());
    putIfNotNull(sourceJSON, p + ".client-name", provider.getClientName());
    putIfNotNull(sourceJSON, p + ".authorization-grant-type", provider.getAuthorizationGrantType());
    putIfNotNull(sourceJSON, p + ".redirect-uri", provider.getRedirectURI());
    putIfNotNull(sourceJSON, p + ".code_challenge_method", provider.getCodeChallengeMethod());
    putIfNotNull(sourceJSON, p + ".client-authentication-method", provider.getClientAuthenticationMethod());
    putIfNotNull(sourceJSON, p + ".token-uri", provider.getTokenURI());
  }

  /**
   * Configures OAuth provider settings.
   */
  private void configureOAuthProvider(JSONObject sourceJSON, ETRXoAuthProvider provider,
      String providerName) throws JSONException {
    final String p = SPRING_SECURITY_OAUTH_2_CLIENT_PROVIDER + providerName;
    putIfNotNull(sourceJSON, p + ".authorization-uri", provider.getAuthorizationURI());
    putIfNotNull(sourceJSON, p + ".jwk-set-uri", provider.getJWKSetURI());
    putIfNotNull(sourceJSON, p + ".token-uri", provider.getTokenURI());
    putIfNotNull(sourceJSON, p + ".user-info-uri", provider.getUserInfoURI());
    putIfNotNull(sourceJSON, p + ".user-name-attribute", provider.getUserNameAttribute());
  }

  /**
   * Puts a value into the JSON object only if the value is not null.
   */
  private static void putIfNotNull(JSONObject json, String key, String value) throws JSONException {
    if (value != null) {
      json.put(key, value);
    }
  }

  /**
   * This method sends the response to the client.
   *
   * @param response
   *     The HTTP response.
   * @param result
   *     The result JSON object.
   * @param sourceJSON
   *     The source JSON object.
   * @param indexFound
   *     The index of the source JSON object in the property sources array.
   * @throws IOException
   *     If there is an error writing the response.
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
   * @param response
   *     The HttpServletResponse object.
   * @param errorMessage
   *     The error message to be sent to the client.
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
      log.error("Error sending the error response: {}", ex.getMessage(), ex);
    }
  }
}
