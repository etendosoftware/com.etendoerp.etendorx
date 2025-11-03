package com.etendoerp.etendorx.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.text.MessageFormat;
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
 * Servlet facade for handling headless DataSource requests inside Etendorx.
 *
 * <p>This class adapts higher-level OpenAPI-style requests into internal
 * Openbravo DataSource servlet calls. It is responsible for:
 *
 * <ul>
 *   <li>Parsing and validating incoming HTTP requests (GET/POST/PUT).</li>
 *   <li>Preparing payloads and wrappers accepted by the internal
 *       {@code org.openbravo.service.datasource.DataSourceServlet}.</li>
 *   <li>Executing form initialization and change event logic when needed
 *       to compute derived or default values before persisting records.</li>
 *   <li>Merging and normalizing responses from internal datasource operations
 *       into a single JSON response for the external client.</li>
 * </ul>
 *
 * <p>Most methods in this class assume they are executed with Openbravo's
 * {@link org.openbravo.dal.core.OBContext} admin mode active; callers must
 * ensure context is appropriately set or the class methods will set/restore
 * admin mode where needed.
 */
public class DataSourceServlet implements WebService {

  private static final Logger log = LogManager.getLogger();
  public static final String _START_ROW = "_startRow";
  public static final String _END_ROW = "_endRow";
  public static final String RESPONSE = "response";
  public static final String ERROR = "error";
  public static final String STATUS = "status";
  /**
   * Charset name used for request/response character encoding.
   */
  public static final String CHARSET_UTF8 = "UTF-8";

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
      response.setCharacterEncoding(CHARSET_UTF8);
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
   * This method ensures all session variables are properly initialized for both regular
   * and headless requests. It calls {@code LoginUtils.fillSessionArguments} to initialize
   * standard session variables and then ensures format-related variables (decimal separators,
   * grouping separators, number formats, etc.) are also initialized by calling
   * {@code LoginUtils.readNumberFormat}.
   * <p>
   * For headless requests where {@code KernelServlet} is not initialized, this method
   * retrieves {@code ConfigParameters} directly from the servlet context to ensure all
   * formatting configuration is available.
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

      // Check if session variables need to be initialized
      boolean needsInitialization = variables.getJavaDateFormat() == null
              || StringUtils.isEmpty(variables.getSessionValue("#DecimalSeparator|generalQtyEdition"));

