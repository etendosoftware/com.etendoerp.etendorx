package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjector;

/**
 * Test class for the BuildConfig's method of updating source configuration with OAuth providers.
 * This test class uses Mockito for mocking dependencies and JUnit for test assertions.
 * It specifically tests the behavior of updating a source JSON object with OAuth provider configurations
 * when the provider list is empty.
 *
 * @see BuildConfig
 * @see ETRXoAuthProvider
 * @see OAuthProviderConfigInjector
 */
@RunWith(MockitoJUnitRunner.class)
public class BuildConfigGetDefaultConfigTest extends OBBaseTest {

  @InjectMocks
  private BuildConfig buildConfig;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<ETRXoAuthProvider> mockCriteria;

  private Method updateSourceWithOAuthProvidersMethod;
  private MockedStatic<OBDal> mockedOBDal;

  /**
   * Sets up the test environment before each test method.
   * Performs the following setup actions:
   * - Calls the parent class's setUp method
   * - Retrieves and makes accessible the private method for updating source with OAuth providers
   * - Creates a static mock of OBDal to control its behavior during testing
   *
   * @throws Exception if any reflection or setup errors occur
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    updateSourceWithOAuthProvidersMethod = BuildConfig.class.getDeclaredMethod(
        "updateSourceWithOAuthProviders",
        JSONObject.class,
        List.class);
    updateSourceWithOAuthProvidersMethod.setAccessible(true);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test method.
   * Closes the mocked OBDal static mock to prevent resource leaks and
   * ensure proper test isolation between test cases.
   *
   * @throws Exception if any cleanup errors occur
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests the scenario of updating a source JSON with an empty list of OAuth providers.
   * Validates that when no OAuth providers are found:
   * - The source JSON remains empty
   * - No modifications are made to the source configuration
   *
   * @throws Exception if any reflection or invocation errors occur during the test
   */
  @Test
  public void testUpdateSourceWithEmptyProviderList() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    List<OAuthProviderConfigInjector> injectors = new ArrayList<>();

    when(mockOBDal.createCriteria(ETRXoAuthProvider.class))
        .thenReturn(mockCriteria);
    when(mockCriteria.setFilterOnReadableOrganization(false))
        .thenReturn(mockCriteria);
    when(mockCriteria.setFilterOnReadableClients(false))
        .thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    updateSourceWithOAuthProvidersMethod.invoke(
        buildConfig, sourceJSON, injectors);

    assertEquals("Source JSON should be empty", 0, sourceJSON.length());
  }
}