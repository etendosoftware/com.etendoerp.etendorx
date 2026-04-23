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

import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * Base test class for SelectorHandlerUtil tests.
 * Contains shared mocks, setup, constants, and reflection helpers.
 */
public abstract class SelectorHandlerUtilBaseTest extends WeldBaseTest {

    static final String TEST_INPUT = "testInput";
    static final String TEST_VALUE = "testValue";
    static final String TEST_COLUMN = "testColumn";
    static final String FALLBACK = "fallback";
    static final String VALUE = "value";
    static final String INP_AD_ORG_ID = "inpadOrgId";
    static final String FALLBACK_NAME = "fallback.name";
    static final String FALLBACK_NAME_NORMALIZED = "fallback$name";
    static final String INP_C_BPARTNER_ID = "inpcBpartnerId";
    static final String INP_C_BPARTNER_ID_DES = "inpcBpartnerId_DES";
    static final String INP_M_PRODUCT_ID = "inpmProductId";
    static final String SALES_PRICE_LIST_FILTER = "pl.salesPriceList = true";

    @Mock
    HttpServletRequest mockRequest;

    @Mock
    HttpSession mockSession;

    @Mock
    Tab mockTab;

    @Mock
    Column mockColumn;

    @Mock
    Field mockField;

    @Mock
    Selector mockSelector;

    @Mock
    SelectorField mockSelectorField;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        when(mockRequest.getSession()).thenReturn(mockSession);
    }

    Object callProcessValue(Object value) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("processValue", Object.class);
        method.setAccessible(true);
        return method.invoke(null, value);
    }

    JSONObject callConvertRowToJSONObject(Map<String, Object> row) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("convertRowToJSONObject", Map.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, row);
    }

    String callFullfillSessionsVariables(String whereClause, Map<String, String> db2Input,
            JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("fullfillSessionsVariables",
                String.class, Map.class, JSONObject.class);
        method.setAccessible(true);
        return (String) method.invoke(null, whereClause, db2Input, dataInpFormat);
    }

    HashMap<String, String> callConvertToHashMap(JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("convertToHashMAp", JSONObject.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, String> result = (HashMap<String, String>) method.invoke(null, dataInpFormat);
        return result;
    }

    String callGetNormalizedFieldName(SelectorField sf, boolean isCustomHql) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getNormalizedFieldName",
                SelectorField.class, boolean.class);
        method.setAccessible(true);
        return (String) method.invoke(null, sf, isCustomHql);
    }

    String callGetTargetKey(String changedColumnInp, SelectorField sf) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getTargetKey",
                String.class, SelectorField.class);
        method.setAccessible(true);
        return (String) method.invoke(null, changedColumnInp, sf);
    }

    String callGetHeadlessFilterClause(Tab tab, Column col, String changedColumnInp,
            JSONObject dataInpFormat) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getHeadlessFilterClause",
                Tab.class, Column.class, String.class, JSONObject.class);
        method.setAccessible(true);
        return (String) method.invoke(null, tab, col, changedColumnInp, dataInpFormat);
    }

    String callGetExtraProperties(Selector selector) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getExtraProperties", Selector.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selector);
    }

    Column callGetValueColumn(org.openbravo.model.ad.domain.Selector selectorValidation,
            Selector selectorDefined) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("getValueColumn",
                org.openbravo.model.ad.domain.Selector.class, Selector.class);
        method.setAccessible(true);
        return (Column) method.invoke(null, selectorValidation, selectorDefined);
    }

    void callSavePrefixFields(JSONObject dataInpFormat, String changedColumnInp,
            Selector selectorDefined, JSONObject obj, boolean isCustomHql) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("savePrefixFields",
                JSONObject.class, String.class, Selector.class, JSONObject.class, boolean.class);
        method.setAccessible(true);
        method.invoke(null, dataInpFormat, changedColumnInp, selectorDefined, obj, isCustomHql);
    }

    JSONObject callFindMatchingRecordInBatch(List<Map<String, Object>> results,
            String recordID, String valueField) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("findMatchingRecordInBatch",
                List.class, String.class, String.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, results, recordID, valueField);
    }

    String callAddFilterClause(Selector selector, HashMap<String, String> hs1,
            HttpServletRequest request) throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("addFilterClause",
                Selector.class, HashMap.class, HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selector, hs1, request);
    }

    String callBuildHQLQuery(Selector selectorDefined, Tab tab, Column col, String changedColumnInp,
            JSONObject dataInpFormat, Map<String, String> db2Input, HttpServletRequest request)
            throws ReflectiveOperationException {
        Method method = SelectorHandlerUtil.class.getDeclaredMethod("buildHQLQuery",
                Selector.class, Tab.class, Column.class, String.class, JSONObject.class, Map.class,
                HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(null, selectorDefined, tab, col, changedColumnInp, dataInpFormat, db2Input,
                request);
    }
}
