package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit test class for the addCriteria method in ManageEntityFieldsDS.
 * These tests validate the behavior of the addCriteria method with various
 * entity field selection criteria.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSAddCriteriaTest {

  private ManageEntityFieldsDS dataSource;
  private Method addCriteriaMethod;
  private Class<?> filtersClass;
  private Object filters;

  @Mock
  private JSONObject mockCriteria;

  /**
   * Sets up the test environment before each test method.
   * Initializes:
   * - ManageEntityFieldsDS instance
   * - addCriteria method via reflection
   * - Internal filters class
   * - Filters instance
   *
   * @throws Exception if setup encounters configuration errors
   */
  @Before
  public void setUp() throws Exception {
    dataSource = new ManageEntityFieldsDS();

    addCriteriaMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "addCriteria",
        Class.forName("com.etendoerp.etendorx.datasource.ManageEntityFieldsDS$EntityFieldSelectedFilters"),
        JSONObject.class
    );
    addCriteriaMethod.setAccessible(true);

    filtersClass = Class.forName("com.etendoerp.etendorx.datasource.ManageEntityFieldsDS$EntityFieldSelectedFilters");
    filters = filtersClass.getDeclaredConstructor(ManageEntityFieldsDS.class).newInstance(dataSource);
  }

  /**
   * Tests the "not null" operator for ID field.
   * Verifies that when the ID field has a "not null" operator,
   * no value is requested from the criteria.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testIdNotNullOperator() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.ID);
    when(mockCriteria.getString("operator")).thenReturn("notNull");

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    verify(mockCriteria, never()).getString("value");
  }

  /**
   * Tests adding a selected ID to the filters.
   * Ensures that when an ID is added with the "equals" operator,
   * it is correctly added to the selected IDs list.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testAddSelectedId() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.ID);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("testId");

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    @SuppressWarnings("unchecked")
    List<String> selectedIds = (List<String>) filtersClass.getMethod("getSelectedIds").invoke(filters);
    assertTrue(selectedIds.contains("testId"));
  }

  /**
   * Tests setting module IDs in the filters.
   * Verifies that when a module criterion is added with the "equals" operator,
   * the module ID is correctly added to the module IDs list.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetModuleIds() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.MODULE);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("moduleId");

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    @SuppressWarnings("unchecked")
    List<String> moduleIds = (List<String>) filtersClass.getMethod("getModuleIds").invoke(filters);
    assertTrue(moduleIds.contains("moduleId"));
  }


  /**
   * Tests setting field mapping criteria.
   * Ensures that when a field mapping is added, the method correctly
   * parses and adds multiple values from a JSON array.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetFieldMapping() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.FIELDMAPPING);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("[\"value1\",\"value2\"]");

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    @SuppressWarnings("unchecked")
    List<String> fieldMappingIds = (List<String>) filtersClass.getMethod("getFieldMappingIds").invoke(filters);
    assertTrue(fieldMappingIds.contains("value1"));
    assertTrue(fieldMappingIds.contains("value2"));
  }

  /**
   * Tests setting the isMandatory flag.
   * Validates that the isMandatory flag is correctly set when
   * a valid boolean value is provided.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetIsMandatory() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.ISMANDATORY);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("true");
    when(mockCriteria.getBoolean("value")).thenReturn(true);

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    Boolean result = (Boolean) filtersClass.getMethod("getIsmandatory").invoke(filters);
    assertTrue(result);
  }

  /**
   * Tests handling of invalid isMandatory value.
   * Verifies that an invalid isMandatory value results in a null flag.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetIsMandatoryInvalidValue() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.ISMANDATORY);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("invalid");

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    Boolean result = (Boolean) filtersClass.getMethod("getIsmandatory").invoke(filters);
    assertNull(result);
  }

  /**
   * Tests setting the identifiesUnivocally flag.
   * Ensures that the identifiesUnivocally flag is correctly set
   * when a valid boolean value is provided.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetIdentifiesUnivocally() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("true");
    when(mockCriteria.getBoolean("value")).thenReturn(true);

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    Boolean result = (Boolean) filtersClass.getMethod("getIdentifiesUnivocally").invoke(filters);
    assertTrue(result);
  }

  /**
   * Tests setting various string properties.
   * Validates that string properties like name, property, line,
   * and jsonPath are correctly set.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testSetStringProperties() throws Exception {
    String[][] testCases = {
        {ManageEntityFieldConstants.NAME, "testName"},
        {ManageEntityFieldConstants.PROPERTY, "testProperty"},
        {ManageEntityFieldConstants.LINE, "testLine"},
        {ManageEntityFieldConstants.JSONPATH, "testJsonPath"}
    };

    for (String[] testCase : testCases) {
      when(mockCriteria.getString("fieldName")).thenReturn(testCase[0]);
      when(mockCriteria.getString("operator")).thenReturn("equals");
      when(mockCriteria.has("value")).thenReturn(true);
      when(mockCriteria.getString("value")).thenReturn(testCase[1]);

      addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

      String getterName = "get" + testCase[0].substring(0, 1).toUpperCase() + testCase[0].substring(1);
      String result = (String) filtersClass.getMethod(getterName).invoke(filters);
      assertEquals(testCase[1], result);
    }
  }

  /**
   * Tests setting the entityFieldCreated flag.
   * Verifies that the entityFieldCreated flag is correctly set
   * when a valid boolean value is provided.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testEntityFieldCreated() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.ENTITYFIELDCREATED);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(true);
    when(mockCriteria.getString("value")).thenReturn("true");
    when(mockCriteria.getBoolean("value")).thenReturn(true);

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    Boolean result = (Boolean) filtersClass.getMethod("getEntityFieldCreated").invoke(filters);
    assertTrue(result);
  }

  /**
   * Tests adding related IDs for various fields.
   * Ensures that related IDs (javaMappingIds, etrxProjectionEntityRelatedIds,
   * etrxConstantValueIds) are correctly added to their respective lists.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testAddRelatedIds() throws Exception {
    String[][] testCases = {
        {ManageEntityFieldConstants.JAVAMAPPING, "getJavaMappingIds"},
        {ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, "getEtrxProjectionEntityRelatedIds"},
        {ManageEntityFieldConstants.ETRXCONSTANTVALUE, "getEtrxConstantValueIds"}
    };

    for (String[] testCase : testCases) {
      when(mockCriteria.getString("fieldName")).thenReturn(testCase[0]);
      when(mockCriteria.getString("operator")).thenReturn("equals");
      when(mockCriteria.has("value")).thenReturn(true);
      when(mockCriteria.getString("value")).thenReturn("testId");

      addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

      @SuppressWarnings("unchecked")
      List<String> ids = (List<String>) filtersClass.getMethod(testCase[1]).invoke(filters);
      assertTrue(ids.contains("testId"));
    }
  }

  /**
   * Tests handling of criteria without a value.
   * Verifies that when no value is present in the criteria,
   * an empty string is set for string properties.
   *
   * @throws Exception if reflection or method invocation fails
   */
  @Test
  public void testNoValuePresent() throws Exception {
    when(mockCriteria.getString("fieldName")).thenReturn(ManageEntityFieldConstants.NAME);
    when(mockCriteria.getString("operator")).thenReturn("equals");
    when(mockCriteria.has("value")).thenReturn(false);

    addCriteriaMethod.invoke(dataSource, filters, mockCriteria);

    String result = (String) filtersClass.getMethod("getName").invoke(filters);
    assertEquals("", result);
  }
}
