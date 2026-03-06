package com.etendoerp.etendorx.utils;

/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;

class OAuthProviderConfigInjectorTest {

  /**
   * A simple implementation of the interface to test the default methods.
   */
  private static class TestInjector implements OAuthProviderConfigInjector {
  }

  @Test
  void testDefaultInjectConfigDoesNotThrow() {
    OAuthProviderConfigInjector injector = new TestInjector();
    JSONObject json = new JSONObject();

    assertDoesNotThrow(() -> injector.injectConfig(json));
  }

  @Test
  void testDefaultInjectConfigWithProviderDoesNotThrow() {
    OAuthProviderConfigInjector injector = new TestInjector();
    JSONObject json = new JSONObject();

    assertDoesNotThrow(() -> injector.injectConfig(json, null));
  }
}
