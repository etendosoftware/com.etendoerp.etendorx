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
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/oAuthToken/ETRX_GetToken.js",
        false));
    resources.add(createStaticResource("web/com.etendoerp.entendorx/js/init-services-toolbar-button.js",
        false));
    resources.add(createStyleSheetResource(("web/com.etendoerp.userinterface.smartclient/etendo/" +
            "skins/default/com.etendoerp.etendorx/init-services-icon-styles.css"),
        false));

    return resources;
  }
}
