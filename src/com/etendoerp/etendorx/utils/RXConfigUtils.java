package com.etendoerp.etendorx.utils;

import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.ETRXConfig;

/**
 * Utility class for RX Configuration related operations.
 */
public class RXConfigUtils {
  private static final Logger log = LoggerFactory.getLogger(RXConfigUtils.class);
  private static final String LOCALHOST_URL = "http://localhost:%d";
  private static final String ASYNCPROCESS = "asyncprocess";
  public static final Map<String, Integer> CONNECTOR_SERVICES = Map.of(
      "worker", 0,
      "obconnsrv", 8101
  );
  public static final Map<String, Integer> SERVICE_PORTS = Map.of(
      "config", 8888,
      "auth", 8094,
      "das", 8092,
      "edge", 8096
  );

  /**
   * Private constructor to prevent instantiation of this utility class.
   * Throws an IllegalStateException if called.
   */
  private RXConfigUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * This method is used to get the RX Configuration.
   * It first gets the ETRXConfig instance by the service name.
   *
   * @param serviceName a String containing the service name
   * @return an ETRXConfig instance
   */
  public static ETRXConfig getRXConfig(String serviceName) {
    return (ETRXConfig) OBDal.getInstance().createCriteria(ETRXConfig.class)
        .add(Restrictions.eq(ETRXConfig.PROPERTY_SERVICENAME, serviceName))
        .setMaxResults(1)
        .uniqueResult();
  }

  /**
   * This method is used to build the service URL.
   * It first checks if the service is enabled and then builds the URL accordingly.
   *
   * @param name a String containing the service name
   * @param port an int containing the service port
   * @param rxEnable a boolean indicating if RX is enabled
   * @param tomcatEnable a boolean indicating if Tomcat is enabled
   * @param asyncEnable a boolean indicating if Async is enabled
   * @param connectorEnable a boolean indicating if Connector is enabled
   * @return a String containing the service URL
   */
  public static String buildServiceUrl(String name, int port, boolean rxEnable, boolean tomcatEnable,
      boolean asyncEnable, boolean connectorEnable) {
    if (tomcatEnable) {
      if (isAsyncOrConnectorEnabled(name, asyncEnable, connectorEnable)
          || (rxEnable && !name.equals(ASYNCPROCESS) && !CONNECTOR_SERVICES.containsKey(name))) {
        return String.format("http://%s:%d", name, port);
      }
      return String.format("http://host.docker.internal:%d", port);
    }
    return String.format(LOCALHOST_URL, port);
  }

  /**
   * This method is used to check if the service Async or Connector are enabled.
   *
   * @param name a String containing the service name
   * @param asyncEnable a boolean indicating if Async is enabled
   * @param connectorEnable a boolean indicating if Connector is enabled
   * @return a boolean indicating if the service is Async or Connector enabled
   */
  private static boolean isAsyncOrConnectorEnabled(String name, boolean asyncEnable, boolean connectorEnable) {
    return (name.equals(ASYNCPROCESS) && asyncEnable) ||
        (CONNECTOR_SERVICES.containsKey(name) && connectorEnable);
  }
}
