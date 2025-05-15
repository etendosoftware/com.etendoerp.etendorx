package com.etendoerp.etendorx.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.test.base.TestConstants;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.etendoerp.etendorx.data.ETRXTokenUser;
import com.etendoerp.etendorx.utils.TokenVerifier;

/**
 * Unit tests for the SWSAuthenticationManager class.
 */
public class SWSAuthenticationManagerTest extends WeldBaseTest {

  public static final String TEST_SECRET = "testSecret";
  public static final String AUTHORIZATION = "Authorization";
  public static final String USER_ID_123 = "userId123";
  public static final String EXAMPLE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJpc3N1ZXIiLCJ1c2VyX21ldGFkYXRhIjp7Im5hbWUiOiJ0ZXN0VXNlciJ9fQ.I8eODDNFArRF4up9iLpLjAdizOy8obNTtTgiRpEyGk0";

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBPropertiesProvider mockPropertiesProvider;


  @Mock
  OBContext obContextMock;

  @Mock
  private com.smf.securewebservices.SWSConfig mockSWSConfig;

  @InjectMocks
  private SWSAuthenticationManager authManager;


  /**
   * Rule for handling expected exceptions in JUnit tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
    super.setUp();
  }

  /**
   * Tests the doWebServiceAuthenticate method with a valid token.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoWebServiceAuthenticate_ValidToken() throws Exception {
    try
        (MockedStatic<com.smf.securewebservices.SWSConfig> staticMockSWSConfigSingleton =
             mockStatic(com.smf.securewebservices.SWSConfig.class);
         MockedStatic<OBContext> staticMockOBContext = mockStatic(OBContext.class)
        ) {
      staticMockSWSConfigSingleton.when(com.smf.securewebservices.SWSConfig::getInstance).thenReturn(mockSWSConfig);
      when(mockSWSConfig.getPrivateKey()).thenReturn(TEST_SECRET);
      when(mockSWSConfig.getExpirationTime()).thenReturn(1000L);
      staticMockOBContext.when(OBContext::getOBContext).thenReturn(obContextMock);
      staticMockOBContext.when(() -> OBContext.setOBContext((OBContext) any())).thenAnswer(invocation -> null);
      staticMockOBContext.when(() -> OBContext.setOBContextInSession(any(), any())).thenAnswer(invocation -> null);

      // Prepare a valid JWT token

      String userId = "testUserId";
      String roleId = "testRoleId";
      String orgId = "testOrgId";
      String warehouseId = "testWarehouseId";
      String clientId = "testClientId";

      String testSecret = TEST_SECRET;
      Algorithm algorithm = Algorithm.HMAC256(testSecret);
      String token = JWT.create()
          .withClaim("user", userId)
          .withClaim("role", roleId)
          .withClaim("organization", orgId)
          .withClaim("warehouse", warehouseId)
          .withClaim("client", clientId)
          .withIssuer("sws")
          .sign(algorithm);

      // Setup mocks
      when(mockRequest.getHeader(AUTHORIZATION)).thenReturn("Bearer " + token);

      // Invoke method
      String result = authManager.doWebServiceAuthenticate(mockRequest);

      // Verify
      assertEquals(userId, result);
    }
  }

  /**
   * Tests the doWebServiceAuthenticate method with an invalid token.
   *
   * @throws OBException
   *     if the token is invalid
   */
  @Test(expected = OBException.class)
  public void testDoWebServiceAuthenticate_InvalidToken() throws Exception {
    // Prepare an invalid token with missing claims
    Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
    String token = JWT.create()
        .withClaim("user", "")
        .sign(algorithm);

    // Setup mocks
    when(mockRequest.getHeader(AUTHORIZATION)).thenReturn("Bearer " + token);

    // Invoke method
    authManager.doWebServiceAuthenticate(mockRequest);
  }

  /**
   * Tests the doAuthenticate method with an invalid token.
   *
   * @throws OBException
   *     if the token is invalid
   */
  @Test(expected = OBException.class)
  public void testDoAuthenticate_InvalidToken() throws Exception {
    // Prepare test data
    String token = "invalidToken";
    String receivedUser = "testuser";

    // Setup mocks
    when(mockRequest.getParameter("access_token")).thenReturn(token);
    when(mockRequest.getParameter("user")).thenReturn(receivedUser);

    // Invoke method will throw OBException
    authManager.doAuthenticate(mockRequest, mockResponse);
  }

