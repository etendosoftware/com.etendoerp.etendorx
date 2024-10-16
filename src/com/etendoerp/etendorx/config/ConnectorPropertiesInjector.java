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

public class ConnectorPropertiesInjector implements OAuthProviderConfigInjector {
  private static final Logger log = LogManager.getLogger();

  @Override
  public void injectConfig(JSONObject sourceJSON) throws JSONException {
    try {
      if (StringUtils.equals("worker", sourceJSON.getString("name"))) {
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

  @Override
  public void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException {
    throw new UnsupportedOperationException("No operation supported for ConnectorPropertiesInjector");
  }
}
