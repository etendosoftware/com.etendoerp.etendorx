package com.etendoerp.etendorx.services;

import static com.etendoerp.etendorx.services.DataSourceServlet.getTabByDataSourceName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.entity.ContentType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

public class DataSourceServletTest extends WeldBaseTest {

  private DataSourceServlet dataSourceServlet;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private
  PrintWriter mockWriter;

  @Mock
  private HttpSession mockSession;

  @Mock
  private Tab mockTab;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Table mockTable;

  @Mock
  private Window mockWindow;

  @Mock
  private org.openbravo.service.datasource.DataSourceServlet dataSourceMockInternal;


  @Before
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    dataSourceServlet = new DataSourceServlet();
    when(mockRequest.getSession(false)).thenReturn(mockSession);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtractDataSourceAndID_ValidURIWithID() {
    DataSourceServlet.extractDataSourceAndID("/datasource/users/123");
  }

  @Test
  public void testExtractDataSourceAndID_ValidURIWithoutID() {
    String[] result = DataSourceServlet.extractDataSourceAndID("/datasource/users");
    assertEquals(2, result.length);
    assertEquals("datasource", result[0]);
    assertEquals("users", result[1]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtractDataSourceAndID_InvalidURI() {
    DataSourceServlet.extractDataSourceAndID("/datasource/users/123/extra");
  }

  @Test
  public void testNormalizedName_StandardName() {
    String result = DataSourceServlet.normalizedName("Business Partner");
    assertEquals("businessPartner", result);
  }

  @Test
  public void testNormalizedName_SpecialCharacters() {
    String result = DataSourceServlet.normalizedName("Business! Partner@");
    assertEquals("businessPartner", result);
  }

  @Test
  public void testNormalizedName_ADRoleID() {
    String result = DataSourceServlet.normalizedName("AD_Role_ID");
    assertEquals("role", result);
  }

  @Test
  public void testNormalizedName_EmptyName() {
    String result = DataSourceServlet.normalizedName("");
    assertEquals("", result);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDoDelete_NotSupported() throws Exception {
    dataSourceServlet.doDelete("/datasource/users", mockRequest, mockResponse);
  }


  @Test(expected = Exception.class)
  public void testConvertURI_InvalidParts() throws OpenAPINotFoundThrowable {
    String[] parts = { };
    dataSourceServlet.convertURI(parts);

  }

  @Test
  public void doGet_ValidRequest_ReturnsExpectedResponse() throws Exception {
    when(mockRequest.getParameter("q")).thenReturn("name==John");
    when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(Collections.singleton("q")));
    when(mockRequest.getSession(false)).thenReturn(mockSession);
    when(mockResponse.getWriter()).thenReturn(mockWriter);
    doNothing().when(mockWriter).write(anyString());
    doNothing().when(mockWriter).flush();

    try (MockedStatic<DataSourceServlet> dataSourceServletMock = Mockito.mockStatic(DataSourceServlet.class)

    ) {
      dataSourceServletMock.when(() -> DataSourceServlet.extractDataSourceAndID(anyString())).thenReturn(
          new String[]{ "datasource", "users" });
      dataSourceServletMock.when(() -> DataSourceServlet.getTabByDataSourceName(any())).thenReturn(mockTab);
      dataSourceServletMock.when(
          DataSourceServlet::getDataSourceServlet
      ).thenReturn(dataSourceMockInternal);

      doAnswer(
          invocation -> {
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

      dataSourceServlet.doGet("/datasource/users", mockRequest, mockResponse);
    }


    verify(mockResponse).setContentType(ContentType.APPLICATION_JSON.getMimeType());
    verify(mockResponse).setCharacterEncoding("UTF-8");
    verify(mockWriter).write(anyString());
  }

  @Test
  public void doGet_InvalidURI_ThrowsIllegalArgumentException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    dataSourceServlet.doGet("/datasource/users/123/extra", mockRequest, mockResponse);
  }

}