package com.etendoerp.etendorx.datasource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;

import com.etendoerp.etendorx.data.ETRXProjectionEntity;

/**
 * Test class for the getMaxValueLineNo method in ManageEntityFieldsDS.
 * Tests the retrieval of maximum line number for entity fields.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageEntityFieldsDSGetMaxValueLineNoTest {

  private ManageEntityFieldsDS dataSource;
  private Method getMaxValueLineNoMethod;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private Query<Long> mockQuery;

  @Mock
  private ETRXProjectionEntity mockProjectionEntity;

  private MockedStatic<OBDal> mockedOBDal;

  private static final String TEST_ENTITY_ID = "test-entity-id";

  /**
   * Sets up the test environment before each test method.
   *
   * @throws Exception if any initialization error occurs
   */
  @Before
  public void setUp() throws Exception {
    dataSource = new ManageEntityFieldsDS();

    getMaxValueLineNoMethod = ManageEntityFieldsDS.class.getDeclaredMethod(
        "getMaxValueLineNo",
        ETRXProjectionEntity.class
    );
    getMaxValueLineNoMethod.setAccessible(true);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
    when(mockProjectionEntity.getId()).thenReturn(TEST_ENTITY_ID);
    when(mockQuery.setParameter("etrxProjectionEntityId", TEST_ENTITY_ID)).thenReturn(mockQuery);
  }

  /**
   * Cleans up the test environment after each test method.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests the {@code getMaxValueLineNo} method when existing lines are present.
   * <p>
   * Verifies that the method returns the maximum line number correctly.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testGetMaxValueLineNoWithExistingLines() throws Exception {
    Long expectedMaxLine = 100L;
    when(mockQuery.uniqueResult()).thenReturn(expectedMaxLine);

    Long result = (Long) getMaxValueLineNoMethod.invoke(dataSource, mockProjectionEntity);

    assertEquals("Max line number should match expected value", expectedMaxLine, result);
  }

  /**
   * Tests the {@code getMaxValueLineNo} method when no lines exist.
   * <p>
   * Verifies that the method returns the default value of 0.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testGetMaxValueLineNoWithNoLines() throws Exception {
    when(mockQuery.uniqueResult()).thenReturn(null);

    Long result = (Long) getMaxValueLineNoMethod.invoke(dataSource, mockProjectionEntity);

    assertEquals("Should return 0 when no lines exist", Long.valueOf(0), result);
  }

  /**
   * Tests the {@code getMaxValueLineNo} method when the maximum line is zero.
   * <p>
   * Verifies that the method correctly handles and returns 0.
   * </p>
   *
   * @throws Exception if any reflection-related error occurs
   */
  @Test
  public void testGetMaxValueLineNoWithZeroLines() throws Exception {
    when(mockQuery.uniqueResult()).thenReturn(0L);

    Long result = (Long) getMaxValueLineNoMethod.invoke(dataSource, mockProjectionEntity);

    assertEquals("Should return 0 when max line is 0", Long.valueOf(0), result);
  }
}