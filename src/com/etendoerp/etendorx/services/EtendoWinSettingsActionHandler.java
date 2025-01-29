package com.etendoerp.etendorx.services;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.WindowSettingsActionHandler;

import java.util.Map;

/**
 * This class extends the WindowSettingsActionHandler to provide custom window settings logic.
 */
public class EtendoWinSettingsActionHandler extends WindowSettingsActionHandler {

  /**
   * Executes the window settings action with the given parameters and data.
   *
   * @param parameters A map of parameters required for the window settings action.
   * @param data The data to be processed during the window settings action.
   * @return A JSONObject containing the result of the window settings action.
   */
  public JSONObject execute(Map<String, Object> parameters, String data) {
    return super.execute(parameters, data);
  }
}
