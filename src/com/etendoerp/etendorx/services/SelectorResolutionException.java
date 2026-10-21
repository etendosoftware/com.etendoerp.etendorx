package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

/** Raised when a selector value cannot be resolved before persisting a headless request. */
public class SelectorResolutionException extends OBException {

  /**
   * Wraps the resolution failure while preserving its message and cause.
   *
   * @param cause the failure that prevented the selector value from being resolved
   */
  public SelectorResolutionException(Throwable cause) {
    super(cause.getMessage(), cause);
  }
}
