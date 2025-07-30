package com.etendoerp.etendorx.ssologin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class SSOLoginTest {

  public static final String SSO_AUTH_TYPE = "sso.auth.type";
  public static final String AUTH_0 = "Auth0";
  public static final String SSO_CLIENT_ID = "sso.client.id";
  public static final String CLIENT_123 = "client123";
  public static final String CALLBACK_URL = "sso.callback.url";
  public static final String CALLBACK = "http://callback";
  public static final String SSO_DOMAIN_URL = "sso.domain.url";
  public static final String EXAMPLE_AUTH_0_COM = "example.auth0.com";
  public static final String HREF = "href='";
  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<Utility> utilityStatic;

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) obPropsStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  /**
   * Scenario: "sso.auth.type" is blank or missing → method returns "".
   */
  @Test
  void testGetLoginPageSignInHTMLCodeWhenAuthTypeBlankReturnsEmpty() {
    // 1) Mock OBPropertiesProvider to return properties with no "sso.auth.type"
    Properties props = new Properties();
    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    // 2) Invoke
    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    // 3) Assert empty string
    assertEquals("", html);
  }

  /**
   * Scenario: "sso.auth.type"="Auth0" but one of domain, clientId, or redirectUri is blank → returns "".
   */
  @Test
  void testGetLoginPageSignInHTMLCodeWhenAuth0MissingConfigReturnsEmpty() {
    // Prepare properties with authType="Auth0" but missing "sso.domain.url"
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    props.setProperty(CALLBACK_URL, CALLBACK);
    // domain missing or blank

    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();
    assertEquals("", html);

    // Now test missing clientId
    props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, EXAMPLE_AUTH_0_COM);
    props.setProperty(CALLBACK_URL, CALLBACK);
    // clientId missing
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    html = sso.getLoginPageSignInHTMLCode();
    assertEquals("", html);

    // Now test missing redirectUri
    props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, EXAMPLE_AUTH_0_COM);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    // callback blank
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    html = sso.getLoginPageSignInHTMLCode();
    assertEquals("", html);
  }

  /**
   * Scenario: "sso.auth.type"="Auth0" and domain, clientId, redirectUri all set → returns correct Auth0 button HTML.
   */
  @Test
  void testGetLoginPageSignInHTMLCodeWhenAuth0ProperConfigReturnsButtonHtml() {
    // 1) Mock properties for Auth0 flow
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, EXAMPLE_AUTH_0_COM);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    props.setProperty(CALLBACK_URL, CALLBACK);
    // context.name not needed here

    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    // 2) Mock OBDal.getInstance().get(Client.class, "0") → return clientMock with language
    var dalMock = mock(OBDal.class);
    var clientMock = mock(Client.class);
    var languageMock = mock(Language.class);
    when(languageMock.getLanguage()).thenReturn("en_US");
    when(clientMock.getLanguage()).thenReturn(languageMock);
    when(dalMock.get(Client.class, "0")).thenReturn(clientMock);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);

    // 3) Mock Utility.messageBD(...) to return login button label
    utilityStatic = mockStatic(Utility.class);
    when(Utility.messageBD(any(DalConnectionProvider.class), eq("ETRX_LoginSSO"), eq("en_US")))
        .thenReturn("SignInWithAuth0");

    // 4) Invoke
    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    // 5) Assertions: must start with "<br><style>" and contain domain, clientId, redirectUri, and loginButtonMessage
    assertTrue(html.startsWith("<br><style>"));
    assertTrue(html.contains("domain: 'example.auth0.com'"));
    assertTrue(html.contains("clientID: 'client123'"));
    assertTrue(html.contains("redirectUri: 'http://callback'"));
    assertTrue(html.contains(">SignInWithAuth0<")); // the button label
    assertTrue(html.contains(".sso-login-button")); // style class included
  }

  /**
   * Scenario: "sso.auth.type" is not "Auth0" (e.g., "Other") → return divider + icon container HTML.
   * Must use sso.middleware.url and sso.middleware.redirectUri.
   */
  @Test
  void testGetLoginPageSignInHTMLCodeWhenNonAuth0ReturnsIconContainerHtml() throws ServletException {
    // 1) Mock properties for non-Auth0 flow
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, "OtherType");
    props.setProperty("sso.middleware.url", "http://middleware.example");
    props.setProperty("sso.middleware.redirectUri", "http://redirect");

    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    // 2) Mock SystemInfo.getSystemIdentifier()
    String fakeAccountId = "system_456";
    var systemInfoMock = mockStatic(SystemInfo.class);
    systemInfoMock.when(SystemInfo::getSystemIdentifier).thenReturn(fakeAccountId);

    // 3) Invoke
    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    // 4) Assertions
    assertTrue(html.contains("class='sso-divider-wrapper'"));
    assertTrue(html.contains("<span>OR</span>"));

    String baseLoginUrl = "http://middleware.example/login";
    String expectedRedirect = "&redirect_uri=http://redirect";

    assertTrue(html.contains(HREF + baseLoginUrl + "?provider=google-oauth2&account_id=" + fakeAccountId + expectedRedirect + "'"));
    assertTrue(html.contains(HREF + baseLoginUrl + "?provider=windowslive&account_id=" + fakeAccountId + expectedRedirect + "'"));
    assertTrue(html.contains(HREF + baseLoginUrl + "?provider=linkedin&account_id=" + fakeAccountId + expectedRedirect + "'"));
    assertTrue(html.contains(HREF + baseLoginUrl + "?provider=github&account_id=" + fakeAccountId + expectedRedirect + "'"));
    assertTrue(html.contains(HREF + baseLoginUrl + "?provider=facebook&account_id=" + fakeAccountId + expectedRedirect + "'"));

    assertTrue(html.contains("alt='Google'"));
    assertTrue(html.contains("alt='Microsoft'"));
    assertTrue(html.contains("alt='LinkedIn'"));
    assertTrue(html.contains("alt='GitHub'"));
    assertTrue(html.contains("alt='Facebook'"));

    systemInfoMock.close();
  }
}
