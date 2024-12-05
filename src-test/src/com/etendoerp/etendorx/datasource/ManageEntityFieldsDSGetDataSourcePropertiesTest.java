package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.datasource.DataSourceProperty;

/**
 * Test class for verifying the behavior of the Entity Fields Management DataSource.
 * Performs unit tests for methods related to retrieving data source properties
 * for entity fields.
 * Extends WeldBaseTest to provide test environment configuration.
 */
public class ManageEntityFieldsDSGetDataSourcePropertiesTest extends WeldBaseTest {

  private ManageEntityFieldsDS dataSource;
  private Method getStringPropertyMethod;
  private Method getListPropertyMethod;
  private Method getIdPropertyMethod;

  /**
   * Sets up the test environment before running tests.
   * Initializes:
   * - System administrator context
   * - DataSource instance
   * - Private methods via reflection for testing
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    setSystemAdministratorContext();

    dataSource = new ManageEntityFieldsDS();

    getStringPropertyMethod = ManageEntityFieldsDS.class.getDeclaredMethod("getStringProperty", String.class);
    getStringPropertyMethod.setAccessible(true);

    getListPropertyMethod = ManageEntityFieldsDS.class.getDeclaredMethod("getListProperty", String.class, String.class);
    getListPropertyMethod.setAccessible(true);

    getIdPropertyMethod = ManageEntityFieldsDS.class.getDeclaredMethod("getIdProperty", String.class);
    getIdPropertyMethod.setAccessible(true);
  }

  /**
   * Tests retrieving data source properties.
   * Verifies that:
   * - Result is not null
   * - Exactly 4 properties are returned
   * - Properties have expected names in correct order
   */
  @Test
  public void testGetDataSourceProperties() {
    try {
      OBContext.setAdminMode(true);

      Map<String, Object> parameters = new HashMap<>();
      List<DataSourceProperty> result = dataSource.getDataSourceProperties(parameters);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return 4 properties", 4, result.size());

      assertEquals("First property should be ID", ManageEntityFieldConstants.ID,
          result.get(0).getName());
      assertEquals("Second property should be NAME", ManageEntityFieldConstants.NAME,
          result.get(1).getName());
      assertEquals("Third property should be JSONPATH", ManageEntityFieldConstants.JSONPATH,
          result.get(2).getName());
      assertEquals("Fourth property should be FIELDMAPPING", ManageEntityFieldConstants.FIELDMAPPING,
          result.get(3).getName());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests retrieving a string property.
   * Checks that:
   * - Method returns a non-null property
   * - Property name matches the provided name
   *
   * @throws Exception if an error occurs during method invocation
   */
  @Test
  public void testGetStringProperty() throws Exception {
    try {
      OBContext.setAdminMode(true);

      DataSourceProperty result = (DataSourceProperty) getStringPropertyMethod.invoke(dataSource, "testName");

      assertNotNull("Result should not be null", result);
      assertEquals("Property name should match", "testName", result.getName());

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests retrieving an ID property.
   *
   * Verifies that:
   * - Method returns a non-null property
   * - Property name matches the provided name
   * - Property is marked as an ID property
   *
   * @throws Exception if an error occurs during method invocation
   */
  @Test
  public void testGetIdProperty() throws Exception {
    try {
      OBContext.setAdminMode(true);

      String testName = "testId";

      DataSourceProperty result = (DataSourceProperty) getIdPropertyMethod.invoke(dataSource, testName);

      assertNotNull("Result should not be null", result);
      assertEquals("Property name should match", testName, result.getName());
      assertEquals("Should be marked as ID property", true, result.isId());

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}