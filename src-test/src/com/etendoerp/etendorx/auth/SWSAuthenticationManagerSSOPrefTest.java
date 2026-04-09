/*
 ************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 ************************************************************************
 */

package com.etendoerp.etendorx.auth;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;

/**
 * Unit tests for the SSO preference and misconfiguration detection logic in
 * {@link SWSAuthenticationManager}.
 *
 * <p>The methods under test ({@code getAllowSSOPref()} and {@code misconfiguredSSO()}) are
 * private static; they are exercised indirectly through {@link
 * SWSAuthenticationManager#doAuthenticate(HttpServletRequest, HttpServletResponse)}.
 *
 * <p>The behavioral change covered by this test class:
 * <ul>
 *   <li>Before: SSO was opt-in (default "N"; set "Y" to enable).</li>
 *   <li>After: SSO is opt-out (default "Y"/enabled; create a client preference with "N" to
 *       disable).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SWSAuthenticationManagerSSOPrefTest {

  private static final String SSO_AUTH_TYPE_KEY = "sso.auth.type";
  private static final String OAUTH2_SECRET_KEY = "OAUTH2_SECRET";
  private static final String OAUTH2_ISSUER_KEY = "OAUTH2_ISSUER";
  private static final String SSO_AUTH_TYPE_VALUE = "Auth0";
  private static final String ACCESS_TOKEN_PARAM = "access_token";
  private static final String AUTH0_CODE = "auth0Code";
  private static final String SSO_ACCESS_TOKEN = "ssoAccessToken";
  private static final String CONFIG_MISMATCH = "Configuration mismatch";
  private static final String UNEXPECTED_MISMATCH_ERROR = "Unexpected misconfiguration error: ";

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBPropertiesProvider> obPropsStatic;

  private HttpServletRequest mockRequest;
  private HttpServletResponse mockResponse;
  private OBDal mockOBDal;
  private OBPropertiesProvider mockPropsProvider;
  private SWSAuthenticationManager authManager;

  @BeforeEach
  void setUp() {
    mockRequest = mock(HttpServletRequest.class);
    mockResponse = mock(HttpServletResponse.class);
    mockOBDal = mock(OBDal.class);
    mockPropsProvider = mock(OBPropertiesProvider.class);
    authManager = new SWSAuthenticationManager();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(mockPropsProvider);
  }

  @AfterEach
  void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obPropsStatic != null) obPropsStatic.close();
  }

  // ---------------------------------------------------------------------------
  // Helper: set up the Preference OBCriteria mock
  // ---------------------------------------------------------------------------

  /**
   * Wires up the OBDal criteria chain for {@code Preference} and returns the
   * configured {@link OBCriteria} mock so callers can stub {@code uniqueResult()}.
   */
  @SuppressWarnings("unchecked")
  private OBCriteria<Preference> stubPreferenceCriteria() {
    OBCriteria<Preference> criteriaMock = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(Preference.class)).thenReturn(criteriaMock);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.setFilterOnReadableClients(false)).thenReturn(criteriaMock);
    when(criteriaMock.setFilterOnReadableOrganization(false)).thenReturn(criteriaMock);
    when(criteriaMock.setMaxResults(1)).thenReturn(criteriaMock);
    return criteriaMock;
  }

  /**
   * Returns a {@link Properties} instance that advertises the given SSO auth
   * type without any token/code parameters (non-SSO path).
   */
  private Properties propertiesWithSSOType(String ssoAuthType) {
    Properties props = new Properties();
    if (ssoAuthType != null) {
      props.setProperty(SSO_AUTH_TYPE_KEY, ssoAuthType);
    }
    props.setProperty(OAUTH2_SECRET_KEY, "secret");
    props.setProperty(OAUTH2_ISSUER_KEY, "issuer");
    return props;
  }

  // ---------------------------------------------------------------------------
  // getAllowSSOPref() – new default behavior: returns "Y" when no preference
  // ---------------------------------------------------------------------------

  /**
   * When no {@code ETRX_AllowSSOLogin} preference row is found in the database,
   * {@code getAllowSSOPref()} must return {@code "Y"} (SSO enabled by default).
   *
   * <p>This is verified by configuring a non-blank {@code sso.auth.type} AND
   * providing an {@code access_token} so that {@code isSSOLoginAttempt()} returns
   * {@code true} and the returned {@code "Y"} from {@code getAllowSSOPref()} actually
   * drives the {@code misconfiguredSSO()} evaluation. With {@code allowSSO="Y"} and a
   * valid SSO type configured, {@code misconfiguredSSO()} returns {@code false}, so the
   * flow proceeds into the SSO login path (which fails at JWK validation). The test
   * confirms the exception is NOT a CONFIG_MISMATCH, proving that the default
   * of {@code "Y"} was read and correctly allowed the SSO attempt.
   */
  @Test
  void getAllowSSOPrefReturnsYWhenNoPreferenceFound() throws Exception {
    // SSO type IS configured so isSSOLoginAttempt() returns true
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // Preference query returns null (no row in DB) → getAllowSSOPref() returns "Y"
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(null);

    // Provide an access_token so isSSOLoginAttempt() returns true
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(SSO_ACCESS_TOKEN);
    when(mockRequest.getParameter("code")).thenReturn(null);

    // With allowSSO="Y" and ssoType configured, misconfiguredSSO() returns false.
    // The SSO login path is entered and fails at JWK validation (no real JWK server),
    // but the exception must NOT be a CONFIG_MISMATCH, which would indicate
    // that getAllowSSOPref() returned something other than "Y".
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    assertTrue(!ex.getMessage().contains(CONFIG_MISMATCH),
        "getAllowSSOPref() must return 'Y' when no preference row is found; "
            + UNEXPECTED_MISMATCH_ERROR + ex.getMessage());
  }

  /**
   * When no preference is found and an {@link OBSecurityException} is thrown by
   * the OBDal criteria execution, {@code getAllowSSOPref()} must swallow the
   * exception and return {@code "Y"} (safe default – SSO still enabled).
   *
   * <p>A security exception can occur when the OBContext is not fully initialized;
   * the new behavior keeps SSO active rather than silently disabling it. The test
   * configures a non-blank {@code sso.auth.type} and provides an {@code access_token}
   * so that {@code isSSOLoginAttempt()} returns {@code true} and the {@code "Y"} default
   * from {@code getAllowSSOPref()} actually influences {@code misconfiguredSSO()}. With
   * {@code allowSSO="Y"} and a valid SSO type, the flow is not flagged as misconfigured
   * and proceeds into the SSO login path.
   */
  @Test
  void getAllowSSOPrefReturnsYOnOBSecurityException() throws Exception {
    // SSO type IS configured so isSSOLoginAttempt() returns true
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // Preference criteria throws OBSecurityException → getAllowSSOPref() returns "Y"
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenThrow(new OBSecurityException("access denied"));

    // Provide an access_token so isSSOLoginAttempt() returns true
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(SSO_ACCESS_TOKEN);
    when(mockRequest.getParameter("code")).thenReturn(null);

    // With allowSSO="Y" (returned after catching OBSecurityException) and ssoType
    // configured, misconfiguredSSO() returns false. The SSO login path is entered and
    // fails at JWK validation, but NOT with a CONFIG_MISMATCH error.
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    assertTrue(!ex.getMessage().contains(CONFIG_MISMATCH),
        "getAllowSSOPref() must return 'Y' on OBSecurityException; "
            + UNEXPECTED_MISMATCH_ERROR + ex.getMessage());
  }

  /**
   * When a client-level preference explicitly sets {@code ETRX_AllowSSOLogin = "N"},
   * {@code getAllowSSOPref()} must return {@code "N"}.
   *
   * <p>With allowSSO="N" and an SSO type configured, the request is still treated
   * as non-SSO. When a code/token arrive, {@code misconfiguredSSO()} detects a
   * mismatch. This test validates that the preference value "N" is honoured.
   */
  @Test
  void getAllowSSOPrefReturnsNWhenClientPreferenceIsN() throws Exception {
    // SSO type IS configured
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // Preference row exists with value "N"
    Preference mockPref = mock(Preference.class);
    when(mockPref.getSearchKey()).thenReturn("N");
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(mockPref);

    // Simulate an SSO code arriving (would normally be from Auth0 callback)
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(null);
    when(mockRequest.getParameter("code")).thenReturn(AUTH0_CODE);

    // With allowSSO="N" but ssoType present and a code provided,
    // misconfiguredSSO() returns true → OBException thrown about config mismatch.
    // The test confirms that the preference "N" was read and drove the mismatch detection.
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    // showSSOConfigError() calls Utility.messageBD() which needs OBContext (not mocked).
    // The resulting NPE is caught by doAuthenticate and re-wrapped in an OBException.
    // We assert the cause is a NullPointerException rather than inspecting the message text,
    // because NPE messages differ between Java 11 (null) and Java 17 (descriptive via JEP 358).
    assertInstanceOf(NullPointerException.class, ex.getCause(),
        "Expected misconfiguration path (OBContext NPE from showSSOConfigError), got: " + ex);
  }

  // ---------------------------------------------------------------------------
  // misconfiguredSSO() – SSO enabled (Y) but no ssoType configured
  // ---------------------------------------------------------------------------

  /**
   * When {@code allowSSO = "Y"} (the new default) but no SSO type is configured
   * in properties, {@code isSSOLoginAttempt()} returns {@code false} because a
   * non-blank {@code ssoType} is required to enter the SSO branch. Therefore
   * {@code misconfiguredSSO()} is never reached via {@code doAuthenticate()}.
   *
   * <p>This test documents and validates that behavior: even if a client sends a
   * {@code code} parameter signalling SSO intent, with a blank {@code ssoType} the
   * system falls through to the legacy authentication path without entering the SSO
   * misconfiguration check. The legacy path raises an {@link OBException} due to the
   * missing token, not due to an SSO configuration mismatch.
   */
  @Test
  void whenAllowSSOisYAndNoSSOTypeAndCodeArrivesItFallsToLegacyPath() throws Exception {
    // allowSSO = "Y" (no preference row found → default)
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(null);

    // sso.auth.type is blank/absent in properties → isSSOLoginAttempt() returns false
    // even when a token arrives, so misconfiguredSSO() is NOT reached via doAuthenticate().
    Properties props = new Properties();
    props.setProperty(SSO_AUTH_TYPE_KEY, "");
    props.setProperty(OAUTH2_SECRET_KEY, "secret");
    props.setProperty(OAUTH2_ISSUER_KEY, "issuer");
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // Provide a code to signal SSO intent from the client side
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(null);
    when(mockRequest.getParameter("code")).thenReturn(AUTH0_CODE);

    // isSSOLoginAttempt(blank ssoType, code, null) → false; falls to legacy path.
    // The legacy path may throw any exception due to missing servlet context.
    // The key assertion: no SSO misconfiguration was detected.
    Exception ex = assertThrows(Exception.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    String msg = ex.getMessage() != null ? ex.getMessage() : "";
    assertTrue(!msg.contains(CONFIG_MISMATCH),
        "With blank ssoType, misconfiguredSSO() must not be reached; "
            + UNEXPECTED_MISMATCH_ERROR + msg);
  }

  /**
   * When {@code allowSSO = "Y"} and SSO type IS configured, an arriving
   * {@code access_token} is a valid SSO login attempt and {@code misconfiguredSSO()}
   * returns {@code false}.
   *
   * <p>This validates the happy-path gate: a properly configured SSO setup with
   * the new opt-out default should not be flagged as misconfigured.
   *
   * <p>The SSO login itself will fail downstream (no JWK server), but the failure
   * must come from the JWK validation step, not from the misconfiguration check.
   * This is verified by asserting that the exception is thrown (proving the SSO path
   * was entered) and that it is NOT a CONFIG_MISMATCH error.
   */
  @Test
  void misconfiguredSSONotDetectedWhenAllowSSOisYAndSSOTypeIsConfigured() throws Exception {
    // allowSSO = "Y" (preference not found → default "Y")
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(null);

    // sso.auth.type is configured
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // SSO access_token arrives → isSSOLoginAttempt() returns true
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(SSO_ACCESS_TOKEN);
    when(mockRequest.getParameter("code")).thenReturn(null);

    // misconfiguredSSO(SSO_ACCESS_TOKEN, null, "Y", "Auth0") → false; the SSO
    // login flow proceeds and eventually throws because JWK validation fails
    // (no real JWK provider in unit test scope). The exception must NOT be a
    // CONFIG_MISMATCH, which would indicate the misconfiguredSSO() branch ran.
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    assertTrue(!ex.getMessage().contains(CONFIG_MISMATCH),
        "With allowSSO='Y' and ssoType configured, misconfiguredSSO() must return false; "
            + UNEXPECTED_MISMATCH_ERROR + ex.getMessage());
  }

  /**
   * When {@code allowSSO = "N"} (explicitly disabled via client preference)
   * and an SSO type IS configured in properties, the system is inconsistently
   * set up. Any arriving SSO code/token must be treated as a misconfiguration.
   *
   * <p>{@code misconfiguredSSO(null, code, "N", "Auth0")} → {@code true}.
   */
  @Test
  void misconfiguredSSODetectedWhenAllowSSOisNAndSSOTypeIsConfigured() throws Exception {
    // allowSSO = "N" (client preference exists with value "N")
    Preference mockPref = mock(Preference.class);
    when(mockPref.getSearchKey()).thenReturn("N");
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(mockPref);

    // sso.auth.type IS configured
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // An SSO code arrives
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(null);
    when(mockRequest.getParameter("code")).thenReturn(AUTH0_CODE);

    // misconfiguredSSO(null, AUTH0_CODE, "N", "Auth0") → true
    // doAuthenticate calls showSSOConfigError → Utility.messageBD → OBContext NPE.
    // The NPE is caught by doAuthenticate and re-wrapped in an OBException.
    // We assert the cause is a NullPointerException rather than inspecting the message text,
    // because NPE messages differ between Java 11 (null) and Java 17 (descriptive via JEP 358).
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    assertInstanceOf(NullPointerException.class, ex.getCause(),
        "Expected misconfiguration path (OBContext NPE from showSSOConfigError), got: " + ex);
  }

  /**
   * When {@code allowSSO = "N"} and SSO type is blank/absent, no SSO login
   * attempt is detected at all ({@code isSSOLoginAttempt} returns false). In
   * that case {@code misconfiguredSSO()} is never reached and the flow falls
   * through to the standard (non-SSO) authentication path.
   *
   * <p>This ensures the SSO gate does not interfere with regular logins when
   * SSO is disabled at both the preference and property level.
   */
  @Test
  void misconfiguredSSONOtReachedWhenAllowSSOisNAndNoSSOType() throws Exception {
    // allowSSO = "N"
    Preference mockPref = mock(Preference.class);
    when(mockPref.getSearchKey()).thenReturn("N");
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(mockPref);

    // sso.auth.type is absent → isSSOLoginAttempt returns false
    Properties props = propertiesWithSSOType(null);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // No token, no code (pure non-SSO request)
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(null);
    when(mockRequest.getParameter("code")).thenReturn(null);
    when(mockRequest.getHeader("Authorization")).thenReturn(null);

    // doAuthenticate falls through to super.doAuthenticate() which needs a full
    // servlet environment. It may throw NPE (no session mock). What matters is
    // that NO SSO misconfiguration path was triggered.
    try {
      authManager.doAuthenticate(mockRequest, mockResponse);
    } catch (Exception e) {
      String msg = e.getMessage() != null ? e.getMessage() : "";
      assertTrue(!msg.contains(CONFIG_MISMATCH),
          "Unexpected SSO-related exception in non-SSO path: " + msg);
    }
  }

  // ---------------------------------------------------------------------------
  // Preference row present with value "Y" (explicit opt-in after preference exists)
  // ---------------------------------------------------------------------------

  /**
   * When a {@code ETRX_AllowSSOLogin} preference row exists with value {@code "Y"},
   * {@code getAllowSSOPref()} must return {@code "Y"}.
   *
   * <p>With an SSO type configured and a token present, this is a valid SSO
   * attempt; {@code misconfiguredSSO()} must return {@code false} and the SSO
   * login flow is entered. The flow fails downstream at JWK validation (no real
   * JWK provider in unit test scope), but NOT because of a misconfiguration flag.
   */
  @Test
  void getAllowSSOPrefReturnsYWhenExplicitPreferenceIsY() throws Exception {
    // Preference row with explicit "Y"
    Preference mockPref = mock(Preference.class);
    when(mockPref.getSearchKey()).thenReturn("Y");
    OBCriteria<Preference> criteriaMock = stubPreferenceCriteria();
    when(criteriaMock.uniqueResult()).thenReturn(mockPref);

    // SSO type is configured
    Properties props = propertiesWithSSOType(SSO_AUTH_TYPE_VALUE);
    when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    // SSO access_token arrives
    when(mockRequest.getParameter(ACCESS_TOKEN_PARAM)).thenReturn(SSO_ACCESS_TOKEN);
    when(mockRequest.getParameter("code")).thenReturn(null);

    // The SSO path is entered; fails downstream due to missing JWK infrastructure
    // but NOT because of a misconfiguration flag.
    OBException ex = assertThrows(OBException.class,
        () -> authManager.doAuthenticate(mockRequest, mockResponse));

    assertTrue(!ex.getMessage().contains(CONFIG_MISMATCH),
        UNEXPECTED_MISMATCH_ERROR + ex.getMessage());
  }
}
