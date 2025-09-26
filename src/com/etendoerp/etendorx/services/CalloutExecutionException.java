package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

/**
 * Exception raised when a callout execution fails within Etendorx.
 *
 * <p>Use this exception to wrap errors that occur while executing callouts (for example,
 * validation or integration callouts) so they can be handled consistently by the calling code
 * and by Openbravo's exception framework.
 */
public class CalloutExecutionException extends OBException {

  /**
   * Creates a new CalloutExectionException with the given detail message.
   *
   * @param message a descriptive message explaining why the callout execution failed
   */
  public CalloutExecutionException(String message) {
    super(message);
  }
}
