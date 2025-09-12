#!/bin/sh
# Copyright Etendo Software.

# This script runs in the final 'runtime' stage. Its only
# responsibility is to start the application with the correct configuration.

# Exit on any error
set -e

# Defaults
: "${ENABLE_OPEN_TELEMETRY:=false}"
: "${DISABLE_DEBUG:=false}"
: "${JAVA_OPTS:=}"
: "${DEBUG_MODE:=false}"

ENTITY_JAR="/app/modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar"

echo "🚀 [RUNTIME STAGE] Starting application..."
echo "--------------------------------------------------------"

# Check if entity jar exists
if [ ! -f "$ENTITY_JAR" ]; then
  echo "⚠️ Entity jar not found: $ENTITY_JAR"
  echo "➡️ To generate it, run: ./gradlew resources.build with the database installed."
  echo "🔄 Starting application without entity jar..."
  JAVA_LOADER_OPTS="-Dloader.path=/app/libs/"
else
  echo "✅ Entity jar found, loading with entities support."
  JAVA_LOADER_OPTS="-Dloader.path=/app/modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar,/app/libs/"
fi

# --- Checking Config Server ---
if [[ -n "$CONFIG_SERVER_URL" ]]; then
  echo "Waiting for the config server to be ready..."
  if [ "$DEBUG_MODE" = "true" ]; then
    echo "⚙️ DEBUG MODE: Config Server: ${CONFIG_SERVER_URL}"
  fi
  until curl -s -o /dev/null -w "%{http_code}" "${CONFIG_SERVER_URL}/application/default" \
      | grep -qE "^2[0-9]{2}$"; do
    sleep 1
  done
else
  echo "CONFIG_SERVER_URL not set, skipping wait for config server"
fi
echo "✅ All set! Starting the main application."

# --- Java Options Configuration ---
JAVA_AGENT_OPTS=""
if [ "$ENABLE_OPEN_TELEMETRY" = "true" ]; then
  JAVA_AGENT_OPTS="-javaagent:/opt/open-telemetry/opentelemetry-javaagent.jar"
  echo "✔️ OpenTelemetry agent enabled."
  if [ "$DEBUG_MODE" = "true" ]; then
    echo "⚙️ DEBUG MODE: Opentelemetry enabled - Configurations:"
    echo "⚙️ DEBUG MODE: OTEL_SERVICE_NAME: ${OTEL_SERVICE_NAME}"
    echo "⚙️ DEBUG MODE: OTEL_METRICS_EXPORTER: ${OTEL_METRICS_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_LOGS_EXPORTER: ${OTEL_LOGS_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_TRACES_EXPORTER: ${OTEL_TRACES_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_PROTOCOL: ${OTEL_EXPORTER_OTLP_PROTOCOL}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_TIMEOUT: ${OTEL_EXPORTER_OTLP_TIMEOUT}"
  fi
fi

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
    echo "✔️ Debug port enabled on port ${DEBUG_PORT}."
fi

if [ "$DEBUG_MODE" = "true" ] && [ -n "${JAVA_OPTS}" ]; then
   echo "⚙️ DEBUG MODE: Additional JAVA_OPTS: ${JAVA_OPTS}"
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_LOADER_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: EntryPoint JAVA_OPTS: ${ENTRYPOINT_JAVA_OPTS}"
fi

echo "🚀 ------- RUNNING APP ------- 🚀"
java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
