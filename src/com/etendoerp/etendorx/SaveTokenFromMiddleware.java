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
        throw new RuntimeException(ex);
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
    // TODO: Change to DB Message.
    String rawTitle = error ? "" : "Token Created Successfully";
    String rawMessage = error ? "" : "The token has been created successfully.<br>Refresh the grid to see the changes.";
    String icon = error ? "❌" : "✔";
    String iconColor = error ? "#f44336" : "#4caf50";

    String responseURL = String.format("/%s/web/com.etendoerp.entendorx/resources/MiddlewareResponse.html"
            + "?title=%s&message=%s&icon=%s&iconColor=%s",
        contextName,
        URLEncoder.encode(rawTitle, StandardCharsets.UTF_8),
        URLEncoder.encode(rawMessage, StandardCharsets.UTF_8),
        URLEncoder.encode(icon, StandardCharsets.UTF_8),
        URLEncoder.encode(iconColor, StandardCharsets.UTF_8)
    );

    return responseURL;
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
