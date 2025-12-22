package com.etendoerp.etendorx.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.utils.RXServiceManagementUtils;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.erpCommon.utility.OBMessageUtils;

class RestartRXServicesTest {

  private MockedStatic<RXServiceManagementUtils> rxServiceManagementUtilsMockedStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsMockedStatic;

  @BeforeEach
  void setUp() {
    rxServiceManagementUtilsMockedStatic = mockStatic(RXServiceManagementUtils.class);
    obMessageUtilsMockedStatic = mockStatic(OBMessageUtils.class);
    obMessageUtilsMockedStatic.when(() -> OBMessageUtils.messageBD(anyString())).thenAnswer(i -> i.getArgument(0));
  }

  @AfterEach
  void tearDown() {
    rxServiceManagementUtilsMockedStatic.close();
    obMessageUtilsMockedStatic.close();
  }

  @Test
  void testActionSuccess() throws Exception {
    ETRXConfig config = mock(ETRXConfig.class);
    when(config.getServiceName()).thenReturn("testService");
    when(config.getServiceURL()).thenReturn("http://localhost:8080");

    RestartRXServices action = new RestartRXServices() {
      @Override
      protected <T extends BaseOBObject> List<T> getInputContents(Class<T> clazz) {
        return Collections.singletonList((T) config);
      }
    };

    doAnswer(invocation -> {
      ActionResult result = invocation.getArgument(1);
      result.setType(Result.Type.SUCCESS);
      return null;
    }).when(RXServiceManagementUtils.class);
    RXServiceManagementUtils.checkRunning(anyString(), any(ActionResult.class));

    doAnswer(invocation -> {
      ActionResult result = invocation.getArgument(1);
      result.setType(Result.Type.SUCCESS);
      return null;
    }).when(RXServiceManagementUtils.class);
    RXServiceManagementUtils.performRestart(anyString(), any(ActionResult.class));

    ActionResult result = action.action(new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.SUCCESS, result.getType());
  }

  @Test
  void testActionConfigService() throws Exception {
    ETRXConfig config = mock(ETRXConfig.class);
    when(config.getServiceName()).thenReturn("config");
    when(config.getServiceURL()).thenReturn("http://localhost:8080");

    RestartRXServices action = new RestartRXServices() {
      @Override
      protected <T extends BaseOBObject> List<T> getInputContents(Class<T> clazz) {
        return Collections.singletonList((T) config);
      }
    };

    ActionResult result = action.action(new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.WARNING, result.getType());
    assertEquals("ETRX_ConfigCanNotRestart", result.getMessage());
  }

  @Test
  void testActionNullService() throws Exception {
    RestartRXServices action = new RestartRXServices() {
      @Override
      protected <T extends BaseOBObject> List<T> getInputContents(Class<T> clazz) {
        return Collections.singletonList(null);
      }
    };

    ActionResult result = action.action(new JSONObject(), new MutableBoolean(false));

    assertEquals(Result.Type.ERROR, result.getType());
  }

}
