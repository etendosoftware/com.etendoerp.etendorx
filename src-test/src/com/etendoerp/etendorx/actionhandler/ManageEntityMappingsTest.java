package com.etendoerp.etendorx.actionhandler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXJavaMapping;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;
import com.etendoerp.etendorx.datasource.ManageEntityFieldConstants;

/**
 * Unit tests for the {@link ManageEntityMappings} class.
 * This class tests private methods using reflection and public methods of {@link ManageEntityMappings}.
 * It covers scenarios related to checking deleted records, module development status, and updates
 * to constant values, projection entities, and Java mappings.
 * Utiliza Mockito para crear mocks y simular comportamiento de dependencias.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityMappingsTest {

  private static final String NEW_MODULE_ID = "newModuleId";
  private static final String ERROR_MESSAGE_KEY = "error.message";
  private static final String PARSED_ERROR_MESSAGE = "Parsed error message";
  private static final String NEW_ID = "newId";

  @Mock
  private OBCriteria<ETRXEntityField> mockCriteria;

  @Mock
  private ETRXProjectionEntity mockProjectionEntity;

  @Mock
  private Module mockModule;

  @InjectMocks
  private ManageEntityMappings manageEntityMappings;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private Method checkDeletedRecordsMethod;

  /**
   * Sets up the mocks and prepares the environment for tests.
   * This method initializes Mockito annotations, creates static mocks for OBDal and OBMessageUtils,
   * and uses reflection to access private methods for testing.
   */
  @Before
  public void setUp() throws Exception {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    OBDal mockDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

    when(mockDal.createCriteria(ETRXEntityField.class)).thenReturn(mockCriteria);
    when(mockCriteria.setFilterOnActive(false)).thenReturn(mockCriteria);

    checkDeletedRecordsMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkDeletedRecords",
        JSONArray.class,
        ETRXProjectionEntity.class
    );
    checkDeletedRecordsMethod.setAccessible(true);
  }

  /**
   * Cleans up resources and closes static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Tests that a record is successfully deleted if the module is in development.
   *
   * @throws Exception if an error occurs while invoking the private method.
   */
  @Test
  public void testSuccessfulDeletion() throws Exception {
    ETRXEntityField entityToDelete = mock(ETRXEntityField.class);
    when(entityToDelete.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(true);

    List<ETRXEntityField> entityFields = Collections.singletonList(entityToDelete);
    when(mockCriteria.list()).thenReturn(entityFields);

    JSONArray selection = new JSONArray();

    checkDeletedRecordsMethod.invoke(manageEntityMappings, selection, mockProjectionEntity);

    verify(OBDal.getInstance()).remove(entityToDelete);
  }

  /**
   * Tests that an exception is thrown when attempting to delete a record in a module
   * not marked as in development.
   *
   * @throws Exception if an error occurs while invoking the private method.
   */
  @Test(expected = OBException.class)
  public void testDeleteNotInDevelopmentModule() throws Exception {
    ETRXEntityField entityToDelete = mock(ETRXEntityField.class);
    when(entityToDelete.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(false);

    List<ETRXEntityField> entityFields = Collections.singletonList(entityToDelete);
    when(mockCriteria.list()).thenReturn(entityFields);

    when(OBMessageUtils.messageBD(anyString())).thenReturn(ERROR_MESSAGE_KEY);
    when(OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(PARSED_ERROR_MESSAGE);

    JSONArray selection = new JSONArray();

    try {
      checkDeletedRecordsMethod.invoke(manageEntityMappings, selection, mockProjectionEntity);
    } catch (Exception e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw e;
    }
  }

  /**
   * Tests that no record is deleted if the selected record matches the entity's ID.
   *
   * @throws Exception if an error occurs while invoking the private method.
   */
  @Test
  public void testNoDeleteForSelectedRecord() throws Exception {
    ETRXEntityField entityField = mock(ETRXEntityField.class);
    String entityId = "selectedEntityId";
    when(entityField.getId()).thenReturn(entityId);

    List<ETRXEntityField> entityFields = Collections.singletonList(entityField);
    when(mockCriteria.list()).thenReturn(entityFields);

    JSONArray selection = new JSONArray();
    JSONObject selectedRow = new JSONObject();
    selectedRow.put(ManageEntityFieldConstants.ID, entityId);
    selection.put(selectedRow);

    checkDeletedRecordsMethod.invoke(manageEntityMappings, selection, mockProjectionEntity);

    verify(OBDal.getInstance(), never()).remove(any(ETRXEntityField.class));
  }

  /**
   * Tests mixed scenarios where some records are deleted and others are retained based on selection.
   *
   * @throws Exception if an error occurs while invoking the private method.
   */
  @Test
  public void testMultipleRecordsWithMixedDeletion() throws Exception {
    ETRXEntityField entityToKeep = mock(ETRXEntityField.class);
    ETRXEntityField entityToDelete = mock(ETRXEntityField.class);

    when(entityToKeep.getId()).thenReturn("keepId");
    when(entityToDelete.getId()).thenReturn("deleteId");
    when(entityToDelete.getModule()).thenReturn(mockModule);
    when(mockModule.isInDevelopment()).thenReturn(true);

    List<ETRXEntityField> entityFields = Arrays.asList(entityToKeep, entityToDelete);
    when(mockCriteria.list()).thenReturn(entityFields);

    JSONArray selection = new JSONArray();
    JSONObject selectedRow = new JSONObject();
    selectedRow.put(ManageEntityFieldConstants.ID, "keepId");
    selection.put(selectedRow);

    checkDeletedRecordsMethod.invoke(manageEntityMappings, selection, mockProjectionEntity);

    verify(OBDal.getInstance(), never()).remove(entityToKeep);
    verify(OBDal.getInstance()).remove(entityToDelete);
  }

  /**
   * Tests that the method returns true when the module is in development.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckModuleInDevelopment() throws Exception {
    when(mockModule.isInDevelopment()).thenReturn(true);

    boolean result = manageEntityMappings.checkModuleIsInDevelopment(mockModule, false);

    assertTrue("Method should return true when module is in development", result);
    verify(mockCriteria, never()).add(any());
  }

  /**
   * Tests that an OBException is thrown when the module is not in development
   * and no template module is found.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test(expected = OBException.class)
  public void testCheckModuleNotInDevelopmentNoTemplate() throws Exception {
    when(mockModule.isInDevelopment()).thenReturn(false);

    OBCriteria<Module> moduleOBCriteria = mock(OBCriteria.class);
    when(OBDal.getInstance().createCriteria(Module.class)).thenReturn(moduleOBCriteria);
    when(moduleOBCriteria.add(any())).thenReturn(moduleOBCriteria);
    when(moduleOBCriteria.setMaxResults(1)).thenReturn(moduleOBCriteria);
    when(moduleOBCriteria.uniqueResult()).thenReturn(null);

    when(OBMessageUtils.messageBD(anyString())).thenReturn(ERROR_MESSAGE_KEY);
    when(OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(PARSED_ERROR_MESSAGE);

    manageEntityMappings.checkModuleIsInDevelopment(mockModule, false);
  }

  /**
   * Tests that the method short-circuits and returns true when the moduleChanged
   * parameter is true, regardless of the module's development status.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckModuleWithModuleChanged() throws Exception {

    boolean result = manageEntityMappings.checkModuleIsInDevelopment(mockModule, true);

    assertTrue("Method should return true when moduleChanged is true", result);
    verify(mockModule, never()).isInDevelopment();
  }

  /**
   * Tests that no update is made if the module has not changed.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateModuleNoChanges() throws Exception {
    Method checkUpdateModuleMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateModule",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateModuleMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getModule()).thenReturn(mockModule);
    when(mockModule.getId()).thenReturn("moduleId");

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.MODULE, "moduleId");

    boolean result = (boolean) checkUpdateModuleMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertFalse("Should return false when module hasn't changed", result);
    verify(entityField, never()).setModule(any());
  }

  /**
   * Tests that an OBException is thrown when attempting to update a module
   * that is not in development.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test(expected = OBException.class)
  public void testCheckUpdateModuleCurrentModuleNotInDevelopment() throws Exception {
    Method checkUpdateModuleMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateModule",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateModuleMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getModule()).thenReturn(mockModule);
    when(mockModule.getId()).thenReturn("currentModuleId");
    when(mockModule.isInDevelopment()).thenReturn(false);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.MODULE, NEW_MODULE_ID);

    when(OBMessageUtils.messageBD(anyString())).thenReturn(ERROR_MESSAGE_KEY);
    when(OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(PARSED_ERROR_MESSAGE);

    try {
      checkUpdateModuleMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw e;
    }
  }

  /**
   * Tests that an OBException is thrown when the new module is not in development.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test(expected = OBException.class)
  public void testCheckUpdateModuleNewModuleNotInDevelopment() throws Exception {
    Method checkUpdateModuleMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateModule",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateModuleMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getModule()).thenReturn(mockModule);
    when(mockModule.getId()).thenReturn("currentModuleId");
    when(mockModule.isInDevelopment()).thenReturn(true);

    Module newModule = mock(Module.class);
    when(newModule.isInDevelopment()).thenReturn(false);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.MODULE, NEW_MODULE_ID);

    when(OBDal.getInstance().get(eq(Module.class), anyString())).thenReturn(newModule);

    when(OBMessageUtils.messageBD(anyString())).thenReturn(ERROR_MESSAGE_KEY);
    when(OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(PARSED_ERROR_MESSAGE);

    try {
      checkUpdateModuleMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof OBException) {
        throw (OBException) e.getCause();
      }
      throw e;
    }
  }

  /**
   * Tests that the module is successfully updated when the new module is valid
   * and in development.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateModuleSuccessful() throws Exception {
    Method checkUpdateModuleMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateModule",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateModuleMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getModule()).thenReturn(mockModule);
    when(mockModule.getId()).thenReturn("currentModuleId");
    when(mockModule.isInDevelopment()).thenReturn(true);

    Module newModule = mock(Module.class);
    when(newModule.isInDevelopment()).thenReturn(true);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.MODULE, NEW_MODULE_ID);

    when(OBDal.getInstance().get(eq(Module.class), anyString())).thenReturn(newModule);

    boolean result = (boolean) checkUpdateModuleMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when module was successfully updated", result);
    verify(entityField).setModule(newModule);
  }

  /**
   * Tests that no changes are made when the constant value has not changed.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateConstantValueNoChange() throws Exception {
    Method checkUpdateConstantValueMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxConstantValue",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateConstantValueMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ConstantValue existingConstantValue = mock(ConstantValue.class);
    when(existingConstantValue.getId()).thenReturn("constantValueId");
    when(entityField.getEtrxConstantValue()).thenReturn(existingConstantValue);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, "constantValueId");

    boolean result = (boolean) checkUpdateConstantValueMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertFalse("Should return false when constant value hasn't changed", result);
    verify(entityField, never()).setEtrxConstantValue(any());
  }

  /**
   * Tests that the constant value is updated successfully when it was previously null.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateConstantValueFromNull() throws Exception {
    Method checkUpdateConstantValueMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxConstantValue",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateConstantValueMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getEtrxConstantValue()).thenReturn(null);

    ConstantValue newConstantValue = mock(ConstantValue.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, "newConstantValueId");

    when(OBDal.getInstance().get(eq(ConstantValue.class), anyString())).thenReturn(newConstantValue);

    boolean result = (boolean) checkUpdateConstantValueMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when constant value was updated from null", result);
    verify(entityField).setEtrxConstantValue(newConstantValue);
  }

  /**
   * Tests that the constant value is updated successfully when it changes.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateConstantValueChanged() throws Exception {
    Method checkUpdateConstantValueMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxConstantValue",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateConstantValueMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ConstantValue existingConstantValue = mock(ConstantValue.class);
    when(existingConstantValue.getId()).thenReturn("oldConstantValueId");
    when(entityField.getEtrxConstantValue()).thenReturn(existingConstantValue);

    ConstantValue newConstantValue = mock(ConstantValue.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, "newConstantValueId");

    when(OBDal.getInstance().get(eq(ConstantValue.class), anyString())).thenReturn(newConstantValue);

    boolean result = (boolean) checkUpdateConstantValueMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when constant value was updated to a different value", result);
    verify(entityField).setEtrxConstantValue(newConstantValue);
  }

  /**
   * Tests that no changes are made when the constant value is empty.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateConstantValueEmpty() throws Exception {
    Method checkUpdateConstantValueMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxConstantValue",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateConstantValueMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXCONSTANTVALUE, "");

    boolean result = (boolean) checkUpdateConstantValueMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertFalse("Should return false when constant value is empty", result);
    verify(entityField, never()).setEtrxConstantValue(any());
  }

  /**
   * Tests that no changes are made when the projection entity has not changed.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateProjectionEntityRelatedNoChange() throws Exception {
    Method checkUpdateProjectionEntityMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxProjectionEntityRelated",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateProjectionEntityMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ETRXProjectionEntity existingProjectionEntity = mock(ETRXProjectionEntity.class);
    when(existingProjectionEntity.getId()).thenReturn("projectionEntityId");
    when(entityField.getEtrxProjectionEntityRelated()).thenReturn(existingProjectionEntity);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, "projectionEntityId");

    boolean result = (boolean) checkUpdateProjectionEntityMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertFalse("Should return false when projection entity hasn't changed", result);
    verify(entityField, never()).setEtrxProjectionEntityRelated(any());
  }

  /**
   * Tests that the projection entity is updated successfully when it was previously null.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateProjectionEntityFromNull() throws Exception {
    Method checkUpdateProjectionEntityMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxProjectionEntityRelated",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateProjectionEntityMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getEtrxProjectionEntityRelated()).thenReturn(null);

    ETRXProjectionEntity newProjectionEntity = mock(ETRXProjectionEntity.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, NEW_ID);

    when(OBDal.getInstance().get(eq(ETRXProjectionEntity.class), anyString())).thenReturn(newProjectionEntity);

    boolean result = (boolean) checkUpdateProjectionEntityMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when projection entity was updated from null", result);
    verify(entityField).setEtrxProjectionEntityRelated(newProjectionEntity);
  }

  /**
   * Tests that the projection entity is updated successfully when it changes.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateProjectionEntityChanged() throws Exception {
    Method checkUpdateProjectionEntityMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateEtrxProjectionEntityRelated",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateProjectionEntityMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ETRXProjectionEntity existingProjectionEntity = mock(ETRXProjectionEntity.class);
    when(existingProjectionEntity.getId()).thenReturn("oldId");
    when(entityField.getEtrxProjectionEntityRelated()).thenReturn(existingProjectionEntity);

    ETRXProjectionEntity newProjectionEntity = mock(ETRXProjectionEntity.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED, NEW_ID);

    when(OBDal.getInstance().get(eq(ETRXProjectionEntity.class), anyString())).thenReturn(newProjectionEntity);

    boolean result = (boolean) checkUpdateProjectionEntityMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when projection entity was changed", result);
    verify(entityField).setEtrxProjectionEntityRelated(newProjectionEntity);
  }

  /**
   * Tests that no changes are made when the Java mapping has not changed.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateJavaMappingNoChange() throws Exception {
    Method checkUpdateJavaMappingMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateJavaMapping",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateJavaMappingMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ETRXJavaMapping existingJavaMapping = mock(ETRXJavaMapping.class);
    when(existingJavaMapping.getId()).thenReturn("javaMappingId");
    when(entityField.getJavaMapping()).thenReturn(existingJavaMapping);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.JAVAMAPPING, "javaMappingId");

    boolean result = (boolean) checkUpdateJavaMappingMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertFalse("Should return false when Java mapping hasn't changed", result);
    verify(entityField, never()).setJavaMapping(any());
  }

  /**
   * Tests that the Java mapping is updated successfully when it was previously null.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateJavaMappingFromNull() throws Exception {
    Method checkUpdateJavaMappingMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateJavaMapping",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateJavaMappingMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    when(entityField.getJavaMapping()).thenReturn(null);

    ETRXJavaMapping newJavaMapping = mock(ETRXJavaMapping.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.JAVAMAPPING, NEW_ID);

    when(OBDal.getInstance().get(eq(ETRXJavaMapping.class), anyString())).thenReturn(newJavaMapping);

    boolean result = (boolean) checkUpdateJavaMappingMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when Java mapping was updated from null", result);
    verify(entityField).setJavaMapping(newJavaMapping);
  }

  /**
   * Tests that the Java mapping is updated successfully when it changes.
   *
   * @throws Exception if an error occurs during the test execution.
   */
  @Test
  public void testCheckUpdateJavaMappingChanged() throws Exception {
    Method checkUpdateJavaMappingMethod = ManageEntityMappings.class.getDeclaredMethod(
        "checkUpdateJavaMapping",
        JSONObject.class,
        ETRXEntityField.class
    );
    checkUpdateJavaMappingMethod.setAccessible(true);

    ETRXEntityField entityField = mock(ETRXEntityField.class);
    ETRXJavaMapping existingJavaMapping = mock(ETRXJavaMapping.class);
    when(existingJavaMapping.getId()).thenReturn("oldId");
    when(entityField.getJavaMapping()).thenReturn(existingJavaMapping);

    ETRXJavaMapping newJavaMapping = mock(ETRXJavaMapping.class);

    JSONObject entityMappingProperties = new JSONObject();
    entityMappingProperties.put(ManageEntityFieldConstants.JAVAMAPPING, NEW_ID);

    when(OBDal.getInstance().get(eq(ETRXJavaMapping.class), anyString())).thenReturn(newJavaMapping);

    boolean result = (boolean) checkUpdateJavaMappingMethod.invoke(manageEntityMappings, entityMappingProperties, entityField);

    assertTrue("Should return true when Java mapping was changed", result);
    verify(entityField).setJavaMapping(newJavaMapping);
  }

}