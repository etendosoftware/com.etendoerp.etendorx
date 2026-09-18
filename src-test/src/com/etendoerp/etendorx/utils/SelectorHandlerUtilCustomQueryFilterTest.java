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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.userinterface.selector.Selector;

/**
 * Unit tests for the ETP-5018 fix, which restricts the HQL of a custom query selector to the record
 * being resolved instead of letting {@code executeHQLAndFindRecord} page through the whole selector
 * result set 100 rows at a time until the record turns up.
 *
 * <p>They also pin down the guards: the record id is concatenated into a hand written HQL, so it is
 * only injected when it is a valid Etendo id and when the alias or <em>Clause Left Part</em> it is
 * compared against has a valid HQL format. Whenever a guard rejects the input the query must come back
 * untouched, so the existing paginated search keeps resolving the record.</p>
 */
public class SelectorHandlerUtilCustomQueryFilterTest extends SelectorHandlerUtilBaseTest {

    private static final String PLACEHOLDER = "@additional_filters@";
    private static final String BASE_HQL = "select p.id as id from Product p where p.active = 'Y'";
    private static final String HQL = BASE_HQL + " and " + PLACEHOLDER;
    private static final String VALID_ID = "4028E6C72959682B01295ADC195D021E";
    private static final String ALIAS = "p";
    private static final String CLAUSE_LEFT_PART = "p.product.id";
    // The filter the fix is expected to build when the selector has no Clause Left Part.
    private static final String ALIAS_ID_FILTER = ALIAS + ".id = '" + VALID_ID + "'";

