package com.etendoerp.etendorx.oauth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.model.ad.access.User;

import com.etendoerp.etendorx.data.ETRXTokenInfo;
import com.etendoerp.etendorx.utils.GoogleServiceUtil;

class GetOAuthTokenTest {

  private MockedStatic<OBDal> obDalMockedStatic;
  private MockedStatic<OBContext> obContextMockedStatic;
  private MockedStatic<SystemInfo> systemInfoMockedStatic;
  private MockedStatic<GoogleServiceUtil> googleServiceUtilMockedStatic;

  private GetOAuthToken servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private StringWriter responseWriter;

  @BeforeEach
  void setUp() throws Exception {
    obDalMockedStatic = mockStatic(OBDal.class);
    obContextMockedStatic = mockStatic(OBContext.class);
    systemInfoMockedStatic = mockStatic(SystemInfo.class);
    googleServiceUtilMockedStatic = mockStatic(GoogleServiceUtil.class);

    servlet = new GetOAuthToken();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    responseWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
  }

  @AfterEach
  void tearDown() {
    obDalMockedStatic.close();
    obContextMockedStatic.close();
    systemInfoMockedStatic.close();
    googleServiceUtilMockedStatic.close();
  }

  @Test
  void testDoGetSuccess() throws Exception {
    OBDal obDal = mock(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDal);
    
    OBCriteria<ETRXTokenInfo> criteria = mock(OBCriteria.class);
    when(obDal.createCriteria(ETRXTokenInfo.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
    
    ETRXTokenInfo token = mock(ETRXTokenInfo.class);
    when(criteria.uniqueResult()).thenReturn(token);
    
    OBContext context = mock(OBContext.class);
    obContextMockedStatic.when(OBContext::getOBContext).thenReturn(context);
    User user = mock(User.class);
    when(context.getUser()).thenReturn(user);
    
    systemInfoMockedStatic.when(SystemInfo::getSystemIdentifier).thenReturn("system-id");
    
    ETRXTokenInfo validToken = mock(ETRXTokenInfo.class);
    when(validToken.getToken()).thenReturn("valid-token");
    googleServiceUtilMockedStatic.when(() -> GoogleServiceUtil.getValidAccessTokenOrRefresh(any(), anyString()))
        .thenReturn(validToken);

    servlet.doGet(request, response);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
    String result = responseWriter.toString();
    assert(result.contains("valid-token"));
  }

  @Test
  void testDoGetTokenNotFound() throws Exception {
    OBDal obDal = mock(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDal);
    
    OBCriteria<ETRXTokenInfo> criteria = mock(OBCriteria.class);
    when(obDal.createCriteria(ETRXTokenInfo.class)).thenReturn(criteria);
    when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(null);
    
    OBContext context = mock(OBContext.class);
    obContextMockedStatic.when(OBContext::getOBContext).thenReturn(context);

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    String result = responseWriter.toString();
    assert(result.contains("Failed to retrieve token"));
  }
}
