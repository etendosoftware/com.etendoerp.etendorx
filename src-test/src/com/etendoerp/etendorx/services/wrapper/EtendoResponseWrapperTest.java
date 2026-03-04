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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBException;

class EtendoResponseWrapperTest {

  @Test
  void testGetWriterReturnsPrintWriter() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    PrintWriter writer = wrapper.getWriter();
    assertNotNull(writer);
  }

  @Test
  void testGetOutputStreamThrowsUnsupportedOperationException() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    UnsupportedOperationException exception = assertThrows(
        UnsupportedOperationException.class,
        wrapper::getOutputStream
    );
    assertEquals("This wrapper only supports getWriter().", exception.getMessage());
  }

  @Test
  void testGetCapturedContentReturnsValidJSON() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    PrintWriter writer = wrapper.getWriter();
    writer.write("{\"key\":\"value\",\"number\":42}");
    writer.flush();

    JSONObject result = wrapper.getCapturedContent();
    assertEquals("value", result.getString("key"));
    assertEquals(42, result.getInt("number"));
  }

  @Test
  void testGetCapturedContentWithEmptyJSON() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    wrapper.getWriter().write("{}");
    wrapper.getWriter().flush();

    JSONObject result = wrapper.getCapturedContent();
    assertNotNull(result);
    assertFalse(result.keys().hasNext());
  }

  @Test
  void testGetCapturedContentInvalidJSONThrowsOBException() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    wrapper.getWriter().write("this is not json");
    wrapper.getWriter().flush();

    assertThrows(OBException.class, wrapper::getCapturedContent);
  }

  @Test
  void testGetWriterReturnsSameInstance() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    EtendoResponseWrapper wrapper = new EtendoResponseWrapper(response);

    PrintWriter writer1 = wrapper.getWriter();
    PrintWriter writer2 = wrapper.getWriter();
    assertEquals(writer1, writer2);
  }
}
