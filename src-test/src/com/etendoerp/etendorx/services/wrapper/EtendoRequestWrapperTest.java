package com.etendoerp.etendorx.services.wrapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

class EtendoRequestWrapperTest {

  @Test
  void testWrapper() throws Exception {
    HttpServletRequest originalRequest = mock(HttpServletRequest.class);
    when(originalRequest.getParameterMap()).thenReturn(new HashMap<>());
    
    String newURI = "/new-uri";
    String newBody = "new body";
    Map<String, String[]> newParams = new HashMap<>();
    newParams.put("param1", new String[]{"value1"});
    
    EtendoRequestWrapper wrapper = new EtendoRequestWrapper(originalRequest, newURI, newBody, newParams);
    
    assertEquals(newURI, wrapper.getRequestURI());
    
    // Test Reader
    BufferedReader reader = wrapper.getReader();
    assertEquals(newBody, reader.readLine());
    
    // Test InputStream
    ServletInputStream inputStream = wrapper.getInputStream();
    assertEquals('n', inputStream.read());
    assertTrue(inputStream.isReady());
    
    // Test Parameters
    assertEquals("value1", wrapper.getParameter("param1"));
    assertArrayEquals(new String[]{"value1"}, wrapper.getParameterValues("param1"));
    assertTrue(wrapper.getParameterMap().containsKey("param1"));
    
    Enumeration<String> names = wrapper.getParameterNames();
    assertTrue(Collections.list(names).contains("param1"));
  }
}
