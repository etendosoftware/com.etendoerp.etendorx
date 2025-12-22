package com.etendoerp.etendorx.services;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.service.web.WebService;

import com.etendoerp.etendorx.printreport.DocumentReportingUtils;

/**
 * Web service servlet that handles document printing requests.
 * It receives parameters like tabId, recordId, and docType to generate and return a PDF document.
 */
public class DocumentPrintServlet implements WebService {
  private static final Logger log4j = LogManager.getLogger(DocumentPrintServlet.class);

  /**
   * Handles GET requests to generate and stream a PDF document.
   *
   * @param path
   *     The request path.
   * @param request
   *     The HTTP servlet request containing 'tabId', 'recordId', and 'docType' parameters.
   * @param response
   *     The HTTP servlet response where the PDF stream will be written.
   * @throws Exception
   *     If an error occurs during PDF generation or streaming.
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String tabId = request.getParameter("tabId");
    String recordId = request.getParameter("recordId");

    if (isInvalidRequest(tabId, recordId)) {
      sendBadRequest(response);
      return;
    }

    setupRequestContext(request);

    try {
      byte[] pdfBytes = DocumentReportingUtils.generatePDF(tabId, recordId);
      sendPdfResponse(response, pdfBytes);
    } catch (Exception e) {
      handleError(response, e);
    }
  }

  /**
   * Validates if the request parameters are valid.
   *
   * @param tabId
   *     The tab ID.
   * @param recordId
   *     The record ID.
   * @return true if any required parameter is missing or invalid.
   */
  private boolean isInvalidRequest(String tabId, String recordId) {
    return tabId == null || recordId == null;
  }

  /**
   * Sends a 400 Bad Request response with a descriptive message.
   *
   * @param response
   *     The HTTP servlet response.
   * @throws Exception
   *     If an error occurs while writing the response.
   */
  private void sendBadRequest(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    try {
      response.getWriter().write("Parameters 'tabId', 'docType' and 'recordId' are required.");
    } catch (Exception e) {
      log4j.error("Error sending bad request response", e);
    }
  }

  /**
   * Ensures the RequestContext is populated with the current request.
   *
   * @param request
   *     The HTTP servlet request.
   */
  private void setupRequestContext(HttpServletRequest request) {
    if (RequestContext.get().getRequest() == null) {
      RequestContext.get().setRequest(request);
    }
  }

  /**
   * Streams the PDF bytes to the HTTP response.
   *
   * @param response
   *     The HTTP servlet response.
   * @param pdfBytes
   *     The byte array containing the PDF data.
   * @throws Exception
   *     If an error occurs while writing to the output stream.
   */
  private void sendPdfResponse(HttpServletResponse response, byte[] pdfBytes) {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "inline; filename=document.pdf");
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
   * @param response
   *     The HTTP servlet response.
   * @param e
   *     The exception that occurred.
   * @throws Exception
   *     If an error occurs while writing the error response.
   */
  private void handleError(HttpServletResponse response, Exception e) {
    try {
      log4j.error("Error generating PDF document", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      e.printStackTrace(response.getWriter());
    } catch (Exception ex) {
      log4j.error("Error sending error response", ex);
    }
  }

  /**
   * Handles POST requests. Currently not implemented.
   *
   * @param path
   *     The request path.
   * @param request
   *     The HTTP servlet request.
   * @param response
   *     The HTTP servlet response.
   * @throws Exception
   *     If an error occurs.
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented

  }

  /**
   * Handles DELETE requests. Currently not implemented.
   *
   * @param path
   *     The request path.
   * @param request
   *     The HTTP servlet request.
   * @param response
   *     The HTTP servlet response.
   * @throws Exception
   *     If an error occurs.
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented

  }

  /**
   * Handles PUT requests. Currently not implemented.
   *
   * @param path
   *     The request path.
   * @param request
   *     The HTTP servlet request.
   * @param response
   *     The HTTP servlet response.
   * @throws Exception
   *     If an error occurs.
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Not implemented

  }
}
