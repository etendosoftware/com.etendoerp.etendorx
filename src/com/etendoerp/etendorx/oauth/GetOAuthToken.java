package com.etendoerp.etendorx.oauth;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.utils.GoogleServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet that handles HTTP GET requests to retrieve the current user's OAuth access token
 * for Google Drive API usage.
 *
 * <p>This servlet queries the database for an {@link ETRXTokenInfo} entry associated with the
 * currently logged-in user and the Google Drive file scope ("google%drive.file").
 * If found, it returns the access token as a JSON response.</p>
 *
 * <p>If no token is found or an error occurs, it returns a JSON error message
 * and sets the HTTP status to 500.</p>
 */
public class GetOAuthToken extends HttpBaseServlet {

  private static final Logger log = LoggerFactory.getLogger(GetOAuthToken.class);

  /**
   * Obtains the account identifier for the current system.
   * <p>
   * This method attempts to retrieve the system identifier using
   * {@link SystemInfo#getSystemIdentifier()}. If the identifier is blank,
   * a warning is logged and the account ID returned to the middleware will be empty.
   * </p>
   *
   * <p>
   * In case of a {@link javax.servlet.ServletException}, the exception is wrapped
   * and rethrown as an {@link org.openbravo.base.exception.OBException}.
   * </p>
   *
   * @return the account identifier of the system, or an empty string if the identifier is blank
   * @throws org.openbravo.base.exception.OBException if an error occurs while retrieving the system identifier
   */
  private static ETRXTokenInfo getValidToken(ETRXTokenInfo token) {
    String accountID = "";
    try {
      accountID = SystemInfo.getSystemIdentifier();
      if (StringUtils.isBlank(accountID)) {
        log.warn("[SSO] - Empty System Identifier, account id to middleware will be empty");
      }
    } catch (ServletException e) {
      throw new OBException(e);
    }
    return GoogleServiceUtil.getValidAccessTokenOrRefresh(token, accountID);
  }

  /**
   * Handles the HTTP GET request by returning the OAuth access token JSON for the current user.
   *
   * <p>The response content type is {@code application/json} with UTF-8 encoding.
   * If the token is found, the response body will contain:
   * <pre>
   * {
   *   "accessToken": "&lt;token_value&gt;"
   * }
   * </pre>
   * If the token is not found or another error occurs, a JSON error object is returned with HTTP 500 status:
   * <pre>
   * {
   *   "error": "Failed to retrieve token"
   * }
   * </pre>
   * </p>
   *
   * @param request  the HTTP request
   * @param response the HTTP response to write the token JSON or error JSON
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    JSONObject tokenInfo = new JSONObject();
    try {
      ETRXTokenInfo token = (ETRXTokenInfo) OBDal.getInstance().createCriteria(ETRXTokenInfo.class)
          .add(Restrictions.like(ETRXTokenInfo.PROPERTY_MIDDLEWAREPROVIDER, "google%drive.file"))
          .add(Restrictions.eq(ETRXTokenInfo.PROPERTY_USER, OBContext.getOBContext().getUser()))
          .setMaxResults(1).uniqueResult();
      if (token == null) {
        throw new OBException("Token not found.");
      }
      tokenInfo.put("accessToken", getValidToken(token).getToken());

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(tokenInfo.toString());

    } catch (Exception e) {
      log.error("Error retrieving token data: ", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      try {
        JSONObject error = new JSONObject();
        error.put("error", "Failed to retrieve token");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(error.toString());
      } catch (Exception ignored) {
        log.error("Ignored error: {}", ignored.getMessage());
      }
    }
  }
}
