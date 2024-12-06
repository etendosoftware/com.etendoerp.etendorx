package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.ad.module.Module;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.ConstantValue;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Unit test class for {@link ManageEntityFieldsDS}.
 * Focuses on verifying the custom comparator logic for sorting field results.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSResultComparatorTest extends OBBaseTest {
  private Constructor<?> comparatorConstructor;
  private Comparator<Map<String, Object>> comparator;


  /**
   * Sets up the test environment.
   * Initializes the ResultComparator constructor using reflection before each test.
   *
   * @throws Exception if the ResultComparator class or constructor cannot be accessed
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    Class<?> comparatorClass = Class.forName("com.etendoerp.etendorx.datasource.ManageEntityFieldsDS$ResultComparator");
    comparatorConstructor = comparatorClass.getDeclaredConstructor(String.class);
    comparatorConstructor.setAccessible(true);
  }

  /**
   * Creates a new instance of  ResultComparator with the given sort field.
   *
   * @param sortField the field by which the comparison should be performed
   * @return an instance of {@link Comparator}
   * @throws Exception if the comparator cannot be instantiated
   */
  private Comparator<Map<String, Object>> createComparator(String sortField) throws Exception {
    return (Comparator<Map<String, Object>>) comparatorConstructor.newInstance(sortField);
  }

  /**
   * Tests the comparison logic for boolean fields.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testBooleanFieldCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    map1.put(ManageEntityFieldConstants.ISMANDATORY, true);
    map1.put(ManageEntityFieldConstants.LINE, "1");
    map2.put(ManageEntityFieldConstants.ISMANDATORY, false);
    map2.put(ManageEntityFieldConstants.LINE, "2");

    comparator = createComparator(ManageEntityFieldConstants.ISMANDATORY);
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for numeric fields.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testNumericCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    map1.put(ManageEntityFieldConstants.LINE, "100");
    map2.put(ManageEntityFieldConstants.LINE, "200");

    comparator = createComparator(ManageEntityFieldConstants.LINE);
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for string fields.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testStringCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    map1.put(ManageEntityFieldConstants.NAME, TestUtils.ALPHA);
    map2.put(ManageEntityFieldConstants.NAME, TestUtils.BETA);

    comparator = createComparator(ManageEntityFieldConstants.NAME);
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for {@link Module} objects based on their identifiers.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testModuleCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    Module module1 = mock(Module.class);
    Module module2 = mock(Module.class);
    when(module1.getIdentifier()).thenReturn(TestUtils.ALPHA);
    when(module2.getIdentifier()).thenReturn(TestUtils.BETA);

    map1.put(ManageEntityFieldConstants.MODULE, module1);
    map2.put(ManageEntityFieldConstants.MODULE, module2);

    comparator = createComparator("module$_identifier");
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for {@link ETRXJavaMapping} objects based on their identifiers.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testJavaMappingCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    ETRXJavaMapping mapping1 = mock(ETRXJavaMapping.class);
    ETRXJavaMapping mapping2 = mock(ETRXJavaMapping.class);
    when(mapping1.getIdentifier()).thenReturn(TestUtils.ALPHA);
    when(mapping2.getIdentifier()).thenReturn(TestUtils.BETA);

    map1.put(ManageEntityFieldConstants.JAVAMAPPING, mapping1);
    map2.put(ManageEntityFieldConstants.JAVAMAPPING, mapping2);

    comparator = createComparator("javaMapping$_identifier");
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for {@link ETRXProjectionEntity} objects based on their identifiers.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testProjectionEntityCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    ETRXProjectionEntity entity1 = mock(ETRXProjectionEntity.class);
    ETRXProjectionEntity entity2 = mock(ETRXProjectionEntity.class);
    when(entity1.getIdentifier()).thenReturn(TestUtils.ALPHA);
    when(entity2.getIdentifier()).thenReturn(TestUtils.BETA);

    map1.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, entity1);
    map2.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, entity2);

    comparator = createComparator("etrxProjectionEntityRelated$_identifier");
    assertEquals(-1, comparator.compare(map1, map2));
  }

  /**
   * Tests the comparison logic for {@link ConstantValue} objects based on their identifiers.
   *
   * @throws Exception if the test setup or comparison fails
   */
  @Test
  public void testConstantValueCompare() throws Exception {
    Map<String, Object> map1 = new HashMap<>();
    Map<String, Object> map2 = new HashMap<>();

    ConstantValue value1 = mock(ConstantValue.class);
    ConstantValue value2 = mock(ConstantValue.class);
    when(value1.getIdentifier()).thenReturn(TestUtils.ALPHA);
    when(value2.getIdentifier()).thenReturn(TestUtils.BETA);

    map1.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, value1);
    map2.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, value2);

    comparator = createComparator("etrxConstantValue$_identifier");
    assertEquals(-1, comparator.compare(map1, map2));
  }

}