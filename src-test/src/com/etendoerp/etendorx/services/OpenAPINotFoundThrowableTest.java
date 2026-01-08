package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test class for OpenAPINotFoundThrowable.
 */
public class OpenAPINotFoundThrowableTest {

  /**
   * Test to verify that the OpenAPINotFoundThrowable constructor sets the message correctly.
   */
  @Test
  public void testConstructor() {
    String message = "Test message";
    OpenAPINotFoundThrowable throwable = new OpenAPINotFoundThrowable(message);
    assertEquals(message, throwable.getMessage());
  }
}
