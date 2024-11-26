package com.etendoerp.etendorx.services.wrapper;

public class RequestField {
  private final String name;
  private final String dBColumnName;

  public RequestField(String name, String dBColumnName) {
    this.name = name;
    this.dBColumnName = dBColumnName;
  }

  public String getName() {
    return name;
  }

  public String getDBColumnName() {
    return dBColumnName;
  }

  public String toString() {
    return name + " (" + dBColumnName + ")";
  }

}
