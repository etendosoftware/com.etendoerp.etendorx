package com.etendoerp.etendorx;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.openbravo.base.HttpSessionWrapper;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.test.base.mock.OBServletContextMock;

import com.etendoerp.etendorx.data.OpenAPITab;
import com.etendoerp.etendorx.utils.MockedResponse;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.etendoerp.openapi.data.OpenApiFlow;
import com.etendoerp.openapi.data.OpenApiFlowPoint;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

/**
 * Utility class containing constant values used for testing purposes across the etendorx application.
 * This class provides standardized test data, configuration values, and common strings
 * used in test scenarios.
 */
public class TestUtils {

  public static final String FORMATS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<!--\n" +
      " *************************************************************************\n" +
      " * The contents of this file are subject to the Openbravo Public License \n" +
      " * Version 1.1 (the \"License\"), being the Mozilla Public License \n" +
      " * version 1.1  with a permitted attribution clause ; you may not use \n" +
      " * this file except in compliance with the License. \n" +
      " * You may obtain a copy of the License at  \n" +
      " * http://www.openbravo.com/legal/license.txt \n" +
      " * Software distributed under the License is distributed on an \n" +
      " * \"AS IS\" basis, WITHOUT WARRANTY OF  ANY KIND, either express or \n" +
      " * implied. See the License for the specific language governing rights \n" +
      " * and  limitations under the License. \n" +
      " * The Original Code is Openbravo ERP. \n" +
      " * The Initial Developer of the Original Code is Openbravo SLU \n" +
      " * All portions are Copyright (C) 2005-2006 Openbravo SLU \n" +
      " * All Rights Reserved. \n" +
      " * Contributor(s): Openbravo S.L.U.\n" +
      " ************************************************************************\n" +
      "--><!--<!DOCTYPE FormatClass SYSTEM \"FormatClass.dtd\">--><Formats>\n" +
      "   <Number name=\"euroInform\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"euroRelation\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"euroEdition\" decimal=\".\" grouping=\",\" formatOutput=\"#0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"euroExcel\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.##\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"priceInform\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"priceRelation\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"priceEdition\" decimal=\".\" grouping=\",\" formatOutput=\"#0.00\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"integerInform\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0\" formatInternal=\"#0\"/>\n" +
      "   <Number name=\"integerRelation\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0\" formatInternal=\"#0\"/>\n" +
      "   <Number name=\"integerEdition\" decimal=\".\" grouping=\",\" formatOutput=\"#0\" formatInternal=\"#0\"/>\n" +
      "   <Number name=\"integerExcel\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0\" formatInternal=\"#0\"/>\n" +
      "   <Number name=\"priceExcel\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.##\" formatInternal=\"#0.00\"/>\n" +
      "   <Number name=\"qtyRelation\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.###\" formatInternal=\"#0.000\"/>\n" +
      "   <Number name=\"qtyEdition\" decimal=\".\" grouping=\",\" formatOutput=\"#0.###\" formatInternal=\"#0.000\"/>\n" +
      "   <Number name=\"qtyExcel\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.###\" formatInternal=\"#0.000\"/>\n" +
      "   <Number name=\"generalQtyRelation\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.######\" formatInternal=\"#0.000000\"/>\n" +
      "   <Number name=\"generalQtyEdition\" decimal=\".\" grouping=\",\" formatOutput=\"#0.######\" formatInternal=\"#0.000000\"/>\n" +
      "   <Number name=\"generalQtyExcel\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.######\" formatInternal=\"#0.000000\"/>\n" +
      "   <Number name=\"amountInform\" decimal=\".\" grouping=\",\" formatOutput=\"#,##0.00\" formatInternal=\"#0.00\"/>\n" +
      "</Formats>";

