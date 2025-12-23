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
import java.util.ArrayList;
import java.util.List;

/**
 * Web service servlet that handles document printing requests.
 * It receives parameters like tabId, recordId, and docType to generate and return a PDF document.
 */
public class DocumentPrintServlet implements WebService {
  private static final Logger log4j = LogManager.getLogger(DocumentPrintServlet.class);

  public void process(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String tabId = request.getParameter("tabId");
    // Obtain request payload as jsonobject
    JSONObject payload = null;
    try (var reader = request.getReader()) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      log4j.debug("Request payload: " + sb.toString());
      payload = new JSONObject(sb.toString());
    } catch (Exception e) {
    }

    if (tabId == null && payload != null && payload.has("tabId")) {
      tabId = payload.getString("tabId");
    }

    if (tabId != null) {
      tabId = tabId.trim();
    }

    if (isInvalidRequest(tabId)) {
      sendBadRequest(response);
      return;
    }

    List<String> recordIds = new ArrayList<>();

    if (request.getParameterValues("recordId") != null) {
      for (String recId : request.getParameterValues("recordId")) {
        recordIds.add(recId);
      }
    } else if (request.getParameterValues("reportid") != null) {
      for (String recId : request.getParameterValues("reportid")) {
        recordIds.add(recId);
      }
    }

    if (recordIds.isEmpty() && payload != null) {
      if (payload.has("recordId")) {
        JSONArray array = payload.optJSONArray("recordId");
        if (array != null) {
          for (int i = 0; i < array.length(); i++) {
            recordIds.add(array.getString(i));
          }
        } else {
          recordIds.add(payload.getString("recordId"));
        }
      } else if (payload.has("reportid")) {
        JSONArray array = payload.optJSONArray("reportid");
        if (array != null) {
          for (int i = 0; i < array.length(); i++) {
            recordIds.add(array.getString(i));
          }
        } else {
          recordIds.add(payload.getString("reportid"));
        }
      }
    }

    setupRequestContext(request);

    try {
      byte[] pdfBytes = DocumentReportingUtils.generatePDF(tabId, recordIds);
      boolean directPrint = DocumentReportingUtils.isDirectPrint(tabId);
      sendPdfResponse(response, pdfBytes, directPrint);
    } catch (Exception e) {
      handleError(response, e);
    }
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
}
