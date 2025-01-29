package com.etendoerp.etendorx.services;

import com.etendoerp.etendorx.openapi.OpenAPIConstants;
import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;
import com.etendoerp.etendorx.services.wrapper.EtendoResponseWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.openapi.data.OpenAPIRequest;
import com.smf.securewebservices.rsql.OBRestUtils;

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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Servlet that handles data source requests.
 */
public class DataSourceServlet implements WebService {

  private static final Logger log = LogManager.getLogger();

  /**
   * Gets the DataSourceServlet instance.
   *
   * @return the DataSourceServlet instance
   */
  private static org.openbravo.service.datasource.DataSourceServlet getDataSourceServlet() {
    return WeldUtils.getInstanceFromStaticBeanManager(
        org.openbravo.service.datasource.DataSourceServlet.class);
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
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      OBContext.setAdminMode();
      DalConnectionProvider conn = new DalConnectionProvider();
      RoleDefaults defaults = LoginUtils.getLoginDefaults(
          OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          conn);

      LoginUtils.fillSessionArguments(conn, new VariablesSecureApp(request),
          OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getLanguage().getLanguage(),
          OBContext.getOBContext().isRTL() ? "Y" : "N", defaults.role, defaults.client,
          OBContext.getOBContext().getCurrentOrganization().getId(), defaults.warehouse);
      String[] extractedParts = extractDataSourceAndID(path);
      String dataSourceName = convertURI(extractedParts);

      String rsql = request.getParameter("q");
      Map<String, String[]> params = new HashMap<>();
      var paramNames = request.getParameterNames();
      while (paramNames.hasMoreElements()) {
        String param = paramNames.nextElement();
        if (!StringUtils.equals(param, "q")) {
          params.put(param, new String[]{ request.getParameter(param) });
        }
      }
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
      if (!params.containsKey("_startRow")) {
        params.put("_startRow", new String[]{ "0" });
      }
      if (!params.containsKey("_endRow")) {
        params.put("_endRow", new String[]{ "100" });
      }
      String dtsn = extractedParts[0];
      Tab tabByDataSourceName = getTabByDataSourceName(dtsn);
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
                DataSourceConstants.RESPONSE)
            .has(DataSourceConstants.ERROR) && capturedResponse.getJSONObject(
                DataSourceConstants.RESPONSE)
            .getJSONObject(DataSourceConstants.ERROR)
            .has(DataSourceConstants.MESSAGE)) {
          message = capturedResponse.getJSONObject(DataSourceConstants.RESPONSE)
              .getJSONObject(DataSourceConstants.ERROR)
              .getString(DataSourceConstants.MESSAGE);
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
    response.getWriter()
        .write(
            "{\"error\": \"Not Found\", \"message\": \"The requested resource was not found.\"}");
    response.getWriter().flush();
  }

  /**
   * Creates the payload for the POST request.
   *
   * @param request
   */
  private JSONObject createPayLoad(HttpServletRequest request) {
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
  private void upsertEntity(String method, String path, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      List<RequestField> fieldList = new ArrayList<>();
      Tab tab = getTab(path, request, response, fieldList);
      if (tab == null) {
        handleNotFoundException(response);
        return;
      }
      String newUri = convertURI(extractDataSourceAndID(path));

      var servlet = getDataSourceServlet();

      if (StringUtils.equals(OpenAPIConstants.POST, method)) {
        EtendoRequestWrapper newRequest = getEtendoPostWrapper(method, request, tab,
            createPayLoad(request), fieldList, newUri);
        servlet.doPost(newRequest, response);
      } else if (StringUtils.equals(OpenAPIConstants.PUT, method)) {
        EtendoRequestWrapper newRequest = getEtendoPutWrapper(request, response,
            createPayLoad(request), fieldList, newUri, path);
        servlet.doPut(newRequest, response);
      } else {
        throw new UnsupportedOperationException("Method not supported: " + method);
      }
    } catch (Exception e) {
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
      List<RequestField> fieldList)
      throws ServletException, DefaultValidationException, IOException {
    Tab tab;
    try {
      var dataSource = extractDataSourceAndID(path);
      OBContext.setAdminMode();
      DalConnectionProvider conn = new DalConnectionProvider();
      RoleDefaults defaults = LoginUtils.getLoginDefaults(
          OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          conn);

      LoginUtils.fillSessionArguments(conn, new VariablesSecureApp(request),
          OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getLanguage().getLanguage(),
          OBContext.getOBContext().isRTL() ? "Y" : "N", defaults.role, defaults.client,
          OBContext.getOBContext().getCurrentOrganization().getId(), defaults.warehouse);

      OBCriteria<OpenAPIRequest> crit = OBDal.getInstance().createCriteria(OpenAPIRequest.class);
      crit.add(Restrictions.eq("name", dataSource[0]));
      OpenAPIRequest req = (OpenAPIRequest) crit.setMaxResults(1).uniqueResult();
      if (req == null || req.getETRXOpenAPITabList().isEmpty()) {
        handleNotFoundException(response);
        return null;
      }
      tab = req.getETRXOpenAPITabList().get(0).getRelatedTabs();
      for (Field field : tab.getADFieldList()) {
        String name = normalizedName(field.getName());
        fieldList.add(new RequestField(name, field.getColumn().getDBColumnName()));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return tab;
  }

  /**
   * Creates the payload for the POST request.
   *
   * @param method
   * @param request
   * @param tab
   * @param newJsonBody
   * @param fieldList
   * @param newUri
   * @throws JSONException
   * @throws IOException
   * @throws OpenAPINotFoundThrowable
   */
  private EtendoRequestWrapper getEtendoPostWrapper(String method, HttpServletRequest request,
      Tab tab, JSONObject newJsonBody, List<RequestField> fieldList, String newUri)
      throws JSONException, IOException, OpenAPINotFoundThrowable, ScriptException {
    JSONObject dataFromOriginalRequest = newJsonBody.getJSONObject(DataSourceConstants.DATA);
    String recordId = dataFromOriginalRequest.optString("id");

    String parentId = getParentId(tab, dataFromOriginalRequest);


    Map<String, Object> parameters = createParameters(method, request, tab.getId(), parentId, recordId,
        null);
    String content = "{}";


    /* the columns uses 3 name format: database columnm name, normalized and input Format. So, to switch between them, we need to generate 3 maps */
    Map<String, String> norm2input = new HashMap<>();
    Map<String, String> input2norm = new HashMap<>();
    Map<String, String> dbname2input = new HashMap<>();
    loadCaches(fieldList, norm2input, input2norm, dbname2input);

    //Initialization
    var formInit = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class);
    var formInitResponse = formInit.execute(parameters, content);
    checkForError(formInitResponse);
    var values = formInitResponse.getJSONObject("columnValues");

    var keys = values.keys();


    //remove the parent properties and ID, to detect properties that has been "changed" to emulate the change event
    // for every property that has been changed, we need to call the formInit with the new value
    JSONObject propsToChange = new JSONObject(dataFromOriginalRequest.toString());
    List<String> parentProperties = getParentProperties(tab, dataFromOriginalRequest);
    for (String parentProperty : parentProperties) {
      propsToChange.remove(parentProperty);
    }


    //this variable will store accumulated data from the original request
    JSONObject dataFromNewRecord = new JSONObject(dataFromOriginalRequest.toString());

    while (keys.hasNext()) {
      String dbkey = (String) keys.next();
      String inpkey = dbname2input.get(dbkey);
      String normalizedKey = input2norm.get(inpkey);
      JSONObject value = values.getJSONObject(dbkey);
      Object val = getValueFromItem(value, "yyyy-MM-dd", "dd-MM-yyyy");
      if (!dataFromNewRecord.has(normalizedKey)) {
        dataFromNewRecord.put(normalizedKey, val);
      }
    }

    //to proceed with Change events, we need convert the keys to normalized format to input format
    JSONObject dataInpFormat = keyConvertion(dataFromNewRecord, norm2input);
    dataInpFormat.put("keyProperty", "id");//    "keyProperty":"id",
    dataInpFormat.put(OBBindingsConstants.WINDOW_ID_PARAM, tab.getWindow().getId());

    // to develop, we assume that the only change is in the productID
    String changedColumnN = propsToChange.keys().next().toString();
    String changedColumnInp = norm2input.get(changedColumnN);
    dataInpFormat.put(changedColumnInp, propsToChange.get(changedColumnN));
    handleColumnSelector(request, tab, dataInpFormat, changedColumnN, changedColumnInp);

    //lets check if the column is a search reference, if is, we need to load the data, because the callouts may need it


    // suppose to change in productID
    Map<String, Object> parameters2 = createParameters("PUT", request, tab.getId(), parentId, recordId,
        changedColumnInp);

    String contentForChange = dataInpFormat.toString();
    var formInitChangeResponse = formInit.execute(parameters2, contentForChange);
    if (!formInitChangeResponse.has("columnValues")) {
      checkForError(formInitChangeResponse);
    }
    values = formInitChangeResponse.getJSONObject("columnValues");

    keys = values.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      String normalizedKey = dbname2input.get(key);
      JSONObject value = values.getJSONObject(key);
      Object val = getValueFromItem(value, "dd-MM-yyyy", "yyyy-MM-dd");
      dataInpFormat.put(normalizedKey, val);

    }

    // to finally save the dataFromOriginalRequest, we need to convert the keys to normalized format
    JSONObject jsonBodyToSave = keyConvertion(dataInpFormat, input2norm);
    newJsonBody.put(DataSourceConstants.DATA, jsonBodyToSave);

    return new EtendoRequestWrapper(request, newUri, newJsonBody.toString(),
        request.getParameterMap());
  }

  private void handleColumnSelector(HttpServletRequest request, Tab tab,
      JSONObject dataInpFormat, String changedColumnN, String changedColumnInp) throws JSONException, ScriptException {
    try {
      OBContext.setAdminMode();
      Column col = null;
      List<Column> adColumnList = tab.getTable().getADColumnList();
      for (Column column : adColumnList) {
        if (StringUtils.equals(normalizedName(column.getName()), changedColumnN)) {
          col = column;
          break;
        }
      }
      if (col == null) {
        throw new OBException("Column not found"); //TODO: change this message
      }
      if (StringUtils.equals(col.getReference().getId(), "30")) { //is type search.
        Reference reference = col.getReferenceSearchKey();
        if (reference.getOBUISELSelectorList().isEmpty()) {
          throw new OBException("Reference not found"); //TODO: change this message
        }
        Selector selector = reference.getOBUISELSelectorList().get(0);
        DefaultDataSourceService dataSourceService = new DefaultDataSourceService();
        HashMap<String, String> convertToHashMAp = convertToHashMAp(dataInpFormat);
        OBDal.getInstance().refresh(selector);
        convertToHashMAp.put("_entityName", selector.getTable().getJavaClassName());
        convertToHashMAp.put("whereAndFilterClause",
            selector.getHQLWhereClause() + addFilterClause(selector, convertToHashMAp, tab, request)
        );
        //if (OB.getParameters().get('inpcCurrencyId') && (OB.getWindowId() == '207' || OB.getWindowId() == '94EAA455D2644E04AB25D93BE5157B6D')) { " e.productPrice.priceListVersion.priceList.currency.id = '" + OB.getParameters().get('inpcCurrencyId') + "'" } else if (OB.getParameters().get('inpcCurrencyId')) { " e.productPrice.priceListVersion.priceList.salesPriceList = " + OB.isSalesTransaction()  + " AND e.productPrice.priceListVersion.priceList.currency.id = '" + OB.getParameters().get('inpcCurrencyId') + "'" }
        convertToHashMAp.put("dataSourceName", selector.getTable().getJavaClassName());
        convertToHashMAp.put("_selectorDefinitionId", selector.getId());
        convertToHashMAp.put("filterClass", "org.openbravo.userinterface.selector.SelectorDataSourceFilter");
        convertToHashMAp.put("IsSelectorItem", "true");
        convertToHashMAp.put("_extraProperties", getExtraProperties(selector));
        int iterations = 0;
        // we will search this record id
        String recordID = dataInpFormat.getString(changedColumnInp);
        // ask for the name of the propertie where the record id is stored in the results
        String valuePropertie = normalizedName(selector.getValuefield().getColumn().getName());
        String valuePropertieDB = selector.getValuefield().getColumn().getDBColumnName();

        JSONObject obj = null;
        while (true) {
          convertToHashMAp.put("_startRow", String.valueOf(0 + (iterations * 100)));
          convertToHashMAp.put("_endRow", String.valueOf(100 + (iterations * 100)));
          String result = dataSourceService.fetch(convertToHashMAp);
          JSONObject resultJson = new JSONObject(result);
          var arr = resultJson.getJSONObject("response").getJSONArray("data");

          for (int i = 0; i < arr.length(); i++) {
            obj = arr.getJSONObject(i);
            if (StringUtils.equals(obj.getString(valuePropertie), recordID)) {
              break;
            }
          }

          log.info(resultJson.toString(2));
          break;
        }
        List<SelectorField> selectorFieldList = selector.getOBUISELSelectorFieldList();
        selectorFieldList = selectorFieldList.stream().filter(
            SelectorField::isOutfield
        ).collect(Collectors.toList());
        for (SelectorField selectorField : selectorFieldList) {
          String normN = selectorField.getProperty().replace(".", "$");
          if (obj.has(normN)) {
            log.info("El objeto encontrado, tiene la propiedad: {} con valor: {}", normN, obj.get(normN));
            if (!StringUtils.isEmpty(selectorField.getSuffix())) {
              dataInpFormat.put(changedColumnInp + selectorField.getSuffix(), obj.get(normN));
            } else {
              dataInpFormat.put("inp" + Sqlc.TransformaNombreColumna(selectorField.getColumn().getDBColumnName()),
                  obj.get(normN));
            }
          }
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String getExtraProperties(Selector selector) {
    return selector.getOBUISELSelectorFieldList().stream().filter(
            sf -> selector.getValuefield() == sf || sf.isOutfield()
        ).sorted(Comparator.comparing(SelectorField::getSortno))
        .map(sf -> StringUtils.replace(sf.getProperty(), ".", "$"))
        .collect(Collectors.joining(","));

  }

  private String addFilterClause(Selector selector, HashMap<String, String> hs1, Tab tab,
      HttpServletRequest request) throws ScriptException {
    //check if the selector has a filter clause and it uses OB., because we need to evalueate in JS.
    var result = (String) ParameterUtils.getJSExpressionResult(hs1, request.getSession(),
        selector.getFilterExpression());
    if (StringUtils.isNotEmpty(result)) {
      return " AND " + result;
    }
    return result;
  }

  private HashMap<String, String> convertToHashMAp(JSONObject dataInpFormat) throws JSONException {
    HashMap<String, String> map = new HashMap<>();
    var keys = dataInpFormat.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      map.put(key, dataInpFormat.get(key).toString());
    }
    return map;
  }

  private static Object getValueFromItem(JSONObject item
      , String patternDateFrom, String patternDateTo
  ) throws JSONException {
    //if the item has a property value with type long, use this value, if not use the classicValue. We dont know the type of the value
    if (item.has("value")) {
      Object value = item.get("value");
      if (value instanceof Long || value instanceof Integer || value instanceof Double) {
        return value;
      }
      if (!(value instanceof String)) {
        return value.toString();
      }
      //checkif the value is a date in patternDateFrom, if so, convert it to patternDateTo
      // for example, if the date is in format yyyy-MM-dd, and we need it in format dd-MM-yyyy
      SimpleDateFormat sdfFrom = new SimpleDateFormat(patternDateFrom);
      SimpleDateFormat sdfTo = new SimpleDateFormat(patternDateTo);
      try {
        return sdfTo.format(sdfFrom.parse(value.toString()));
      } catch (Exception e) {
        return value;
      }
    }
    return item.has("classicValue") ? item.get("classicValue") : null;
  }

  private void checkForError(JSONObject formInitResponse) throws JSONException {
    if (formInitResponse.has("response") && formInitResponse.getJSONObject("response").has("error")) {
      throw new OBException(formInitResponse.getJSONObject("response").getJSONObject("error").getString("message"));
    }
  }

  /**
   * Converts the keys of a JSONObject using a provided map for conversion.
   * <p>
   * This method iterates over the keys of the given JSONObject and converts each key using the provided map.
   * If a key has a corresponding new key in the map, the value is put in the new key. Otherwise, the original key is used.
   *
   * @param data
   *     The original JSONObject whose keys need to be converted.
   * @param mapForConvertion
   *     A map containing the original keys and their corresponding new keys.
   * @return A new JSONObject with the keys converted.
   * @throws JSONException
   *     If there is an error during JSON processing.
   */
  private JSONObject keyConvertion(JSONObject data, Map<String, String> mapForConvertion) throws JSONException {
    // Receives the data and the map to convert the keys
    JSONObject newData = new JSONObject();
    var it = data.keys();
    while (it.hasNext()) {
      String key = (String) it.next();
      String newKey = mapForConvertion.get(key);
      if (newKey != null) {
        newData.put(newKey, data.get(key));
      } else {
        newData.put(key, data.get(key));
      }
    }
    return newData;
  }

  /**
   * Loads caches for normalized and input format keys.
   * <p>
   * This method populates three maps with normalized, input format, and database column name to input format key mappings based on the provided field list.
   *
   * @param fieldList
   *     A list of RequestField objects containing field information.
   * @param norm2input
   *     A map to store normalized to input format key mappings.
   * @param input2norm
   *     A map to store input to normalized format key mappings.
   * @param dbname2input
   *     A map to store database column name to input format key mappings.
   */
  private void loadCaches(List<RequestField> fieldList, Map<String, String> norm2input,
      Map<String, String> input2norm, Map<String, String> dbname2input) {
    for (RequestField field : fieldList) {
      String columnName = field.getDBColumnName();
      String normalizedName = normalizedName(field.getName());
      String inpName = getInpName(columnName);
      norm2input.put(normalizedName, inpName);
      input2norm.put(inpName, normalizedName);
      dbname2input.put(columnName, inpName);
    }
  }

  /**
   * Gets the input name for a given column name.
   * <p>
   * This method transforms a column name into its corresponding input name format.
   *
   * @param columnName
   *     The column name to be transformed.
   * @return The input name corresponding to the given column name.
   */
  private static String getInpName(String columnName) {
    return "inp" + Sqlc.TransformaNombreColumna(columnName);
  }

  /**
   * Retrieves the parent ID from the given tab and data.
   * <p>
   * This method finds the parent ID by checking the data properties of the parent columns in the tab.
   *
   * @param tab
   *     The Tab object containing field information.
   * @param data
   *     The JSONObject containing data to be checked for parent properties.
   * @return The parent ID if found, otherwise null.
   */
  private String getParentId(Tab tab, JSONObject data) {
    try {
      OBContext.setAdminMode(false);
      List<String> dataPropertiesOfParents = tab.getADFieldList().stream().filter(
          field -> field.getColumn() != null && field.getColumn().isLinkToParentColumn()
      ).map(field -> normalizedName(field.getName())).collect(Collectors.toList());
      for (String parentProperty : dataPropertiesOfParents) {
        if (data.has(parentProperty)) {
          return data.optString(parentProperty);
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Retrieves the parent ID from the given tab and data.
   * <p>
   * This method finds the parent ID by checking the data properties of the parent columns in the tab.
   *
   * @param tab
   *     The Tab object containing field information.
   * @param data
   *     The JSONObject containing data to be checked for parent properties.
   * @return The parent ID if found, otherwise null.
   */
  private List<String> getParentProperties(Tab tab, JSONObject data) {
    try {
      OBContext.setAdminMode(false);
      return tab.getADFieldList().stream().filter(
          field -> field.getColumn() != null && field.getColumn().isLinkToParentColumn()
      ).map(field -> normalizedName(field.getName())).collect(Collectors.toList());
    } finally {
      OBContext.restorePreviousMode();
    }
  }


  /**
   * Creates the payload for the PUT request.
   *
   * @param request
   * @param response
   * @param newJsonBody
   * @param fieldList
   * @param newUri
   * @param path
   * @throws JSONException
   * @throws IOException
   * @throws ServletException
   * @throws OpenAPINotFoundThrowable
   */
  private EtendoRequestWrapper getEtendoPutWrapper(HttpServletRequest request,
      HttpServletResponse response, JSONObject newJsonBody, List<RequestField> fieldList,
      String newUri, String path)
      throws JSONException, IOException, ServletException, OpenAPINotFoundThrowable {
    String getURI = convertURI(extractDataSourceAndID(path));
    var newRequest = new EtendoRequestWrapper(request, getURI, "", request.getParameterMap());
    var newResponse = new EtendoResponseWrapper(response);
    getDataSourceServlet().doGet(newRequest, newResponse);
    JSONObject capturedResponse = newResponse.getCapturedContent();
    JSONObject values = capturedResponse.getJSONObject(DataSourceConstants.RESPONSE)
        .getJSONArray(DataSourceConstants.DATA)
        .getJSONObject(0);
    JSONObject data = newJsonBody.getJSONObject(DataSourceConstants.DATA);
    var keys = values.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      String normalizedKey = normalizedKey(fieldList, key);
      Object value = values.get(key);
      if (!data.has(normalizedKey)) {
        data.put(normalizedKey, value);
      }
    }
    return new EtendoRequestWrapper(request, newUri, newJsonBody.toString(),
        request.getParameterMap());
  }

  /**
   * Creates the parameters for the request.
   *
   * @param method
   * @param request
   * @param tabId
   * @param parentId
   * @param recordId
   * @param changedColumn
   */
  private static Map<String, Object> createParameters(String method, HttpServletRequest request,
      String tabId, String parentId, String recordId, String changedColumn) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("_httpRequest", request);
    parameters.put("_httpSession", request.getSession(false));
    parameters.put("MODE", StringUtils.equals(method, OpenAPIConstants.POST) ? "NEW" : "CHANGE");
    parameters.put("_action",
        "org.openbravo.client.application.window.FormInitializationComponent");
    parameters.put("PARENT_ID", parentId);
    parameters.put("TAB_ID", StringUtils.isEmpty(tabId) ? "null" : tabId);
    parameters.put("ROW_ID", StringUtils.isEmpty(recordId) ? "null" : recordId);
    if (StringUtils.isNotEmpty(changedColumn)) {
      parameters.put("CHANGED_COLUMN", changedColumn);
    }
    return parameters;
  }

  /**
   * Normalizes the param name. The first word is in lower case and the rest in upper case.
   *
   * @param name
   */
  public static String normalizedName(String name) {
    if (StringUtils.equals(name, "AD_Role_ID")) {
      return "role";
    }

    if (StringUtils.isBlank(name)) {
      return "";
    }

    StringBuilder retName = new StringBuilder();
    String removeSpecialChars = StringUtils.replaceEach(name,
        new String[]{ "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "=", "~", "." },
        new String[]{ "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" }
    );
    //remove multiple Spaces, replacing with single
    while (removeSpecialChars.contains("  ")) {
      removeSpecialChars = StringUtils.replace(removeSpecialChars, "  ", " ");
    }
    String[] parts = removeSpecialChars.split(" ");

    for (int i = 0; i < parts.length; i++) {
      if (StringUtils.isNotEmpty(parts[i])) {
        if (i == 0) {
          retName.append(StringUtils.lowerCase(StringUtils.substring(parts[i], 0, 1)))
              .append(StringUtils.substring(parts[i], 1));
        } else {
          retName.append(StringUtils.upperCase(StringUtils.substring(parts[i], 0, 1)))
              .append(StringUtils.substring(parts[i], 1));
        }
      }
    }
    return retName.toString();
  }

  /**
   * Normalizes the field key.
   *
   * @param fieldList
   * @param key
   */
  private String normalizedKey(List<RequestField> fieldList, String key) {
    for (RequestField field : fieldList) {
      if (StringUtils.equals(field.getDBColumnName(), key)) {
        return field.getName();
      }
    }
    return key;
  }

  /**
   * Extracts the data source and ID from the request URI.
   *
   * @param requestURI
   * @return the extracted parts, being the first part the data source name and the second part the ID
   */
  static String[] extractDataSourceAndID(String requestURI) {
    String[] parts = requestURI.split("/");
    if (parts.length < 1 || parts.length > 3) {
      throw new IllegalArgumentException("Invalid request URI: " + requestURI);
    }

    String dataSourceName = parts[1];
    String id = (parts.length > 2) ? parts[2] : null;

    return id != null ? new String[]{ dataSourceName, id } : new String[]{ dataSourceName };
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
      Tab tab = getTabByDataSourceName(dataSourceName);
      String requestName = tab
          .getTable()
          .getName();

      StringBuilder newUri = new StringBuilder();
      newUri.append("/com.etendoerp.etendorx.datasource/")
          .append(dataSourceName)
          .append(DataSourceConstants.DATASOURCE_SERVLET_PATH)
          .append(requestName);

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

  private static Tab getTabByDataSourceName(String dataSourceName) throws OpenAPINotFoundThrowable {
    OpenAPIRequest apiRequest = (OpenAPIRequest) OBDal.getInstance()
        .createCriteria(OpenAPIRequest.class)
        .add(Restrictions.eq("name", dataSourceName))
        .setMaxResults(1)
        .uniqueResult();

    if (apiRequest == null) {
      throw new OpenAPINotFoundThrowable("OpenAPI request not found: " + dataSourceName);
    }

    if (apiRequest.getETRXOpenAPITabList().isEmpty()) {
      throw new OpenAPINotFoundThrowable(
          "OpenAPI request does not have any related tabs: " + dataSourceName);
    }

    Tab tab = apiRequest.getETRXOpenAPITabList()
        .get(0)
        .getRelatedTabs();
    return tab;
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
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
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
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    upsertEntity(OpenAPIConstants.PUT, path, request, response);
  }
}
