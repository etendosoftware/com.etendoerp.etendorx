# Stage 1: Builder - Descarga el JAR usando Gradle
# Esta etapa utiliza una imagen de Gradle con JDK 21 para descargar las dependencias
# y el JAR de la aplicación. Esto mantiene las herramientas de build fuera de la imagen final.
FROM gradle:8.8-jdk21-alpine AS builder
ARG DEPENDENCIES
ARG REPO_URL
ARG REPO_USER
ARG REPO_PASSWORD
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
WORKDIR /app
COPY rx/build.gradle /app/
# Ejecuta la tarea de Gradle para descargar el JAR, pasando los argumentos necesarios
RUN gradle downloadJar --no-daemon \
    -PDEPENDENCIES=${DEPENDENCIES} \
    -PREPO_URL=${REPO_URL} \
    -PREPO_USER=${REPO_USER} \
    -PREPO_PASSWORD=${REPO_PASSWORD}

# Stage 2: jlink - Crea una JRE mínima y personalizada
# Esta etapa utiliza jdeps para analizar las dependencias del JAR y luego jlink
# para crear un Java Runtime Environment (JRE) que solo contiene los módulos necesarios.
FROM eclipse-temurin:21-jdk-alpine AS jlink
WORKDIR /app
# Copia el JAR de la etapa anterior
COPY --from=builder /app/libs/app.jar /app/app.jar
# Analiza las dependencias y crea la JRE personalizada
RUN jdeps --ignore-missing-deps --print-module-deps --multi-release 21 /app/app.jar > modules.txt \
    && jlink --add-modules $(cat modules.txt),java.net.http,jdk.crypto.ec,java.security.jgss,java.management,jdk.unsupported,java.sql,java.naming,java.instrument,java.desktop,jdk.localedata,jdk.jdwp.agent \
            --compress=2 --strip-debug --no-header-files --no-man-pages --output /custom-jre

# Stage 3: Runtime - La imagen final, ligera y optimizada
# Esta es la imagen final que se ejecutará. Se basa en Alpine Linux, que es muy ligera.
FROM alpine:3.20
ARG DEBUG_PORT=8000
ARG SPRING_PROFILES_ACTIVE
ARG SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS
ARG CONFIG_SERVER_URL
ARG DB_HOST
ARG DB_PORT
ARG DB_SID
# Instala dependencias mínimas necesarias en tiempo de ejecución
RUN apk add --no-cache curl unzip libstdc++ wget
# Copia la JRE personalizada de la etapa 'jlink'
COPY --from=jlink /custom-jre /opt/jre
# Añade la JRE al PATH para que el comando 'java' esté disponible
ENV PATH="/opt/jre/bin:$PATH"
WORKDIR /app
# Copia el JAR de la aplicación desde la etapa 'builder'
COPY --from=builder /app/libs/app.jar /app/app.jar
# Establece las variables de entorno para la aplicación Spring
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=${SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS}
ENV CONFIG_SERVER_URL=${CONFIG_SERVER_URL}
ENV DB_HOST=${DB_HOST}
ENV DB_PORT=${DB_PORT}
ENV DB_SID=${DB_SID}
# Comando de inicio: espera al config-server (si está definido) y luego ejecuta la aplicación
COPY rx/run.sh /app/run.sh
RUN chmod +x /app/run.sh

ENTRYPOINT [ "/app/run.sh" ]
