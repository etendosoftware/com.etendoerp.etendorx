package com.etendoerp.etendorx;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.Map;
import java.util.Properties;

/**
 * This class is responsible for retrieving SSO properties from the Openbravo properties file.
 * It extends the BaseActionHandler class and overrides the execute method to perform the action.
 */
public class GetSSOProperties extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(GetSSOProperties.class);

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
  private static String getAccountID() {
    String accountID;
    try {
      accountID = SystemInfo.getSystemIdentifier();
      if (StringUtils.isBlank(accountID)) {
        log.warn("[SSO] - Empty System Identifier, account id to middleware will be empty");
      }
    } catch (ServletException e) {
      throw new OBException(e);
    }
    return accountID;
  }

  /**
   * This method is called to execute the action of retrieving SSO properties.
   * It reads the properties from the Openbravo properties file and returns them in a JSON object.
   *
   * @param parameters a map containing the parameters for the action
   * @param content    a string containing the content of the request
   * @return a JSON object containing the SSO properties
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();
    try {
      Properties obProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      final JSONObject jsonData = new JSONObject(content);
      final String neededProperties = jsonData.getString("properties");
      String[] ssoProperties = neededProperties.split(",");
      for (String ssoProperty : ssoProperties) {
        String completeProperty = "sso." + ssoProperty.trim();
        if (obProperties.containsKey(completeProperty)) {
          result.put(StringUtils.replace(ssoProperty.trim(), ".", ""), obProperties.getProperty(completeProperty));
        } else if (StringUtils.equals("account", ssoProperty)) {
          result.put("account", getAccountID());
        }
      }
    } catch (JSONException e) {
      log.error("Error while getting SSO properties", e);
      throw new OBException("Error while getting SSO properties", e);
    }
    return result;
  }
}
