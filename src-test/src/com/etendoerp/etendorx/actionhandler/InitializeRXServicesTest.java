package com.etendoerp.etendorx.actionhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONObject;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.test.base.TestConstants;

import com.etendoerp.etendorx.data.ETRXConfig;
import com.etendoerp.etendorx.utils.ServiceURLConfig;

@RunWith(Parameterized.class)
public class InitializeRXServicesTest extends WeldBaseTest {

  @InjectMocks
  private InitializeRXServices initializeRXServices;

  @Mock
  private OBPropertiesProvider obPropertiesProvider;

  private final boolean rxEnable;
  private final boolean tomcatEnable;
  private final boolean asyncEnable;
  private final boolean connectorEnable;

  // Constructor para recibir parámetros
  public InitializeRXServicesTest(boolean rxEnable, boolean tomcatEnable, boolean asyncEnable, boolean connectorEnable) {
    this.rxEnable = rxEnable;
    this.tomcatEnable = tomcatEnable;
    this.asyncEnable = asyncEnable;
    this.connectorEnable = connectorEnable;
  }

  @Parameters
  public static Collection<Object[]> propertyCombinations() {
    return Arrays.asList(new Object[][]{
        {true, true, true, true},
        {true, true, true, false},
        {true, true, false, true},
        {true, true, false, false},
        {true, false, true, true},
        {false, true, true, true},
        {false, false, true, false},
        {false, false, false, false}
    });
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId()
    );
    RequestContext.get().setVariableSecureApp(vars);
  }

  @Test
  public void testExecute_success_withProperties() throws Exception {
    Properties mockProperties = new Properties();
    mockProperties.setProperty("docker_com.etendoerp.etendorx", String.valueOf(rxEnable));
    mockProperties.setProperty("docker_com.etendoerp.tomcat", String.valueOf(tomcatEnable));
    mockProperties.setProperty("docker_com.etendoerp.etendorx_async", String.valueOf(asyncEnable));
    mockProperties.setProperty("docker_com.etendoerp.etendorx_connector", String.valueOf(connectorEnable));

    String propertyKey = String.format(
        "rxEnable:%b,tomcatEnable:%b,asyncEnable:%b,connectorEnable:%b",
        rxEnable, tomcatEnable, asyncEnable, connectorEnable
    );

    List<String> expectedUrls = ServiceURLConfig.SERVICE_URLS.get(propertyKey);
    assertNotNull("No expected configurations found for the key: " + propertyKey, expectedUrls);

    try (MockedStatic<OBPropertiesProvider> mockedStatic = mockStatic(OBPropertiesProvider.class)) {
      mockedStatic.when(OBPropertiesProvider::getInstance).thenReturn(obPropertiesProvider);
      when(obPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);

      JSONObject result = initializeRXServices.execute(Map.of(), "");
      assertNotNull(result);
      assertEquals("success", result.getString("severity"));

      List<ETRXConfig> configs = OBDal.getInstance().createCriteria(ETRXConfig.class).list();

      Map<String, String> createdServiceUrls = configs.stream()
          .collect(Collectors.toMap(ETRXConfig::getServiceName, ETRXConfig::getServiceURL));

      for (String expectedUrl : expectedUrls) {
        boolean found = createdServiceUrls.values().contains(expectedUrl);
        assertTrue("The expected URL was not found: " + expectedUrl, found);
      }
    }
    List<ETRXConfig> rxConfigsForTest = OBDal.getInstance().createCriteria(ETRXConfig.class).list();
    rxConfigsForTest.forEach(OBDal.getInstance()::remove);
    OBDal.getInstance().flush();
  }
}
