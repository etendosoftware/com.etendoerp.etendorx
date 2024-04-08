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
import java.util.stream.Collectors;

/**
 * This class extends ReadOnlyDataSourceService and is used to handle asynchronous process logs.
 */
public class AsyncProcessLogsDS extends ReadOnlyDataSourceService {

  /**
   * Logger instance for logging events, info, and errors.
   */
  private static final Logger log = LogManager.getLogger();

  /**
   * This method returns the count of data fetched from the parameters.
   *
   * @param parameters a map of parameters used to fetch the data
   * @return the size of the data list
   */
  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  /**
   * This method fetches data based on the provided parameters and row range.
   *
   * @param parameters a map of parameters used to fetch the data
   * @param startRow   the starting index of the row range
   * @param endRow     the ending index of the row range
   * @return a list of maps representing the fetched data
   * @throws OBException if an error occurs while getting the data
   */
  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    try {
      return AsyncProcessUtil.getList("/async-process/latest")
          .stream()
          .map(this::convertRow)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error getting data", e);
      throw new OBException("Error getting data " + e.getMessage(), e);
    }
  }

  /**
   * This method converts a row of data into a new format.
   *
   * @param row a map representing a row of data
   * @return a map representing the converted row of data
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
      newRow.put("lastupdate", AsyncProcessUtil.fmtDate((String) row.get("lastUpdate")));
    } catch (ParseException e) {
      log.error("Error parsing date", e);
    }
    newRow.put("createdBy", "0");
    newRow.put("created", new Date());
    newRow.put("description", row.get("description"));
    return newRow;
  }

}
