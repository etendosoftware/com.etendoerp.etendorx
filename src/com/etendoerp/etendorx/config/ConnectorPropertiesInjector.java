package com.etendoerp.etendorx.config;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

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
        InstanceConnector instanceConnector = (InstanceConnector) OBDal.getInstance().createCriteria(
                InstanceConnector.class)
            .setMaxResults(1)
            .uniqueResult();
        User tokenUser = instanceConnector.getUserForToken();
        String token = SecureWebServicesUtils.generateToken(tokenUser);
        JSONObject propertySources = sourceJSON.getJSONArray("propertySources")
            .getJSONObject(0)
            .getJSONObject("source");
        propertySources.put("token", token);
        propertySources.put("classic.token", token);
      }
    } catch (NullPointerException npe) {
      log.error(String.format("Null User for connector instance - %s", npe.getMessage()), npe);
      throw new OBException(npe);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  /**
   * Injects configuration data into the provided JSONObject.
   * The configuration data is sourced from the sourceJSON parameter and the provider parameter.
   *
   * @param sourceJSON the JSONObject that contains the configuration data to be injected
   * @param provider the ETRXoAuthProvider that contains additional configuration data to be injected
   * @throws JSONException if an error occurs while injecting the configuration data
   */
  @Override
  public void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException {
    throw new UnsupportedOperationException("No operation supported for ConnectorPropertiesInjector");
  }
}
