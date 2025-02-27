package com.etendoerp.etendorx.services;

import java.math.BigDecimal;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
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

/**
 * Unit tests for the DataSourceServlet class.
 */
public class DataSourceServletCalloutTest extends WeldBaseTest {

  public static final String PRODUCT_WATER_ID = "BDE2F1CF46B54EF58D33E20A230DA8D2";
  public static final String BP_ALSUPER_ID = "A6750F0D15334FB890C254369AC750A8";
  public static final String BP_LOCATION_ALSUPER_ID = "6518D3040ED54008A1FC0C09ED140D66";
  private static final Logger log = LoggerFactory.getLogger(DataSourceServletCalloutTest.class);
  private AutoCloseable mocks;


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
    //TEST CASE 1: Create a sales order
    // Given
    ImageUploadServlet servlet = new ImageUploadServlet();
    @NotNull MockedResponse responsePack = TestUtils.getResponseMocked();


    HttpServletRequest request = TestUtils.setupRequestMocked(new JSONObject().put("businessPartner", BP_ALSUPER_ID));

    // When
    new DataSourceServlet().doPost("/TestSalesOrderHeader", request, responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    JSONObject responseString = new JSONObject(responsePack.getResponseContent());

    assert responseString.has("response") && responseString.getJSONObject("response").has(
        "data") && responseString.getJSONObject("response").getJSONArray("data").length() > 0;
    var headerCreatedJSon = responseString.getJSONObject("response").getJSONArray("data").getJSONObject(0);
    var salesOrderOB = OBDal.getInstance().get(Order.class, headerCreatedJSon.getString("id"));
    assert salesOrderOB != null;
    elementsToClean.add(salesOrderOB); //only add the header to clean up, because the lines are automatically deleted
    assert StringUtils.equalsIgnoreCase(salesOrderOB.getPartnerAddress().getId(), BP_LOCATION_ALSUPER_ID);

    //TEST CASE 2: Create a sales order line
    // Given a request of creation of a sales order line
    responsePack = TestUtils.getResponseMocked();
    JSONObject body = new JSONObject().put("salesOrder", salesOrderOB.getId()).put("product", PRODUCT_WATER_ID).put(
        "orderedQuantity", 1);

    request = TestUtils.setupRequestMocked(body);
    // When
    new DataSourceServlet().doPost("/TestSalesOrderLine", request, responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    responseString = new JSONObject(responsePack.getResponseContent());
    assert responseString.has("response") && responseString.getJSONObject("response").has(
        "data") && responseString.getJSONObject("response").getJSONArray("data").length() > 0;
    var lineCreatedJSon = responseString.getJSONObject("response").getJSONArray("data").getJSONObject(0);
    var salesOrderLineOB = OBDal.getInstance().get(OrderLine.class, lineCreatedJSon.getString("id"));
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

    request = TestUtils.setupRequestMocked(body);
    // When
    new DataSourceServlet().doPut("/TestSalesOrderLine/" + salesOrderLineOB.getId(), request,
        responsePack.getResponse());

    // Then
    responsePack.flushResponse();
    responseString = new JSONObject(responsePack.getResponseContent());
    assert responseString.has("response") && responseString.getJSONObject("response").has(
        "data") && responseString.getJSONObject("response").getJSONArray("data").length() > 0;
    lineCreatedJSon = responseString.getJSONObject("response").getJSONArray("data").getJSONObject(0);
    salesOrderLineOB = OBDal.getInstance().get(OrderLine.class, lineCreatedJSon.getString("id"));
    assert salesOrderLineOB != null;

    OBDal.getInstance().refresh(salesOrderOB);
    //the Order must have a grand total greater than 0 and greater than the previous total
    assert totalAmountWithOneUnit.compareTo(salesOrderOB.getGrandTotalAmount()) < 0;


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