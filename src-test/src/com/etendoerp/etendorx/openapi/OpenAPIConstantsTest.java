package com.etendoerp.etendorx.openapi;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OpenAPIConstantsTest {

  @Test
  void testGetConstant() {
    assertEquals("GET", OpenAPIConstants.GET);
  }

  @Test
  void testPostConstant() {
    assertEquals("POST", OpenAPIConstants.POST);
  }

  @Test
  void testPutConstant() {
    assertEquals("PUT", OpenAPIConstants.PUT);
  }

  @Test
  void testResponseConstant() {
    assertEquals("response", OpenAPIConstants.RESPONSE);
  }

  @Test
  void testStringConstant() {
    assertEquals("string", OpenAPIConstants.STRING);
  }

  @Test
  void testNumberConstant() {
    assertEquals("number", OpenAPIConstants.NUMBER);
  }

  @Test
  void testObjectConstant() {
    assertEquals("object", OpenAPIConstants.OBJECT);
  }

  @Test
  void testBasePath() {
    assertEquals("/sws/com.etendoerp.etendorx.datasource/", OpenAPIConstants.BASE_PATH);
  }
}
