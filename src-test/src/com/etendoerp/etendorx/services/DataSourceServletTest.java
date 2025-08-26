package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;

import com.etendoerp.etendorx.utils.DataSourceUtils;
import com.etendoerp.openapi.data.OpenAPIRequest;

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
    DataSourceUtils.extractDataSourceAndID("/datasource/users/123");
  }

  /**
   * Tests the extractDataSourceAndID method with a valid URI without an ID.
   */
  @Test
  public void testExtractDataSourceAndID_ValidURIWithoutID() {
    String[] result = DataSourceUtils.extractDataSourceAndID(DATASOURCE_USERS);
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
    DataSourceUtils.extractDataSourceAndID("/datasource/users/123/extra");
  }

  /**
   * Tests the normalizedName method with a standard name.
   */
  @Test
  public void testNormalizedName_StandardName() {
    Field field = OBDal.getInstance().get(Field.class, "1573");
    String result = DataSourceUtils.getHQLColumnName(field)[0];
    assertEquals("businessPartner", result);
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

  @Test
  public void testGetTab_IgnoresFieldsWithNullColumnAndOrdersBySeqNo() throws Throwable {
    // Prepare mocks for OBDal and criteria
    var mockOBDal = org.mockito.Mockito.mock(org.openbravo.dal.service.OBDal.class);
    var mockCriteria = org.mockito.Mockito.mock(org.openbravo.dal.service.OBCriteria.class);
    var mockRequestEntity = org.mockito.Mockito.mock(OpenAPIRequest.class);
    var mockOpenAPITab = org.mockito.Mockito.mock(com.etendoerp.etendorx.data.OpenAPITab.class);
    var mockTabLocal = org.mockito.Mockito.mock(org.openbravo.model.ad.ui.Tab.class);

    // Create Field/Column mocks
    org.openbravo.model.ad.ui.Field fieldWithCol1 = org.mockito.Mockito.mock(org.openbravo.model.ad.ui.Field.class);
    org.openbravo.model.ad.datamodel.Column col1 = org.mockito.Mockito.mock(
        org.openbravo.model.ad.datamodel.Column.class);
    org.openbravo.model.ad.ui.Field fieldWithNullCol = org.mockito.Mockito.mock(org.openbravo.model.ad.ui.Field.class);
    org.openbravo.model.ad.ui.Field fieldWithCol2 = org.mockito.Mockito.mock(org.openbravo.model.ad.ui.Field.class);
    org.openbravo.model.ad.datamodel.Column col2 = org.mockito.Mockito.mock(
        org.openbravo.model.ad.datamodel.Column.class);

    // Wire columns
    org.mockito.Mockito.when(fieldWithCol1.getColumn()).thenReturn(col1);
    org.mockito.Mockito.when(fieldWithNullCol.getColumn()).thenReturn(null);
    org.mockito.Mockito.when(fieldWithCol2.getColumn()).thenReturn(col2);

    // Prepare OpenAPIRequestField mocks used by getSeqNo
    com.etendoerp.etendorx.data.OpenAPIRequestField apif1 = org.mockito.Mockito.mock(
        com.etendoerp.etendorx.data.OpenAPIRequestField.class);
    com.etendoerp.etendorx.data.OpenAPIRequestField apif2 = org.mockito.Mockito.mock(
        com.etendoerp.etendorx.data.OpenAPIRequestField.class);
    // apif1 corresponds to col1 with seqNo 2, apif2 to col2 with seqNo 1 -> expect ordering by seqNo
    org.mockito.Mockito.when(apif1.getField()).thenReturn(fieldWithCol1);
    org.mockito.Mockito.when(apif1.getSeqno()).thenReturn(2L);
    org.mockito.Mockito.when(apif2.getField()).thenReturn(fieldWithCol2);
    org.mockito.Mockito.when(apif2.getSeqno()).thenReturn(1L);

    // Tab fields include the three fields (one null column)
    java.util.List<org.openbravo.model.ad.ui.Field> adFields = java.util.List.of(fieldWithCol1, fieldWithNullCol,
        fieldWithCol2);
    org.mockito.Mockito.when(mockTabLocal.getADFieldList()).thenReturn(adFields);

    // OpenAPIRequest tab wiring
    // Ensure the OpenAPIRequest tab returns the local mockTab with our ADFieldList
    org.mockito.Mockito.when(mockOpenAPITab.getRelatedTabs()).thenReturn(mockTabLocal);
    org.mockito.Mockito.when(mockRequestEntity.getETRXOpenAPITabList()).thenReturn(java.util.List.of(mockOpenAPITab));
    org.mockito.Mockito.when(mockOpenAPITab.getEtrxOpenapiFieldList()).thenReturn(java.util.List.of(apif1, apif2));

    // Mock static OBDal.getInstance() to return our mockOBDal and criteria behavior
    try (var obDalStatic = org.mockito.Mockito.mockStatic(org.openbravo.dal.service.OBDal.class)) {
      // Mock both no-arg and tenant-keyed getInstance calls used by Openbravo utilities
      obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
      obDalStatic.when(
          () -> org.openbravo.dal.service.OBDal.getInstance(org.mockito.ArgumentMatchers.anyString())).thenReturn(
          mockOBDal);
      org.mockito.Mockito.when(mockOBDal.createCriteria(OpenAPIRequest.class)).thenReturn(mockCriteria);
      org.mockito.Mockito.when(mockCriteria.add(org.mockito.ArgumentMatchers.any())).thenReturn(mockCriteria);
      org.mockito.Mockito.when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
      org.mockito.Mockito.when(mockCriteria.uniqueResult()).thenReturn(mockRequestEntity);

      // Mock LoginUtils to avoid DB connections during default login retrieval and session filling
      try (var loginUtilsStatic = org.mockito.Mockito.mockStatic(org.openbravo.base.secureApp.LoginUtils.class)) {
        // Create a simple RoleDefaults holder using the inner class from LoginUtils
        org.openbravo.base.secureApp.LoginUtils.RoleDefaults defaults = new org.openbravo.base.secureApp.LoginUtils.RoleDefaults();
        defaults.role = "-";
        defaults.client = "-";
        defaults.warehouse = "-";
        loginUtilsStatic.when(
            () -> org.openbravo.base.secureApp.LoginUtils.getLoginDefaults(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(DalConnectionProvider.class))).thenReturn(
            defaults);
        loginUtilsStatic.when(
            () -> org.openbravo.base.secureApp.LoginUtils.fillSessionArguments(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()
            )).thenAnswer(inv -> null);

        // Mock DataSourceUtils.getHQLColumnName to return stable names
        try (var dsUtilsStatic = org.mockito.Mockito.mockStatic(com.etendoerp.etendorx.utils.DataSourceUtils.class)) {
          dsUtilsStatic.when(() -> com.etendoerp.etendorx.utils.DataSourceUtils.getHQLColumnName(
              org.mockito.ArgumentMatchers.eq(fieldWithCol1))).thenReturn(new String[]{ "col1" });
          dsUtilsStatic.when(() -> com.etendoerp.etendorx.utils.DataSourceUtils.getHQLColumnName(
              org.mockito.ArgumentMatchers.eq(fieldWithCol2))).thenReturn(new String[]{ "col2" });
          // Also stub extractDataSourceAndID because we are mocking the whole static class
          dsUtilsStatic.when(() -> com.etendoerp.etendorx.utils.DataSourceUtils.extractDataSourceAndID(org.mockito.ArgumentMatchers.eq(DATASOURCE_USERS))).thenReturn(new String[]{ "datasource", "users" });

          // Invoke private static method getTab via MethodHandles lookup (avoids setAccessible)
          java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(
              DataSourceServlet.class, java.lang.invoke.MethodHandles.lookup());
          java.lang.invoke.MethodHandle mh = lookup.findStatic(DataSourceServlet.class, "getTab",
              java.lang.invoke.MethodType.methodType(org.openbravo.model.ad.ui.Tab.class, String.class,
                  javax.servlet.http.HttpServletRequest.class, javax.servlet.http.HttpServletResponse.class,
                  java.util.List.class));
          java.util.List<com.etendoerp.etendorx.services.wrapper.RequestField> fieldList = new java.util.ArrayList<>();
          Object tabResult = mh.invoke(DATASOURCE_USERS, mockRequest, mockResponse, fieldList);

          // Assertions: fieldList should contain only fields with non-null columns (2 entries)
          org.junit.Assert.assertEquals(2, fieldList.size());
          // After ordering by seqNo, first should be the one with seqNo = 1 (col2), then seqNo = 2 (col1)
          org.junit.Assert.assertEquals("col2", fieldList.get(0).getName());
          org.junit.Assert.assertEquals("col1", fieldList.get(1).getName());
          // Tab result should be the mockTab
          org.junit.Assert.assertSame(mockTabLocal, tabResult);
        }
      }
    }
  }
}
