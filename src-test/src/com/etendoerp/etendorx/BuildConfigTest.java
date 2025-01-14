package com.etendoerp.etendorx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.system.Client;

import com.etendoerp.etendorx.data.ConfigServiceParam;
import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import com.etendoerp.etendorx.utils.OAuthProviderConfigInjector;
import com.etendoerp.etendorx.utils.RXConfigUtils;
import com.smf.securewebservices.SWSConfig;

/**
 * Test class for the BuildConfig functionality in the EtendoRX module.
 * This class contains unit tests that verify the configuration building process
 * and related utility methods for the EtendoRX configuration system.
 */
@RunWith(MockitoJUnitRunner.class)
public class BuildConfigTest {


  private BuildConfig buildConfig;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private OBDal obDal;

  @Mock
  private SWSConfig swsConfig;

  @Mock
  private ETRXConfig rxConfig;

  @Mock
  private PrintWriter writer;

  /**
   * Sets up the test environment before each test method.
   * Initializes the BuildConfig instance and configures the mock response writer.
   *
   * @throws Exception
   *     if any initialization fails
   */
  @Before
  public void setup() throws Exception {
    buildConfig = new BuildConfig();
    when(response.getWriter()).thenReturn(writer);
  }

  /**
   * Tests the extraction of URI from an HTTP request.
   * Verifies that the method correctly extracts the URI portion after the servlet path.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testGetURIFromRequest() throws Exception {
    String servletPath = TestUtils.DEFAULT_CONFIG_PATH;
    String requestURL = "http://localhost:8080" + servletPath + TestUtils.AUTH_ENDPOINT;
    when(request.getServletPath()).thenReturn(servletPath);
    when(request.getRequestURL()).thenReturn(new StringBuffer(requestURL));

    Method method = BuildConfig.class.getDeclaredMethod("getURIFromRequest", HttpServletRequest.class);
    method.setAccessible(true);
    String result = (String) method.invoke(buildConfig, request);

    assertEquals(TestUtils.AUTH_ENDPOINT, result);
  }

  /**
   * Tests the source finding functionality in a JSON array.
   * Verifies the correct identification and retrieval of a source object by name.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testFindSource() throws Exception {
    JSONArray propSource = new JSONArray();
    JSONObject sourceObj = new JSONObject();
    sourceObj.put(TestUtils.NAME, TestUtils.AUTH);
    JSONObject source = new JSONObject();
    source.put("key", TestUtils.VALUE);
    sourceObj.put(TestUtils.SOURCE, source);
    propSource.put(sourceObj);

    Method method = BuildConfig.class.getDeclaredMethod("findSource", JSONArray.class, String.class);
    method.setAccessible(true);
    SimpleEntry<Integer, JSONObject> result =
        (SimpleEntry<Integer, JSONObject>) method.invoke(buildConfig, propSource, TestUtils.AUTH);

    assertEquals(0, result.getKey().intValue());
    assertEquals(TestUtils.VALUE, result.getValue().getString("key"));
  }

  /**
   * Tests the addition of RX parameters to a JSON object.
   * Verifies that configuration parameters are correctly added to the source JSON.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testAddRXParams() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    List<ConfigServiceParam> params = new ArrayList<>();
    ConfigServiceParam param = mock(ConfigServiceParam.class);
    when(param.getParameterKey()).thenReturn("testKey");
    when(param.getParameterValue()).thenReturn("testValue");
    params.add(param);
    when(rxConfig.getETRXServiceParamList()).thenReturn(params);

    Method method = BuildConfig.class.getDeclaredMethod("addRXParams", JSONObject.class, ETRXConfig.class);
    method.setAccessible(true);
    method.invoke(buildConfig, sourceJSON, rxConfig);

    assertEquals("testValue", sourceJSON.getString("testKey"));
  }

  /**
   * Tests the update of source configuration with OAuth provider information.
   * Verifies that OAuth provider details are correctly integrated into the source JSON.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testUpdateSourceWithOAuthProvider() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    ETRXoAuthProvider provider = mock(ETRXoAuthProvider.class);
    when(provider.getValue()).thenReturn("testProvider");
    when(provider.getOAuthAPIURL()).thenReturn("http://api.test");
    when(provider.getIDForClient()).thenReturn("clientId");
    when(provider.getScope()).thenReturn("scope");
    List<OAuthProviderConfigInjector> injectors = new ArrayList<>();
    String authURL = "http://auth.test";

    Method method = BuildConfig.class.getDeclaredMethod("updateSourceWithOAuthProvider",
        JSONObject.class, ETRXoAuthProvider.class, List.class, String.class);
    method.setAccessible(true);
    method.invoke(buildConfig, sourceJSON, provider, injectors, authURL);

    assertTrue(sourceJSON.has("testProvider-api"));
    assertEquals("http://api.test", sourceJSON.getString("testProvider-api"));
  }

  /**
   * Tests the error response handling mechanism.
   * Verifies that error messages are properly formatted and sent in the HTTP response.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testSendErrorResponse() throws Exception {
    String errorMessage = "Test error";

    Method method = BuildConfig.class.getDeclaredMethod("sendErrorResponse",
        HttpServletResponse.class, String.class);
    method.setAccessible(true);
    method.invoke(buildConfig, response, errorMessage);

    verify(response).setContentType(TestUtils.APPLICATION_JSON);
    verify(response).setCharacterEncoding(TestUtils.UTF_8);
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(writer).write(anyString());
  }

  /**
   * Tests the successful execution of the GET request handler.
   * Verifies the complete flow of configuration retrieval and response generation.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testDoGetSuccess() throws Exception {

    try (MockedStatic<SWSConfig> mockedSWSConfig = mockStatic(SWSConfig.class);
         MockedStatic<OBContext> mockedContextStatic = mockStatic(OBContext.class);
         MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<Preferences> mockedPreferences = mockStatic(Preferences.class);
         MockedStatic<RXConfigUtils> mockedRXConfig = mockStatic(RXConfigUtils.class)) {
      mockedContextStatic.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(invocation -> null);
      mockedContextStatic.when(OBContext::restorePreviousMode).thenAnswer(invocation -> null);

      mockedSWSConfig.when(SWSConfig::getInstance).thenReturn(swsConfig);
      when(swsConfig.getPrivateKey()).thenReturn("testKey");
      mockedPreferences.when(() -> Preferences.getPreferenceValue(anyString(), anyBoolean(),
          (Client) any(), any(), any(), any(), any())).thenReturn("ES256");

      mockedRXConfig.when(() -> RXConfigUtils.getRXConfig(anyString())).thenReturn(rxConfig);

      buildConfig.doGet(request, response);

      verify(response).setContentType(TestUtils.APPLICATION_JSON);
      verify(response).setCharacterEncoding(TestUtils.UTF_8);
    }
  }

  /**
   * Tests the response sending mechanism.
   * Verifies that the JSON response is correctly formatted and written to the response output.
   *
   * @throws Exception
   *     if the test execution fails
   */
  @Test
  public void testSendResponse() throws Exception {
    JSONObject result = new JSONObject();
    JSONArray propSources = new JSONArray();
    JSONObject propSource = new JSONObject();
    propSource.put(TestUtils.SOURCE, new JSONObject());
    propSources.put(propSource);
    result.put(TestUtils.PROPERTY_SOURCES, propSources);

    JSONObject sourceJSON = new JSONObject();
    Integer indexFound = 0;

    Method method = BuildConfig.class.getDeclaredMethod("sendResponse",
        HttpServletResponse.class, JSONObject.class, JSONObject.class, Integer.class);
    method.setAccessible(true);
    method.invoke(buildConfig, response, result, sourceJSON, indexFound);

    verify(response).setContentType(TestUtils.APPLICATION_JSON);
    verify(response).setCharacterEncoding(TestUtils.UTF_8);
    ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
    verify(writer).write(responseCaptor.capture());
    String jsonResponse = responseCaptor.getValue();
    JSONObject jsonResponseObject = new JSONObject(jsonResponse);
    assertTrue(jsonResponseObject.has(TestUtils.PROPERTY_SOURCES));
    assertEquals(1, jsonResponseObject.getJSONArray(TestUtils.PROPERTY_SOURCES).length());
  }

}