package com.etendoerp.etendorx.callout;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

/**
 * Test class for EntityProjectionChange callout.
 * Validates the behavior of the callout when building projection names.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityProjectionChangeTest {

  @InjectMocks
  private EntityProjectionChange callout;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  private static final String MAPPING_TYPE = "E";
  private static final String EXTERNAL_NAME = "TestExternalName";
  private static final String PROJECTION_ID = "testProjectionId";

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    when(mockInfo.getStringParameter("inpmappingType")).thenReturn(MAPPING_TYPE);
    when(mockInfo.getStringParameter("inpexternalName")).thenReturn(EXTERNAL_NAME);
    when(mockInfo.getStringParameter("inpetrxProjectionId")).thenReturn(PROJECTION_ID);
  }

  /**
   * Tests the execute method with all parameters present.
   */
  @Test
  public void testExecuteWithAllParameters() throws Exception {
    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          EXTERNAL_NAME,
          MAPPING_TYPE,
          PROJECTION_ID,
          mockInfo
      ));
    }
  }

  /**
   * Tests the execute method with null parameters.
   */
  @Test
  public void testExecuteWithNullParameters() throws Exception {
    when(mockInfo.getStringParameter("inpmappingType")).thenReturn(null);
    when(mockInfo.getStringParameter("inpexternalName")).thenReturn(null);
    when(mockInfo.getStringParameter("inpetrxProjectionId")).thenReturn(null);

    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          null,
          null,
          null,
          mockInfo
      ));
    }
  }

  /**
   * Tests the execute method with empty string parameters.
   */
  @Test
  public void testExecuteWithEmptyParameters() throws Exception {
    when(mockInfo.getStringParameter("inpmappingType")).thenReturn("");
    when(mockInfo.getStringParameter("inpexternalName")).thenReturn("");
    when(mockInfo.getStringParameter("inpetrxProjectionId")).thenReturn("");

    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          "",
          "",
          "",
          mockInfo
      ));
    }
  }
}