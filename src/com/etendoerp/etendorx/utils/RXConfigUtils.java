package com.etendoerp.etendorx.utils;

import com.etendoerp.etendorx.data.ETRXConfig;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for RX Configuration related operations.
 */
public class RXConfigUtils {
  public static final String WORKER = "worker";
  public static final String OBCONNSRV = "obconnsrv";
  public static final String COMPOSE_DIR = "/modules/com.etendoerp.etendorx/compose";
  private static final Logger log = LoggerFactory.getLogger(RXConfigUtils.class);
  private static final String LOCALHOST_URL = "http://localhost:%d";
  private static final String ASYNCPROCESS = "asyncprocess";

  // Simple cache to avoid reparsing YAML files multiple times in the same session
  private static final Map<ServiceConfigType, Map<String, Integer>> servicePortsCache = new EnumMap<>(ServiceConfigType.class);

  /**
   * Private constructor to prevent instantiation of this utility class.
   * Throws an IllegalStateException if called.
   */
  private RXConfigUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Gets service configurations for a specific type (etendorx, async, connector, or all).
   * This is the main generic method that replaces the previous specific methods.
   * Services are always read from YAML files - no defaults are used.
   * Results are cached to avoid reparsing YAML files multiple times.
   *
   * @param configType The type of configuration to retrieve
   * @return Map containing service names and their corresponding ports
   * @throws OBException if compose directory or YAML files are not found
   */
  public static Map<String, Integer> getServicesByType(ServiceConfigType configType) {
    // Check cache first
    if (servicePortsCache.containsKey(configType)) {
      log.debug("Returning cached services for type {}", configType);
      return servicePortsCache.get(configType);
    }

    Map<String, Integer> services = new HashMap<>();

    try {
      Path composePath = getComposePath();
      if (composePath == null || !composePath.toFile().exists()) {
        throw new OBException("Compose directory not found. YAML files are required.");
      }

      if (configType == ServiceConfigType.ALL) {
        // Parse all YAML files for ALL type
        File composeDir = composePath.toFile();
        File[] ymlFiles = composeDir.listFiles((dir, name) -> name.endsWith(".yml"));

        if (ymlFiles != null) {
          for (File ymlFile : ymlFiles) {
            log.debug("Parsing YAML file for ALL type: {}", ymlFile.getName());
            parseYamlFile(ymlFile, services);
          }
        }
      } else {
        // Parse specific YAML file for the given type
        File yamlFile = new File(composePath.toFile(), configType.getYamlFile());
        if (!yamlFile.exists()) {
          throw new OBException("YAML file not found for type " + configType + ": " + configType.getYamlFile());
        }

        log.debug("Parsing YAML file for type {}: {}", configType, yamlFile.getName());
        parseYamlFile(yamlFile, services);
      }

      // Cache the result
      servicePortsCache.put(configType, services);

      log.info("Parsed {} services for type {}: {}", services.size(), configType, services);

      if (services.isEmpty()) {
        throw new OBException("No services found in YAML for type: " + configType);
      }

    } catch (OBException e) {
      throw new OBException("Failed to parse services from YAML for type: " + configType, e);
    }

    return services;
  }

  /**
   * Clears the cached mapping of service ports.
   * <p>
   * This method removes all entries from the {@code servicePortsCache},
   * which stores the association between {@link ServiceConfigType} and
   * their resolved service ports.
   * </p>
   * <p>
   * It should be called when the service configurations or their port
   * definitions may have changed, ensuring that future lookups are
   * re-parsed from the source YAML files instead of relying on outdated data.
   * </p>
   *
   * <p><b>Side effects:</b></p>
   * <ul>
   *   <li>The entire cache is cleared (no partial reset is possible).</li>
   *   <li>A log entry at INFO level is generated indicating the cache reset.</li>
   * </ul>
   */
  public static void resetCache() {
    servicePortsCache.clear();
    log.info("Service ports cache cleared");
  }

