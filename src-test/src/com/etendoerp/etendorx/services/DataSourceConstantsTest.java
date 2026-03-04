package com.etendoerp.etendorx.services;

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

class DataSourceConstantsTest {

  @Test
  void testDataSourceServletPath() {
    assertEquals("/org.openbravo.service.datasource/", DataSourceConstants.DATASOURCE_SERVLET_PATH);
  }

  @Test
  void testErrorInDataSourceServlet() {
    assertEquals("Error in DataSourceServlet", DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET);
  }

  @Test
  void testResponseConstant() {
    assertEquals("response", DataSourceConstants.RESPONSE);
  }

  @Test
  void testDataConstant() {
    assertEquals("data", DataSourceConstants.DATA);
  }

  @Test
  void testErrorConstant() {
    assertEquals("error", DataSourceConstants.ERROR);
  }

  @Test
  void testErrorsConstant() {
    assertEquals("errors", DataSourceConstants.ERRORS);
  }

  @Test
  void testMessageConstant() {
    assertEquals("message", DataSourceConstants.MESSAGE);
  }
}
