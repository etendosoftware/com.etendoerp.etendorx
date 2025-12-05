package com.etendoerp.etendorx.actionhandler;

import com.etendoerp.etendorx.data.ETRXConfig;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class InitializeRXServicesTest extends WeldBaseTest {

  private final boolean rxEnable;
  private final boolean tomcatEnable;
  private final boolean asyncEnable;
  private final boolean connectorEnable;
  @InjectMocks
  private InitializeRXServices initializeRXServices;
  @Mock
  private OBPropertiesProvider obPropertiesProvider;

  /**
   * Creates a new test instance for initializing RX services with the given configuration flags.
   *
   * @param rxEnable        whether the RX services should be enabled
   * @param tomcatEnable    whether Tomcat is running in a dockerized environment
   * @param asyncEnable     whether the async process service should be enabled
   * @param connectorEnable whether the connector service should be enabled
   */
  public InitializeRXServicesTest(boolean rxEnable, boolean tomcatEnable, boolean asyncEnable,
                                  boolean connectorEnable) {
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

    List<ETRXConfig> pre = OBDal.getInstance().createCriteria(ETRXConfig.class).list();
    pre.forEach(OBDal.getInstance()::remove);
    OBDal.getInstance().flush();
  }

  @Test
  public void testExecute_success_withProperties() throws Exception {
    Properties mockProperties = new Properties();
    mockProperties.setProperty("docker_com.etendoerp.etendorx", String.valueOf(rxEnable));
    mockProperties.setProperty("docker_com.etendoerp.tomcat", String.valueOf(tomcatEnable));
    mockProperties.setProperty("docker_com.etendoerp.etendorx_async", String.valueOf(asyncEnable));
    mockProperties.setProperty("docker_com.etendoerp.etendorx_connector", String.valueOf(connectorEnable));
    mockProperties.setProperty("docker.exclude", "");
    // Add source.path property for RXConfigUtils to find compose directory
    mockProperties.setProperty("source.path", System.getProperty("user.dir"));

    try (MockedStatic<OBPropertiesProvider> mockedStatic = mockStatic(OBPropertiesProvider.class)) {
      mockedStatic.when(OBPropertiesProvider::getInstance).thenReturn(obPropertiesProvider);
      when(obPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);

      JSONObject result = initializeRXServices.execute(Map.of(), "");
      assertNotNull(result);
      assertEquals("success", result.getString("severity"));

      List<ETRXConfig> configs = OBDal.getInstance().createCriteria(ETRXConfig.class).list();
      Map<String, String> createdByName = configs.stream()
          .collect(Collectors.toMap(ETRXConfig::getServiceName, ETRXConfig::getServiceURL));
      Map<String, Integer> expectedServices = new java.util.LinkedHashMap<>();
      if (rxEnable) {
        // Get core RX services (including asyncprocess, but excluding kafka/connect and connector services)
        Map<String, Integer> allServices = com.etendoerp.etendorx.utils.RXConfigUtils.getServicePorts();
        Map<String, Integer> connectorServices = com.etendoerp.etendorx.utils.RXConfigUtils.getConnectorServices();

        allServices.entrySet().stream()
            .filter(entry -> !connectorServices.containsKey(entry.getKey()))
            .filter(entry -> !entry.getKey().equals("kafka") &&
                !entry.getKey().equals("connect"))
            .forEach(entry -> expectedServices.put(entry.getKey(), entry.getValue()));
      }
      if (asyncEnable) {
        // Get async services (kafka and connect only)
        // Note: asyncprocess is now in ETENDORX file, so it's ONLY included when rxEnable=true
        Map<String, Integer> asyncServices = com.etendoerp.etendorx.utils.RXConfigUtils.getServicesByType(
            com.etendoerp.etendorx.utils.RXConfigUtils.ServiceConfigType.ASYNC);
        expectedServices.putAll(asyncServices);
      }
      if (connectorEnable) {
        expectedServices.putAll(com.etendoerp.etendorx.utils.RXConfigUtils.getConnectorServices());
      }

      for (Map.Entry<String, Integer> e : expectedServices.entrySet()) {
        String expectedUrl = com.etendoerp.etendorx.utils.RXConfigUtils.buildServiceUrl(
            e.getKey(), rxEnable, tomcatEnable, asyncEnable, connectorEnable);

        assertTrue("Falta servicio: " + e.getKey(), createdByName.containsKey(e.getKey()));
        assertEquals("URL distinta para " + e.getKey(), expectedUrl, createdByName.get(e.getKey()));
      }
    }
    List<ETRXConfig> rxConfigsForTest = OBDal.getInstance().createCriteria(ETRXConfig.class).list();
    rxConfigsForTest.forEach(OBDal.getInstance()::remove);
    OBDal.getInstance().flush();
  }
}