  /**
   * Gets the path to the compose directory.
   * Uses the fixed path to the compose directory within the module.
   * Falls back to current working directory for tests when source.path is null.
   *
   * @return Path to compose directory
   */
  private static Path getComposePath() {
    String sourcePath = OBPropertiesProvider.getInstance().getOpenbravoProperties().get("source.path").toString();
    String moduleComposePath;

    if (sourcePath != null) {
      // Production/normal case - use source.path from properties
      moduleComposePath = sourcePath + COMPOSE_DIR;
    } else {
      // Test case - source.path is null, use current working directory
      moduleComposePath = System.getProperty("user.dir") + COMPOSE_DIR;
      log.debug("source.path is null (likely in test), using current working directory");
    }

    Path composePath = Paths.get(moduleComposePath);

    log.debug("Using compose path: {}", composePath.toAbsolutePath());

    if (composePath.toFile().exists()) {
      log.debug("Compose directory found at: {}", composePath.toAbsolutePath());
      return composePath;
    } else {
      log.warn("Compose directory does not exist at path: {}", composePath.toAbsolutePath());
      return null;
    }
  }

  /**
   * Parses a YAML file to extract service port mappings.
   * This is a simple YAML parser that extracts ports without external dependencies.
   * Handles Docker Compose format with services: section.
   *
   * @param yamlFile The YAML file to parse
   * @param portsMap The map to populate with service ports
   */
  private static void parseYamlFile(File yamlFile, Map<String, Integer> portsMap) {
    log.debug("Starting to parse YAML file: {}", yamlFile.getName());

    try (FileInputStream fis = new FileInputStream(yamlFile)) {
      String content = new String(fis.readAllBytes());
      String[] lines = content.split("\n");

      YamlParsingContext context = new YamlParsingContext(yamlFile.getName());

      for (String line : lines) {
        processYamlLine(line, context, portsMap);
      }

      log.debug("Completed parsing {}: found {} services", yamlFile.getName(), context.getServicesFoundInFile());

    } catch (IOException e) {
      log.error("Error reading YAML file {}", yamlFile.getName(), e);
    }
  }

  /**
   * Processes a single line of YAML content.
   *
   * @param line     The line to process
   * @param context  The parsing context
   * @param portsMap The map to populate with service ports
   */
  private static void processYamlLine(String line, YamlParsingContext context, Map<String, Integer> portsMap) {
    String trimmedLine = line.trim();

    if (isServicesSection(trimmedLine)) {
      context.enterServicesSection();
      return;
    }

    if (!context.isInServicesSection()) {
      return;
    }

    if (tryProcessServiceDefinition(line, trimmedLine, context)) {
      return;
    }

    if (tryProcessPortsSection(trimmedLine, context)) {
      return;
    }

    if (context.isInPortsSection()) {
      processPortMapping(line, trimmedLine, context, portsMap);
    }
  }

  /**
   * Checks if the line indicates the start of services section.
   */
  private static boolean isServicesSection(String trimmedLine) {
    return trimmedLine.equals("services:");
  }

  /**
   * Tries to process a service definition line.
   *
   * @return true if the line was processed as a service definition
   */
  private static boolean tryProcessServiceDefinition(String line, String trimmedLine, YamlParsingContext context) {
    if (trimmedLine.startsWith("#")) {
      return false;
    }

    Pattern servicePattern = Pattern.compile("^\\s*(\\w+):\\s*$");
    Matcher serviceMatcher = servicePattern.matcher(line);

    if (serviceMatcher.matches() && isServiceIndentation(line)) {
      String serviceName = serviceMatcher.group(1);
      context.setCurrentService(serviceName);
      return true;
    }

    return false;
  }

  /**
   * Checks if the line has correct indentation for a service definition.
   */
  private static boolean isServiceIndentation(String line) {
    return line.startsWith("  ") && !line.startsWith("    ");
  }

