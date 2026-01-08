package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

/**
 * Test class to verify the constants defined in ManageEntityFieldConstants.
 */
public class ManageEntityFieldConstantsTest {

  /**
   * Test to verify that all constants in ManageEntityFieldConstants have the expected values.
   */
  @Test
  public void testConstants() {
    assertEquals("9738ABD7629C4E59AA1B8AD3631696F7", ManageEntityFieldConstants.MANAGE_ENTITY_FIELDS_TABLE_ID);
    assertEquals("id", ManageEntityFieldConstants.ID);
    assertEquals("Client", ManageEntityFieldConstants.CLIENT);
    assertEquals("Organization", ManageEntityFieldConstants.ORGANIZATION);
    assertEquals("etrxProjectionEntity", ManageEntityFieldConstants.ETRXPROJECTIONENTITY);
    assertEquals("creationDate", ManageEntityFieldConstants.CREATIONDATE);
    assertEquals("createdBy", ManageEntityFieldConstants.CREATEDBY);
    assertEquals("updated", ManageEntityFieldConstants.UPDATED);
    assertEquals("updatedBy", ManageEntityFieldConstants.UPDATEDBY);
    assertEquals("Active", ManageEntityFieldConstants.ACTIVE);
    assertEquals("property", ManageEntityFieldConstants.PROPERTY);
    assertEquals("name", ManageEntityFieldConstants.NAME);
    assertEquals("ismandatory", ManageEntityFieldConstants.ISMANDATORY);
    assertEquals("identifiesUnivocally", ManageEntityFieldConstants.IDENTIFIESUNIVOCALLY);
    assertEquals("module", ManageEntityFieldConstants.MODULE);
    assertEquals("fieldMapping", ManageEntityFieldConstants.FIELDMAPPING);
    assertEquals("javaMapping", ManageEntityFieldConstants.JAVAMAPPING);
    assertEquals("line", ManageEntityFieldConstants.LINE);
    assertEquals("etrxProjectionEntityRelated", ManageEntityFieldConstants.ETRXPROJECTIONENTITYRELATED);
    assertEquals("jsonpath", ManageEntityFieldConstants.JSONPATH);
    assertEquals("etrxConstantValue", ManageEntityFieldConstants.ETRXCONSTANTVALUE);
    assertEquals("obSelected", ManageEntityFieldConstants.OBSELECTED);
    assertEquals("entityFieldCreated", ManageEntityFieldConstants.ENTITYFIELDCREATED);
  }

  /**
   * Test to verify that the private constructor of ManageEntityFieldConstants cannot be accessed.
   *
   * @throws InvocationTargetException if an error occurs during reflection.
   */
  @Test(expected = InvocationTargetException.class)
  public void testPrivateConstructor() throws Exception {
    Constructor<ManageEntityFieldConstants> constructor = ManageEntityFieldConstants.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}
