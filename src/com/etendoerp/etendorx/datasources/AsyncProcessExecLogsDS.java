package com.etendoerp.etendorx.datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AsyncProcessExecLogsDS extends ReadOnlyDataSourceService {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    HttpClient client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8099/async-process/" + parameters.get("@ETRX_Async_Proc.id@")))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNzA3OTQ3NjcyLCJhZF91c2VyX2lkIjoiNUE3OTY2NzA5Njk2NEU4M0E2OTg1RDU0OUM5ODgyNzUiLCJhZF9jbGllbnRfaWQiOiIyM0M1OTU3NUI5Q0Y0NjdDOTYyMDc2MEVCMjU1QjM4OSIsImFkX29yZ19pZCI6IkI4NDNDMzA0NjFFQTQ1MDE5MzVDQjFEMTI1QzlDMjVBIiwiYWRfcm9sZV9pZCI6IjQyRDBFRUIxQzY2RjQ5N0E5MERENTI2REM1OTdFNkYwIiwic2VhcmNoX2tleSI6Im9iY29ubiIsInNlcnZpY2VfaWQiOiIwNzkzNjk2RDI1OTg0MTUwOEQ4M0VBRUZFODcwMDAwRCJ9.GhurQlHq-IEmeRNz7lTIRsNay_zONK-XjitsmGop62edCsfMk5LTBbiFKQVF0oqUSkm3Kp3gCvWns9HkGIL7EY-hCwr1GvciCa-bMPLp6VCc_tpoO89Msx_K-crfc28yxE0MemAHJaD48w-tya1bKG_qVfW3GhJaqLxIQC5MQybYhPLjC5hq6X8V5Icn3zO258QsYOYEvbk5LGPjL37tJAVRlzaAvPAWuXofh0IfPf4b0sjhXXK7PAQAohmvLp0MXaqiaL8baujXJlE50o5fQ9hhNvj_sy_xjTXSvYN1YP93wsKnRDy0Rg9T2r24hDNtT7aCKo9uKvaRx9i6_3TvHQ")
        .GET()
        .build();
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      ObjectMapper objectMapper = new ObjectMapper();
      Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
      List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("executions");
      List<Map<String, Object>> result = new ArrayList<>();
      SimpleDateFormat fmtToDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS");

      int lineno = rows.size() * 10;
      for (Map<String, Object> stringObjectMap : rows) {
        Map<String, Object> newRow = new HashMap<>();
        newRow.put("id", stringObjectMap.get("id"));
        newRow.put("organization", "0");
        newRow.put("client", "0");
        newRow.put("state", stringObjectMap.get("state"));
        newRow.put("isactive", true);
        newRow.put("updated", new Date());
        newRow.put("updatedBy", "0");
        try {
          newRow.put("time", fmtToDate.parse((String) stringObjectMap.get("time")));
        } catch (ParseException ignored) {}
        newRow.put("async_ID", stringObjectMap.get("id"));
        newRow.put("createdBy", "0");
        newRow.put("created", new Date());
        newRow.put("description", stringObjectMap.get("description"));
        newRow.put("params", stringObjectMap.get("params"));
        newRow.put("log", stringObjectMap.get("log"));
        newRow.put("lineno", lineno);
        result.add(newRow);
        lineno -= 10;
      }
      return result;
    } catch (IOException e) {
      log.error("Error getting data from async-process", e);
      return Collections.emptyList();
    } catch (InterruptedException e) {
      throw new OBException("Error getting data from async-process", e);
    }
  }

}
