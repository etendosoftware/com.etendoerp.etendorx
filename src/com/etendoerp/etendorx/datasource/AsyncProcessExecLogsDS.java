/*
 * Copyright 2022-2024  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendoerp.etendorx.datasource;

import com.etendoerp.etendorx.utils.AsyncProcessUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class extends ReadOnlyDataSourceService and is used to handle asynchronous process execution logs.
 */
public class AsyncProcessExecLogsDS extends ReadOnlyDataSourceService {

  private static final Logger log = LogManager.getLogger();

  // ThreadLocal instance for storing line number information.
  private ThreadLocal<Integer> lineno = new ThreadLocal<>();

  /**
   * This method is used to get the count of data.
   * @param parameters The parameters used to get the data.
   * @return The size of the data.
   */
  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  /**
   * This method is used to get the data.
   * @param parameters The parameters used to get the data.
   * @param startRow The starting row for getting the data.
   * @param endRow The ending row for getting the data.
   * @return The list of data.
   */
  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    try {
      return AsyncProcessUtil.getList("/async-process/" + parameters.get("@ETRX_Async_Proc.id@"),
          rowCount -> lineno.set(rowCount), this::convertRow);
    } catch (Exception e) {
      log.error("Error getting data", e);
      throw new OBException("Error getting data " + e.getMessage(), e);
    } finally {
      lineno.remove();
    }
  }

  /**
   * This method is used to convert a row of data.
   * @param row The row to be converted.
   * @return The converted row.
   */
  private Map<String, Object> convertRow(Map<String, Object> row) {
    Map<String, Object> newRow = new HashMap<>();
    newRow.put("id", row.get("id"));
    newRow.put("organization", "0");
    newRow.put("client", "0");
    newRow.put("state", row.get("state"));
    newRow.put("isactive", true);
    newRow.put("updated", new Date());
    newRow.put("updatedBy", "0");
    try {
      newRow.put("lastupdate", AsyncProcessUtil.fmtDate((String) row.get("time")));
    } catch (ParseException e) {
      log.error("Error parsing date", e);
    }
    newRow.put("async_ID", row.get("id"));
    newRow.put("createdBy", "0");
    newRow.put("created", new Date());
    newRow.put("description", row.get("description"));
    newRow.put("params", row.get("params"));
    newRow.put("log", row.get("log"));
    newRow.put("lineno", lineno.get());
    lineno.set(lineno.get() - 10);
    return newRow;
  }

}
