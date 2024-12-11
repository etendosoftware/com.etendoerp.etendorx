package com.etendoerp.etendorx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.data.InstanceConnector;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Test class for the ConnectorPropertiesInjector.
 * This test class verifies the behavior of the {@link ConnectorPropertiesInjector}
 * when injecting configuration properties for different scenarios.
 * The tests cover the following cases:
 * <ul>
 *   <li>Successful configuration injection for a worker service</li>
 *   <li>Handling of missing connector instance</li>
 *   <li>Handling of non-worker services</li>
 *   <li>Handling of configuration injection with provider</li>
 * </ul>
 *
 * Uses Mockito for mocking dependencies and simulating various scenarios.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectorPropertiesInjectorTest {


  private ConnectorPropertiesInjector injector;

  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<InstanceConnector> criteria;

  @Mock
  private InstanceConnector instanceConnector;

  @Mock
  private User tokenUser;

  /**
   * Sets up the test environment before each test method.
   * Initializes the injector and configures mock objects
   * for OBDal and related dependencies.
   */
  @Before
  public void setup() {
    injector = new ConnectorPropertiesInjector();

    when(obDal.createCriteria(InstanceConnector.class)).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
  }

  /**
   * Creates a sample JSON object representing a worker service.
   * Helps in setting up test scenarios by providing a standard
   * JSON structure for worker services.
   *
   * @return JSONObject representing a worker service configuration
   * @throws Exception if there's an error creating the JSON object
   */
  private JSONObject createWorkerJSON() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put(TestUtils.NAME, "worker");

    JSONObject source = new JSONObject();
    JSONArray propertySources = new JSONArray();
    propertySources.put(new JSONObject().put(TestUtils.SOURCE, source));
    sourceJSON.put(TestUtils.PROPERTY_SOURCES, propertySources);

    return sourceJSON;
  }

  /**
   * Tests successful configuration injection for a worker service.
   * Verifies that:
   * <ul>
   *   <li>Connector instance ID is correctly injected</li>
   *   <li>Token is generated and injected correctly</li>
   *   <li>Both 'token' and 'classic.token' are set</li>
   * </ul>
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testInjectConfigSuccess() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<SecureWebServicesUtils> mockedUtils = mockStatic(SecureWebServicesUtils.class)) {

      String connectorId = "test-connector-id";
      String expectedToken = "test-token";
      JSONObject sourceJSON = createWorkerJSON();

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(criteria.uniqueResult()).thenReturn(instanceConnector);
      when(instanceConnector.getId()).thenReturn(connectorId);
      when(instanceConnector.getUserForToken()).thenReturn(tokenUser);
      mockedUtils.when(() -> SecureWebServicesUtils.generateToken(tokenUser)).thenReturn(expectedToken);

      injector.injectConfig(sourceJSON);

      JSONObject resultSource = sourceJSON.getJSONArray(TestUtils.PROPERTY_SOURCES)
          .getJSONObject(0)
          .getJSONObject(TestUtils.SOURCE);

      assertEquals(connectorId, resultSource.getString("connector.instance"));
      assertEquals(expectedToken, resultSource.getString("token"));
      assertEquals(expectedToken, resultSource.getString("classic.token"));
    }
  }

  /**
   * Tests handling of configuration injection when no connector instance exists.
   * Expects an {@link OBException} to be thrown when no connector instance
   * can be found during configuration injection.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test(expected = OBException.class)
  public void testInjectConfigNoConnectorInstance() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      JSONObject sourceJSON = createWorkerJSON();

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(criteria.uniqueResult()).thenReturn(null);

      injector.injectConfig(sourceJSON);
    }
  }

  /**
   * Tests configuration injection for a non-worker service.
   * Verifies that no property sources are added for services
   * that are not worker services.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testInjectConfigNonWorkerService() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put(TestUtils.NAME, "other-service");

    injector.injectConfig(sourceJSON);

    assertFalse(sourceJSON.has(TestUtils.PROPERTY_SOURCES));
  }

  /**
   * Tests configuration injection with a provider.
   * Expects an {@link UnsupportedOperationException} to be thrown
   * when attempting to inject configuration with a provider.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testInjectConfigWithProvider() throws Exception {
    injector.injectConfig(new JSONObject(), null);
  }
}