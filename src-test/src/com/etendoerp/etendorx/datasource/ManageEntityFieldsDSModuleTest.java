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
import org.hibernate.criterion.Criterion;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.etendoerp.etendorx.data.ETRXProjection;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;
import org.openbravo.model.ad.module.Module;

/**
 * Test class for the ManageEntityFieldsDS module's functionality.
 * This class contains unit tests for the {@code getModuleFilterData} method
 * of the ManageEntityFieldsDS class, focusing on different scenarios of
 * module filtering and data retrieval.
 * The tests cover scenarios including:
 * - Module filtering with development modules
 * - Module filtering without development modules
 * - Handling cases with no module results
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSModuleTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private Query<Module> query;

  @Mock
  private OBCriteria<Module> obCriteria;

  @Mock
  private ETRXProjectionEntity projectionEntity;

  @Mock
  private ETRXProjection projection;

  @Mock
  private Module module1;

  @Mock
  private Module module2;

  @Mock
  private Module projectionModule;

  private Method getModuleFilterDataMethod;

  /**
   * Sets up the test environment before each test method.
   *
   * Prepares the private method for testing by making it accessible
   * and setting up mock objects for projection entities and modules.
   *
   * @throws Exception if method setup fails
   */
  @Before
  public void setUp() throws Exception {
    getModuleFilterDataMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getModuleFilterData",
        ETRXProjectionEntity.class
    );
    getModuleFilterDataMethod.setAccessible(true);

    // Basic setup for projection entity and its module
    when(projectionEntity.getId()).thenReturn("test-projection-id");
    when(projectionEntity.getProjection()).thenReturn(projection);
    when(projection.getModule()).thenReturn(projectionModule);
  }

  /**
   * Tests module filter data retrieval with results and a development module.
   * Verifies that:
   * - Correct number of modules are returned
   * - Module details are correctly mapped
   * - Projection module is included when in development
   *
   * @throws Exception if test method invocation fails
   */
  @Test
  public void testGetModuleFilterDataWithResultsAndDevModule() throws Exception {
    when(module1.getId()).thenReturn("module-1");
    when(module1.getIdentifier()).thenReturn("Module 1");
    when(module2.getId()).thenReturn("module-2");
    when(module2.getIdentifier()).thenReturn("Module 2");

    when(projectionModule.getId()).thenReturn("proj-module");
    when(projectionModule.getIdentifier()).thenReturn("Projection Module");
    when(projectionModule.isInDevelopment()).thenReturn(true);

    List<Module> mockModules = Arrays.asList(module1, module2);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(Module.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(mockModules);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getModuleFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertEquals(3, result.size());

        Map<String, Object> firstResult = result.get(0);
        assertEquals("module-1", firstResult.get("id"));
        assertEquals("Module 1", firstResult.get("name"));
        assertEquals(Module.ENTITY_NAME, firstResult.get("_entityName"));

        Map<String, Object> projModuleResult = result.get(2);
        assertEquals("proj-module", projModuleResult.get("id"));
        assertEquals("Projection Module", projModuleResult.get("name"));

        obContextMock.verify(OBContext::setAdminMode);
        obContextMock.verify(OBContext::restorePreviousMode);
      }
    }
  }

  /**
   * Tests module filter data retrieval for non-development modules with criteria.
   * Checks the behavior when:
   * - Projection module is not in development
   * - Additional module filtering criteria are applied
   * - A development module is fetched separately
   *
   * @throws Exception if test method invocation fails
   */
  @Test
  public void testGetModuleFilterDataWithNonDevModuleAndCriteria() throws Exception {
    when(module1.getId()).thenReturn("module-1");
    when(module1.getIdentifier()).thenReturn("Module 1");

    when(projectionModule.isInDevelopment()).thenReturn(false);

    Module inDevModule = mock(Module.class);
    when(inDevModule.getId()).thenReturn("dev-module");
    when(inDevModule.getIdentifier()).thenReturn("Dev Module");

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(Module.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(module1));

        when(obDal.createCriteria(Module.class)).thenReturn(obCriteria);
        when(obCriteria.add(any(Criterion.class))).thenReturn(obCriteria);
        when(obCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(obCriteria);
        when(obCriteria.setMaxResults(1)).thenReturn(obCriteria);
        when(obCriteria.uniqueResult()).thenReturn(inDevModule);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getModuleFilterDataMethod.invoke(
            manageEntityFieldsDS,
            projectionEntity
        );

        assertEquals(2, result.size());

        Map<String, Object> firstResult = result.get(0);
        assertEquals("module-1", firstResult.get("id"));

        Map<String, Object> devModuleResult = result.get(1);
        assertEquals("dev-module", devModuleResult.get("id"));
        assertEquals("Dev Module", devModuleResult.get("name"));

        verify(obCriteria, times(3)).add(any(Criterion.class));
        verify(obCriteria).setMaxResults(1);
        verify(obCriteria).uniqueResult();
      }
    }
  }

  /**
   * Tests module filter data retrieval when no modules are found.
   * Ensures that:
   * - An empty list is returned when no modules match the criteria
   * - No exceptions are thrown during the process
   *
   * @throws Exception if test method invocation fails
   */
  @Test
  public void testGetModuleFilterDataNoResults() throws Exception {
    when(projectionModule.isInDevelopment()).thenReturn(false);

    try (MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
        obDalMock.when(OBDal::getInstance).thenReturn(obDal);
        when(obDal.getSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(Module.class))).thenReturn(query);
        when(query.setParameter(eq("etrxProjectionEntityId"), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        when(obDal.createCriteria(Module.class)).thenReturn(obCriteria);
        when(obCriteria.add(any(Criterion.class))).thenReturn(obCriteria);
        when(obCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(obCriteria);
        when(obCriteria.setMaxResults(1)).thenReturn(obCriteria);
        when(obCriteria.uniqueResult()).thenReturn(null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) getModuleFilterDataMethod.invoke(
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