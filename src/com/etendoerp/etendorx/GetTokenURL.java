package com.etendoerp.etendorx;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * This class is responsible for getting the token URL.
 * It extends the BaseActionHandler class.
 */
public class GetTokenURL extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(GetTokenURL.class);

  /**
   * This method is used to execute the action of getting the token URL.
   * It first sets the admin mode, then gets the ETRXoAuthProvider instance, and creates the token URL.
   * If any error occurs during the process, it logs the error and sets the ActionResult type to ERROR.
   *
   * @param parameters a Map containing the parameters for the action
   * @param content a String containing the content for the action
   * @return a JSONObject containing the result of the action
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      final JSONObject jsonData = new JSONObject(content);
      final String oAuthProviderId = jsonData.getString("id");
      ETRXoAuthProvider oauthProvider = OBDal.getInstance().get(ETRXoAuthProvider.class, oAuthProviderId);
      ETRXConfig rxConfig = (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class)
          .add(Restrictions.eq(ETRXConfig.PROPERTY_SERVICENAME, "auth"))
          .setMaxResults(1)
          .uniqueResult();
      String getTokenURL = rxConfig.getServiceURL() + oauthProvider.getAuthorizationEndpoint() + oauthProvider.getValue()
          + "?userId=" + OBContext.getOBContext().getUser().getId() + "&etrxOauthProviderId=" + oAuthProviderId;
      result.put("auth_url", getTokenURL);
    } catch (OBException | JSONException e) {
      log.error(e.getMessage(), e);
      handleErrorMessage(e, result);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * This method is responsible for handling error messages.
   * It takes an exception and a JSONObject as input.
   * It modifies the JSONObject to include the error message.
   *
   * @param e The exception to handle.
   * @param result The JSONObject to modify.
   */
  private static void handleErrorMessage(Exception e, JSONObject result) {
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "error");
      errorMessage.put("title", "ERROR");
      errorMessage.put("text", e.getMessage());
      result.put("message", errorMessage);
    } catch (JSONException e2) {
      log.error(e.getMessage(), e2);
    }
  }
}
