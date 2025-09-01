package com.etendoerp.etendorx.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.DefaultValidationException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.LoginUtils.RoleDefaults;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import com.etendoerp.etendorx.data.OpenAPIRequestField;
import com.etendoerp.etendorx.data.OpenAPITab;
import com.etendoerp.etendorx.openapi.OpenAPIConstants;
import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;
import com.etendoerp.etendorx.services.wrapper.EtendoResponseWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.etendorx.utils.DataSourceUtils;
import com.etendoerp.etendorx.utils.SelectorHandlerUtil;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.SecureWebServicesUtils;


/**
 * Servlet that handles data source requests.
 */
public class DataSourceServlet implements WebService {

  private static final Logger log = LogManager.getLogger();
  public static final String _START_ROW = "_startRow";
  public static final String _END_ROW = "_endRow";
  public static final String RESPONSE = "response";

  /**
   * Gets the DataSourceServlet instance.
   *
   * @return the DataSourceServlet instance
   */
  static org.openbravo.service.datasource.DataSourceServlet getDataSourceServlet() {
    return WeldUtils.getInstanceFromStaticBeanManager(org.openbravo.service.datasource.DataSourceServlet.class);
  }

  /**
   * Handles HTTP GET requests.
   *
   * @param path
   *     the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *     the HttpServletRequest
   * @param response
   *     the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    try {
      OBContext.setAdminMode();
      fillSessionVariableInRequest(request);
      DalConnectionProvider conn = new DalConnectionProvider();
      RoleDefaults defaults = LoginUtils.getLoginDefaults(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getRole().getId(), conn);

      LoginUtils.fillSessionArguments(conn, new VariablesSecureApp(request), OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getLanguage().getLanguage(), OBContext.getOBContext().isRTL() ? "Y" : "N",
          defaults.role, defaults.client, OBContext.getOBContext().getCurrentOrganization().getId(),
          defaults.warehouse);
      String[] extractedParts = DataSourceUtils.extractDataSourceAndID(path);
      String dataSourceName = convertURI(extractedParts);

      String rsql = request.getParameter("q");
      Map<String, String[]> params = new HashMap<>();
      var paramNames = request.getParameterNames();
      loadParams(request, paramNames, params);
      params.put("isImplicitFilterApplied", new String[]{ "false" });
      params.put("_operationType", new String[]{ "fetch" });
      params.put("_noActiveFilter", new String[]{ "true" });
      if (StringUtils.isNotEmpty(rsql)) {
        params.put("operator", new String[]{ "and" });
        params.put("_constructor", new String[]{ "AdvancedCriteria" });
        if (!StringUtils.isEmpty(rsql)) {
          // url encode criteria
          convertCriterion(params, rsql);
        }

        params.put("_textMatchStyle", new String[]{ "substring" });
      }
      if (!params.containsKey(_START_ROW)) {
        params.put(_START_ROW, new String[]{ "0" });
      }
      if (!params.containsKey(_END_ROW)) {
        params.put(_END_ROW, new String[]{ "100" });
      }
      String dtsn = extractedParts[0];
      Tab tabByDataSourceName = DataSourceUtils.getTabByDataSourceName(dtsn);
      params.put("tabId", new String[]{ tabByDataSourceName.getId() });
      params.put("windowId", new String[]{ tabByDataSourceName.getWindow().getId() });
      String csrf = "123";
      request.getSession(false).setAttribute("#CSRF_TOKEN", csrf);
      params.put("csrfToken", new String[]{ csrf });

      var newRequest = new EtendoRequestWrapper(request, dataSourceName, "", params);
      var newResponse = new EtendoResponseWrapper(response);
      getDataSourceServlet().doPost(newRequest, newResponse);
      JSONObject capturedResponse = newResponse.getCapturedContent();
      if (!capturedResponse.has(DataSourceConstants.RESPONSE) || !capturedResponse.getJSONObject(
          DataSourceConstants.RESPONSE).has(DataSourceConstants.DATA)) {
        // Standard error
        String message = DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET;
        if (capturedResponse.has(DataSourceConstants.RESPONSE) && capturedResponse.getJSONObject(
            DataSourceConstants.RESPONSE).has(DataSourceConstants.ERROR) && capturedResponse.getJSONObject(
            DataSourceConstants.RESPONSE).getJSONObject(DataSourceConstants.ERROR).has(DataSourceConstants.MESSAGE)) {
          message = capturedResponse.getJSONObject(DataSourceConstants.RESPONSE).getJSONObject(
              DataSourceConstants.ERROR).getString(DataSourceConstants.MESSAGE);
        }
        throw new OBException(message);
      }
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(capturedResponse.toString());
    } catch (OpenAPINotFoundThrowable e) {
      handleNotFoundException(response);
    } catch (OBException | IOException e) {
      log.error(DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET, e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Loads parameters from the HttpServletRequest into the provided map, excluding the "q" parameter.
   *
   * @param request
   *     The HttpServletRequest containing the parameters.
   * @param paramNames
   *     An enumeration of parameter names from the request.
   * @param params
   *     The map where the parameters will be stored.
   */
  private static void loadParams(HttpServletRequest request, Enumeration<String> paramNames,
      Map<String, String[]> params) {
    while (paramNames.hasMoreElements()) {
      String param = paramNames.nextElement();
      if (!StringUtils.equals(param, "q")) {
        params.put(param, new String[]{ request.getParameter(param) });
      }
    }
  }

