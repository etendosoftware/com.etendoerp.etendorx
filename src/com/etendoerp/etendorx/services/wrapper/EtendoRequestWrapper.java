package com.etendoerp.etendorx.services.wrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class EtendoRequestWrapper extends HttpServletRequestWrapper {
  private final String modifiedBody;
  private final String requestURI;

  public EtendoRequestWrapper(HttpServletRequest originalRequest, String requestURI, String newBody) throws IOException {
    super(originalRequest);
    this.requestURI = requestURI;
    this.modifiedBody = newBody;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(modifiedBody.getBytes(StandardCharsets.UTF_8))
    ));
  }

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

  public String getRequestURI() {
    return requestURI;
  }

}
