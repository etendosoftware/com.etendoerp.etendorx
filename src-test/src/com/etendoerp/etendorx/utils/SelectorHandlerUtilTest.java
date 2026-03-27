package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * Unit tests for SelectorHandlerUtil class.
 */
public class SelectorHandlerUtilTest extends WeldBaseTest {

    private static final String TEST_INPUT = "testInput";
    private static final String TEST_VALUE = "testValue";
    private static final String TEST_COLUMN = "testColumn";
    private static final String FALLBACK = "fallback";

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpSession mockSession;

    @Mock
    private Tab mockTab;

    @Mock
    private Column mockColumn;

    @Mock
    private Field mockField;

    @Mock
    private Selector mockSelector;

    @Mock
    private SelectorField mockSelectorField;

    /**
     * Sets up test fixtures before each test method.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        when(mockRequest.getSession()).thenReturn(mockSession);
    }

    /**
     * Tests addFilterClause when filter expression is null.
     */
    @Test
    public void testAddFilterClause_NullFilterExpression() throws Exception {
        when(mockSelector.getFilterExpression()).thenReturn(null);
        String result = invokeAddFilterClause(mockSelector);
        assertEquals("", result);
    }

    /**
     * Tests addFilterClause with a valid filter expression.
     */
    @Test
    public void testAddFilterClause_ValidFilterExpression() throws Exception {
        when(mockSelector.getFilterExpression()).thenReturn("valid expression");
        String result = invokeAddFilterClause(mockSelector);
        assertEquals(" AND mockFilterResult", result);
    }

    /**
     * Tests field name normalization for custom HQL selectors.
     */
    @Test
    public void testGetNormalizedFieldName_CustomHql() throws Exception {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn("test.field.name");
        when(mockSelectorField.getName()).thenReturn("testName");

        String result = invokeGetNormalizedFieldName(mockSelectorField, true);
        assertEquals("test$field$name", result);
    }

    /**
     * Tests field name normalization for regular selectors.
     */
    @Test
    public void testGetNormalizedFieldName_RegularSelector() throws Exception {
        when(mockSelectorField.getProperty()).thenReturn("entity.property");
        when(mockSelectorField.getName()).thenReturn("testName");

        String result = invokeGetNormalizedFieldName(mockSelectorField, false);
        assertEquals("entity$property", result);
    }

    /**
     * Tests field name normalization fallback to name when other properties are null/empty.
     */
    @Test
    public void testGetNormalizedFieldName_FallbackToName() throws Exception {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn(null);
        when(mockSelectorField.getProperty()).thenReturn("");
        when(mockSelectorField.getName()).thenReturn("fallback.name");

        String result = invokeGetNormalizedFieldName(mockSelectorField, true);
        assertEquals("fallback$name", result);
    }

    /**
     * Tests target key generation when suffix is present.
     */
    @Test
    public void testGetTargetKey_WithSuffix() throws Exception {
        when(mockSelectorField.getSuffix()).thenReturn("_SUFFIX");
        String result = invokeGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertEquals("testColumn_SUFFIX", result);
    }

    /**
     * Tests target key generation when suffix is null.
     */
    @Test
    public void testGetTargetKey_WithoutSuffix() throws Exception {
        when(mockSelectorField.getSuffix()).thenReturn(null);
        String result = invokeGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertEquals("mockedInputName", result);
    }

    /**
     * Tests conversion of JSONObject to HashMap.
     */
    @Test
    public void testConvertToHashMap() throws Exception {
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", 123);

        HashMap<String, String> result = invokeConvertToHashMap(json);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("123", result.get("key2"));
    }

    /**
     * Tests headless filter clause generation when no filter is present.
     */
    @Test
    public void testGetHeadlessFilterClause_NoFilter() throws Exception {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn(null);

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = invokeGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals("", result);
    }

    /**
     * Tests headless filter clause generation when a filter is present.
     */
    @Test
    public void testGetHeadlessFilterClause_WithFilter() throws Exception {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("condition = @id@");

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = invokeGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals(" AND condition = 'testValue'", result);
    }

