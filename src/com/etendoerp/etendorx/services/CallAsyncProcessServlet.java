package com.etendoerp.etendorx.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.service.db.CallProcess;

import com.etendoerp.db.CallAsyncProcess;
import org.openbravo.service.web.WebService;

/**
 * REST Endpoint for executing database processes synchronously or asynchronously.
 * * <p><b>URL:</b> /ws/com.etendoerp.etendorx.services.CallAsyncProcessServlet</p>
 * * <p><b>Method: POST (Execute)</b></p>
 * Payload:
 * <pre>
 * {
 * "processId": "32 Characters UUID",
 * "recordId": "Target Record UUID (Optional)",
 * "async": true,
 * "params": {
 * "ParameterName": "Value",
 * "DateParam": "2025-01-01"
 * }
 * }
 * </pre>
 * * <p><b>Method: GET (Status Polling)</b></p>
 * Param: ?id=PInstanceID
 */
public class CallAsyncProcessServlet implements WebService {

  private static final long serialVersionUID = 1L;
  private static final String PARAM_ID = "id";
  private static final String JSON_STATUS = "status";
  private static final String JSON_MESSAGE = "message";
  private static final String JSON_PROCESS_INSTANCE_ID = "processInstanceId";
  private static final String JSON_PROCESS_ID = "processId";
  private static final String JSON_RECORD_ID = "recordId";
  private static final String JSON_RESULT = "result";
  private static final String JSON_EXECUTION_MODE = "executionMode";
  private static final String JSON_PARAMS = "params";
  private static final String JSON_ASYNC = "async";
  private static final String EXECUTION_MODE_ASYNC = "async";
  private static final String EXECUTION_MODE_SYNC = "sync";
  private static final String STATUS_OK = "ok";
  private static final String STATUS_ERROR = "error";
  private static final String STATUS_NOT_FOUND = "not_found";
  private static final String STATUS_RUNNING = "running";
  private static final String STATUS_SUCCESS = "success";
  private static final String STATUS_EXCEPTION = "exception";
  private static final String ENCODING_UTF8 = "UTF-8";
  private static final String HEADER_CACHE_CONTROL = "Cache-Control";
  private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
  private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";
  private static final String PROCEDURE_NAME = "procedureName";

