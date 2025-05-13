package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;

/**
 * Configures the Etendo Middleware OAuth provider with default values.
 * This class is used to set up the OAuth provider for Etendo Middleware integration.
 */
public class ConfigEtendoMiddleware extends Action {
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    ETRXoAuthProvider etendoMiddleware = OBProvider.getInstance().get(ETRXoAuthProvider.class);
    etendoMiddleware.setValue("EtendoMiddleware");
    etendoMiddleware.setClientName("EtendoMiddleware");
    etendoMiddleware.setIDForClient("---");
    etendoMiddleware.setClientSecret("---");
    etendoMiddleware.setScope("https://www.googleapis.com/auth/spreadsheets");
    etendoMiddleware.setRedirectURI("http://localhost:8080/oauth/saveTokenMiddleware");
    etendoMiddleware.setAuthorizationEndpoint("http://etendoauth-middleware-env.eba-purewhpv.sa-east-1.elasticbeanstalk.com/oauth-integrations/start?provider=google&account_id=etendo_123&scope=https://www.googleapis.com/auth/spreadsheets&redirect_uri=http://localhost:8080/oauth/saveTokenMiddleware");
    OBDal.getInstance().save(etendoMiddleware);
    return actionResult;
  }

  @Override
  protected Class<ConfigEtendoMiddleware> getInputClass() { return ConfigEtendoMiddleware.class; }
}
