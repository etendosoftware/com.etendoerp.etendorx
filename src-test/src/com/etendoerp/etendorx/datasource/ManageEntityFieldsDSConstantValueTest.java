package com.etendoerp.etendorx.datasource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test class for the ManageEntityFieldsDS (Data Source) focusing on Constant Value filter data retrieval.
 *
 * This test class uses Mockito to create mocks and verify the behavior of the getConstantValueFilterData method
 * under different scenarios, such as when results are present and when no results are found.
 * @since 2024-12-04
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSConstantValueTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private Query<ConstantValue> query;

  @Mock
  private ConstantValue constantValue1;

  @Mock
  private ConstantValue constantValue2;

  private Method getConstantValueFilterDataMethod;

  /**
   * Sets up the test environment before each test method.
   * Retrieves the private method 'getConstantValueFilterData' using reflection and makes it accessible
   * for testing purposes. This allows testing of the private method without modifying its original implementation.
   *
   * @throws Exception if there is an error accessing the method via reflection
   */
  @Before
  public void setUp() throws Exception {
    getConstantValueFilterDataMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getConstantValueFilterData",
        ETRXProjectionEntity.class
    );
    getConstantValueFilterDataMethod.setAccessible(true);
  }

  /**
   * Tests the getConstantValueFilterData method when results are available.
   * Verifies that:
   * - The method returns the correct number of results
   * - Each result contains the expected properties (id, name, _identifier, _entityName)
   * - OBContext is properly set to admin mode and restored
   *
   * @throws Exception if an error occurs during method invocation or reflection
   */
  @Test
  public void testGetConstantValueFilterDataWithResults() throws Exception {
    ETRXProjectionEntity projectionEntity = new ETRXProjectionEntity();
    String entityId = "test-entity-id";
    projectionEntity.setId(entityId);

    when(constantValue1.getId()).thenReturn("cv-1");
    when(constantValue1.getIdentifier()).thenReturn("Constant 1");

    when(constantValue2.getId()).thenReturn("cv-2");
    when(constantValue2.getIdentifier()).thenReturn("Constant 2");

    List<ConstantValue> mockConstantValues = Arrays.asList(constantValue1, constantValue2);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ConstantValue.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(mockConstantValues);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getConstantValueFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertEquals(2, result.size());

        Map<String, Object> firstResult = result.get(0);
        assertEquals("cv-1", firstResult.get("id"));
        assertEquals("Constant 1", firstResult.get("name"));
        assertEquals("Constant 1", firstResult.get("_identifier"));
        assertEquals(ConstantValue.ENTITY_NAME, firstResult.get("_entityName"));

        Map<String, Object> secondResult = result.get(1);
        assertEquals("cv-2", secondResult.get("id"));
        assertEquals("Constant 2", secondResult.get("name"));
        assertEquals("Constant 2", secondResult.get("_identifier"));
        assertEquals(ConstantValue.ENTITY_NAME, secondResult.get("_entityName"));

        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }

  /**
   * Tests the getConstantValueFilterData method when no results are available.
   * Verifies that:
   * - An empty list is returned when no constant values are found
   * - OBContext is properly set to admin mode and restored
   *
   * @throws Exception if an error occurs during method invocation or reflection
   */
  @Test
  public void testGetConstantValueFilterDataNoResults() throws Exception {
    ETRXProjectionEntity projectionEntity = new ETRXProjectionEntity();
    projectionEntity.setId("test-entity-id");

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ConstantValue.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getConstantValueFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertTrue(result.isEmpty());
        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }
}