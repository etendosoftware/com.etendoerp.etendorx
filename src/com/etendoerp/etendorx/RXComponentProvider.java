package com.etendoerp.etendorx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(RXComponentProvider.RX_COMPONENT_TYPE)
public class RXComponentProvider  extends BaseComponentProvider {
  public static final String RX_COMPONENT_TYPE = "RX_ComponentType";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) { return null; }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<>();
    // Create Middleware Provider (oAuth Provider window)
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/init-services-toolbar-button.js",
        false));
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/oAuthToken/ETRX_GetToken.js",
        false));
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/oAuthToken/ETRX_GetMiddlewareToken.js",
        false));
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/SSOLogin/log-out-from-sso.js",
        false));
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/SSOLogin/link-sso-account.js",
        false));
    // Create Middleware Provider
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/SSOLogin/config-etendo-middleware.js",
        false));
    // Open Google Picker
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/google-picker.js",
        false));
    // Approve google doc (Calling Google Picker)
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/approveGoogleDoc-picker.js",
        false));
    resources.add(createStyleSheetResource(("web/com.etendoerp.userinterface.smartclient/etendo/" +
            "skins/default/com.etendoerp.etendorx/init-services-icon-styles.css"),
        false));
    resources.add(createStyleSheetResource(("web/com.etendoerp.userinterface.smartclient/etendo/" +
            "skins/default/com.etendoerp.etendorx/config-middleware-icon-style.css"),
        false));

    return resources;
  }
}
