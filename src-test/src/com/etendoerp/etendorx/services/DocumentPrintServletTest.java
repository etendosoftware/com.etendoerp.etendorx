package com.etendoerp.etendorx.services;

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

  public static final String TAB_ID = "tabId";
  public static final String RECORD_ID = "recordId";
  public static final String PATH = "/path";
  public static final String TEST_TAB_ID = "testTabId";
  public static final String TEST_RECORD_ID = "testRecordId";
  private MockedStatic<DocumentReportingUtils> documentReportingUtilsMockedStatic;
  private MockedStatic<RequestContext> requestContextMockedStatic;

  private DocumentPrintServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ServletOutputStream outputStream;
  private PrintWriter writer;

  /**
   * Sets up the test environment before each test.
   * @throws IOException If an error occurs during setup.
   */
  @Before
  public void setUp() throws IOException {
    documentReportingUtilsMockedStatic = mockStatic(DocumentReportingUtils.class);
    requestContextMockedStatic = mockStatic(RequestContext.class);

    servlet = new DocumentPrintServlet();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    RequestContext requestContext = mock(RequestContext.class);
    outputStream = mock(ServletOutputStream.class);
    writer = mock(PrintWriter.class);

    requestContextMockedStatic.when(RequestContext::get).thenReturn(requestContext);
    when(response.getOutputStream()).thenReturn(outputStream);
    when(response.getWriter()).thenReturn(writer);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    documentReportingUtilsMockedStatic.close();
    requestContextMockedStatic.close();
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} for successful PDF generation.
   *
   * @throws Exception
   *     If an error occurs during the test execution.
   */
  @Test
  public void testDoGetSuccess() throws Exception {
    String tabId = TEST_TAB_ID;
    String recordId = TEST_RECORD_ID;
    byte[] pdfBytes = new byte[]{1, 2, 3};

    when(request.getParameter(TAB_ID)).thenReturn(tabId);
    when(request.getParameter(RECORD_ID)).thenReturn(recordId);
    documentReportingUtilsMockedStatic.when(() -> DocumentReportingUtils.generatePDF(tabId, recordId))
        .thenReturn(pdfBytes);

    servlet.doGet(PATH, request, response);

    verify(response).setContentType("application/pdf");
    verify(response).setHeader("Content-Disposition", "inline; filename=document.pdf");
    verify(response).setContentLength(pdfBytes.length);
    verify(outputStream).write(pdfBytes);
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} with missing parameters.
   *
   * @throws Exception
   *     If an error occurs during the test execution.
   */
  @Test
  public void testDoGetBadRequest() throws Exception {
    when(request.getParameter(TAB_ID)).thenReturn(null);
    when(request.getParameter(RECORD_ID)).thenReturn("someId");

    servlet.doGet(PATH, request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(writer).write(anyString());
  }

  /**
   * Tests {@link DocumentPrintServlet#doGet(String, HttpServletRequest, HttpServletResponse)} when an exception occurs.
   *
   * @throws Exception
   *     If an error occurs during the test execution.
   */
  @Test
  public void testDoGetInternalServerError() throws Exception {
    String tabId = TEST_TAB_ID;
    String recordId = TEST_RECORD_ID;

    when(request.getParameter(TAB_ID)).thenReturn(tabId);
    when(request.getParameter(RECORD_ID)).thenReturn(recordId);
    documentReportingUtilsMockedStatic.when(() -> DocumentReportingUtils.generatePDF(tabId, recordId))
        .thenThrow(new RuntimeException("Test exception"));

    servlet.doGet(PATH, request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
