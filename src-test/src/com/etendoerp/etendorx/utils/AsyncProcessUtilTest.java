package com.etendoerp.etendorx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.User;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

class AsyncProcessUtilTest {

  private MockedStatic<RXConfigUtils> rxConfigUtilsMockedStatic;
  private MockedStatic<SecureWebServicesUtils> secureWebServicesUtilsMockedStatic;
  private MockedStatic<OBContext> obContextMockedStatic;
  private MockedStatic<HttpClient> httpClientMockedStatic;

  @BeforeEach
  void setUp() {
    rxConfigUtilsMockedStatic = mockStatic(RXConfigUtils.class);
    secureWebServicesUtilsMockedStatic = mockStatic(SecureWebServicesUtils.class);
    obContextMockedStatic = mockStatic(OBContext.class);
    httpClientMockedStatic = mockStatic(HttpClient.class);
  }

  @AfterEach
  void tearDown() {
    rxConfigUtilsMockedStatic.close();
    secureWebServicesUtilsMockedStatic.close();
    obContextMockedStatic.close();
    httpClientMockedStatic.close();
  }

  @Test
  void testGetRequestSuccess() {
    ETRXConfig config = mock(ETRXConfig.class);
    when(config.getServiceURL()).thenReturn("http://localhost:8080");
    rxConfigUtilsMockedStatic.when(() -> RXConfigUtils.getRXConfig("asyncprocess")).thenReturn(config);

    OBContext context = mock(OBContext.class);
    User user = mock(User.class);
    obContextMockedStatic.when(OBContext::getOBContext).thenReturn(context);
    when(context.getUser()).thenReturn(user);

    secureWebServicesUtilsMockedStatic.when(() -> SecureWebServicesUtils.generateToken(user)).thenReturn("test-token");

    HttpRequest request = AsyncProcessUtil.getRequest("/test");

    assertNotNull(request);
    assertEquals("http://localhost:8080/test", request.uri().toString());
    assertEquals("Bearer test-token", request.headers().firstValue("Authorization").orElse(null));
  }

  @Test
  void testFmtDate() throws ParseException {
    String input = "22-12-2025 10:00:00:000";
    String expected = "2025-12-22 10:00:00:000";
    assertEquals(expected, AsyncProcessUtil.fmtDate(input));
  }

  @Test
  void testGetRows() throws Exception {
    String json = "[{\"key\": \"value\"}]";
    List<Map<String, Object>> rows = AsyncProcessUtil.getRows(json);
    assertNotNull(rows);
    assertEquals(1, rows.size());
    assertEquals("value", rows.get(0).get("key"));
  }

  @Test
  void testGetList() throws Exception {
    ETRXConfig config = mock(ETRXConfig.class);
    when(config.getServiceURL()).thenReturn("http://localhost:8080");
    rxConfigUtilsMockedStatic.when(() -> RXConfigUtils.getRXConfig("asyncprocess")).thenReturn(config);

    OBContext context = mock(OBContext.class);
    User user = mock(User.class);
    obContextMockedStatic.when(OBContext::getOBContext).thenReturn(context);
    when(context.getUser()).thenReturn(user);
    secureWebServicesUtilsMockedStatic.when(() -> SecureWebServicesUtils.generateToken(user)).thenReturn("test-token");

    HttpClient httpClient = mock(HttpClient.class);
    httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(httpClient);

    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.body()).thenReturn("[{\"key\": \"value\"}]");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

    List<Map<String, Object>> result = AsyncProcessUtil.getList("/test");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("value", result.get(0).get("key"));
  }
}
