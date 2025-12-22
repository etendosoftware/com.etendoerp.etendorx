package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormInitializationExceptionTest {

  @Test
  public void testConstructor() {
    Exception cause = new Exception("Cause message");
    FormInitializationException exception = new FormInitializationException(cause);
    assertEquals(cause, exception.getCause());
  }
}
