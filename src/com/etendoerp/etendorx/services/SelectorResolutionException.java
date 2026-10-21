package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

/** Raised when a selector value cannot be resolved before persisting a headless request. */
public class SelectorResolutionException extends OBException {

  public SelectorResolutionException(Throwable cause) {
    super(cause.getMessage(), cause);
  }
}
