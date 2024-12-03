package com.etendoerp.etendorx.services;

import com.etendoerp.etendorx.openapi.OpenAPIConstants;
import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;
import com.etendoerp.etendorx.services.wrapper.EtendoResponseWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.openapi.data.OpenAPIRequest;
import org.apache.commons.lang3.StringUtils;
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.ResourceNotFoundException;
import org.openbravo.service.web.WebService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet that handles data source requests.
 */
public class DataSourceServlet implements WebService {

  private static final Logger log = LogManager.getLogger();

  /**
   * Handles HTTP GET requests.
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String dataSourceName;
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
      dataSourceName = convertURI(path);
    } catch (OpenAPINotFoundException e) {
      handleNotFoundException(response);
      return;
    } catch (OBException e) {
      log.error("Error in DataSourceServlet", e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

    var newRequest = new EtendoRequestWrapper(request, dataSourceName, "");
    var newResponse = new EtendoResponseWrapper(response);
    WeldUtils.getInstanceFromStaticBeanManager(
        org.openbravo.service.datasource.DataSourceServlet.class).doGet(newRequest, newResponse);
    try {
      JSONObject capturedResponse = new JSONObject(newResponse.getCapturedContent());
      String responseString = capturedResponse.toString();
      if (!capturedResponse.has("response")) {
        throw new OBException("Error in DataSourceServlet");
      }
      if (capturedResponse.getJSONObject("response").getJSONArray("data").length() == 0) {
        throw new ResourceNotFoundException("Record not found");
      }
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(responseString);
    } catch (IOException e) {
      throw new OBException(e);
    }
  }

  /**
   * Handles HTTP POST requests.
   * @param response
   * @throws IOException
   */
  private static void handleNotFoundException(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.setContentType("application/json");
    response.getWriter()
        .write(
            "{\"error\": \"Not Found\", \"message\": \"The requested resource was not found.\"}");
    response.getWriter().flush();
  }

