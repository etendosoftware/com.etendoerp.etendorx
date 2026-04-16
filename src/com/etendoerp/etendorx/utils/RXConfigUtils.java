package com.etendoerp.etendorx.utils;

/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance
 * with the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openbravo.dal.service.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etendoerp.etendorx.data.ETRXConfig;

/**
 * Utility class for RX Configuration related operations.
 */
public class RXConfigUtils {
  public static final String COMPOSE_DIR = "/build/compose";
  private static final String MODULE_BUILD_COMPOSE_DIR = "/build/etendo/modules/com.etendoerp.etendorx/compose";
  private static final String MODULE_SOURCE_COMPOSE_DIR = "/modules/com.etendoerp.etendorx/compose";
  private static final Logger log = LoggerFactory.getLogger(RXConfigUtils.class);
  private static final String LOCALHOST_URL = "http://localhost:%d";
  private static final String ASYNCPROCESS = "asyncprocess";

  private static final Pattern SERVICE_PATTERN = Pattern.compile("^\\s*(\\w+):\\s*$");
  private static final Pattern PORT_PATTERN = Pattern.compile("^\\s*-\\s*\"?(\\d+):(\\d+)\"?\\s*$");

  // Simple cache to avoid reparsing YAML files multiple times in the same session
  private static final Map<ServiceConfigType, Map<String, Integer>> servicePortsCache = new EnumMap<>(
      ServiceConfigType.class);

  private RXConfigUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Gets service configurations for a specific type (etendorx, async, connector, or all).
   * This is the main generic method that replaces the previous specific methods.
   * Services are always read from YAML files - no defaults are used.
   * Results are cached to avoid reparsing YAML files multiple times.
   *
   * @param configType
   *     The type of configuration to retrieve
   * @return Map containing service names and their corresponding ports
   * @throws OBException
   *     if compose directory or YAML files are not found
   */
  public static Map<String, Integer> getServicesByType(ServiceConfigType configType) {
    if (servicePortsCache.containsKey(configType)) {
      log.debug("Returning cached services for type {}", configType);
      return servicePortsCache.get(configType);
    }

    Map<String, Integer> services = new HashMap<>();

    if (configType == ServiceConfigType.ALL) {
      parseAllYamlFiles(services);
    } else {
      File yamlFile = findYamlFile(configType.getYamlFile());
      if (yamlFile == null) {
        throw new OBException("YAML file not found for type " + configType + ": " + configType.getYamlFile());
      }
      log.debug("Parsing YAML file for type {}: {}", configType, yamlFile.getAbsolutePath());
      parseYamlFile(yamlFile, services);
    }

    if (services.isEmpty()) {
      throw new OBException("No services found in YAML for type: " + configType);
    }

    servicePortsCache.put(configType, services);
    log.debug("Parsed {} services for type {}: {}", services.size(), configType, services);

    return services;
  }

  /**
   * Clears the cached mapping of service ports.
   */
  public static void resetCache() {
    servicePortsCache.clear();
    log.debug("Service ports cache cleared");
  }

  /**
   * Gets the base source path from properties or current working directory.
   *
   * @return the base path string
   */
  private static String getBasePath() {
    Object sourcePathObj = OBPropertiesProvider.getInstance().getOpenbravoProperties().get("source.path");
    if (sourcePathObj != null) {
      return sourcePathObj.toString();
    }
    log.debug("source.path is null (likely in test), using current working directory");
    return System.getProperty("user.dir");
  }

  /**
   * Finds a YAML file by searching in multiple directories in order:
   * 1. build/compose/
   * 2. build/etendo/modules/com.etendoerp.etendorx/compose/
   * 3. modules/com.etendoerp.etendorx/compose/
   *
   * @param yamlFileName
   *     the YAML file name to search for
   * @return the File if found, or null if not found in any location
   */
  private static File findYamlFile(String yamlFileName) {
    String basePath = getBasePath();
    String[] searchDirs = {
        basePath + COMPOSE_DIR,
        basePath + MODULE_BUILD_COMPOSE_DIR,
        basePath + MODULE_SOURCE_COMPOSE_DIR
    };

    for (String dir : searchDirs) {
      File candidate = new File(dir, yamlFileName);
      if (candidate.exists()) {
        log.debug("Found YAML file {} at {}", yamlFileName, candidate.getAbsolutePath());
        return candidate;
      }
      log.debug("YAML file {} not found at {}", yamlFileName, candidate.getAbsolutePath());
    }

    log.warn("YAML file {} not found in any search directory", yamlFileName);
    return null;
  }

