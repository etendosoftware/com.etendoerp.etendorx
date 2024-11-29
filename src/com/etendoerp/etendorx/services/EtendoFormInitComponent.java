package com.etendoerp.etendorx.services;

import org.codehaus.jettison.json.JSONObject;

import java.util.Map;

/**
 * This class extends the FormInitializationComponent to provide custom form initialization logic.
 */
public class EtendoFormInitComponent extends org.openbravo.client.application.window.FormInitializationComponent {

  /**
   * Executes the form initialization with the given parameters and content.
   *
   * @param parameters A map of parameters required for form initialization.
   * @param content The content to be processed during form initialization.
   * @return A JSONObject containing the result of the form initialization.
   */
  public JSONObject execute(Map<String, Object> parameters, String content) {
    return super.execute(parameters, content);
  }

}
