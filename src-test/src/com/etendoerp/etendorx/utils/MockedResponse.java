package com.etendoerp.etendorx.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

/**
 * A utility class that encapsulates a mocked HttpServletResponse along with
 * a StringWriter and PrintWriter for capturing response content.
 */
public class MockedResponse {
  private final HttpServletResponse response;
  private final StringWriter stringWriter;
  private final PrintWriter printWriter;

  /**
   * Constructs a MockedResponse with the given HttpServletResponse, StringWriter, and PrintWriter.
   *
   * @param response
   *     The mocked HttpServletResponse.
   * @param stringWriter
   *     The StringWriter to capture response content.
   * @param printWriter
   *     The PrintWriter to write response content.
   */
  public MockedResponse(HttpServletResponse response, StringWriter stringWriter, PrintWriter printWriter) {
    this.response = response;
    this.stringWriter = stringWriter;
    this.printWriter = printWriter;
  }

  /**
   * Returns the mocked HttpServletResponse.
   *
   * @return The mocked HttpServletResponse.
   */
  public HttpServletResponse getResponse() {
    return response;
  }

  /**
   * Flushes the PrintWriter and returns the content written to the StringWriter.
   *
   * @return The content written to the StringWriter.
   */
  public String getResponseContent() {
    flushResponse();
    return stringWriter.toString();
  }

  /**
   * Flushes the PrintWriter to ensure all content is written to the StringWriter.
   */
  public void flushResponse() {
    printWriter.flush(); // Ensure all content is written
  }
}