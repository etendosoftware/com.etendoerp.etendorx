package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test class for FormInitializationException.
 */
public class FormInitializationExceptionTest {

  /**
   * Test to verify that the FormInitializationException constructor sets the cause correctly.
   */
  @Test
  public void testConstructor() {
    Exception cause = new Exception("Cause message");
    FormInitializationException exception = new FormInitializationException(cause);
    assertEquals(cause, exception.getCause());
  }
}
