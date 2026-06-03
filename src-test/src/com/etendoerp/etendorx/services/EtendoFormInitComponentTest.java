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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Validation;
import org.openbravo.model.ad.ui.Field;

/**
 * Tests for {@link EtendoFormInitComponent}.
 * <p>
 * The component prepares the lazy metadata of the SAME cached {@code Field}/{@code Column}
 * instances {@code FormInitializationComponent} reads (via {@code ApplicationDictionaryCachedStructures}),
 * re-attaching detached columns to the current session and evicting them so the initialized
 * proxies survive FIC's per-call {@code session.clear()} (issue #92 / ETP-4137).
 */
public class EtendoFormInitComponentTest {

  private static final String TAB_ID = "TAB_ID";
  private static final String TEST_TAB_ID = "TEST_TAB_ID";

  private MockedStatic<OBContext> obContextMock;
  private MockedStatic<OBDal> obDalMock;
  private MockedStatic<WeldUtils> weldMock;
  private MockedStatic<Hibernate> hibernateMock;

  private OBContext mockContext;
  private OBDal mockDal;
  private Session mockSession;
  private ApplicationDictionaryCachedStructures mockCachedStructures;
  private EtendoFormInitComponent component;
  private JSONObject superResult;

  /**
   * Sets up static mocks for OBContext, OBDal, WeldUtils and Hibernate, and a spied component whose
   * super.execute() is stubbed.
   *
   * @throws Exception if the stubbed super result JSON cannot be parsed
   */
  @Before
  public void setUp() throws Exception {
    mockContext = mock(OBContext.class);
    mockDal = mock(OBDal.class);
    mockSession = mock(Session.class);
    mockCachedStructures = mock(ApplicationDictionaryCachedStructures.class);

    obContextMock = mockStatic(OBContext.class);
    obContextMock.when(OBContext::getOBContext).thenReturn(mockContext);

    obDalMock = mockStatic(OBDal.class);
    obDalMock.when(OBDal::getInstance).thenReturn(mockDal);
    when(mockDal.getSession()).thenReturn(mockSession);

    weldMock = mockStatic(WeldUtils.class);
    weldMock.when(() -> WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class))
        .thenReturn(mockCachedStructures);

    hibernateMock = mockStatic(Hibernate.class);

    superResult = new JSONObject("{\"status\":\"ok\"}");

