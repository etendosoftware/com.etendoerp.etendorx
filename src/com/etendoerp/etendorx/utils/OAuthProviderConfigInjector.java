package com.etendoerp.etendorx.utils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

public interface OAuthProviderConfigInjector {
  void injectConfig(JSONObject sourceJSON, ETRXoAuthProvider provider) throws JSONException;
}
