package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.entity.ContentType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

/**
 * Unit tests for the DataSourceServlet class.
 */
public class DataSourceServletTest extends WeldBaseTest {

  public static final String DATASOURCE_USERS = "/datasource/users";
  private DataSourceServlet dataSourceServlet;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private PrintWriter mockWriter;

  @Mock
  private HttpSession mockSession;

  @Mock
  private Tab mockTab;

  /**
   * Rule for handling expected exceptions in JUnit tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Table mockTable;

  @Mock
  private Window mockWindow;

  @Mock
  private org.openbravo.service.datasource.DataSourceServlet dataSourceMockInternal;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    dataSourceServlet = new DataSourceServlet();
    when(mockRequest.getSession(false)).thenReturn(mockSession);
  }

  /**
   * Tests the extractDataSourceAndID method with a valid URI containing an ID.
   *
   * @throws IllegalArgumentException
   *     if the URI is invalid
   */
  @Test(expected = IllegalArgumentException.class)
  public void testExtractDataSourceAndID_ValidURIWithID() {
    DataSourceServlet.extractDataSourceAndID("/datasource/users/123");
  }

  /**
   * Tests the extractDataSourceAndID method with a valid URI without an ID.
   */
  @Test
  public void testExtractDataSourceAndID_ValidURIWithoutID() {
    String[] result = DataSourceServlet.extractDataSourceAndID(DATASOURCE_USERS);
    assertEquals(2, result.length);
    assertEquals("datasource", result[0]);
    assertEquals("users", result[1]);
  }

  /**
   * Tests the extractDataSourceAndID method with an invalid URI.
   *
   * @throws IllegalArgumentException
   *     if the URI is invalid
   */
  @Test(expected = IllegalArgumentException.class)
  public void testExtractDataSourceAndID_InvalidURI() {
    DataSourceServlet.extractDataSourceAndID("/datasource/users/123/extra");
  }

  /**
   * Tests the normalizedName method with a standard name.
   */
  @Test
  public void testNormalizedName_StandardName() {
    String result = DataSourceServlet.normalizedName("Business Partner");
    assertEquals("businessPartner", result);
  }

  /**
   * Tests the normalizedName method with a name containing special characters.
   */
  @Test
  public void testNormalizedName_SpecialCharacters() {
    String result = DataSourceServlet.normalizedName("Business! Partner@");
    assertEquals("businessPartner", result);
  }

  /**
   * Tests the normalizedName method with the name "AD_Role_ID".
   */
  @Test
  public void testNormalizedName_ADRoleID() {
    String result = DataSourceServlet.normalizedName("AD_Role_ID");
    assertEquals("role", result);
  }

  /**
   * Tests the normalizedName method with an empty name.
   */
  @Test
  public void testNormalizedName_EmptyName() {
    String result = DataSourceServlet.normalizedName("");
    assertEquals("", result);
  }

  /**
   * Tests the doDelete method, which is not supported.
   *
   * @throws UnsupportedOperationException
   *     if the method is called
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testDoDelete_NotSupported() throws Exception {
    dataSourceServlet.doDelete(DATASOURCE_USERS, mockRequest, mockResponse);
  }

  /**
   * Tests the convertURI method with invalid parts.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test(expected = Exception.class)
  public void testConvertURI_InvalidParts() throws OpenAPINotFoundThrowable {
    String[] parts = { };
    dataSourceServlet.convertURI(parts);
  }

  /**
   * Tests the doGet method with a valid request.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void doGet_ValidRequest_ReturnsExpectedResponse() throws Exception {
    when(mockRequest.getParameter("q")).thenReturn("name==John");
    when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(Collections.singleton("q")));
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockResponse.getWriter()).thenReturn(mockWriter);
    doNothing().when(mockWriter).write(anyString());
    doNothing().when(mockWriter).flush();

    try (MockedStatic<DataSourceServlet> dataSourceServletMock = Mockito.mockStatic(DataSourceServlet.class)) {
      dataSourceServletMock.when(() -> DataSourceServlet.extractDataSourceAndID(anyString())).thenReturn(
          new String[]{ "datasource", "users" });
      dataSourceServletMock.when(() -> DataSourceServlet.getTabByDataSourceName(any())).thenReturn(mockTab);
      dataSourceServletMock.when(DataSourceServlet::getDataSourceServlet).thenReturn(dataSourceMockInternal);

      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        HttpServletResponse newResponse = (HttpServletResponse) args[1];
        JSONObject responseForMock = new JSONObject();

        JSONArray dataArray = new JSONArray();
        JSONObject dataItem = new JSONObject();
        dataItem.put("id", 1);
        dataItem.put("name", "Sample Data");
        dataArray.put(dataItem);
        JSONObject responseObject = new JSONObject();
        responseObject.put("data", dataArray);
        responseForMock.put("response", responseObject);
        newResponse.getWriter().write(responseForMock.toString());

        return null;
      }).when(dataSourceMockInternal).doPost(any(), any());

      when(mockTab.getTable()).thenReturn(mockTable);
      when(mockTable.getId()).thenReturn("123222");
      when(mockTable.getName()).thenReturn("m_product");
      when(mockTab.getWindow()).thenReturn(mockWindow);
      when(mockWindow.getId()).thenReturn("1233323");

      dataSourceServlet.doGet(DATASOURCE_USERS, mockRequest, mockResponse);
    }

    verify(mockResponse).setContentType(ContentType.APPLICATION_JSON.getMimeType());
    verify(mockResponse).setCharacterEncoding("UTF-8");
    verify(mockWriter).write(anyString());
  }

  /**
   * Tests the doGet method with an invalid URI.
   *
   * @throws IllegalArgumentException
   *     if the URI is invalid
   */
  @Test
  public void doGet_InvalidURI_ThrowsIllegalArgumentException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    dataSourceServlet.doGet("/datasource/users/123/extra", mockRequest, mockResponse);
  }


  @Test
  public void createPayLoadSuccess() throws IOException, JSONException {

    // Given
    String jsonInput = "{\"field1\":\"value1\"}";
    InputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes());
    ServletInputStream servletInputStream = new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        // The method is empty because is only for tests.
      }

      @Override
      public int read() throws IOException {
        return inputStream.read();
      }
    };
    when(mockRequest.getInputStream()).thenReturn(servletInputStream);
    when(mockRequest.getParameter("componentId")).thenReturn("isc_OBViewForm_0");
    when(mockRequest.getParameter("dataSource")).thenReturn("isc_OBViewDataSource_0");
    when(mockRequest.getParameter("operationType")).thenReturn("add");
    when(mockRequest.getParameter("csrfToken")).thenReturn("123");


    // When
    JSONObject result = new DataSourceServlet().createPayLoad(mockRequest);

    // Then
    assertEquals("isc_OBViewDataSource_0", result.getString("dataSource"));
    assertEquals("add", result.getString("operationType"));
    assertEquals("isc_OBViewForm_0", result.getString("componentId"));
    assertEquals("123", result.getString("csrfToken"));
    JSONObject data = result.getJSONObject(DataSourceConstants.DATA);
    assertEquals("value1", data.getString("field1"));
  }
}
