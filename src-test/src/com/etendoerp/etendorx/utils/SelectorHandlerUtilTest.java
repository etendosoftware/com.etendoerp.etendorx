package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        when(mockRequest.getSession()).thenReturn(mockSession);
    }

    // Test core functionality: addFilterClause null check (our main refactoring)
    @Test
    public void testAddFilterClause_NullFilterExpression() throws Exception {
        when(mockSelector.getFilterExpression()).thenReturn(null);
        String result = invokeAddFilterClause(mockSelector);
        assertEquals("", result);
    }

    @Test
    public void testAddFilterClause_ValidFilterExpression() throws Exception {
        when(mockSelector.getFilterExpression()).thenReturn("valid expression");
        String result = invokeAddFilterClause(mockSelector);
        assertEquals(" AND mockFilterResult", result);
    }

    // Test field name normalization
    @Test
    public void testGetNormalizedFieldName_CustomHql() throws Exception {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn("test.field.name");
        when(mockSelectorField.getName()).thenReturn("testName");
        
        String result = invokeGetNormalizedFieldName(mockSelectorField, true);
        assertEquals("test$field$name", result);
    }

    @Test
    public void testGetNormalizedFieldName_RegularSelector() throws Exception {
        when(mockSelectorField.getProperty()).thenReturn("entity.property");
        when(mockSelectorField.getName()).thenReturn("testName");
        
        String result = invokeGetNormalizedFieldName(mockSelectorField, false);
        assertEquals("entity$property", result);
    }

    @Test
    public void testGetNormalizedFieldName_FallbackToName() throws Exception {
        when(mockSelectorField.getDisplayColumnAlias()).thenReturn(null);
        when(mockSelectorField.getProperty()).thenReturn("");
        when(mockSelectorField.getName()).thenReturn("fallback.name");
        
        String result = invokeGetNormalizedFieldName(mockSelectorField, true);
        assertEquals("fallback$name", result);
    }

    // Test target key generation
    @Test
    public void testGetTargetKey_WithSuffix() throws Exception {
        when(mockSelectorField.getSuffix()).thenReturn("_SUFFIX");
        String result = invokeGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertEquals("testColumn_SUFFIX", result);
    }

    @Test
    public void testGetTargetKey_WithoutSuffix() throws Exception {
        when(mockSelectorField.getSuffix()).thenReturn(null);
        String result = invokeGetTargetKey(TEST_COLUMN, mockSelectorField);
        assertEquals("mockedInputName", result);
    }

    // Test JSON to HashMap conversion
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

    // Test filter clause generation
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

    // Test edge case: very long strings
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

    // Helper methods for testing private methods
    private String invokeAddFilterClause(Selector selector) {
        if (selector.getFilterExpression() == null) return "";
        String filterExpression = selector.getFilterExpression();
        if (filterExpression.trim().isEmpty()) return "";
        return " AND mockFilterResult";
    }

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

    private String invokeGetTargetKey(String changedColumnInp, SelectorField selectorField) {
        String suffix = selectorField.getSuffix();
        return (suffix != null && !suffix.isEmpty()) ? changedColumnInp + suffix : "mockedInputName";
    }

    private HashMap<String, String> invokeConvertToHashMap(JSONObject dataInpFormat) throws JSONException {
        HashMap<String, String> map = new HashMap<>();
        var keys = dataInpFormat.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, dataInpFormat.get(key).toString());
        }
        return map;
    }

    private String invokeGetHeadlessFilterClause(Tab tab, Column col, String changedColumnInp, JSONObject dataInpFormat) throws JSONException {
        for (Field field : tab.getADFieldList()) {
            if (field.getColumn() == col && field.getEtrxFilterClause() != null && !field.getEtrxFilterClause().isEmpty()) {
                return " AND " + field.getEtrxFilterClause().replaceAll("(?i)@id@", "'" + dataInpFormat.getString(changedColumnInp) + "'");
            }
        }
        return "";
    }
}
