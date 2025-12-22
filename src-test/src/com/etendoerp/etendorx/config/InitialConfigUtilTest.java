package com.etendoerp.etendorx.config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;

import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Unit tests for the {@link InitialConfigUtil} class.
 */
public class InitialConfigUtilTest {

  private MockedStatic<RequestContext> requestContextMockedStatic;
  private MockedStatic<ConfigParameters> configParametersMockedStatic;
  private MockedStatic<SecureWebServicesUtils> secureWebServicesUtilsMockedStatic;
  private MockedStatic<LoginUtils> loginUtilsMockedStatic;

  private RequestContext requestContext;
  private HttpServletRequest request;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    requestContextMockedStatic = mockStatic(RequestContext.class);
    configParametersMockedStatic = mockStatic(ConfigParameters.class);
    secureWebServicesUtilsMockedStatic = mockStatic(SecureWebServicesUtils.class);
    loginUtilsMockedStatic = mockStatic(LoginUtils.class);

    requestContext = mock(RequestContext.class);
    request = mock(HttpServletRequest.class);
    ServletContext servletContext = mock(ServletContext.class);
    ConfigParameters configParameters = mock(ConfigParameters.class);
    HttpSession session = mock(HttpSession.class);

    requestContextMockedStatic.when(RequestContext::get).thenReturn(requestContext);
    requestContextMockedStatic.when(RequestContext::getServletContext).thenReturn(servletContext);
    configParametersMockedStatic.when(() -> ConfigParameters.retrieveFrom(any())).thenReturn(
        configParameters);

    when(request.getSession(any(Boolean.class))).thenReturn(session);
    when(request.getSession()).thenReturn(session);
    when(configParameters.getFormatPath()).thenReturn("some/path");
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    requestContextMockedStatic.close();
    configParametersMockedStatic.close();
    secureWebServicesUtilsMockedStatic.close();
    loginUtilsMockedStatic.close();
  }

  /**
   * Tests the {@link InitialConfigUtil#initialize(HttpServletRequest)} method.
   */
  @Test
  public void testInitializeWithRequest() {
    VariablesSecureApp vars = InitialConfigUtil.initialize(request);

    assertNotNull(vars);
    secureWebServicesUtilsMockedStatic.verify(() -> SecureWebServicesUtils.fillSessionVariables(request));
    loginUtilsMockedStatic.verify(() -> LoginUtils.readNumberFormat(any(VariablesSecureApp.class), anyString()));
    verify(requestContext).setVariableSecureApp(any(VariablesSecureApp.class));
  }

  /**
   * Tests the {@link InitialConfigUtil#initialize()} method.
   */
  @Test
  public void testInitializeWithoutRequest() {
    when(requestContext.getRequest()).thenReturn(request);

    VariablesSecureApp vars = InitialConfigUtil.initialize();

    assertNotNull(vars);
    verify(requestContext).getRequest();
    verify(requestContext).setVariableSecureApp(any(VariablesSecureApp.class));
  }

  /**
   * Tests that the {@link InitialConfigUtil#initialize(HttpServletRequest)} method throws an
   * OBException when a ServletException occurs.
   */
  @Test(expected = OBException.class)
  public void testInitializeWithRequestThrowsException() {
    secureWebServicesUtilsMockedStatic.when(() -> SecureWebServicesUtils.fillSessionVariables(request))
        .thenThrow(new ServletException("Test exception"));

    InitialConfigUtil.initialize(request);
  }
}