  /**
   * Tries to process a ports section line.
   *
   * @return true if the line was processed as a ports section
   */
  private static boolean tryProcessPortsSection(String trimmedLine, YamlParsingContext context) {
    if (context.getCurrentService() != null && trimmedLine.equals("ports:")) {
      context.enterPortsSection();
      return true;
    }
    return false;
  }

  /**
   * Processes a port mapping line.
   */
  private static void processPortMapping(String line, String trimmedLine, YamlParsingContext context, Map<String, Integer> portsMap) {
    Pattern portPattern = Pattern.compile("^\\s*-\\s*\"?(\\d+):(\\d+)\"?\\s*$");
    Matcher portMatcher = portPattern.matcher(line);

    if (portMatcher.matches()) {
      extractAndStorePort(portMatcher, context, portsMap);
    } else if (isEndOfPortsSection(trimmedLine, line)) {
      context.exitPortsSection();
    }
  }

  /**
   * Extracts port number and stores it in the ports map.
   */
  private static void extractAndStorePort(Matcher portMatcher, YamlParsingContext context, Map<String, Integer> portsMap) {
    String externalPort = portMatcher.group(1);

    try {
      int port = Integer.parseInt(externalPort);
      portsMap.put(context.getCurrentService(), port);
      context.incrementServicesFound();

      log.debug("Found service '{}' with port {} in file {}",
          context.getCurrentService(), port, context.getFileName());

      // Take the first port mapping for each service
      context.completeCurrentService();

    } catch (NumberFormatException e) {
      log.warn("Invalid port number in {}: {}", context.getFileName(), portMatcher.group(0));
    }
  }

  /**
   * Checks if the current line indicates the end of ports section.
   */
  private static boolean isEndOfPortsSection(String trimmedLine, String line) {
    return !trimmedLine.isEmpty() &&
        !trimmedLine.startsWith("-") &&
        !line.startsWith("      ");
  }

