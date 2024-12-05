package com.etendoerp.etendorx.callout;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.dal.service.OBDal;
import com.etendoerp.etendorx.data.ETRXProjection;

/**
 * Test class for EntityProjectionUtils.
 * Validates the name building logic for entity projections.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityProjectionUtilsTest {

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private ETRXProjection mockProjection;

  @Mock
  private OBDal mockOBDal;

  private static final String PROJECTION_ID = "testProjectionId";
  private static final String EXTERNAL_NAME = "TestExternalName";
  private static final String PROJECTION_NAME = "ProjectionName";

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    when(mockProjection.getName()).thenReturn(PROJECTION_NAME);
  }

  /**
   * Tests buildName with Write mapping type.
   */
  @Test
  public void testBuildNameWithWriteMapping() {
    String expectedName = PROJECTION_NAME.toUpperCase() + " - " + EXTERNAL_NAME + " - Write";

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(ETRXProjection.class, PROJECTION_ID)).thenReturn(mockProjection);

      EntityProjectionUtils.buildName(EXTERNAL_NAME, "W", PROJECTION_ID, mockInfo);

      verify(mockInfo).addResult("inpname", expectedName);
      verify(mockProjection).getName();
    }
  }

  /**
   * Tests buildName with Read mapping type.
   */
  @Test
  public void testBuildNameWithReadMapping() {
    String expectedName = PROJECTION_NAME.toUpperCase() + " - " + EXTERNAL_NAME + " - Read";

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(ETRXProjection.class, PROJECTION_ID)).thenReturn(mockProjection);

      EntityProjectionUtils.buildName(EXTERNAL_NAME, "R", PROJECTION_ID, mockInfo);

      verify(mockInfo).addResult("inpname", expectedName);
      verify(mockProjection).getName();
    }
  }

  /**
   * Tests buildName with a different mapping type (no suffix added).
   */
  @Test
  public void testBuildNameWithOtherMapping() {
    String expectedName = PROJECTION_NAME.toUpperCase() + " - " + EXTERNAL_NAME;

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(ETRXProjection.class, PROJECTION_ID)).thenReturn(mockProjection);

      EntityProjectionUtils.buildName(EXTERNAL_NAME, "E", PROJECTION_ID, mockInfo);

      verify(mockInfo).addResult("inpname", expectedName);
      verify(mockProjection).getName();
    }
  }

  /**
   * Tests buildName with empty external name.
   */
  @Test
  public void testBuildNameWithEmptyExternalName() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      EntityProjectionUtils.buildName("", "W", PROJECTION_ID, mockInfo);

      verify(mockInfo, never()).addResult(anyString(), anyString());
      verify(mockOBDal, never()).get((Class<Object>) any(), any());
    }
  }

  /**
   * Tests buildName with empty mapping type.
   */
  @Test
  public void testBuildNameWithEmptyMappingType() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      EntityProjectionUtils.buildName(EXTERNAL_NAME, "", PROJECTION_ID, mockInfo);

      verify(mockInfo, never()).addResult(anyString(), anyString());
      verify(mockOBDal, never()).get((Class<Object>) any(), any());
    }
  }

  /**
   * Tests buildName with null parameters.
   */
  @Test
  public void testBuildNameWithNullParameters() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      EntityProjectionUtils.buildName(null, null, PROJECTION_ID, mockInfo);

      verify(mockInfo, never()).addResult(anyString(), anyString());
      verify(mockOBDal, never()).get((Class<Object>) any(), any());
    }
  }
}