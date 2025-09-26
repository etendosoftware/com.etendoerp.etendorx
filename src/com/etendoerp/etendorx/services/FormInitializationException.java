package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

/**
 * Exception thrown when an error occurs during the initialization of a form within Etendorx.
 *
 * <p>This exception wraps the underlying cause and is propagated to the caller so the
 * initialization failure can be handled or reported consistently by the framework.
 */
public class FormInitializationException extends OBException {

  /**
   * Creates a new FormInitializationException wrapping the given exception.
   *
   * @param e the underlying exception that caused the form initialization to fail
   */
  public FormInitializationException(Exception e) {
    super(e);
  }
}
