package com.etendoerp.etendorx.filter;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StatelessErrorFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    RequestContext.setServletContext(filterConfig.getServletContext());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String statelessParam = httpRequest.getParameter("stateless");

      //if (statelessParam != null) {
        try {
          chain.doFilter(request, response);
        } catch (Exception e) {
          httpResponse.setContentType("application/json");
          httpResponse.setCharacterEncoding("UTF-8");
          httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

          String jsonErrorResponse = "{" + "\"error\": \"Internal Server Error\"," + "\"message\": \"" + (
              e.getMessage() != null ?
                  e.getMessage() :
                  "An unexpected error occurred.") + "\"," + "\"status\": 500," + "\"timestamp\": \"" + java.time.Instant.now()
              .toString() + "\"" + "}";

          httpResponse.getWriter().write(jsonErrorResponse);
        }
        /*
      } else {
        chain.doFilter(request, response);
      }
         */
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }
}
