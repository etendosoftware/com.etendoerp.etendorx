package com.etendoerp.etendorx.callout;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.datamodel.Table;

/**
 * Test class for {@link EntityProjectionAdTableChange}.
 * This class ensures that the {@code execute} method behaves as expected
 * under various input conditions.
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityProjectionAdTableChangeTest {

  @InjectMocks
  private EntityProjectionAdTableChange callout;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private Table mockTable;

  @Mock
  private OBDal mockOBDal;

  private static final String TABLE_ID = "testTableId";
  private static final String TABLE_NAME = "TestTable";
  private static final String MAPPING_TYPE = "E";
  private static final String PROJECTION_ID = "testProjectionId";
  private static final String EXTERNAL_NAME = "TestExternalName";

  /**
   * Sets up the test environment by initializing mocks and configuring behaviors.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    when(mockInfo.getStringParameter("inpadTableId")).thenReturn(TABLE_ID);
    when(mockInfo.getStringParameter("inpmappingType")).thenReturn(MAPPING_TYPE);
    when(mockInfo.getStringParameter("inpetrxProjectionId")).thenReturn(PROJECTION_ID);
    when(mockInfo.getStringParameter("inpexternalName")).thenReturn(EXTERNAL_NAME);

    when(mockTable.getName()).thenReturn(TABLE_NAME);
  }

  /**
   * Tests the {@code execute} method when a valid table ID is provided.
   * Verifies that the correct external name is added as a result and the utility method is called.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testExecuteWithValidTableId() throws Exception {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.get(Table.class, TABLE_ID)).thenReturn(mockTable);

      callout.execute(mockInfo);

      verify(mockInfo).addResult("inpexternalName", TABLE_NAME);

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          TABLE_NAME,
          MAPPING_TYPE,
          PROJECTION_ID,
          mockInfo
      ));
    }
  }

  /**
   * Tests the {@code execute} method when the table ID is empty.
   * Verifies that no external name is added and the utility method is called with the external name.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testExecuteWithEmptyTableId() throws Exception {
    when(mockInfo.getStringParameter("inpadTableId")).thenReturn("");

    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      verify(mockInfo, never()).addResult(eq("inpexternalName"), anyString());

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          EXTERNAL_NAME,
          MAPPING_TYPE,
          PROJECTION_ID,
          mockInfo
      ));
    }
  }

  /**
   * Tests the {@code execute} method when the table ID is null.
   * Verifies that no external name is added and the utility method is called with the external name.
   *
   * @throws Exception if an error occurs during execution
   */
  @Test
  public void testExecuteWithNullTableId() throws Exception {
    when(mockInfo.getStringParameter("inpadTableId")).thenReturn(null);

    try (MockedStatic<EntityProjectionUtils> utilsMock = mockStatic(EntityProjectionUtils.class)) {
      callout.execute(mockInfo);

      verify(mockInfo, never()).addResult(eq("inpexternalName"), anyString());

      utilsMock.verify(() -> EntityProjectionUtils.buildName(
          EXTERNAL_NAME,
          MAPPING_TYPE,
          PROJECTION_ID,
          mockInfo
      ));
    }
  }
}