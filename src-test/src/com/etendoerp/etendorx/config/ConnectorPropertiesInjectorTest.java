package com.etendoerp.etendorx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Language;

import com.etendoerp.etendorx.data.InstanceConnector;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

class ConnectorPropertiesInjectorTest {

  private MockedStatic<OBDal> obDalMockedStatic;
  private MockedStatic<OBContext> obContextMockedStatic;
  private MockedStatic<SecureWebServicesUtils> secureWebServicesUtilsMockedStatic;

  private ConnectorPropertiesInjector injector;

  @BeforeEach
  void setUp() {
    obDalMockedStatic = mockStatic(OBDal.class);
    obContextMockedStatic = mockStatic(OBContext.class);
    secureWebServicesUtilsMockedStatic = mockStatic(SecureWebServicesUtils.class);
    injector = new ConnectorPropertiesInjector();
  }

  @AfterEach
  void tearDown() {
    obDalMockedStatic.close();
    obContextMockedStatic.close();
    secureWebServicesUtilsMockedStatic.close();
  }

  @Test
  void testInjectConfigSuccess() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put("name", "worker");
    
    JSONObject source = new JSONObject();
    source.put("connector.instance", "test-instance-id");
    
    JSONObject propertySource = new JSONObject();
    propertySource.put("source", source);
    
    JSONArray propertySources = new JSONArray();
    propertySources.put(propertySource);
    
    sourceJSON.put("propertySources", propertySources);

    OBDal obDal = mock(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDal);
    
    InstanceConnector instanceConnector = mock(InstanceConnector.class);
    when(obDal.get(InstanceConnector.class, "test-instance-id")).thenReturn(instanceConnector);
    
    User user = mock(User.class);
    when(instanceConnector.getUserForToken()).thenReturn(user);
    
    secureWebServicesUtilsMockedStatic.when(() -> SecureWebServicesUtils.generateToken(user)).thenReturn("test-token");

    injector.injectConfig(sourceJSON);

    assertEquals("test-token", source.getString("token"));
    assertEquals("test-token", source.getString("classic.token"));
  }

  @Test
  void testInjectConfigNotWorker() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put("name", "other");
    
    injector.injectConfig(sourceJSON);
    // Should do nothing
  }

  @Test
  void testInjectConfigInstanceNotFound() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put("name", "worker");
    
    JSONObject source = new JSONObject();
    source.put("connector.instance", "test-instance-id");
    
    JSONObject propertySource = new JSONObject();
    propertySource.put("source", source);
    
    JSONArray propertySources = new JSONArray();
    propertySources.put(propertySource);
    
    sourceJSON.put("propertySources", propertySources);

    OBDal obDal = mock(OBDal.class);
    obDalMockedStatic.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.get(InstanceConnector.class, "test-instance-id")).thenReturn(null);
    
    OBContext context = mock(OBContext.class);
    obContextMockedStatic.when(OBContext::getOBContext).thenReturn(context);
    Language language = mock(Language.class);
    when(context.getLanguage()).thenReturn(language);

    assertThrows(OBException.class, () -> injector.injectConfig(sourceJSON));
  }
}
