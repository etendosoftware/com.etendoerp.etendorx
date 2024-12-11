package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.model.Property;
import java.lang.reflect.Method;

/**
 * Unit test class for {@link ManageEntityFieldsDS}.
 * Focuses on validating the private method {@code isValidEntityReference(Property)}.
 */
public class ManageEntityFieldsDSValidEntityReferenceTest {

  @Mock
  private Property mockProperty;
  @Mock
  private Property mockReferencedProperty;

  private ManageEntityFieldsDS classUnderTest;

  /**
   * Initializes the test environment before each test.
   * Sets up the mock objects and instantiates the class under test.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    classUnderTest = new ManageEntityFieldsDS();
  }

  /**
   * Unit test for {@link ManageEntityFieldsDS(Property)}.
   * Verifies that the method returns false for properties marked as one-to-many relationships.
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsValidEntityReferenceOneToManyProperty() throws Exception {
    when(mockProperty.isOneToMany()).thenReturn(true);

    boolean result = invokeIsValidEntityReference(mockProperty);

    assertFalse("Expected false for one-to-many property", result);
  }

  /**
   * Unit test for {@link ManageEntityFieldsDS(Property)}.
   * Ensures the method returns false for properties with non-blank SQL logic.
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsValidEntityReferenceSqlLogicNotBlank() throws Exception {
    when(mockProperty.isOneToMany()).thenReturn(false);
    when(mockProperty.getSqlLogic()).thenReturn("Some SQL Logic");

    boolean result = invokeIsValidEntityReference(mockProperty);

    assertFalse("Expected false for property with SQL logic", result);
  }

  /**
   * Unit test for {@link ManageEntityFieldsDS#(Property)}.
   * Validates that the method returns false for properties with the name "_computedColumns".
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsValidEntityReferenceComputedColumns() throws Exception {
    when(mockProperty.isOneToMany()).thenReturn(false);
    when(mockProperty.getSqlLogic()).thenReturn("");
    when(mockProperty.getName()).thenReturn("_computedColumns");

    boolean result = invokeIsValidEntityReference(mockProperty);

    assertFalse("Expected false for property named '_computedColumns'", result);
  }

  /**
   * Unit test for {@link ManageEntityFieldsDS#(Property)}.
   * Checks that the method returns false for properties referencing a column with "AD_Image_ID".
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsValidEntityReferenceReferencedPropertyWithADImageID() throws Exception {
    when(mockProperty.isOneToMany()).thenReturn(false);
    when(mockProperty.getSqlLogic()).thenReturn("");
    when(mockProperty.getName()).thenReturn("ValidName");
    when(mockProperty.getReferencedProperty()).thenReturn(mockReferencedProperty);
    when(mockReferencedProperty.getColumnName()).thenReturn("AD_Image_ID");

    boolean result = invokeIsValidEntityReference(mockProperty);

    assertFalse("Expected false for property with referenced property 'AD_Image_ID'", result);
  }

  /**
   * Unit test for {@link ManageEntityFieldsDS(Property)}.
   * Confirms that the method returns true for a property with valid characteristics
   * (not one-to-many, no SQL logic, valid name, no referenced property, and no domain type).
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testIsValidEntityReferenceValidProperty() throws Exception {
    when(mockProperty.isOneToMany()).thenReturn(false);
    when(mockProperty.getSqlLogic()).thenReturn("");
    when(mockProperty.getName()).thenReturn("ValidName");
    when(mockProperty.getReferencedProperty()).thenReturn(null);
    when(mockProperty.getDomainType()).thenReturn(null);

    boolean result = invokeIsValidEntityReference(mockProperty);

    assertTrue("Expected true for valid property", result);
  }

  /**
   * Utility method to invoke the private {@code isValidEntityReference} method using reflection.
   *
   * @param property the {@link Property} instance to be validated
   * @return the result of the validation
   * @throws Exception if reflection fails
   */
  private boolean invokeIsValidEntityReference(Property property) throws Exception {
    Method method = ManageEntityFieldsDS.class.getDeclaredMethod("isValidEntityReference", Property.class);
    method.setAccessible(true);
    return (boolean) method.invoke(classUnderTest, property);
  }
}