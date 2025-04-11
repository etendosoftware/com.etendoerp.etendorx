package com.etendoerp.etendorx.services.wrapper;

import org.openbravo.model.ad.datamodel.Column;

/**
 * Represents a request field with a name, database column name, and associated column.
 */
public class RequestField {
  private final String name;
  private final String dBColumnName;
  private final Column adColumn;
  private final Long seqNo;

  /**
   * Constructs a new RequestField.
   *
   * @param name
   *     The name of the request field.
   * @param col
   *     The database column name associated with the request field.
   */
  public RequestField(String name, Column col, Long seqNo) {
    this.name = name;
    this.adColumn = col;
    this.dBColumnName = col.getDBColumnName();
    this.seqNo = seqNo;
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
   * Retrieves the column associated with this request field.
   *
   * @return the {@link Column} object representing the column.
   */
  public Column getColumn() {
    return adColumn;
  }

  /**
   * Retrieves the sequence number of the request field.
   *
   * @return the sequence number of the request field.
   */
  public Long getSeqNo() {
    return seqNo;
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