      if (needsInitialization) {
        // Initialize core session variables (user, role, client, org, etc.)
        DalConnectionProvider conn = new DalConnectionProvider(false);
        OBContext context = OBContext.getOBContext();
        org.openbravo.model.common.enterprise.Warehouse warehouse = context.getWarehouse();

        LoginUtils.fillSessionArguments(
                conn,
                variables,
                context.getUser().getId(),
                context.getLanguage().getLanguage(),
                context.isRTL() ? "Y" : "N",
                context.getRole().getId(),
                context.getCurrentClient().getId(),
                context.getCurrentOrganization().getId(),
                warehouse != null ? warehouse.getId() : null
        );

        // Ensure number format is always initialized, even in headless requests
        // First try to get ConfigParameters from KernelServlet (normal requests)
        org.openbravo.base.ConfigParameters configParams =
                org.openbravo.client.kernel.KernelServlet.getGlobalParameters();

        // If KernelServlet is not initialized (headless request), get from servlet context
        if (configParams == null) {
          configParams = org.openbravo.base.ConfigParameters.retrieveFrom(request.getServletContext());
        }

        // Read all format configurations (numbers, dates, etc.)
        if (configParams != null) {
          LoginUtils.readNumberFormat(variables, configParams.getFormatPath());
          log.debug("Session variables and number formats initialized for request.");
        } else {
          log.warn("ConfigParameters not available - some formatting may not work correctly.");
        }

      } else {
        log.debug("Session variables already initialized.");
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
      executeUpsert(method, path, request, response);
    } catch (CalloutExecutionException e) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Callout Error",
          e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
    } catch (FormInitializationException e) {
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Form Initialization Error",
          e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during form initialization.");
    } catch (PayloadPostException e) {
      handlePayloadPostException(response, e);
    } catch (BatchUpdateException e) {
      String message = OBMessageUtils.messageBD("ETRX_BatchUpdateException");
      if (e.getMessage() != null) {
        message += ": " + e.getMessage();
      }
      sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Database Error", message);
    } catch (Exception e) {
      // preserve logging + internal error handling
      e.printStackTrace();
      log.error(DataSourceConstants.ERROR_IN_DATA_SOURCE_SERVLET, e);
      handleInternalServerError(response, e);
    } catch (OpenAPINotFoundThrowable e) {
      handleOpenApiNotFound(response);
    }
  }

  /**
   * Writes the message from a {@link PayloadPostException} directly to the
   * response using a 400 (Bad Request) HTTP status. This preserves the
   * original behavior where the exception's payload is sent back to the client
   * as-is.
   *
   * @param response
   *     the HttpServletResponse used to write the payload
   * @param e
   *     the PayloadPostException containing the payload to return
   * @throws OBException
   *     when writing the response fails (wrapped IOException)
   */
  private void handlePayloadPostException(HttpServletResponse response, PayloadPostException e) {
    try {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
      response.setCharacterEncoding(CHARSET_UTF8);
      response.getWriter().write(e.getMessage());
      response.getWriter().flush();
    } catch (IOException ex) {
      throw new OBException(ex);
    }
  }

  /**
   * Handles a case where the OpenAPI resource is not found by delegating to
   * {@link #handleNotFoundException(HttpServletResponse)} and wrapping IO
   * errors in an {@link OBException}.
   *
   * @param response
   *     the HttpServletResponse used to write the 404 payload
   * @throws OBException
   *     if writing the response fails
   */
  private void handleOpenApiNotFound(HttpServletResponse response) {
    try {
      handleNotFoundException(response);
    } catch (IOException ex) {
      throw new OBException(ex);
    }
  }

  /**
   * Extracted core logic for upserting entities. This method contains the
   * original workflow for resolving the tab, building the request wrapper and
   * delegating to the internal DataSource servlet. Exceptions are propagated
   * to the caller so they can be handled in a single place without changing
   * the original behavior.
   *
   * @param method
   *     the HTTP method being handled (e.g. {@code "POST"} or {@code "PUT"})
   * @param path
   *     the request path (HttpServletRequest.getPathInfo())
   * @param request
   *     the original HttpServletRequest
   * @param response
   *     the original HttpServletResponse
   * @throws Exception
   *     propagated exceptions thrown by downstream processing
   * @throws OpenAPINotFoundThrowable
   *     when the requested OpenAPI resource cannot be found
   */
  private void executeUpsert(String method, String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception, OpenAPINotFoundThrowable {
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
  }

  /* Helper to reduce duplication while preserving behavior */
  private void sendJsonError(HttpServletResponse response, int httpStatus, String errorTitle, String message) {
    try {
      response.setStatus(httpStatus);
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
      response.setCharacterEncoding(CHARSET_UTF8);
      JSONObject jsonErrorResponse = new JSONObject();
      jsonErrorResponse.put(DataSourceConstants.ERROR, errorTitle);
      jsonErrorResponse.put(DataSourceConstants.MESSAGE, message);
      // existing code used numeric 400 in JSON for bad requests; keep same numeric value for consistency
      jsonErrorResponse.put(STATUS, 400);
      response.getWriter().write(jsonErrorResponse.toString());
      response.getWriter().flush();
    } catch (IOException | JSONException ex) {
      throw new OBException(ex);
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

    EtendoRequestWrapper newRequest;
    HttpServletResponse wrappedResponse;

    JSONObject payLoad = createPayLoad(request, payload);
    JSONObject dataFromOriginalRequest = payLoad.getJSONObject(DataSourceConstants.DATA);
    String idToLock = DataSourceUtils.getParentId(tab, dataFromOriginalRequest);

    if (idToLock != null) {
      try (LockManager.LockLease lease = LockManager.lock(idToLock)) {
        log.debug(
            MessageFormat.format("Acquired lock for session ID: {0}. at {1}", idToLock, System.currentTimeMillis()));
        try {
          newRequest = getEtendoPostWrapper(request, tab,
              payLoad, fieldList, newUri);
          wrappedResponse = new EtendoResponseWrapper(response);
          log.debug(MessageFormat.format("Processing payload with lock for session ID: {0}", idToLock));
          servlet.doPost(newRequest, wrappedResponse);
          log.debug(
              MessageFormat.format("Released lock for session ID: {0}. at {1}", idToLock, System.currentTimeMillis()));
        } catch (Exception e) {
          log.error("Error processing POST request", e);
          throw e;
        }
      }
    } else {
      try {
        log.debug("Processing payload without lock (lockId is null)");
        newRequest = getEtendoPostWrapper(request, tab,
            payLoad, fieldList, newUri);
        wrappedResponse = new EtendoResponseWrapper(response);
        servlet.doPost(newRequest, wrappedResponse);
      } catch (Exception e) {
        log.error("Error processing POST request", e);
        throw e;
      }
    }

    String content = ((EtendoResponseWrapper) wrappedResponse).getCapturedContent().toString();
    JSONObject jsonContent = new JSONObject(content);
    status = saveResponse(jsonData, status, jsonContent);

    return status;
  }

  /**
   * Saves response data and errors from a captured datasource JSON response into the provided
   * jsonData array, and computes the resulting status.
   *
   * <p>Behavior:
   * - If the captured jsonContent contains a top-level "response" object with a numeric
   * "status" property, the returned status will be set to that value.
   * - If after reading the status its value is -1 and the "response" object contains an
   * "error" object, a PayloadPostException is thrown containing the error details.
   * - If jsonContent contains a response with a "data" array, and that array is non-empty,
   * the first element is appended to jsonData.
   * - If the response contains "error" or "errors" properties, the status is set to -1 and the
   * corresponding object is appended to jsonData.
   * <p>
   * This method centralizes the logic for combining multiple internal datasource responses when
   * processing batched payloads: it returns the computed status and appends any useful data or
   * error objects into the provided jsonData container for the client-facing response.
   *
   * @param jsonData
   *     the JSONArray to which response data or errors will be appended
   * @param status
   *     the incoming status value (0 for success). May be updated and returned
   * @param jsonContent
   *     the full JSON content captured from the internal datasource servlet response
   * @return the computed status after inspecting jsonContent
   * @throws JSONException
   *     if JSON parsing or access fails
   * @throws PayloadPostException
   *     when the response status is -1 and contains an error object
   */
  private static int saveResponse(JSONArray jsonData, int status, JSONObject jsonContent) throws JSONException {
    if (jsonContent.has(RESPONSE) && jsonContent.getJSONObject(RESPONSE).has(STATUS)) {
      status = jsonContent.getJSONObject(RESPONSE).getInt(STATUS);
    }
    if (status == -1 && jsonContent.has(RESPONSE) && jsonContent.getJSONObject(RESPONSE).has(ERROR)) {
      throw new PayloadPostException(jsonContent.getJSONObject(RESPONSE).getJSONObject(ERROR).toString());
    }

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
      if (responseContent.has(DataSourceConstants.ERRORS)) {
        status = -1;
        jsonData.put(responseContent.getJSONObject(DataSourceConstants.ERRORS));
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
    jsonResponse.put(STATUS, status);
    response.setStatus(status == -1 ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_OK);
    response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
    response.setCharacterEncoding(CHARSET_UTF8);
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
    EtendoRequestWrapper newRequest;

    JSONObject payLoad = createPayLoad(request, jsonBody);
    String idToLock = getIdToLockForPut(path, payLoad);

    if (idToLock != null) {
      try (LockManager.LockLease lease = LockManager.lock(idToLock)) {
        try {
          log.debug(
              MessageFormat.format("Acquired lock for session ID: {0}. at {1}", idToLock, System.currentTimeMillis()));
          log.debug(MessageFormat.format("Processing PUT request with lock for session ID: {0}", idToLock));
          newRequest = getEtendoPutWrapper(request, response,
              payLoad, fieldList, newUri, path);
          servlet.doPut(newRequest, response);
          log.debug(
              MessageFormat.format("Released lock for session ID: {0}. at {1}", idToLock, System.currentTimeMillis()));
        } catch (Exception e) {
          log.error("Error processing PUT request", e);
          throw e;
        }
      }
    } else {
      try {
        log.debug("Processing PUT request without lock (lockId is null)");
        newRequest = getEtendoPutWrapper(request, response,
            payLoad, fieldList, newUri, path);
        servlet.doPut(newRequest, response);
      } catch (Exception e) {
        log.error("Error processing PUT request", e);
        throw e;
      }
    }
  }

  /**
   * Determines the identifier to use for locking operations for PUT requests.
   * <p>
   * This method tries to extract the ID from the request path first (the usual
   * REST-style URI where the second segment is the resource ID). If the path
   * does not contain an ID, it falls back to looking for an "id" property in
   * the provided JSON payload.
   * <p>
   * Behavior details:
   * - If the path contains an ID (extractedParts length &gt; 1), that value is returned.
   * - Otherwise, if the JSON payload contains an "id" field, that value is returned.
   * - If neither source provides an ID, the method returns null. Callers should
   * treat a null result as "no locking required" for that request.
   *
   * @param path
   *     the request path used to extract data source and optional ID
   * @param dataFromOriginalRequest
   *     the JSON payload from the request
   * @return the lock id to use for synchronizing PUT operations, or {@code null}
   *     when no id could be determined
   * @throws JSONException
   *     when JSON parsing fails while reading the payload
   */
  private static String getIdToLockForPut(String path, JSONObject dataFromOriginalRequest) throws JSONException {
    String idToLock = null;
    String[] extractedParts = DataSourceUtils.extractDataSourceAndID(path);
    if (extractedParts.length > 1) {
      idToLock = extractedParts[1];
    }
    if (idToLock == null && (dataFromOriginalRequest.has("id"))) {
      idToLock = dataFromOriginalRequest.getString("id");
    }
    return idToLock;
  }

  /**
   * Handles an unexpected internal error encountered while processing a client
   * request and writes a JSON response describing the failure.
   *
   * <p>Behavior:
   * <ol>
   *   <li>If the exception message is valid JSON, that JSON payload is returned
   *       to the client after appending a numeric {@code status: 400} property.
   *   <li>Otherwise a standard JSON error object is returned with the keys
   *       {@code error}, {@code message} and a numeric {@code status: 400}.
   * </ol>
   *
   * <p>The method always sets the HTTP status to 500 (Internal Server Error)
   * and sets the response Content-Type to {@code application/json}. Any IO or
   * JSON parsing errors while trying to write the optional message are caught
   * and ignored in favor of the fallback standard error response.
   *
   * @param response
   *     HttpServletResponse used to write the JSON error payload
   * @param e
   *     the exception that triggered the internal error response; its
   *     {@link Throwable#getMessage() message} is used as the error body
   */
  private void handleInternalServerError(HttpServletResponse response, Exception e) {
    try {
      response.setContentType("application/json");
      response.setCharacterEncoding(CHARSET_UTF8);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      String message = e.getMessage();
      boolean catchedException = false;
      catchedException = tryWriteJsonMessage(response, message);
      if (!catchedException) {
        JSONObject jsonErrorResponse = new JSONObject();
        jsonErrorResponse.put(DataSourceConstants.ERROR, "Internal Server Error");
        jsonErrorResponse.put(DataSourceConstants.MESSAGE,
            e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
        jsonErrorResponse.put(STATUS, 400);
        response.getWriter().write(jsonErrorResponse.toString());
        response.getWriter().flush();
      }
    } catch (Exception ex) {
      throw new OBException(ex);
    }
  }

  /**
   * Attempts to interpret the {@code message} as a JSON object and write it
   * directly to the {@code response}.
   *
   * <p>If parsing succeeds the method appends a numeric {@code status: 400}
   * property to the parsed object and writes it to the response output stream;
   * it returns {@code true} to indicate that the custom JSON payload was
   * emitted. If parsing fails or writing the response throws an
   * {@link IOException}, this method returns {@code false} and leaves the
   * fallback-standard error handling to the caller.
   *
   * @param response
   *     the HttpServletResponse to write to
   * @param message
   *     the exception message to interpret as JSON
   * @return {@code true} when the message was valid JSON and was written;
   *     {@code false} otherwise
   */
  private boolean tryWriteJsonMessage(HttpServletResponse response, String message) {
    try {
      JSONObject jsonMessage = new JSONObject(message);
      jsonMessage.put(STATUS, 400);
      response.getWriter().write(jsonMessage.toString());
      response.getWriter().flush();
      return true;
    } catch (JSONException | IOException jsonException) {
      // Not a JSON message or write failed — caller will handle fallback
      return false;
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
      String reqName = dataSource[0].trim();
      crit.add(Restrictions.eq(OpenAPIRequest.PROPERTY_NAME, reqName));
      OpenAPIRequest req = (OpenAPIRequest) crit.setMaxResults(1).uniqueResult();
      if (req == null) {
        throw new OBException(String.format(OBMessageUtils.messageBD("ETRX_HeadlessEndpNF"), reqName));
      }
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
    JSONObject formInitResponse;
    try {
      formInitResponse = formInit.execute(parameters, content);
    } catch (Exception e) {
      log.error("Error during form initialization", e);
      throw new FormInitializationException(e);
    }
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
      JSONObject formInitChangeResponse;
      try {
        formInitChangeResponse = formInit.execute(parameters2, contentForChange);
      } catch (Exception e) {
        log.error("Error during form initialization for change event", e);
        throw new OBException(e);
      }
      if (formInitChangeResponse.has(RESPONSE) && formInitChangeResponse.getJSONObject(RESPONSE).has(
          ERROR)) {
        throw new CalloutExecutionException(
            formInitChangeResponse.getJSONObject(RESPONSE).getJSONObject(ERROR).getString(
                "message"));
      }
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
    if (formInitResponse.has(RESPONSE) && formInitResponse.getJSONObject(RESPONSE).has(ERROR)) {
      throw new OBException(formInitResponse.getJSONObject(RESPONSE).getJSONObject(ERROR).getString("message"));
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
   * @param dataInpFormat
   *     optional JSONObject containing the input format data with "inp" parameters to clear
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
