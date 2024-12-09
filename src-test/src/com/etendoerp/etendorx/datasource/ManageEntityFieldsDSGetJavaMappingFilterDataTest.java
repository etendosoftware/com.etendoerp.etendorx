package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test class for the {@code getJavaMappingFilterData} method in {@link ManageEntityFieldsDS}.
 * This test class verifies the behavior of retrieving Java mapping filter data
 * for a projection entity using mocked dependencies.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSGetJavaMappingFilterDataTest {

  @Mock
  private Session mockSession;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Query<ETRXJavaMapping> mockQuery;

  @Mock
  private ETRXProjectionEntity mockProjectionEntity;

  @Mock
  private ETRXJavaMapping mockJavaMapping;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private ManageEntityFieldsDS dataSource;
  private Method getJavaMappingFilterDataMethod;

  /**
   * Sets up the test environment before each test method.
   * Initializes mocked dependencies, prepares the data source,
   * and sets up the reflection method for testing a private method.
   *
   * @throws Exception if any setup error occurs during initialization
   */
  @Before
  public void setUp() throws Exception {
    dataSource = new ManageEntityFieldsDS();

    getJavaMappingFilterDataMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getJavaMappingFilterData", ETRXProjectionEntity.class);
    getJavaMappingFilterDataMethod.setAccessible(true);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBContext = mockStatic(OBContext.class);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockProjectionEntity.getId()).thenReturn("test-projection-id");

    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

    mockedOBContext.when(OBContext::setAdminMode).then(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).then(invocation -> null);

    when(mockSession.createQuery(anyString(), eq(ETRXJavaMapping.class)))
        .thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any()))
        .thenReturn(mockQuery);
  }

  /**
   * Tears down the test environment after each test method.
   * Closes any mocked static contexts to prevent resource leaks.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
  }

  /**
   * Tests the {@code getJavaMappingFilterData} method with existing results.
   * Verifies that:
   * <ul>
   *   <li>The method returns a list with exactly one record</li>
   *   <li>The returned record has the correct id</li>
   *   <li>The returned record has the correct name and identifier</li>
   *   <li>The returned record contains the correct entity name</li>
   * </ul>
   *
   * @throws Exception if any error occurs during method invocation
   */
  @Test
  public void testGetJavaMappingFilterDataWithResults() throws Exception {
    String expectedId = "test-mapping-id";
    String expectedIdentifier = "test-mapping-identifier";

    when(mockJavaMapping.getId()).thenReturn(expectedId);
    when(mockJavaMapping.getIdentifier()).thenReturn(expectedIdentifier);
    when(mockQuery.list()).thenReturn(Collections.singletonList(mockJavaMapping));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>)
        getJavaMappingFilterDataMethod.invoke(dataSource, mockProjectionEntity);

    assertEquals("Should return one record", 1, result.size());
    Map<String, Object> dataRecord = result.get(0);
    assertEquals("Should have correct id", expectedId, dataRecord.get("id"));
    assertEquals("Should have correct name", expectedIdentifier, dataRecord.get(TestUtils.NAME));
    assertEquals("Should have correct identifier", expectedIdentifier, dataRecord.get("_identifier"));
    assertEquals("Should have correct entity name", ETRXJavaMapping.ENTITY_NAME, dataRecord.get("_entityName"));
  }

  /**
   * Tests the {@code getJavaMappingFilterData} method when no results are found.
   * Verifies that the method returns an empty list when no Java mappings exist
   * for the given projection entity.
   *
   * @throws Exception if any error occurs during method invocation
   */
  @Test
  public void testGetJavaMappingFilterDataNoResults() throws Exception {
    when(mockQuery.list()).thenReturn(Collections.emptyList());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>)
        getJavaMappingFilterDataMethod.invoke(dataSource, mockProjectionEntity);

    assertTrue("Should return empty list", result.isEmpty());
  }

}