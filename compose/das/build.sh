#!/bin/sh
# Copyright Etendo Software.

# Este script se ejecuta en la etapa 'builder' del Dockerfile para preparar
# todos los artefactos necesarios antes de la etapa de ejecución.

# Salir si ocurre cualquier error
set -e

# --- Valores por Defecto ---
: "${GRADLE_FLAGS:=--no-daemon --info --refresh-dependencies}"
: "${TASK:=downloadJar}"

export USER_DEPENDENCIES=${DEPENDENCIES}
echo "🚀 [ETAPA DE BUILD] Iniciando la generación de artefactos..."
echo "--------------------------------------------------------"

# --- 1. Generación Dinámica de Entidades ---
echo "🧬 [PASO 1/3] Generando entidades desde la base de datos..."
echo "  -> Configurando dependencia para la herramienta 'generate-entities'."

echo "  -> Descargando la herramienta con Gradle..."
# Pasa las dependencias como una propiedad de proyecto a Gradle
export DEPENDENCIES="com.etendorx:generate-entities:${ETENDORX_VERSION}"
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
gradle --stop

echo "  -> Limpiando código fuente antiguo (si existe)."
rm -rf /app/modules_gen/com.etendorx.entities/src

echo "  -> Creando archivo gradle.properties para la generación de entidades."
# Utiliza las variables de entorno (pasadas como ARG en el Dockerfile) para la conexión
echo -e "bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://host.docker.internal:${DB_PORT}
bbdd.sid=${DB_SID}
bbdd.systemUser=postgres
bbdd.systemPassword=syspass
bbdd.user=tad
bbdd.password=tad
bbdd.sessionConfig=select
update_dateFormat('DD-MM-YYYY')
rx.generateCode=true" > /app/gradle.properties

echo "  -> Contenido de gradle.properties:"
cat /app/gradle.properties
echo "-------------------------------"

echo "  -> Ejecutando la herramienta para generar el código fuente de las entidades..."
java -jar libs/app.jar

# --- 2. Compilación de Entidades Generadas ---
echo "🛠️ [PASO 2/3] Compilando las entidades generadas..."
cd /app/modules_gen/com.etendorx.entities
echo "ETENDORX_VERSION=${ETENDORX_VERSION}" > gradle.properties
echo "  -> Compilando el código fuente..."
gradle build ${GRADLE_FLAGS}

# --- 3. Descarga de Dependencias de la Aplicación ---
echo "📦 [PASO 3/3] Descargando las dependencias principales de la aplicación..."
cd /app

echo "  -> Descargando el artefacto principal (DAS)..."
export DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}"
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"

echo "  -> Descargando las dependencias adicionales del usuario..."
# Utiliza la variable DEPENDENCIES definida en el Dockerfile
export DEPENDENCIES=${USER_DEPENDENCIES}
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
gradle --stop

echo "✅ [ETAPA DE BUILD] La generación de artefactos ha finalizado."
