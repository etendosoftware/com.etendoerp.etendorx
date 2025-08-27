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

echo "🚀 [RUNTIME STAGE] Starting application..."
echo "--------------------------------------------------------"

# --- Java Options Configuration ---
JAVA_AGENT_OPTS=""
if [ "$ENABLE_OPEN_TELEMETRY" = "true" ]; then
  JAVA_AGENT_OPTS="-javaagent:/opt/open-telemetry/opentelemetry-javaagent.jar"
  echo "✔️ OpenTelemetry agent enabled."
fi

JAVA_LOADER_OPTS="-Dloader.path=/app/modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar,/app/libs/"

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
    echo "✔️ Debug mode enabled on port ${DEBUG_PORT}."
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_LOADER_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"

echo "🚀 ------- RUNNING APP ------- 🚀"
echo "Java options: ${ENTRYPOINT_JAVA_OPTS}"
java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
