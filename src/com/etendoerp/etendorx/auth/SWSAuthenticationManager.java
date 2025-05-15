package com.etendoerp.etendorx.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendoerp.etendorx.data.ETRXTokenUser;
import com.etendoerp.etendorx.utils.TokenVerifier;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesHistory;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.service.web.BaseWebServiceServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The default servlet which catches all requests for a webservice. This servlet finds the WebService
 * instance implementing the requested service by calling the {@link OBProvider} with the top segment
 * in the path. When the WebService implementation is found the request is forwarded to that service.
 */
public class SWSAuthenticationManager extends DefaultAuthenticationManager {

  private static final Logger log4j = LogManager.getLogger();
  private static final String ACCESS_TOKEN = "access_token";
  private static final String SWS_TOKEN_IS_NOT_VALID = "SWS - Token is not valid";
  private static final String SSO_DOMAIN_URL = "sso.domain.url";

  /**
   * Default constructor.
   */
  public SWSAuthenticationManager() {
    super();
  }

  /**
   * This method is called to authenticate the user based on the provided request.
   * It checks if the request contains a valid token and retrieves the user information from it.
   *
   * @param request the HttpServletRequest object containing the request information
   * @return the user ID of the authenticated user
   */
  @Override
  protected String doWebServiceAuthenticate(HttpServletRequest request) {

    String authStr = request.getHeader("Authorization");
    String token = null;
    if (StringUtils.startsWith(authStr, "Bearer ")) {
      token = StringUtils.substring(authStr, 7);
    }
    if (token != null) {
      try {
        log4j.debug(" Decoding token {}", token);
        DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
        if (decodedToken != null) {
          String userId = decodedToken.getClaim("user").asString();
          String roleId = decodedToken.getClaim("role").asString();
          String orgId = decodedToken.getClaim("organization").asString();
          String warehouseId = decodedToken.getClaim("warehouse").asString();
          String clientId = decodedToken.getClaim("client").asString();
          if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(roleId) || StringUtils.isEmpty(
              orgId) || StringUtils.isEmpty(warehouseId) || StringUtils.isEmpty(clientId)) {
            throw new OBException(SWS_TOKEN_IS_NOT_VALID);
          }
          log4j.debug("SWS accessed by userId {}", userId);
          OBContext.setOBContext(
              SecureWebServicesUtils.createContext(userId, roleId, orgId, warehouseId, clientId));
          OBContext.setOBContextInSession(request, OBContext.getOBContext());
          SessionInfo.setUserId(userId);
          SessionInfo.setProcessType("WS");
          SessionInfo.setProcessId("DAL");
          try {
            OBContext.setAdminMode();
            return userId;
          } finally {
            OBContext.restorePreviousMode();
          }
        }
      } catch (Exception e) {
        throw new OBException(e);
      }
    }
    return super.doWebServiceAuthenticate(request);
  }

  /**
   * Sets the CORS headers for the response.
   *
   * @param request  the HttpServletRequest object
   * @param response the HttpServletResponse object
   * @throws ServletException if an error occurs during servlet processing
   * @throws IOException      if an I/O error occurs
   */
  protected void setCORSHeaders(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String origin = request.getHeader("Origin");

    if (!StringUtils.isBlank(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      response.setHeader("Access-Control-Allow-Credentials", "true");
      response.setHeader("Access-Control-Allow-Headers",
          "Content-Type, origin, accept, X-Requested-With");
      response.setHeader("Access-Control-Max-Age", "1000");
    }
  }

/**
   * Decodes the provided token using the specified secret and issuer.
   *
   * @param token  the JWT token to be decoded
   * @param secret the secret key used for decoding
   * @param issuer the issuer of the token
   * @return a DecodedJWT object containing the decoded token information
   */
  private static DecodedJWT decodeToken(String token, String secret, String issuer) throws UnsupportedEncodingException {
    Algorithm algorithm = Algorithm.HMAC256(secret);
    JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
    return verifier.verify(token);   }

  /**
   * This method is called to authenticate the user based on the provided request and response.
   * It checks if the request contains a valid token and retrieves the user information from it.
   *
   * @param request  the HttpServletRequest object containing the request information
   * @param response the HttpServletResponse object to send the response
   * @return the user ID of the authenticated user
   * @throws AuthenticationException if an error occurs during authentication
   * @throws ServletException        if an error occurs during servlet processing
   * @throws IOException             if an I/O error occurs
   */
  @Override
  protected String doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {
    try {
      final String allowSSO = getAllowSSOPref();
      final String hasSSOType = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.auth.type");
      if (misconfiguredSSO(request, allowSSO, hasSSOType)) {
        final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String logoutRedirectUri = StringUtils.remove(request.getRequestURL().toString(),
                request.getServletPath()).trim();
        String contextName = ((String) openbravoProperties.get("context.name")).trim();
        String title = URLEncoder.encode("SSO Configuration Error", StandardCharsets.UTF_8);
        String description = URLEncoder.encode(
                "There is a misconfiguration in the SSO setup. Please contact your administrator.",
                StandardCharsets.UTF_8);
        String errorUrl = String.format("/%s/web/com.etendoerp.entendorx/resources/Auth0ErrorPage.html"
                        + "?logoutRedirectUri=%s&title=%s&description=%s",
                contextName,
                URLEncoder.encode(logoutRedirectUri, StandardCharsets.UTF_8),
                title,
                description);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", errorUrl);
        response.flushBuffer();
        throw new OBException("SSO - Configuration mismatch detected.");
      }
      if ((!StringUtils.isBlank(request.getParameter("code")) ||
          !StringUtils.isBlank(request.getParameter(ACCESS_TOKEN))) &&
          StringUtils.equals("Y", allowSSO)) {
        log4j.debug("SSO Code to request token: {}", request.getParameter("code"));
        log4j.debug("SSO Token coming from the request: {}", request.getParameter(ACCESS_TOKEN));

        String token = StringUtils.isBlank(request.getParameter(ACCESS_TOKEN)) ?
            getAuthToken(request) : request.getParameter(ACCESS_TOKEN);

        HashMap<String, String> tokenValues = decodeToken(token);
        User adUser = matchUser(token, tokenValues.get("sub"));
        if (adUser == null) {
          handleWhenUserIsNull(request, response);
        }

        markRequestAsSelfAuthenticated(request);
        prepareLoginSession(request, adUser);
        response.sendRedirect("/" + OBPropertiesProvider.getInstance().getOpenbravoProperties().get("context.name"));
        return adUser.getId();
      }
    } catch (Exception e) {
      log4j.error("Error while authenticating: {}", e.getMessage(), e);
      throw new OBException(e);
    }

    String token = request.getParameter(ACCESS_TOKEN);
    if (StringUtils.isEmpty(token)) {
      String authStr = request.getHeader("Authorization");

      if (authStr != null && StringUtils.startsWith(authStr, "Bearer ")) {
        return doWebServiceAuthenticate(request);
      } else {
        return super.doAuthenticate(request, response);
      }
    }
    String receivedUser = request.getParameter("user");

    setCORSHeaders(request, response);

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    final String secret = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("OAUTH2_SECRET");
    final String issuer = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("OAUTH2_ISSUER");
    TokenVerifier.isValid(token, secret);
    final Map<String, Object> userMetadata = getUserMetadata(token, secret, issuer);

    String userId;
    try {
      OBContext.setAdminMode(true);
      User adUser = (User) OBDal.getInstance()
          .createCriteria(User.class)
          .add(Restrictions.eq(User.PROPERTY_USERNAME, receivedUser))
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .setMaxResults(1)
          .uniqueResult();
      if (adUser != null) {
        userId = adUser.getId();
        if (request.getSession(false) == null && AuthenticationManager.isStatelessRequest(
            request)) {
          return webServiceAuthenticate(request);
        }
        markRequestAsSelfAuthenticated(request);
        String user;
        user = vars.getStringParameter(LOGIN_PARAM);
        if (StringUtils.isEmpty(user)) {
          user = vars.getStringParameter(BaseWebServiceServlet.LOGIN_PARAM);
        }
        loginName.set(user);
        final String sessionId = createDBSession(request, user, userId);
        vars.setSessionValue("#AD_User_ID", userId);

        request.getSession(true).setAttribute("#Authenticated_user", userId);

        vars.setSessionValue("#AD_SESSION_ID", sessionId);
        vars.setSessionValue("#LogginIn", "Y");

        adUser.setFirstName((String) userMetadata.get("name"));
        OBDal.getInstance().save(adUser);
        OBDal.getInstance().flush();
        return userId;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return super.doAuthenticate(request, response);
  }

    /**
     * Checks if the SSO configuration is misconfigured based on the provided parameters.
     *
     * @param request     the HttpServletRequest object
     * @param allowSSO    the preference value for allowing SSO login
     * @param hasSSOType  the SSO type
     * @return true if the configuration is misconfigured, false otherwise
     */
  private static boolean misconfiguredSSO(HttpServletRequest request, String allowSSO, String hasSSOType) {
    return !StringUtils.isBlank(request.getParameter("access_token")) &&
            ((StringUtils.equals("Y", allowSSO) && StringUtils.isBlank(hasSSOType)) ||
                    (StringUtils.equals("N", allowSSO) && !StringUtils.isBlank(hasSSOType)));
  }

  /**
   * Retrieves the preference value for allowing SSO login.
   *
   * @return the preference value as a String
   */
  private static String getAllowSSOPref() {
    Preference allowSSOPref = (Preference) OBDal.getInstance()
        .createCriteria(Preference.class)
        .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, "ETRX_AllowSSOLogin"))
        .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setMaxResults(1).uniqueResult();
    return allowSSOPref != null ? allowSSOPref.getSearchKey() : "N";
  }

  /**
   * Retrieves the user metadata from the provided token.
   *
   * @param token  the JWT token
   * @param secret the secret key used for decoding
   * @param issuer the issuer of the token
   * @return a Map containing the user metadata
   */
  private static Map<String, Object> getUserMetadata(String token, String secret, String issuer) {
    Map<String, Object> userMetadata = null;
    try {
      DecodedJWT decodedToken = decodeToken(token, secret, issuer);
      Object userMeta = decodedToken.getClaim("user_metadata").as(Object.class);
      if(userMeta instanceof Map) {
        userMetadata = (Map<String, Object>) userMeta;
      }
      if(userMetadata == null) {
        throw new OBException(SWS_TOKEN_IS_NOT_VALID);
      }
    } catch (Exception e) {
      throw new OBException(SWS_TOKEN_IS_NOT_VALID);
    }
    return userMetadata;
  }

  /**
   * Handles the case when the user is null after authentication.
   *
   * @param request  the HttpServletRequest object
   * @param response the HttpServletResponse object
   * @throws IOException if an I/O error occurs
   */
  private static void handleWhenUserIsNull(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String ssoDomain = ((String) openbravoProperties.get(SSO_DOMAIN_URL)).trim();
    String clientId = ((String) openbravoProperties.get("sso.client.id")).trim();
    String logoutRedirectUri = StringUtils.remove(request.getRequestURL().toString(),
        request.getServletPath()).trim();
    String contextName = ((String) openbravoProperties.get("context.name")).trim();
    String errorTitle = URLEncoder.encode("No User linked", StandardCharsets.UTF_8);
    String errorDescription = URLEncoder.encode("You need to log in with an ERP user and then, link the SSO account", StandardCharsets.UTF_8);
    String ssoNoUserLinkURL = String.format("/%s/web/com.etendoerp.entendorx/resources/Auth0ErrorPage.html"
                    + "?ssoDomain=%s&clientId=%s&logoutRedirectUri=%s&title=%s&description=%s",
            contextName,
            URLEncoder.encode(ssoDomain, StandardCharsets.UTF_8),
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(logoutRedirectUri, StandardCharsets.UTF_8),
            errorTitle,
            errorDescription);
    response.setStatus(HttpServletResponse.SC_FOUND);
    response.setHeader("Location", ssoNoUserLinkURL);
    response.flushBuffer();
    throw new OBException("SSO - No user link to SSO Account.");
  }

  /**
   * Matches the user based on the provided token and subject.
   *
   * @param token the authentication token
   * @param sub   the subject identifier from the token
   * @return the matched User object, or null if no match is found
   */
  private User matchUser(String token, String sub) {
    try {
      OBContext.setAdminMode(true);
      ETRXTokenUser tokenUser = getTokenUser(sub);
      if (tokenUser != null) {
        tokenUser.setOAuthToken(token);
      } else {
        return null;
      }
      return tokenUser.getUserForToken();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ETRXTokenUser getTokenUser(String sub) {
    return (ETRXTokenUser) OBDal.getInstance().createCriteria(ETRXTokenUser.class)
        .add(Restrictions.eq(ETRXTokenUser.PROPERTY_SUB, sub))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setMaxResults(1).uniqueResult();
  }

  /**
   * Decodes the provided token and extracts its claims.
   *
   * @param token the authentication token
   * @return a HashMap containing the token claims
   */
  private HashMap<String, String> decodeToken(String token) {

    HashMap<String, String> tokenValues = new HashMap<>();
    DecodedJWT decodedJWT = JWT.decode(token);

    tokenValues.put("given_name", decodedJWT.getClaim("given_name").asString());
    tokenValues.put("family_name", decodedJWT.getClaim("family_name").asString());
    tokenValues.put("email", decodedJWT.getClaim("email").asString());
    tokenValues.put("sid", decodedJWT.getClaim("sid").asString());
    tokenValues.put("sub", decodedJWT.getClaim("sub").asString());
    return tokenValues;
  }

  /**
   * Retrieves the authentication token from the request.
   *
   * @param request the HttpServletRequest object
   * @return the authentication token, or null if the token could not be retrieved
   */
  private String getAuthToken(HttpServletRequest request) {
    String code = request.getParameter("code");
    String token = "";
    String domain = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(SSO_DOMAIN_URL);
    String tokenEndpoint = "https://" + domain + "/oauth/token";
    try {
      URL url = new URL(tokenEndpoint);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      con.setDoOutput(true);

      String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.id");
      String clientSecret = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.secret");

      String codeVerifier = (String) request.getSession().getAttribute("code_verifier");
      boolean isPKCE = (codeVerifier != null && !codeVerifier.isEmpty());
      String strDirection = request.getScheme() + "://" + request.getServerName() + ":8080" + request.getContextPath() + "/secureApp/LoginHandler.html";
      String params;
      params = getParams(isPKCE, clientId, code, strDirection, codeVerifier, clientSecret);

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      } catch (Exception e) {
        log4j.error(e.getMessage(), e);
      }

      int status = con.getResponseCode();
      if (status == 200) {
        try (InputStream in = con.getInputStream()) {
          String responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          JSONObject jsonResponse = new JSONObject(responseBody);
          token = jsonResponse.getString("id_token");
        }
      } else {
        log4j.error(con.getResponseMessage());
        token = null;
        throw new OBException("Error trying to login - Error code:" + status + " - " + con.getResponseMessage());
      }
    } catch (JSONException | IOException e) {
      log4j.error(e);
    }
    return token;
  }

  /**
   * Constructs the parameters for the token request based on the provided information.
   *
   * @param isPKCE        indicates if PKCE is used
   * @param clientId      the client ID
   * @param code          the authorization code
   * @param strDirection  the redirect URI
   * @param codeVerifier  the code verifier (if PKCE is used)
   * @param clientSecret  the client secret (if not using PKCE)
   * @return a formatted string containing the parameters for the token request
   */
  private static String getParams(boolean isPKCE, String clientId, String code, String strDirection,
      String codeVerifier, String clientSecret) {
    String params;
    if (isPKCE) {
      params = String.format(
          "grant_type=authorization_code&client_id=%s&code=%s&redirect_uri=%s&code_verifier=%s",
          URLEncoder.encode(clientId, StandardCharsets.UTF_8),
          URLEncoder.encode(code, StandardCharsets.UTF_8),
          URLEncoder.encode(strDirection, StandardCharsets.UTF_8),
          URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
      );
    } else {
      params = String.format(
          "grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
          URLEncoder.encode(clientId, StandardCharsets.UTF_8),
          URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
          URLEncoder.encode(code, StandardCharsets.UTF_8),
          URLEncoder.encode(strDirection, StandardCharsets.UTF_8)
      );
    }
    return params;
  }

  /**
   * Prepares the login session for the user.
   *
   * @param request the HttpServletRequest object
   * @param user    the User object representing the authenticated user
   */
  private void prepareLoginSession(HttpServletRequest request, User user) {
    loginName.set(user.getName());
    final String sessionId = createDBSession(request, user.getUsername(), user.getId());

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    vars.setSessionValue("#AD_User_ID", user.getId());
    request.getSession(true).setAttribute("#Authenticated_user", user.getId());
    vars.setSessionValue("#AD_SESSION_ID", sessionId);
    vars.setSessionValue("#LogginIn", "Y");
    VariablesHistory variables = new VariablesHistory(request);
    String strTarget = request.getRequestURL().toString();
    String qString = request.getQueryString();
    String strDirectionLocal = HttpBaseUtils.getLocalAddress(request);

    if (!strTarget.endsWith("/security/Menu.html")) {
      variables.setSessionValue("targetmenu", strTarget);
    }

    // Storing target string to redirect after a successful login
    variables.setSessionValue("target",
        strDirectionLocal + "/" + (!StringUtils.isBlank(qString) ? "?" + qString : ""));
  }
}