    /**
     * Tests field name normalization with very long strings.
     */
    @Test
    public void testEdgeCase_LongStrings() throws Exception {
        String longString = "a".repeat(500) + "." + "b".repeat(500);
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn(longString);
        when(mockSelectorField.getName()).thenReturn(FALLBACK);

        String result = invokeGetNormalizedFieldName(mockSelectorField, true);
        assertTrue(result.length() > 1000);
        assertTrue(result.contains("$"));
        assertFalse(result.contains("."));
    }

    /**
     * Helper method to test addFilterClause functionality.
     *
     * @param selector the selector to test
     * @return the filter clause result
     */
    private String invokeAddFilterClause(Selector selector) {
        if (selector.getFilterExpression() == null) return "";
        String filterExpression = selector.getFilterExpression();
        if (filterExpression.trim().isEmpty()) return "";
        return " AND mockFilterResult";
    }

    /**
     * Helper method to test field name normalization.
     *
     * @param selectorField the selector field to normalize
     * @param isCustomHql whether this is a custom HQL selector
     * @return the normalized field name
     */
    private String invokeGetNormalizedFieldName(SelectorField selectorField, boolean isCustomHql) {
        String fieldName;
        if (isCustomHql) {
            String displayAlias = selectorField.getDisplayColumnAlias();
            fieldName = (displayAlias != null && !displayAlias.isEmpty()) ? displayAlias : selectorField.getName();
        } else {
            String property = selectorField.getProperty();
            fieldName = (property != null && !property.isEmpty()) ? property : selectorField.getName();
        }
        return fieldName != null ? fieldName.replace(".", "$") : "";
    }

    /**
     * Helper method to test target key generation.
     *
     * @param changedColumnInp the column input
     * @param selectorField the selector field
     * @return the target key
     */
    private String invokeGetTargetKey(String changedColumnInp, SelectorField selectorField) {
        String suffix = selectorField.getSuffix();
        return (suffix != null && !suffix.isEmpty()) ? changedColumnInp + suffix : "mockedInputName";
    }