  /**
   * Parses all YAML files found across the search directories.
   * Scans build/compose/, build/etendo/modules/.../compose/, and modules/.../compose/
   * to collect all services. Files already parsed (by name) are not parsed again.
   *
   * @param services
   *     map to populate with service names and ports
   */
  private static void parseAllYamlFiles(Map<String, Integer> services) {
    String basePath = getBasePath();
    String[] searchDirs = {
        basePath + COMPOSE_DIR,
        basePath + MODULE_BUILD_COMPOSE_DIR,
        basePath + MODULE_SOURCE_COMPOSE_DIR
    };

    java.util.Set<String> parsedFileNames = new java.util.HashSet<>();

    for (String dir : searchDirs) {
      File dirFile = new File(dir);
      if (!dirFile.exists()) {
        log.debug("Directory not found for ALL scan: {}", dir);
      } else {
        parseYamlFilesInDir(dirFile, dir, parsedFileNames, services);
      }
    }
  }

  private static void parseYamlFilesInDir(File dirFile, String dir,
      java.util.Set<String> parsedFileNames, Map<String, Integer> services) {
    File[] ymlFiles = dirFile.listFiles((d, name) -> name.endsWith(".yml"));
    if (ymlFiles == null) {
      return;
    }
    for (File ymlFile : ymlFiles) {
      if (parsedFileNames.add(ymlFile.getName())) {
        log.debug("Parsing YAML file for ALL type: {} from {}", ymlFile.getName(), dir);
        parseYamlFile(ymlFile, services);
      }
    }
  }

  /**
   * Holds mutable state used while walking the lines of a Docker Compose YAML file.
   */
  private static class YamlParserState {
    boolean inServicesSection;
    boolean inPortsSection;
    String currentService;
    int servicesFound;
  }

  private static void parseYamlFile(File yamlFile, Map<String, Integer> portsMap) {
    log.debug("Starting to parse YAML file: {}", yamlFile.getName());

    String content;
    try {
      content = Files.readString(yamlFile.toPath());
    } catch (IOException e) {
      log.error("Error reading YAML file {}", yamlFile.getName(), e);
      return;
    }

    YamlParserState state = new YamlParserState();
    for (String line : content.split("\n")) {
      processYamlLine(line, state, portsMap, yamlFile.getName());
    }

    log.debug("Completed parsing {}: found {} services", yamlFile.getName(), state.servicesFound);
  }

  private static void processYamlLine(String line, YamlParserState state,
      Map<String, Integer> portsMap, String fileName) {
    String trimmed = line.trim();

    if (trimmed.equals("services:")) {
      state.inServicesSection = true;
      return;
    }

    if (!state.inServicesSection) {
      return;
    }

    if (tryParseServiceName(line, trimmed, state)) {
      return;
    }

    if (state.currentService != null && trimmed.equals("ports:")) {
      state.inPortsSection = true;
      return;
    }

    if (state.inPortsSection) {
      processPortLine(line, trimmed, state, portsMap, fileName);
    }
  }

  private static boolean tryParseServiceName(String line, String trimmed, YamlParserState state) {
    if (trimmed.startsWith("#") || !line.startsWith("  ") || line.startsWith("    ")) {
      return false;
    }
    Matcher serviceMatcher = SERVICE_PATTERN.matcher(line);
    if (!serviceMatcher.matches()) {
      return false;
    }
    state.currentService = serviceMatcher.group(1);
    state.inPortsSection = false;
    return true;
  }

  private static void processPortLine(String line, String trimmed, YamlParserState state,
      Map<String, Integer> portsMap, String fileName) {
    Matcher portMatcher = PORT_PATTERN.matcher(line);
    if (portMatcher.matches()) {
      try {
        int port = Integer.parseInt(portMatcher.group(1));
        portsMap.put(state.currentService, port);
        state.servicesFound++;
        log.debug("Found service '{}' with port {} in file {}", state.currentService, port, fileName);
      } catch (NumberFormatException e) {
        log.warn("Invalid port number in {}: {}", fileName, portMatcher.group(0));
      }
      // Take the first port mapping for each service, then move on
      state.inPortsSection = false;
      state.currentService = null;
    } else if (!trimmed.isEmpty() && !trimmed.startsWith("-") && !line.startsWith("      ")) {
      state.inPortsSection = false;
    }
  }

  /**
   * Gets the port for a specific service from the parsed YAML configuration.
   *
   * @param serviceName
   *     Name of the service
   * @return Port number for the service, or 0 if not found
   */
  public static int getServicePort(String serviceName) {
    Map<String, Integer> allPorts = getServicesByType(ServiceConfigType.ALL);
    return allPorts.getOrDefault(serviceName, 0);
  }

  /**
   * Gets all connector services and their ports.
   *
   * @return Map of connector services and their ports
   */
  public static Map<String, Integer> getConnectorServices() {
    return getServicesByType(ServiceConfigType.CONNECTOR);
  }

