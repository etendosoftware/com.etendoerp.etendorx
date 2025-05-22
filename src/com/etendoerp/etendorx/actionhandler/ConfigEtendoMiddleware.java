package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import java.util.Map;

/**
 * Configures the Etendo Middleware OAuth provider with default values.
 * This class is used to set up the OAuth provider for Etendo Middleware integration.
 */
public class ConfigEtendoMiddleware extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();
  private static final String MESSAGE = "message";
  private static final String MESSAGE_SEVERITY = "severity";
  private static final String MESSAGE_TEXT = "text";
  private static final String MESSAGE_SUCCESS = "success";
  private static final String MESSAGE_ERROR = "error";
  private static final String ERROR_TITLE = "ERROR";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject actionResult = new JSONObject();
    try {
      actionResult.put(MESSAGE_SEVERITY, MESSAGE_SUCCESS);
      actionResult.put(MESSAGE_TEXT, OBMessageUtils.messageBD(MESSAGE_SUCCESS) + "<br/>" +
          OBMessageUtils.messageBD("ETRX_Middleware_Configuration_Success"));
      ETRXoAuthProvider etendoMiddleware = OBProvider.getInstance().get(ETRXoAuthProvider.class);
      etendoMiddleware.setValue("EtendoMiddleware");
      etendoMiddleware.setClientName("EtendoMiddleware");
      etendoMiddleware.setIDForClient("---");
      etendoMiddleware.setClientSecret("---");
      etendoMiddleware.setScope("---");
      String envURL = parameters.get("envURL").toString();
      etendoMiddleware.setRedirectURI(envURL + "saveTokenMiddleware");
      String clientName = OBContext.getOBContext().getCurrentClient().getName();
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
