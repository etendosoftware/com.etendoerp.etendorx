package com.etendoerp.etendorx.utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.session.OBPropertiesProvider;
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
  public static String[] getHQLColumnName(Field field) {
    return getHQLColumnName(field.getColumn());
  }

  /**
   * Retrieves the HQL column name for a given column.
   *
   * @param fieldColumn
   *     The column for which to retrieve the HQL column name.
   * @return The HQL column name.
   */
  public static String[] getHQLColumnName(Column fieldColumn) {
    if (fieldColumn == null || fieldColumn.getTable() == null) {
      return new String[]{ "null", "String" };
    }
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
  public static String[] getHQLColumnName(boolean exceptionOnFail, String dbTableName, String dbColumnName) {
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


    var type = "String";
    if (prop.isPrimitive()) {
      type = prop.getPrimitiveType().getSimpleName();
    }
    log.info(String.format("HQL column name: %s, type: %s ", entAl, type));
    return new String[]{ entAl, type };
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

  /**
   * Converts the keys of a JSONObject using a provided map for conversion.
   * <p>
   * This method iterates over the keys of the given JSONObject and converts each key using the provided map.
   * If a key has a corresponding new key in the map, the value is put in the new key. Otherwise, the original key is used.
   *
   * @param data
   *     The original JSONObject whose keys need to be converted.
   * @param mapForConvertion
   *     A map containing the original keys and their corresponding new keys.
   * @return A new JSONObject with the keys converted.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  public static JSONObject keyConvertion(JSONObject data, Map<String, String> mapForConvertion) throws JSONException {
    // Receives the data and the map to convert the keys
    JSONObject newData = new JSONObject();
    var it = data.keys();
    while (it.hasNext()) {
      String key = (String) it.next();
      String newKey = mapForConvertion.get(key);
      if (newKey != null) {
        newData.put(newKey, data.get(key));
      } else {
        newData.put(key, data.get(key));
      }
    }
    return newData;
  }

  public static JSONObject valuesConvertion(JSONObject jsonBodyToSave,
      Map<String, String> columnTypes) throws JSONException, ParseException {
    JSONObject newJsonBodyToSave = new JSONObject();
    var it = jsonBodyToSave.keys();
    while (it.hasNext()) {
      String key = (String) it.next();
      String value = jsonBodyToSave.getString(key);
      String type = columnTypes.get(key);
      if (type != null) {
        switch (type) {
          case "BigDecimal":
            newJsonBodyToSave.put(key, new BigDecimal(value));
            break;
          case "Long":
            newJsonBodyToSave.put(key, Long.parseLong(value));
            break;
          case "Boolean":
            newJsonBodyToSave.put(key, Boolean.parseBoolean(value));
            break;
          case "Date":
            newJsonBodyToSave.put(key, getformatedDate(value));
            break;
          case "Datetime":
            newJsonBodyToSave.put(key, getformatedDatetime(value));
            break;
          default:
            newJsonBodyToSave.put(key, value);
            break;

        }
      } else {
        newJsonBodyToSave.put(key, value);
      }
    }
    return newJsonBodyToSave;

  }

  private static String getformatedDatetime(String value) throws ParseException {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    SimpleDateFormat sdfFrom = new SimpleDateFormat(props.getProperty("dateTimeFormat.java"));
    SimpleDateFormat sdfTo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
    return sdfTo.format(sdfFrom.parse(value));

  }

  private static String getformatedDate(String value) throws ParseException {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    SimpleDateFormat sdfFrom = new SimpleDateFormat(props.getProperty("dateFormat.java"));
    SimpleDateFormat sdfTo = new SimpleDateFormat("yyyy-MM-dd");
    return sdfTo.format(sdfFrom.parse(value));
  }
}
