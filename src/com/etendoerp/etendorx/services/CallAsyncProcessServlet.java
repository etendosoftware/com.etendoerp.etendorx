package com.etendoerp.etendorx.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.etendoerp.copilot.util.OpenAIUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * This servlet provides a web service interface to trigger Openbravo/Etendo processes
 * and poll for their execution status.
 *
 * <p><b>URL:</b> /ws/com.etendoerp.etendorx.services.CallAsyncProcessServlet</p>
 *
 * <p><b>Method: POST (Execute)</b></p>
 * Triggers a process execution.
 * Payload:
 * <pre>
 * {
 *   "processId": "32 Characters UUID",
 *   "recordId": "Target Record UUID (Optional)",
 *   "async": true,
 *   "params": {
 *     "ParameterName": "Value",
 *     "DateParam": "2025-01-01"
 *   }
 * }
 * </pre>
 *
 * <p><b>Method: GET (Status Polling)</b></p>
 * Checks the status of a previously triggered process instance.
 * Param: ?id=PInstanceID
 */
public class CallAsyncProcessServlet implements WebService {

  private static final Logger log4j = LogManager.getLogger(CallAsyncProcessServlet.class);
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
  private static final String HEADER_CACHE_CONTROL = "Cache-Control";
  private static final String HEADER_CACHE_CONTROL_VALUE = "no-cache";
  private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

  /**
   * GET Method: Checks the status of a Process Instance.
   * Useful for polling after an asynchronous call to track progress and retrieve results.
   *
   * @param path the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request the HttpServletRequest containing the 'id' parameter
   * @param response the HttpServletResponse where the status JSON will be written
   * @throws Exception if an error occurs during status retrieval
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String pInstanceId = request.getParameter(PARAM_ID);
    JSONObject jsonResponse = new JSONObject();

    if (pInstanceId == null || pInstanceId.isEmpty()) {
      jsonResponse.put(JSON_STATUS, STATUS_ERROR);
      jsonResponse.put(JSON_MESSAGE, "Parameter 'id' (Process Instance ID) is missing.");
      writeResult(response, jsonResponse);
      return;
    }

    try {
      OBContext.setAdminMode(true);
      ProcessInstance pInstance = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);

      if (pInstance == null) {
        jsonResponse.put(JSON_STATUS, STATUS_NOT_FOUND);
        jsonResponse.put(JSON_MESSAGE, "Process Instance not found with ID: " + pInstanceId);
      } else {
        fillProcessInstanceData(jsonResponse, pInstance);
      }
    } catch (Exception e) {
      log4j.error("Error while fetching Process Instance status", e);
      jsonResponse.put(JSON_STATUS, STATUS_EXCEPTION);
      jsonResponse.put(JSON_MESSAGE, "An error occurred while processing the request.");
    } finally {
      OBContext.restorePreviousMode();
    }

    writeResult(response, jsonResponse);
  }

  /**
   * POST Method: Triggers the execution of a process.
   * Supports both synchronous and asynchronous execution modes.
   *
   * @param path the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request the HttpServletRequest containing the JSON payload with process details
   * @param response the HttpServletResponse where the execution result or instance ID will be written
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    JSONObject jsonResponse = new JSONObject();

    try {
      // 1. Parse Request Body
      JSONObject jsonBody = parseBody(request);

      String processId = jsonBody.optString(JSON_PROCESS_ID);
      String recordId = jsonBody.optString(JSON_RECORD_ID, null);
      boolean async = jsonBody.optBoolean(JSON_ASYNC, true); // Default to ASYNC
      JSONObject paramsJson = jsonBody.optJSONObject(JSON_PARAMS);

      // 2. Resolve Process Definition
      Process process = getProcess(processId);

      // 3. Convert Params (JSON -> Map)
      Map<String, Object> parameters = convertParams(paramsJson);

      // 4. EXECUTE
      ProcessInstance pInstance = executeProcess(process, recordId, parameters, async, jsonResponse);

      // 5. Build Response
      jsonResponse.put(JSON_STATUS, STATUS_OK);
      jsonResponse.put(JSON_PROCESS_INSTANCE_ID, pInstance.getId());
      jsonResponse.put(JSON_MESSAGE, pInstance.getErrorMsg());
      jsonResponse.put(JSON_RESULT, pInstance.getResult());

    } catch (Exception e) {
      log4j.error("Error while executing process", e);
      try {
        jsonResponse.put(JSON_STATUS, STATUS_ERROR);
        jsonResponse.put(JSON_MESSAGE, "An error occurred while processing the request.");
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

  /**
   * DELETE Method: Not Implemented.
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new OBException("DELETE method is not implemented for CallAsyncProcessServlet.");
  }

  /**
   * PUT Method: Not Implemented.
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   * @throws Exception
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new OBException("PUT method is not implemented for CallAsyncProcessServlet.");
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
    BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
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

  /**
   * Resolves a Process definition from the database using its UUID.
   *
   * @param processId The UUID of the process to retrieve.
   * @return The {@link Process} object.
   * @throws OBException If the processId is missing or the process cannot be found.
   */
  private Process getProcess(String processId) {
    if (processId == null || processId.isEmpty()) {
      throw new OBException("Please provide 'processId' (UUID).");
    }
    Process process = null;
    try {
      OBContext.setAdminMode(true);
      process = OBDal.getInstance().get(Process.class, processId);
    } finally {
      OBContext.restorePreviousMode();
    }
    if (process == null) {
      throw new OBException("Process definition not found. Provide a valid 'processId'.");
    }
    return process;
  }

