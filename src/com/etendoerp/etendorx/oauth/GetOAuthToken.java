package com.etendoerp.etendorx.oauth;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetOAuthToken extends HttpBaseServlet {

  private static final Logger log = LoggerFactory.getLogger(GetOAuthToken.class);

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

      tokenInfo.put("accessToken", token.getToken());

      // Enviar respuesta JSON
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
      }
    }
  }
}
