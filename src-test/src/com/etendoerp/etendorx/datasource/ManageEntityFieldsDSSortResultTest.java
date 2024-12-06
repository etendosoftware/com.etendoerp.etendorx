package com.etendoerp.etendorx.datasource;

 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mockito.InjectMocks;
 import org.mockito.junit.MockitoJUnitRunner;

 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;

 import com.etendoerp.etendorx.TestUtils;

/**
 * Test class for testing the filter and sorting functionality of ManageEntityFieldsDS.
 * This test suite uses Mockito for unit testing the private method 'applyFiltersAndSort'
 * through reflection. It covers various scenarios of filtering entity fields.
 *
 * @see ManageEntityFieldsDS
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSSortResultTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  private Method applyFiltersAndSortMethod;

  /**
   * Sets up the test environment by making the private 'applyFiltersAndSort' method
   * accessible via reflection for testing.
   *
   * @throws Exception if method reflection fails
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
   * Tests the applyFiltersAndSort method with no filters applied.
   * Verifies that when no filters are provided, the method returns
   * the original list of data without modification.
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testApplyFiltersAndSortNoFilters() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_FIELD);
    testData.add(row1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(testData.size(), result.size());
  }

  /**
   * Tests filtering by mandatory field status.
   * Checks that only fields marked as mandatory are returned
   * when the 'ismandatory' filter is set to true.
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testApplyFiltersAndSortIsMandatoryFilter() throws Exception {
    // Prepare test data
    Map<String, String> parameters = new HashMap<>();
    parameters.put("ismandatory", "true");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.ISMANDATORY, true);
    testData.add(row1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertTrue((Boolean) result.get(0).get(ManageEntityFieldConstants.ISMANDATORY));
  }

  /**
   * Tests filtering by field name.
   * Verifies that fields containing the specified name substring
   * are correctly filtered and returned.
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testApplyFiltersAndSortNameFilter() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("name", "test");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_FIELD);
    testData.add(row1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertTrue(result.get(0).get(ManageEntityFieldConstants.NAME).toString().contains("test"));
  }

  /**
   * Tests applying multiple filters simultaneously.
   * Ensures that fields can be filtered by multiple criteria
   * (such as mandatory status and name) concurrently.
   *
   * @throws Exception if method invocation fails
   */
  @Test
  public void testApplyFiltersAndSortMultipleFilters() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("ismandatory", "true");
    parameters.put("name", "test");

    List<Map<String, Object>> testData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put(ManageEntityFieldConstants.ISMANDATORY, true);
    row1.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_FIELD);
    testData.add(row1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> result = (List<Map<String, Object>>) applyFiltersAndSortMethod.invoke(
        manageEntityFieldsDS,
        parameters,
        testData
    );

    assertEquals(1, result.size());
    assertTrue((Boolean) result.get(0).get(ManageEntityFieldConstants.ISMANDATORY));
    assertTrue(result.get(0).get(ManageEntityFieldConstants.NAME).toString().contains("test"));
  }
  
}
