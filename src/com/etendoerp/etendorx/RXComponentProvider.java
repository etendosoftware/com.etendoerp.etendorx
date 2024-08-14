package com.etendoerp.etendorx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

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

    return resources;
  }
}
