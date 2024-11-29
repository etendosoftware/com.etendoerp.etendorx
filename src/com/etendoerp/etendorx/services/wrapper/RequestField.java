package com.etendoerp.etendorx.services.wrapper;

/**
 * Represents a request field with a name and a database column name.
 */
public class RequestField {
  private final String name;
  private final String dBColumnName;

  /**
   * Constructs a new RequestField.
   *
   * @param name The name of the request field.
   * @param dBColumnName The database column name associated with the request field.
   */
  public RequestField(String name, String dBColumnName) {
    this.name = name;
    this.dBColumnName = dBColumnName;
  }

  /**
   * Gets the name of the request field.
   *
   * @return The name of the request field.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the database column name associated with the request field.
   *
   * @return The database column name.
   */
  public String getDBColumnName() {
    return dBColumnName;
  }

  /**
   * Returns a string representation of the request field.
   *
   * @return A string representation of the request field in the format "name (dBColumnName)".
   */
  public String toString() {
    return name + " (" + dBColumnName + ")";
  }

}
