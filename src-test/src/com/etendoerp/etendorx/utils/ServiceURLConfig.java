package com.etendoerp.etendorx.utils;

import java.util.List;
import java.util.Map;

/**
 * Utility class for testing the generation of services URLs.
 */
public class ServiceURLConfig {

  public static final Map<String, List<String>> SERVICE_URLS = Map.of(
      // rxEnable = true, tomcatEnable = true, asyncEnable = true, connectorEnable = true
      "rxEnable:true,tomcatEnable:true,asyncEnable:true,connectorEnable:true", List.of(
          "http://config:8888",
          "http://auth:8094",
          "http://das:8092",
          "http://edge:8096",
          "http://asyncprocess:9092",
          "http://obconnsrv:8101",
          "http://worker:0"
      ),

      // rxEnable = false, tomcatEnable = false, asyncEnable = false, connectorEnable = false
      "rxEnable:false,tomcatEnable:false,asyncEnable:false,connectorEnable:false", List.of(
          "http://localhost:8888",
          "http://localhost:8094",
          "http://localhost:8092",
          "http://localhost:8096",
          "http://localhost:9092",
          "http://localhost:8101",
          "http://localhost:0"
      ),

      // rxEnable = false, tomcatEnable = true, asyncEnable = false, connectorEnable = false
      "rxEnable:false,tomcatEnable:true,asyncEnable:false,connectorEnable:false", List.of(
          "http://host.docker.internal:8888",
          "http://host.docker.internal:8094",
          "http://host.docker.internal:8092",
          "http://host.docker.internal:8096",
          "http://host.docker.internal:9092",
          "http://host.docker.internal:8101",
          "http://host.docker.internal:0"
      ),

      // rxEnable = true, tomcatEnable = false, asyncEnable = false, connectorEnable = false
      "rxEnable:true,tomcatEnable:false,asyncEnable:false,connectorEnable:false", List.of(
          "http://localhost:8888",
          "http://localhost:8094",
          "http://localhost:8092",
          "http://localhost:8096",
          "http://localhost:9092",
          "http://localhost:8101",
          "http://localhost:0"
      ),

      // rxEnable = true, tomcatEnable = true, asyncEnable = false, connectorEnable = false
      "rxEnable:true,tomcatEnable:true,asyncEnable:false,connectorEnable:false", List.of(
          "http://config:8888",
          "http://auth:8094",
          "http://das:8092",
          "http://edge:8096",
          "http://host.docker.internal:9092",
          "http://host.docker.internal:8101",
          "http://host.docker.internal:0"
      ),

      // rxEnable = true, tomcatEnable = true, asyncEnable = true, connectorEnable = false
      "rxEnable:true,tomcatEnable:true,asyncEnable:true,connectorEnable:false", List.of(
          "http://config:8888",
          "http://auth:8094",
          "http://das:8092",
          "http://edge:8096",
          "http://asyncprocess:9092",
          "http://host.docker.internal:8101",
          "http://host.docker.internal:0"
      ),

      // rxEnable = true, tomcatEnable = true, asyncEnable = false, connectorEnable = true
      "rxEnable:true,tomcatEnable:true,asyncEnable:false,connectorEnable:true", List.of(
          "http://config:8888",
          "http://auth:8094",
          "http://das:8092",
          "http://edge:8096",
          "http://host.docker.internal:9092",
          "http://obconnsrv:8101",
          "http://worker:0"
      ),

      "rxEnable:true,tomcatEnable:false,asyncEnable:true,connectorEnable:true", List.of(
          "http://localhost:8888",
          "http://localhost:8094",
          "http://localhost:8092",
          "http://localhost:8096",
          "http://localhost:9092",
          "http://localhost:8101",
          "http://localhost:0"
      ),

      "rxEnable:false,tomcatEnable:false,asyncEnable:true,connectorEnable:false", List.of(
          "http://localhost:8888",
          "http://localhost:8094",
          "http://localhost:8092",
          "http://localhost:8096",
          "http://localhost:9092",
          "http://localhost:8101",
          "http://localhost:0"
      ),

      // rxEnable = false, tomcatEnable = true, asyncEnable = true, connectorEnable = true
      "rxEnable:false,tomcatEnable:true,asyncEnable:true,connectorEnable:true", List.of(
          "http://host.docker.internal:8888",
          "http://host.docker.internal:8094",
          "http://host.docker.internal:8092",
          "http://host.docker.internal:8096",
          "http://asyncprocess:9092",
          "http://obconnsrv:8101",
          "http://worker:0"
      )
  );
}
