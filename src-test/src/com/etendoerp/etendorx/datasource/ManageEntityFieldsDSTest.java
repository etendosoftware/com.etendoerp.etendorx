package com.etendoerp.etendorx.datasource;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.etendoerp.etendorx.data.ETRXProjection;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Unit test class for {@link ManageEntityFieldsDS}.
 * Validates the behavior of data retrieval with distinct modules and filters.
 * Uses Mockito to mock dependencies and simulate behavior.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSTest {

  private static final String TEST_PROJECTION_ID = "testProjectionId";
  private static final String TEST_MODULE = "Test Module";

  private ManageEntityFieldsDS dataSource;

  private Method getIdMethod;

  @Mock
  private OBDal mockDal;

  @Mock
  private OBContext mockContext;

  @Mock
  private ModelProvider mockModelProvider;

  @Mock
  private Session mockSession;

  @Mock
  private ETRXProjectionEntity mockProjectionEntity;

  @Mock
  private Module mockModule;


  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<ModelProvider> mockedModelProvider;

  /**
   * Initializes the test environment before each test.
   * Sets up mocked static dependencies and injects mock objects.
   */
  @Before
  public void setUp() throws NoSuchMethodException {
    dataSource = new ManageEntityFieldsDS();
    getIdMethod = ManageEntityFieldsDS.class.getDeclaredMethod("getId");
    getIdMethod.setAccessible(true);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedModelProvider = mockStatic(ModelProvider.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

    when(mockDal.getSession()).thenReturn(mockSession);

    when(mockProjectionEntity.getId()).thenReturn(TEST_PROJECTION_ID);
    when(mockProjectionEntity.getProjection()).thenReturn(mock(ETRXProjection.class));
    when(mockProjectionEntity.getProjection().getModule()).thenReturn(mockModule);
  }

  /**
   * Tests {@link ManageEntityFieldsDS#getData(Map, int, int)} for retrieving distinct module data.
   * Verifies that the returned list contains the expected module data.
   */
  @Test
  public void testGetDataWithDistinctModule() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("@ETRX_Projection_Entity.id@", TEST_PROJECTION_ID);
    parameters.put("_distinct", "module");

    when(mockModule.getId()).thenReturn("testModuleId");
    when(mockModule.getIdentifier()).thenReturn(TEST_MODULE);
    when(mockModule.isInDevelopment()).thenReturn(true);

    when(mockDal.get(ETRXProjectionEntity.class, TEST_PROJECTION_ID)).thenReturn(mockProjectionEntity);
    when(mockProjectionEntity.getId()).thenReturn(TEST_PROJECTION_ID);
    when(mockProjectionEntity.getProjection().getModule()).thenReturn(mockModule);

    Query<Module> moduleQuery = mock(Query.class);
    when(mockSession.createQuery(anyString(), eq(Module.class))).thenReturn(moduleQuery);
    when(moduleQuery.setParameter(anyString(), any())).thenReturn(moduleQuery);
    when(moduleQuery.list()).thenReturn(Collections.singletonList(mockModule));

    List<Map<String, Object>> result = dataSource.getData(parameters, 0, 10);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());

    Map<String, Object> moduleData = result.get(0);
    assertEquals("testModuleId", moduleData.get("id"));
    assertEquals(TEST_MODULE, moduleData.get("name"));
    assertEquals(TEST_MODULE, moduleData.get("_identifier"));
    assertEquals(Module.ENTITY_NAME, moduleData.get("_entityName"));
  }

  /**
   * Verifies that {@code getId} throws an {@link InvocationTargetException}
   * when a null result is encountered.
   * @throws Exception if any reflection-related error occurs during the invocation
   */
  @Test(expected = InvocationTargetException.class)
  public void testGetIdNullResult() throws Exception {
    getIdMethod.invoke(dataSource);
  }

  /**
   * Cleans up the mocked static dependencies after each test.
   * Ensures no resource leaks or interference between tests.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedModelProvider != null) {
      mockedModelProvider.close();
    }
  }
}