  /**
   * Fills session variables in the request if needed.
   * <p>
   * This method checks if the session variables are already loaded. If not, it loads the session variables
   * using the `SecureWebServicesUtils.fillSessionVariables` method.
   *
   * @param request
   *     The HttpServletRequest object.
   * @throws RuntimeException
   *     If there is a ServletException during the process.
   */
  private void fillSessionVariableInRequest(HttpServletRequest request) {
    try {
      OBContext.setAdminMode();
      VariablesSecureApp variables = RequestContext.get().getVariablesSecureApp();
      if (variables.getJavaDateFormat() == null || StringUtils.isEmpty(
          variables.getSessionValue("#DecimalSeparator|generalQtyEdition"))) {
        SecureWebServicesUtils.fillSessionVariables(request);
        log.debug("Session variables loaded in DataSourceServlet.");

      } else {
        log.debug("Session variables already loaded previously.");
      }
    } catch (ServletException e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Converts the criterion to a list of parameters.
   *
   * @param params
   * @param rsql
   */
  private void convertCriterion(Map<String, String[]> params, String rsql) {
    try {
      var criteria = OBRestUtils.criteriaFromRSQL(rsql);
      criteria.put("_constructor", "AdvancedCriteria");
      params.put("criteria", new String[]{ criteria.toString() });
    } catch (JSONException e) {
      throw new OBException("Cannot convert RSQL to criteria " + rsql, e);
    }
  }

  /**
   * Handles HTTP POST requests.
   *
   * @param response
   * @throws IOException
   */
  private static void handleNotFoundException(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
    response.getWriter().write("{\"error\": \"Not Found\", \"message\": \"The requested resource was not found.\"}");
    response.getWriter().flush();
  }

  String getBodyFromRequest(HttpServletRequest request) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStream inputStream = request.getInputStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
      }
    }
    return sb.toString();
  }


  /**
   * Creates a payload for the POST request.
   *
   * <p>This method constructs a `JSONObject` payload by first extracting the body
   * from the provided `HttpServletRequest` and then passing it to another overloaded
   * `createPayLoad` method for further processing.
   *
   * @param request
   *     The `HttpServletRequest` object containing the request data.
   * @return A `JSONObject` representing the payload for the POST request.
   * @throws IOException
   *     If an I/O error occurs while reading the request body.
   * @throws JSONException
   *     If an error occurs while parsing the JSON data.
   */
  JSONObject createPayLoad(HttpServletRequest request) throws IOException, JSONException {
    return createPayLoad(request, new JSONObject(getBodyFromRequest(request)));
  }

  /**
   * Creates the payload for the POST request.
   *
   * @param request
   * @param jsonBody
   */
  JSONObject createPayLoad(HttpServletRequest request, JSONObject jsonBody) {
    String csrf = "123";
    request.getSession(false).setAttribute("#CSRF_TOKEN", csrf);
    try {
      JSONObject newJsonBody = new JSONObject();

      newJsonBody.put("dataSource", "isc_OBViewDataSource_0");
      newJsonBody.put("operationType", "add");
      newJsonBody.put("componentId", "isc_OBViewForm_0");
      newJsonBody.put("csrfToken", csrf);
      newJsonBody.put(DataSourceConstants.DATA, jsonBody);
      return newJsonBody;
    } catch (JSONException e) {
      log.error(DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET, e);
      throw new OBException(e);
    }
  }

  /**
   * Dispatches the POST request to the DataSourceServlet.
   *
   * @param path
   *     the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *     the HttpServletRequest
   * @param response
   *     the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    fillSessionVariableInRequest(request);
    upsertEntity(OpenAPIConstants.POST, path, request, response);
  }

  /**
   * Upserts the entity depending on the method.
   *
   * @param method
   * @param path
   * @param request
   * @param response
   */
  private void upsertEntity(String method, String path, HttpServletRequest request, HttpServletResponse response) {
    try {
      List<RequestField> fieldList = new ArrayList<>();
      Tab tab = getTab(path, request, response, fieldList);
      if (tab == null) {
        handleNotFoundException(response);
        return;
      }
      String newUri = convertURI(DataSourceUtils.extractDataSourceAndID(path));
      var servlet = getDataSourceServlet();

      if (StringUtils.equals(OpenAPIConstants.POST, method)) {
        processPostRequest(request, response, tab, fieldList, newUri, servlet);
      } else if (StringUtils.equals(OpenAPIConstants.PUT, method)) {
        processPutRequest(request, response, fieldList, newUri, path, servlet);
      } else {
        throw new UnsupportedOperationException("Method not supported: " + method);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET, e);
      handleInternalServerError(response, e);
    } catch (OpenAPINotFoundThrowable e) {
      try {
        handleNotFoundException(response);
      } catch (IOException ex) {
        throw new OBException(ex);
      }
    }
  }

  /**
   * Processes the POST request.
   *
   * @param request
   * @param response
   * @param tab
   * @param fieldList
   * @param newUri
   * @param servlet
   * @throws Exception
   */
  private void processPostRequest(HttpServletRequest request, HttpServletResponse response,
      Tab tab, List<RequestField> fieldList, String newUri,
      org.openbravo.service.datasource.DataSourceServlet servlet)
      throws Exception {
    String jsonBody = getBodyFromRequest(request);
    JSONArray payloads = preparePayloads(jsonBody);
    JSONObject jsonResponse = new JSONObject();
    jsonResponse.put(DataSourceConstants.RESPONSE, new JSONObject());
    JSONArray jsonData = new JSONArray();
    jsonResponse.getJSONObject(DataSourceConstants.RESPONSE).put(DataSourceConstants.DATA, jsonData);
    int status = 0;
    for (int i = 0; i < payloads.length(); i++) {
      JSONObject payload = payloads.getJSONObject(i);
      try {
        int currentStatus = processPayload(request, response, tab, fieldList, newUri, servlet,
            payload, jsonData, status);
        if (status != -1) {
          status = currentStatus;
        }
      } catch (JSONException | OpenAPINotFoundThrowable e) {
        status = -1;
        jsonData.put(new JSONObject(e.getMessage()));
      }
    }

    sendResponse(response, jsonResponse, status);
  }

  /**
   * Prepares the payloads for the POST request.
   *
   * @param jsonBody
   * @throws JSONException
   */
  private JSONArray preparePayloads(String jsonBody) throws JSONException {
    boolean isArray = jsonBody.trim().startsWith("[");
    if (isArray) {
      return new JSONArray(jsonBody);
    } else {
      JSONArray payloads = new JSONArray();
      payloads.put(new JSONObject(jsonBody));
      return payloads;
    }
  }

  /**
   * Processes the payload for the POST request.
   *
   * @param request
   * @param response
   * @param tab
   * @param fieldList
   * @param newUri
   * @param servlet
   * @param payload
   * @param jsonData
   * @param status
   * @throws Exception
   * @throws OpenAPINotFoundThrowable
   */
  private int processPayload(HttpServletRequest request, HttpServletResponse response,
      Tab tab, List<RequestField> fieldList, String newUri,
      org.openbravo.service.datasource.DataSourceServlet servlet,
      JSONObject payload, JSONArray jsonData, int status)
      throws Exception, OpenAPINotFoundThrowable {
    // Clear session variables to prevent cross-record contamination
    clearSessionVariables(null);
    
    EtendoRequestWrapper newRequest = getEtendoPostWrapper(request, tab,
        createPayLoad(request, payload), fieldList, newUri);
    HttpServletResponse wrappedResponse = new EtendoResponseWrapper(response);

    servlet.doPost(newRequest, wrappedResponse);
    String content = ((EtendoResponseWrapper) wrappedResponse).getCapturedContent().toString();
    JSONObject jsonContent = new JSONObject(content);

    if (jsonContent.has(DataSourceConstants.RESPONSE)) {
      JSONObject responseContent = jsonContent.getJSONObject(DataSourceConstants.RESPONSE);
      if (responseContent.has(DataSourceConstants.DATA)) {
        JSONArray data = responseContent.getJSONArray(DataSourceConstants.DATA);
        if (data.length() > 0) {
          jsonData.put(data.getJSONObject(0));
        }
      }
      if (responseContent.has(DataSourceConstants.ERROR)) {
        status = -1;
        jsonData.put(responseContent.getJSONObject(DataSourceConstants.ERROR));
      }
    }

    return status;
  }

  /**
   * Sends the response back to the client.
   *
   * @param response
   * @param jsonResponse
   * @param status
   * @throws IOException
   * @throws JSONException
   */
  private void sendResponse(HttpServletResponse response, JSONObject jsonResponse, int status)
      throws IOException, JSONException {
    jsonResponse.put("status", status);
    response.setStatus(status == -1 ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_OK);
    response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(jsonResponse.toString());
    response.getWriter().flush();
  }

  /**
   * Processes the PUT request.
   *
   * @param request
   * @param response
   * @param fieldList
   * @param newUri
   * @param path
   * @param servlet
   * @throws Exception
   * @throws OpenAPINotFoundThrowable
   */
  private void processPutRequest(HttpServletRequest request, HttpServletResponse response,
      List<RequestField> fieldList, String newUri, String path,
      org.openbravo.service.datasource.DataSourceServlet servlet)
      throws Exception, OpenAPINotFoundThrowable {
    JSONObject jsonBody = new JSONObject(getBodyFromRequest(request));
    EtendoRequestWrapper newRequest = getEtendoPutWrapper(request, response,
        createPayLoad(request, jsonBody), fieldList, newUri, path);
    servlet.doPut(newRequest, response);
  }

  /**
   * Handles the internal server error.
   *
   * @param response
   * @param e
   */
  private void handleInternalServerError(HttpServletResponse response, Exception e) {
    try {
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      JSONObject jsonErrorResponse = new JSONObject();
      jsonErrorResponse.put(DataSourceConstants.ERROR, "Internal Server Error");
      jsonErrorResponse.put(DataSourceConstants.MESSAGE,
          e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      jsonErrorResponse.put("status", 500);
      response.getWriter().write(jsonErrorResponse.toString());
      response.getWriter().flush();
    } catch (Exception ex) {
      throw new OBException(ex);
    }
  }

  /**
   * Gets the tab for the given path.
   *
   * @param path
   * @param request
   * @param response
   * @param fieldList
   * @throws ServletException
   * @throws DefaultValidationException
   * @throws IOException
   */
  private static Tab getTab(String path, HttpServletRequest request, HttpServletResponse response,
      List<RequestField> fieldList) throws ServletException, DefaultValidationException, IOException {
    Tab tab;
    try {
      var dataSource = DataSourceUtils.extractDataSourceAndID(path);
      OBContext.setAdminMode();
      DalConnectionProvider conn = new DalConnectionProvider();
      RoleDefaults defaults = LoginUtils.getLoginDefaults(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getRole().getId(), conn);

      LoginUtils.fillSessionArguments(conn, new VariablesSecureApp(request), OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getLanguage().getLanguage(), OBContext.getOBContext().isRTL() ? "Y" : "N",
          defaults.role, defaults.client, OBContext.getOBContext().getCurrentOrganization().getId(),
          defaults.warehouse);

      OBCriteria<OpenAPIRequest> crit = OBDal.getInstance().createCriteria(OpenAPIRequest.class);
      crit.add(Restrictions.eq("name", dataSource[0]));
      OpenAPIRequest req = (OpenAPIRequest) crit.setMaxResults(1).uniqueResult();
      OBDal.getInstance().refresh(req);
      if (req == null || req.getETRXOpenAPITabList().isEmpty()) {
        handleNotFoundException(response);
        return null;
      }
      tab = req.getETRXOpenAPITabList().get(0).getRelatedTabs();
      for (Field field : tab.getADFieldList()) {
        Column column = field.getColumn();
        if (column == null) {
          continue;
        }
        String name = DataSourceUtils.getHQLColumnName(field)[0];
        fieldList.add(
            new RequestField(name, column, getSeqNo(req.getETRXOpenAPITabList().get(0), column)));
      }
      fieldList.sort(Comparator.comparing(RequestField::getSeqNo));
    } finally {
      OBContext.restorePreviousMode();
    }
    return tab;
  }

  /**
   * Retrieves the sequence number (seqno) associated with a specific field column
   * in the given OpenAPITab. If the field column is not found or the sequence number
   * is null, it returns {@link Long#MAX_VALUE}.
   *
   * @param openAPITab
   *     The OpenAPITab object containing the list of OpenAPIRequestField objects.
   * @param fieldColumn
   *     The Column object representing the field column to search for.
   * @return The sequence number (seqno) of the matching field, or {@link Long#MAX_VALUE}
   *     if the field is not found or its sequence number is null.
   */
  private static Long getSeqNo(OpenAPITab openAPITab, Column fieldColumn) {
    Optional<OpenAPIRequestField> optField = openAPITab.getEtrxOpenapiFieldList().stream().filter(
        fi -> fi.getField().getColumn().equals(fieldColumn)).findFirst();
    if (optField.isEmpty()) {
      return Long.MAX_VALUE;
    }
    OpenAPIRequestField field = optField.get();
    if (field.getSeqno() == null) {
      return Long.MAX_VALUE;
    }
    return field.getSeqno();
  }

  /**
   * Creates the payload for the POST request.
   *
   * @param request
   * @param tab
   * @param newJsonBody
   * @param fieldList
   * @param newUri
   * @throws JSONException
   * @throws IOException
   * @throws OpenAPINotFoundThrowable
   */
  private EtendoRequestWrapper getEtendoPostWrapper(HttpServletRequest request, Tab tab, JSONObject newJsonBody,
      List<RequestField> fieldList,
      String newUri) throws JSONException, IOException, OpenAPINotFoundThrowable, ScriptException, ParseException {
    JSONObject dataFromOriginalRequest = newJsonBody.getJSONObject(DataSourceConstants.DATA);
    String recordId = dataFromOriginalRequest.optString("id");

    String parentId = DataSourceUtils.getParentId(tab, dataFromOriginalRequest);


    Map<String, Object> parameters = createParameters(request, tab.getId(), parentId, recordId, null, "NEW");
    String content = "{}";


    /* the columns uses 3 name format: database column name, hql(or normalized) and input Format. So, to switch between them, we need to generate 3 maps */
    LinkedHashMap<String, String> norm2input = new LinkedHashMap<>(); // to keep the order
    Map<String, String> input2norm = new HashMap<>();
    Map<String, String> dbname2input = new HashMap<>();
    Map<String, String> columnTypes = new HashMap<>();
    DataSourceUtils.loadCaches(fieldList, norm2input, input2norm, dbname2input, columnTypes);


    //remove the parent properties and ID, to detect properties that has been "changed" to emulate the change event
    // for every property that has been changed, we need to call the formInit with the new value
    JSONObject propsToChange = new JSONObject(dataFromOriginalRequest.toString());
    List<String> parentProperties = DataSourceUtils.getParentProperties(tab);
    for (String parentProperty : parentProperties) {
      propsToChange.remove(parentProperty);
    }

    //this variable will store accumulated data from the "New" Initialization, but first we need to convert the keys to input format
    JSONObject dataFromNewRecord = new JSONObject(dataFromOriginalRequest.toString());
    dataFromNewRecord = DataSourceUtils.keyConvertion(dataFromNewRecord, norm2input);


    // Clear session variables to avoid conflicts between records
    clearSessionVariables(null);

    //Initialization of new record, saving the data in input format
    var formInit = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class);
    var formInitResponse = formInit.execute(parameters, content);
    DataSourceUtils.applyColumnValues(formInitResponse, dbname2input, dataFromNewRecord);

    //to proceed with Change events, we need convert the keys to normalized format to input format
    JSONObject dataInpFormat = DataSourceUtils.keyConvertion(dataFromNewRecord, norm2input);
    dataInpFormat.put("keyProperty", "id");//    "keyProperty":"id",
    dataInpFormat.put(OBBindingsConstants.WINDOW_ID_PARAM, tab.getWindow().getId());


    //the props in the request are in normalized format, so to maintain the order in that list, we can iterate over the keys of the normalized map,
    // but filtering the props that are not in the request
    List<String> orderedPropsToChange = norm2input.keySet().stream().filter(propsToChange::has).collect(
        Collectors.toList());
    for (String changedColumnN : orderedPropsToChange) {
      logChangeEvent(changedColumnN);
      String changedColumnInp = norm2input.get(changedColumnN);
      var type = columnTypes.get(changedColumnN);
      dataInpFormat.put(changedColumnInp,
          DataSourceUtils.valueConvertToInputFormat(propsToChange.get(changedColumnN), type));
      SelectorHandlerUtil.handleColumnSelector(request, tab, dataInpFormat, changedColumnN, changedColumnInp,
          dbname2input);

      // suppose to change in productID
      Map<String, Object> parameters2 = createParameters(request, tab.getId(), parentId, recordId, changedColumnInp,
          "CHANGE");

      // Clear session variables before CHANGE events (always clear for all field types)
      clearSessionVariables(dataInpFormat);

      String contentForChange = dataInpFormat.toString();
      var formInitChangeResponse = formInit.execute(parameters2, contentForChange);
      DataSourceUtils.applyColumnValues(formInitChangeResponse, dbname2input, dataInpFormat);
    }


    // to finally save the dataFromOriginalRequest, we need to convert the keys to normalized format
    JSONObject jsonBodyToSave = DataSourceUtils.keyConvertion(dataInpFormat, input2norm);
    jsonBodyToSave = DataSourceUtils.valuesConvertion(jsonBodyToSave, columnTypes);
    newJsonBody.put(DataSourceConstants.DATA, jsonBodyToSave);

    return new EtendoRequestWrapper(request, newUri, newJsonBody.toString(), request.getParameterMap());
  }

  /**
   * Logs the change event for a specified column.
   * <p>
   * This method logs a debug message indicating that the logic for a change event is being recreated
   * and the value for the specified column is being set.
   *
   * @param changedColumnN
   *     The name of the column that has changed.
   */
  private static void logChangeEvent(String changedColumnN) {
    log.debug(" Recreating logic for change event, setting value for: {}", changedColumnN);
  }

  /**
   * Checks for errors in the form initialization response.
   * <p>
   * This method examines the provided JSON object for an error response. If an error is found,
   * it throws an OBException with the error message.
   *
   * @param formInitResponse
   *     The JSON object containing the form initialization response.
   * @throws JSONException
   *     If there is an error during JSON processing.
   * @throws OBException
   *     If the response contains an error message.
   */
  private void checkForError(JSONObject formInitResponse) throws JSONException {
    if (formInitResponse.has(RESPONSE) && formInitResponse.getJSONObject(RESPONSE).has("error")) {
      throw new OBException(formInitResponse.getJSONObject(RESPONSE).getJSONObject("error").getString("message"));
    }
  }


  /**
   * Creates the payload for the PUT request.
   *
   * @param request
   * @param response
   * @param fullDataBody
   * @param fieldList
   * @param newUri
   * @param path
   * @throws JSONException
   * @throws IOException
   * @throws ServletException
   * @throws OpenAPINotFoundThrowable
   */
  private EtendoRequestWrapper getEtendoPutWrapper(HttpServletRequest request, HttpServletResponse response,
      JSONObject fullDataBody, List<RequestField> fieldList, String newUri,
      String path) throws JSONException, IOException, ServletException, OpenAPINotFoundThrowable, ScriptException, ParseException {
    String[] extractedParts = DataSourceUtils.extractDataSourceAndID(path);
    String getURI = convertURI(extractedParts);
    JSONObject newData = fullDataBody.optJSONObject("data");
    if (extractedParts.length < 2 || StringUtils.isEmpty(extractedParts[1])) {
      throw new OBException(OBMessageUtils.messageBD("ETRX_RecordIdNotFound"));
    }
    String recordId = extractedParts[1];


    var newRequest = new EtendoRequestWrapper(request, getURI, "", request.getParameterMap());
    var newResponse = new EtendoResponseWrapper(response);
    getDataSourceServlet().doGet(newRequest, newResponse);
    JSONObject capturedResponse = newResponse.getCapturedContent();
    JSONObject preexistentData = capturedResponse.getJSONObject(DataSourceConstants.RESPONSE).getJSONArray(
        DataSourceConstants.DATA).getJSONObject(0);

    //define the maps
    LinkedHashMap<String, String> norm2input = new LinkedHashMap<>();
    Map<String, String> input2norm = new HashMap<>();
    Map<String, String> dbname2input = new HashMap<>();
    Map<String, String> columnTypes = new HashMap<>();

    DataSourceUtils.loadCaches(fieldList, norm2input, input2norm, dbname2input, columnTypes);

    //invoinv the formInit to get the data in input format, beign the base of the new data and the change events
    //we need to execute the forminit in mode EDIT
    Map<String, Object> parameters = createParameters(request,
        DataSourceUtils.getTabByDataSourceName(extractedParts[0]).getId(), null, recordId, null, "EDIT");

    // Clear session variables to avoid conflicts between records
    clearSessionVariables(null);

    String content = "{}";
    var formInitResponse = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class).execute(parameters,
        content);
    checkForError(formInitResponse);

    //apply the values from the formInitResponse to the dataInpFormat

    JSONObject dataInpFormat = new JSONObject();

    DataSourceUtils.applyColumnValues(formInitResponse, dbname2input, dataInpFormat);

    dataInpFormat.put("keyProperty", "id");
    dataInpFormat.put(OBBindingsConstants.WINDOW_ID_PARAM,
        DataSourceUtils.getTabByDataSourceName(extractedParts[0]).getWindow().getId());

    //to proceed with Change events, we need to iterate over the keys of newData, setting the values in dataInpFormat and calling the formInit.
    // we need to convert the keys to normalized format to input format.
    //we will mantain the order in norm2input, so we can iterate over the keys of norm2input, filtering the props that are not in the request
    List<String> orderedPropsToChange = norm2input.keySet().stream().filter(newData::has).collect(Collectors.toList());

    for (String changedColumnN : orderedPropsToChange) {
      if (StringUtils.equalsIgnoreCase(changedColumnN, "id")) {
        //we don't need to change the id
        continue;
      }
      logChangeEvent(changedColumnN);
      String changedColumnInp = norm2input.get(changedColumnN);
      String type = columnTypes.get(changedColumnN);
      String valueInpFormat = DataSourceUtils.valueConvertToInputFormat(newData.get(changedColumnN), type);
      dataInpFormat.put(changedColumnInp, valueInpFormat);
      SelectorHandlerUtil.handleColumnSelector(request, DataSourceUtils.getTabByDataSourceName(extractedParts[0]),
          dataInpFormat,
          changedColumnN, changedColumnInp, dbname2input);
      // suppose to change in productID
      Map<String, Object> parameters2 = createParameters(request,
          DataSourceUtils.getTabByDataSourceName(extractedParts[0]).getId(), null, recordId, changedColumnInp,
          "CHANGE");
      
      // Clear session variables before CHANGE events (always clear for all field types)
      clearSessionVariables(dataInpFormat);
      
      String contentForChange = dataInpFormat.toString();
      var formInitChangeResponse = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class).execute(
          parameters2, contentForChange);
      DataSourceUtils.applyColumnValues(formInitChangeResponse, dbname2input, dataInpFormat);
    }

    // to finally save the dataFromOriginalRequest, we need to convert the keys to normalized format
    JSONObject jsonBodyToApply = DataSourceUtils.keyConvertion(dataInpFormat, input2norm);
    jsonBodyToApply = DataSourceUtils.valuesConvertion(jsonBodyToApply, columnTypes);
    //finally, we need to apply the changes to the original data
    JSONObject jsonBodyToSave = DataSourceUtils.applyChanges(preexistentData, jsonBodyToApply);
    fullDataBody.put(DataSourceConstants.DATA, jsonBodyToSave);
    return new EtendoRequestWrapper(request, newUri, fullDataBody.toString(), request.getParameterMap());
  }

  /**
   * Creates the parameters for the request.
   *
   * @param request
   * @param tabId
   * @param parentId
   * @param recordId
   * @param changedColumn
   * @param mode
   */
  private static Map<String, Object> createParameters(HttpServletRequest request, String tabId, String parentId,
      String recordId, String changedColumn, String mode) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("_httpRequest", request);
    parameters.put("_httpSession", request.getSession(false));
    parameters.put("MODE", mode);
    parameters.put("_action", "org.openbravo.client.application.window.FormInitializationComponent");
    parameters.put("PARENT_ID", parentId);
    parameters.put("TAB_ID", StringUtils.isEmpty(tabId) ? "null" : tabId);
    parameters.put("ROW_ID", StringUtils.isEmpty(recordId) ? "null" : recordId);
    if (StringUtils.isNotEmpty(changedColumn)) {
      parameters.put("CHANGED_COLUMN", changedColumn);
    }
    return parameters;
  }


  /**
   * Converts the request URI to the new URI.
   *
   * @param extractedParts
   *     the extracted parts from the request URI, the first part is the data source name and
   *     the second part is the ID
   * @throws OpenAPINotFoundThrowable
   */
  String convertURI(String[] extractedParts) throws OpenAPINotFoundThrowable {
    try {
      OBContext.setAdminMode();
      String dataSourceName = extractedParts[0];
      Tab tab = DataSourceUtils.getTabByDataSourceName(dataSourceName);
      String requestName = tab.getTable().getName();

      StringBuilder newUri = new StringBuilder();
      newUri.append("/com.etendoerp.etendorx.datasource/").append(dataSourceName).append(
          DataSourceConstants.DATASOURCE_SERVLET_PATH).append(requestName);

      if (extractedParts.length > 1) {
        newUri.append("/").append(extractedParts[1]);
      }

      return newUri.toString();
    } catch (OBException e) {
      log.error(DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET, e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Handles HTTP DELETE requests.
   *
   * @param path
   *     the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *     the HttpServletRequest
   * @param response
   *     the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    throw new UnsupportedOperationException("DELETE method not supported.");
  }

  /**
   * Handles HTTP PUT requests.
   *
   * @param path
   *     the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *     the HttpServletRequest
   * @param response
   *     the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
    fillSessionVariableInRequest(request);
    upsertEntity(OpenAPIConstants.PUT, path, request, response);
  }

  /**
   * Clears session variables to prevent conflicts between records.
   * This method removes session variables that are used by callouts and form initialization
   * to ensure that when processing multiple records, each record starts with a clean session state.
   *
   * @param dataInpFormat optional JSONObject containing the input format data with "inp" parameters to clear
   */
  private void clearSessionVariables(JSONObject dataInpFormat) {
    RequestContext requestContext = RequestContext.get();
    
    // Collect all "inp" parameter names from dataInpFormat if available
    List<String> inpParametersToClean = new ArrayList<>();
    if (dataInpFormat != null) {
      @SuppressWarnings("unchecked")
      Iterator<String> keys = dataInpFormat.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (key.startsWith("inp")) {
          inpParametersToClean.add(key);
        }
      }
    }
    
    // Clear RequestContext session attributes and request parameters
    for (String param : inpParametersToClean) {
      // Clear from RequestContext session attributes
      Object sessionValue = requestContext.getSessionAttribute(param);
      if (sessionValue != null) {
        requestContext.setSessionAttribute(param, null);
      }
      
      // Clear from RequestContext request parameters
      String requestValue = requestContext.getRequestParameter(param);
      if (requestValue != null) {
        requestContext.setRequestParameter(param, null);
      }
    }
  }
}
