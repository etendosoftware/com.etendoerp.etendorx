package com.etendoerp.etendorx.datasource;

import com.etendoerp.etendorx.utils.AsyncProcessUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link AsyncProcessExecLogsDS} class.
 * This class uses Mockito to mock static methods and verify the behavior of the data source.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncProcessExecLogsDSTest {

  @InjectMocks
  private AsyncProcessExecLogsDS asyncProcessExecLogsDS;

  private static final String ETRX_ASYNC_PROC_ID = "@ETRX_Async_Proc.id@";
  private static final  String ASYNC_PROC_TEST_ID = "1";

  /**
   * Tests the {@code getCount} method of {@link AsyncProcessExecLogsDS}.
   * Verifies that the method correctly calculates the count of rows
   */
  @Test
  public void testGetCount() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ETRX_ASYNC_PROC_ID, ASYNC_PROC_TEST_ID);
    List<Map<String, Object>> mockData = getMockData();

    try (MockedStatic<AsyncProcessUtil> mockedAsyncProcessUtil = mockStatic(AsyncProcessUtil.class)) {
      when(AsyncProcessUtil.getList(any())).thenReturn(mockData);

      int count = asyncProcessExecLogsDS.getCount(parameters);
      assertEquals(mockData.size(), count);
    }
  }

  /**
   * Tests the {@code getData} method of {@link AsyncProcessExecLogsDS}.
   * Ensures the method retrieves the correct subset of data
   * based on the given parameters and row range.
   */
  @Test
  public void testGetData() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ETRX_ASYNC_PROC_ID, ASYNC_PROC_TEST_ID);
    int startRow = 0;
    int endRow = 10;
    List<Map<String, Object>> mockData = getMockData();

    try (MockedStatic<AsyncProcessUtil> mockedAsyncProcessUtil = mockStatic(AsyncProcessUtil.class)) {
      when(AsyncProcessUtil.getList(any())).thenReturn(mockData);

      List<Map<String, Object>> data = asyncProcessExecLogsDS.getData(parameters, startRow, endRow);
      assertEquals(mockData.size(), data.size());
    }
  }

  /**
   * Provides mock data for testing purposes.
   * Each row represents a process log with predefined fields.
   *
   * @return a list of mock data rows.
   */
  private List<Map<String, Object>> getMockData() {
    List<Map<String, Object>> mockData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put("id", "1");
    row1.put("state", "RUNNING");
    row1.put("time", "2023-04-01 10:00:00");
    row1.put("description", "Test process 1");
    row1.put("params", "{\"param1\":\"value1\"}");
    row1.put("log", "This is a log message");
    mockData.add(row1);

    Map<String, Object> row2 = new HashMap<>();
    row2.put("id", "2");
    row2.put("state", "COMPLETED");
    row2.put("time", "2023-04-02 15:30:00");
    row2.put("description", "Test process 2");
    row2.put("params", "{\"param2\":\"value2\"}");
    row2.put("log", "This is another log message");
    mockData.add(row2);

    return mockData;
  }
}