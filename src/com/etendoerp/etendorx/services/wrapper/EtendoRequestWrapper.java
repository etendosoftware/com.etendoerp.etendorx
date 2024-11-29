package com.etendoerp.etendorx.services.wrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * A wrapper class for HttpServletRequest that allows modification of the request body and URI.
 * This class is used to modify the request body and URI before passing it to the servlet.
 */
public class EtendoRequestWrapper extends HttpServletRequestWrapper {
  private final String modifiedBody;
  private final String requestURI;

  /**
   * Constructs a new EtendoRequestWrapper.
   *
   * @param originalRequest The original HttpServletRequest.
   * @param requestURI The new request URI.
   * @param newBody The new request body.
   * @throws IOException If an I/O error occurs.
   */
  public EtendoRequestWrapper(HttpServletRequest originalRequest, String requestURI, String newBody) throws IOException {
    super(originalRequest);
    this.requestURI = requestURI;
    this.modifiedBody = newBody;
  }

  /**
   * Returns a BufferedReader for reading the modified request body.
   *
   * @return A BufferedReader for reading the modified request body.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(modifiedBody.getBytes(StandardCharsets.UTF_8))
    ));
  }

  /**
   * Returns a ServletInputStream for reading the modified request body.
   *
   * @return A ServletInputStream for reading the modified request body.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public javax.servlet.ServletInputStream getInputStream() throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(modifiedBody.getBytes(StandardCharsets.UTF_8));
    return new javax.servlet.ServletInputStream() {
      @Override
      public int read() throws IOException {
        return byteArrayInputStream.read();
      }

      @Override
      public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(javax.servlet.ReadListener readListener) {
        // No-op
      }
    };
  }

  /**
   * Returns the modified request URI.
   *
   * @return The modified request URI.
   */
  public String getRequestURI() {
    return requestURI;
  }

}
