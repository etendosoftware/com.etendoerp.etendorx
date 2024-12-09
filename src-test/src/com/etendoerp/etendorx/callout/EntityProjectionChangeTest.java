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

import com.etendoerp.etendorx.TestUtils;

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

  /**
   * Sets up the test environment before each test method.
   * Initialize mocks and configures default return values
   * for input parameters.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    when(mockInfo.getStringParameter(TestUtils.INP_MAPPING_TYPE)).thenReturn(TestUtils.MAPPING_TYPE);
    when(mockInfo.getStringParameter(TestUtils.INP_EXTERNAL_NAME)).thenReturn(TestUtils.EXTERNAL_NAME);
    when(mockInfo.getStringParameter(TestUtils.INP_ETRX_PROJECTION_ID)).thenReturn(TestUtils.PROJECTION_ID);
  }

  /**
   * Tests callout execution with all parameters provided.
   * Verifies that {@link EntityProjectionUtils#buildName}
   * is correctly called with the expected parameters when all values are supplied.
   *
   * @throws Exception if an error occurs during testing
   */
  @Test
  public void testExecuteWithAllParameters() throws Exception {
    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          TestUtils.EXTERNAL_NAME,
          TestUtils.MAPPING_TYPE,
          TestUtils.PROJECTION_ID,
          mockInfo
      ));
    }
  }

  /**
   * Tests callout execution with null parameters.
   * Checks that {@link EntityProjectionUtils#buildName}
   * is invoked correctly when all input parameters are null.
   *
   * @throws Exception if an error occurs during testing
   */
  @Test
  public void testExecuteWithNullParameters() throws Exception {
    when(mockInfo.getStringParameter(TestUtils.INP_MAPPING_TYPE)).thenReturn(null);
    when(mockInfo.getStringParameter(TestUtils.INP_EXTERNAL_NAME)).thenReturn(null);
    when(mockInfo.getStringParameter(TestUtils.INP_ETRX_PROJECTION_ID)).thenReturn(null);

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
   * Tests callout execution with empty parameters.
   * Verifies that {@link EntityProjectionUtils#buildName}
   * is called correctly when all input parameters are empty strings.
   *
   * @throws Exception if an error occurs during testing
   */
  @Test
  public void testExecuteWithEmptyParameters() throws Exception {
    when(mockInfo.getStringParameter(TestUtils.INP_MAPPING_TYPE)).thenReturn("");
    when(mockInfo.getStringParameter(TestUtils.INP_EXTERNAL_NAME)).thenReturn("");
    when(mockInfo.getStringParameter(TestUtils.INP_ETRX_PROJECTION_ID)).thenReturn("");

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