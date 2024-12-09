package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.etendoerp.etendorx.TestUtils;

/**
 * Test class for the {@code readCriteria} method in {@link ManageEntityFieldsDS}.
 * <p>
 * This class uses reflection to test the private {@code readCriteria} method
 * and validate its behavior with different input criteria.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSReadCriteriaTest {

  @InjectMocks
  private ManageEntityFieldsDS manageEntityFieldsDS;

  private Method readCriteriaMethod;
  private Class<?> entityFieldSelectedFiltersClass;

  /**
   * Sets up the test environment by initializing the required methods and classes via reflection.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Before
  public void setUp() throws Exception {
    readCriteriaMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "readCriteria",
        Map.class
    );
    readCriteriaMethod.setAccessible(true);

    entityFieldSelectedFiltersClass = Class.forName(
        "com.etendoerp.etendorx.datasource.ManageEntityFieldsDS$EntityFieldSelectedFilters");
    Constructor<?> entityFieldSelectedFiltersConstructor = entityFieldSelectedFiltersClass.getDeclaredConstructor(
        ManageEntityFieldsDS.class);
    entityFieldSelectedFiltersConstructor.setAccessible(true);
  }

  /**
   * Tests the {@code readCriteria} method with simple criteria.
   * <p>
   * Validates that the method correctly parses basic criteria into the expected filter object.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testReadCriteriaSimpleCriteria() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TestUtils.CRITERIA, createSimpleCriteriaJson());

    Object result = readCriteriaMethod.invoke(
        manageEntityFieldsDS,
        parameters
    );

    assertNotNull(TestUtils.RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue(entityFieldSelectedFiltersClass.isInstance(result));

    Method getNameMethod = entityFieldSelectedFiltersClass.getDeclaredMethod("getName");
    Method getIsMandatoryMethod = entityFieldSelectedFiltersClass.getDeclaredMethod("getIsmandatory");
    getNameMethod.setAccessible(true);
    getIsMandatoryMethod.setAccessible(true);

    assertEquals("Test Name", getNameMethod.invoke(result));
    assertEquals(true, getIsMandatoryMethod.invoke(result));
  }

  /**
   * Tests the {@code readCriteria} method with advanced criteria.
   * <p>
   * Verifies that the method handles nested and more complex criteria.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testReadCriteriaAdvancedCriteria() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TestUtils.CRITERIA, createAdvancedCriteriaJson());

    Object result = readCriteriaMethod.invoke(
        manageEntityFieldsDS,
        parameters
    );

    assertNotNull(TestUtils.RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue(entityFieldSelectedFiltersClass.isInstance(result));

    Method getNameMethod = entityFieldSelectedFiltersClass.getDeclaredMethod("getName");
    Method getIdentifiesUnivocallyMethod = entityFieldSelectedFiltersClass.getDeclaredMethod("getIdentifiesUnivocally");
    getNameMethod.setAccessible(true);
    getIdentifiesUnivocallyMethod.setAccessible(true);

    assertEquals("Advanced Test", getNameMethod.invoke(result));
    assertEquals(true, getIdentifiesUnivocallyMethod.invoke(result));
  }

  /**
   * Tests the {@code readCriteria} method with a module filter.
   * <p>
   * Ensures that the method correctly parses criteria related to modules.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testReadCriteriaWithModuleFilter() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TestUtils.CRITERIA, createModuleFilterCriteriaJson());

    Object result = readCriteriaMethod.invoke(
        manageEntityFieldsDS,
        parameters
    );

    assertNotNull(TestUtils.RESULT_SHOULD_NOT_BE_NULL, result);

    Method getModuleIdsMethod = entityFieldSelectedFiltersClass.getDeclaredMethod("getModuleIds");
    getModuleIdsMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<String> moduleIds = (List<String>) getModuleIdsMethod.invoke(result);
    assertEquals(1, moduleIds.size());
    assertTrue(moduleIds.contains("testModuleId"));
  }

  /**
   * Creates JSON for simple criteria used in the {@code testReadCriteriaSimpleCriteria} test.
   *
   * @return a JSON string representing simple criteria
   * @throws Exception if any JSON creation error occurs
   */
  private String createSimpleCriteriaJson() throws Exception {
    JSONArray criteriaArray = new JSONArray();
    JSONObject criteria = new JSONObject();
    criteria.put(TestUtils.FIELD_NAME, TestUtils.NAME);
    criteria.put(TestUtils.VALUE, "Test Name");
    criteria.put(TestUtils.OPERATOR, TestUtils.IS_EQUAL_TO);
    criteriaArray.put(criteria);

    JSONObject ismandatoryCriteria = new JSONObject();
    ismandatoryCriteria.put(TestUtils.FIELD_NAME, "ismandatory");
    ismandatoryCriteria.put(TestUtils.VALUE, true);
    ismandatoryCriteria.put(TestUtils.OPERATOR, TestUtils.IS_EQUAL_TO);
    criteriaArray.put(ismandatoryCriteria);

    return criteriaArray.toString();
  }

  /**
   * Creates JSON for advanced criteria used in the {@code testReadCriteriaAdvancedCriteria} test.
   *
   * @return a JSON string representing advanced criteria
   * @throws Exception if any JSON creation error occurs
   */
  private String createAdvancedCriteriaJson() throws Exception {
    JSONObject advancedCriteria = new JSONObject();
    advancedCriteria.put("_constructor", "AdvancedCriteria");

    JSONArray innerCriteria = new JSONArray();
    JSONObject criteria = new JSONObject();
    criteria.put(TestUtils.FIELD_NAME, TestUtils.NAME);
    criteria.put(TestUtils.VALUE, "Advanced Test");
    criteria.put(TestUtils.OPERATOR, TestUtils.IS_EQUAL_TO);
    innerCriteria.put(criteria);

    JSONObject identifiesCriteria = new JSONObject();
    identifiesCriteria.put(TestUtils.FIELD_NAME, "identifiesUnivocally");
    identifiesCriteria.put(TestUtils.VALUE, true);
    identifiesCriteria.put(TestUtils.OPERATOR, TestUtils.IS_EQUAL_TO);
    innerCriteria.put(identifiesCriteria);

    advancedCriteria.put(TestUtils.CRITERIA, innerCriteria.toString());

    JSONArray outerArray = new JSONArray();
    outerArray.put(advancedCriteria);
    return outerArray.toString();
  }

  /**
   * Creates JSON for module filter criteria used in the {@code testReadCriteriaWithModuleFilter} test.
   *
   * @return a JSON string representing module filter criteria
   * @throws Exception if any JSON creation error occurs
   */
  private String createModuleFilterCriteriaJson() throws Exception {
    JSONArray criteriaArray = new JSONArray();
    JSONObject criteria = new JSONObject();
    criteria.put(TestUtils.FIELD_NAME, "module");
    criteria.put(TestUtils.VALUE, "testModuleId");
    criteria.put(TestUtils.OPERATOR, TestUtils.IS_EQUAL_TO);
    criteriaArray.put(criteria);
    return criteriaArray.toString();
  }
}