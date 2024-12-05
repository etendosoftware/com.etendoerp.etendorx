package com.etendoerp.etendorx.datasource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.ad.module.Module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test class for testing the filter and sorting functionality of ManageEntityFieldsDS.
 * This test suite uses Mockito for unit testing the private method 'applyFiltersAndSort'
 * through reflection.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSApplyFiltersAndSortTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  @Mock
  private Module mockModule;

  private Method applyFiltersAndSortMethod;

  /**
   * Prepares the test environment by setting up the private method for reflection.
   * This method is executed before each test to make the protected 'applyFiltersAndSort'
   * method accessible for testing.
   *
   * @throws Exception if there's an issue accessing the method via reflection
   */
  @Before
  public void setUp() throws Exception {
    applyFiltersAndSortMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "applyFiltersAndSort",
        Map.class,
        List.class
    );
    applyFiltersAndSortMethod.setAccessible(true);
  }

  /**
   * Tests filtering of records based on the mandatory field status.
   * Verifies that the method correctly filters and returns only records
   * where the 'ismandatory' field matches the specified boolean value.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testIsMandatoryFilter() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("criteria", "[{\"fieldName\":\"ismandatory\",\"value\":true,\"operator\":\"equals\"}]");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.ISMANDATORY, true);
    Map<String, Object> row2 = new HashMap<>();
    row2.put(ManageEntityFieldConstants.ISMANDATORY, false);
    testData.addAll(Arrays.asList(row1, row2));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertEquals(true, result.get(0).get(ManageEntityFieldConstants.ISMANDATORY));
  }

  /**
   * Tests filtering of records based on the field name.
   * Checks that the method can successfully filter records by performing
   * a contains search on the 'name' field.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testNameFilter() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("criteria", "[{\"fieldName\":\"name\",\"value\":\"test\",\"operator\":\"contains\"}]");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.NAME, "test_field");
    Map<String, Object> row2 = new HashMap<>();
    row2.put(ManageEntityFieldConstants.NAME, "other_field");
    testData.addAll(Arrays.asList(row1, row2));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertEquals("test_field", result.get(0).get(ManageEntityFieldConstants.NAME));
  }

  /**
   * Tests filtering of records based on the associated module.
   * Ensures that records can be filtered by matching the module identifier.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testModuleFilter() throws Exception {
    String moduleId = "test-module-id";
    when(mockModule.getId()).thenReturn(moduleId);

    Map<String, String> parameters = new HashMap<>();
    parameters.put("criteria", "[{\"fieldName\":\"module\",\"value\":\"" + moduleId + "\",\"operator\":\"equals\"}]");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.MODULE, mockModule);
    Map<String, Object> row2 = new HashMap<>();
    row2.put(ManageEntityFieldConstants.MODULE, mock(Module.class));
    testData.addAll(Arrays.asList(row1, row2));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertEquals(mockModule, result.get(0).get(ManageEntityFieldConstants.MODULE));
  }

  /**
   * Tests applying multiple filters simultaneously.
   * Verifies that the method can correctly handle and apply multiple filtering
   * criteria in a single operation, returning only records that satisfy all conditions.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testMultipleFilters() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("criteria", "[" +
        "{\"fieldName\":\"ismandatory\",\"value\":true,\"operator\":\"equals\"}," +
        "{\"fieldName\":\"name\",\"value\":\"test\",\"operator\":\"contains\"}" +
        "]");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.ISMANDATORY, true);
    row1.put(ManageEntityFieldConstants.NAME, "test_field");
    Map<String, Object> row2 = new HashMap<>();
    row2.put(ManageEntityFieldConstants.ISMANDATORY, true);
    row2.put(ManageEntityFieldConstants.NAME, "other_field");
    Map<String, Object> row3 = new HashMap<>();
    row3.put(ManageEntityFieldConstants.ISMANDATORY, false);
    row3.put(ManageEntityFieldConstants.NAME, "test_wrong");
    testData.addAll(Arrays.asList(row1, row2, row3));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertEquals(true, result.get(0).get(ManageEntityFieldConstants.ISMANDATORY));
    assertEquals("test_field", result.get(0).get(ManageEntityFieldConstants.NAME));
  }

  /**
   * Tests the behavior of filtering with empty or null filter conditions.
   * Confirms that when no filter criteria are specified, all records
   * are returned unmodified.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testEmptyFilters() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("criteria", "[]");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.NAME, "test");
    testData.add(row1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(testData.size(), result.size());
  }
}
