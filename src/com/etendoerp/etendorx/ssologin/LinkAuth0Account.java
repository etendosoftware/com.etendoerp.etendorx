package com.etendoerp.etendorx.ssologin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.etendoerp.etendorx.auth.JwkRSAKeyProvider;
import com.etendoerp.etendorx.auth.SWSAuthenticationManager;
import com.etendoerp.etendorx.data.ETRXTokenUser;
import com.etendoerp.etendorx.utils.TokenEncryptionUtil;

/**
 * This class handles the linking of Auth0 accounts with the application.
 */
public class LinkAuth0Account extends HttpBaseServlet {

  public static final String MIDDLEWARE = "Middleware";
  public static final String ETENDO = "etendo";

  /**
   * Handles GET requests by delegating to the doPost method.
   *
   * @param request  the HttpServletRequest object
   * @param response the HttpServletResponse object
   * @throws IOException      if an input or output error is detected
   * @throws ServletException if the request could not be handled
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  /**
   * Handles POST requests to link Auth0 accounts.
   *
   * @param req the HttpServletRequest object
   * @param res the HttpServletResponse object
   * @throws IOException      if an input or output error is detected
   * @throws ServletException if the request could not be handled
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    String token = getAuthToken(req);
    try {
      verifyToken(token);
    } catch (OBException e) {
      log4j.error("[LinkAuth0Account] Aborting account linking due to invalid token", e);
      res.sendRedirect("/" + OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(SWSAuthenticationManager.CONTEXT_NAME, ETENDO));
      return;
    }
    HashMap<String, String> tokenValues = decodeToken(token);
    matchUser(token, tokenValues.get("sub"));
    res.sendRedirect("/" + OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(SWSAuthenticationManager.CONTEXT_NAME, ETENDO));
  }

  /**
   * Verifies the JWT signature against the JWKS exposed by the middleware or Auth0.
   * Throws OBException if the token is invalid, expired, or has a bad signature.
   *
   * @param token the JWT to verify
   */
  private void verifyToken(String token) {
    try {
      Properties obProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      String authType = obProperties.getProperty(SWSAuthenticationManager.SSO_AUTH_TYPE);
      String middlewareUrl = obProperties.getProperty("sso.middleware.url", "");
      String domainUrl = obProperties.getProperty("sso.domain.url", "");
      if (StringUtils.equals(MIDDLEWARE, authType) && StringUtils.isBlank(middlewareUrl)) {
        throw new OBException("[LinkAuth0Account] Property 'sso.middleware.url' is not configured");
      }
      if (!StringUtils.equals(MIDDLEWARE, authType) && StringUtils.isBlank(domainUrl)) {
        throw new OBException("[LinkAuth0Account] Property 'sso.domain.url' is not configured");
      }
      String baseURL = StringUtils.equals(MIDDLEWARE, authType)
          ? middlewareUrl
          : "https://" + domainUrl;

      URL jwkURL = new URL(baseURL + "/.well-known/jwks.json");
      JwkProvider jwkProvider = new UrlJwkProvider(jwkURL);

      DecodedJWT jwt = JWT.decode(token);
      RSAKeyProvider keyProvider = new JwkRSAKeyProvider(jwkProvider, jwt.getKeyId());
      Algorithm algorithm = Algorithm.RSA256(keyProvider);

      String issuer = StringUtils.equals(MIDDLEWARE, authType)
          ? SWSAuthenticationManager.MIDDLEWARE_ISSUER
          : baseURL + "/";

      JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
      verifier.verify(token);
    } catch (Exception e) {
      log4j.error("[LinkAuth0Account] Token verification failed", e);
      throw new OBException("Token verification failed during account linking", e);
    }
  }

  /**
   * Matches the user based on the provided token and subject.
   *
   * @param token the authentication token
   * @param sub   the subject identifier from the token
   */
  private void matchUser(String token, String sub) {
    try {
      OBContext.setAdminMode(true);
      ETRXTokenUser tokenUser = (ETRXTokenUser) OBDal.getInstance().createCriteria(ETRXTokenUser.class)
          .add(Restrictions.eq(ETRXTokenUser.PROPERTY_SUB, sub))
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .setMaxResults(1).uniqueResult();
      if (tokenUser != null) {
        OBDal.getInstance().remove(tokenUser);
      }
      tokenUser = OBProvider.getInstance().get(ETRXTokenUser.class);
      tokenUser.setSub(sub);
      tokenUser.setOAuthToken(TokenEncryptionUtil.isKeyConfigured() ? TokenEncryptionUtil.encrypt(token) : token);
      String[] provider = StringUtils.split(sub, "|");
      tokenUser.setTokenProvider(provider[0]);
      tokenUser.setUserForToken(OBContext.getOBContext().getUser());
      OBDal.getInstance().save(tokenUser);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error(e);
      throw new OBException("Error al vincular la cuenta de Google con la de Auth0", e);
    } finally {
      OBContext.restorePreviousMode();
    }
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
   * @return the authentication token
   */
  private String getAuthToken(HttpServletRequest request) {
    String authType = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(SWSAuthenticationManager.SSO_AUTH_TYPE);
    return StringUtils.equals("Auth0", authType) ? getTokenFromAuth0(request) : request.getParameter("access_token");
  }

    /**
     * Retrieves the token from Auth0 using the provided request.
     *
     * @param request the HttpServletRequest object
     * @return the authentication token
     */
  private String getTokenFromAuth0(HttpServletRequest request) {
    String code = request.getParameter("code");
    String token = "";
    String domain = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.domain.url");
    String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.id");
    String clientSecret = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.secret");
    String tokenEndpoint = "https://" + domain + "/oauth/token";
    String ssoCallbackURL = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.callback.url");
    try {
      URL url = new URL(tokenEndpoint);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      con.setDoOutput(true);

      String codeVerifier = (String) request.getSession().getAttribute("code_verifier");
      boolean isPKCE = (codeVerifier != null && !codeVerifier.isEmpty());

      String params;
      if (isPKCE) {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&code=%s&redirect_uri=%s&code_verifier=%s",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(ssoCallbackURL, StandardCharsets.UTF_8),
            URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
        );
      } else {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(ssoCallbackURL, StandardCharsets.UTF_8)
        );
      }

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int status = con.getResponseCode();
      if (status == 200) {
        try (InputStream in = con.getInputStream()) {
          String responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          JSONObject jsonResponse = new JSONObject(responseBody);
          token = jsonResponse.getString("id_token");
        }
      } else {
        token = null;
      }
    } catch (JSONException | IOException e) {
      log4j.error(e);
    }
    return token;
  }
}
