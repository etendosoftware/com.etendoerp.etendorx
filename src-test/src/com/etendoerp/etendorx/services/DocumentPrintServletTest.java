package com.etendoerp.etendorx.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.client.kernel.RequestContext;

import com.etendoerp.etendorx.printreport.DocumentReportingUtils;

/**
 * Unit tests for the {@link DocumentPrintServlet} class.
 */
public class DocumentPrintServletTest {

  private MockedStatic<DocumentReportingUtils> documentReportingUtilsMockedStatic;
  private MockedStatic<RequestContext> requestContextMockedStatic;

  private DocumentPrintServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private RequestContext requestContext;
  private ServletOutputStream outputStream;
  private PrintWriter writer;

  @Before
  public void setUp() throws IOException {
    documentReportingUtilsMockedStatic = mockStatic(DocumentReportingUtils.class);
    requestContextMockedStatic = mockStatic(RequestContext.class);

    servlet = new DocumentPrintServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    requestContext = mock(RequestContext.class);
    outputStream = mock(ServletOutputStream.class);
    writer = mock(PrintWriter.class);

    requestContextMockedStatic.when(RequestContext::get).thenReturn(requestContext);
    when(response.getOutputStream()).thenReturn(outputStream);
    when(response.getWriter()).thenReturn(writer);
  }

  @After
  public void tearDown() {
    documentReportingUtilsMockedStatic.close();
    requestContextMockedStatic.close();
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} with valid parameters.
   */
  @Test
  public void testDoGetSuccess() throws Exception {
    String tabId = "testTabId";
    String recordId = "testRecordId";
    byte[] pdfBytes = new byte[]{1, 2, 3};

    when(request.getParameter("tabId")).thenReturn(tabId);
    when(request.getParameter("recordId")).thenReturn(recordId);
    documentReportingUtilsMockedStatic.when(() -> DocumentReportingUtils.generatePDF(tabId, recordId))
        .thenReturn(pdfBytes);

    servlet.doGet("/path", request, response);

    verify(response).setContentType("application/pdf");
    verify(response).setHeader("Content-Disposition", "inline; filename=document.pdf");
    verify(response).setContentLength(pdfBytes.length);
    verify(outputStream).write(pdfBytes);
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} with missing parameters.
   */
  @Test
  public void testDoGetBadRequest() throws Exception {
    when(request.getParameter("tabId")).thenReturn(null);
    when(request.getParameter("recordId")).thenReturn("someId");

    servlet.doGet("/path", request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(writer).write(anyString());
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} when an exception occurs.
   */
  @Test
  public void testDoGetInternalServerError() throws Exception {
    String tabId = "testTabId";
    String recordId = "testRecordId";

    when(request.getParameter("tabId")).thenReturn(tabId);
    when(request.getParameter("recordId")).thenReturn(recordId);
    documentReportingUtilsMockedStatic.when(() -> DocumentReportingUtils.generatePDF(tabId, recordId))
        .thenThrow(new RuntimeException("Test exception"));

    servlet.doGet("/path", request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