  /**
   * Creates the payload for the POST request.
   * @param request
   * @return
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
      newJsonBody.put("data", jsonBody);
      return newJsonBody;
    } catch (JSONException | IOException e) {
      log.error("Error in DataSourceServlet", e);
      throw new OBException(e);
    }
  }

  /**
   * Dispatches the POST request to the DataSourceServlet.
   *
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    upsertEntity(OpenAPIConstants.POST, path, request, response);
  }

  private void upsertEntity(String method, String path, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      List<RequestField> fieldList = new ArrayList<>();
      Tab tab = getTab(path, request, response, fieldList);
      if (tab == null) {
        handleNotFoundException(response);
      }
      String newUri;
      try {
        newUri = convertURI(path);
      } catch (OpenAPINotFoundException e) {
        handleNotFoundException(response);
        return;
      }

      var servlet = WeldUtils.getInstanceFromStaticBeanManager(
          org.openbravo.service.datasource.DataSourceServlet.class);

      EtendoRequestWrapper newRequest;
      try {
        newRequest = getEtendoRequestWrapper(method, request, response, tab, createPayLoad(request),
            fieldList, newUri, path);
      } catch (OpenAPINotFoundException e) {
        handleNotFoundException(response);
        return;
      }
      if (OpenAPIConstants.POST.equals(method)) {
        servlet.doPost(newRequest, response);
      } else if (OpenAPIConstants.PUT.equals(method)) {
        servlet.doPut(newRequest, response);
      } else {
        throw new UnsupportedOperationException("Method not supported: " + method);
      }
    } catch (Exception e) {
      log.error("Error in DataSourceServlet", e);
      try {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        JSONObject jsonErrorResponse = new JSONObject();
        jsonErrorResponse.put("error", "Internal Server Error");
        jsonErrorResponse.put("message",
            e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
        jsonErrorResponse.put("status", 500);
        response.getWriter().write(jsonErrorResponse.toString());
        response.getWriter().flush();
      } catch (Exception ex) {
        throw new OBException(ex);
      }
    }
  }

  /**
   * Gets the tab for the given path.
   * @param path
   * @param request
   * @param response
   * @param fieldList
   * @return
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
   * Gets the EtendoRequestWrapper for the given parameters.
   * @param method
   * @param request
   * @param response
   * @param tab
   * @param newJsonBody
   * @param fieldList
   * @param newUri
   * @param path
   * @return
   * @throws JSONException
   * @throws IOException
   * @throws ServletException
   * @throws OpenAPINotFoundException
   */
  private EtendoRequestWrapper getEtendoRequestWrapper(String method, HttpServletRequest request,
      HttpServletResponse response, Tab tab, JSONObject newJsonBody, List<RequestField> fieldList,
      String newUri, String path)
      throws JSONException, IOException, ServletException, OpenAPINotFoundException {
    var formInit = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("_httpRequest", request);
    parameters.put("_httpSession", request.getSession(false));
    parameters.put("MODE", StringUtils.equals(method, OpenAPIConstants.POST) ? "NEW" : "CHANGE");
    parameters.put("_action",
        "org.openbravo.client.application.window.FormInitializationComponent");
    parameters.put("PARENT_ID", "null");
    parameters.put("TAB_ID", tab.getId());
    parameters.put("ROW_ID", "null");

    String content = "{}";
    if (method.equals(OpenAPIConstants.PUT)) {
      String getURI = convertURI(path);
      var newRequest = new EtendoRequestWrapper(request, getURI, "");
      var newResponse = new EtendoResponseWrapper(response);
      WeldUtils.getInstanceFromStaticBeanManager(
          org.openbravo.service.datasource.DataSourceServlet.class).doGet(newRequest, newResponse);
      String capturedOutput = newResponse.getCapturedContent();
      JSONObject capturedResponse = new JSONObject(capturedOutput);
      JSONObject values = capturedResponse.getJSONObject("response")
          .getJSONArray("data")
          .getJSONObject(0);
      JSONObject data = newJsonBody.getJSONObject("data");
      var keys = values.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        String normalizedKey = normalizedKey(fieldList, key);
        Object value = values.get(key);
        if (!data.has(normalizedKey)) {
          data.put(normalizedKey, value);
        }
      }
    } else if (method.equals(OpenAPIConstants.POST)) {
      var formInitResponse = formInit.execute(parameters, content);
      var values = formInitResponse.getJSONObject("columnValues");
      var keys = values.keys();
      JSONObject data = newJsonBody.getJSONObject("data");
      while (keys.hasNext()) {
        String key = (String) keys.next();
        String normalizedKey = normalizedKey(fieldList, key);
        JSONObject value = values.getJSONObject(key);
        Object val = value.has("value") ? value.get("value") : null;
        if (!data.has(normalizedKey)) {
          data.put(normalizedKey, val);
        }
      }
    }
    return new EtendoRequestWrapper(request, newUri, newJsonBody.toString());
  }

  /**
   * Normalizes the param name.
   * @param name
   * @return
   */
  public static String normalizedName(String name) {
    if (StringUtils.equals(name, "AD_Role_ID")) {
      return "role";
    }

    if (StringUtils.isBlank(name)) {
      return "";
    }

    StringBuilder retName = new StringBuilder();
    String[] parts = StringUtils.replaceChars(name, ".-", "").split(" ");

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
   * @param fieldList
   * @param key
   * @return
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
   * @param requestURI
   * @return
   */
  static String[] extractDataSourceAndID(String requestURI) {
    String[] parts = requestURI.split("/");
    if (parts.length < 1 || parts.length > 3) {
      throw new IllegalArgumentException("Invalid request URI: " + requestURI);
    }

    String dataSourceName = parts[1];
    String id = (parts.length > 2) ? parts[2] : null;

    return id != null ? new String[] { dataSourceName, id } : new String[] { dataSourceName };
  }

  /**
   * Converts the request URI to the new URI.
   * @param requestURI
   * @return
   * @throws OpenAPINotFoundException
   */
  String convertURI(String requestURI) throws OpenAPINotFoundException {
    String[] extractedParts = extractDataSourceAndID(requestURI);
    String dataSourceName = extractedParts[0];

    try {
      OBContext.setAdminMode();
      OpenAPIRequest apiRequest = (OpenAPIRequest) OBDal.getInstance()
          .createCriteria(OpenAPIRequest.class)
          .add(Restrictions.eq("name", dataSourceName))
          .setMaxResults(1)
          .uniqueResult();

      if (apiRequest == null) {
        throw new OpenAPINotFoundException("OpenAPI request not found: " + dataSourceName);
      }

      if (apiRequest.getETRXOpenAPITabList().isEmpty()) {
        throw new OpenAPINotFoundException(
            "OpenAPI request does not have any related tabs: " + dataSourceName);
      }

      String requestName = apiRequest.getETRXOpenAPITabList()
          .get(0)
          .getRelatedTabs()
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
      log.error("Error in DataSourceServlet", e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Handles HTTP DELETE requests.
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException("DELETE method not supported.");
  }

  /**
   * Handles HTTP PUT requests.
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    upsertEntity(OpenAPIConstants.PUT, path, request, response);
  }
}