    component = spy(new EtendoFormInitComponent());
    doReturn(superResult).when(component).invokeSuperExecute(any(Map.class), anyString());
  }

  /**
   * Closes all static mocks (reverse creation order).
   */
  @After
  public void tearDown() {
    hibernateMock.close();
    weldMock.close();
    obDalMock.close();
    obContextMock.close();
  }

  private Map<String, Object> params(String tabId) {
    Map<String, Object> parameters = new HashMap<>();
    if (tabId != null) {
      parameters.put(TAB_ID, tabId);
    }
    return parameters;
  }

  private Field fieldWithColumn(Column column) {
    Field field = mock(Field.class);
      doReturn(column).when(field).getColumn();
    return field;
  }

  private Column columnWithValidation(Validation validation) {
    Column column = mock(Column.class);
    when(column.getValidation()).thenReturn(validation);
    when(column.getCallout()).thenReturn(null);
    when(column.getReference()).thenReturn(null);
    when(column.getReferenceSearchKey()).thenReturn(null);
    return column;
  }

  /**
   * When no tab id is provided, metadata preparation is skipped entirely (the cache is never
   * queried) and the previous OBContext mode is restored.
   */
  @Test
  public void testNullTabIdSkipsMetadataInitialization() {
    when(mockContext.isInAdministratorMode()).thenReturn(false);

    JSONObject result = component.execute(params(null), "{}");

    assertEquals(superResult, result);
    weldMock.verify(() -> WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class),
        never());
    obContextMock.verify(OBContext::restorePreviousMode);
  }

  /**
   * When already in administrator mode, the previous mode is NOT restored.
   */
  @Test
  public void testAlreadyInAdminModeDoesNotRestore() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    JSONObject result = component.execute(params(null), "{}");

    assertNotNull(result);
    obContextMock.verify(OBContext::restorePreviousMode, never());
  }

  /**
   * A detached column whose validation proxy is uninitialized is re-attached (lock + refresh) and
   * its validation is force-initialized within the current session.
   */
  @Test
  public void testDetachedUninitializedValidationIsReattachedAndInitialized() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Validation validation = mock(Validation.class);
    Column column = columnWithValidation(validation);
    Field field = fieldWithColumn(column);
    when(mockCachedStructures.getFieldsOfTab(TEST_TAB_ID)).thenReturn(
      Collections.singletonList(field));
    hibernateMock.when(() -> Hibernate.isInitialized(validation)).thenReturn(false);
    when(mockSession.contains(column)).thenReturn(false);
    Session.LockRequest lockRequest = mock(Session.LockRequest.class);
    when(mockSession.buildLockRequest(any(LockOptions.class))).thenReturn(lockRequest);

    component.execute(params(TEST_TAB_ID), "{}");

    verify(mockSession).buildLockRequest(any(LockOptions.class));
    verify(lockRequest).lock(column);
    verify(mockSession).refresh(column);
    verify(validation).getValidationCode();
  }

  /**
   * Already-initialized metadata is left untouched: no re-attach, no refresh, no re-initialization.
   * This is the steady state that keeps concurrent requests lock-free.
   */
  @Test
  public void testInitializedMetadataIsSkipped() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Validation validation = mock(Validation.class);
    Column column = columnWithValidation(validation);
    Field field = fieldWithColumn(column);
    when(mockCachedStructures.getFieldsOfTab(TEST_TAB_ID)).thenReturn(
      Collections.singletonList(field));
    hibernateMock.when(() -> Hibernate.isInitialized(validation)).thenReturn(true);

    component.execute(params(TEST_TAB_ID), "{}");

    verify(mockSession, never()).buildLockRequest(any(LockOptions.class));
    verify(validation, never()).getValidationCode();
  }

  /**
   * Fields whose column is null are ignored without touching the session.
   */
  @Test
  public void testNullColumnFieldIsIgnored() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);
    Field field = fieldWithColumn(null);
    when(mockCachedStructures.getFieldsOfTab(TEST_TAB_ID)).thenReturn(
      Collections.singletonList(field));

    JSONObject result = component.execute(params(TEST_TAB_ID), "{}");

    assertNotNull(result);
    verify(mockSession, never()).buildLockRequest(any(LockOptions.class));
  }

  /**
   * A failure preparing one column must not abort the tab loop: later columns are still prepared,
   * otherwise FIC would fail on them.
   */
  @Test
  public void testOneColumnFailureDoesNotAbortRemainingColumns() {
    when(mockContext.isInAdministratorMode()).thenReturn(true);

    Validation validation1 = mock(Validation.class);
    Column column1 = columnWithValidation(validation1);
    Field field1 = fieldWithColumn(column1);
    Validation validation2 = mock(Validation.class);
    Column column2 = columnWithValidation(validation2);
    Field field2 = fieldWithColumn(column2);

    when(mockCachedStructures.getFieldsOfTab(TEST_TAB_ID)).thenReturn(
      Arrays.asList(field1, field2));
    hibernateMock.when(() -> Hibernate.isInitialized(validation1)).thenReturn(false);
    hibernateMock.when(() -> Hibernate.isInitialized(validation2)).thenReturn(false);

    Session.LockRequest lockRequest = mock(Session.LockRequest.class);
    when(mockSession.buildLockRequest(any(LockOptions.class))).thenReturn(lockRequest);
    // First column blows up during re-attach; the second must still be processed.
    doThrow(new RuntimeException("reattach boom")).when(lockRequest).lock(column1);

    component.execute(params(TEST_TAB_ID), "{}");

    verify(validation2).getValidationCode();
  }
}
