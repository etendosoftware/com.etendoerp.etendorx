package com.etendoerp.etendorx.actionhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Properties;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.etendorx.data.ETRXoAuthProvider;

/**
 * Unit tests for ConfigEtendoMiddleware using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ConfigEtendoMiddlewareTest {

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<OBPropertiesProvider> obPropsStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  @AfterEach
  void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
    if (obPropsStatic != null) obPropsStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
  }

  /**
   * Scenario: an ETRXoAuthProvider with value="EtendoMiddleware" already exists.
   * Then execute() returns a JSON with severity="warning" and the warning text.
   */
  @Test
  void testExecute_whenProviderExists_returnsWarningJson() throws Exception {
    // 1) Mock OBDal.getInstance().createCriteria(...).uniqueResult() → return a non-null existing object
    var dalMock = mock(OBDal.class);
    OBCriteria<ETRXoAuthProvider> criteriaMock = mock(OBCriteria.class);
    var existingProvider = mock(ETRXoAuthProvider.class);

    when(dalMock.createCriteria(ETRXoAuthProvider.class)).thenReturn((OBCriteria<ETRXoAuthProvider>) criteriaMock);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.setMaxResults(1)).thenReturn(criteriaMock);
    when(criteriaMock.uniqueResult()).thenReturn(existingProvider);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);

    // 2) Mock OBMessageUtils.messageBD for "Warning" and "ETRX_Middleware_Config_AlreadyExist"
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    when(OBMessageUtils.messageBD("Warning")).thenReturn("WarningText");
    when(OBMessageUtils.messageBD("ETRX_Middleware_Config_AlreadyExist"))
        .thenReturn("AlreadyExistText");

    // 3) Invoke execute()
    ConfigEtendoMiddleware handler = new ConfigEtendoMiddleware();
    // parameters map may contain anything, but since provider exists, they are ignored
    JSONObject result = handler.execute(Map.of("envURL", "ignored"), "{}");

    // 4) Verify returned JSON
    assertEquals("warning", result.getString("severity"));
    assertEquals("WarningText<br/>AlreadyExistText", result.getString("text"));

    // 5) Verify that OBProvider and OBPropertiesProvider were never invoked
    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.verifyNoInteractions();
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.verifyNoInteractions();

    // 6) Verify OBDal.save was never called
    verify(dalMock, never()).save(any());
  }

  /**
   * Scenario: no ETRXoAuthProvider with value="EtendoMiddleware" exists.
   * Then execute() should:
   *  - return JSON with severity="success" and success text,
   *  - create a new ETRXoAuthProvider, set its fields correctly,
   *  - and call OBDal.save(...) with that new entity.
   */
  @Test
  void testExecute_whenNoProvider_createsAndSavesProvider() throws Exception {
    // 1) Mock OBDal.getInstance().createCriteria(...).uniqueResult() → return null
    var dalMock = mock(OBDal.class);
    @SuppressWarnings("unchecked")
    OBCriteria<ETRXoAuthProvider> criteriaMock = mock(OBCriteria.class);

    when(dalMock.createCriteria(ETRXoAuthProvider.class)).thenReturn((OBCriteria<ETRXoAuthProvider>) criteriaMock);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.setMaxResults(1)).thenReturn(criteriaMock);
    when(criteriaMock.uniqueResult()).thenReturn(null);

    // prepare OBDal.save(...)
    doNothing().when(dalMock).save(any(ETRXoAuthProvider.class));

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(dalMock);

    // 2) Mock OBProvider.getInstance().get(ETRXoAuthProvider.class) → return a mock provider entity
    var providerMock = mock(OBProvider.class);
    var newProviderMock = mock(ETRXoAuthProvider.class);

    when(providerMock.get(ETRXoAuthProvider.class)).thenReturn(newProviderMock);

    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(providerMock);

    // 3) Mock OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.middleware.url")
    Properties fakeProps = new Properties();
    fakeProps.setProperty("sso.middleware.url", "http://middleware.com");
    var propsProviderMock = mock(OBPropertiesProvider.class);
    when(propsProviderMock.getOpenbravoProperties()).thenReturn(fakeProps);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(propsProviderMock);

    // 4) Mock OBMessageUtils.messageBD for "success" and "ETRX_Middleware_Configuration_Success"
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    when(OBMessageUtils.messageBD("success")).thenReturn("SuccessText");
    when(OBMessageUtils.messageBD("ETRX_Middleware_Configuration_Success"))
        .thenReturn("ConfiguredText");

    // 5) Invoke execute() with an envURL parameter
    String envURL = "http://app.com/";
    ConfigEtendoMiddleware handler = new ConfigEtendoMiddleware();
    JSONObject result = handler.execute(Map.of("envURL", envURL), "{}");

    // 6) Verify returned JSON
    assertEquals("success", result.getString("severity"));
    assertEquals("SuccessText<br/>ConfiguredText", result.getString("text"));

    // 7) Verify setters on newProviderMock with correct arguments
    verify(newProviderMock).setValue("EtendoMiddleware");
    verify(newProviderMock).setClientName("EtendoMiddleware");
    verify(newProviderMock).setIDForClient("---");
    verify(newProviderMock).setClientSecret("---");
    verify(newProviderMock).setScope("---");
    verify(newProviderMock).setRedirectURI(envURL + "saveTokenMiddleware");
    verify(newProviderMock).setAuthorizationEndpoint("http://middleware.com/oauth-integrations");

    // 8) Verify OBDal.save(...) was called exactly with newProviderMock
    verify(dalMock).save(newProviderMock);
  }
}
