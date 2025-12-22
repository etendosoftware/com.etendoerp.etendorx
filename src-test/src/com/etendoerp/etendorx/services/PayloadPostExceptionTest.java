package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PayloadPostExceptionTest {

  @Test
  public void testConstructor() {
    String message = "Test message";
    PayloadPostException exception = new PayloadPostException(message);
    assertEquals(message, exception.getMessage());
  }
}
