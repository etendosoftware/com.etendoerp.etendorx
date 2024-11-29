package com.etendoerp.etendorx.services;

import com.etendoerp.etendorx.services.wrapper.EtendoRequestWrapper;
import com.etendoerp.etendorx.services.wrapper.RequestField;
import com.etendoerp.openapi.data.OpenAPIRequest;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.LoginUtils.RoleDefaults;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

  /**
   * Handles GET requests.
   *
   * @param path The request path.
   * @param request The HTTP request.
   * @param response The HTTP response.
   * @throws Exception If an error occurs.
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
    } finally {
      OBContext.restorePreviousMode();
    }

    String dataSourceName = convertURI(request.getRequestURI());

    var newRequest = new EtendoRequestWrapper(request, dataSourceName, "");

    WeldUtils.getInstanceFromStaticBeanManager(
        org.openbravo.service.datasource.DataSourceServlet.class).doGet(newRequest, response);

  }

  /**
   * Handles POST requests.
   *
   * @param path The request path.
   * @param request The HTTP request.
   * @param response The HTTP response.
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    try {
      Tab tab;
      List<RequestField> fieldList = new ArrayList<>();
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

        String tabId = request.getParameter("tabId");
        tab = OBDal.getInstance().get(Tab.class, tabId);
        for (Field field : tab.getADFieldList()) {
          String name = normalizedName(field.getName());
          fieldList.add(new RequestField(name, field.getColumn().getDBColumnName()));
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      String csrf = "123";
      request.getSession(false).setAttribute("#CSRF_TOKEN", csrf);

      // Get body content
      StringBuilder sb = new StringBuilder();
      try (InputStream inputStream = request.getInputStream()) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
      }
      String oldBody = sb.toString();
      JSONObject jsonBody = new JSONObject(oldBody);
      JSONObject newJsonBody = new JSONObject();

      var servlet = WeldUtils.getInstanceFromStaticBeanManager(
          org.openbravo.service.datasource.DataSourceServlet.class);

      newJsonBody.put("dataSource", "isc_OBViewDataSource_0");
      newJsonBody.put("operationType", "add");
      newJsonBody.put("componentId", "isc_OBViewForm_0");
      newJsonBody.put("csrfToken", csrf);
      newJsonBody.put("data", jsonBody);

      String newUri = convertURI(request.getRequestURI());

      var formInit = WeldUtils.getInstanceFromStaticBeanManager(EtendoFormInitComponent.class);
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("_httpRequest", request);
      parameters.put("_httpSession", request.getSession(false));
      parameters.put("MODE", "NEW");
      parameters.put("_action",
          "org.openbravo.client.application.window.FormInitializationComponent");
      parameters.put("PARENT_ID", "null");
      parameters.put("TAB_ID", tab.getId());
      parameters.put("ROW_ID", "null");

      String content = "{}";
      var formInitResponse = formInit.execute(parameters, content);

      var values = formInitResponse.getJSONObject("columnValues");
      var keys = values.keys();
      JSONObject data = newJsonBody.getJSONObject("data");
      while (keys.hasNext()) {
        String key = (String) keys.next();
        String normalizedKey = normalizedKey(fieldList, key);
        JSONObject value = values.getJSONObject(key);
        Object val = null;
        if (value.has("value")) {
          val = value.get("value");
        }
        if (!data.has(normalizedKey)) {
          data.put(normalizedKey, val);
        }
      }
      var newRequest = new EtendoRequestWrapper(request, newUri, newJsonBody.toString());
      // new request
      servlet.doPost(newRequest, response);
    } catch (Exception e) {
      try {
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      String jsonErrorResponse = "{" + "\"error\": \"Internal Server Error\"," + "\"message\": \"" + (
          e.getMessage() != null ?
              e.getMessage() :
              "An unexpected error occurred.") + "\"," + "\"status\": 500," + "\"timestamp\": \"" + java.time.Instant.now()
          .toString() + "\"" + "}";

      response.getWriter().write(jsonErrorResponse);
      } catch (Exception ex) {
        throw new OBException(ex);
      }
    }
  }

  /**
   * Normalizes the name of a field.
   *
   * @param name The original name.
   * @return The normalized name.
   */
  public static String normalizedName(String name) {
    if (StringUtils.equals(name, "AD_Role_ID")) {
      return "role";
    }

    if (StringUtils.isBlank(name)) {
      return ""; // Return an empty string for null, empty, or blank input
    }

    StringBuilder retName = new StringBuilder();
    String[] parts = StringUtils.replaceChars(name, ".-", "").split(" ");

    for (int i = 0; i < parts.length; i++) {
      if (StringUtils.isNotEmpty(parts[i])) { // Null-safe check for each part
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
   * Normalizes the key of a field.
   *
   * @param fieldList The list of request fields.
   * @param key The original key.
   * @return The normalized key.
   */
  private String normalizedKey(List<RequestField> fieldList, String key) {
    String normalizedKey = key;
    for (RequestField field : fieldList) {
      if (StringUtils.equals(field.getDBColumnName(), key)) {
        normalizedKey = field.getName();
      }
    }
    return normalizedKey;
  }

  /**
   * Converts the request URI to a new format.
   *
   * @param requestURI The original request URI.
   * @return The converted URI.
   */
  private String convertURI(String requestURI) {
    String[] parts = requestURI.split("/");
    StringBuilder newUri = new StringBuilder();
    for (int i = 0; i < parts.length - 1; i++) {
      newUri.append(parts[i]).append("/");
    }
    String dataSourceName = parts[parts.length - 1];

    OpenAPIRequest apiRequest = (OpenAPIRequest) OBDal.getInstance().createCriteria(OpenAPIRequest.class)
        .add(Restrictions.eq("name", dataSourceName))
        .setMaxResults(1)
        .uniqueResult();

    String requestName = apiRequest.getETRXOpenAPITabList().get(0).getRelatedTabs().getTable().getName();
    return newUri.append(DataSourceConstants.DATASOURCE_SERVLET_PATH)
        .append(requestName)
        .toString();
  }

  /**
   * Handles DELETE requests.
   *
   * @param path The request path.
   * @param request The HTTP request.
   * @param response The HTTP response.
   * @throws Exception If an error occurs.
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Method not implemented
    throw new UnsupportedOperationException("DELETE method not supported.");
  }

  /**
   * Handles PUT requests.
   *
   * @param path The request path.
   * @param request The HTTP request.
   * @param response The HTTP response.
   * @throws Exception If an error occurs.
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Method not implemented
    throw new UnsupportedOperationException("PUT method not supported.");
  }

}
