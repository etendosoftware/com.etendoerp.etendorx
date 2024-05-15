package com.etendoerp.etendorx.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class OAuthProviderConfigInjectorRegistry {
  private static final List<OAuthProviderConfigInjector> injectors = new ArrayList<>();

  public static void registerInjector(OAuthProviderConfigInjector injector) {
    injectors.add(injector);
  }

  public static List<OAuthProviderConfigInjector> getInjectors() {
    return Collections.unmodifiableList(injectors);
  }
}