  /**
   * Gets the port for a specific service from the parsed YAML configuration.
   *
   * @param serviceName Name of the service
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
   * Uses dynamic port loading from YAML files only - no fallback to hardcoded ports.
   *
   * @param name            a String containing the service name
   * @param rxEnable        a boolean indicating if RX is enabled
   * @param tomcatEnable    a boolean indicating if Tomcat is enabled
   * @param asyncEnable     a boolean indicating if Async is enabled
   * @param connectorEnable a boolean indicating if Connector is enabled
   * @return a String containing the service URL
   * @throws OBException if service port cannot be found in YAML configuration
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

  /**
   * This method is used to check if the service Async or Connector are enabled.
   *
   * @param name            a String containing the service name
   * @param asyncEnable     a boolean indicating if Async is enabled
   * @param connectorEnable a boolean indicating if Connector is enabled
   * @return a boolean indicating if the service is Async or Connector enabled
   */
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
      debug.append("Class location: ").append(RXConfigUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).append("\n");
    } catch (Exception e) {
      debug.append("Class location: ERROR - ").append(e.getMessage()).append("\n");
    }

    // Show compose path
    Path composePath = null;
    try {
      composePath = getComposePath();
      debug.append("Fixed compose path: ").append(composePath != null ? composePath.toString() : "NOT FOUND").append("\n");
    } catch (Exception e) {
      debug.append("Compose path: ERROR - ").append(e.getMessage()).append("\n");
    }

    if (composePath != null && composePath.toFile().exists()) {
      File[] ymlFiles = composePath.toFile().listFiles((dir, name) -> name.endsWith(".yml"));
      debug.append("YAML files found: ");
      if (ymlFiles != null) {
        for (File file : ymlFiles) {
          debug.append(file.getName()).append(" ");
        }
      } else {
        debug.append("none");
      }
      debug.append("\n");
    } else {
      debug.append("Compose directory not accessible\n");
    }

    return debug.toString();
  }

  /**
   * Enum representing different types of service configurations.
   */
  public enum ServiceConfigType {
    ETENDORX("com.etendoerp.etendorx.yml", new String[]{"config", "auth", "das", "edge", ASYNCPROCESS}),
    ASYNC("com.etendoerp.etendorx_async.yml", new String[]{"kafka", "connect"}),
    CONNECTOR("com.etendoerp.etendorx_connector.yml", new String[]{OBCONNSRV, WORKER}),
    ALL("", new String[]{}); // Special type to get all services

    private final String yamlFile;
    private final String[] expectedServices;

    /**
     * Creates a new service configuration type.
     *
     * @param yamlFile         the name of the YAML file associated with this configuration type
     * @param expectedServices the list of expected service names defined in the YAML file
     */
    ServiceConfigType(String yamlFile, String[] expectedServices) {
      this.yamlFile = yamlFile;
      this.expectedServices = expectedServices;
    }

    /**
     * Gets the YAML file name associated with this configuration type.
     *
     * @return the YAML file name
     */
    public String getYamlFile() {
      return yamlFile;
    }

    /**
     * Gets the expected service names associated with this configuration type.
     *
     * @return an array of expected service names
     */
    public String[] getExpectedServices() {
      return expectedServices;
    }
  }

  /**
   * Helper class to maintain parsing context and reduce method parameters.
   */
  private static class YamlParsingContext {
    private final String fileName;
    private String currentService;
    private boolean inServicesSection;
    private boolean inPortsSection;
    private int servicesFoundInFile;

    /**
     * Creates a new parsing context for a given YAML file.
     *
     * @param fileName the name of the YAML file being parsed
     */
    public YamlParsingContext(String fileName) {
      this.fileName = fileName;
      this.inServicesSection = false;
      this.inPortsSection = false;
      this.servicesFoundInFile = 0;
    }

    /**
     * Marks the beginning of the "services" section in the YAML file.
     */
    public void enterServicesSection() {
      this.inServicesSection = true;
      log.debug("Entering services section in file: {}", fileName);
    }

    /**
     * Marks the beginning of the "ports" section for the current service.
     */
    public void enterPortsSection() {
      this.inPortsSection = true;
      log.debug("Entering ports section for service: {}", currentService);
    }

    /**
     * Marks the end of the "ports" section for the current service.
     */
    public void exitPortsSection() {
      this.inPortsSection = false;
    }

    /**
     * Completes the parsing of the current service definition.
     * Resets the current service and exits the ports section.
     */
    public void completeCurrentService() {
      this.inPortsSection = false;
      this.currentService = null;
    }

    /**
     * Increments the counter of services found in the YAML file.
     */
    public void incrementServicesFound() {
      this.servicesFoundInFile++;
    }

    /**
     * Gets the name of the YAML file being parsed.
     *
     * @return the file name
     */
    public String getFileName() {
      return fileName;
    }

    /**
     * Gets the name of the current service being parsed.
     *
     * @return the current service name, or {@code null} if none is set
     */
    public String getCurrentService() {
      return currentService;
    }

    /**
     * Sets the current service being parsed.
     *
     * @param serviceName the name of the service
     */
    public void setCurrentService(String serviceName) {
      this.currentService = serviceName;
      this.inPortsSection = false;
      log.debug("Found service definition: {}", serviceName);
    }

    /**
     * Checks if the parser is currently inside the "services" section.
     *
     * @return {@code true} if inside the services section, {@code false} otherwise
     */
    public boolean isInServicesSection() {
      return inServicesSection;
    }

    /**
     * Checks if the parser is currently inside the "ports" section.
     *
     * @return {@code true} if inside the ports section, {@code false} otherwise
     */
    public boolean isInPortsSection() {
      return inPortsSection;
    }

    /**
     * Gets the number of services found in the YAML file so far.
     *
     * @return the count of services found
     */
    public int getServicesFoundInFile() {
      return servicesFoundInFile;
    }
  }
}
