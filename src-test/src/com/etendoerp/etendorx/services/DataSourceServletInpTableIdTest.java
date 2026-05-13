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
 * All portions are Copyright (C) 2024 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

import com.etendoerp.etendorx.services.wrapper.EtendoResponseWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.etendorx.utils.DataSourceUtils;
import com.etendoerp.etendorx.utils.SelectorHandlerUtil;

/**
 * Unit test for ETP-3601: verifies that inpTableId is included in the
 * dataInpFormat JSON passed to formInit.execute() during CHANGE events
 * in both POST and PUT flows of the Headless API.
 */
public class DataSourceServletInpTableIdTest {

  private static final String EXPECTED_TABLE_ID = "B21F4EC171EE4CD49A175FB3C2485F78";
  private static final String EXPECTED_WINDOW_ID = "TEST_WINDOW_ID";
  private static final String EXPECTED_TAB_ID = "TEST_TAB_ID";
  private static final String TABLE_NAME = "TestTable";
  private static final String BUSINESS_PARTNER = "businessPartner";
  private static final String INP_C_BPARTNER_ID = "inpcBpartnerId";

  private Tab createMockTab() {
    Tab tab = mock(Tab.class);
    Table table = mock(Table.class);
    Window window = mock(Window.class);
    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn(EXPECTED_TABLE_ID);
    when(table.getName()).thenReturn(TABLE_NAME);
    when(tab.getWindow()).thenReturn(window);
    when(window.getId()).thenReturn(EXPECTED_WINDOW_ID);
    when(tab.getId()).thenReturn(EXPECTED_TAB_ID);
    return tab;
  }

