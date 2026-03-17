package com.etendoerp.etendorx.services;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

/**
 * Tests for EtendoFormInitComponent.
 * Uses JUnit 4 following the pattern of service-level tests in this project.
 * Uses spy to stub invokeSuperExecute() which depends on injected cachedStructures.
 */
public class EtendoFormInitComponentTest {

  public static final String TEST_TAB_ID = "TEST_TAB_ID";
  public static final String TAB_ID = "TAB_ID";
  private MockedStatic<OBContext> obContextMock;
  private MockedStatic<OBDal> obDalMock;
  private OBContext mockContext;
  private OBDal mockDal;
  private EtendoFormInitComponent component;
  private JSONObject superResult;

  /**
   * Sets up the test environment with mocked OBContext, OBDal and a spied EtendoFormInitComponent.
   *
   * @throws Exception if the super result JSON cannot be parsed
   */
  @Before
  public void setUp() throws Exception {
    mockContext = mock(OBContext.class);
    mockDal = mock(OBDal.class);
    Session mockSession = mock(Session.class);

    obContextMock = mockStatic(OBContext.class);
    obContextMock.when(OBContext::getOBContext).thenReturn(mockContext);

    obDalMock = mockStatic(OBDal.class);
    obDalMock.when(OBDal::getInstance).thenReturn(mockDal);
    when(mockDal.getSession()).thenReturn(mockSession);

    superResult = new JSONObject("{\"status\":\"ok\"}");

    // Spy on the component so we can stub invokeSuperExecute()
    component = spy(new EtendoFormInitComponent());
    doReturn(superResult).when(component).invokeSuperExecute(any(Map.class), anyString());
  }

  /**
   * Closes the static mocks for OBContext and OBDal.
   */
  @After
  public void tearDown() {
    obContextMock.close();
    obDalMock.close();
  }

  /**
   * Verifies that execute completes and restores previous mode when TAB_ID parameter is absent.
   */
  @Test
  public void testExecuteWithNullTabId() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);

    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = component.execute(parameters, "{}");

    assertEquals(superResult, result);
    obContextMock.verify(OBContext::restorePreviousMode);
  }

  /**
   * Verifies that execute skips the tab lookup when TAB_ID is the literal string "null".
   */
  @Test
  public void testExecuteWithNullStringTabId() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, "null");

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    // Verify that OBDal.get was NOT called for "null" tabId
    verify(mockDal, never()).get(eq(Tab.class), eq("null"));
    obContextMock.verify(OBContext::restorePreviousMode);
  }

  /**
   * Verifies that execute retrieves the tab and processes its fields when a valid TAB_ID is provided.
   */
  @Test
  public void testExecuteWithValidTabId() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.emptyList());

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(mockDal).get(Tab.class, TEST_TAB_ID);
    obContextMock.verify(OBContext::restorePreviousMode);
  }

  /**
   * Verifies that execute handles gracefully a TAB_ID that does not match any existing tab.
   */
  @Test
  public void testExecuteWithTabNotFound() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);
    when(mockDal.get(Tab.class, "INVALID_ID")).thenReturn(null);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, "INVALID_ID");

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    obContextMock.verify(OBContext::restorePreviousMode);
  }

  /**
   * Verifies that restorePreviousMode is not called when the context is already in administrator mode.
   */
  @Test
  public void testExecuteAlreadyInAdminMode() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Map<String, Object> parameters = new HashMap<>();

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    // restorePreviousMode should NOT be called since we were already in admin mode
    obContextMock.verify(OBContext::restorePreviousMode, never());
  }

  /**
   * Verifies that execute processes field columns when the tab contains fields with associated columns.
   */
  @Test
  public void testExecuteWithFieldsHavingColumns() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(null);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, "TAB_WITH_FIELDS")).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, "TAB_WITH_FIELDS");

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(mockField).getColumn();
    obContextMock.verify(OBContext::restorePreviousMode);
  }
}
