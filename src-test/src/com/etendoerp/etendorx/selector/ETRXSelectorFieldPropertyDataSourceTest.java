package com.etendoerp.etendorx.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.userinterface.selector.Selector;

import com.etendoerp.etendorx.TestUtils;

/**
 * Unit tests for the ETRXSelectorFieldPropertyDataSource class.
 * This test suite verifies the behavior of methods that handle selector field properties
 * and their related data sources.
 */
@RunWith(MockitoJUnitRunner.class)
public class ETRXSelectorFieldPropertyDataSourceTest extends OBBaseTest {

  @InjectMocks
  private ETRXSelectorFieldPropertyDataSource dataSource;

  @Mock
  private OBContext mockContext;

  @Mock
  private ModelProvider mockModelProvider;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Entity mockEntity;

  @Mock
  private Selector mockSelector;

  @Mock
  private Table mockTable;



  /**
   * Sets up the test environment by initializing mocks and setting up basic behaviors.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    when(mockTable.getName()).thenReturn(TestUtils.TEST_TABLE);

    when(mockEntity.getProperties()).thenReturn(new ArrayList<>());
  }

  /**
   * Tests the getBaseEntity method with valid selector parameters.
   * Verifies that the correct entity is returned based on the selector ID.
   */
  @Test
  public void testGetBaseEntityWithValidSelector() {
    // Arrange
    Map<String, String> parameters = new HashMap<>();
    parameters.put("inpobuiselSelectorId", TestUtils.TEST_SELECTOR_ID);

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

      when(mockOBDal.get(Selector.class, TestUtils.TEST_SELECTOR_ID)).thenReturn(mockSelector);
      when(mockSelector.getTable()).thenReturn(mockTable);
      when(mockTable.getName()).thenReturn(TestUtils.TEST_TABLE);
      when(mockModelProvider.getEntity(TestUtils.TEST_TABLE)).thenReturn(mockEntity);

      Entity result = dataSource.getBaseEntity(parameters);

      assertNotNull(result);
      assertEquals(mockEntity, result);
    }
  }

  /**
   * Tests the checkFetchDatasourceAccess method with valid access parameters.
   * Ensures no exceptions are thrown for valid access conditions.
   */
  @Test
  public void testCheckFetchDatasourceAccessWithValidAccess() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("inpTableId", TestUtils.TEST_TABLE_ID);

    try (MockedStatic<OBContext> contextMock = mockStatic(OBContext.class);
         MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class)) {

      contextMock.when(OBContext::getOBContext).thenReturn(mockContext);
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

      when(mockModelProvider.getEntityByTableId(TestUtils.TEST_TABLE_ID)).thenReturn(mockEntity);
      when(mockContext.getEntityAccessChecker()).thenReturn(mock(org.openbravo.dal.security.EntityAccessChecker.class));

      dataSource.checkFetchDatasourceAccess(parameters);
    }
  }

  /**
   * Tests the getEntityProperties method to verify filtering of image properties.
   * Ensures that properties referencing image columns are excluded.
   */
  @Test
  public void testGetEntityPropertiesFilteringImageProperties() {
    Property normalProperty = mock(Property.class);
    Property imageProperty = mock(Property.class);
    Property referencedProperty = mock(Property.class);

    when(normalProperty.getName()).thenReturn("normalProperty");
    when(normalProperty.getReferencedProperty()).thenReturn(null);

    when(imageProperty.getName()).thenReturn("imageProperty");
    when(imageProperty.getReferencedProperty()).thenReturn(referencedProperty);
    when(referencedProperty.getColumnName()).thenReturn(TestUtils.AD_IMAGE_ID);

    List<Property> properties = new ArrayList<>();
    properties.add(normalProperty);
    properties.add(imageProperty);

    when(mockEntity.getProperties()).thenReturn(properties);

    List<Property> result = dataSource.getEntityProperties(mockEntity);

    assertEquals("Should have filtered out the image property", 2, result.size());
    assertTrue("Should contain normal property", result.contains(normalProperty));
    assertFalse("Should not contain image property", result.contains(imageProperty));
  }
}