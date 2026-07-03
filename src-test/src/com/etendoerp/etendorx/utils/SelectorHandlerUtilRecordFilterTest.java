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

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Unit tests for {@code SelectorHandlerUtil.appendRecordIdFilter}, the ETP-4398 fix that restricts a
 * search-selector datasource query directly to the target record so the (lazy-count) pagination scan
 * in {@code searchForRecord} can never miss a record that sorts beyond the first ~200 rows.
 *
 * <p>These tests also pin down the HQL-injection guard: the record id comes from the request payload
 * and is concatenated into the HQL where clause, so it must be restricted to a valid Etendo id
 * (32 hex chars, no hyphens) before it is appended.</p>
 */
public class SelectorHandlerUtilRecordFilterTest extends SelectorHandlerUtilBaseTest {

    private static final String BASE_WHERE = "e.active='Y'";
    private static final String VALUE_PROPERTY = "product";
    private static final String VALID_ID = "4028E6C72959682B01295ADC195D021E";

    private String callAppendRecordIdFilter(String whereClauseAndFilters, String valueProperty,
            String recordID) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("appendRecordIdFilter",
                String.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, whereClauseAndFilters, valueProperty, recordID);
    }

    @Test
    public void appendsFilterForValidEtendoId() throws ReflectiveOperationException {
        String result = callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY, VALID_ID);
        assertEquals(BASE_WHERE + " AND e." + VALUE_PROPERTY + " = '" + VALID_ID + "'", result);
    }

    @Test
    public void appendsFilterForDottedValueProperty() throws ReflectiveOperationException {
        String result = callAppendRecordIdFilter(BASE_WHERE, "product.id", VALID_ID);
        assertEquals(BASE_WHERE + " AND e.product.id = '" + VALID_ID + "'", result);
    }

    @Test
    public void appendsFilterForLowercaseHexId() throws ReflectiveOperationException {
        // The guard accepts hex in either case; lowercase 32-char ids are still valid Etendo ids.
        String lowercaseId = VALID_ID.toLowerCase();
        String result = callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY, lowercaseId);
        assertEquals(BASE_WHERE + " AND e." + VALUE_PROPERTY + " = '" + lowercaseId + "'", result);
    }

    @Test
    public void leavesClauseUntouchedForNullId() throws ReflectiveOperationException {
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY, null));
    }

    @Test
    public void leavesClauseUntouchedForInjectionAttempt() throws ReflectiveOperationException {
        String malicious = "x' OR '1'='1";
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY, malicious));
    }

    @Test
    public void leavesClauseUntouchedForWrongLengthId() throws ReflectiveOperationException {
        // 31 chars (too short) and 33 chars (too long) must both be rejected
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY,
                "4028E6C72959682B01295ADC195D021"));
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY,
                "4028E6C72959682B01295ADC195D021EE"));
    }

    @Test
    public void leavesClauseUntouchedForNonHexId() throws ReflectiveOperationException {
        // hyphenated UUID form and a non-hex letter must both be rejected
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY,
                "4028e6c7-2959-682b-0129-5adc195d021e"));
        assertEquals(BASE_WHERE, callAppendRecordIdFilter(BASE_WHERE, VALUE_PROPERTY,
                "4028E6C72959682B01295ADC195D021G"));
    }
}