  // Private constructor to prevent instantiation
  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static final String JSON_KEY_MESSAGE = "message";
  public static final String JSON_KEY_SEVERITY = "severity";
  public static final String JSON_KEY_TITLE = "title";
  public static final String ERROR_SEVERITY = "error";
  public static final String ERROR_TITLE = "ERROR";
  public static final String APPLICATION_JSON = "application/json";
  public static final String UTF_8 = "utf-8";
  public static final String DEFAULT_CONFIG_PATH = "/etendorx/config";
  public static final String TEST_TABLE = "TestTable";
  public static final String VALID_PROJECTION_NAME = "Valid";
  public static final String TOO_SHORT_NAME = "ab";
  public static final String INVALID_PROJECTION_MESSAGE = "JavaMappingsEventHandler.invalidQualifier";
  public static final String VALID_QUALIFIER = "ValidQualifier";
  public static final String TEST_MAPPING = "TestMapping";
  public static final String OTHER = "Other";
  public static final String VALID_ENTITY = "ValidEntity";
  public static final String INVALID_ENTITY = "InvalidEntity";
  public static final String TEST_PROJECTION_ID = "testProjectionId";
  public static final String TEST_MODULE = "Test Module";
  public static final String ALPHA = "Alpha";
  public static final String BETA = "Beta";
  public static final String CRITERIA = "criteria";
  public static final String RESULT_SHOULD_NOT_BE_NULL = "Result should not be null";
  public static final String FIELD_NAME = "fieldName";
  public static final String IS_EQUAL_TO = "equals";
  public static final String RELATED_ENTITY_1 = "Related Entity 1";
  public static final String RELATED_ENTITY_2 = "Related Entity 2";
  public static final String MODULE_1 = "module-1";
  public static final String MODULE_1_NAME = "Module 1";
  public static final String ETRX_PROJECTION_ENTITY_ID = "etrxProjectionEntityId";
  public static final String JAVA_MAPPING_1 = "Java Mapping 1";
  public static final String JAVA_MAPPING_2 = "Java Mapping 2";
  public static final String CONSTANT_ONE = "Constant 1";
  public static final String CONSTANT_TWO = "Constant 2";
  public static final String TEST_FIELD = "test_field";
  public static final String OPERATOR = "operator";
  public static final String VALUE = "value";
  public static final String TEST_ID = "testId";
  public static final String GET_SELECTED_MAPPING_VALUES = "getSelectedMappingValues";
  public static final String ETRX_ASYNC_PROC_ID = "@ETRX_Async_Proc.id@";
  public static final String ASYNC_PROC_TEST_ID = "1";
  public static final String PROPERTY_SOURCES = "propertySources";
  public static final String PROJECTION_ID = "testProjectionId";
  public static final String EXTERNAL_NAME = "TestExternalName";
  public static final String PROJECTION_NAME = "ProjectionName";
  public static final String INP_NAME = "inpname";
  public static final String MAPPING_TYPE = "E";
  public static final String INP_MAPPING_TYPE = "inpmappingType";
  public static final String INP_EXTERNAL_NAME = "inpexternalName";
  public static final String INP_ETRX_PROJECTION_ID = "inpetrxProjectionId";
  public static final String INP_AD_TABLE_ID = "inpadTableId";
  public static final String NEW_MODULE_ID = "newModuleId";
  public static final String ERROR_MESSAGE_KEY = "error.message";
  public static final String PARSED_ERROR_MESSAGE = "Parsed error message";
  public static final String NEW_ID = "newId";
  public static final String CHECK_UPDATE_MODULE = "checkUpdateModule";
  public static final String CURRENT_MODULE_ID = "currentModuleId";
  public static final String CHECK_UPDATE_ETRX_CONSTANT_VALUE = "checkUpdateEtrxConstantValue";
  public static final String CHECK_UPDATE_ETRX_PROJECTION_ENTITY_RELATED = "checkUpdateEtrxProjectionEntityRelated";
  public static final String CHECK_UPDATE_JAVA_MAPPING = "checkUpdateJavaMapping";
  public static final String DM = "DM";
  public static final String JM = "JM";
  public static final String MODULE_ID = "module-id";
  public static final String TEST_NAME = "TestName";
  public static final String SOURCE = "source";
  public static final String NAME = "name";
  public static final String AUTH_ENDPOINT = "/auth";
  public static final String AUTH = "auth";
  public static final String AD_IMAGE_ID = "AD_Image_ID";
  public static final String TEST_SELECTOR_ID = "testSelectorId";
  public static final String TEST_TABLE_ID = "testTableId";
  public static final String ETRX_WRONG_ENTITY_NAME = "ETRX_WrongEntityName";
  public static final String GET_JAVA_MAPPING_IDS = "getJavaMappingIds";
  public static final String GET_MODULE_IDS = "getModuleIds";
  public static final String ID = "id";
  public static final String STATE = "state";
  public static final String TIME = "time";
  public static final String DESCRIPTION = "description";
  public static final String PARAMS = "params";
  public static final String LOG = "log";
  public static final String LAST_UPDATE = "lastUpdate";
  protected static final Logger logger = LogManager.getLogger();