  /**
   * Gets all service ports.
   *
   * @return Map of all services and their ports
   */
  public static Map<String, Integer> getServicePorts() {
    return getServicesByType(ServiceConfigType.ALL);
  }

  /**
   * This method is used to get the RX Configuration.
   * It first gets the ETRXConfig instance by the service name.
   *
   * @param serviceName
   *     a String containing the service name
   * @return an ETRXConfig instance
   */
  public static ETRXConfig getRXConfig(String serviceName) {
    try {
      return (ETRXConfig) OBDal.getInstance()
          .createCriteria(ETRXConfig.class)
          .add(Restrictions.eq(ETRXConfig.PROPERTY_SERVICENAME, serviceName))
          .setMaxResults(1)
          .uniqueResult();
    } catch (RuntimeException e) {
      throw new OBException("Error retrieving ETRXConfig for service: " + serviceName, e);
    }
  }

  /**
   * This method is used to build the service URL.
   * It first checks if the service is enabled and then builds the URL accordingly.
   * Uses dynamic port loading from YAML files only - no fallback to hardcoded ports.
   *
   * @param name
   *     a String containing the service name
   * @param rxEnable
   *     a boolean indicating if RX is enabled
   * @param tomcatEnable
   *     a boolean indicating if Tomcat is enabled
   * @param asyncEnable
   *     a boolean indicating if Async is enabled
   * @param connectorEnable
   *     a boolean indicating if Connector is enabled
   * @return a String containing the service URL
   * @throws OBException
   *     if service port cannot be found in YAML configuration
   */
  public static String buildServiceUrl(String name, boolean rxEnable, boolean tomcatEnable,
      boolean asyncEnable, boolean connectorEnable) {

    // Get actual port from YAML configuration - no fallback
    int actualPort = getServicePort(name);
    if (actualPort == 0) {
      throw new OBException("Service port not found in YAML configuration for service: " + name);
    }

    if (tomcatEnable) {
      if (isAsyncOrConnectorEnabled(name, asyncEnable, connectorEnable)
          || (rxEnable && !name.equals(ASYNCPROCESS) && !getConnectorServices().containsKey(name))) {
        return String.format("http://%s:%d", name, actualPort);
      }
      return String.format("http://host.docker.internal:%d", actualPort);
    }
    return String.format(LOCALHOST_URL, actualPort);
  }

  private static boolean isAsyncOrConnectorEnabled(String name, boolean asyncEnable, boolean connectorEnable) {
    return (name.equals(ASYNCPROCESS) && asyncEnable) ||
        (getConnectorServices().containsKey(name) && connectorEnable);
  }

  /**
   * Debug method to show compose path and YAML files found.
   * This method is useful for troubleshooting YAML parsing.
   *
   * @return String with debug information
   */
  public static String getDebugInfo() {
    StringBuilder debug = new StringBuilder();
    debug.append("=== RXConfigUtils Debug Info ===\n");

    // Show environment information
    debug.append("Current working directory: ").append(System.getProperty("user.dir")).append("\n");
    try {
      debug.append("Class location: ").append(
          RXConfigUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).append("\n");
    } catch (Exception e) {
      debug.append("Class location: ERROR - ").append(e.getMessage()).append("\n");
    }

    // Show all search directories and their YAML files
    String basePath = getBasePath();
    String[] searchDirs = {
        basePath + COMPOSE_DIR,
        basePath + MODULE_BUILD_COMPOSE_DIR,
        basePath + MODULE_SOURCE_COMPOSE_DIR
    };

    for (String dir : searchDirs) {
      File dirFile = new File(dir);
      debug.append("Search dir: ").append(dir).append(dirFile.exists() ? " [EXISTS]" : " [NOT FOUND]").append("\n");
      if (dirFile.exists()) {
        File[] ymlFiles = dirFile.listFiles((d, name) -> name.endsWith(".yml"));
        if (ymlFiles != null && ymlFiles.length > 0) {
          debug.append("  YAML files: ");
          for (File file : ymlFiles) {
            debug.append(file.getName()).append(" ");
          }
          debug.append("\n");
        }
      }
    }

    return debug.toString();
  }

  /**
   * Enum representing different types of service configurations.
   */
  public enum ServiceConfigType {
    ETENDORX("com.etendoerp.etendorx.yml"),
    ASYNC("com.etendoerp.etendorx_async.yml"),
    CONNECTOR("com.etendoerp.etendorx_connector.yml"),
    ALL("");

    private final String yamlFile;

    ServiceConfigType(String yamlFile) {
      this.yamlFile = yamlFile;
    }

    public String getYamlFile() {
      return yamlFile;
    }
  }
}
