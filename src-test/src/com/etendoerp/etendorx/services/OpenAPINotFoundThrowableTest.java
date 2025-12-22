package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OpenAPINotFoundThrowableTest {

  @Test
  public void testConstructor() {
    String message = "Test message";
    OpenAPINotFoundThrowable throwable = new OpenAPINotFoundThrowable(message);
    assertEquals(message, throwable.getMessage());
  }
}
