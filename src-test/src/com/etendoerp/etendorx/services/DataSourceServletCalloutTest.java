package com.etendoerp.etendorx.services;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.test.base.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.TestUtils;
import com.etendoerp.etendorx.utils.MockedResponse;
import com.etendoerp.openapi.OpenAPIController;

/**
 * Unit tests for the DataSourceServlet class.
 */
public class DataSourceServletCalloutTest extends WeldBaseTest {

  public static final String PRODUCT_WATER_ID = "BDE2F1CF46B54EF58D33E20A230DA8D2";
  public static final String BP_ALSUPER_ID = "A6750F0D15334FB890C254369AC750A8";
  public static final String BP_LOCATION_ALSUPER_ID = "6518D3040ED54008A1FC0C09ED140D66";
  private static final Logger log = LoggerFactory.getLogger(DataSourceServletCalloutTest.class);
  public static final String RESPONSE = "response";
  public static final String DATA = "data";
  public static final String ID = "id";
  private AutoCloseable mocks;
  private Document formatXMLDocument;

  private List<BaseOBObject> elementsToClean;

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

    elementsToClean = new ArrayList<>();
    elementsToClean = TestUtils.buildExampleHeadlessFlow();
    OBDal.getInstance().flush();

    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());
    vars.setSessionValue("#User_Client", OBContext.getOBContext().getCurrentClient().getId());
    RequestContext.get().setVariableSecureApp(vars);
    // Convertir XML String a Document
    SAXReader reader = new SAXReader();
    formatXMLDocument = reader.read(new StringReader(TestUtils.FORMATS_XML));
    OBPropertiesProvider.setInstance(new OBPropertiesProvider());
  }

  /**
   * Tests the flowSalesOrder method.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void flowSalesOrder() throws Exception {
    //TEST CASE 1: Create a sales order
    // Given
    @NotNull MockedResponse responsePack = TestUtils.getResponseMocked();


    JSONObject jsonToSend = new JSONObject().put("businessPartner", BP_ALSUPER_ID);
    HttpServletRequest request = TestUtils.setupRequestMocked(jsonToSend,
        formatXMLDocument);

    // When
    new DataSourceServlet().doPost("/TestSalesOrderHeader", request, responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    JSONObject responseString = new JSONObject(responsePack.getResponseContent());
    log.info("Post to TestSalesOrderHeader:" + jsonToSend.toString(2));
    log.info("Response: " + responseString.toString(2));
    assert responseString.has(RESPONSE) && responseString.getJSONObject(RESPONSE).has(
        DATA) && responseString.getJSONObject(RESPONSE).getJSONArray(DATA).length() > 0;
    var headerCreatedJSon = responseString.getJSONObject(RESPONSE).getJSONArray(DATA).getJSONObject(0);
    var salesOrderOB = OBDal.getInstance().get(Order.class, headerCreatedJSon.getString(ID));
    assert salesOrderOB != null;
    elementsToClean.add(salesOrderOB); //only add the header to clean up, because the lines are automatically deleted
    assert StringUtils.equalsIgnoreCase(salesOrderOB.getPartnerAddress().getId(), BP_LOCATION_ALSUPER_ID);

    //TEST CASE 2: Create a sales order line
    // Given a request of creation of a sales order line
    responsePack = TestUtils.getResponseMocked();
    JSONObject body = new JSONObject().put("salesOrder", salesOrderOB.getId()).put("product", PRODUCT_WATER_ID).put(
        "orderedQuantity", 1);

    request = TestUtils.setupRequestMocked(body, formatXMLDocument);
    // When
    new DataSourceServlet().doPost("/TestSalesOrderLine", request, responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    responseString = new JSONObject(responsePack.getResponseContent());
    log.info("Post to TestSalesOrderLine:" + body.toString(2));
    log.info("Response: " + responseString.toString(2));
    assert responseString.has(RESPONSE) && responseString.getJSONObject(RESPONSE).has(
        DATA) && responseString.getJSONObject(RESPONSE).getJSONArray(DATA).length() > 0;
    var lineCreatedJSon = responseString.getJSONObject(RESPONSE).getJSONArray(DATA).getJSONObject(0);
    var salesOrderLineOB = OBDal.getInstance().get(OrderLine.class, lineCreatedJSon.getString(ID));
    assert salesOrderLineOB != null;
    assert StringUtils.equalsIgnoreCase(salesOrderLineOB.getSalesOrder().getId(), salesOrderOB.getId());

    OBDal.getInstance().refresh(salesOrderOB);
    //The prices must be greater than 0, because must be automatically calculated
    assert BigDecimal.ZERO.compareTo(salesOrderLineOB.getUnitPrice()) < 0;
    //the Order must have a grand total greater than 0
    assert BigDecimal.ZERO.compareTo(salesOrderOB.getGrandTotalAmount()) < 0;
    BigDecimal totalAmountWithOneUnit = salesOrderOB.getGrandTotalAmount();

    // Test case 3: Update the sales order line
    // Given a request of update of a sales order line
    responsePack = TestUtils.getResponseMocked();
    body = new JSONObject().put("orderedQuantity", 2);

    request = TestUtils.setupRequestMocked(body, formatXMLDocument);
    // When
    new DataSourceServlet().doPut("/TestSalesOrderLine/" + salesOrderLineOB.getId(), request,
        responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    responseString = new JSONObject(responsePack.getResponseContent());
    log.info("Put to TestSalesOrderLine:" + body.toString(2));
    log.info("Response: " + responseString.toString(2));
    assert responseString.has(RESPONSE) && responseString.getJSONObject(RESPONSE).has(
        DATA) && responseString.getJSONObject(RESPONSE).getJSONArray(DATA).length() > 0;
    lineCreatedJSon = responseString.getJSONObject(RESPONSE).getJSONArray(DATA).getJSONObject(0);
    salesOrderLineOB = OBDal.getInstance().get(OrderLine.class, lineCreatedJSon.getString(ID));
    assert salesOrderLineOB != null;

    OBDal.getInstance().refresh(salesOrderOB);
    //the Order must have a grand total greater than 0 and greater than the previous total
    assert totalAmountWithOneUnit.compareTo(salesOrderOB.getGrandTotalAmount()) < 0;


  }

  //Another test
  @Test
  public void generateOpenAPISpec() throws Exception {
    // Given the flow created in setup

    // When
    String openAPISpec = new OpenAPIController().getOpenAPIJson("localhost", "TESTFLOW",
        "http://localhost:8080/etendo");
    // Then
    assert StringUtils.isNotEmpty(openAPISpec);
    assert StringUtils.containsIgnoreCase(openAPISpec, "TestSalesOrderHeader");
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