  /**
   * Converts a JSON object of parameters into a Map suitable for process execution.
   *
   * @param paramsJson The {@link JSONObject} containing parameter keys and values.
   * @return A {@link Map} of parameters.
   * @throws JSONException If an error occurs during JSON parsing.
   */
  private Map<String, Object> convertParams(JSONObject paramsJson) throws JSONException {
    Map<String, Object> parameters = new HashMap<>();
    if (paramsJson != null) {
      Iterator<?> keys = paramsJson.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        Object value = paramsJson.get(key);
        parameters.put(key, value);
      }
    }
    return parameters;
  }

  /**
   * Executes the specified process using either the synchronous or asynchronous engine.
   *
   * @param process The process definition to execute.
   * @param recordId The ID of the record to process (optional).
   * @param parameters The parameters for the process.
   * @param async Whether to execute asynchronously.
   * @param jsonResponse The JSON response object to update with execution mode.
   * @return The resulting {@link ProcessInstance}.
   * @throws JSONException If an error occurs while updating the response.
   */
  private ProcessInstance executeProcess(Process process, String recordId, Map<String, Object> parameters,
      boolean async, JSONObject jsonResponse) throws JSONException {
    ProcessInstance pInstance;
    if (async) {
      pInstance = CallAsyncProcess.getInstance().callProcess(process, recordId, parameters, true);
      jsonResponse.put(JSON_EXECUTION_MODE, EXECUTION_MODE_ASYNC);
    } else {
      pInstance = CallProcess.getInstance().callProcess(process, recordId, parameters, true);
      jsonResponse.put(JSON_EXECUTION_MODE, EXECUTION_MODE_SYNC);
    }
    return pInstance;
  }

  /**
   * Fills the JSON response with data from a Process Instance and interprets its status.
   *
   * @param jsonResponse The {@link JSONObject} to populate.
   * @param pInstance The {@link ProcessInstance} containing the execution data.
   * @throws JSONException If an error occurs while updating the response.
   */
  private void fillProcessInstanceData(JSONObject jsonResponse, ProcessInstance pInstance) throws JSONException {
    jsonResponse.put(JSON_PROCESS_INSTANCE_ID, pInstance.getId());
    jsonResponse.put(JSON_PROCESS_ID, pInstance.getProcess().getId());
    jsonResponse.put(JSON_RESULT, pInstance.getResult());
    jsonResponse.put(JSON_MESSAGE, pInstance.getErrorMsg());

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

}
