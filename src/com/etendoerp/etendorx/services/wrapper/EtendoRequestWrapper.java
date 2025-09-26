package com.etendoerp.etendorx.services.wrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A wrapper class for HttpServletRequest that allows modification of the request body and URI.
 * This class is used to modify the request body and URI before passing it to the servlet.
 */
public class EtendoRequestWrapper extends HttpServletRequestWrapper {
  private final String modifiedBody;
  private final String requestURI;
  private final Map<String, String[]> modifiedParameters;


  /**
   * Constructor for EtendoRequestWrapper.
   *
   * @param originalRequest
   *     The original request.
   * @param requestURI
   *     The modified request URI.
   * @param newBody
   *     The modified request body.
   * @param newParameters
   *     The modified request parameters.
   * @throws IOException
   *     If an I/O error occurs.
   */
  public EtendoRequestWrapper(HttpServletRequest originalRequest, String requestURI, String newBody,
      Map<String, String[]> newParameters) throws IOException {
    super(originalRequest);
    this.requestURI = requestURI;
    this.modifiedBody = newBody;
    this.modifiedParameters = new HashMap<>(newParameters);
  }

  /**
   * Get the modified request body.
   *
   * @throws IOException
   */
  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(modifiedBody.getBytes(StandardCharsets.UTF_8))));
  }

  /**
   * Get the modified request body as InputStream.
   *
   * @throws IOException
   */
  @Override
  public javax.servlet.ServletInputStream getInputStream() throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        modifiedBody.getBytes(StandardCharsets.UTF_8));
    /**
     * Return a new ServletInputStream that reads from the modified request body.
     */
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
   * Get the modified request URI.
   */
  @Override
  public String getRequestURI() {
    return requestURI;
  }

  /**
   * Get the modified request URL.
   *
   * @param name
   */
  @Override
  public String getParameter(String name) {
    if (modifiedParameters.containsKey(name)) {
      String[] values = modifiedParameters.get(name);
      return values != null && values.length > 0 ? values[0] : null;
    }
    return super.getParameter(name);
  }

  /**
   * Get the modified request parameters.
   *
   * @param name
   */
  @Override
  public String[] getParameterValues(String name) {
    if (modifiedParameters.containsKey(name)) {
      return modifiedParameters.get(name);
    }
    return super.getParameterValues(name);
  }

  /**
   * Get the modified request parameters.
   */
  @Override
  public Map<String, String[]> getParameterMap() {
    Map<String, String[]> originalParams = super.getParameterMap();
    Map<String, String[]> combinedParams = new HashMap<>(originalParams);
    combinedParams.putAll(modifiedParameters); // Override with modified params
    return Collections.unmodifiableMap(combinedParams);
  }

  /**
   * Get the modified request parameter names.
   */
  @Override
  public Enumeration<String> getParameterNames() {
    Set<String> paramNames = new HashSet<>(super.getParameterMap().keySet());
    paramNames.addAll(modifiedParameters.keySet());
    return Collections.enumeration(paramNames);
  }
}
