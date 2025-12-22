package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CalloutExecutionExceptionTest {

  @Test
  public void testConstructor() {
    String message = "Test message";
    CalloutExecutionException exception = new CalloutExecutionException(message);
    assertEquals(message, exception.getMessage());
  }
}
