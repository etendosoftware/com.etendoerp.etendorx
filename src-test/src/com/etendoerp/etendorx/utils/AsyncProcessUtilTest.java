package com.etendoerp.etendorx.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;

import com.etendoerp.etendorx.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
/**
 * Unit tests for the AsyncProcessUtil class.
 * This test suite verifies the behavior of utility methods for asynchronous processing,
 * including JSON parsing, HTTP request handling, and date formatting.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncProcessUtilTest {


  @Mock
  private OBPropertiesProvider mockPropertiesProvider;

  @Mock
  private HttpClient mockHttpClient;

  @Mock
  private HttpResponse<String> mockResponse;

  @Mock
  private Properties mockProperties;

  /**
   * Sets up the test environment by clearing system properties and initializing mocks.
   */
  @Before
  public void setUp() {
    mockProperties = new Properties();
    mockProperties.setProperty("async.url", TestUtils.TEST_ASYNC_URL);
    mockProperties.setProperty("async.token", TestUtils.TEST_ASYNC_TOKEN);

    System.clearProperty("ASYNC_URL");
    System.clearProperty("ASYNC_TOKEN");
  }

  /**
   * Tests the parsing of JSON strings into a list of maps using getRows.
   *
   * @throws JsonProcessingException if the JSON string cannot be parsed
   */
  @Test
  public void testGetRows() throws JsonProcessingException {
    String jsonBody = "[{\"key1\":\"value1\"},{\"key2\":\"value2\"}]";

    List<Map<String, Object>> result = AsyncProcessUtil.getRows(jsonBody);

    assertNotNull("Result should not be null", result);
    assertEquals("Should have 2 elements", 2, result.size());
    assertEquals("value1", result.get(0).get("key1"));
    assertEquals("value2", result.get(1).get("key2"));
  }

  /**
   * Tests the construction of an HTTP request with properties configuration.
   */
  @Test
  public void testGetRequestWithPropertiesConfig() {
    try (MockedStatic<OBPropertiesProvider> mockedProvider = mockStatic(OBPropertiesProvider.class)) {
      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
      mockedProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);

      HttpRequest request = AsyncProcessUtil.getRequest(TestUtils.TEST_URI);

      assertNotNull("Request should not be null", request);
      assertEquals(TestUtils.TEST_ASYNC_URL + TestUtils.TEST_URI, request.uri().toString());
      assertTrue(request.headers().map().containsKey("Authorization"));
      assertEquals("Bearer " + TestUtils.TEST_ASYNC_TOKEN,
          request.headers().firstValue("Authorization").orElse(""));
    }
  }

  /**
   * Tests the fetching of a JSON list from an HTTP endpoint.
   *
   * @throws IOException if an IO error occurs during the HTTP request
   * @throws InterruptedException if the HTTP request is interrupted
   */
  @Test
  public void testGetList() throws IOException, InterruptedException {
    try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class);
         MockedStatic<OBPropertiesProvider> mockedProvider = mockStatic(OBPropertiesProvider.class)) {

      String jsonResponse = "[{\"key\":\"value\"}]";
      when(mockResponse.body()).thenReturn(jsonResponse);
      when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(mockResponse);
      mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
      mockedProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);

      List<Map<String, Object>> result = AsyncProcessUtil.getList(TestUtils.TEST_URI);

      assertNotNull("Result should not be null", result);
      assertEquals("Should have 1 element", 1, result.size());
      assertEquals("value", result.get(0).get("key"));
    }
  }

  /**
   * Tests the behavior of getList when an IOException occurs.
   */
  @Test
  public void testGetListWithIOException() {
    try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class);
         MockedStatic<OBPropertiesProvider> mockedProvider = mockStatic(OBPropertiesProvider.class)) {

      HttpClient mockClient = mock(HttpClient.class);
      try {
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Test IO Exception"));
      } catch (IOException e) {
        fail("Mock setup failed: " + e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Mock setup failed: " + e.getMessage());
      }

      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
      mockedProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);
      mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockClient);

      try {
        AsyncProcessUtil.getList(TestUtils.TEST_URI);
        fail("Should throw OBException");
      } catch (OBException e) {
        assertTrue("Exception cause should be IOException",
            e.getCause() instanceof IOException);
      }
    }
  }

  /**
   * Tests the behavior of getList when an InterruptedException occurs.
   */
  @Test
  public void testGetListWithInterruptedException() {
    try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class);
         MockedStatic<OBPropertiesProvider> mockedProvider = mockStatic(OBPropertiesProvider.class)) {

      HttpClient mockClient = mock(HttpClient.class);
      try {
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new InterruptedException("Test Interrupted Exception"));
      } catch (IOException e) {
        fail("Mock setup failed: " + e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
      mockedProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);
      mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockClient);

      try {
        AsyncProcessUtil.getList(TestUtils.TEST_URI);
        fail("Should throw OBException");
      } catch (OBException e) {
        assertTrue("Exception cause should be InterruptedException",
            e.getCause() instanceof InterruptedException);
        Thread.interrupted();
      }
    }
  }

  /**
   * Tests the formatting of dates using fmtDate.
   *
   * @throws ParseException if the input date format is invalid
   */
  @Test
  public void testFmtDate() throws ParseException {
    String inputDate = "25-12-2023 10:30:45:123";
    String expectedDate = "2023-12-25 10:30:45:123";

    String result = AsyncProcessUtil.fmtDate(inputDate);

    assertEquals(expectedDate, result);
  }

  /**
   * Tests the fmtDate method with an invalid date format, expecting a ParseException.
   *
   * @throws ParseException always thrown due to invalid input format
   */
  @Test(expected = ParseException.class)
  public void testFmtDateWithInvalidFormat() throws ParseException {
    AsyncProcessUtil.fmtDate("invalid-date-format");
  }
}