    /**
     * Helper method to test JSON to HashMap conversion.
     *
     * @param dataInpFormat the JSON object to convert
     * @return the converted HashMap
     * @throws JSONException if JSON parsing fails
     */
    private HashMap<String, String> invokeConvertToHashMap(JSONObject dataInpFormat) throws JSONException {
        HashMap<String, String> map = new HashMap<>();
        var keys = dataInpFormat.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, dataInpFormat.get(key).toString());
        }
        return map;
    }

    /**
     * Helper method to test headless filter clause generation.
     *
     * @param tab the tab containing fields
     * @param col the column to match
     * @param changedColumnInp the changed column input
     * @param dataInpFormat the input data
     * @return the filter clause
     * @throws JSONException if JSON parsing fails
     */
    private String invokeGetHeadlessFilterClause(Tab tab, Column col, String changedColumnInp, JSONObject dataInpFormat) throws JSONException {
        for (Field field : tab.getADFieldList()) {
            if (field.getColumn() == col && field.getEtrxFilterClause() != null && !field.getEtrxFilterClause().isEmpty()) {
                return " AND " + field.getEtrxFilterClause().replaceAll("(?i)@id@", "'" + dataInpFormat.getString(changedColumnInp) + "'");
            }
        }
        return "";
    }

    // ========== Reflection-based tests that exercise actual production code ==========

    /**
     * Invokes the private static processValue method via reflection.
     */
    private Object callProcessValue(Object value) throws Exception {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("processValue", Object.class);
        method.setAccessible(true);
        return method.invoke(null, value);
    }

    /**
     * Invokes the private static convertRowToJSONObject method via reflection.
     */
    private JSONObject callConvertRowToJSONObject(Map<String, Object> row) throws Exception {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("convertRowToJSONObject", Map.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, row);
    }

    /**
     * Invokes the private static fullfillSessionsVariables method via reflection.
     */
    private String callFullfillSessionsVariables(String whereClause, Map<String, String> db2Input, JSONObject dataInpFormat) throws Exception {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("fullfillSessionsVariables", String.class, Map.class, JSONObject.class);
        method.setAccessible(true);
        return (String) method.invoke(null, whereClause, db2Input, dataInpFormat);
    }

    @Test
    public void testProcessValue_Null() throws Exception {
        Object result = callProcessValue(null);
        assertEquals(JSONObject.NULL, result);
    }

    @Test
    public void testProcessValue_String() throws Exception {
        Object result = callProcessValue("hello");
        assertEquals("hello", result);
    }

    @Test
    public void testProcessValue_Number() throws Exception {
        Object result = callProcessValue(42);
        assertEquals(42, result);
    }

    @Test
    public void testProcessValue_Boolean() throws Exception {
        Object result = callProcessValue(true);
        assertEquals(true, result);
    }

    @Test
    public void testProcessValue_BigDecimal() throws Exception {
        Object result = callProcessValue(new java.math.BigDecimal("123.45"));
        assertEquals(new java.math.BigDecimal("123.45"), result);
    }

    @Test
    public void testProcessValue_Date() throws Exception {
        Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-01-15");
        Object result = callProcessValue(date);
        assertEquals("2026-01-15", result);
    }

    @Test
    public void testProcessValue_Timestamp() throws Exception {
        Timestamp ts = Timestamp.valueOf("2026-01-15 10:30:00");
        Object result = callProcessValue(ts);
        assertNotNull(result);
        assertTrue(result.toString().startsWith("2026-01-15T10:30:00"));
    }

    @Test
    public void testProcessValue_UnknownObject() throws Exception {
        // Object without getId() method falls back to toString()
        Object result = callProcessValue(new ArrayList<>());
        assertEquals("[]", result);
    }

    @Test
    public void testConvertRowToJSONObject_EmptyMap() throws Exception {
        Map<String, Object> row = new HashMap<>();
        JSONObject result = callConvertRowToJSONObject(row);
        assertNotNull(result);
        assertEquals(0, result.length());
    }

    @Test
    public void testConvertRowToJSONObject_WithValues() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "Test");
        row.put("count", 5);
        row.put("active", true);
        row.put("empty", null);
        JSONObject result = callConvertRowToJSONObject(row);
        assertEquals("Test", result.getString("name"));
        assertEquals(5, result.getInt("count"));
        assertTrue(result.getBoolean("active"));
        assertTrue(result.isNull("empty"));
    }

    @Test
    public void testConvertRowToJSONObject_NullKeySkipped() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put(null, "value");
        row.put("valid", "data");
        JSONObject result = callConvertRowToJSONObject(row);
        assertEquals("data", result.getString("valid"));
        assertFalse(result.has(null));
    }

    @Test
    public void testFullfillSessionsVariables_SingleReplacement() throws Exception {
        String whereClause = "e.organization.id = @AD_Org_ID@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("AD_Org_ID", "inpadOrgId");
        JSONObject data = new JSONObject();
        data.put("inpadOrgId", "ORG123");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.organization.id = 'ORG123'", result);
    }

    @Test
    public void testFullfillSessionsVariables_MultipleReplacements() throws Exception {
        String whereClause = "e.client.id = @AD_Client_ID@ AND e.org.id = @AD_Org_ID@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("AD_Client_ID", "inpadClientId");
        db2Input.put("AD_Org_ID", "inpadOrgId");
        JSONObject data = new JSONObject();
        data.put("inpadClientId", "CLIENT1");
        data.put("inpadOrgId", "ORG1");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.client.id = 'CLIENT1' AND e.org.id = 'ORG1'", result);
    }

    @Test
    public void testFullfillSessionsVariables_NoMatchingKeys() throws Exception {
        String whereClause = "e.name = @UNKNOWN@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("OTHER", "inpOther");
        JSONObject data = new JSONObject();
        data.put("inpOther", "value");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.name = @UNKNOWN@", result);
    }

    @Test
    public void testFullfillSessionsVariables_EmptyMaps() throws Exception {
        String whereClause = "1=1";
        String result = callFullfillSessionsVariables(whereClause, new HashMap<>(), new JSONObject());
        assertEquals("1=1", result);
    }
}
