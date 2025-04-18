package com.etendoerp.etendorx;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.RXServiceManagementUtils;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

/**
 * This class extends the Action class and is used to refresh OAuth configurations.
 */
public class RefreshOAuthConfigs extends Action {

  private static final Logger log = LogManager.getLogger();
  private static final String ACTUATOR_RESTART = "/actuator/restart";

  /**
   * This method is used to perform the action of refreshing OAuth configurations.
   * It first gets the ETRXConfig instance, then creates a connection to the authentication server,
   * sends a POST request, and checks the response. If any error occurs during the process, it logs the error and sets the ActionResult type to ERROR.
   *
   * @param parameters a JSONObject containing the parameters for the action
   * @param isStopped a MutableBoolean that indicates whether the action should be stopped
   * @return an ActionResult that indicates the result of the action
   */
  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    actionResult.setMessage("OAuth configurations are being refreshed.");

    List<ETRXConfig> rxConfig = OBDal.getInstance().createCriteria(ETRXConfig.class)
        .add(Restrictions.eq(ETRXConfig.PROPERTY_UPDATEABLECONFIGS, true))
        .list();

    if (rxConfig.isEmpty()) {
      actionResult.setType(Result.Type.WARNING);
      String noConfigMessage = OBMessageUtils.getI18NMessage("ETRX_NoConfigToRefresh");
      actionResult.setMessage(noConfigMessage);
      throw new OBException(noConfigMessage);
    }

    String serviceName = "";
    StringBuilder sucRestServices = new StringBuilder();
    try {
      for(ETRXConfig actualService : rxConfig) {
        serviceName = actualService.getServiceName();
        RXServiceManagementUtils.performRestart(actualService.getServiceURL() + ACTUATOR_RESTART, actionResult);
        sucRestServices.append(String.format("%s Has been restarted.", serviceName));
      }

    } catch (ConnectException e1) {
      log.error("Failed to connect: {}", e1.getMessage(), e1);
      actionResult.setType(Result.Type.WARNING);
      actionResult.setMessage(sucRestServices +
          String.format("Failed to restart %s. Please restart the server manually.", serviceName));
    } catch (IOException e2) {
      log.error("I/O error: {}", e2.getMessage(), e2);
      actionResult.setType(Result.Type.ERROR);
      actionResult.setMessage(e2.getMessage());
    }

    return actionResult;
  }

  /**
   * This method is used to get the input class for the action.
   *
   * @return the Class object for ETRXoAuthProvider
   */
  @Override
  protected Class<ETRXoAuthProvider> getInputClass() {
    return ETRXoAuthProvider.class;
  }
}
