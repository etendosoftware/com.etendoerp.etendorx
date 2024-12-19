package com.etendoerp.etendorx.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

/**
 * This class is a registry for OAuthProviderConfigInjector instances.
 * It provides methods to register and retrieve OAuthProviderConfigInjector instances.
 * It also automatically registers all implementations of OAuthProviderConfigInjector found in the "com.etendoerp" package.
 */
public class OAuthProviderConfigInjectorRegistry {
  private static final Logger log = LogManager.getLogger();
  private static final List<OAuthProviderConfigInjector> injectors = new ArrayList<>();
  static {
    Reflections reflections = new Reflections("com.etendoerp");
    Set<Class<? extends OAuthProviderConfigInjector>> injectorClasses =
        reflections.getSubTypesOf(OAuthProviderConfigInjector.class);

    for (Class<? extends OAuthProviderConfigInjector> injectorClass : injectorClasses) {
      try {
        OAuthProviderConfigInjector injector = injectorClass.getDeclaredConstructor().newInstance();
        registerInjector(injector);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Registers an instance of OAuthProviderConfigInjector to the list of injectors.
   * This method is used to add a new OAuthProviderConfigInjector instance to the static list of registered injectors.
   * This allows the application to keep track of all the OAuthProviderConfigInjector instances that have been created and registered.
   *
   * @param injector the OAuthProviderConfigInjector instance to be registered
   */
  public static void registerInjector(OAuthProviderConfigInjector injector) {
    injectors.add(injector);
  }

  /**
   * Returns an unmodifiable list of registered OAuthProviderConfigInjector instances.
   * This method is used to get all the registered OAuthProviderConfigInjector instances in a form that cannot be modified.
   * This is to prevent accidental modification of the list of registered OAuthProviderConfigInjector instances.
   *
   * @return an unmodifiable list of OAuthProviderConfigInjector instances
   */
  public static List<OAuthProviderConfigInjector> getInjectors() {
    return Collections.unmodifiableList(injectors);
  }
}
