package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * All tests invoke actual production code via reflection to ensure SonarQube coverage.
 */
public class SelectorHandlerUtilTest extends WeldBaseTest {

    private static final String TEST_INPUT = "testInput";
    private static final String TEST_VALUE = "testValue";
    private static final String TEST_COLUMN = "testColumn";
    private static final String FALLBACK = "fallback";
    private static final String VALUE = "value";
    private static final String INP_AD_ORG_ID = "inpadOrgId";
    private static final String FALLBACK_NAME = "fallback.name";
    private static final String FALLBACK_NAME_NORMALIZED = "fallback$name";
    private static final String INP_C_BPARTNER_ID = "inpcBpartnerId";
    private static final String INP_C_BPARTNER_ID_DES = "inpcBpartnerId_DES";

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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        when(mockRequest.getSession()).thenReturn(mockSession);
    }

    // ========== Reflection helpers ==========

    private Object callProcessValue(Object value) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("processValue", Object.class);
        method.setAccessible(true);
        return method.invoke(null, value);
    }

    private JSONObject callConvertRowToJSONObject(Map<String, Object> row) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("convertRowToJSONObject", Map.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, row);
    }

    private String callFullfillSessionsVariables(String whereClause, Map<String, String> db2Input,
            JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("fullfillSessionsVariables",
                String.class, Map.class, JSONObject.class);
        method.setAccessible(true);
        return (String) method.invoke(null, whereClause, db2Input, dataInpFormat);
    }

    private HashMap<String, String> callConvertToHashMAp(JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("convertToHashMAp", JSONObject.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, String> result = (HashMap<String, String>) method.invoke(null, dataInpFormat);
        return result;
    }

    private String callGetNormalizedFieldName(SelectorField sf, boolean isCustomHql) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getNormalizedFieldName",
                SelectorField.class, boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(null, sf, isCustomHql);
    }

    private String callGetTargetKey(String changedColumnInp, SelectorField sf) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getTargetKey",
                String.class, SelectorField.class);
        method.setAccessible(true);
        return (String) method.invoke(null, changedColumnInp, sf);
    }

    private String callGetHeadlessFilterClause(Tab tab, Column col, String changedColumnInp,
            JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getHeadlessFilterClause",
                Tab.class, Column.class, String.class, JSONObject.class);
        method.setAccessible(true);
        return (String) method.invoke(null, tab, col, changedColumnInp, dataInpFormat);
    }

    private String callGetExtraProperties(Selector selector) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getExtraProperties", Selector.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selector);
    }

    private Column callGetValueColumn(org.openbravo.model.ad.domain.Selector selectorValidation,
            Selector selectorDefined) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getValueColumn",
                org.openbravo.model.ad.domain.Selector.class, Selector.class);
        method.setAccessible(true);
        return (Column) method.invoke(null, selectorValidation, selectorDefined);
    }

    private void callSavePrefixFields(JSONObject dataInpFormat, String changedColumnInp,
            Selector selectorDefined, JSONObject obj, boolean isCustomHql) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("savePrefixFields",
                JSONObject.class, String.class, Selector.class, JSONObject.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, dataInpFormat, changedColumnInp, selectorDefined, obj, isCustomHql);
    }

    private JSONObject callFindMatchingRecordInBatch(List<Map<String, Object>> results,
            String recordID, String valueField) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("findMatchingRecordInBatch",
                List.class, String.class, String.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, results, recordID, valueField);
    }

    private String callAddFilterClause(Selector selector, HashMap<String, String> hs1,
            HttpServletRequest request) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("addFilterClause",
                Selector.class, HashMap.class, HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selector, hs1, request);
    }

    private String callBuildHQLQuery(Selector selectorDefined, Tab tab, Column col, String changedColumnInp,
            JSONObject dataInpFormat, Map<String, String> db2Input, HttpServletRequest request)
            throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("buildHQLQuery",
                Selector.class, Tab.class, Column.class, String.class, JSONObject.class, Map.class,
                HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selectorDefined, tab, col, changedColumnInp, dataInpFormat, db2Input,
                request);
    }

    // ========== processValue tests ==========

    @Test
    public void testProcessValue_Null() throws ReflectiveOperationException {
        Object result = callProcessValue(null);
        assertEquals(JSONObject.NULL, result);
    }

    @Test
    public void testProcessValue_String() throws ReflectiveOperationException {
        Object result = callProcessValue("hello");
        assertEquals("hello", result);
    }

    @Test
    public void testProcessValue_EmptyString() throws ReflectiveOperationException {
        Object result = callProcessValue("");
        assertEquals("", result);
    }

    @Test
    public void testProcessValue_Number() throws ReflectiveOperationException {
        Object result = callProcessValue(42);
        assertEquals(42, result);
    }

    @Test
    public void testProcessValue_Double() throws ReflectiveOperationException {
        Object result = callProcessValue(3.14);
        assertEquals(3.14, result);
    }

    @Test
    public void testProcessValue_Long() throws ReflectiveOperationException {
        Object result = callProcessValue(999999999L);
        assertEquals(999999999L, result);
    }

    @Test
    public void testProcessValue_Boolean() throws ReflectiveOperationException {
        assertEquals(true, callProcessValue(true));
        assertEquals(false, callProcessValue(false));
    }

    @Test
    public void testProcessValue_BigDecimal() throws ReflectiveOperationException {
        Object result = callProcessValue(new java.math.BigDecimal("123.45"));
        assertEquals(new java.math.BigDecimal("123.45"), result);
    }

    @Test
    public void testProcessValue_Date() throws ReflectiveOperationException, java.text.ParseException {
        Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-01-15");
        Object result = callProcessValue(date);
        assertEquals("2026-01-15", result);
    }

    @Test
    public void testProcessValue_Timestamp() throws ReflectiveOperationException {
        Timestamp ts = Timestamp.valueOf("2026-01-15 10:30:00");
        Object result = callProcessValue(ts);
        assertNotNull(result);
        assertTrue(result.toString().startsWith("2026-01-15T10:30:00"));
    }

    @Test
    public void testProcessValue_UnknownObjectFallsBackToString() throws ReflectiveOperationException {
        Object result = callProcessValue(new ArrayList<>());
        assertEquals("[]", result);
    }

    // ========== convertRowToJSONObject tests ==========

    @Test
    public void testConvertRowToJSONObject_EmptyMap() throws ReflectiveOperationException {
        Map<String, Object> row = new HashMap<>();
        JSONObject result = callConvertRowToJSONObject(row);
        assertNotNull(result);
        assertEquals(0, result.length());
    }

    @Test
    public void testConvertRowToJSONObject_WithValues() throws ReflectiveOperationException, JSONException {
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
    public void testConvertRowToJSONObject_NullKeySkipped() throws ReflectiveOperationException, JSONException {
        Map<String, Object> row = new HashMap<>();
        row.put(null, VALUE);
        row.put("valid", "data");
        JSONObject result = callConvertRowToJSONObject(row);
        assertEquals("data", result.getString("valid"));
        assertFalse(result.has(null));
    }

    @Test
    public void testConvertRowToJSONObject_WithDateValues() throws ReflectiveOperationException, JSONException,
            java.text.ParseException {
        Map<String, Object> row = new LinkedHashMap<>();
        Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-03-15");
        Timestamp ts = Timestamp.valueOf("2026-03-15 14:30:00");
        row.put("dateVal", date);
        row.put("tsVal", ts);
        JSONObject result = callConvertRowToJSONObject(row);
        assertEquals("2026-03-15", result.getString("dateVal"));
        assertTrue(result.getString("tsVal").startsWith("2026-03-15T14:30:00"));
    }

    // ========== fullfillSessionsVariables tests ==========

    @Test
    public void testFullfillSessionsVariables_SingleReplacement() throws ReflectiveOperationException, JSONException {
        String whereClause = "e.organization.id = @AD_Org_ID@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("AD_Org_ID", INP_AD_ORG_ID);
        JSONObject data = new JSONObject();
        data.put(INP_AD_ORG_ID, "ORG123");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.organization.id = 'ORG123'", result);
    }

    @Test
    public void testFullfillSessionsVariables_MultipleReplacements() throws ReflectiveOperationException,
            JSONException {
        String whereClause = "e.client.id = @AD_Client_ID@ AND e.org.id = @AD_Org_ID@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("AD_Client_ID", "inpadClientId");
        db2Input.put("AD_Org_ID", INP_AD_ORG_ID);
        JSONObject data = new JSONObject();
        data.put("inpadClientId", "CLIENT1");
        data.put(INP_AD_ORG_ID, "ORG1");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.client.id = 'CLIENT1' AND e.org.id = 'ORG1'", result);
    }

    @Test
    public void testFullfillSessionsVariables_NoMatchingKeys() throws ReflectiveOperationException, JSONException {
        String whereClause = "e.name = @UNKNOWN@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("OTHER", "inpOther");
        JSONObject data = new JSONObject();
        data.put("inpOther", VALUE);

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.name = @UNKNOWN@", result);
    }

    @Test
    public void testFullfillSessionsVariables_EmptyMaps() throws ReflectiveOperationException {
        String whereClause = "1=1";
        String result = callFullfillSessionsVariables(whereClause, new HashMap<>(), new JSONObject());
        assertEquals("1=1", result);
    }

    @Test
    public void testFullfillSessionsVariables_KeyInMapButNotInJson() throws ReflectiveOperationException {
        String whereClause = "e.id = @M_Product_ID@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("M_Product_ID", "inpmProductId");
        JSONObject data = new JSONObject();

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.id = @M_Product_ID@", result);
    }

    @Test
    public void testFullfillSessionsVariables_CaseInsensitive() throws ReflectiveOperationException, JSONException {
        String whereClause = "e.org = @ad_org_id@";
        Map<String, String> db2Input = new HashMap<>();
        db2Input.put("ad_org_id", INP_AD_ORG_ID);
        JSONObject data = new JSONObject();
        data.put(INP_AD_ORG_ID, "O1");

        String result = callFullfillSessionsVariables(whereClause, db2Input, data);
        assertEquals("e.org = 'O1'", result);
    }

    // ========== convertToHashMAp tests ==========

    @Test
    public void testConvertToHashMAp_BasicConversion() throws ReflectiveOperationException, JSONException {
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", 123);

        HashMap<String, String> result = callConvertToHashMAp(json);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("123", result.get("key2"));
    }

    @Test
    public void testConvertToHashMAp_EmptyJson() throws ReflectiveOperationException {
        JSONObject json = new JSONObject();
        HashMap<String, String> result = callConvertToHashMAp(json);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConvertToHashMAp_BooleanValue() throws ReflectiveOperationException, JSONException {
        JSONObject json = new JSONObject();
        json.put("flag", true);
        HashMap<String, String> result = callConvertToHashMAp(json);
        assertEquals("true", result.get("flag"));
    }

    // ========== getNormalizedFieldName tests ==========

    @Test
    public void testGetNormalizedFieldName_CustomHql_WithDisplayAlias() throws ReflectiveOperationException {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn("test.field.name");
        when(mockSelectorField.getName()).thenReturn("testName");

        String result = callGetNormalizedFieldName(mockSelectorField, true);
        assertEquals("test$field$name", result);
    }

    @Test
    public void testGetNormalizedFieldName_CustomHql_FallbackToName() throws ReflectiveOperationException {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn(null);
        when(mockSelectorField.getName()).thenReturn(FALLBACK_NAME);

        String result = callGetNormalizedFieldName(mockSelectorField, true);
        assertEquals(FALLBACK_NAME_NORMALIZED, result);
    }

    @Test
    public void testGetNormalizedFieldName_CustomHql_EmptyAliasFallbackToName() throws ReflectiveOperationException {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn("");
        when(mockSelectorField.getName()).thenReturn(FALLBACK_NAME);

        String result = callGetNormalizedFieldName(mockSelectorField, true);
        assertEquals(FALLBACK_NAME_NORMALIZED, result);
    }

    @Test
    public void testGetNormalizedFieldName_RegularSelector_WithProperty() throws ReflectiveOperationException {
        when(mockSelectorField.getProperty()).thenReturn("entity.property");
        when(mockSelectorField.getName()).thenReturn("testName");

        String result = callGetNormalizedFieldName(mockSelectorField, false);
        assertEquals("entity$property", result);
    }

    @Test
    public void testGetNormalizedFieldName_RegularSelector_FallbackToName() throws ReflectiveOperationException {
        when(mockSelectorField.getProperty()).thenReturn("");
        when(mockSelectorField.getName()).thenReturn(FALLBACK_NAME);

        String result = callGetNormalizedFieldName(mockSelectorField, false);
        assertEquals(FALLBACK_NAME_NORMALIZED, result);
    }

    @Test
    public void testGetNormalizedFieldName_RegularSelector_NullPropertyFallbackToName()
            throws ReflectiveOperationException {
        when(mockSelectorField.getProperty()).thenReturn(null);
        when(mockSelectorField.getName()).thenReturn("simple");

        String result = callGetNormalizedFieldName(mockSelectorField, false);
        assertEquals("simple", result);
    }

    @Test
    public void testGetNormalizedFieldName_NoDots() throws ReflectiveOperationException {
        when(mockSelectorField.getProperty()).thenReturn("nodots");
        String result = callGetNormalizedFieldName(mockSelectorField, false);
        assertEquals("nodots", result);
    }

    @Test
    public void testGetNormalizedFieldName_LongStringWithDots() throws ReflectiveOperationException {
        String longString = "a".repeat(500) + "." + "b".repeat(500);
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn(longString);
        when(mockSelectorField.getName()).thenReturn(FALLBACK);

        String result = callGetNormalizedFieldName(mockSelectorField, true);
        assertTrue(result.length() > 1000);
        assertTrue(result.contains("$"));
        assertFalse(result.contains("."));
    }

    // ========== getTargetKey tests ==========

    @Test
    public void testGetTargetKey_WithSuffix() throws ReflectiveOperationException {
        when(mockSelectorField.getSuffix()).thenReturn("_SUFFIX");
        String result = callGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertEquals("testColumn_SUFFIX", result);
    }

    @Test
    public void testGetTargetKey_EmptySuffixUsesInpName() throws ReflectiveOperationException {
        when(mockSelectorField.getSuffix()).thenReturn("");
        Column col = mock(Column.class);
        when(mockSelectorField.getColumn()).thenReturn(col);
        when(col.getDBColumnName()).thenReturn("C_BPartner_ID");

        String result = callGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertNotNull(result);
    }

    @Test
    public void testGetTargetKey_NullSuffixUsesInpName() throws ReflectiveOperationException {
        when(mockSelectorField.getSuffix()).thenReturn(null);
        Column col = mock(Column.class);
        when(mockSelectorField.getColumn()).thenReturn(col);
        when(col.getDBColumnName()).thenReturn("M_Product_ID");

        String result = callGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertNotNull(result);
    }

    // ========== getHeadlessFilterClause tests ==========

    @Test
    public void testGetHeadlessFilterClause_NoFilter() throws ReflectiveOperationException, JSONException {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn(null);

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals("", result);
    }

    @Test
    public void testGetHeadlessFilterClause_EmptyFilter() throws ReflectiveOperationException, JSONException {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("");

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals("", result);
    }

    @Test
    public void testGetHeadlessFilterClause_WithFilter() throws ReflectiveOperationException, JSONException {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("condition = @id@");

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals(" AND condition = 'testValue'", result);
    }

    @Test
    public void testGetHeadlessFilterClause_CaseInsensitiveIdReplacement() throws ReflectiveOperationException,
            JSONException {
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("col = @ID@ AND col2 = @Id@");

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, "ABC");

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals(" AND col = 'ABC' AND col2 = 'ABC'", result);
    }

    @Test
    public void testGetHeadlessFilterClause_NoMatchingColumn() throws ReflectiveOperationException, JSONException {
        Column otherColumn = mock(Column.class);
        List<Field> fieldList = new ArrayList<>();
        fieldList.add(mockField);
        when(mockTab.getADFieldList()).thenReturn(fieldList);
        when(mockField.getColumn()).thenReturn(otherColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("some filter");

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals("", result);
    }

    @Test
    public void testGetHeadlessFilterClause_EmptyFieldList() throws ReflectiveOperationException, JSONException {
        when(mockTab.getADFieldList()).thenReturn(new ArrayList<>());

        JSONObject data = new JSONObject();
        data.put(TEST_INPUT, TEST_VALUE);

        String result = callGetHeadlessFilterClause(mockTab, mockColumn, TEST_INPUT, data);
        assertEquals("", result);
    }

    // ========== addFilterClause tests ==========

    @Test
    public void testAddFilterClause_NullFilterExpression() throws ReflectiveOperationException {
        when(mockSelector.getFilterExpression()).thenReturn(null);
        String result = callAddFilterClause(mockSelector, new HashMap<>(), mockRequest);
        assertEquals("", result);
    }

    // ========== getValueColumn tests ==========

    @Test
    public void testGetValueColumn_DefinedSelectorWithValueField() throws ReflectiveOperationException {
        Column valueColumn = mock(Column.class);
        SelectorField valueField = mock(SelectorField.class);
        when(valueField.getColumn()).thenReturn(valueColumn);
        when(mockSelector.getValuefield()).thenReturn(valueField);
        when(mockSelector.isCustomQuery()).thenReturn(false);

        org.openbravo.model.ad.domain.Selector selectorValidation = mock(
                org.openbravo.model.ad.domain.Selector.class);

        Column result = callGetValueColumn(selectorValidation, mockSelector);
        assertEquals(valueColumn, result);
    }

    @Test
    public void testGetValueColumn_CustomQueryFallsBackToValidation() throws ReflectiveOperationException {
        SelectorField valueField = mock(SelectorField.class);
        Column definedColumn = mock(Column.class);
        when(valueField.getColumn()).thenReturn(definedColumn);
        when(mockSelector.getValuefield()).thenReturn(valueField);
        when(mockSelector.isCustomQuery()).thenReturn(true);

        Column validationColumn = mock(Column.class);
        org.openbravo.model.ad.domain.Selector selectorValidation = mock(
                org.openbravo.model.ad.domain.Selector.class);
        when(selectorValidation.getColumn()).thenReturn(validationColumn);

        Column result = callGetValueColumn(selectorValidation, mockSelector);
        assertEquals(validationColumn, result);
    }

    @Test
    public void testGetValueColumn_NullSelectorDefined() throws ReflectiveOperationException {
        Column validationColumn = mock(Column.class);
        org.openbravo.model.ad.domain.Selector selectorValidation = mock(
                org.openbravo.model.ad.domain.Selector.class);
        when(selectorValidation.getColumn()).thenReturn(validationColumn);

        Column result = callGetValueColumn(selectorValidation, null);
        assertEquals(validationColumn, result);
    }

    @Test
    public void testGetValueColumn_NullValueFieldFallsBackToValidation() throws ReflectiveOperationException {
        when(mockSelector.getValuefield()).thenReturn(null);

        Column validationColumn = mock(Column.class);
        org.openbravo.model.ad.domain.Selector selectorValidation = mock(
                org.openbravo.model.ad.domain.Selector.class);
        when(selectorValidation.getColumn()).thenReturn(validationColumn);

        Column result = callGetValueColumn(selectorValidation, mockSelector);
        assertEquals(validationColumn, result);
    }

    // ========== getExtraProperties tests ==========

    @Test
    public void testGetExtraProperties_EmptyList() throws ReflectiveOperationException {
        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(new ArrayList<>());
        String result = callGetExtraProperties(mockSelector);
        assertEquals("", result);
    }

    @Test
    public void testGetExtraProperties_WithOutfields() throws ReflectiveOperationException {
        SelectorField sf1 = mock(SelectorField.class);
        when(sf1.isOutfield()).thenReturn(true);
        when(sf1.getProperty()).thenReturn("product.name");
        when(sf1.getSortno()).thenReturn(10L);

        SelectorField sf2 = mock(SelectorField.class);
        when(sf2.isOutfield()).thenReturn(true);
        when(sf2.getProperty()).thenReturn("product.id");
        when(sf2.getSortno()).thenReturn(20L);

        SelectorField sfNonOut = mock(SelectorField.class);
        when(sfNonOut.isOutfield()).thenReturn(false);
        when(sfNonOut.getProperty()).thenReturn("hidden");
        when(sfNonOut.getSortno()).thenReturn(5L);

        when(mockSelector.getValuefield()).thenReturn(null);
        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf1, sf2, sfNonOut));

        String result = callGetExtraProperties(mockSelector);
        assertEquals("product$name,product$id", result);
    }

    @Test
    public void testGetExtraProperties_ValueFieldIncluded() throws ReflectiveOperationException {
        SelectorField valueField = mock(SelectorField.class);
        when(valueField.isOutfield()).thenReturn(false);
        when(valueField.getProperty()).thenReturn("id");
        when(valueField.getSortno()).thenReturn(1L);

        when(mockSelector.getValuefield()).thenReturn(valueField);
        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(valueField));

        String result = callGetExtraProperties(mockSelector);
        assertEquals("id", result);
    }

    @Test
    public void testGetExtraProperties_SortedBySortno() throws ReflectiveOperationException {
        SelectorField sf1 = mock(SelectorField.class);
        when(sf1.isOutfield()).thenReturn(true);
        when(sf1.getProperty()).thenReturn("second");
        when(sf1.getSortno()).thenReturn(20L);

        SelectorField sf2 = mock(SelectorField.class);
        when(sf2.isOutfield()).thenReturn(true);
        when(sf2.getProperty()).thenReturn("first");
        when(sf2.getSortno()).thenReturn(10L);

        when(mockSelector.getValuefield()).thenReturn(null);
        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf1, sf2));

        String result = callGetExtraProperties(mockSelector);
        assertEquals("first,second", result);
    }

    // ========== findMatchingRecordInBatch tests ==========

    @Test
    public void testFindMatchingRecordInBatch_Found() throws ReflectiveOperationException, JSONException {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", "AAA");
        row1.put("name", "Product A");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("id", "BBB");
        row2.put("name", "Product B");

        List<Map<String, Object>> results = Arrays.asList(row1, row2);

        JSONObject found = callFindMatchingRecordInBatch(results, "BBB", "id");
        assertNotNull(found);
        assertEquals("BBB", found.getString("id"));
        assertEquals("Product B", found.getString("name"));
    }

    @Test
    public void testFindMatchingRecordInBatch_NotFound() throws ReflectiveOperationException {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", "AAA");

        List<Map<String, Object>> results = Collections.singletonList(row1);

        JSONObject found = callFindMatchingRecordInBatch(results, "ZZZ", "id");
        assertNull(found);
    }

    @Test
    public void testFindMatchingRecordInBatch_EmptyList() throws ReflectiveOperationException {
        List<Map<String, Object>> results = new ArrayList<>();
        JSONObject found = callFindMatchingRecordInBatch(results, "AAA", "id");
        assertNull(found);
    }

    @Test
    public void testFindMatchingRecordInBatch_MissingValueField() throws ReflectiveOperationException {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("name", "Product A");

        List<Map<String, Object>> results = Collections.singletonList(row1);

        JSONObject found = callFindMatchingRecordInBatch(results, "AAA", "id");
        assertNull(found);
    }

    // ========== savePrefixFields tests ==========

    @Test
    public void testSavePrefixFields_WithOutfield() throws ReflectiveOperationException, JSONException {
        SelectorField sf = mock(SelectorField.class);
        when(sf.isOutfield()).thenReturn(true);
        when(sf.getProperty()).thenReturn("businessPartner.name");
        when(sf.getSuffix()).thenReturn("_DES");
        when(sf.getSortno()).thenReturn(10L);

        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf));

        JSONObject dataInp = new JSONObject();
        JSONObject selectorResult = new JSONObject();
        selectorResult.put("businessPartner$name", "Test Partner");

        callSavePrefixFields(dataInp, INP_C_BPARTNER_ID, mockSelector, selectorResult, false);

        assertTrue(dataInp.has(INP_C_BPARTNER_ID_DES));
        assertEquals("Test Partner", dataInp.getString(INP_C_BPARTNER_ID_DES));
    }

    @Test
    public void testSavePrefixFields_NoOutfields() throws ReflectiveOperationException, JSONException {
        SelectorField sf = mock(SelectorField.class);
        when(sf.isOutfield()).thenReturn(false);
        when(sf.getSortno()).thenReturn(10L);

        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf));
        when(mockSelector.getValuefield()).thenReturn(null);

        JSONObject dataInp = new JSONObject();
        JSONObject selectorResult = new JSONObject();

        callSavePrefixFields(dataInp, INP_C_BPARTNER_ID, mockSelector, selectorResult, false);
        assertEquals(0, dataInp.length());
    }

    @Test
    public void testSavePrefixFields_CustomHql_UsesDisplayAlias() throws ReflectiveOperationException, JSONException {
        SelectorField sf = mock(SelectorField.class);
        when(sf.isOutfield()).thenReturn(true);
        when(sf.getDisplayColumnAlias()).thenReturn("bp_name");
        when(sf.getName()).thenReturn("bpName");
        when(sf.getSuffix()).thenReturn("_DES");
        when(sf.getSortno()).thenReturn(10L);

        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf));

        JSONObject dataInp = new JSONObject();
        JSONObject selectorResult = new JSONObject();
        selectorResult.put("bp_name", "Custom Partner");

        callSavePrefixFields(dataInp, INP_C_BPARTNER_ID, mockSelector, selectorResult, true);

        assertTrue(dataInp.has(INP_C_BPARTNER_ID_DES));
        assertEquals("Custom Partner", dataInp.getString(INP_C_BPARTNER_ID_DES));
    }

    @Test
    public void testSavePrefixFields_FieldNotInResult() throws ReflectiveOperationException, JSONException {
        SelectorField sf = mock(SelectorField.class);
        when(sf.isOutfield()).thenReturn(true);
        when(sf.getProperty()).thenReturn("missing.field");
        when(sf.getSortno()).thenReturn(10L);

        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf));

        JSONObject dataInp = new JSONObject();
        JSONObject selectorResult = new JSONObject();
        selectorResult.put("other$field", VALUE);

        callSavePrefixFields(dataInp, INP_C_BPARTNER_ID, mockSelector, selectorResult, false);
        assertEquals(0, dataInp.length());
    }

    @Test
    public void testSavePrefixFields_MultipleOutfields() throws ReflectiveOperationException, JSONException {
        SelectorField sf1 = mock(SelectorField.class);
        when(sf1.isOutfield()).thenReturn(true);
        when(sf1.getProperty()).thenReturn("name");
        when(sf1.getSuffix()).thenReturn("_DES");
        when(sf1.getSortno()).thenReturn(10L);

        SelectorField sf2 = mock(SelectorField.class);
        when(sf2.isOutfield()).thenReturn(true);
        when(sf2.getProperty()).thenReturn("taxId");
        when(sf2.getSuffix()).thenReturn("_TAX");
        when(sf2.getSortno()).thenReturn(20L);

        when(mockSelector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf1, sf2));

        JSONObject dataInp = new JSONObject();
        JSONObject selectorResult = new JSONObject();
        selectorResult.put("name", "Partner");
        selectorResult.put("taxId", "12345");

        callSavePrefixFields(dataInp, INP_C_BPARTNER_ID, mockSelector, selectorResult, false);
        assertEquals("Partner", dataInp.getString(INP_C_BPARTNER_ID_DES));
        assertEquals("12345", dataInp.getString("inpcBpartnerId_TAX"));
    }

    // ========== buildHQLQuery tests (ETP-3623) ==========

    @Test
    public void testBuildHQLQuery_EmptyFilters_RemovesAndPlaceholder()
            throws ReflectiveOperationException, JSONException {
        when(mockSelector.getHQL()).thenReturn("SELECT e FROM Entity e WHERE 1=1 and @additional_filters@");
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockTab.getADFieldList()).thenReturn(Collections.emptyList());

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, "inpmProductId",
                new JSONObject(), new HashMap<>(), mockRequest);

        assertEquals("SELECT e FROM Entity e WHERE 1=1 ", result);
    }

    @Test
    public void testBuildHQLQuery_NonEmptyFilters_AndPreceded_DoesNotDuplicateAND()
            throws ReflectiveOperationException, JSONException {
        when(mockSelector.getHQL()).thenReturn("SELECT e FROM Entity e WHERE 1=1 and @additional_filters@");
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("pl.salesPriceList = true");
        when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put("inpmProductId", "test");

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, "inpmProductId",
                dataInpFormat, new HashMap<>(), mockRequest);

        assertFalse("HQL must not contain 'and AND'", result.contains("and AND"));
        assertTrue("HQL must contain the filter clause", result.contains("pl.salesPriceList = true"));
    }

    @Test
    public void testBuildHQLQuery_NonEmptyFilters_StandalonePlaceholder_Replaced()
            throws ReflectiveOperationException, JSONException {
        when(mockSelector.getHQL()).thenReturn("SELECT e FROM Entity e WHERE @additional_filters@");
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn("pl.salesPriceList = true");
        when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put("inpmProductId", "test");

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, "inpmProductId",
                dataInpFormat, new HashMap<>(), mockRequest);

        assertTrue("Standalone placeholder must be replaced", result.contains("pl.salesPriceList = true"));
        assertFalse("Placeholder must be removed", result.contains("@additional_filters@"));
    }
}
