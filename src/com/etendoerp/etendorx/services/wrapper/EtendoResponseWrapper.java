package com.etendoerp.etendorx.services.wrapper;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A wrapper for HttpServletResponse that captures the response content.
 */
public class EtendoResponseWrapper extends HttpServletResponseWrapper {
  private final CharArrayWriter charArrayWriter = new CharArrayWriter();
  private final PrintWriter writer = new PrintWriter(charArrayWriter);

  /**
   * Constructs a response wrapper for the given HttpServletResponse.
   *
   * @param response the original HttpServletResponse
   */
  public EtendoResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  /**
   * Returns a PrintWriter to capture the response content.
   *
   * @return a PrintWriter to capture the response content
   */
  @Override
  public PrintWriter getWriter() {
    return writer;
  }

  /**
   * Throws UnsupportedOperationException as this wrapper only supports getWriter().
   *
   * @return nothing
   * @throws UnsupportedOperationException always
   */
  @Override
  public ServletOutputStream getOutputStream() {
    throw new UnsupportedOperationException("This wrapper only supports getWriter().");
  }

  /**
   * Returns the captured response content as a String.
   *
   * @return the captured response content
   */
  public JSONObject getCapturedContent() {
    try {
      return new JSONObject(charArrayWriter.toString());
    } catch (JSONException e) {
      throw new OBException("Error getting captured content", e);
    }
  }

}
