package com.etendoerp.etendorx;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servlet to save the token received from the middleware.
 * This servlet is called by the middleware after the user has authenticated and authorized access.
 */
public class SaveTokenFromMiddleware extends HttpBaseServlet {

  public static final String RED = "#f44336";
  public static final String GREEN = "#4caf50";
  public static final String ERROR_ICON = "❌";
  public static final String SUCCESS_ICON = "✔";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      OBContext.setAdminMode();
      ETRXTokenInfo newToken = OBProvider.getInstance().get(ETRXTokenInfo.class);
      newToken.setToken(request.getParameter("access_token"));
      String providerScope = request.getParameter("provider") + " - " + request.getParameter("scope");
      newToken.setMiddlewareProvider(providerScope);
      newToken.setEtrxOauthProvider(getETRXoAuthProvider());
      newToken.setUser(OBContext.getOBContext().getUser());
      OBDal.getInstance().save(newToken);

      String responseURL = getResponseBody(false);
      response.sendRedirect(responseURL);
      response.flushBuffer();
    } catch (OBException | IOException e) {
      log4j.error("Error saving the token from the middleware", e);
      String responseURL = getResponseBody(true);
      response.setHeader("Location", responseURL);
      try {
        response.flushBuffer();
      } catch (IOException ex) {
        log4j.error(ex);
        throw new OBException(ex);
      }
    }
  }

  /**
   * Generates the response URL to be sent back to the middleware after saving the token.
   * The URL contains a message indicating whether the token was saved successfully or if there was an error.
   *
   * @param error true if there was an error, false otherwise
   * @return the response URL
   */
  private static String getResponseBody(boolean error) {
    String contextName = ((String) OBPropertiesProvider.getInstance().getOpenbravoProperties().get("context.name")).trim();
    String tokenCreated = Utility.messageBD(new DalConnectionProvider(), "ETRX_TokenCreated",
        OBContext.getOBContext().getLanguage().getLanguage());
    String description = Utility.messageBD(new DalConnectionProvider(), "ETRX_TokenCreatedDescription",
        OBContext.getOBContext().getLanguage().getLanguage());
    String rawTitle = error ? "" : tokenCreated;
    String rawMessage = error ? "" : description;
    String icon = error ? ERROR_ICON : SUCCESS_ICON;
    String iconColor = error ? RED : GREEN;

    return String.format("/%s/web/com.etendoerp.entendorx/resources/MiddlewareResponse.html"
            + "?title=%s&message=%s&icon=%s&iconColor=%s",
        contextName,
        URLEncoder.encode(rawTitle, StandardCharsets.UTF_8),
        URLEncoder.encode(rawMessage, StandardCharsets.UTF_8),
        URLEncoder.encode(icon, StandardCharsets.UTF_8),
        URLEncoder.encode(iconColor, StandardCharsets.UTF_8)
    );
  }

  /**
   * Retrieves the ETRXoAuthProvider instance for the Etendo Middleware.
   * This method queries the database to find the OAuth provider with the value "EtendoMiddleware".
   *
   * @return the ETRXoAuthProvider instance
   */
  private ETRXoAuthProvider getETRXoAuthProvider() {
    return (ETRXoAuthProvider) OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
        .add(Restrictions.eq(ETRXoAuthProvider.PROPERTY_VALUE, "EtendoMiddleware"))
        .setMaxResults(1).uniqueResult();
  }
}
