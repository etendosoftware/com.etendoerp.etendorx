package com.etendoerp.etendorx.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectorHandlerUtil {

    private static final Logger log = LogManager.getLogger();
    public static final String _START_ROW = "_startRow";
    public static final String _END_ROW = "_endRow";
    public static final String RESPONSE = "response";

    /*
     * Private constructor to prevent instantiation.
     */
    private SelectorHandlerUtil() {
    }

    /**
     * Handles the column selector for a specified column.
     * <p>
     * This method processes the column selector for the specified column, evaluates the filter clause,
     * and updates the data input format with the selected values.
     *
     * @param request
     *     The HttpServletRequest object.
     * @param tab
     *     The Tab object associated with the selector.
     * @param dataInpFormat
     *     The JSON object containing the data input format.
     * @param changedColumnN
     *     The name of the column that has changed.
     * @param changedColumnInp
     *     The input format name of the column that has changed.
     * @param db2Input
     *     A map of database column names to input format names.
     * @throws JSONException
     *     If there is an error during JSON processing.
     * @throws ScriptException
     *     If there is an error during the evaluation of the filter expression.
     */
    public static void handleColumnSelector(HttpServletRequest request, Tab tab, JSONObject dataInpFormat,
                                            String changedColumnN, String changedColumnInp,
                                            Map<String, String> db2Input) {
        try {
            OBContext.setAdminMode();
            Column col = getColumnByHQLName(tab, changedColumnN);
            if (col == null) {
                throw new OBException(OBMessageUtils.messageBD("ETRX_ColumnNotFound"));
            }
            if (StringUtils.equals(col.getReference().getId(), "95E2A8B50A254B2AAE6774B8C2F28120")) {
                handleCustomDefinedSelector(request, tab, dataInpFormat, changedColumnInp, db2Input, col);
                return;
            }
            if (!StringUtils.equals(col.getReference().getId(), "30")) {
                return;
            }  //is type search.
            Reference reference = col.getReferenceSearchKey();
            if (reference.getOBUISELSelectorList().isEmpty()) {
                throw new OBException(OBMessageUtils.messageBD("ETRX_ReferenceNotFound"));
            }

            String headlessFilterClause = getHeadlessFilterClause(tab, col, changedColumnInp, dataInpFormat);
            org.openbravo.model.ad.domain.Selector selectorValidation = reference.getADSelectorList().get(0);
            Selector selectorDefined = reference.getOBUISELSelectorList().get(0);
            DefaultDataSourceService dataSourceService = new DefaultDataSourceService();
            HashMap<String, String> convertToHashMAp = convertToHashMAp(dataInpFormat);
            OBDal.getInstance().refresh(selectorDefined);
            convertToHashMAp.put("_entityName", selectorDefined.getTable().getJavaClassName());
            String whereClauseAndFilters = selectorDefined.getHQLWhereClause() + headlessFilterClause + addFilterClause(selectorDefined,
                    convertToHashMAp, tab, request);
            whereClauseAndFilters = fullfillSessionsVariables(whereClauseAndFilters, db2Input, dataInpFormat);
            convertToHashMAp.put("whereAndFilterClause", whereClauseAndFilters);
            convertToHashMAp.put("dataSourceName", selectorDefined.getTable().getJavaClassName());
            convertToHashMAp.put("_selectorDefinitionId", selectorDefined.getId());
            convertToHashMAp.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
            convertToHashMAp.put("IsSelectorItem", "true");
            convertToHashMAp.put("_extraProperties", getExtraProperties(selectorDefined));
            int iterations = 0;
            // we will search this record id
            String recordID = dataInpFormat.getString(changedColumnInp);
            //find the column of the table, that determines the "column" where the data is stored. For example, in the case of
            // a selector of "M_Product", the value stored in the column "M_Product_ID"
            Column valueColumn = getValueColumn(selectorValidation, selectorDefined);
            // ask for the name of the property where the record id is stored in the results
            String valueProperty = DataSourceUtils.getHQLColumnName(valueColumn)[0];

            JSONObject obj = searchForRecord(dataSourceService, convertToHashMAp, recordID, valueProperty);
            if (obj == null) {
                log.error("Record not found in selector");
                throw new OBException(OBMessageUtils.messageBD("ETRX_RecordNotFound"));
            }

            savePrefixFields(dataInpFormat, changedColumnInp, selectorDefined, obj, false);

        } catch (Exception e) {
            log.error("Error in handleColumnSelector", e);
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    /**
     * Retrieves the column by its HQL name from the given tab.
     * <p>
     * This method iterates over the columns of the tab and returns the column that matches the given HQL name.
     *
     * @param tab
     *     The Tab object containing the columns.
     * @param changedColumnN
     *     The HQL name of the column to be retrieved.
     * @return The Column object that matches the given HQL name, or null if not found.
     */
    private static Column getColumnByHQLName(Tab tab, String changedColumnN) {
        Column col = null;
        List<Column> adColumnList = DataSourceUtils.getAdColumnList(tab);
        for (Column column : adColumnList) {
            if (StringUtils.equals((DataSourceUtils.getHQLColumnName(column))[0], changedColumnN)) {
                col = column;
                break;
            }
        }
        return col;
    }

    /**
     * Handles the column selector for definedSelector type (95E2A8B50A254B2AAE6774B8C2F28120).
     */
    private static void handleCustomDefinedSelector(HttpServletRequest request, Tab tab, JSONObject dataInpFormat,
                                                    String changedColumnInp, Map<String, String> db2Input, Column col) {
        try {
            OBContext.setAdminMode();
            Reference reference = col.getReferenceSearchKey();
            if (reference.getOBUISELSelectorList().isEmpty()) {
                throw new OBException(OBMessageUtils.messageBD("ETRX_ReferenceNotFound"));
            }

            Selector selectorDefined = reference.getOBUISELSelectorList().get(0);
            if (!selectorDefined.isCustomQuery() || selectorDefined.getOBUISELSelectorFieldList().isEmpty()) {
                return;
            }

            // Build the HQL query with filters
            String hqlQuery = buildHQLQuery(selectorDefined, tab, col, changedColumnInp, dataInpFormat, db2Input, request);

            // Execute query and find matching record
            String recordID = dataInpFormat.getString(changedColumnInp);
            JSONObject matchedRecord = executeHQLAndFindRecord(hqlQuery, recordID, selectorDefined);

            if (matchedRecord == null) {
                log.error("Record not found in selector");
                throw new OBException(OBMessageUtils.messageBD("ETRX_RecordNotFound"));
            }

            savePrefixFields(dataInpFormat, changedColumnInp, selectorDefined, matchedRecord, true);

        } catch (Exception e) {
            log.error("Error in handleCustomDefinedSelector", e);
            throw new OBException(OBMessageUtils.messageBD("ETRX_ErrorInSelectorColumn") + e.getMessage());
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    /**
     * Builds the filter clause for the given column based on the field's EtrxFilterClause.
     * <p>
     * If a field associated with the specified column has a non-null filter clause, this method
     * replaces occurrences of "@id@" (case-insensitive) with the value of the changed column from
     * the input format and returns it as part of an SQL WHERE clause.
     *
     * @param tab
     *     The tab containing the list of fields to search through.
     * @param col
     *     The column to find the associated field and filter clause.
     * @param changedColumnInp
     *     The input format name of the changed column.
     * @param dataInpFormat
     *     The JSON object containing the current input values.
     * @return
     *     The filter clause string, or an empty string if no applicable clause is found.
     * @throws JSONException
     *     If an error occurs while reading values from the JSON object.
     */
    private static String getHeadlessFilterClause(Tab tab, Column col, String changedColumnInp, JSONObject dataInpFormat) throws JSONException {
        for (Field field : tab.getADFieldList()) {
            if (field.getColumn() == col && !StringUtils.isEmpty(field.getEtrxFilterClause())) {
                return " AND " + field.getEtrxFilterClause()
                        .replaceAll("(?i)@id@", "'" + dataInpFormat.getString(changedColumnInp) + "'");
            }
        }
        return "";
    }

    /**
     * Converts a JSONObject to a HashMap.
     * <p>
     * This method iterates over the keys of the provided JSONObject and adds them to a HashMap.
     *
     * @param dataInpFormat
     *     The JSONObject to be converted.
     * @return A HashMap containing the key-value pairs from the JSONObject.
     * @throws JSONException
     *     If there is an error during JSON processing.
     */
    private static HashMap<String, String> convertToHashMAp(JSONObject dataInpFormat) throws JSONException {
        HashMap<String, String> map = new HashMap<>();
        var keys = dataInpFormat.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, dataInpFormat.get(key).toString());
        }
        return map;
    }

    /**
     * Adds a filter clause to the selector's query.
     * <p>
     * This method checks if the selector has a filter clause that uses OB. If it does,
     * the filter clause is evaluated in JavaScript and appended to the query.
     *
     * @param selector
     *     The selector object containing the filter expression.
     * @param hs1
     *     A map of parameters to be used in the filter expression.
     * @param tab
     *     The tab object associated with the selector.
     * @param request
     *     The HttpServletRequest object.
     * @return The filter clause to be appended to the query.
     * @throws ScriptException
     *     If there is an error during the evaluation of the filter expression.
     */
    private static String addFilterClause(Selector selector, HashMap<String, String> hs1, Tab tab,
                                          HttpServletRequest request) throws ScriptException {
        // Check if the selector has a filter expression before evaluating
        if (selector.getFilterExpression() == null) {
            return "";
        }
        
        // Check if the selector has a filter clause and it uses OB., because we need to evaluate in JS.
        var result = (String) ParameterUtils.getJSExpressionResult(hs1, request.getSession(),
                selector.getFilterExpression());
        if (StringUtils.isNotEmpty(result)) {
            return " AND " + result;
        }
        return result;
    }

    /**
     * Replaces session variables in the where clause and filters.
     * <p>
     * This method replaces placeholders in the where clause and filters with actual values from the provided
     * data input format.
     *
     * @param whereClauseAndFilters
     *     The where clause and filters containing placeholders.
     * @param db2Input
     *     A map of database column names to input format names.
     * @param dataInpFormat
     *     The JSON object containing the data input format.
     * @return The where clause and filters with placeholders replaced by actual values.
     * @throws JSONException
     *     If there is an error during JSON processing.
     */
    private static String fullfillSessionsVariables(String whereClauseAndFilters, Map<String, String> db2Input,
                                                    JSONObject dataInpFormat) throws JSONException {
        String result = whereClauseAndFilters;
        for (Map.Entry<String, String> entry : db2Input.entrySet()) {
            if (dataInpFormat.has(entry.getValue())) {
                result = StringUtils.replaceIgnoreCase(result, "@" + entry.getKey() + "@",
                        String.format("'%s'", dataInpFormat.get(entry.getValue())));
            }
        }
        return result;
    }

    /**
     * Retrieves the extra properties for the given selector.
     * <p>
     * This method collects the extra properties for the given selector, including the value field and outfields,
     * and returns them as a comma-separated string.
     *
     * @param selector
     *     The selector object.
     * @return A comma-separated string of extra properties for the given selector.
     */
    private static String getExtraProperties(Selector selector) {
        return selector.getOBUISELSelectorFieldList().stream().filter(
                sf -> selector.getValuefield() == sf || sf.isOutfield()).sorted(
                Comparator.comparing(SelectorField::getSortno)).map(
                sf -> StringUtils.replace(sf.getProperty(), ".", "$")).collect(Collectors.joining(","));
    }

    /**
     * Retrieves the value column for the given selector.
     * <p>
     * This method determines the value column based on the provided selector. If the selector is defined
     * and has a value field that is not a custom query, it returns the value field's column. Otherwise,
     * it returns the column from the selector validation.
     *
     * @param selectorValidation
     *     The selector validation object.
     * @param selectorDefined
     *     The defined selector object.
     * @return The value column for the given selector.
     */
    private static Column getValueColumn(org.openbravo.model.ad.domain.Selector selectorValidation,
                                         Selector selectorDefined) {
        if (selectorDefined != null && selectorDefined.getValuefield() != null && !selectorDefined.isCustomQuery()) {
            return selectorDefined.getValuefield().getColumn();
        } else {
            return selectorValidation.getColumn();
        }
    }

    /**
     * Searches for a record in the data source using pagination.
     */
    private static JSONObject searchForRecord(DefaultDataSourceService dataSourceService,
                                              HashMap<String, String> convertToHashMAp,
                                              String recordID, String valueProperty) throws JSONException {
        JSONObject obj = null;
        int totalRows = -1;
        int endRow = -1;
        int iterations = 0;

        while (obj == null && (totalRows == -1 || endRow < totalRows)) {
            convertToHashMAp.put(_START_ROW, String.valueOf((iterations * 100)));
            endRow = 100 + (iterations * 100);
            convertToHashMAp.put(_END_ROW, String.valueOf(endRow));
            String result = dataSourceService.fetch(convertToHashMAp);
            JSONObject resultJson = new JSONObject(result);
            if (totalRows == -1) {
                totalRows = resultJson.getJSONObject(RESPONSE).getInt("totalRows");
            }
            var arr = resultJson.getJSONObject(RESPONSE).getJSONArray("data");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject current = arr.getJSONObject(i);
                if (StringUtils.equals(current.getString(valueProperty), recordID)) {
                    obj = current;
                    break;
                }
            }
            iterations++;
        }
        return obj;
    }

    /**
     * Saves the prefix fields from the selector into the data input format.
     * <p>
     * This method iterates over the outfields of the selector and adds their values to the data input format.
     *
     * @param dataInpFormat
     *     The JSON object containing the data input format.
     * @param changedColumnInp
     *     The input format name of the column that has changed.
     * @param selectorDefined
     *     The defined selector object.
     * @param obj
     *     The JSON object containing the selector field values.
     * @throws JSONException
     *     If there is an error during JSON processing.
     */
    private static void savePrefixFields(JSONObject dataInpFormat, String changedColumnInp, Selector selectorDefined,
                                         JSONObject obj, boolean isCustomHql) throws JSONException {
        List<SelectorField> outfields = selectorDefined.getOBUISELSelectorFieldList().stream()
                .filter(SelectorField::isOutfield)
                .collect(Collectors.toList());
        
        for (SelectorField selectorField : outfields) {
            String normalizedName = getNormalizedFieldName(selectorField, isCustomHql);
            
            if (obj.has(normalizedName)) {
                String targetKey = getTargetKey(changedColumnInp, selectorField);
                dataInpFormat.put(targetKey, obj.get(normalizedName));
            }
        }
    }

    /**
     * Gets the normalized field name based on the selector field and whether it's a custom HQL query.
     *
     * @param selectorField The selector field to get the name from
     * @param isCustomHql Whether this is a custom HQL query
     * @return The normalized field name with dots replaced by dollar signs
     */
    private static String getNormalizedFieldName(SelectorField selectorField, boolean isCustomHql) {
        String fieldName;
        if (isCustomHql) {
            fieldName = StringUtils.isNotEmpty(selectorField.getDisplayColumnAlias()) 
                    ? selectorField.getDisplayColumnAlias() 
                    : selectorField.getName();
        } else {
            fieldName = StringUtils.isNotEmpty(selectorField.getProperty()) 
                    ? selectorField.getProperty() 
                    : selectorField.getName();
        }
        return fieldName.replace(".", "$");
    }

    /**
     * Gets the target key for storing the field value in the data input format.
     *
     * @param changedColumnInp The input format name of the changed column
     * @param selectorField The selector field
     * @return The target key for the data input format
     */
    private static String getTargetKey(String changedColumnInp, SelectorField selectorField) {
        if (!StringUtils.isEmpty(selectorField.getSuffix())) {
            return changedColumnInp + selectorField.getSuffix();
        } else {
            return DataSourceUtils.getInpName(selectorField.getColumn());
        }
    }

    /**
     * Builds the complete HQL query with all filters applied.
     */
    private static String buildHQLQuery(Selector selectorDefined, Tab tab, Column col, String changedColumnInp,
                                        JSONObject dataInpFormat, Map<String, String> db2Input, HttpServletRequest request) throws JSONException, ScriptException {
        String hqlQuery = selectorDefined.getHQL();
        String headlessFilterClause = getHeadlessFilterClause(tab, col, changedColumnInp, dataInpFormat);
        HashMap<String, String> convertToHashMAp = convertToHashMAp(dataInpFormat);
        String additionalFilterClause = addFilterClause(selectorDefined, convertToHashMAp, tab, request);
        String additionalFilters = headlessFilterClause + (additionalFilterClause != null ? additionalFilterClause : "");

        String result;
        if (StringUtils.isEmpty(additionalFilters.trim())) {
            result = hqlQuery.replace("and @additional_filters@", "").replace("@additional_filters@", "");
        } else {
            result = hqlQuery.replace("@additional_filters@", additionalFilters);
        }

        return fullfillSessionsVariables(result, db2Input, dataInpFormat);
    }

    /**
     * Executes HQL query and finds the matching record by ID.
     * Uses selector field information to map results with proper aliases
     */
    private static JSONObject executeHQLAndFindRecord(String hqlQuery, String recordID, Selector selectorDefined) {
        try {
            Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
            query.setFirstResult(0);
            query.setMaxResults(1000);
            query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = query.list();

            String valueField = selectorDefined.getValuefield().getDisplayColumnAlias();

            // Iterate through results to find the matching record
            for (Map<String, Object> row : results) {
                JSONObject obj = new JSONObject();

                // Convert Map to JSONObject, extracting IDs from entity objects
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (key != null) {
                        Object processedValue = processValue(value);
                        obj.put(key, processedValue);
                    }
                }

                // Check if this record matches the recordID we're looking for
                if (obj.has(valueField) && StringUtils.equals(obj.getString(valueField), recordID)) {
                    return obj;
                }
            }

            // Record not found
            return null;

        } catch (Exception e) {
            log.error("Error executing definedSelector HQL query: " + hqlQuery, e);
            throw new OBException(OBMessageUtils.messageBD("ETRX_ErrorExecutingHQL") + e.getMessage());
        }
    }

    /**
     * Processes a value from Hibernate query results.
     * Extracts ID from entity objects while preserving simple types and dates.
     */
    private static Object processValue(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        }

        // Handle simple types
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        // Handle dates with proper formatting
        if (value instanceof Date && !(value instanceof Timestamp)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.format((Date) value);
        }

        if (value instanceof Timestamp) {
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            return timestampFormat.format((Timestamp) value);
        }

        // For entity objects, try to extract the ID
        try {
            // Use reflection to get the getId() method
            java.lang.reflect.Method getIdMethod = value.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(value);
            return id != null ? id.toString() : JSONObject.NULL;
        } catch (Exception e) {
            // If we can't get the ID, return the string representation
            return value.toString();
        }
    }

}
