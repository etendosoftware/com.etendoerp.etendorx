package com.etendoerp.etendorx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Properties;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GetSSOProperties using JUnit 5 and Mockito.
 */
@ExtendWith(MockitoExtension.class)
class GetSSOPropertiesTest {

  private MockedStatic<OBPropertiesProvider> obPropsStatic;

  @AfterEach
  void tearDown() {
    if (obPropsStatic != null) {
      obPropsStatic.close();
    }
  }

  @Test
  void testExecute_ExistingProperties() throws Exception {
    Properties fakeProps = new Properties();
    fakeProps.setProperty("sso.url", "http://example.com");
    fakeProps.setProperty("sso.clientId", "client123");
    // "sso.missing" not set

    JSONObject result = executeHandler(fakeProps, "url, clientId, missing");

    assertEquals("http://example.com", result.getString("url"));
    assertEquals("client123", result.getString("clientId"));
    assertFalse(result.has("missing"));
  }

  @Test
  void testExecute_InvalidJSON_ThrowsOBException() {
    // Mock OBPropertiesProvider so handler won't fail on properties lookup
    Properties fakeProps = new Properties();
    var providerMock = mock(OBPropertiesProvider.class);
    when(providerMock.getOpenbravoProperties()).thenReturn(fakeProps);
    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(providerMock);

    GetSSOProperties handler = new GetSSOProperties();
    String invalidContent = "not a JSON";

    OBException ex = assertThrows(OBException.class, () -> {
      handler.execute(Map.of(), invalidContent);
    });
    assertTrue(ex.getCause() instanceof JSONException);
  }

  @Test
  void testExecute_NoMatchingProperties() throws Exception {
    Properties fakeProps = new Properties();
    fakeProps.setProperty("otherKey", "value");
    // no "sso.*" entries

    JSONObject result = executeHandler(fakeProps, "url, clientId");
    assertEquals(0, result.length());
  }

  /**
   * Mocks OBPropertiesProvider to return the given Properties, then calls
   * GetSSOProperties.execute with a JSON that has "properties" key set to comma-separated propsList.
   * Returns the resulting JSONObject.
   */
  private JSONObject executeHandler(Properties propsToReturn, String propsList) throws Exception {
    var providerMock = mock(OBPropertiesProvider.class);
    when(providerMock.getOpenbravoProperties()).thenReturn(propsToReturn);

    obPropsStatic = mockStatic(OBPropertiesProvider.class);
    obPropsStatic.when(OBPropertiesProvider::getInstance).thenReturn(providerMock);

    JSONObject input = new JSONObject();
    input.put("properties", propsList);
    return new GetSSOProperties().execute(Map.of(), input.toString());
  }
}