  /**
   * GET Method: Checks the status of a Process Instance.
   * Useful for polling after an async call.
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String pInstanceId = request.getParameter(PARAM_ID);

    JSONObject jsonResponse = new JSONObject();

    if (pInstanceId == null || pInstanceId.isEmpty()) {
      try {
        jsonResponse.put(JSON_STATUS, STATUS_ERROR);
        jsonResponse.put(JSON_MESSAGE, "Parameter 'id' (Process Instance ID) is missing.");
        writeResult(response, jsonResponse);
      } catch (Exception e) {
        throw new OBException(e);
      }
      return;
    }

    OBContext.setAdminMode(true);
    try {
      ProcessInstance pInstance = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);

      if (pInstance == null) {
        jsonResponse.put(JSON_STATUS, STATUS_NOT_FOUND);
        jsonResponse.put(JSON_MESSAGE, "Process Instance not found with ID: " + pInstanceId);
      } else {
        // Map PInstance to JSON
        jsonResponse.put(JSON_PROCESS_INSTANCE_ID, pInstance.getId());
        jsonResponse.put(JSON_PROCESS_ID, pInstance.getProcess().getId());
        jsonResponse.put(JSON_RESULT, pInstance.getResult()); // 0 or 1
        jsonResponse.put(JSON_MESSAGE, pInstance.getErrorMsg());

        // Interpret Status
        // Result 0 can mean "Error" OR "Processing". We differentiate by message convention.
        boolean isProcessing = (pInstance.getResult() == 0L &&
            (pInstance.getErrorMsg() == null || pInstance.getErrorMsg().contains("Processing")));

        if (isProcessing) {
          jsonResponse.put(JSON_STATUS, STATUS_RUNNING);
        } else if (pInstance.getResult() == 1L) {
          jsonResponse.put(JSON_STATUS, STATUS_SUCCESS);
        } else {
          jsonResponse.put(JSON_STATUS, STATUS_ERROR);
        }
      }
    } catch (Exception e) {
      try {
        jsonResponse.put(JSON_STATUS, STATUS_EXCEPTION);
        jsonResponse.put(JSON_MESSAGE, e.getMessage());
      } catch (JSONException ex) {
        throw new OBException(ex);
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    try {
      writeResult(response, jsonResponse);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * POST Method: Triggers the execution of a process.
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    JSONObject jsonResponse = new JSONObject();

    try {
      // 1. Parse Request Body
      JSONObject jsonBody = parseBody(request);

      String processId = jsonBody.optString(JSON_PROCESS_ID);
      String procedureName = jsonBody.optString(PROCEDURE_NAME); // Alternative lookup
      String recordId = jsonBody.optString(JSON_RECORD_ID, null);
      boolean async = jsonBody.optBoolean(JSON_ASYNC, true); // Default to ASYNC
      JSONObject paramsJson = jsonBody.optJSONObject(JSON_PARAMS);

      // 2. Resolve Process Definition
      Process process = null;
      OBContext.setAdminMode(true);
      try {
        if (processId != null && !processId.isEmpty()) {
          process = OBDal.getInstance().get(Process.class, processId);
        } else if (procedureName != null && !procedureName.isEmpty()) {
          // Find by Procedure Name (Standard CallProcess logic)
          // Note: In a real servlet we might query DB here, but let's assume client sends ID mostly
          // or we throw error to keep it simple.
          throw new OBException("Please provide 'processId' (UUID). Lookup by procedureName not fully implemented in Servlet context.");
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      if (process == null) {
        throw new OBException("Process definition not found. Provide a valid 'processId'.");
      }

      // 3. Convert Params (JSON -> Map)
      Map<String, Object> parameters = new HashMap<>();
      if (paramsJson != null) {
        Iterator<?> keys = paramsJson.keys();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          Object value = paramsJson.get(key);
          // Simple type conversion could be added here if needed (e.g. ISO Date String to Java Date)
          parameters.put(key, value);
        }
      }

      // 4. EXECUTE
      ProcessInstance pInstance;
      if (async) {
        // Use the new ASYNC engine
        pInstance = CallAsyncProcess.getInstance().callProcess(process, recordId, parameters, true);
        jsonResponse.put(JSON_EXECUTION_MODE, EXECUTION_MODE_ASYNC);
      } else {
        // Use the new/refactored SYNC engine
        pInstance = CallProcess.getInstance().callProcess(process, recordId, parameters, true);
        jsonResponse.put(JSON_EXECUTION_MODE, EXECUTION_MODE_SYNC);
      }

      // 5. Build Response
      jsonResponse.put(JSON_STATUS, STATUS_OK);
      jsonResponse.put(JSON_PROCESS_INSTANCE_ID, pInstance.getId());
      jsonResponse.put(JSON_MESSAGE, pInstance.getErrorMsg());
      jsonResponse.put(JSON_RESULT, pInstance.getResult());

    } catch (Exception e) {
      try {
        jsonResponse.put(JSON_STATUS, STATUS_ERROR);
        jsonResponse.put(JSON_MESSAGE, e.getMessage());
        e.printStackTrace();
      } catch (JSONException ex) {
        throw new OBException(ex);
      }
    }

    try {
      writeResult(response, jsonResponse);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  // ===========================================================================
  // Helpers
  // ===========================================================================

  /**
   * Reads the raw request body and converts it into a JSON object for downstream processing.
   *
   * @param request HTTP request containing the JSON payload.
   * @return Parsed {@link JSONObject} representing the body, or an empty object when no content.
   * @throws Exception if the input stream cannot be read or parsed.
   */
  private JSONObject parseBody(HttpServletRequest request) throws Exception {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), ENCODING_UTF8));
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }
    String body = sb.toString();
    return body.isEmpty() ? new JSONObject() : new JSONObject(body);
  }

  /**
   * Writes the provided JSON response using the servlet response writer with the proper headers.
   *
   * @param response servlet response where the JSON payload will be written.
   * @param json response body to send back to the client.
   * @throws Exception if writing to the response stream fails.
   */
  private void writeResult(HttpServletResponse response, JSONObject json) throws Exception {
    response.setContentType(CONTENT_TYPE_JSON_UTF8);
    response.setHeader(HEADER_CACHE_CONTROL, HEADER_CACHE_CONTROL_VALUE);
    Writer w = response.getWriter();
    w.write(json.toString());
    w.close();
  }

}
