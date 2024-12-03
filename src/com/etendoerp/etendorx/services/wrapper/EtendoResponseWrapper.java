package com.etendoerp.etendorx.services.wrapper;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class EtendoResponseWrapper extends HttpServletResponseWrapper {
  private CharArrayWriter charArrayWriter = new CharArrayWriter();
  private PrintWriter writer = new PrintWriter(charArrayWriter);

  public EtendoResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return writer;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("This wrapper only supports getWriter().");
  }

  public String getCapturedContent() {
    return charArrayWriter.toString();
  }

  public void writeResponse() throws IOException {
    String capturedContent = getCapturedContent();
    String processedContent = processContent(capturedContent);
    HttpServletResponse originalResponse = (HttpServletResponse) getResponse();
    originalResponse.getWriter().write(processedContent);
  }

  private String processContent(String content) {
    return content.toUpperCase(); // Ejemplo: convierte el contenido a mayúsculas
  }

  @Override
  public void setStatus(int sc) {
    // Do nothing
  }
}
