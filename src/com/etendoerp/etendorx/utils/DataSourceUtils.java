package com.etendoerp.etendorx.utils;


import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import com.etendoerp.etendorx.services.OpenAPINotFoundThrowable;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.openapi.data.OpenAPIRequest;

/**
 * Utility class for data source operations.
 * Another usefull utility for this is the {@link com.smf.mobile.utils.webservices.WindowUtils} class.
 * Other class is {@link com.smf.mobile.utils.webservices.Window}
 */
public class DataSourceUtils {

  private static final Logger log = LogManager.getLogger();
  public static final String CLASSIC_VALUE = "classicValue";
  public static final String REFERENCE_SEARCH = "30";
  public static final String REFERENCE_TABLEDIR = "19";
  public static final String REFERENCE_TABLE = "18";
  public static final String REFERENCE_ID = "13";

  /*
   * Private constructor to prevent instantiation.
   */
  private DataSourceUtils() {
  }

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
   *     The database table name.
   * @param dbColumnName
   *     The database column name.
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
    log.debug(String.format("HQL column name: %s, type: %s ", entAl, type));
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

  /**
   * Converts the values in the given JSON object to HQL format based on the provided column types.
   * <p>
   * This method iterates over the keys of the given JSON object, retrieves the corresponding type from the column types map,
   * and converts each value to the appropriate HQL format.
   *
   * @param jsonBodyToSave
   *     The JSON object containing the values to be converted.
   * @param columnTypes
   *     A map of column names to their corresponding types.
   * @return A new JSON object with the values converted to HQL format.
   * @throws JSONException
   *     If there is an error during JSON processing.
   * @throws ParseException
   *     If there is an error during date parsing.
   */
  public static JSONObject valuesConvertion(JSONObject jsonBodyToSave,
      Map<String, String> columnTypes) throws JSONException, ParseException {
    JSONObject newJsonBodyToSave = new JSONObject();
    var it = jsonBodyToSave.keys();
    while (it.hasNext()) {
      String key = (String) it.next();
      String value = jsonBodyToSave.getString(key);
      String type = columnTypes.get(key);
      newJsonBodyToSave.put(key, convertValueFromInputToHQL(type, value));
    }
    return newJsonBodyToSave;
  }

  /**
   * Converts a value from input format to HQL format based on the specified type.
   * <p>
   * This method converts the given value to the appropriate HQL format based on the provided type.
   *
   * @param type
   *     The type of the value to be converted.
   * @param value
   *     The value to be converted.
   * @return The converted value in HQL format.
   * @throws ParseException
   *     If there is an error during date parsing.
   */
  private static Object convertValueFromInputToHQL(String type, String value) throws ParseException {

    if (type == null) {
      return value;
    }
    switch (type) {
      case "BigDecimal":
        return new BigDecimal(value);
      case "Long":
        return Long.parseLong(value);
      case "Boolean":
        return Boolean.parseBoolean(value) || StringUtils.equalsIgnoreCase(value, "Y");
      case "Date":
        return getformatedDate(value, true);
      case "Datetime":
        return getformatedDatetime(value, true);
      default:
        return value;
    }

  }

  /**
   * Formats a datetime string to the standard HQL datetime format.
   * <p>
   * This method converts the given datetime string to the standard HQL datetime format.
   *
   * @param value
   *     The datetime string to be formatted.
   * @param inp2hql
   *     The boolean value indicates if the datetime is from the input format to the HQL format. If false,
   *     the datetime is from the HQL format to the input format.
   * @return The formatted datetime string.
   * @throws ParseException
   *     If there is an error during date parsing.
   */
  private static String getformatedDatetime(String value, boolean inp2hql) throws ParseException {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    SimpleDateFormat sdfInp = new SimpleDateFormat(props.getProperty("dateTimeFormat.java"));
    SimpleDateFormat sdfHql = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
    if (inp2hql) {
      return sdfHql.format(sdfInp.parse(value));
    } else {
      return sdfInp.format(sdfHql.parse(value));
    }
  }

