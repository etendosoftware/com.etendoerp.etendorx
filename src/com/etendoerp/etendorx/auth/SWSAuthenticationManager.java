package com.etendoerp.etendorx.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationExpirationPasswordException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesHistory;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.BaseWebServiceServlet;
import org.openbravo.service.web.WebService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class SWSAuthenticationManager extends DefaultAuthenticationManager {

  private static final Logger log4j = LogManager.getLogger();

  public SWSAuthenticationManager() {
  }

  @Override
  protected String doWebServiceAuthenticate(HttpServletRequest request) {

    String authStr = request.getHeader("Authorization");
    String token = null;
    if (authStr != null && authStr.startsWith("Bearer ")) {
      token = authStr.substring(7);
    }
    if(token != null) {
      try {
        DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
        if (decodedToken != null) {
          String userId = decodedToken.getClaim("user").asString();
          String roleId = decodedToken.getClaim("role").asString();
          String orgId = decodedToken.getClaim("organization").asString();
          String warehouseId = decodedToken.getClaim("warehouse").asString();
          String clientId = decodedToken.getClaim("client").asString();
          if (userId == null || userId.isEmpty() || roleId == null || roleId.isEmpty() || orgId == null || orgId.isEmpty() || warehouseId == null || warehouseId.isEmpty() || clientId == null || clientId.isEmpty()) {
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
        throw new RuntimeException(e);
      }
    }
    return super.doWebServiceAuthenticate(request);
  }

}
