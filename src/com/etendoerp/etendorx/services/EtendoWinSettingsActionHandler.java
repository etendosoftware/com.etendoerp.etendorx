package com.etendoerp.etendorx.services;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.WindowSettingsActionHandler;

import java.util.Map;

public class EtendoWinSettingsActionHandler extends WindowSettingsActionHandler {
  public JSONObject execute(Map<String, Object> parameters, String data) {
    return super.execute(parameters, data);
  }
}
