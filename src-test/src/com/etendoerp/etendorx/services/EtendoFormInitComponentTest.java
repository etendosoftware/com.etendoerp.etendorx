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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.openbravo.model.ad.domain.Callout;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

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

  /**
   * Verifies that execute eagerly initializes callout and its model implementation list.
   */
  @Test
  public void testExecuteInitializesCalloutWithModelImplementations() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Callout mockCallout = mock(Callout.class);
    when(mockCallout.getId()).thenReturn("CALLOUT1");
    doReturn(new ArrayList<>()).when(mockCallout).getADModelImplementationList();

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(mockCallout);
    when(mockColumn.getReference()).thenReturn(null);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(mockCallout).getId();
    verify(mockCallout, org.mockito.Mockito.atLeastOnce()).getADModelImplementationList();
  }

  /**
   * Verifies that execute eagerly initializes callout but handles null model implementation list.
   */
  @Test
  public void testExecuteInitializesCalloutWithNullModelImplList() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Callout mockCallout = mock(Callout.class);
    when(mockCallout.getId()).thenReturn("CALLOUT2");
    doReturn(null).when(mockCallout).getADModelImplementationList();

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(mockCallout);
    when(mockColumn.getReference()).thenReturn(null);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(mockCallout).getId();
  }

  /**
   * Verifies that execute eagerly initializes reference with selector list and selector fields.
   */
  @Test
  public void testExecuteInitializesReferenceWithSelectorFields() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    SelectorField sf1 = mock(SelectorField.class);
    when(sf1.getId()).thenReturn("SF1");
    when(sf1.getProperty()).thenReturn("businessPartner");

    SelectorField sf2 = mock(SelectorField.class);
    when(sf2.getId()).thenReturn("SF2");
    when(sf2.getProperty()).thenReturn(null);

    Selector selector = mock(Selector.class);
    when(selector.getOBUISELSelectorFieldList()).thenReturn(Arrays.asList(sf1, sf2));

    Reference reference = mock(Reference.class);
    when(reference.getId()).thenReturn("REF1");
    when(reference.getOBUISELSelectorList()).thenReturn(Collections.singletonList(selector));

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(reference);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(reference).getId();
    verify(sf1).getId();
    verify(sf2).getId();
  }

  /**
   * Verifies that execute eagerly initializes reference with null selector list.
   */
  @Test
  public void testExecuteInitializesReferenceWithNullSelectorList() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Reference reference = mock(Reference.class);
    when(reference.getId()).thenReturn("REF2");
    when(reference.getOBUISELSelectorList()).thenReturn(null);

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(reference);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(reference).getId();
    verify(reference).getOBUISELSelectorList();
  }

  /**
   * Verifies that execute eagerly initializes referenceSearchKey with selector list.
   */
  @Test
  public void testExecuteInitializesRefSearchKeyWithSelectorList() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Selector selector = mock(Selector.class);
    when(selector.getOBUISELSelectorFieldList()).thenReturn(new ArrayList<>());

    Reference refSearchKey = mock(Reference.class);
    when(refSearchKey.getId()).thenReturn("RSK1");
    when(refSearchKey.getOBUISELSelectorList()).thenReturn(Collections.singletonList(selector));

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(null);
    when(mockColumn.getReferenceSearchKey()).thenReturn(refSearchKey);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(refSearchKey).getId();
    verify(refSearchKey, org.mockito.Mockito.atLeastOnce()).getOBUISELSelectorList();
  }

  /**
   * Verifies that execute eagerly initializes referenceSearchKey with null selector list.
   */
  @Test
  public void testExecuteInitializesRefSearchKeyWithNullSelectorList() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Reference refSearchKey = mock(Reference.class);
    when(refSearchKey.getId()).thenReturn("RSK2");
    when(refSearchKey.getOBUISELSelectorList()).thenReturn(null);

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(null);
    when(mockColumn.getReferenceSearchKey()).thenReturn(refSearchKey);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(refSearchKey).getId();
    verify(refSearchKey).getOBUISELSelectorList();
  }

  /**
   * Verifies that execute handles field with null column gracefully (skips initialization).
   */
  @Test
  public void testExecuteWithFieldNullColumn() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Field fieldWithNull = mock(Field.class);
    when(fieldWithNull.getColumn()).thenReturn(null);

    Field fieldWithColumn = mock(Field.class);
    Column col = mock(Column.class);
    when(fieldWithColumn.getColumn()).thenReturn(col);
    when(col.getCallout()).thenReturn(null);
    when(col.getReference()).thenReturn(null);
    when(col.getReferenceSearchKey()).thenReturn(null);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Arrays.asList(fieldWithNull, fieldWithColumn));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(fieldWithNull).getColumn();
    verify(fieldWithColumn).getColumn();
  }

  /**
   * Verifies that execute handles selector with null selectorFieldList gracefully.
   */
  @Test
  public void testExecuteInitializesSelectorWithNullFieldList() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Selector selector = mock(Selector.class);
    when(selector.getOBUISELSelectorFieldList()).thenReturn(null);

    Reference reference = mock(Reference.class);
    when(reference.getId()).thenReturn("REF3");
    when(reference.getOBUISELSelectorList()).thenReturn(Collections.singletonList(selector));

    Column mockColumn = mock(Column.class);
    when(mockColumn.getCallout()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(reference);
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);

    Field mockField = mock(Field.class);
    when(mockField.getColumn()).thenReturn(mockColumn);

    Tab mockTab = mock(Tab.class);
    when(mockDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getADFieldList()).thenReturn(Collections.singletonList(mockField));

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TAB_ID, TEST_TAB_ID);

    JSONObject result = component.execute(parameters, "{}");

    assertNotNull(result);
    verify(selector).getOBUISELSelectorFieldList();
  }
}