    private String callAddCustomQueryRecordIdFilter(String hqlQuery, Selector selectorDefined,
            String recordID) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("addCustomQueryRecordIdFilter",
                String.class, Selector.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, hqlQuery, selectorDefined, recordID);
    }

    /**
     * Configures the selector mock as a custom query one, with the given Clause Left Part and entity alias.
     *
     * @param clauseLeftPart
     *     The Clause Left Part of the value field, or {@code null} to leave the value field without one.
     * @param entityAlias
     *     The entity alias declared for the selector.
     */
    private void givenCustomQuerySelector(String clauseLeftPart, String entityAlias) {
        when(mockSelector.isCustomQuery()).thenReturn(Boolean.TRUE);
        when(mockSelector.getValuefield()).thenReturn(mockSelectorField);
        when(mockSelectorField.getClauseLeftPart()).thenReturn(clauseLeftPart);
        when(mockSelector.getEntityAlias()).thenReturn(entityAlias);
    }

    @Test
    public void filtersByClauseLeftPartWhenConfigured() throws ReflectiveOperationException {
        givenCustomQuerySelector(CLAUSE_LEFT_PART, ALIAS);

        String result = callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID);

        assertTrue("The Clause Left Part must drive the filter",
                result.contains(CLAUSE_LEFT_PART + " = '" + VALID_ID + "' and " + PLACEHOLDER));
    }

    @Test
    public void filtersByEntityAliasIdWhenNoClauseLeftPart() throws ReflectiveOperationException {
        givenCustomQuerySelector("  ", ALIAS);

        String result = callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID);

        assertTrue("The entity alias id must drive the filter",
                result.contains(ALIAS_ID_FILTER + " and " + PLACEHOLDER));
    }

    @Test
    public void filtersByEntityAliasIdWhenNoValueField() throws ReflectiveOperationException {
        when(mockSelector.isCustomQuery()).thenReturn(Boolean.TRUE);
        when(mockSelector.getValuefield()).thenReturn(null);
        when(mockSelector.getEntityAlias()).thenReturn(ALIAS);

        String result = callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID);

        assertTrue("A selector without value field must fall back to the entity alias",
                result.contains(ALIAS_ID_FILTER));
    }

    @Test
    public void acceptsLowercaseHexId() throws ReflectiveOperationException {
        givenCustomQuerySelector(null, ALIAS);
        String lowercaseId = VALID_ID.toLowerCase();

        String result = callAddCustomQueryRecordIdFilter(HQL, mockSelector, lowercaseId);

        assertTrue("Lowercase hex ids are valid Etendo ids", result.contains("'" + lowercaseId + "'"));
    }

    @Test
    public void leavesQueryUntouchedForInjectionAttempt() throws ReflectiveOperationException {
        givenCustomQuerySelector(CLAUSE_LEFT_PART, ALIAS);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, "x' or '1'='1"));
    }

    @Test
    public void leavesQueryUntouchedForNullOrMalformedId() throws ReflectiveOperationException {
        givenCustomQuerySelector(CLAUSE_LEFT_PART, ALIAS);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, null));
        assertEquals("A hyphenated uuid is not an Etendo id", HQL,
                callAddCustomQueryRecordIdFilter(HQL, mockSelector, "4028e6c7-2959-682b-0129-5adc195d021e"));
        assertEquals("A 31 character id is too short", HQL,
                callAddCustomQueryRecordIdFilter(HQL, mockSelector, "4028E6C72959682B01295ADC195D021"));
    }

    @Test
    public void leavesQueryUntouchedForNonCustomQuerySelector() throws ReflectiveOperationException {
        when(mockSelector.isCustomQuery()).thenReturn(Boolean.FALSE);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID));
    }

    @Test
    public void leavesQueryUntouchedWithoutPlaceholder() throws ReflectiveOperationException {
        givenCustomQuerySelector(CLAUSE_LEFT_PART, ALIAS);
        assertEquals(BASE_HQL, callAddCustomQueryRecordIdFilter(BASE_HQL, mockSelector, VALID_ID));
    }

    @Test
    public void leavesQueryUntouchedForMalformedClauseLeftPart() throws ReflectiveOperationException {
        givenCustomQuerySelector("p.id = 1 or 1=1", ALIAS);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID));
    }

    @Test
    public void leavesQueryUntouchedForClauseLeftPartWithEmptySegment() throws ReflectiveOperationException {
        givenCustomQuerySelector("p..id", ALIAS);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID));
    }

    @Test
    public void leavesQueryUntouchedForSingleSegmentClauseLeftPart() throws ReflectiveOperationException {
        givenCustomQuerySelector("id", ALIAS);

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID));
    }

    @Test
    public void leavesQueryUntouchedForMalformedEntityAlias() throws ReflectiveOperationException {
        givenCustomQuerySelector(null, "p p");

        assertEquals(HQL, callAddCustomQueryRecordIdFilter(HQL, mockSelector, VALID_ID));
    }

    @Test
    public void keepsRecordFilterWhenBuildingQueryWithoutAdditionalFilters()
            throws ReflectiveOperationException, JSONException {
        givenCustomQuerySelector(null, ALIAS);
        when(mockSelector.getHQL()).thenReturn(HQL);
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockTab.getADFieldList()).thenReturn(Collections.emptyList());
        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put(INP_M_PRODUCT_ID, VALID_ID);

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, INP_M_PRODUCT_ID,
                dataInpFormat, new HashMap<>(), mockRequest);

        assertTrue("The record filter must survive the placeholder removal",
                result.contains(ALIAS_ID_FILTER));
        assertFalse("The placeholder must be gone", result.contains(PLACEHOLDER));
        assertFalse("The query must not be left with a dangling and", result.toLowerCase().contains("and and"));
    }

    @Test
    public void keepsRecordFilterAndAdditionalFiltersTogether()
            throws ReflectiveOperationException, JSONException {
        givenCustomQuerySelector(null, ALIAS);
        when(mockSelector.getHQL()).thenReturn(HQL);
        when(mockSelector.getFilterExpression()).thenReturn(null);
        when(mockField.getColumn()).thenReturn(mockColumn);
        when(mockField.getEtrxFilterClause()).thenReturn(SALES_PRICE_LIST_FILTER);
        when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));
        JSONObject dataInpFormat = new JSONObject();
        dataInpFormat.put(INP_M_PRODUCT_ID, VALID_ID);

        String result = callBuildHQLQuery(mockSelector, mockTab, mockColumn, INP_M_PRODUCT_ID,
                dataInpFormat, new HashMap<>(), mockRequest);

        assertTrue("The record filter must be kept", result.contains(ALIAS_ID_FILTER));
        assertTrue("The headless filter clause must be kept", result.contains(SALES_PRICE_LIST_FILTER));
        assertFalse("The placeholder must be gone", result.contains(PLACEHOLDER));
    }
}
