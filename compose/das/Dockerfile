# =========================================================================================
# STAGE 1: BUILDER
# This stage is responsible for the entire build process: it generates entities,
# compiles code, and downloads all dependencies.
# =========================================================================================
FROM amazoncorretto:17-alpine-jdk AS builder

# Build arguments
ARG GRADLE_VERSION=8.8
ARG ETENDORX_VERSION=2.3.3
ARG DEPENDENCIES
ARG REPO_URL
ARG REPO_USER
ARG REPO_PASSWORD

# --- Tools Installation ---
RUN apk add --no-cache wget unzip curl postgresql-client

# --- Gradle Installation ---
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip /tmp/gradle-${GRADLE_VERSION}-bin.zip -d /opt \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle

# --- Build Environment Variables Configuration ---
ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
ENV GRADLE_HOME=/opt/gradle
ENV PATH=${JAVA_HOME}/bin:${GRADLE_HOME}/bin:${PATH}
ENV ETENDORX_VERSION=${ETENDORX_VERSION}
ENV DEPENDENCIES=${DEPENDENCIES}

WORKDIR /app

# --- Build Files and Script Copy ---
COPY das/build.gradle ./
COPY das/modules_gen/com.etendorx.entities/build.gradle ./modules_gen/com.etendorx.entities/
COPY das/build.sh ./build.sh
RUN chmod +x ./build.sh

# --- Build Process Execution ---
ARG DB_HOST
ARG DB_PORT
ARG DB_SID
ENV DB_HOST=${DB_HOST}
ENV DB_PORT=${DB_PORT}
ENV DB_SID=${DB_SID}
RUN ./build.sh

# =========================================================================================
# STAGE 2: JLINKER
# This stage creates a minimal and customized JRE for the application.
# =========================================================================================
FROM amazoncorretto:17-alpine-jdk AS jlinker

# Installs 'binutils' which contains the 'objcopy' tool, necessary for jlink.
RUN apk add --no-cache binutils

WORKDIR /app

# Copies the compiled artifacts from the previous stage
COPY --from=builder /app .

# Analyzes dependencies and creates the customized JRE
RUN jdeps \
        --ignore-missing-deps \
        --print-module-deps \
        --multi-release 17 \
        --class-path "libs/*" \
        --class-path "modules_gen/com.etendorx.entities/build/libs/*" \
        libs/app.jar > modules.txt \
    && jlink \
        --add-modules $(cat modules.txt),java.net.http,jdk.crypto.ec,java.security.jgss,java.management,jdk.unsupported,java.sql,java.naming,java.instrument,java.desktop,jdk.localedata,jdk.jdwp.agent \
        --compress=2 \
        --no-header-files \
        --no-man-pages \
        --output /custom-jre

# =========================================================================================
# STAGE 3: OPENTELEMETRY
# Stage dedicated to downloading the OpenTelemetry agent.
# =========================================================================================
FROM alpine:3.20 AS opentelemetry
ARG OPEN_TELEMETRY_VERSION=v2.18.1
WORKDIR /opt/open-telemetry
RUN apk add --no-cache wget
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OPEN_TELEMETRY_VERSION}/opentelemetry-javaagent.jar

# =========================================================================================
# STAGE 4: RUNTIME
# Final lightweight image, using the customized JRE from the 'jlinker' stage.
# =========================================================================================
FROM alpine:3.20

# Runtime arguments
ARG DEBUG_PORT=5021
ARG CONFIG_SERVER_URL
ARG SPRING_PROFILES_ACTIVE

# --- Minimal Dependencies Installation ---
RUN apk add --no-cache curl libstdc++

# --- Java Environment Configuration ---
# Copy the custom and optimized JRE from the 'jlinker' stage.
COPY --from=jlinker /custom-jre /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=${JAVA_HOME}/bin:$PATH

WORKDIR /app

# --- Artifacts Copy ---
# Copy only the directories containing the necessary JARs for execution.
COPY --from=builder /app/libs ./libs/
COPY --from=builder /app/modules_gen/com.etendorx.entities/build/libs ./modules_gen/com.etendorx.entities/build/libs/
# Copy the OpenTelemetry agent.
COPY --from=opentelemetry /opt/open-telemetry/opentelemetry-javaagent.jar /opt/open-telemetry/

# --- Runtime Environment Variables Configuration ---
ENV CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV DEBUG_PORT=${DEBUG_PORT}

# OpenTelemetry Configuration
ENV OTEL_SERVICE_NAME=dynamic-das
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TIMEOUT=10000

# --- Startup Script ---
COPY das/run.sh ./run.sh
RUN chmod +x ./run.sh

# --- Entrypoint ---
ENTRYPOINT [ "./run.sh" ]