  /**
   * Tests the setCORSHeaders method to ensure it sets the correct CORS headers.
   *
   * @throws ServletException
   *     if a servlet-specific error occurs
   * @throws IOException
   *     if an I/O error occurs
   */
  @Test
  public void testSetCORSHeaders() throws ServletException, IOException {
    // Prepare test data
    String origin = "http://test.com";

    // Setup mocks
    when(mockRequest.getHeader("Origin")).thenReturn(origin);

    // Invoke method
    authManager.setCORSHeaders(mockRequest, mockResponse);

    // Verify CORS headers
    verify(mockResponse).setHeader("Access-Control-Allow-Origin", origin);
    verify(mockResponse).setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    verify(mockResponse).setHeader("Access-Control-Allow-Credentials", "true");
    verify(mockResponse).setHeader("Access-Control-Allow-Headers",
        "Content-Type, origin, accept, X-Requested-With");
    verify(mockResponse).setHeader("Access-Control-Max-Age", "1000");
  }

  /**
   * Tests the setCORSHeaders method when no origin is provided.
   *
   * @throws ServletException
   *     if a servlet-specific error occurs
   * @throws IOException
   *     if an I/O error occurs
   */
  @Test
  public void testSetCORSHeaders_NoOrigin() throws ServletException, IOException {
    // Setup mocks
    when(mockRequest.getHeader("Origin")).thenReturn(null);

    // Invoke method
    authManager.setCORSHeaders(mockRequest, mockResponse);

    // Verify no headers set
    verify(mockResponse, never()).setHeader(anyString(), anyString());
  }

  /**
   * Tests the doWebServiceAuthenticate method when no token is provided.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoWebServiceAuthenticate_NoToken() throws Exception {
    // Setup mocks
    when(mockRequest.getHeader(AUTHORIZATION)).thenReturn(null);

    // Invoke method
    String result = authManager.doWebServiceAuthenticate(mockRequest);

    // Verify fallback to parent method
    assertNull(result);
  }

  /**
   * Tests the valid token and user authentication flow with successful session creation.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void test_valid_token_authentication_flow() throws Exception {
    // Arrange
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);

    User mockUser = mock(User.class);
    Properties props = mock(Properties.class);
    Preference mockPref = mock(Preference.class);

    OBCriteria<User> criteria = mock(OBCriteria.class);
    OBCriteria<Preference> criteriaPref = mock(OBCriteria.class);

    try (MockedStatic<OBDal> obDalMockedStatic = Mockito.mockStatic(OBDal.class)) {
      obDalMockedStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(request.getParameter("access_token")).thenReturn(EXAMPLE_TOKEN);
      when(request.getParameter("user")).thenReturn("testUser");
      when(request.getSession(true)).thenReturn(session);

      OBPropertiesProvider.setInstance(mockPropertiesProvider);
      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(props);
      when(props.getProperty("OAUTH2_SECRET")).thenReturn("secret");
      when(props.getProperty("OAUTH2_ISSUER")).thenReturn("issuer");

      when(mockOBDal.createCriteria(User.class)).thenReturn(criteria);
      when(criteria.add(any())).thenReturn(criteria);
      when(criteria.setFilterOnReadableClients(false)).thenReturn(criteria);
      when(criteria.setFilterOnReadableOrganization(false)).thenReturn(criteria);
      when(criteria.setMaxResults(1)).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(mockUser);
      when(mockUser.getId()).thenReturn(USER_ID_123);

      when(mockOBDal.createCriteria(Preference.class)).thenReturn(criteriaPref);
      when(criteriaPref.add(any())).thenReturn(criteriaPref);
      when(criteriaPref.setFilterOnReadableClients(false)).thenReturn(criteriaPref);
      when(criteriaPref.setFilterOnReadableOrganization(false)).thenReturn(criteriaPref);
      when(criteriaPref.setMaxResults(1)).thenReturn(criteriaPref);
      when(criteriaPref.uniqueResult()).thenReturn(mockPref);
      when(mockPref.getSearchKey()).thenReturn("N");

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("name", "Test Name");

      // Act
      String result = authManager.doAuthenticate(request, response);

      // Assert
      assertEquals(USER_ID_123, result);
      verify(session).setAttribute("#Authenticated_user", USER_ID_123);
      verify(OBDal.getInstance()).save(mockUser);
      verify(OBDal.getInstance()).flush();
    }
  }
}
