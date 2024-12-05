package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.etendoerp.etendorx.data.ETRXEntityField;

/**
 * Test class for {@code EntityFieldSelectedFilters} in {@link ManageEntityFieldsDS}.
 * <p>
 * This class ensures the proper functionality of the filter-related features,
 * including collection initialization, additions, modifications, and validation of properties.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityFieldSelectedFiltersTest {

  private Object filters;
  private Class<?> filtersClass;
  private static final String TEST_ID = "test-id";
  private static final String SECOND_TEST_ID = "test-id-2";

  /**
   * Sets up the test environment by initializing the {@code EntityFieldSelectedFilters} class
   * using reflection before each test method.
   *
   * @throws Exception if any setup error occurs
   */
  @Before
  public void setUp() throws Exception {
    filtersClass = Class.forName("com.etendoerp.etendorx.datasource.ManageEntityFieldsDS$EntityFieldSelectedFilters");
    Constructor<?> constructor = filtersClass.getDeclaredConstructor(ManageEntityFieldsDS.class);
    constructor.setAccessible(true);
    filters = constructor.newInstance(new ManageEntityFieldsDS());
  }

  /**
   * Tests that all collections within {@code EntityFieldSelectedFilters} are properly initialized.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testCollectionInitialization() throws Exception {
    assertNotNull("SelectedMappingValues should be initialized",
        filtersClass.getMethod("getSelectedMappingValues").invoke(filters));
    assertNotNull("SelectedIds should be initialized",
        filtersClass.getMethod("getSelectedIds").invoke(filters));
    assertNotNull("ModuleIds should be initialized",
        filtersClass.getMethod("getModuleIds").invoke(filters));
    assertNotNull("FieldMappingIds should be initialized",
        filtersClass.getMethod("getFieldMappingIds").invoke(filters));
    assertNotNull("JavaMappingIds should be initialized",
        filtersClass.getMethod("getJavaMappingIds").invoke(filters));
  }

  /**
   * Tests the addition of multiple elements to various collections in {@code EntityFieldSelectedFilters}.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testMultipleAdditionsToCollections() throws Exception {
    List<String> testIds = Arrays.asList("id1", "id2", "id3", "id4", "id5");

    for (String id : testIds) {
      filtersClass.getMethod("addSelectedID", String.class).invoke(filters, id);
      filtersClass.getMethod("addModuleIds", String.class).invoke(filters, id);
      filtersClass.getMethod("addJavaMappingIds", String.class).invoke(filters, id);
      filtersClass.getMethod("addFieldMappingValues", String.class).invoke(filters, id);
      filtersClass.getMethod("addEtrxProjectionEntityRelatedIds", String.class).invoke(filters, id);
      filtersClass.getMethod("addEtrxConstantValueIds", String.class).invoke(filters, id);
    }

    for (String methodName : Arrays.asList(
        "getSelectedIds", "getModuleIds", "getJavaMappingIds",
        "getFieldMappingIds", "getEtrxProjectionEntityRelatedIds", "getEtrxConstantValueIds")) {
      @SuppressWarnings("unchecked")
      List<String> collection = (List<String>) filtersClass.getMethod(methodName).invoke(filters);
      assertEquals("Collection should contain all items", testIds.size(), collection.size());
      assertTrue("Collection should contain all test IDs", collection.containsAll(testIds));
    }
  }

  /**
   * Tests handling of complex {@code SelectedMappingValues} with multiple entries and nested lists.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testComplexSelectedMappingValues() throws Exception {
    Map<String, List<ETRXEntityField>> complexMap = new HashMap<>();
    List<ETRXEntityField> entityList1 = Arrays.asList(mock(ETRXEntityField.class), mock(ETRXEntityField.class));
    List<ETRXEntityField> entityList2 = Collections.singletonList(mock(ETRXEntityField.class));

    complexMap.put("key1", entityList1);
    complexMap.put("key2", entityList2);

    filtersClass.getMethod("setSelectedMappingValues", Map.class).invoke(filters, complexMap);

    @SuppressWarnings("unchecked")
    Map<String, List<ETRXEntityField>> result = (Map<String, List<ETRXEntityField>>)
        filtersClass.getMethod("getSelectedMappingValues").invoke(filters);

    assertEquals("Map should contain correct number of entries", 2, result.size());
    assertEquals("First list should have correct size", 2, result.get("key1").size());
    assertEquals("Second list should have correct size", 1, result.get("key2").size());
  }

  /**
   * Tests null handling for various properties in {@code EntityFieldSelectedFilters}.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testNullHandling() throws Exception {
    for (String property : Arrays.asList("Name", "Jsonpath", "Property", "Line")) {
      Method setter = filtersClass.getDeclaredMethod("set" + property, String.class);
      Method getter = filtersClass.getDeclaredMethod("get" + property);

      setter.invoke(filters, new Object[]{null});
      Object result = getter.invoke(filters);

      assertNull(property + " should be null", result);
    }
  }

  /**
   * Tests boolean property toggling for {@code EntityFieldSelectedFilters}.
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testBooleanToggling() throws Exception {
    String[] booleanProperties = {"Ismandatory", "IdentifiesUnivocally", "EntityFieldCreated"};

    for (String property : booleanProperties) {
      Method setter = filtersClass.getDeclaredMethod("set" + property, Boolean.class);
      Method getter = filtersClass.getDeclaredMethod("get" + property);

      setter.invoke(filters, Boolean.TRUE);
      assertTrue(property + " should be true",
          (Boolean) getter.invoke(filters));

      setter.invoke(filters, Boolean.FALSE);
      assertFalse(property + " should be false",
          (Boolean) getter.invoke(filters));

      setter.invoke(filters, new Object[]{null});
      assertNull(property + " should be null",
          getter.invoke(filters));
    }
  }

  /**
   * Tests replacing lists in various properties of {@code EntityFieldSelectedFilters}.
   * <p>
   * Verifies that empty, singleton, and multi-element lists are handled correctly by the setter and getter methods.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testListReplacements() throws Exception {
    List<String> emptyList = Collections.emptyList();
    List<String> singletonList = Collections.singletonList("single");
    List<String> multipleList = Arrays.asList("multiple1", "multiple2");

    for (String setterMethod : Arrays.asList(
        "setSelectedIds", "setModuleIds", "setFieldMappingIds",
        "setJavaMappingIds", "setEtrxProjectionEntityRelatedIds", "setEtrxConstantValueIds")) {

      filtersClass.getMethod(setterMethod, List.class).invoke(filters, emptyList);
      @SuppressWarnings("unchecked")
      List<String> result1 = (List<String>) filtersClass.getMethod(
          setterMethod.replace("set", "get")).invoke(filters);
      assertTrue(setterMethod + " should be empty", result1.isEmpty());

      filtersClass.getMethod(setterMethod, List.class).invoke(filters, singletonList);
      @SuppressWarnings("unchecked")
      List<String> result2 = (List<String>) filtersClass.getMethod(
          setterMethod.replace("set", "get")).invoke(filters);
      assertEquals(setterMethod + " should have one item", 1, result2.size());

      filtersClass.getMethod(setterMethod, List.class).invoke(filters, multipleList);
      @SuppressWarnings("unchecked")
      List<String> result3 = (List<String>) filtersClass.getMethod(
          setterMethod.replace("set", "get")).invoke(filters);
      assertEquals(setterMethod + " should have multiple items", 2, result3.size());
    }
  }

  /**
   * Tests setting and retrieving string properties with long strings and special characters.
   * <p>
   * Ensures that string values are stored and retrieved correctly, even with unusual content.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testStringProperties() throws Exception {
    String longString = "This is a very long string that we will use to test the property values " +
        "to ensure they can handle longer content correctly";
    String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    for (String setter : Arrays.asList("setName", "setJsonpath", "setProperty", "setLine")) {
      filtersClass.getMethod(setter, String.class).invoke(filters, longString);
      assertEquals("Long string should be stored correctly",
          longString, filtersClass.getMethod(setter.replace("set", "get")).invoke(filters));

      filtersClass.getMethod(setter, String.class).invoke(filters, specialChars);
      assertEquals("Special characters should be stored correctly",
          specialChars, filtersClass.getMethod(setter.replace("set", "get")).invoke(filters));
    }
  }

  /**
   * Tests handling of {@code SelectedMappingValues} with empty and null lists.
   * <p>
   * Verifies that empty lists are properly stored and null values are handled without errors.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testSelectedMappingValuesWithEmptyLists() throws Exception {
    Map<String, List<ETRXEntityField>> testMap = new HashMap<>();
    testMap.put("emptyKey", Collections.emptyList());
    testMap.put("nullKey", null);

    filtersClass.getMethod("setSelectedMappingValues", Map.class).invoke(filters, testMap);

    @SuppressWarnings("unchecked")
    Map<String, List<ETRXEntityField>> result = (Map<String, List<ETRXEntityField>>)
        filtersClass.getMethod("getSelectedMappingValues").invoke(filters);

    assertTrue("Should contain empty list", result.get("emptyKey").isEmpty());
    assertNull("Should contain null value", result.get("nullKey"));
  }
}