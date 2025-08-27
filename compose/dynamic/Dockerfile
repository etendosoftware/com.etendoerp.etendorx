# Stage 1: Builder - Download the JAR using Gradle
# This stage uses a Gradle image with JDK 21 to download the dependencies
# and the application JAR. This keeps the build tools out of the final image.
FROM gradle:8.8-jdk21-alpine AS builder
ARG DEPENDENCIES
ARG REPO_URL
ARG REPO_USER
ARG REPO_PASSWORD
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
WORKDIR /app
COPY dynamic/build.gradle /app/
# Runs the Gradle task to download the JAR, passing the necessary arguments
RUN gradle downloadJar --no-daemon \
    -PDEPENDENCIES=${DEPENDENCIES} \
    -PREPO_URL=${REPO_URL} \
    -PREPO_USER=${REPO_USER} \
    -PREPO_PASSWORD=${REPO_PASSWORD}

# Stage 2: jlink - Creates a minimal and custom JRE
# This stage uses jdeps to analyze the JAR dependencies and then jlink
# to create a Java Runtime Environment (JRE) that only contains the necessary modules.
FROM eclipse-temurin:21-jdk-alpine AS jlink
WORKDIR /app
# Copy the JAR from the previous stage
COPY --from=builder /app/libs/app.jar /app/app.jar
# Analyze dependencies and create the custom JRE
RUN jdeps --ignore-missing-deps --print-module-deps --multi-release 21 /app/app.jar > modules.txt \
    && jlink --add-modules $(cat modules.txt),java.net.http,jdk.crypto.ec,java.security.jgss,java.management,jdk.unsupported,java.sql,java.naming,java.instrument,java.desktop,jdk.localedata,jdk.jdwp.agent \
            --compress=2 --strip-debug --no-header-files --no-man-pages --output /custom-jre

# Stage 3: Runtime - The final, lightweight and optimized image
# This is the final image that will run. It is based on Alpine Linux, which is very lightweight.
FROM alpine:3.20
ARG DEBUG_PORT=8000
ARG SPRING_PROFILES_ACTIVE
ARG SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS
ARG CONFIG_SERVER_URL
ARG DB_HOST
ARG DB_PORT
ARG DB_SID
# Installs minimal runtime dependencies
RUN apk add --no-cache curl unzip libstdc++ wget
# Copy the custom JRE from the 'jlink' stage
COPY --from=jlink /custom-jre /opt/jre
# Add the JRE to PATH so the 'java' command is available
ENV PATH="/opt/jre/bin:$PATH"
WORKDIR /app
# Copy the application JAR from the 'builder' stage
COPY --from=builder /app/libs/app.jar /app/app.jar
# Set environment variables for the Spring application
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=${SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS}
ENV CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
ENV DB_HOST=${DB_HOST}
ENV DB_PORT=${DB_PORT}
ENV DB_SID=${DB_SID}
# Startup command: waits for the config-server (if defined) and then runs the application
COPY dynamic/run.sh /app/run.sh
RUN chmod +x /app/run.sh

ENTRYPOINT [ "/app/run.sh" ]
