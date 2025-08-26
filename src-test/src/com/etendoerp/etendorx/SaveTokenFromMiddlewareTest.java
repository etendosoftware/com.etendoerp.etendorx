package com.etendoerp.etendorx;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.data.ETRXoAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;
import org.openbravo.service.db.DalConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SaveTokenFromMiddleware.
 */
@ExtendWith(MockitoExtension.class)
class SaveTokenFromMiddlewareTest {

  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private MockedStatic<Utility> utilityStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<SaveTokenFromMiddleware> middlewareStatic;

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) obPropsStatic.close();
    if (utilityStatic != null) utilityStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (middlewareStatic != null) middlewareStatic.close();
  }

  @Test
  void testDoGet_whenSaveSucceeds_redirectsWithSuccessUrl() throws Exception {
    HttpServletRequest request = mockRequest("token123", "MyProvider", "read write");
    HttpServletResponse response = mock(HttpServletResponse.class);

    mockProperties("myContext");
    mockUtilityMessages("CREATED", "DESCRIPTION");
    mockContextLanguage("en_US");
    mockOBProviderTokenInfo();
    mockOBDalSaveSuccess();
    mockGetETRXoAuthProvider();

    new SaveTokenFromMiddleware().doGet(request, response);

    String expectedUrl = buildUrl("myContext", "CREATED", "DESCRIPTION", SaveTokenFromMiddleware.SUCCESS_ICON, SaveTokenFromMiddleware.GREEN);
    verify(response).sendRedirect(expectedUrl);
    verify(response).flushBuffer();
    verify(response, never()).setHeader(eq("Location"), anyString());
  }

  @Test
  void testDoGet_whenSaveThrowsOBException_setsErrorHeader() throws Exception {
    HttpServletRequest request = mockRequest("tokenError", "ProviderX", "scopeX");
    HttpServletResponse response = mock(HttpServletResponse.class);

    mockProperties("errorContext");
    mockUtilityMessages("FAIL", "FAILDESC");
    mockContextLanguage("es_ES");
    mockOBProviderTokenInfo();
    var dalMock = mockOBDalSaveThrows();
    mockGetETRXoAuthProvider();

    new SaveTokenFromMiddleware().doGet(request, response);

    String expectedErrorUrl = buildUrl("errorContext", "", "", SaveTokenFromMiddleware.ERROR_ICON, SaveTokenFromMiddleware.RED);
    verify(response).setHeader("Location", expectedErrorUrl);
    verify(response).flushBuffer();
    verify(response, never()).sendRedirect(anyString());
  }

  private HttpServletRequest mockRequest(String token, String provider, String scope) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getParameter("access_token")).thenReturn(token);
    when(req.getParameter("provider")).thenReturn(provider);
    when(req.getParameter("scope")).thenReturn(scope);
    when(req.getParameter("error")).thenReturn(null);
    when(req.getParameter("error_description")).thenReturn(null);

    return req;
  }

  private void mockProperties(String contextName) {
    Properties props = new Properties();
    props.setProperty("context.name", contextName);
    var propsProvider = mock(OBPropertiesProvider.class);
    when(propsProvider.getOpenbravoProperties()).thenReturn(props);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProvider);
  }

  private void mockUtilityMessages(String titleMsg, String descMsg) {
    utilityStatic = mockStatic(Utility.class);
    when(Utility.messageBD(any(DalConnectionProvider.class), eq("ETRX_TokenCreated"), anyString()))
        .thenReturn(titleMsg);
    when(Utility.messageBD(any(DalConnectionProvider.class), eq("ETRX_TokenCreatedDescription"), anyString()))
        .thenReturn(descMsg);
  }

  private void mockContextLanguage(String languageCode) {
    var contextMock = mock(OBContext.class);
    var langMock = mock(Language.class);
    when(langMock.getLanguage()).thenReturn(languageCode);
    when(contextMock.getLanguage()).thenReturn(langMock);
    when(contextMock.getUser()).thenReturn(mock(org.openbravo.model.ad.access.User.class));

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
    obContextStatic.when(OBContext::getOBContext).thenReturn(contextMock);
  }

  private void mockOBProviderTokenInfo() {
    var providerMock = mock(OBProvider.class);
    var tokenInfoMock = mock(ETRXTokenInfo.class);
    when(providerMock.get(ETRXTokenInfo.class)).thenReturn(tokenInfoMock);
    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(providerMock);
  }

  private OBDal mockOBDalSaveSuccess() {
    var dalMock = mock(OBDal.class);
    doNothing().when(dalMock).save(any(ETRXTokenInfo.class));
    // mock createCriteria to avoid NullPointerException in removeOldTokens
    var criteriaMock = mock(OBCriteria.class);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.list()).thenReturn(Collections.emptyList());
    when(dalMock.createCriteria(ETRXTokenInfo.class)).thenReturn(criteriaMock);
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);
    return dalMock;
  }

  private OBDal mockOBDalSaveThrows() {
    var dalMock = mock(OBDal.class);
    doThrow(new OBException("db error")).when(dalMock).save(any(ETRXTokenInfo.class));
    // mock createCriteria to avoid NullPointerException in removeOldTokens
    var criteriaMock = mock(OBCriteria.class);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.list()).thenReturn(Collections.emptyList());
    when(dalMock.createCriteria(ETRXTokenInfo.class)).thenReturn(criteriaMock);
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);
    return dalMock;
  }

  private void mockGetETRXoAuthProvider() {
    var providerEntity = mock(ETRXoAuthProvider.class);
    // Configure the existing OBDal mock (created by mockOBDalSaveSuccess/mockOBDalSaveThrows)
    // to return a criteria that yields the provider entity when queried.
    OBDal dal = OBDal.getInstance();
    var criteriaForProvider = mock(OBCriteria.class);
    when(criteriaForProvider.add(any())).thenReturn(criteriaForProvider);
    when(criteriaForProvider.setMaxResults(anyInt())).thenReturn(criteriaForProvider);
    when(criteriaForProvider.uniqueResult()).thenReturn(providerEntity);
    when(dal.createCriteria(ETRXoAuthProvider.class)).thenReturn(criteriaForProvider);
  }

  private String buildUrl(String contextName, String rawTitle, String rawMessage, String icon, String iconColor) {
    String titleEncoded = URLEncoder.encode(rawTitle, StandardCharsets.UTF_8);
    String messageEncoded = URLEncoder.encode(rawMessage, StandardCharsets.UTF_8);
    String iconEncoded = URLEncoder.encode(icon, StandardCharsets.UTF_8);
    String colorEncoded = URLEncoder.encode(iconColor, StandardCharsets.UTF_8);
    return String.format(
        "/%s/web/com.etendoerp.etendorx/resources/MiddlewareResponse.html" +
            "?title=%s&message=%s&icon=%s&iconColor=%s",
        contextName, titleEncoded, messageEncoded, iconEncoded, colorEncoded
    );
  }
}
