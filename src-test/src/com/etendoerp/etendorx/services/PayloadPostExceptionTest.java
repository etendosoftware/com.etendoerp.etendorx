package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test class for PayloadPostException.
 */
public class PayloadPostExceptionTest {

  /**
   * Test to verify that the PayloadPostException constructor sets the message correctly.
   */
  @Test
  public void testConstructor() {
    String message = "Test message";
    PayloadPostException exception = new PayloadPostException(message);
    assertEquals(message, exception.getMessage());
  }
}
