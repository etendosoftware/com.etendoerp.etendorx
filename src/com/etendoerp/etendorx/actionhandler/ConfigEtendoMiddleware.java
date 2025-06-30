package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import java.util.Map;

/**
 * Configures the Etendo Middleware OAuth provider with default values.
 * This class is used to set up the OAuth provider for Etendo Middleware integration.
 */
public class ConfigEtendoMiddleware extends BaseActionHandler {

  private static final String MESSAGE_SEVERITY = "severity";
  private static final String MESSAGE_TEXT = "text";
  private static final String MESSAGE_SUCCESS = "success";
  public static final String ETENDO_MIDDLEWARE = "EtendoMiddleware";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject actionResult = new JSONObject();
    try {
      ETRXoAuthProvider middlewareExist = (ETRXoAuthProvider) OBDal.getInstance().createCriteria(ETRXoAuthProvider.class)
          .add(Restrictions.eq(ETRXoAuthProvider.PROPERTY_VALUE, ETENDO_MIDDLEWARE))
          .setMaxResults(1).uniqueResult();
      if (middlewareExist != null) {
        actionResult.put(MESSAGE_SEVERITY, "warning");
        actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD("Warning") + "<br/>" +
            OBMessageUtils.messageBD("ETRX_Middleware_Config_AlreadyExist"));
        return actionResult;
      }
      actionResult.put(MESSAGE_SEVERITY, MESSAGE_SUCCESS);
      actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD(MESSAGE_SUCCESS) + "<br/>" +
          OBMessageUtils.messageBD("ETRX_Middleware_Configuration_Success"));
      ETRXoAuthProvider etendoMiddleware = OBProvider.getInstance().get(ETRXoAuthProvider.class);
      etendoMiddleware.setValue(ETENDO_MIDDLEWARE);
      etendoMiddleware.setClientName(ETENDO_MIDDLEWARE);
      etendoMiddleware.setIDForClient("---");
      etendoMiddleware.setClientSecret("---");
      etendoMiddleware.setScope("---");
      String envURL = parameters.get("envURL").toString();
      etendoMiddleware.setRedirectURI(envURL + "saveTokenMiddleware");
      String middlewareURL = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("sso.middleware.url") + "/oauth-integrations";
      etendoMiddleware.setAuthorizationEndpoint(middlewareURL);
      OBDal.getInstance().save(etendoMiddleware);
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return actionResult;
  }
}
