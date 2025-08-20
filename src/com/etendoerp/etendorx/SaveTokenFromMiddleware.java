package com.etendoerp.etendorx;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

      String error = request.getParameter("error");
      String errorDescription = request.getParameter("error_description");

      if (StringUtils.isNotBlank(error)) {
        String formattedError = formatOAuthError(error);
        String description = (StringUtils.isNotBlank(errorDescription) && !StringUtils.equals("undefined", errorDescription))
            ? errorDescription : "";
        String responseURL = getResponseBody(true, formattedError, description);
        response.sendRedirect(responseURL);
        response.flushBuffer();
        return;
      }
      User currentUser = OBContext.getOBContext().getUser();
      Organization currentOrg = OBContext.getOBContext().getCurrentOrganization();
      List<ETRXTokenInfo> oldTokenList = OBDal.getInstance().createCriteria(ETRXTokenInfo.class)
          .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_USER, currentUser))
          .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_ORGANIZATION, currentOrg))
          .list();
      for (ETRXTokenInfo oldToken : oldTokenList) {
        OBDal.getInstance().remove(oldToken);
      }
      ETRXTokenInfo newToken = OBProvider.getInstance().get(ETRXTokenInfo.class);
      newToken.setToken(request.getParameter("access_token"));
      String providerScope = request.getParameter("provider") + " - " + request.getParameter("scope");
      newToken.setMiddlewareProvider(providerScope);
      newToken.setEtrxOauthProvider(getETRXoAuthProvider());
      newToken.setUser(currentUser);
      newToken.setOrganization(currentOrg);
      Date now = new Date();
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(now);
      calendar.add(Calendar.HOUR_OF_DAY, 1);
      newToken.setValidUntil(calendar.getTime());
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
   * Converts an error code like "access_denied" to a user-friendly format like "Access Denied".
   *
   * @param errorCode the raw error code from OAuth
   * @return formatted error message
   */
  private static String formatOAuthError(String errorCode) {
    if (StringUtils.isBlank(errorCode)) {
      return "Unknown Error";
    }

    String[] parts = errorCode.split("_");
    StringBuilder formatted = new StringBuilder();

    for (String part : parts) {
      if (!part.isEmpty()) {
        formatted.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) {
          formatted.append(part.substring(1).toLowerCase());
        }
        formatted.append(" ");
      }
    }

    return formatted.toString().trim();
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

    return String.format("/%s/web/com.etendoerp.etendorx/resources/MiddlewareResponse.html"
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
  protected static ETRXoAuthProvider getETRXoAuthProvider() {
    return (ETRXoAuthProvider) OBDal.getInstance()
        .createCriteria(ETRXoAuthProvider.class)
        .add(Restrictions.eq(ETRXoAuthProvider.PROPERTY_VALUE, "EtendoMiddleware"))
        .setMaxResults(1)
        .uniqueResult();
  }

  private static String getResponseBody(boolean error, String customTitle, String customMessage) {
    String contextName = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("context.name")
        .trim();

    String rawTitle = error
        ? (Objects.requireNonNullElse(customTitle, "Error"))
        : Utility.messageBD(new DalConnectionProvider(), "ETRX_TokenCreated", OBContext.getOBContext().getLanguage().getLanguage());

    String rawMessage = error
        ? (Objects.requireNonNullElse(customMessage, "Se produjo un error al procesar el token."))
        : Utility.messageBD(new DalConnectionProvider(), "ETRX_TokenCreatedDescription", OBContext.getOBContext().getLanguage().getLanguage());

    String icon = error ? ERROR_ICON : SUCCESS_ICON;
    String iconColor = error ? RED : GREEN;

    return String.format("/%s/web/com.etendoerp.etendorx/resources/MiddlewareResponse.html"
            + "?title=%s&message=%s&icon=%s&iconColor=%s",
        contextName,
        URLEncoder.encode(rawTitle, StandardCharsets.UTF_8),
        URLEncoder.encode(rawMessage, StandardCharsets.UTF_8),
        URLEncoder.encode(icon, StandardCharsets.UTF_8),
        URLEncoder.encode(iconColor, StandardCharsets.UTF_8)
    );
  }

}
