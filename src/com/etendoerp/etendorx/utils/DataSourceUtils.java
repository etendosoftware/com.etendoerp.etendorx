package com.etendoerp.etendorx.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;

public class DataSourceUtils {

  private static final Logger log = LogManager.getLogger();

  /**
   * Retrieves the HQL column name for a given field.
   *
   * @param field
   *     The field for which to retrieve the HQL column name.
   * @return The HQL column name.
   */
  public static String getHQLColumnName(Field field) {
    return getHQLColumnName(field.getColumn());
  }

  /**
   * Retrieves the HQL column name for a given column.
   *
   * @param fieldColumn
   *     The column for which to retrieve the HQL column name.
   * @return The HQL column name.
   */
  public static String getHQLColumnName(Column fieldColumn) {
    return getHQLColumnName(false, fieldColumn.getTable().getDBTableName(), fieldColumn.getDBColumnName());
  }

  /**
   * Retrieves the HQL column name for a given column, with an option to throw an exception on failure.
   *
   * @param exceptionOnFail
   *     If true, throws an exception if the entity or property is not found.
   * @param dbTableName
   * @param dbColumnName
   * @return The HQL column name.
   * @throws OBException
   *     if the entity or property is not found and exceptionOnFail is true.
   */
  public static String getHQLColumnName(boolean exceptionOnFail, String dbTableName, String dbColumnName) {
    Entity entity = ModelProvider.getInstance().getEntityByTableName(dbTableName);
    if (exceptionOnFail && entity == null) {
      log.error(String.format("Entity of %s not found.", dbTableName));
      throw new OBException();
    }
    Property prop = entity.getPropertyByColumnName(dbColumnName, false);
    if (exceptionOnFail && prop == null) {
      throw new OBException();
    }
    String entAl = prop != null ? prop.getName() : "null";
    return entAl;
  }

  /**
   * Gets the input name for a given column name.
   * <p>
   * This method transforms a column name into its corresponding input name format.
   *
   * @param columnName
   *     The column name to be transformed.
   * @return The input name corresponding to the given column name.
   */
  public static String getInpName(String columnName) {
    return "inp" + Sqlc.TransformaNombreColumna(columnName);
  }

  /**
   * Gets the input name for a given column.
   * <p>
   * This method transforms a column's database column name into its corresponding input name format.
   *
   * @param column
   *     The column to be transformed.
   * @return The input name corresponding to the given column's database column name.
   */
  public static String getInpName(Column column) {
    return getInpName(column.getDBColumnName());
  }
}