  /**
   * Builds an example headless flow for testing purposes.
   * <p>
   * This method creates and saves a series of OpenAPI-related objects, linking them together
   * to form a complete flow. The created objects are added to a list, which is then reversed
   * and returned.
   *
   * @return A list of created BaseOBObject instances representing the headless flow.
   */
  public static List<BaseOBObject> buildExampleHeadlessFlow() {
    var createdElements = new ArrayList<BaseOBObject>();
    // new request header
    OpenAPIRequest opApiRequest = OBProvider.getInstance().get(OpenAPIRequest.class);
    opApiRequest.setNewOBObject(true);
    opApiRequest.setName("TestSalesOrderHeader");
    opApiRequest.setDescription("Test Sales Order Header");
    opApiRequest.setType("ETRX_Tab");
    OBDal.getInstance().save(opApiRequest);
    createdElements.add(opApiRequest);

    // link tab to request (header)
    OpenAPITab opApiTab = OBProvider.getInstance().get(OpenAPITab.class);
    opApiTab.setNewOBObject(true);
    opApiTab.setOpenAPIRequest(opApiRequest);
    opApiTab.setRelatedTabs(OBDal.getInstance().get(Tab.class, "186"));
    OBDal.getInstance().save(opApiTab);
    createdElements.add(opApiTab);

    // new request line
    OpenAPIRequest opApiRequestLine = OBProvider.getInstance().get(OpenAPIRequest.class);
    opApiRequestLine.setNewOBObject(true);
    opApiRequestLine.setName("TestSalesOrderLine");
    opApiRequestLine.setDescription("Test Sales Order Line");
    opApiRequestLine.setType("ETRX_Tab");
    OBDal.getInstance().save(opApiRequestLine);
    createdElements.add(opApiRequestLine);

    // link tab to request (line)
    OpenAPITab opApiTabLine = OBProvider.getInstance().get(OpenAPITab.class);
    opApiTabLine.setNewOBObject(true);
    opApiTabLine.setOpenAPIRequest(opApiRequestLine);
    opApiTabLine.setRelatedTabs(OBDal.getInstance().get(Tab.class, "187"));
    OBDal.getInstance().save(opApiTabLine);
    createdElements.add(opApiTabLine);

    // new flow
    OpenApiFlow flow = OBProvider.getInstance().get(OpenApiFlow.class);
    flow.setNewOBObject(true);
    flow.setName("TESTFLOW");
    flow.setDescription("Test Flow Description");
    OBDal.getInstance().save(flow);
    createdElements.add(flow);

    // new flow-request link (header)
    OpenApiFlowPoint flowpoint = OBProvider.getInstance().get(OpenApiFlowPoint.class);
    flowpoint.setNewOBObject(true);
    flowpoint.setEtapiOpenapiReq(opApiRequest);
    flowpoint.setEtapiOpenapiFlow(flow);
    OBDal.getInstance().save(flowpoint);
    createdElements.add(flowpoint);

    // new flow-request link (line)
    OpenApiFlowPoint flowpointLine = OBProvider.getInstance().get(OpenApiFlowPoint.class);
    flowpointLine.setNewOBObject(true);
    flowpointLine.setEtapiOpenapiReq(opApiRequestLine);
    flowpointLine.setEtapiOpenapiFlow(flow);
    OBDal.getInstance().save(flowpointLine);
    createdElements.add(flowpointLine);


    // to do the remove in the reverse order
    Collections.reverse(createdElements);

    return createdElements;
  }

  /**
   * Sets up a mocked HttpServletResponse and returns a MockedResponse object.
   *
   * @return A MockedResponse object containing the mocked HttpServletResponse,
   *     StringWriter, and PrintWriter.
   * @throws IOException
   *     If an I/O error occurs.
   */
  public static @NotNull MockedResponse getResponseMocked() throws IOException {
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    when(response.getWriter()).thenReturn(printWriter);

    return new MockedResponse(response, stringWriter, printWriter);
  }


