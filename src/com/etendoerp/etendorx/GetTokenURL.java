package com.etendoerp.etendorx;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.RXConfigUtils;

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
      ETRXConfig rxConfig = RXConfigUtils.getRXConfig("auth");
      if (rxConfig == null) {
        final String etrxNoConfigAuthFound = OBMessageUtils.getI18NMessage("ETRX_NoConfigAuthFound");
        handleErrorMessage(etrxNoConfigAuthFound, result);
        throw new OBException(etrxNoConfigAuthFound);
      }
      final String getTokenURL = getGetTokenURL(rxConfig, oauthProvider, oAuthProviderId);
      result.put("auth_url", getTokenURL);
    } catch (OBException | JSONException e) {
      log.error(e.getMessage(), e);
      handleErrorMessage(e.getMessage(), result);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * This method is used to create the token URL.
   *
   * @param rxConfig an ETRXConfig instance
   * @param oauthProvider an ETRXoAuthProvider instance
   * @param oAuthProviderId a String containing the OAuth provider ID
   * @return a String containing the token URL
   */
  private static String getGetTokenURL(ETRXConfig rxConfig, ETRXoAuthProvider oauthProvider,
      String oAuthProviderId) {
    return rxConfig.getPublicURL() + oauthProvider.getAuthorizationEndpoint() + oauthProvider.getValue()
        + String.format("?userId=%s&etrxOauthProviderId=%s",
        OBContext.getOBContext().getUser().getId(), oAuthProviderId);
  }

  /**
   * This method is used to handle error messages.
   * It creates a JSONObject containing the error message and adds it to the result.
   *
   * @param message a String containing the error message
   * @param result a JSONObject containing the result
   */
  private static void handleErrorMessage(String message, JSONObject result) {
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "error");
      errorMessage.put("title", "ERROR");
      errorMessage.put("text", message);
      result.put("message", errorMessage);
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
    }
  }
}
