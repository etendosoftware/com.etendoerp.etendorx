package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test class for CalloutExecutionException.
 */
public class CalloutExecutionExceptionTest {

  /**
   * Test to verify that the CalloutExecutionException constructor sets the message correctly.
   */
  @Test
  public void testConstructor() {
    String message = "Test message";
    CalloutExecutionException exception = new CalloutExecutionException(message);
    assertEquals(message, exception.getMessage());
  }
}
