package com.etendoerp.etendorx.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

public class OAuthProviderConfigInjectorRegistry {
  private static final Logger log = LogManager.getLogger();
  private static final List<OAuthProviderConfigInjector> injectors = new ArrayList<>();
  static {
    // Automatically register all implementations of OAuthProviderConfigInjector
    Reflections reflections = new Reflections("com.etendoerp");
    Set<Class<? extends OAuthProviderConfigInjector>> injectorClasses =
        reflections.getSubTypesOf(OAuthProviderConfigInjector.class);

    for (Class<? extends OAuthProviderConfigInjector> injectorClass : injectorClasses) {
      try {
        OAuthProviderConfigInjector injector = injectorClass.getDeclaredConstructor().newInstance();
        registerInjector(injector);
      } catch (Exception e) {
        // Handle exception (e.g., log it)
        log.error(e.getMessage(), e);
      }
    }
  }

  public static void registerInjector(OAuthProviderConfigInjector injector) {
    injectors.add(injector);
  }

  public static List<OAuthProviderConfigInjector> getInjectors() {
    return Collections.unmodifiableList(injectors);
  }
}
