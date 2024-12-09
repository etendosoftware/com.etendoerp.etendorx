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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test class for the ManageEntityFieldsDS class, focusing on the getJavaMappingFilterData method.
 * This class uses Mockito for mocking dependencies and testing the method's behavior.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSJavaMappingTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private Query<ETRXJavaMapping> query;

  @Mock
  private ETRXJavaMapping javaMapping1;

  @Mock
  private ETRXJavaMapping javaMapping2;

  private Method getJavaMappingFilterDataMethod;

  /**
   * Sets up the test environment by using reflection to access the private
   * getJavaMappingFilterData method of the ManageEntityFieldsDS class.
   *
   * This method is called before each test method to prepare the test fixture
   * by making the private method accessible for testing.
   *
   * @throws Exception if method reflection fails, such as if the method cannot be found
   */
  @Before
  public void setUp() throws Exception {
    getJavaMappingFilterDataMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getJavaMappingFilterData",
        ETRXProjectionEntity.class
    );
    getJavaMappingFilterDataMethod.setAccessible(true);
  }

  /**
   * Tests the getJavaMappingFilterData method when Java mappings are found.
   * Verifies that:
   * - The method returns a list of maps with correct data
   * - Each map contains the expected keys and values
   * - Admin mode is set and restored correctly
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testGetJavaMappingFilterDataWithResults() throws Exception {
    ETRXProjectionEntity projectionEntity = mock(ETRXProjectionEntity.class);
    when(projectionEntity.getId()).thenReturn("test-projection-id");

    when(javaMapping1.getId()).thenReturn("java-mapping-1");
    when(javaMapping1.getIdentifier()).thenReturn(TestUtils.JAVA_MAPPING_1);

    when(javaMapping2.getId()).thenReturn("java-mapping-2");
    when(javaMapping2.getIdentifier()).thenReturn(TestUtils.JAVA_MAPPING_2);

    List<ETRXJavaMapping> mockJavaMappings = Arrays.asList(javaMapping1, javaMapping2);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ETRXJavaMapping.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(mockJavaMappings);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getJavaMappingFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertEquals(2, result.size());

        Map<String, Object> firstResult = result.get(0);
        assertEquals("java-mapping-1", firstResult.get("id"));
        assertEquals(TestUtils.JAVA_MAPPING_1, firstResult.get(TestUtils.NAME));
        assertEquals(TestUtils.JAVA_MAPPING_1, firstResult.get("_identifier"));
        assertEquals(ETRXJavaMapping.ENTITY_NAME, firstResult.get("_entityName"));

        Map<String, Object> secondResult = result.get(1);
        assertEquals("java-mapping-2", secondResult.get("id"));
        assertEquals(TestUtils.JAVA_MAPPING_2, secondResult.get(TestUtils.NAME));
        assertEquals(TestUtils.JAVA_MAPPING_2, secondResult.get("_identifier"));
        assertEquals(ETRXJavaMapping.ENTITY_NAME, secondResult.get("_entityName"));

        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }

  /**
   * Tests the getJavaMappingFilterData method when no Java mappings are found.
   * Verifies that:
   * - An empty list is returned when no mappings exist
   * - The result is not null
   * - Admin mode is set and restored correctly
   * - Appropriate methods are called on the session and OBDal
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testGetJavaMappingFilterDataNoResults() throws Exception {
    ETRXProjectionEntity projectionEntity = mock(ETRXProjectionEntity.class);
    when(projectionEntity.getId()).thenReturn("test-projection-id");

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(ETRXJavaMapping.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getJavaMappingFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);

        verify(session).createQuery(anyString(), eq(ETRXJavaMapping.class));
        verify(obDal).getSession();
      }
    }
  }

}