package com.etendoerp.etendorx.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * This interface defines the methods that an OAuthProviderConfigInjector must implement.
 * An OAuthProviderConfigInjector is responsible for injecting configuration data into a JSONObject.
 * This configuration data is used to configure an OAuth provider.
 */
public interface OAuthProviderConfigInjector {

  /**
   * Injects configuration data into the provided JSONObject.
   * The configuration data is sourced from the sourceJSON parameter.
   *
   * @param sourceJSON the JSONObject that contains the configuration data to be injected
   * @throws JSONException if an error occurs while injecting the configuration data
   */
  void injectConfig(JSONObject sourceJSON) throws JSONException;

  /**
   * Injects configuration data into the provided JSONObject.
   * The configuration data is sourced from the sourceJSON parameter and the provider parameter.
   *
   * @param sourceJSON the JSONObject that contains the configuration data to be injected
   * @param provider the ETRXoAuthProvider that contains additional configuration data to be injected
   * @throws JSONException if an error occurs while injecting the configuration data
   */
  void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException;
}