  /**
   * Sets up a mocked HttpServletRequest with the provided JSON payload.
   *
   * @param jsonPayload
   *     The JSON payload to be used in the request.
   * @param xmlDocument
   * @return A mocked HttpServletRequest with the provided JSON payload.
   * @throws JSONException
   *     If there is an error parsing the JSON payload.
   * @throws IOException
   *     If there is an I/O error.
   * @throws ServletException
   *     If there is a servlet error.
   */
  public static @NotNull HttpServletRequest setupRequestMocked(
      JSONObject jsonPayload, Document xmlDocument) throws JSONException, IOException, ServletException {
    // Mock the HttpServletRequest
    HttpServletRequest request = mock(HttpServletRequest.class);

    // Convert the JSON payload to a ByteArrayInputStream
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        jsonPayload.toString().getBytes(StandardCharsets.UTF_8));

    // Create a ServletInputStream that reads from the ByteArrayInputStream
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
        // No implementation needed for this mock
      }

      @Override
      public int read() throws IOException {
        return byteArrayInputStream.read();
      }
    };

    // Configure the mock to return the correct ServletInputStream
    when(request.getInputStream()).thenReturn(servletInputStream);

    // Create a new HttpSessionWrapper and configure the mock to return it
    HttpSession httpSession = new HttpSessionWrapper();
    when(request.getSession(anyBoolean())).thenReturn(httpSession);
    when(request.getSession()).thenReturn(httpSession);

    // Set session attributes for user client and organization
    request.getSession().setAttribute(getUpperCase("#User_Client"),
        String.format("'%s'", OBContext.getOBContext().getCurrentClient().getId()));
    request.getSession().setAttribute(getUpperCase("#User_Org"),
        String.format("'%s'", OBContext.getOBContext().getCurrentOrganization().getId()));

    // Get the format XML document and load the format settings into the session
    loadFormats(httpSession, xmlDocument);

    // Set the request and variable secure app in the RequestContext
    RequestContext.get().setRequest(request);
    VariablesSecureApp varsN = new VariablesSecureApp(request);
    RequestContext.get().setVariableSecureApp(varsN);
    RequestContext.setServletContext(new OBServletContextMock());

    // Fill session variables using SecureWebServicesUtils
    SecureWebServicesUtils.fillSessionVariables(request);

    // Return the mocked HttpServletRequest
    return request;
  }

  /**
   * Converts the given string to uppercase.
   *
   * @param s
   *     The string to be converted to uppercase.
   * @return The uppercase version of the given string.
   */
  private static @NotNull String getUpperCase(String s) {
    return StringUtils.upperCase(s);
  }

  /**
   * Loads format settings from the format XML document into the provided HttpSession.
   * <p>
   * This method retrieves the format XML document, extracts the <Number> elements,
   * and stores their attributes in the session as format settings.
   *
   * @param httpSession
   *     The HttpSession where the format settings will be stored.
   * @param xmlDocument
   */
  private static void loadFormats(HttpSession httpSession, Document xmlDocument) {
    Document formatDoc = xmlDocument;
    Element root = formatDoc.getRootElement();

    // Get all <Number> elements
    List<Element> numberNodes = root.elements("Number");

    for (Element numberElement : numberNodes) {

      // Get attributes
      String name = numberElement.attributeValue("name");
      String decimal = numberElement.attributeValue("decimal");
      String grouping = numberElement.attributeValue("grouping");
      String formatOutput = numberElement.attributeValue("formatOutput");
      String formatInternal = numberElement.attributeValue("formatInternal");

      logger.info(" Loading for format: {} decimal: {} grouping: {} formatOutput: {} formatInternal: {}", name, decimal,
          grouping, formatOutput, formatInternal);
      // Store the values in the session
      httpSession.setAttribute(getUpperCase("#FormatOutput|" + name), formatOutput);
      httpSession.setAttribute(getUpperCase("#DecimalSeparator|" + name), decimal);
      httpSession.setAttribute(getUpperCase("#GroupSeparator|" + name), grouping);
      httpSession.setAttribute(getUpperCase("#FormatInternal|" + name), formatInternal);
    }
  }
}