package com.etendoerp.etendorx.services;

import org.junit.jupiter.api.Test;

import static com.etendoerp.etendorx.services.DataSourceServlet.extractDataSourceAndID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for DataSourceServlet.
 */
public class DataSourceServletTest {
  @Test
  void testExtractWithID() {
    String requestURI = "/Assistant/87365CC2593947349D72C08CE6C7FD18";
    String[] result = extractDataSourceAndID(requestURI);

    assertEquals(2, result.length);
    assertEquals("Assistant", result[0]);
    assertEquals("87365CC2593947349D72C08CE6C7FD18", result[1]);
  }

  @Test
  void testExtractWithoutID() {
    String requestURI = "/Assistant";
    String[] result = extractDataSourceAndID(requestURI);

    assertEquals(1, result.length);
    assertEquals("Assistant", result[0]);
  }

  @Test
  void testInvalidURIThrowsException() {
    String requestURI = "/etendo/sws/invalid/URI";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> extractDataSourceAndID(requestURI));

    assertEquals("Invalid request URI: /etendo/sws/invalid/URI", exception.getMessage());
  }

}
