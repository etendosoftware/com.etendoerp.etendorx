package com.etendoerp.etendorx.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;

import com.etendoerp.etendorx.utils.DataSourceUtils;

/** Verifies the HTTP and transaction contract when an upsert cannot resolve a selector. */
public class DataSourceServletSelectorErrorTest {
    @Test
    public void selectorFailureAbortsPostAndMarksRollback() throws Exception {
        assertSelectorFailure("POST");
    }

    @Test
    public void selectorFailureAbortsPutAndMarksRollback() throws Exception {
        assertSelectorFailure("PUT");
    }

    private void assertSelectorFailure(String method) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        SessionHandler session = mock(SessionHandler.class);
        SelectorResolutionException failure = new SelectorResolutionException(
                new IllegalArgumentException("Selector record not found"));
        try (MockedStatic<DataSourceUtils> utils = mockStatic(DataSourceUtils.class);
             MockedStatic<OBContext> context = mockStatic(OBContext.class);
             MockedStatic<SessionHandler> sessions = mockStatic(SessionHandler.class);
             MockedStatic<WeldUtils> weld = mockStatic(WeldUtils.class);
             MockedStatic<RequestContext> requests = mockStatic(RequestContext.class)) {
            RequestContext requestContext = mock(RequestContext.class);
            VariablesSecureApp variables = mock(VariablesSecureApp.class);
            requests.when(RequestContext::get).thenReturn(requestContext);
            when(requestContext.getVariablesSecureApp()).thenReturn(variables);
            when(variables.getJavaDateFormat()).thenReturn("dd-MM-yyyy");
            when(variables.getSessionValue("#DecimalSeparator|generalQtyEdition")).thenReturn(".");
            // Inject a downstream failure through the shared upsert workflow.
            utils.when(() -> DataSourceUtils.extractDataSourceAndID("/SalesOrder")).thenThrow(failure);
            sessions.when(SessionHandler::getInstance).thenReturn(session);
            DataSourceServlet servlet = new DataSourceServlet();
            if ("POST".equals(method)) {
                servlet.doPost("/SalesOrder", request, response);
            } else {
                servlet.doPut("/SalesOrder", request, response);
            }
            verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
            verify(session).setDoRollback(true);
            JSONObject error = new JSONObject(body.toString());
            assertEquals("Selector Error", error.getString("error"));
            assertEquals("Selector record not found", error.getString("message"));
            // No persistence datasource is obtained after the failure.
            weld.verifyNoInteractions();
        }
    }
}
