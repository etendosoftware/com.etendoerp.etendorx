package com.etendoerp.etendorx.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

import com.etendoerp.etendorx.openapi.OpenAPIConstants;
import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;
import com.etendoerp.etendorx.services.wrapper.EtendoResponseWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.etendorx.utils.DataSourceUtils;
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

  /**
   * Creates the payload for the POST request.
   *
   * @param request
   */
  JSONObject createPayLoad(HttpServletRequest request) {
    String csrf = "123";
    request.getSession(false).setAttribute("#CSRF_TOKEN", csrf);

    try {
      StringBuilder sb = new StringBuilder();
      try (InputStream inputStream = request.getInputStream()) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
      }
      String oldBody = sb.toString();

      JSONObject newJsonBody = new JSONObject();
      JSONObject jsonBody = new JSONObject(oldBody);

      newJsonBody.put("dataSource", "isc_OBViewDataSource_0");
      newJsonBody.put("operationType", "add");
      newJsonBody.put("componentId", "isc_OBViewForm_0");
      newJsonBody.put("csrfToken", csrf);
      newJsonBody.put(DataSourceConstants.DATA, jsonBody);
      return newJsonBody;
    } catch (JSONException | IOException e) {
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
        EtendoRequestWrapper newRequest = getEtendoPostWrapper(request, tab, createPayLoad(request), fieldList,
            newUri);
        servlet.doPost(newRequest, response);
      } else if (StringUtils.equals(OpenAPIConstants.PUT, method)) {
        EtendoRequestWrapper newRequest = getEtendoPutWrapper(request, response, createPayLoad(request), fieldList,
            newUri, path);
        servlet.doPut(newRequest, response);
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
      if (req == null || req.getETRXOpenAPITabList().isEmpty()) {
        handleNotFoundException(response);
        return null;
      }
      tab = req.getETRXOpenAPITabList().get(0).getRelatedTabs();
      for (Field field : tab.getADFieldList()) {
        String name = DataSourceUtils.getHQLColumnName(field)[0];
        fieldList.add(new RequestField(name, field.getColumn()));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return tab;
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
  private EtendoRequestWrapper getEtendoPostWrapper(HttpServletRequest request, Tab tab,
      JSONObject newJsonBody, List<RequestField> fieldList,
      String newUri) throws JSONException, IOException, OpenAPINotFoundThrowable, ScriptException, ParseException {
    JSONObject dataFromOriginalRequest = newJsonBody.getJSONObject(DataSourceConstants.DATA);
    String recordId = dataFromOriginalRequest.optString("id");

    String parentId = DataSourceUtils.getParentId(tab, dataFromOriginalRequest);


    Map<String, Object> parameters = createParameters(request, tab.getId(), parentId, recordId, null, "NEW");
    String content = "{}";


    /* the columns uses 3 name format: database column name, hql(or normalized) and input Format. So, to switch between them, we need to generate 3 maps */
    Map<String, String> norm2input = new HashMap<>();
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


    //Initialization of new record, saving the data in input format
    var formInit = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class);
    var formInitResponse = formInit.execute(parameters, content);
    DataSourceUtils.applyColumnValues(formInitResponse, dbname2input, dataFromNewRecord);

    //to proceed with Change events, we need convert the keys to normalized format to input format
    JSONObject dataInpFormat = DataSourceUtils.keyConvertion(dataFromNewRecord, norm2input);
    dataInpFormat.put("keyProperty", "id");//    "keyProperty":"id",
    dataInpFormat.put(OBBindingsConstants.WINDOW_ID_PARAM, tab.getWindow().getId());


    var a = propsToChange.keys();
    while (a.hasNext()) {// to develop, we assume that the only change is in the productID
      String changedColumnN = (String) a.next();
      logChangeEvent(changedColumnN);
      String changedColumnInp = norm2input.get(changedColumnN);
      var type = columnTypes.get(changedColumnN);
      dataInpFormat.put(changedColumnInp,
          DataSourceUtils.valueConvertToInputFormat(propsToChange.get(changedColumnN), type));
      handleColumnSelector(request, tab, dataInpFormat, changedColumnN, changedColumnInp, dbname2input);

      // suppose to change in productID
      Map<String, Object> parameters2 = createParameters(request, tab.getId(), parentId, recordId, changedColumnInp,
          "CHANGE");

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
   * Handles the column selector for a specified column.
   * <p>
   * This method processes the column selector for the specified column, evaluates the filter clause,
   * and updates the data input format with the selected values.
   *
   * @param request
   *     The HttpServletRequest object.
   * @param tab
   *     The Tab object associated with the selector.
   * @param dataInpFormat
   *     The JSON object containing the data input format.
   * @param changedColumnN
   *     The name of the column that has changed.
   * @param changedColumnInp
   *     The input format name of the column that has changed.
   * @param db2Input
   *     A map of database column names to input format names.
   * @throws JSONException
   *     If there is an error during JSON processing.
   * @throws ScriptException
   *     If there is an error during the evaluation of the filter expression.
   */
  private void handleColumnSelector(HttpServletRequest request, Tab tab, JSONObject dataInpFormat,
      String changedColumnN, String changedColumnInp,
      Map<String, String> db2Input) throws JSONException, ScriptException {
    try {
      OBContext.setAdminMode();
      Column col = getColumnByHQLName(tab, changedColumnN);
      if (col == null) {
        throw new OBException(OBMessageUtils.messageBD("ETRX_ColumnNotFound"));
      }
      if (!StringUtils.equals(col.getReference().getId(), "30")) {
        return;
      }  //is type search.
      Reference reference = col.getReferenceSearchKey();
      if (reference.getOBUISELSelectorList().isEmpty()) {
        throw new OBException(OBMessageUtils.messageBD("ETRX_ReferenceNotFound"));
      }
      org.openbravo.model.ad.domain.Selector selectorValidation = reference.getADSelectorList().get(0);
      Selector selectorDefined = reference.getOBUISELSelectorList().get(0);
      DefaultDataSourceService dataSourceService = new DefaultDataSourceService();
      HashMap<String, String> convertToHashMAp = convertToHashMAp(dataInpFormat);
      OBDal.getInstance().refresh(selectorDefined);
      convertToHashMAp.put("_entityName", selectorDefined.getTable().getJavaClassName());
      String whereClauseAndFilters = selectorDefined.getHQLWhereClause() + addFilterClause(selectorDefined,
          convertToHashMAp, tab, request);
      whereClauseAndFilters = fullfillSessionsVariables(whereClauseAndFilters, db2Input, dataInpFormat);
      convertToHashMAp.put("whereAndFilterClause", whereClauseAndFilters);
      convertToHashMAp.put("dataSourceName", selectorDefined.getTable().getJavaClassName());
      convertToHashMAp.put("_selectorDefinitionId", selectorDefined.getId());
      convertToHashMAp.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
      convertToHashMAp.put("IsSelectorItem", "true");
      convertToHashMAp.put("_extraProperties", getExtraProperties(selectorDefined));
      int iterations = 0;
      // we will search this record id
      String recordID = dataInpFormat.getString(changedColumnInp);
      //find the column of the table, that determines the "column" where the data is stored. For example, in the case of
      // a selector of "M_Product", the value stored in the column "M_Product_ID"
      Column valueColumn = getValueColumn(selectorValidation, selectorDefined);
      // ask for the name of the propertie where the record id is stored in the results
      String valuePropertie = DataSourceUtils.getHQLColumnName(valueColumn)[0];

      JSONObject obj = null;
      int totalRows = -1;
      int endRow = -1;

      while (obj == null && (totalRows == -1 || endRow < totalRows)) {
        convertToHashMAp.put(_START_ROW, String.valueOf(0 + (iterations * 100)));
        endRow = 100 + (iterations * 100);
        convertToHashMAp.put(_END_ROW, String.valueOf(endRow));
        String result = dataSourceService.fetch(convertToHashMAp);
        JSONObject resultJson = new JSONObject(result);
        if (totalRows == -1) {
          totalRows = resultJson.getJSONObject(RESPONSE).getInt("totalRows");
        }
        var arr = resultJson.getJSONObject(RESPONSE).getJSONArray("data");

        for (int i = 0; i < arr.length(); i++) {
          JSONObject current = arr.getJSONObject(i);
          if (StringUtils.equals(current.getString(valuePropertie), recordID)) {
            obj = current;
            break;
          }
        }
        iterations++;
      }
      if (obj == null) {
        log.error("Record not found in selector");
        throw new OBException("Record " + recordID + " not found in Search selector execution.");
      }

      savePrefixFields(dataInpFormat, changedColumnInp, selectorDefined, obj);


    } catch (Exception e) {
      log.error("Error in handleColumnSelector", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Saves the prefix fields from the selector into the data input format.
   * <p>
   * This method iterates over the outfields of the selector and adds their values to the data input format.
   *
   * @param dataInpFormat
   *     The JSON object containing the data input format.
   * @param changedColumnInp
   *     The input format name of the column that has changed.
   * @param selectorDefined
   *     The defined selector object.
   * @param obj
   *     The JSON object containing the selector field values.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  private static void savePrefixFields(JSONObject dataInpFormat, String changedColumnInp, Selector selectorDefined,
      JSONObject obj) throws JSONException {
    List<SelectorField> selectorFieldList = selectorDefined.getOBUISELSelectorFieldList();
    selectorFieldList = selectorFieldList.stream().filter(SelectorField::isOutfield).collect(Collectors.toList());
    for (SelectorField selectorField : selectorFieldList) {
      String normN = StringUtils.isNotEmpty(
          selectorField.getProperty()) ? selectorField.getProperty() : selectorField.getName();
      normN = normN.replace(".", "$");
      if (obj.has(normN)) {
        if (!StringUtils.isEmpty(selectorField.getSuffix())) {
          dataInpFormat.put(changedColumnInp + selectorField.getSuffix(), obj.get(normN));
        } else {
          dataInpFormat.put(DataSourceUtils.getInpName(selectorField.getColumn()), obj.get(normN));
        }
      }
    }
  }

  /**
   * Retrieves the column by its HQL name from the given tab.
   * <p>
   * This method iterates over the columns of the tab and returns the column that matches the given HQL name.
   *
   * @param tab
   *     The Tab object containing the columns.
   * @param changedColumnN
   *     The HQL name of the column to be retrieved.
   * @return The Column object that matches the given HQL name, or null if not found.
   */
  private static Column getColumnByHQLName(Tab tab, String changedColumnN) {
    Column col = null;
    List<Column> adColumnList = DataSourceUtils.getAdColumnList(tab);
    for (Column column : adColumnList) {
      if (StringUtils.equals((DataSourceUtils.getHQLColumnName(column))[0], changedColumnN)) {
        col = column;
        break;
      }
    }
    return col;
  }

  /**
   * Retrieves the value column for the given selector.
   * <p>
   * This method determines the value column based on the provided selector. If the selector is defined
   * and has a value field that is not a custom query, it returns the value field's column. Otherwise,
   * it returns the column from the selector validation.
   *
   * @param selectorValidation
   *     The selector validation object.
   * @param selectorDefined
   *     The defined selector object.
   * @return The value column for the given selector.
   */
  private static Column getValueColumn(org.openbravo.model.ad.domain.Selector selectorValidation,
      Selector selectorDefined) {
    if (selectorDefined != null && selectorDefined.getValuefield() != null && !selectorDefined.isCustomQuery()) {
      return selectorDefined.getValuefield().getColumn();
    } else {
      return selectorValidation.getColumn();
    }
  }

  /**
   * Replaces session variables in the where clause and filters.
   * <p>
   * This method replaces placeholders in the where clause and filters with actual values from the provided
   * data input format.
   *
   * @param whereClauseAndFilters
   *     The where clause and filters containing placeholders.
   * @param db2Input
   *     A map of database column names to input format names.
   * @param dataInpFormat
   *     The JSON object containing the data input format.
   * @return The where clause and filters with placeholders replaced by actual values.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  private String fullfillSessionsVariables(String whereClauseAndFilters, Map<String, String> db2Input,
      JSONObject dataInpFormat) throws JSONException {
    String result = whereClauseAndFilters;
    for (Map.Entry<String, String> entry : db2Input.entrySet()) {
      if (dataInpFormat.has(entry.getValue())) {
        result = StringUtils.replaceIgnoreCase(result, "@" + entry.getKey() + "@",
            String.format("'%s'", dataInpFormat.get(entry.getValue())));
      }
    }
    return result;
  }

  /**
   * Retrieves the extra properties for the given selector.
   * <p>
   * This method collects the extra properties for the given selector, including the value field and outfields,
   * and returns them as a comma-separated string.
   *
   * @param selector
   *     The selector object.
   * @return A comma-separated string of extra properties for the given selector.
   */
  private static String getExtraProperties(Selector selector) {
    return selector.getOBUISELSelectorFieldList().stream().filter(
        sf -> selector.getValuefield() == sf || sf.isOutfield()).sorted(
        Comparator.comparing(SelectorField::getSortno)).map(
        sf -> StringUtils.replace(sf.getProperty(), ".", "$")).collect(Collectors.joining(","));
  }

  /**
   * Adds a filter clause to the selector's query.
   * <p>
   * This method checks if the selector has a filter clause that uses OB. If it does,
   * the filter clause is evaluated in JavaScript and appended to the query.
   *
   * @param selector
   *     The selector object containing the filter expression.
   * @param hs1
   *     A map of parameters to be used in the filter expression.
   * @param tab
   *     The tab object associated with the selector.
   * @param request
   *     The HttpServletRequest object.
   * @return The filter clause to be appended to the query.
   * @throws ScriptException
   *     If there is an error during the evaluation of the filter expression.
   */
  private String addFilterClause(Selector selector, HashMap<String, String> hs1, Tab tab,
      HttpServletRequest request) throws ScriptException {
    // Check if the selector has a filter clause and it uses OB., because we need to evaluate in JS.
    var result = (String) ParameterUtils.getJSExpressionResult(hs1, request.getSession(),
        selector.getFilterExpression());
    if (StringUtils.isNotEmpty(result)) {
      return " AND " + result;
    }
    return result;
  }

  /**
   * Converts a JSONObject to a HashMap.
   * <p>
   * This method iterates over the keys of the provided JSONObject and adds them to a HashMap.
   *
   * @param dataInpFormat
   *     The JSONObject to be converted.
   * @return A HashMap containing the key-value pairs from the JSONObject.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  private HashMap<String, String> convertToHashMAp(JSONObject dataInpFormat) throws JSONException {
    HashMap<String, String> map = new HashMap<>();
    var keys = dataInpFormat.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      map.put(key, dataInpFormat.get(key).toString());
    }
    return map;
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
    Map<String, String> norm2input = new HashMap<>();
    Map<String, String> input2norm = new HashMap<>();
    Map<String, String> dbname2input = new HashMap<>();
    Map<String, String> columnTypes = new HashMap<>();

    DataSourceUtils.loadCaches(fieldList, norm2input, input2norm, dbname2input, columnTypes);

    //invoinv the formInit to get the data in input format, beign the base of the new data and the change events
    //we need to execute the forminit in mode EDIT
    Map<String, Object> parameters = createParameters(request,
        DataSourceUtils.getTabByDataSourceName(extractedParts[0]).getId(), null, recordId, null, "EDIT");

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

    //to proceed with Change events, we need to iterate over the keys of newData, setting the values in dataInpFormat and calling the formInit
    var columnToChange = newData.keys();
    while (columnToChange.hasNext()) {
      String changedColumnN = (String) columnToChange.next();
      if (StringUtils.equalsIgnoreCase(changedColumnN, "id")) {
        //we don't need to change the id
        continue;
      }
      logChangeEvent(changedColumnN);
      String changedColumnInp = norm2input.get(changedColumnN);
      String type = columnTypes.get(changedColumnN);
      String valueInpFormat = DataSourceUtils.valueConvertToInputFormat(newData.get(changedColumnN), type);
      dataInpFormat.put(changedColumnInp, valueInpFormat);
      handleColumnSelector(request, DataSourceUtils.getTabByDataSourceName(extractedParts[0]), dataInpFormat,
          changedColumnN, changedColumnInp, dbname2input);
      // suppose to change in productID
      Map<String, Object> parameters2 = createParameters(request,
          DataSourceUtils.getTabByDataSourceName(extractedParts[0]).getId(), null, recordId, changedColumnInp,
          "CHANGE");
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
}
