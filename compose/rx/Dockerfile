# Stage 1: Builder - Download JAR with Gradle
FROM gradle:8.8-jdk21-alpine AS builder
ARG DEPENDENCIES
ARG REPO_URL
ARG REPO_USER
ARG REPO_PASSWORD
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
WORKDIR /app
COPY rx/build.gradle /app/
RUN gradle downloadJar --no-daemon \
    -PDEPENDENCIES=${DEPENDENCIES} \
    -PREPO_URL=${REPO_URL} \
    -PREPO_USER=${REPO_USER} \
    -PREPO_PASSWORD=${REPO_PASSWORD}

# Stage 2: jlink - Create custom minimal JRE
FROM eclipse-temurin:21-jdk-alpine AS jlink
WORKDIR /app
COPY --from=builder /app/libs/app.jar /app/app.jar
RUN jdeps --ignore-missing-deps --print-module-deps --multi-release 21 /app/app.jar > modules.txt \
    && jlink --add-modules $(cat modules.txt),java.net.http,jdk.crypto.ec,java.security.jgss,java.management,jdk.unsupported,java.sql,java.naming,java.instrument,java.desktop,jdk.localedata \
            --compress=2 --strip-debug --no-header-files --no-man-pages --output /custom-jre

# Stage 3: Runtime - Slim with custom JRE
FROM alpine:3.20
ARG DEBUG_PORT=8000
ARG SPRING_PROFILES_ACTIVE
ARG SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS
ARG CONFIG_SERVER_URL
ARG DB_HOST
ARG DB_PORT
ARG DB_SID
RUN apk add --no-cache libstdc++ curl
COPY --from=jlink /custom-jre /opt/jre
ENV PATH="/opt/jre/bin:$PATH"
WORKDIR /app
COPY --from=builder /app/libs/app.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=${SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS}
ENV CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
ENV DB_HOST=${DB_HOST}
ENV DB_PORT=${DB_PORT}
ENV DB_SID=${DB_SID}
CMD if [ -n "${CONFIG_SERVER_URL}" ]; then \
        while ! curl -f -s "${CONFIG_SERVER_URL}/application/default" >/dev/null; do echo "Waiting for config server..."; sleep 1; done; \
    fi && \
    java -XX:+UseContainerSupport -XX:+UseZGC -jar app.jar
