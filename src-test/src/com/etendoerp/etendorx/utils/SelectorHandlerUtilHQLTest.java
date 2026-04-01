/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * Unit tests for SelectorHandlerUtil class.
 * Covers: getHeadlessFilterClause, addFilterClause, getValueColumn, getExtraProperties,
 * findMatchingRecordInBatch, savePrefixFields, buildHQLQuery.
 * All tests invoke actual production code via reflection to ensure SonarQube coverage.
 */
public class SelectorHandlerUtilHQLTest extends SelectorHandlerUtilBaseTest {

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

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, INP_M_PRODUCT_ID,
                new JSONObject(), new HashMap<>(), mockRequest);

        assertEquals("SELECT e FROM Entity e WHERE 1=1 ", result);
    }

    @Test
    public void testBuildHQLQuery_NonEmptyFilters_AndPreceded_DoesNotDuplicateAND()
            throws ReflectiveOperationException, JSONException {
        when(mockSelector.getHQL()).thenReturn("SELECT e FROM Entity e WHERE 1=1 and @additional_filters@");
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn(SALES_PRICE_LIST_FILTER);
        when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put(INP_M_PRODUCT_ID, "test");

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, INP_M_PRODUCT_ID,
                dataInpFormat, new HashMap<>(), mockRequest);

        assertFalse("HQL must not contain duplicate AND regardless of case",
                result.toLowerCase().contains("and and"));
        assertTrue("HQL must contain the filter clause", result.contains(SALES_PRICE_LIST_FILTER));
    }

    @Test
    public void testBuildHQLQuery_NonEmptyFilters_StandalonePlaceholder_Replaced()
            throws ReflectiveOperationException, JSONException {
        when(mockSelector.getHQL()).thenReturn("SELECT e FROM Entity e WHERE @additional_filters@");
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn(SALES_PRICE_LIST_FILTER);
        when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put(INP_M_PRODUCT_ID, "test");

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, INP_M_PRODUCT_ID,
                dataInpFormat, new HashMap<>(), mockRequest);

        assertTrue("Standalone placeholder must be replaced", result.contains(SALES_PRICE_LIST_FILTER));
        assertFalse("Placeholder must be removed", result.contains("@additional_filters@"));
    }
}
