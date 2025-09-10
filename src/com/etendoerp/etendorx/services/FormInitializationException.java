package com.etendoerp.etendorx.services;

import org.openbravo.base.exception.OBException;

public class FormInitializationException extends OBException {
  public FormInitializationException(Exception e) {
    super(e);
  }
}
