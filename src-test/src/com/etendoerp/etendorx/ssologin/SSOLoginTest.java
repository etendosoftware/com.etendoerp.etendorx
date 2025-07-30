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

@ExtendWith(MockitoExtension.class)
class SSOLoginTest {

  private static final String MISCONFIGURED_MESSAGE_SNIPPET = "External providers cannot be displayed.";
  private static final String AUTH_0 = "Auth0";
  private static final String SSO_AUTH_TYPE = "sso.auth.type";
  private static final String CLIENT_123 = "client123";
  private static final String SSO_CLIENT_ID = "sso.client.id";
  private static final String SSO_CALLBACK_URL = "sso.callback.url";
  private static final String CALLBACK_URL = "http://callback";
  private static final String SSO_DOMAIN_URL = "sso.domain.url";
  private static final String DOMAIN_URL = "example.auth0.com";

  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<Utility> utilityStatic;
  private MockedStatic<SystemInfo> systemInfoStatic;

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) obPropsStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (utilityStatic != null) utilityStatic.close();
    if (systemInfoStatic != null) systemInfoStatic.close();
  }

  @Test
  void testGetLoginPageSignInHTMLCode_whenAuthTypeBlank_returnsEmpty() {
    Properties props = new Properties();
    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    assertEquals("", html);
  }

  @Test
  void testGetLoginPageSignInHTMLCode_whenAuth0MissingConfig_returnsMisconfiguredMessage() {
    var props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    props.setProperty(SSO_CALLBACK_URL, CALLBACK_URL);
    // domain missing
    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();
    assertTrue(html.contains(MISCONFIGURED_MESSAGE_SNIPPET));

    // clientId missing
    props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, DOMAIN_URL);
    props.setProperty(SSO_CALLBACK_URL, CALLBACK_URL);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);

    html = sso.getLoginPageSignInHTMLCode();
    assertTrue(html.contains(MISCONFIGURED_MESSAGE_SNIPPET));

    // redirectUri missing
    props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, DOMAIN_URL);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);

    html = sso.getLoginPageSignInHTMLCode();
    assertTrue(html.contains(MISCONFIGURED_MESSAGE_SNIPPET));
  }

  @Test
  void testGetLoginPageSignInHTMLCode_whenAuth0ProperConfig_returnsButtonHtml() {
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, AUTH_0);
    props.setProperty(SSO_DOMAIN_URL, DOMAIN_URL);
    props.setProperty(SSO_CLIENT_ID, CLIENT_123);
    props.setProperty(SSO_CALLBACK_URL, CALLBACK_URL);

    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    var dalMock = mock(OBDal.class);
    var clientMock = mock(Client.class);
    var languageMock = mock(Language.class);
    when(languageMock.getLanguage()).thenReturn("en_US");
    when(clientMock.getLanguage()).thenReturn(languageMock);
    when(dalMock.get(Client.class, "0")).thenReturn(clientMock);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);

    utilityStatic = mockStatic(Utility.class);
    when(Utility.messageBD(any(DalConnectionProvider.class), eq("ETRX_LoginSSO"), eq("en_US")))
        .thenReturn("SignInWithAuth0");

    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    assertTrue(html.contains("domain: 'example.auth0.com'"));
    assertTrue(html.contains("clientID: 'client123'"));
    assertTrue(html.contains("redirectUri: 'http://callback'"));
    assertTrue(html.contains(">SignInWithAuth0<"));
    assertTrue(html.contains(".sso-login-button"));
  }

  @Test
  void testGetLoginPageSignInHTMLCode_whenNonAuth0_returnsIconContainerHtml() {
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE, "Other");
    props.setProperty("sso.middleware.url", "http://middleware.example");
    props.setProperty("sso.middleware.redirectUri", "http://redirect");

    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    String fakeAccountId = "sys_123";
    systemInfoStatic = mockStatic(SystemInfo.class);
    systemInfoStatic.when(SystemInfo::getSystemIdentifier).thenReturn(fakeAccountId);

    SSOLogin sso = new SSOLogin();
    String html = sso.getLoginPageSignInHTMLCode();

    assertTrue(html.contains("class='sso-divider-wrapper'"));
    assertTrue(html.contains("alt='Google'"));
    assertTrue(html.contains("account_id" + fakeAccountId));
    assertTrue(html.contains("&redirect_uri=http://redirect"));
  }
}
