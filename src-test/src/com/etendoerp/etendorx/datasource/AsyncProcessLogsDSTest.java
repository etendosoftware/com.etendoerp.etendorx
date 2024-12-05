package com.etendoerp.etendorx.datasource;

import com.etendoerp.etendorx.utils.AsyncProcessUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link AsyncProcessLogsDS} class.
 * This class verifies the behavior of methods that handle asynchronous process logs,
 * including retrieving the count of logs and their details.
 */

@RunWith(MockitoJUnitRunner.class)
public class AsyncProcessLogsDSTest {

  @InjectMocks
  private AsyncProcessLogsDS asyncProcessLogsDS;


  /**
   * Tests the {@code getCount} method of {@link AsyncProcessLogsDS}.
   * Ensures that the method correctly calculates the number of logs
   * returned by {@link AsyncProcessUtil(Map)}.
   */
  @Test
  public void testGetCount() {
    Map<String, String> parameters = new HashMap<>();
    List<Map<String, Object>> mockData = getMockData();

    try (MockedStatic<AsyncProcessUtil> mockedAsyncProcessUtil = mockStatic(AsyncProcessUtil.class)) {
      when(AsyncProcessUtil.getList(any())).thenReturn(mockData);

      int count = asyncProcessLogsDS.getCount(parameters);
      assertEquals(mockData.size(), count);
    }
  }

  /**
   * Tests the {@code getData} method of {@link AsyncProcessLogsDS}.
   * Ensures that the method retrieves the correct log data
   * for the specified range of rows.
   */
  @Test
  public void testGetData() {
    Map<String, String> parameters = new HashMap<>();
    int startRow = 0;
    int endRow = 10;
    List<Map<String, Object>> mockData = getMockData();

    try (MockedStatic<AsyncProcessUtil> mockedAsyncProcessUtil = mockStatic(AsyncProcessUtil.class)) {
      when(AsyncProcessUtil.getList(any())).thenReturn(mockData);

      List<Map<String, Object>> data = asyncProcessLogsDS.getData(parameters, startRow, endRow);
      assertEquals(mockData.size(), data.size());
    }
  }

  /**
   * Tests the {@code getData} method to handle exceptions.
   * Verifies that a {@link RuntimeException} thrown by {@link AsyncProcessUtil(Map)}
   * is correctly wrapped in an {@link OBException}.
   */
  @Test(expected = OBException.class)
  public void testGetDataWithException() {
    Map<String, String> parameters = new HashMap<>();
    int startRow = 0;
    int endRow = 10;

    try (MockedStatic<AsyncProcessUtil> mockedAsyncProcessUtil = mockStatic(AsyncProcessUtil.class)) {
      when(AsyncProcessUtil.getList(any())).thenThrow(new RuntimeException("Test exception"));

      asyncProcessLogsDS.getData(parameters, startRow, endRow);
    }
  }

  /**
   * Provides mock data for testing purposes.
   * Each entry in the list represents a simulated log of an asynchronous process.
   *
   * @return a list of mock data representing process logs.
   */
  private List<Map<String, Object>> getMockData() {
    List<Map<String, Object>> mockData = new ArrayList<>();
    Map<String, Object> row1 = new HashMap<>();
    row1.put("id", "1");
    row1.put("state", "RUNNING");
    row1.put("lastUpdate", "2023-04-01 10:00:00");
    row1.put("description", "Test process 1");
    mockData.add(row1);

    Map<String, Object> row2 = new HashMap<>();
    row2.put("id", "2");
    row2.put("state", "COMPLETED");
    row2.put("lastUpdate", "2023-04-02 15:30:00");
    row2.put("description", "Test process 2");
    mockData.add(row2);

    return mockData;
  }
}