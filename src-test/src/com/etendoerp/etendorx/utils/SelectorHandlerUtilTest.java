package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * Unit tests for SelectorHandlerUtil class.
 * Covers: processValue, convertRowToJSONObject, fulfillSessionsVariables,
 * convertToHashMap, getNormalizedFieldName, getTargetKey.
 * All tests invoke actual production code via reflection to ensure SonarQube coverage.
 */
public class SelectorHandlerUtilTest extends SelectorHandlerUtilBaseTest {

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
        db2Input.put("M_Product_ID", INP_M_PRODUCT_ID);
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

        HashMap<String, String> result = callConvertToHashMap(json);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("123", result.get("key2"));
    }

    @Test
    public void testConvertToHashMAp_EmptyJson() throws ReflectiveOperationException {
        JSONObject json = new JSONObject();
        HashMap<String, String> result = callConvertToHashMap(json);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConvertToHashMAp_BooleanValue() throws ReflectiveOperationException, JSONException {
        JSONObject json = new JSONObject();
        json.put("flag", true);
        HashMap<String, String> result = callConvertToHashMap(json);
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
}
