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

# --- Function to check database connectivity ---
check_database_connectivity() {
    echo "🔍 Checking database connectivity..."
    
    # Install PostgreSQL client if not available
    if ! command -v psql > /dev/null 2>&1; then
        echo "  -> Installing PostgreSQL client..."
        apk add --no-cache postgresql-client
    fi
    
    # Test database connection
    if timeout 10 psql -h host.docker.internal -p "${DB_PORT}" -U tad -d "${DB_SID}" -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✅ Database is accessible"
        return 0
    else
        echo "❌ Database is not accessible"
        return 1
    fi
}

# --- 1. Database Connectivity Check ---
echo "🔗 [PASO 0/3] Verificando conectividad con la base de datos..."
if ! check_database_connectivity; then
    echo "⚠️  Database is not accessible. Skipping source code generation and compilation."
    echo "   DAS will start but will show a 'compilation needed' message on port 8092."
    echo "   Please ensure the database is accessible and restart the container to compile."
    
    # Create minimal structure to allow DAS to start
    mkdir -p /app/libs
    mkdir -p /app/modules_gen/com.etendorx.entities/build/libs
    
    # Download only the DAS application without entities generation
    echo "📦 Downloading DAS application..."
    export DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}"
    gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
    
    # Download user dependencies
    if [ -n "${USER_DEPENDENCIES}" ]; then
        echo "📦 Downloading user dependencies..."
        export DEPENDENCIES=${USER_DEPENDENCIES}
        gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
    fi
    
    gradle --stop
    
    echo "✅ DAS will start without compilation. Access port 8092 for compilation status."
    exit 0
fi

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
