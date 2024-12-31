package com.etendoerp.etendorx.actionhandler;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.utils.RXServiceManagementUtils;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

public class RestartRXServices extends Action {
  private static final Logger log = LogManager.getLogger();
  private static final String ACTUATOR_RESTART = "/actuator/restart";

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult actionResult = new ActionResult();
    actionResult.setType(Result.Type.SUCCESS);
    var input = getInputContents(getInputClass());
    StringBuilder sucRestServices = new StringBuilder();
    for (ETRXConfig currentService : input) {
      String serviceName = currentService.getServiceName().toUpperCase();
      if (StringUtils.equals("config", currentService.getServiceName())) {
        log.warn("Config Service can not be restarted. ");
        sucRestServices.append("Config Service can not be restarted. ");
      } else {
        try {
          RXServiceManagementUtils.performRestart(currentService.getServiceURL() + ACTUATOR_RESTART, actionResult);
          sucRestServices.append(String.format("%s Has been restarted. ", serviceName));
        } catch (ConnectException ex) {
          actionResult.setType(Result.Type.WARNING);
          sucRestServices.append(String.format("%s service is not running yet. ", serviceName));
        } catch (IOException e) {
          log.error("Error while restarting RX services", e);
          actionResult.setType(Result.Type.ERROR);
          actionResult.setMessage(e.getMessage());
        }
      }
    }
    actionResult.setMessage(sucRestServices.toString());
    return actionResult;
  }

  @Override
  protected Class<ETRXConfig> getInputClass() { return ETRXConfig.class; }
}
