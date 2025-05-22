package com.etendoerp.etendorx.oauth;

import com.etendoerp.etendorx.data.DriveFile;
import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer;
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

public class ProcessDriveFile extends HttpBaseServlet {
  private static final Logger log = LoggerFactory.getLogger(ProcessDriveFile.class);

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
