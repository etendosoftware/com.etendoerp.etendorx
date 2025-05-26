package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.util.List;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.smf.jobs.Result;

/**
 * Unit tests for the {@link RefreshOAuthConfigs} class.
 * This class verifies the behavior of the `action` method under various scenarios
 * related to OAuth configuration handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class RefreshOAuthConfigsActionTest {

  @InjectMocks
  private RefreshOAuthConfigs refreshOAuthConfigs;

  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<ETRXConfig> mockCriteria;


  /**
   * Sets up the testing environment by initializing mocked dependencies and
   * their behavior.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    when(obDal.createCriteria(ETRXConfig.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any())).thenReturn(mockCriteria);

  }



  /**
   * Tests the behavior of the `action` method when the service URL is empty.
   * Verifies that the result type is set to ERROR and that an appropriate
   * error message is included in the result.
   *
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testEmptyServiceURL() throws Exception {
    JSONObject params = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    ETRXConfig config = mock(ETRXConfig.class);
    when(config.getServiceName()).thenReturn("TestService");
    when(config.getServiceURL()).thenReturn("");
    when(mockCriteria.list()).thenReturn(List.of(config));

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

      var result = refreshOAuthConfigs.action(params, isStopped);

      assertEquals(Result.Type.ERROR, result.getType());
      assertTrue(result.getMessage().contains("no protocol"));
    }
  }
}