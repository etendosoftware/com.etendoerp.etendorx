package com.etendoerp.etendorx.services;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.HttpSessionWrapper;
import org.openbravo.base.model.Entity;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.base.mock.OBServletContextMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.TestUtils;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Unit tests for the DataSourceServlet class.
 */
public class DataSourceServletCalloutTest extends WeldBaseTest {

  public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
  public static final String IMAGE_ID = "imageId";
  private static final Logger log = LoggerFactory.getLogger(DataSourceServletCalloutTest.class);
  private AutoCloseable mocks;

  @Mock
  private Organization mockOrg;

  @Mock
  private Entity mockEntity;

  private ArrayList<BaseOBObject> elementsToClean;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    super.setUp();

    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.SYS_ADMIN, TestConstants.Clients.SYSTEM,
        TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);

    elementsToClean = new ArrayList<BaseOBObject>();
    elementsToClean = TestUtils.buildExampleHeadlessFlow();
    OBDal.getInstance().flush();

    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());
    vars.setSessionValue("#User_Client", OBContext.getOBContext().getCurrentClient().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  /**
   * Tests the flowSalesOrder method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void flowSalesOrder() throws Exception {
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String jsonPayload = new JSONObject().put("businessPartner", "A6750F0D15334FB890C254369AC750A8").toString();

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jsonPayload.getBytes(StandardCharsets.UTF_8));

    // Crear un ServletInputStream que funcione correctamente
    ServletInputStream servletInputStream = new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }

      @Override
      public int read() throws IOException {
        return byteArrayInputStream.read();
      }
    };

    // Configurar el mock para devolver el ServletInputStream correcto
    when(request.getInputStream()).thenReturn(servletInputStream);


    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
    HttpSession httpSession = new HttpSessionWrapper();
    when(request.getSession(anyBoolean())).thenReturn(httpSession);
    when(request.getSession()).thenReturn(httpSession);
    DataSourceServlet dataSourceServlet = new DataSourceServlet();
    RequestContext.get().setRequest(request);
    request.getSession().setAttribute("#User_Client".toUpperCase(),
        String.format("'%s'", OBContext.getOBContext().getCurrentClient().getId()));
    request.getSession().setAttribute("#User_Org".toUpperCase(),
        String.format("'%s'", OBContext.getOBContext().getCurrentOrganization().getId()));

    request.getSession().setAttribute("#FormatOutput|euroEdition".toUpperCase(), "#0.00");
    request.getSession().setAttribute("#DecimalSeparator|euroEdition".toUpperCase(), ".");
    request.getSession().setAttribute("#GroupSeparator|euroEdition".toUpperCase(), ",");


    VariablesSecureApp varsN = new VariablesSecureApp(request);
    String cli = Utility.getContext(new DalConnectionProvider(false), varsN, "#User_Client", "143", 1);

    
    RequestContext.get().setVariableSecureApp(varsN);
    RequestContext.setServletContext(new OBServletContextMock());
    SecureWebServicesUtils.fillSessionVariables(request);

    // When
    dataSourceServlet.doPost("/TestSalesOrderHeader", request, response);

    // Then
    printWriter.flush();
    JSONObject responseString = new JSONObject(stringWriter.toString());

    assert responseString.has("response") && responseString.getJSONObject("response").has("data") && responseString
        .getJSONObject("response").getJSONArray("data").length() > 0;
    var headerCreatedJSon = responseString.getJSONObject("response").getJSONArray("data").getJSONObject(0);
    var salesOrderOB = OBDal.getInstance().get(Order.class, headerCreatedJSon.getString("id"));
    assert salesOrderOB != null;
    elementsToClean.add(salesOrderOB);
    assert org.apache.commons.lang.StringUtils.equalsIgnoreCase(salesOrderOB.getPartnerAddress().getId(),
        "6518D3040ED54008A1FC0C09ED140D66");
    
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.SYS_ADMIN, TestConstants.Clients.SYSTEM,
        TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
    for (BaseOBObject element : elementsToClean) {
      log.info("Removing element: " + element.getEntityName() + " with id: " + element.getId());
      OBDal.getInstance().remove(element);
    }
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }
}