package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

/**
 * Exception thrown when an error occurs while posting a payload to an external service or
 * endpoint from Etendorx.
 *
 * <p>This checked runtime exception wraps a message describing the failure and extends
 * {@link org.openbravo.base.exception.OBException} so it integrates with Openbravo's
 * exception handling.
 */
public class PayloadPostException extends OBException {

  /**
   * Constructs a new PayloadPostException with the specified detail message.
   *
   * @param message a descriptive message explaining the reason for the exception
   */
  public PayloadPostException(String message) {
    super(message);
  }
}
