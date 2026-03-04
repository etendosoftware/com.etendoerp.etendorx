package com.etendoerp.etendorx.services.wrapper;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.openbravo.model.ad.datamodel.Column;

class RequestFieldTest {

  @Test
  void testConstructorAndGetters() {
    Column column = mock(Column.class);
    when(column.getDBColumnName()).thenReturn("C_ORDER_ID");

    RequestField field = new RequestField("orderID", column, 10L);

    assertEquals("orderID", field.getName());
    assertEquals("C_ORDER_ID", field.getDBColumnName());
    assertEquals(column, field.getColumn());
    assertEquals(10L, field.getSeqNo());
  }

  @Test
  void testToString() {
    Column column = mock(Column.class);
    when(column.getDBColumnName()).thenReturn("AD_CLIENT_ID");

    RequestField field = new RequestField("client", column, 20L);

    assertEquals("client (AD_CLIENT_ID)", field.toString());
  }

  @Test
  void testWithNullSeqNo() {
    Column column = mock(Column.class);
    when(column.getDBColumnName()).thenReturn("NAME");

    RequestField field = new RequestField("name", column, null);

    assertEquals("name", field.getName());
    assertEquals("NAME", field.getDBColumnName());
    assertNotNull(field.getColumn());
    assertEquals(null, field.getSeqNo());
  }
}
