package com.etendoerp.etendorx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

import com.etendoerp.etendorx.data.InstanceConnector;
import com.smf.securewebservices.utils.SecureWebServicesUtils;

@RunWith(MockitoJUnitRunner.class)
public class ConnectorPropertiesInjectorTest {

  private ConnectorPropertiesInjector injector;

  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<InstanceConnector> criteria;

  @Mock
  private InstanceConnector instanceConnector;

  @Mock
  private User tokenUser;

  @Before
  public void setup() {
    injector = new ConnectorPropertiesInjector();

    when(obDal.createCriteria(InstanceConnector.class)).thenReturn(criteria);
    when(criteria.setMaxResults(1)).thenReturn(criteria);
  }

  private JSONObject createWorkerJSON() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put("name", "worker");

    JSONObject source = new JSONObject();
    JSONArray propertySources = new JSONArray();
    propertySources.put(new JSONObject().put("source", source));
    sourceJSON.put("propertySources", propertySources);

    return sourceJSON;
  }

  @Test
  public void testInjectConfigSuccess() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<SecureWebServicesUtils> mockedUtils = mockStatic(SecureWebServicesUtils.class)) {

      String connectorId = "test-connector-id";
      String expectedToken = "test-token";
      JSONObject sourceJSON = createWorkerJSON();

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(criteria.uniqueResult()).thenReturn(instanceConnector);
      when(instanceConnector.getId()).thenReturn(connectorId);
      when(instanceConnector.getUserForToken()).thenReturn(tokenUser);
      mockedUtils.when(() -> SecureWebServicesUtils.generateToken(tokenUser)).thenReturn(expectedToken);

      injector.injectConfig(sourceJSON);

      JSONObject resultSource = sourceJSON.getJSONArray("propertySources")
          .getJSONObject(0)
          .getJSONObject("source");

      assertEquals(connectorId, resultSource.getString("connector.instance"));
      assertEquals(expectedToken, resultSource.getString("token"));
      assertEquals(expectedToken, resultSource.getString("classic.token"));
    }
  }

  @Test(expected = OBException.class)
  public void testInjectConfigNoConnectorInstance() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      JSONObject sourceJSON = createWorkerJSON();

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(criteria.uniqueResult()).thenReturn(null);

      injector.injectConfig(sourceJSON);
    }
  }

  @Test
  public void testInjectConfigNonWorkerService() throws Exception {
    JSONObject sourceJSON = new JSONObject();
    sourceJSON.put("name", "other-service");

    injector.injectConfig(sourceJSON);

    assertFalse(sourceJSON.has("propertySources"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testInjectConfig_WithProvider() throws Exception {
    injector.injectConfig(new JSONObject(), null);
  }
}