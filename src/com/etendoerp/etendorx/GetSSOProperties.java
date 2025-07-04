package com.etendoerp.etendorx;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for retrieving SSO properties from the Openbravo properties file.
 * It extends the BaseActionHandler class and overrides the execute method to perform the action.
 */
public class GetSSOProperties extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(GetSSOProperties.class);

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
        }
      }
    } catch (JSONException e) {
      log.error("Error while getting SSO properties", e);
      throw new OBException("Error while getting SSO properties", e);
    }
    return result;
  }
}
