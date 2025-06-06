package com.etendoerp.etendorx.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.data.InstanceConnector;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjector;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * This class is responsible for injecting configuration properties.
 */
public class ConnectorPropertiesInjector implements OAuthProviderConfigInjector {
  private static final Logger log = LogManager.getLogger();
  private static final String WORKER_SERVICE = "worker";

  /**
   * Injects configuration data into the provided JSONObject.
   * The configuration data is sourced from the sourceJSON parameter.
   *
   * @param sourceJSON the JSONObject that contains the configuration data to be injected
   * @throws JSONException if an error occurs while injecting the configuration data
   */
  @Override
  public void injectConfig(JSONObject sourceJSON) throws JSONException {
    try {
      if (StringUtils.equals(WORKER_SERVICE, sourceJSON.getString("name"))) {
        var jsonObject = sourceJSON.getJSONArray("propertySources").getJSONObject(0).getJSONObject("source");
        InstanceConnector instanceConnector = OBDal.getInstance().get(InstanceConnector.class, jsonObject.getString("connector.instance"));
        if (instanceConnector == null) {
          String dbMessage = Utility.messageBD(new DalConnectionProvider(), "ETRX_NoConnectorInstance",
              OBContext.getOBContext().getLanguage().getLanguage());
          log.error(dbMessage);
          throw new OBException(dbMessage);
        }
        User tokenUser = instanceConnector.getUserForToken();
        String token = SecureWebServicesUtils.generateToken(tokenUser);
        JSONObject propertySources = sourceJSON.getJSONArray("propertySources")
            .getJSONObject(0)
            .getJSONObject("source");
        propertySources.put("token", token);
        propertySources.put("classic.token", token);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }
}
