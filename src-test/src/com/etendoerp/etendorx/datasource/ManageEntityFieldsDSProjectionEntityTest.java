package com.etendoerp.etendorx.datasource;

import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.hibernate.Session;
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
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test class for the ManageEntityFieldsDS, specifically testing the projection entity related filter data retrieval.
 * This test class verifies the behavior of the {@code getProjectionEntityRelatedFilterData} method
 * under different scenarios using Mockito for mocking dependencies.
 *
 * @author Generated
 * @version 1.0
 * @see ManageEntityFieldsDS
 * @see ETRXProjectionEntity
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSProjectionEntityTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private Query<ETRXProjectionEntity> query;

  @Mock
  private ETRXProjectionEntity relatedEntity1;

  @Mock
  private ETRXProjectionEntity relatedEntity2;

  private Method getProjectionEntityRelatedFilterDataMethod;

  /**
   * Sets up the test environment before each test method.
   * Prepares the private reflection method for testing by making it accessible
   * and retrieving it from the ManageEntityFieldsDS class.
   *
   * @throws Exception if method retrieval fails
   */
  @Before
  public void setUp() throws Exception {
    getProjectionEntityRelatedFilterDataMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getProjectionEntityRelatedFilterData",
        ETRXProjectionEntity.class
    );
    getProjectionEntityRelatedFilterDataMethod.setAccessible(true);
  }

  /**
   * Tests the retrieval of projection entity related filter data when results are present.
   * Verifies that:
   * - The method correctly retrieves related entities
   * - The returned list contains the expected number of results
   * - Each result map has correct id, name, identifier, and entity name
   * - Admin mode is set and restored properly
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testGetProjectionEntityRelatedFilterDataWithResults() throws Exception {
    ETRXProjectionEntity mainEntity = new ETRXProjectionEntity();
    String entityId = "main-entity-id";
    mainEntity.setId(entityId);

    when(relatedEntity1.getId()).thenReturn("related-1");
    when(relatedEntity1.getIdentifier()).thenReturn("Related Entity 1");

    when(relatedEntity2.getId()).thenReturn("related-2");
    when(relatedEntity2.getIdentifier()).thenReturn("Related Entity 2");

    List<ETRXProjectionEntity> mockRelatedEntities = Arrays.asList(relatedEntity1, relatedEntity2);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ETRXProjectionEntity.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(mockRelatedEntities);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getProjectionEntityRelatedFilterDataMethod.invoke(
            manageEntityFieldsDS,
            mainEntity
        );

        assertEquals(2, result.size());

        Map<String, Object> firstResult = result.get(0);
        assertEquals("related-1", firstResult.get("id"));
        assertEquals("Related Entity 1", firstResult.get("name"));
        assertEquals("Related Entity 1", firstResult.get("_identifier"));
        assertEquals(ETRXProjectionEntity.ENTITY_NAME, firstResult.get("_entityName"));

        Map<String, Object> secondResult = result.get(1);
        assertEquals("related-2", secondResult.get("id"));
        assertEquals("Related Entity 2", secondResult.get("name"));
        assertEquals("Related Entity 2", secondResult.get("_identifier"));
        assertEquals(ETRXProjectionEntity.ENTITY_NAME, secondResult.get("_entityName"));

        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }

  /**
   * Tests the retrieval of projection entity related filter data when no results are found.
   * Verifies that:
   * - An empty list is returned when no related entities exist
   * - Admin mode is set and restored properly
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testGetProjectionEntityRelatedFilterDataNoResults() throws Exception {
    ETRXProjectionEntity mainEntity = new ETRXProjectionEntity();
    mainEntity.setId("main-entity-id");

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ETRXProjectionEntity.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getProjectionEntityRelatedFilterDataMethod.invoke(
            manageEntityFieldsDS,
            mainEntity
        );

        assertTrue(result.isEmpty());
        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }
}