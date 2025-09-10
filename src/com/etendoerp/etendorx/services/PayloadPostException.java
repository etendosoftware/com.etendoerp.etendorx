package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

public class PayloadPostException extends OBException {
  public PayloadPostException(String message) {
    super(message);
  }
}