  /**
   * Formats a date string to the standard HQL date format.
   * <p>
   * This method converts the given date string to the standard HQL date format.
   *
   * @param value
   *     The date string to be formatted.
   * @param inp2hql
   *     The boolean value indicates if the date is from the input format to the HQL format. If false,
   *     the date is from the HQL format to the input format.
   * @return The formatted date string.
   * @throws ParseException
   *     If there is an error during date parsing.
   */
  private static String getformatedDate(String value, boolean inp2hql) throws ParseException {

    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    SimpleDateFormat sdfInp = new SimpleDateFormat(props.getProperty("dateFormat.java"));
    SimpleDateFormat sdfHql = new SimpleDateFormat("yyyy-MM-dd");
    if (inp2hql) {
      return sdfHql.format(sdfInp.parse(value));
    } else {
      return sdfInp.format(sdfHql.parse(value));
    }
  }

  /**
   * Applies changes from one JSON object to another.
   * <p>
   * This method iterates over the keys of the given JSON object and updates the preexistent data with the new values.
   *
   * @param preexistentData
   *     The original JSON object to be updated.
   * @param jsonBodyToApply
   *     The JSON object containing the new values to be applied.
   * @return The updated JSON object.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  public static JSONObject applyChanges(JSONObject preexistentData, JSONObject jsonBodyToApply) throws JSONException {
    var it = jsonBodyToApply.keys();
    while (it.hasNext()) {
      String key = (String) it.next();
      preexistentData.put(key, jsonBodyToApply.get(key));
    }
    return preexistentData;
  }

  /**
   * Loads caches for normalized and input format keys.
   * <p>
   * This method populates three maps with normalized, input format, and database column name to input format key mappings based on the provided field list.
   *
   * @param fieldList
   *     A list of RequestField objects containing field information.
   * @param norm2input
   *     A map to store normalized to input format key mappings.
   * @param input2norm
   *     A map to store input to normalized format key mappings.
   * @param dbname2input
   *     A map to store database column name to input format key mappings.
   * @param columnTypes
   *     A map to store column types.
   */
  public static void loadCaches(List<RequestField> fieldList, LinkedHashMap<String, String> norm2input,
      Map<String, String> input2norm, Map<String, String> dbname2input, Map<String, String> columnTypes
  ) {
    try {
      OBContext.setAdminMode();
      for (RequestField field : fieldList) {
        String columnName = field.getDBColumnName();
        String[] hqlColumnNameAndtype = getHQLColumnName(field.getColumn());
        String normalizedName = hqlColumnNameAndtype[0];
        String inpName = getInpName(columnName);
        norm2input.put(normalizedName, inpName);
        input2norm.put(inpName, normalizedName);
        dbname2input.put(columnName, inpName);
        columnTypes.put(normalizedName, hqlColumnNameAndtype[1]);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Retrieves the parent ID from the given tab and data.
   * <p>
   * This method finds the parent ID by checking the data properties of the parent columns in the tab.
   *
   * @param tab
   *     The Tab object containing field information.
   * @param data
   *     The JSONObject containing data to be checked for parent properties.
   * @return The parent ID if found, otherwise null.
   */
  public static String getParentId(Tab tab, JSONObject data) {
    try {
      OBContext.setAdminMode(false);
      List<String> dataPropertiesOfParents = tab.getADFieldList().stream().filter(
          field -> field.getColumn() != null && field.getColumn().isLinkToParentColumn()).map(
          f -> getHQLColumnName(f)[0]).collect(Collectors.toList());
      for (String parentProperty : dataPropertiesOfParents) {
        if (data.has(parentProperty)) {
          return data.optString(parentProperty);
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Retrieves the parent ID from the given tab and data.
   * <p>
   * This method finds the parent ID by checking the data properties of the parent columns in the tab.
   *
   * @param tab
   *     The Tab object containing field information.
   * @return The parent ID if found, otherwise null.
   */
  public static List<String> getParentProperties(Tab tab) {
    try {
      OBContext.setAdminMode(false);
      return tab.getADFieldList().stream().filter(
          field -> field.getColumn() != null && field.getColumn().isLinkToParentColumn()).filter(
          field -> isParentRecordProperty(field, tab)).map(f -> getHQLColumnName(f)[0]).collect(Collectors.toList());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method taken from {@link com.smf.mobile.utils.webservices.Window}.
   * If this method changes to public, this method can be removed, and the method can be used directly.
   */
  private static boolean isParentRecordProperty(Field field, Tab tab) {
    Entity parentEntity = null;

    if (!field.getColumn().isLinkToParentColumn()) {
      return false;
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);

    if (parentTab != null && ApplicationConstants.TABLEBASEDTABLE.equals(parentTab.getTable().getDataOriginType())) {
      parentEntity = ModelProvider.getInstance().getEntityByTableName(parentTab.getTable().getDBTableName());
    }

    Property property = KernelUtils.getProperty(field);
    Entity referencedEntity = property.getReferencedProperty().getEntity();
    return referencedEntity.equals(parentEntity);

  }

  /**
   * Applies column values from the form initialization response to the new record data.
   * <p>
   * This method iterates over the column values in the form initialization response,
   * converts the keys using the provided map, and updates the new record data with the values.
   *
   * @param formInitResponse
   *     The JSON object containing the form initialization response.
   * @param mapConvertionKey
   *     A map for converting old keys to new keys.
   * @param dataFromNewRecord
   *     The JSON object representing the new record data to be updated.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  public static void applyColumnValues(JSONObject formInitResponse, Map<String, String> mapConvertionKey,
      JSONObject dataFromNewRecord) throws JSONException {
    var columnValues = formInitResponse.getJSONObject("columnValues");
    var keys = columnValues.keys();
    while (keys.hasNext()) {
      String oldKey = (String) keys.next();
      String inpkey = mapConvertionKey.getOrDefault(oldKey, oldKey);
      JSONObject value = columnValues.getJSONObject(oldKey);
      Object val = getClassicValue(value);
      if (val != null && (!(val instanceof String) || org.apache.commons.lang3.StringUtils.isNotEmpty((String) val))) {
        dataFromNewRecord.put(inpkey, val);
      }
    }
  }

  /**
   * Retrieves the classic value from the given JSON object.
   * <p>
   * This method extracts the classic value from the provided JSON object using the specified date patterns.
   *
   * @param value
   *     The JSON object containing the value.
   * @return The classic value extracted from the JSON object.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  private static Object getClassicValue(JSONObject value) throws JSONException {
    return getValueFromItem(value, "yyyy-MM-dd", "dd-MM-yyyy", true);
  }

  /**
   * Retrieves the list of columns for the given tab.
   * <p>
   * This method refreshes the table associated with the tab and returns the list of columns.
   *
   * @param tab
   *     The Tab object containing the table information.
   * @return The list of columns for the given tab.
   */
  public static List<Column> getAdColumnList(Tab tab) {
    Table table = tab.getTable();
    OBDal.getInstance().refresh(table);
    return table.getADColumnList();
  }

  /**
   * Retrieves the value from the given JSON object based on the specified date patterns.
   * <p>
   * This method extracts the value from the provided JSON object, converting date values if necessary.
   *
   * @param item
   *     The JSON object containing the value.
   * @param patternDateFrom
   *     The date pattern to convert from.
   * @param patternDateTo
   *     The date pattern to convert to.
   * @param directFromClassic
   *     If true, retrieves the classic value directly.
   * @return The extracted value from the JSON object.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  public static Object getValueFromItem(JSONObject item, String patternDateFrom, String patternDateTo,
      boolean directFromClassic) throws JSONException {
    if (directFromClassic) {
      return (item.has(CLASSIC_VALUE) && org.apache.commons.lang3.StringUtils.isNotEmpty(
          item.optString(CLASSIC_VALUE))) ? item.get(CLASSIC_VALUE) : null;
    }
    // if the item has a property value with type long, use this value, if not use the classicValue. We don't know the type of the value
    if (item.has("value")) {
      Object value = item.get("value");
      if (value instanceof Long || value instanceof Integer || value instanceof Double) {
        return value;
      }
      if (!(value instanceof String)) {
        return value.toString();
      }
      // check if the value is a date in patternDateFrom, if so, convert it to patternDateTo
      // for example, if the date is in format yyyy-MM-dd, and we need it in format dd-MM-yyyy
      SimpleDateFormat sdfFrom = new SimpleDateFormat(patternDateFrom);
      SimpleDateFormat sdfTo = new SimpleDateFormat(patternDateTo);
      try {
        return sdfTo.format(sdfFrom.parse(value.toString()));
      } catch (Exception e) {
        return value;
      }
    }
    return item.has(CLASSIC_VALUE) ? item.get(CLASSIC_VALUE) : null;
  }

  /**
   * Extracts the data source and ID from the request URI.
   *
   * @param requestURI
   *     The request URI to be processed.
   * @return the extracted parts, being the first part the data source name and the second part the ID
   */
  public static String[] extractDataSourceAndID(String requestURI) {
    String[] parts = requestURI.split("/");
    if (parts.length < 1 || parts.length > 3) {
      throw new IllegalArgumentException("Invalid request URI: " + requestURI);
    }

    String dataSourceName = parts[1];
    String id = (parts.length > 2) ? parts[2] : null;

    return id != null ? new String[]{ dataSourceName, id } : new String[]{ dataSourceName };
  }

  /**
   * Retrieves the Tab object associated with the given data source name.
   * <p>
   * This method queries the database for an OpenAPIRequest with the specified data source name,
   * and returns the related Tab object. If no matching OpenAPIRequest is found, or if the request
   * does not have any related tabs, an OpenAPINotFoundThrowable is thrown.
   *
   * @param dataSourceName
   *     The name of the data source to look up.
   * @return The Tab object associated with the given data source name.
   * @throws OpenAPINotFoundThrowable
   *     If no matching OpenAPIRequest is found, or if the request does not have any related tabs.
   */
  public static Tab getTabByDataSourceName(String dataSourceName) throws OpenAPINotFoundThrowable {
    try {
      OBContext.setAdminMode();
      OpenAPIRequest apiRequest = (OpenAPIRequest) OBDal.getInstance().createCriteria(OpenAPIRequest.class).add(
          Restrictions.eq("name", dataSourceName)).setMaxResults(1).uniqueResult();

      if (apiRequest == null) {
        throw new OpenAPINotFoundThrowable("OpenAPI request not found: " + dataSourceName);
      }

      if (apiRequest.getETRXOpenAPITabList().isEmpty()) {
        throw new OpenAPINotFoundThrowable("OpenAPI request does not have any related tabs: " + dataSourceName);
      }

      return apiRequest.getETRXOpenAPITabList().get(0).getRelatedTabs();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Converts a value from HQL format to input format based on the specified type.
   * <p>
   * This method converts the given value to the appropriate input format based on the provided type.
   *
   * @param o
   *     The value to be converted.
   * @param type
   *     The type of the value to be converted.
   * @return The converted value in input format.
   * @throws ParseException
   *     If there is an error during date parsing.
   */
  public static String valueConvertToInputFormat(Object o, String type) throws ParseException {
    if (type == null) {
      return o.toString();
    }
    switch (type) {
      case "BigDecimal":
        return o.toString();
      case "Long":
        if (o instanceof Integer) {
          return Integer.toString((Integer) o);
        } else {
          return Long.toString((Long) o);
        }
      case "Boolean":
        return (Boolean) o ? "Y" : "N";
      case "Date":
        return getformatedDate(o.toString(), false);
      case "Datetime":
        return getformatedDatetime(o.toString(), false);
      default:
        return o.toString();
    }
  }

  /**
   * Checks if the given reference is to another table.
   * <p>
   * This method determines if the provided reference ID matches any of the predefined reference IDs
   * that indicate a reference to another table.
   *
   * @param ref
   *     The Reference object to be checked.
   * @return true if the reference is to another table, false otherwise.
   */
  public static boolean isReferenceToAnotherTable(Reference ref) {
    return StringUtils.equals(ref.getId(), REFERENCE_SEARCH) // Ref: Search
        || StringUtils.equals(ref.getId(), REFERENCE_TABLEDIR) || StringUtils.equals(ref.getId(),
        REFERENCE_TABLE) || StringUtils.equals(ref.getId(), REFERENCE_ID);
  }
}