  private void mockDataSourceUtils(MockedStatic<DataSourceUtils> ds, Tab tab) {
    ds.when(() -> DataSourceUtils.getParentId(any(Tab.class), any(JSONObject.class)))
        .thenReturn(null);
    ds.when(() -> DataSourceUtils.getParentProperties(any(Tab.class)))
        .thenReturn(Collections.emptyList());
    ds.when(() -> DataSourceUtils.getTabByDataSourceName(anyString()))
        .thenReturn(tab);
    ds.when(() -> DataSourceUtils.loadCaches(any(), any(), any(), any(), any()))
        .thenAnswer(inv -> {
          LinkedHashMap<String, String> norm2input = inv.getArgument(1);
          Map<String, String> input2norm = inv.getArgument(2);
          norm2input.put(BUSINESS_PARTNER, INP_C_BPARTNER_ID);
          input2norm.put(INP_C_BPARTNER_ID, BUSINESS_PARTNER);
          return null;
        });
    ds.when(() -> DataSourceUtils.keyConvertion(any(JSONObject.class), any(Map.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    ds.when(
            () -> DataSourceUtils.applyColumnValues(any(JSONObject.class), any(Map.class), any(JSONObject.class)))
        .thenAnswer(inv -> null);
    ds.when(() -> DataSourceUtils.valueConvertToInputFormat(any(), any()))
        .thenAnswer(inv -> {
          Object val = inv.getArgument(0);
          return val != null ? val.toString() : "";
        });
    ds.when(() -> DataSourceUtils.valuesConvertion(any(JSONObject.class), any(Map.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    ds.when(() -> DataSourceUtils.applyChanges(any(JSONObject.class), any(JSONObject.class)))
        .thenAnswer(inv -> inv.getArgument(1));
    ds.when(() -> DataSourceUtils.extractDataSourceAndID(anyString()))
        .thenReturn(new String[] { "TestEntity", "RECORD_123" });
  }

  private void assertInpTableIdInChangeContent(EtendoFormInitComponent formInit) throws Exception {
    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(formInit, atLeast(2)).execute(any(), contentCaptor.capture());

    List<String> allContents = contentCaptor.getAllValues();
    // First call is NEW/EDIT mode with "{}", subsequent calls are CHANGE with dataInpFormat
    String changeContent = allContents.get(allContents.size() - 1);
    JSONObject changeJson = new JSONObject(changeContent);

    assertTrue("dataInpFormat should contain inpTableId",
        changeJson.has("inpTableId"));
    assertEquals("inpTableId should equal the Tab's Table ID",
        EXPECTED_TABLE_ID, changeJson.getString("inpTableId"));
  }

  /**
   * Verifies that getEtendoPostWrapper includes inpTableId in the dataInpFormat
   * JSON passed to formInit.execute() during CHANGE events.
   *
   * @throws Exception if reflection invocation or JSON parsing fails
   */
  @Test
  @SuppressWarnings("unchecked")
  public void postWrapperIncludesInpTableIdInChangeContent() throws Exception {
    Tab tab = createMockTab();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(mock(HttpSession.class));

    EtendoFormInitComponent formInit = mock(EtendoFormInitComponent.class);
    when(formInit.execute(anyMap(), anyString())).thenReturn(new JSONObject());

    JSONObject data = new JSONObject().put(BUSINESS_PARTNER, "BP_ID");
    JSONObject body = new JSONObject().put("data", data);
    List<RequestField> fieldList = new ArrayList<>();

    RequestContext mockRequestContext = mock(RequestContext.class);

    try (MockedStatic<WeldUtils> weld = Mockito.mockStatic(WeldUtils.class);
        MockedStatic<DataSourceUtils> ds = Mockito.mockStatic(DataSourceUtils.class);
        MockedStatic<SelectorHandlerUtil> sh = Mockito.mockStatic(SelectorHandlerUtil.class);
        MockedStatic<RequestContext> rc = Mockito.mockStatic(RequestContext.class)) {

      rc.when(RequestContext::get).thenReturn(mockRequestContext);
      weld.when(() -> WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class))
          .thenReturn(formInit);
      mockDataSourceUtils(ds, tab);
      sh.when(() -> SelectorHandlerUtil.handleColumnSelector(
          any(), any(), any(), any(), any(), any())).thenAnswer(inv -> null);

      // When: invoke the private getEtendoPostWrapper method
      Method method = DataSourceServlet.class.getDeclaredMethod("getEtendoPostWrapper",
          HttpServletRequest.class, Tab.class, JSONObject.class, List.class, String.class);
      method.setAccessible(true);
      method.invoke(new DataSourceServlet(), request, tab, body, fieldList, "/test");

      // Then: verify inpTableId in CHANGE content
      assertInpTableIdInChangeContent(formInit);
    }
  }

  /**
   * Verifies that getEtendoPutWrapper includes inpTableId in the dataInpFormat
   * JSON passed to formInit.execute() during CHANGE events.
   *
   * @throws Exception if reflection invocation or JSON parsing fails
   */
  @Test
  @SuppressWarnings("unchecked")
  public void putWrapperIncludesInpTableIdInChangeContent() throws Exception {
    Tab tab = createMockTab();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(mock(HttpSession.class));
    when(request.getParameterMap()).thenReturn(Collections.emptyMap());

    HttpServletResponse response = mock(HttpServletResponse.class);

    EtendoFormInitComponent formInit = mock(EtendoFormInitComponent.class);
    when(formInit.execute(anyMap(), anyString())).thenReturn(new JSONObject());

    // Mock the internal DataSourceServlet used for the GET call inside getEtendoPutWrapper
    org.openbravo.service.datasource.DataSourceServlet internalServlet =
        mock(org.openbravo.service.datasource.DataSourceServlet.class);

    // When doGet is called, write a fake response with existing record data
    JSONObject existingRecord = new JSONObject().put("id", "RECORD_123").put(BUSINESS_PARTNER, "OLD_BP");
    JSONObject fakeGetResponse = new JSONObject()
        .put(DataSourceConstants.RESPONSE, new JSONObject()
            .put(DataSourceConstants.DATA, new JSONArray().put(existingRecord)));

    doAnswer(inv -> {
      EtendoResponseWrapper respWrapper = inv.getArgument(1);
      respWrapper.getWriter().write(fakeGetResponse.toString());
      respWrapper.getWriter().flush();
      return null;
    }).when(internalServlet).doGet(any(), any());

    JSONObject updateData = new JSONObject().put(BUSINESS_PARTNER, "NEW_BP");
    JSONObject fullBody = new JSONObject().put("data", updateData);
    List<RequestField> fieldList = new ArrayList<>();

    RequestContext mockRequestContext = mock(RequestContext.class);

    try (MockedStatic<WeldUtils> weld = Mockito.mockStatic(WeldUtils.class);
        MockedStatic<DataSourceUtils> ds = Mockito.mockStatic(DataSourceUtils.class);
        MockedStatic<SelectorHandlerUtil> sh = Mockito.mockStatic(SelectorHandlerUtil.class);
        MockedStatic<RequestContext> rc = Mockito.mockStatic(RequestContext.class);
        MockedStatic<OBContext> ob = Mockito.mockStatic(OBContext.class)) {

      rc.when(RequestContext::get).thenReturn(mockRequestContext);
      weld.when(() -> WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class))
          .thenReturn(formInit);
      weld.when(() -> WeldUtils.getInstanceFromStaticBeanManager(
              org.openbravo.service.datasource.DataSourceServlet.class))
          .thenReturn(internalServlet);
      ob.when(OBContext::setAdminMode).thenAnswer(inv -> null);
      ob.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);

      mockDataSourceUtils(ds, tab);
      sh.when(() -> SelectorHandlerUtil.handleColumnSelector(
          any(), any(), any(), any(), any(), any())).thenAnswer(inv -> null);

      // When: invoke the private getEtendoPutWrapper method via reflection
      Method method = DataSourceServlet.class.getDeclaredMethod("getEtendoPutWrapper",
          HttpServletRequest.class, HttpServletResponse.class, JSONObject.class,
          List.class, String.class, String.class);
      method.setAccessible(true);
      method.invoke(new DataSourceServlet(), request, response, fullBody, fieldList,
          "/test", "/TestEntity/RECORD_123");

      // Then: verify inpTableId in CHANGE content
      assertInpTableIdInChangeContent(formInit);
    }
  }
}
