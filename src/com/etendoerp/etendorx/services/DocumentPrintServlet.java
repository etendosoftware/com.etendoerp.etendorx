package com.etendoerp.etendorx.services;

import com.etendoerp.etendorx.printreport.DocumentReportingUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.service.web.WebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.*;

/**
 * Web service servlet that handles document printing requests.
 * It receives parameters like tabId, recordId, and docType to generate and return a PDF document.
 */
public class DocumentPrintServlet implements WebService {
  private static final Logger log4j = LogManager.getLogger(DocumentPrintServlet.class);
  public static final String TAB_ID = "tabId";
  public static final String RECORD_ID = "recordId";

  public void process(HttpServletRequest request, HttpServletResponse response) {
    JSONObject payload = getPayload(request);
    String tabId = getTabId(request, payload);

    if (isInvalidRequest(tabId)) {
      sendBadRequest(response);
      return;
    }

    List<Map<String, String>> params = getRequestParameters(payload);

    setupRequestContext(request);

    try {
      byte[] pdfBytes = DocumentReportingUtils.generatePDF(tabId, params);
      boolean directPrint = DocumentReportingUtils.isDirectPrint(tabId);
      sendPdfResponse(response, pdfBytes, directPrint);
    } catch (Exception e) {
      handleError(response, e);
    }
  }

  private List<Map<String, String>> getRequestParameters(JSONObject payload) {
    if (payload != null) {
      try {
        JSONArray paramsObj = payload.optJSONArray("parameters");
        if (paramsObj != null) {
          List<Map<String, String>> paramsList = new ArrayList<>();
          for (int i = 0; i < paramsObj.length(); i++) {
            JSONObject paramObj = paramsObj.getJSONObject(i);
            Map<String, String> paramMap = jsonObjectToMap(paramObj);
            paramsList.add(paramMap);
          }
          // For simplicity, returning the first set of parameters
          return paramsList;
        }
      } catch (Exception e) {
        log4j.error("Error extracting parameters from payload", e);
      }
    }
    return Collections.emptyList();
  }

  private Map<String, String> jsonObjectToMap(JSONObject paramsObj) {
    Map<String, String> map = new java.util.HashMap<>();
    try {
      Iterator<String> itk = paramsObj.keys();
      for (Iterator<String> it = itk; it.hasNext(); ) {
        String key = it.next();
        map.put(key, paramsObj.getString(key));
      }
      return map;
    } catch (Exception e) {
      log4j.error("Error converting JSONObject to Map", e);
    }
    return Collections.emptyMap();
  }

  /**
   * Handles GET requests to generate and stream a PDF document.
   *
   * @param path     The request path.
   * @param request  The HTTP servlet request containing 'tabId', 'recordId', and 'docType' parameters.
   * @param response The HTTP servlet response where the PDF stream will be written.
   * @throws Exception If an error occurs during PDF generation or streaming.
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    process(request, response);
  }

  /**
   * Validates if the request parameters are valid.
   *
   * @param tabId The tab ID.
   * @return true if any required parameter is missing or invalid.
   */
  private boolean isInvalidRequest(String tabId) {
    return tabId == null;
  }

  /**
   * Sends a 400 Bad Request response with a descriptive message.
   *
   * @param response The HTTP servlet response.
   */
  private void sendBadRequest(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    try {
      response.getWriter().write("Parameter 'tabId' are required.");
    } catch (Exception e) {
      log4j.error("Error sending bad request response", e);
    }
  }

  /**
   * Ensures the RequestContext is populated with the current request.
   *
   * @param request The HTTP servlet request.
   */
  private void setupRequestContext(HttpServletRequest request) {
    if (RequestContext.get().getRequest() == null) {
      RequestContext.get().setRequest(request);
    }
  }

  /**
   * Streams the PDF bytes to the HTTP response.
   *
   * @param response    The HTTP servlet response.
   * @param pdfBytes    The byte array containing the PDF data.
   * @param directPrint Whether to use inline or attachment disposition.
   * @throws OBException If an error occurs while writing to the output stream.
   */
  private void sendPdfResponse(HttpServletResponse response, byte[] pdfBytes, boolean directPrint) {
    response.setContentType("application/pdf");
    String disposition = directPrint ? "inline" : "attachment";
    response.setHeader("Content-Disposition", disposition + "; filename=document.pdf");
    response.setContentLength(pdfBytes.length);
    try (OutputStream os = response.getOutputStream()) {
      os.write(pdfBytes);
      os.flush();
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }
  }

  /**
   * Handles errors by logging them and sending a 500 Internal Server Error response.
   *
   * @param response The HTTP servlet response.
   * @param e        The exception that occurred.
   */
  private void handleError(HttpServletResponse response, Exception e) {
    try {
      log4j.error("Error generating PDF document", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("An error occurred while generating the PDF document.");
    } catch (Exception ex) {
      log4j.error("Error sending error response", ex);
    }
  }

  /**
   * Handles POST requests. Currently not implemented.
   *
   * @param path     The request path.
   * @param request  The HTTP servlet request.
   * @param response The HTTP servlet response.
   * @throws Exception If an error occurs.
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented
    process(request, response);
  }

  /**
   * Handles DELETE requests. Currently not implemented.
   *
   * @param path     The request path.
   * @param request  The HTTP servlet request.
   * @param response The HTTP servlet response.
   * @throws Exception If an error occurs.
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented

  }

  /**
   * Handles PUT requests. Currently not implemented.
   *
   * @param path     The request path.
   * @param request  The HTTP servlet request.
   * @param response The HTTP servlet response.
   * @throws Exception If an error occurs.
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented

  }

  /**
   * Extracts the JSON payload from the request.
   *
   * @param request The HTTP servlet request.
   * @return The JSONObject payload, or null if not present or invalid.
   */
  private JSONObject getPayload(HttpServletRequest request) {
    try (var reader = request.getReader()) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      String body = sb.toString();
      if (body.isEmpty()) {
        return null;
      }
      log4j.debug("Request payload: " + body);
      return new JSONObject(body);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Retrieves the tab ID from the request parameters or payload.
   *
   * @param request The HTTP servlet request.
   * @param payload The JSON payload.
   * @return The tab ID, or null if not found.
   */
  private String getTabId(HttpServletRequest request, JSONObject payload) {
    String tabId = request.getParameter(TAB_ID);
    try {
      if (tabId == null && payload != null && payload.has(TAB_ID)) {
        tabId = payload.getString(TAB_ID);
      }
    } catch (Exception e) {
      log4j.error("Error getting tabId from payload", e);
    }
    return tabId != null ? tabId.trim() : null;
  }

}
