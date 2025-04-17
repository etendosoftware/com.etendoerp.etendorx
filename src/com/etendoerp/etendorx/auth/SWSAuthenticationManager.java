package com.etendoerp.etendorx.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendoerp.etendorx.utils.TokenVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationExpirationPasswordException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesHistory;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.BaseWebServiceServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * The default servlet which catches all requests for a webservice. This servlet finds the WebService
 * instance implementing the requested service by calling the {@link OBProvider} with the top segment
 * in the path. When the WebService implementation is found the request is forwarded to that service.
 */
public class SWSAuthenticationManager extends DefaultAuthenticationManager {

  private static final Logger log4j = LogManager.getLogger();

  /**
   * Default constructor.
   */
  public SWSAuthenticationManager() {
    super();
  }

  @Override
  protected String doWebServiceAuthenticate(HttpServletRequest request) {

    String authStr = request.getHeader("Authorization");
    String token = null;
    if (StringUtils.startsWith(authStr, "Bearer ")) {
      token = StringUtils.substring(authStr, 7);
    }
    if (token != null) {
      try {
        log4j.debug(" Decoding token " + token);
        DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
        if (decodedToken != null) {
          String userId = decodedToken.getClaim("user").asString();
          String roleId = decodedToken.getClaim("role").asString();
          String orgId = decodedToken.getClaim("organization").asString();
          String warehouseId = decodedToken.getClaim("warehouse").asString();
          String clientId = decodedToken.getClaim("client").asString();
          if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(roleId) || StringUtils.isEmpty(
              orgId) || StringUtils.isEmpty(warehouseId) || StringUtils.isEmpty(clientId)) {
            throw new OBException("SWS - Token is not valid");
          }
          log4j.debug("SWS accessed by userId " + userId);
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

  protected void setCORSHeaders(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String origin = request.getHeader("Origin");

    if (origin != null && !origin.equals("")) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      response.setHeader("Access-Control-Allow-Credentials", "true");
      response.setHeader("Access-Control-Allow-Headers",
          "Content-Type, origin, accept, X-Requested-With");
      response.setHeader("Access-Control-Max-Age", "1000");
    }
  }

  private static DecodedJWT decodeToken(String token, String secret, String issuer) throws Exception {
    Algorithm algorithm = Algorithm.HMAC256(secret);
    JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
    return verifier.verify(token);
  }

  @Override
  protected String doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {

    String token = request.getParameter("access_token");
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
    Map<String, Object> userMetadata = null;
    // get jwt token claim
    try {
      DecodedJWT decodedToken = decodeToken(token, secret, issuer);
      Object userMeta = decodedToken.getClaim("user_metadata").as(Object.class);
      if(userMeta instanceof Map) {
        userMetadata = (Map<String, Object>) userMeta;
      }
      if(userMetadata == null) {
        throw new OBException("SWS - Token is not valid");
      }
    } catch (Exception e) {
      throw new OBException("SWS - Token is not valid");
    }

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
}

