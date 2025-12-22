package com.etendoerp.etendorx.config;

import com.smf.securewebservices.utils.SecureWebServicesUtils;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;


/**
 * Utility class for initializing the Etendo environment and session variables.
 * It provides methods to set up the RequestContext and VariablesSecureApp.
 */
public class InitialConfigUtil {

  private InitialConfigUtil() {
    // Private constructor to prevent instantiation
  }

  /**
   * Initializes the session variables and RequestContext using the provided HTTP request.
   *
   * @param request
   *     The HTTP servlet request to initialize from.
   * @return The initialized VariablesSecureApp instance.
   * @throws OBException
   *     If an error occurs during initialization.
   */
  public static VariablesSecureApp initialize(HttpServletRequest request) {
    ConfigParameters servletConfiguration = ConfigParameters.retrieveFrom(
        RequestContext.getServletContext());
    try {
      SecureWebServicesUtils.fillSessionVariables(request);
      VariablesSecureApp vars = new VariablesSecureApp(request);
      LoginUtils.readNumberFormat(vars, servletConfiguration.getFormatPath());
      RequestContext.get().setVariableSecureApp(vars);
      return vars;
    } catch (ServletException e) {
      throw new OBException(e);
    }
  }

  /**
   * Initializes the session variables and RequestContext using the request from the current RequestContext.
   *
   * @return The initialized VariablesSecureApp instance.
   */
  public static VariablesSecureApp initialize() {
    return initialize(RequestContext.get().getRequest());
  }
}
