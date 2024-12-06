package com.etendoerp.etendorx.actionhandler;
import com.etendoerp.etendorx.TestUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;
import com.etendoerp.etendorx.datasource.ManageEntityFieldConstants;

/**
 * Unit tests for the EntityFieldManagement class.
 * This test suite ensures correct behavior for managing entity fields in the context of the
 * Etendo ERP application.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityFieldManagementTest {


  /**
   * Rule to expect exceptions in test cases.
   */
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private OBDal obDal;

  @Mock
  private ETRXProjectionEntity projectionEntity;

  @Mock
  private ETRXEntityField entityField;

  @Mock
  private Client client;

  @Mock
  private Module module;

  @Mock
  private Organization organization;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;
  private ManageEntityMappings manageEntityMappings;
  private Method createEntityFieldMethod;
  private Method updateEntityFieldMethod;


  /**
   * Utility method to create a base JSON object for entity field properties.
   *
   * @param id the unique identifier of the entity field
   * @return a JSONObject containing base properties
   * @throws JSONException if JSON properties cannot be created
   */
  private JSONObject createBaseJsonObject(String id) throws JSONException {
    JSONObject properties = new JSONObject();
    properties.put(ManageEntityFieldConstants.ID, id);
    properties.put(ManageEntityFieldConstants.FIELDMAPPING, "");
    properties.put(ManageEntityFieldConstants.NAME, "");
    properties.put(ManageEntityFieldConstants.PROPERTY, "");
    properties.put(ManageEntityFieldConstants.LINE, "");
    properties.put(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY, false);
    properties.put(ManageEntityFieldConstants.ISMANDATORY, false);
    properties.put(ManageEntityFieldConstants.JSONPATH, "");
    properties.put(ManageEntityFieldConstants.MODULE, "");
    properties.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, "");
    properties.put(ManageEntityFieldConstants.JAVAMAPPING, "");
    properties.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, "");
    return properties;
  }

  /**
   * Sets up mock behavior and initializes test environment.
   *
   * @throws NoSuchMethodException if methods cannot be accessed via reflection
   */
  @Before
  public void setUp() throws NoSuchMethodException {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.get(ETRXEntityField.class, TestUtils.TEST_ID)).thenReturn(entityField);
    when(module.isInDevelopment()).thenReturn(true);
    when(entityField.getModule()).thenReturn(module);

    createEntityFieldMethod = ManageEntityMappings.class.getDeclaredMethod(
        "createEntityField",
        JSONObject.class,
        ETRXProjectionEntity.class
    );
    createEntityFieldMethod.setAccessible(true);

    updateEntityFieldMethod = ManageEntityMappings.class.getDeclaredMethod(
        "updateEntityField",
        JSONObject.class
    );
    updateEntityFieldMethod.setAccessible(true);


    manageEntityMappings = new ManageEntityMappings();

    when(projectionEntity.getClient()).thenReturn(client);
    when(projectionEntity.getOrganization()).thenReturn(organization);

  }

  /**
   * Cleans up static mocks after tests.
   */
  @After
  public void tearDown() {
    mockedOBDal.close();
    mockedOBContext.close();
    mockedOBProvider.close();
    mockedMessageUtils.close();
  }

  /**
   * Tests the updateEntityField method when no changes are detected.
   *
   * @throws Exception if reflection calls fail
   */
  @Test
  public void testUpdateEntityFieldNoChanges() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.FIELDMAPPING, TestUtils.DM);
    properties.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_NAME);
    properties.put(ManageEntityFieldConstants.PROPERTY, "testProperty");
    properties.put(ManageEntityFieldConstants.LINE, "10");

    when(entityField.getFieldMapping()).thenReturn(TestUtils.DM);
    when(entityField.getName()).thenReturn(TestUtils.TEST_NAME);
    when(entityField.getProperty()).thenReturn("testProperty");
    when(entityField.getLine()).thenReturn(10L);
    when(entityField.isIdentifiesUnivocally()).thenReturn(false);
    when(entityField.isMandatory()).thenReturn(false);

    updateEntityFieldMethod.invoke(manageEntityMappings, properties);

    verify(obDal, never()).save(any(ETRXEntityField.class));
  }

  /**
   * Tests the updateEntityField method with changes in field properties.
   *
   * @throws Exception if reflection calls fail
   */
  @Test
  public void testUpdateEntityFieldWithChanges() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.FIELDMAPPING, TestUtils.JM);
    properties.put(ManageEntityFieldConstants.NAME, "newName");
    properties.put(ManageEntityFieldConstants.PROPERTY, "newProperty");
    properties.put(ManageEntityFieldConstants.LINE, "20");
    properties.put(ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY, true);
    properties.put(ManageEntityFieldConstants.ISMANDATORY, true);

    when(entityField.getFieldMapping()).thenReturn(TestUtils.DM);
    when(entityField.getName()).thenReturn("oldName");
    when(entityField.getProperty()).thenReturn("oldProperty");
    when(entityField.getLine()).thenReturn(10L);
    when(entityField.isIdentifiesUnivocally()).thenReturn(false);
    when(entityField.isMandatory()).thenReturn(false);

    try {
      updateEntityFieldMethod.invoke(manageEntityMappings, properties);
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof Exception) {
        throw (Exception) e.getTargetException();
      }
      throw e;
    }

    verify(entityField).setFieldMapping(TestUtils.JM);
    verify(entityField).setProperty(null);
    verify(entityField).setName("newName");
    verify(entityField).setLine(20L);
    verify(entityField).setIdentifiesUnivocally(true);
    verify(entityField).setMandatory(true);
    verify(obDal).save(entityField);
  }

  /**
   * Tests the updateEntityField method with invalid JSON input.
   *
   * @throws Exception if reflection calls fail or an invalid JSON input is provided
   */
  @Test
  public void testUpdateEntityFieldInvalidJson() throws Exception {
    thrown.expect(InvocationTargetException.class);

    JSONObject properties = new JSONObject();
    properties.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_NAME);

    updateEntityFieldMethod.invoke(manageEntityMappings, properties);
  }

  /**
   * Tests the updateEntityField method when the module is not in development.
   * Verifies that an OBException is thrown with the expected error message.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testUpdateEntityFieldModuleNotInDevelopment() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.MODULE, "new-module-id");

    String errorMessage = "Module not in development";
    mockedMessageUtils.when(() -> OBMessageUtils.messageBD("20533")).thenReturn(errorMessage);
    mockedMessageUtils.when(() -> OBMessageUtils.parseTranslation(errorMessage, new HashMap<>()))
        .thenReturn(errorMessage);

    when(module.isInDevelopment()).thenReturn(false);
    when(entityField.getModule()).thenReturn(module);

    try {
      updateEntityFieldMethod.invoke(manageEntityMappings, properties);
      fail("Expected OBException was not thrown");
    } catch (InvocationTargetException e) {
      Throwable actualException = e.getTargetException();
      assertTrue(actualException instanceof OBException);
      assertEquals(errorMessage, actualException.getMessage());
    }
  }

  /**
   * Tests the createEntityField method when the module is not in development.
   * Verifies that an OBException is thrown with the expected error message.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testCreateEntityFieldModuleNotInDevelopment() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.MODULE,TestUtils.MODULE_ID);

    when(module.isInDevelopment()).thenReturn(false);
    when(obDal.get(Module.class,TestUtils.MODULE_ID)).thenReturn(module);

    String errorMessage = "Module not in development";
    mockedMessageUtils.when(() -> OBMessageUtils.messageBD("20533")).thenReturn(errorMessage);
    mockedMessageUtils.when(() -> OBMessageUtils.parseTranslation(errorMessage, new HashMap<>()))
        .thenReturn(errorMessage);

    try {
      createEntityFieldMethod.invoke(manageEntityMappings, properties, projectionEntity);
      fail("Expected OBException was not thrown");
    } catch (InvocationTargetException e) {
      Throwable actualException = e.getTargetException();
      assertTrue(actualException instanceof OBException);
      assertEquals(errorMessage, actualException.getMessage());
    }
  }

  /**
   * Tests the successful creation of an entity field using the createEntityField method.
   * Verifies that all the expected properties are set and the entity field is saved.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testCreateEntityFieldSuccess() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.MODULE,TestUtils.MODULE_ID);
    properties.put(ManageEntityFieldConstants.FIELDMAPPING, TestUtils.DM);
    properties.put(ManageEntityFieldConstants.NAME, TestUtils.TEST_NAME);
    properties.put(ManageEntityFieldConstants.LINE, "10");

    when(module.isInDevelopment()).thenReturn(true);
    when(obDal.get(Module.class,TestUtils.MODULE_ID)).thenReturn(module);

    when(OBProvider.getInstance()).thenReturn(mock(OBProvider.class));
    when(OBProvider.getInstance().get(ETRXEntityField.class)).thenReturn(entityField);

    createEntityFieldMethod.invoke(manageEntityMappings, properties, projectionEntity);

    verify(entityField).setClient(client);
    verify(entityField).setOrganization(organization);
    verify(entityField).setEtrxProjectionEntity(projectionEntity);
    verify(entityField).setModule(module);
    verify(entityField).setFieldMapping(TestUtils.DM);
    verify(entityField).setName(TestUtils.TEST_NAME);
    verify(entityField).setLine(10L);
    verify(obDal).save(entityField);
  }

  /**
   * Tests the createEntityField method for a Java Mapping (JM) field mapping.
   * Verifies that the property is correctly set to null.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testCreateEntityFieldJavaMapping() throws Exception {
    JSONObject properties = createBaseJsonObject(TestUtils.TEST_ID);
    properties.put(ManageEntityFieldConstants.MODULE,TestUtils.MODULE_ID);
    properties.put(ManageEntityFieldConstants.FIELDMAPPING, TestUtils.JM);
    properties.put(ManageEntityFieldConstants.PROPERTY, "someProperty");

    when(module.isInDevelopment()).thenReturn(true);
    when(obDal.get(Module.class,TestUtils.MODULE_ID)).thenReturn(module);

    when(OBProvider.getInstance()).thenReturn(mock(OBProvider.class));
    when(OBProvider.getInstance().get(ETRXEntityField.class)).thenReturn(entityField);

    createEntityFieldMethod.invoke(manageEntityMappings, properties, projectionEntity);

    verify(entityField).setProperty(null);
  }

}