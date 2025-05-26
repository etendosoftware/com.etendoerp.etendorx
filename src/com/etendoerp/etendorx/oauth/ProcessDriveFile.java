package com.etendoerp.etendorx.oauth;

import com.etendoerp.etendorx.data.DriveFile;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Servlet that processes HTTP POST requests to approve and store a Google Drive file ID
 * associated with the current user and a specific OAuth provider.
 *
 * <p>This servlet reads a JSON request body containing a {@code fileId} field,
 * creates a new {@link DriveFile} entity linked to the current user and the "EtendoMiddleware" OAuth provider,
 * saves it to the database, and returns a JSON response indicating success or failure.</p>
 *
 * <p>On success, the response will be:
 * <pre>
 * {
 *   "status": "ok",
 *   "message": "File approved successfully."
 * }
 * </pre>
 * On failure, the response will be:
 * <pre>
 * {
 *   "status": "error",
 *   "message": "Error processing spreadsheet."
 * }
 * </pre>
 * and the HTTP status will be set to 500.</p>
 */
public class ProcessDriveFile extends HttpBaseServlet {
  private static final Logger log = LoggerFactory.getLogger(ProcessDriveFile.class);

  /**
   * Handles HTTP POST requests containing a JSON body with a Google Drive file ID.
   *
   * <p>The method extracts the {@code fileId} from the request body, creates and persists a new {@link DriveFile}
   * entity for the current user linked to the OAuth provider with value "EtendoMiddleware".</p>
   *
   * @param req  the HTTP servlet request containing the JSON body with {@code fileId}
   * @param resp the HTTP servlet response to write the JSON success or error message
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try {
      BufferedReader reader = req.getReader();
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      JSONObject body = new JSONObject(new JSONTokener(sb.toString()));

      String fileId = body.getString("fileId");

      DriveFile approvedFile = OBProvider.getInstance().get(DriveFile.class);
      approvedFile.setUser(OBContext.getOBContext().getUser());
      ETRXoAuthProvider middlewareProvider =  (ETRXoAuthProvider) OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
              .add(Restrictions.eq(ETRXoAuthProvider.PROPERTY_VALUE, "EtendoMiddleware"))
              .setMaxResults(1).uniqueResult();

      approvedFile.setEtrxOauthProvider(middlewareProvider);
      approvedFile.setIdfile(fileId);
      OBDal.getInstance().save(approvedFile);

      JSONObject result = new JSONObject();
      result.put("status", "ok");
      result.put("message", "File approved successfully.");

      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getWriter().write(result.toString());

    } catch (Exception e) {
      log.error("Error processing spreadsheet", e);
      try {
        JSONObject error = new JSONObject();
        error.put("status", "error");
        error.put("message", "Error processing spreadsheet.");
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.setContentType("application/json");
        resp.getWriter().write(error.toString());
      } catch (IOException | JSONException ignored) {}
    }
  }
}
