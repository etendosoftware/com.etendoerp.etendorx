package com.etendoerp.etendorx.ssologin;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Criterion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBCriteria;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendoerp.etendorx.data.ETRXTokenUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkAuth0AccountTest {

  public static final String DUMMY_TOKEN = "dummyToken";
  public static final String TOKEN_XYZ = "tokenXYZ";
  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private MockedStatic<JWT> jwtStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<OBDal> obDalStatic;

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) obPropsStatic.close();
    if (jwtStatic != null) jwtStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
    if (obDalStatic != null) obDalStatic.close();
  }

  @Test
  void testDoPost_nonAuth0_existingUser_removesAndCreatesTokenUser_andRedirects() throws Exception {
    HttpServletRequest request = mockRequest(DUMMY_TOKEN, "NotAuth0", "ctx");
    HttpServletResponse response = mock(HttpServletResponse.class);

    DecodedJWT decodedJwt = mockDecodedJwt(
        "Auth0Provider|user123",
        "John", "Doe", "john@example.com", "sidValue"
    );
    mockJwtDecode(DUMMY_TOKEN, decodedJwt);

    var obContextMock = mockContextWithUser();
    mockOBContext(obContextMock);

    var dalMock = mockDalWithExistingUser();
    mockOBDal(dalMock);

    ETRXTokenUser newTokenUser = mock(ETRXTokenUser.class);
    mockOBProvider(newTokenUser);

    new LinkAuth0Account().doPost(request, response);

    verify(dalMock).remove(any(ETRXTokenUser.class));
    verify(newTokenUser).setSub("Auth0Provider|user123");
    verify(newTokenUser).setOAuthToken(DUMMY_TOKEN);
    verify(newTokenUser).setTokenProvider("Auth0Provider");
    verify(newTokenUser).setUserForToken(obContextMock.getUser());
    verify(dalMock).save(newTokenUser);
    verify(dalMock).flush();
    verify(response).sendRedirect("/ctx");
  }

  @Test
  void testDoPost_nonAuth0_noExistingUser_createsTokenUser_andRedirects() throws Exception {
    HttpServletRequest request = mockRequest(TOKEN_XYZ, "Whatever", "myApp");
    HttpServletResponse response = mock(HttpServletResponse.class);

    DecodedJWT decodedJwt = mockDecodedJwt(
        "ProviderABC|user789",
        "Alice", "Smith", "alice@example.com", "sid123"
    );
    mockJwtDecode(TOKEN_XYZ, decodedJwt);

    var obContextMock = mockContextWithUser();
    mockOBContext(obContextMock);

    var dalMock = mockDalNoExistingUser();
    mockOBDal(dalMock);

    ETRXTokenUser newTokenUser = mock(ETRXTokenUser.class);
    mockOBProvider(newTokenUser);

    new LinkAuth0Account().doPost(request, response);

    verify(dalMock, never()).remove(any(ETRXTokenUser.class));
    verify(newTokenUser).setSub("ProviderABC|user789");
    verify(newTokenUser).setOAuthToken(TOKEN_XYZ);
    verify(newTokenUser).setTokenProvider("ProviderABC");
    verify(newTokenUser).setUserForToken(obContextMock.getUser());
    verify(dalMock).save(newTokenUser);
    verify(dalMock).flush();
    verify(response).sendRedirect("/myApp");
  }

  private HttpServletRequest mockRequest(String token, String authType, String contextName) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getParameter("access_token")).thenReturn(token);

    Properties props = new Properties();
    props.setProperty("sso.auth.type", authType);
    props.setProperty("context.name", contextName);
    var propsProvider = mock(OBPropertiesProvider.class);
    when(propsProvider.getOpenbravoProperties()).thenReturn(props);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProvider);

    return req;
  }

  /**
   * Mocks a DecodedJWT so that getClaim(...) returns non-null Claim objects for each key used:
   * "sub", "given_name", "family_name", "email", "sid".
   */
  private DecodedJWT mockDecodedJwt(
      String subValue,
      String givenName,
      String familyName,
      String email,
      String sid
  ) {
    DecodedJWT jwt = mock(DecodedJWT.class);

    Claim subClaim = mock(Claim.class);
    when(subClaim.asString()).thenReturn(subValue);

    Claim givenClaim = mock(Claim.class);
    when(givenClaim.asString()).thenReturn(givenName);

    Claim familyClaim = mock(Claim.class);
    when(familyClaim.asString()).thenReturn(familyName);

    Claim emailClaim = mock(Claim.class);
    when(emailClaim.asString()).thenReturn(email);

    Claim sidClaim = mock(Claim.class);
    when(sidClaim.asString()).thenReturn(sid);

    when(jwt.getClaim("sub")).thenReturn(subClaim);
    when(jwt.getClaim("given_name")).thenReturn(givenClaim);
    when(jwt.getClaim("family_name")).thenReturn(familyClaim);
    when(jwt.getClaim("email")).thenReturn(emailClaim);
    when(jwt.getClaim("sid")).thenReturn(sidClaim);

    return jwt;
  }

  private void mockJwtDecode(String token, DecodedJWT decodedJwt) {
    jwtStatic = mockStatic(JWT.class);
    jwtStatic.when(() -> JWT.decode(token)).thenReturn(decodedJwt);
  }

  private OBContext mockContextWithUser() {
    var context = mock(OBContext.class);
    var user = mock(org.openbravo.model.ad.access.User.class);
    when(context.getUser()).thenReturn(user);
    return context;
  }

  private void mockOBContext(OBContext context) {
    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
    obContextStatic.when(OBContext::getOBContext).thenReturn(context);
    obContextStatic.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);
  }

  private OBDal mockDalWithExistingUser() {
    var dal = mock(OBDal.class);
    @SuppressWarnings("unchecked")
    OBCriteria<ETRXTokenUser> criteria = mock(OBCriteria.class);
    var existing = mock(ETRXTokenUser.class);

    when(dal.createCriteria(ETRXTokenUser.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.setFilterOnReadableClients(false)).thenReturn(criteria);
    when(criteria.setFilterOnReadableOrganization(false)).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(existing);

    doNothing().when(dal).remove(existing);
    doNothing().when(dal).save(any(ETRXTokenUser.class));
    doNothing().when(dal).flush();
    return dal;
  }

  private OBDal mockDalNoExistingUser() {
    var dal = mock(OBDal.class);
    @SuppressWarnings("unchecked")
    OBCriteria<ETRXTokenUser> criteria = mock(OBCriteria.class);

    when(dal.createCriteria(ETRXTokenUser.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.setFilterOnReadableClients(false)).thenReturn(criteria);
    when(criteria.setFilterOnReadableOrganization(false)).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(null);

    doNothing().when(dal).save(any(ETRXTokenUser.class));
    doNothing().when(dal).flush();
    return dal;
  }

  private void mockOBDal(OBDal dal) {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dal);
  }

  private void mockOBProvider(ETRXTokenUser tokenUser) {
    var provider = mock(OBProvider.class);
    when(provider.get(ETRXTokenUser.class)).thenReturn(tokenUser);

    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(provider);
  }
}
