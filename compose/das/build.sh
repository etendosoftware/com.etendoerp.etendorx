#!/bin/sh
# Copyright Etendo Software.

# This script runs in the 'builder' stage of the Dockerfile to prepare
# all necessary artifacts before the execution stage.

# Exit on any error
set -e

# --- Default Values ---
: "${GRADLE_FLAGS:=--no-daemon --info --refresh-dependencies}"
: "${TASK:=downloadJar}"

export USER_DEPENDENCIES=${DEPENDENCIES}
echo "🚀 [BUILD STAGE] Starting artifact generation..."
echo "--------------------------------------------------------"

# --- Helpers to build unique candidate list ---
add_candidate() {
    CAND="$1"
    [ -z "$CAND" ] && return 0
    for EXIST in $CANDIDATES; do
        if [ "$EXIST" = "$CAND" ]; then
            return 0
        fi
    done
    CANDIDATES="$CANDIDATES $CAND"
}

# --- Build ordered list of host candidates ---
get_docker_host_candidates() {
    echo "🔍 Building Docker host candidates..." >&2
    CANDIDATES=""

    # 1) macOS/Windows (Docker Desktop): host.docker.internal resolved IP
    RESOLVED_IP=""
    if command -v getent >/dev/null 2>&1; then
        RESOLVED_IP=$(getent hosts host.docker.internal 2>/dev/null | awk '{print $1}' | head -n1)
    fi
    if [ -z "$RESOLVED_IP" ] && command -v nslookup >/dev/null 2>&1; then
        RESOLVED_IP=$(nslookup host.docker.internal 2>/dev/null | awk 'BEGIN{ip=""} /^Address: [0-9.]+/{ip=$2; exit} /^Address [0-9]+: [0-9.]+/{ip=$3; exit} END{print ip}')
    fi
    if [ -n "$RESOLVED_IP" ]; then
        echo "  -> Candidate (resolved host.docker.internal): $RESOLVED_IP" >&2
        add_candidate "$RESOLVED_IP"
    fi

    # 1b) Add literal hostname (works on macOS/Windows)
    echo "  -> Candidate (literal host.docker.internal)" >&2
    add_candidate "host.docker.internal"

    # 2) Universal Linux: default gateway from /proc/net/route
    HEXGW=$(awk '$2=="00000000" && $3!="00000000" {print $3; exit}' /proc/net/route 2>/dev/null)
    if [ -n "$HEXGW" ]; then
        A=$((0x${HEXGW:6:2}))
        B=$((0x${HEXGW:4:2}))
        C=$((0x${HEXGW:2:2}))
        D=$((0x${HEXGW:0:2}))
        GW_IP="$A.$B.$C.$D"
        echo "  -> Candidate (gateway from /proc/net/route): $GW_IP" >&2
        add_candidate "$GW_IP"
    fi

    # 3) Fallback: ip route show default
    if command -v ip >/dev/null 2>&1; then
        GW2=$(ip route show default 2>/dev/null | awk '/default/ {print $3; exit}')
        if [ -n "$GW2" ]; then
            echo "  -> Candidate (gateway from ip route): $GW2" >&2
            add_candidate "$GW2"
        fi
    fi

    # Output candidates space-separated (stdout)
    echo "$CANDIDATES"
}

# --- Function to check database connectivity ---
check_database_connectivity() {
    echo "🔍 Checking database connectivity..."

    CANDIDATES=$(get_docker_host_candidates)
    echo "  -> Candidates to try: $CANDIDATES"

    # Install PostgreSQL client if not available
    if ! command -v psql > /dev/null 2>&1; then
        echo "  -> Installing PostgreSQL client..."
        apk add --no-cache postgresql-client
    fi

    ATTEMPT=0
    for HOST_CAND in $CANDIDATES; do
        ATTEMPT=$((ATTEMPT+1))
        echo "  -> Attempt #$ATTEMPT: Testing host '$HOST_CAND' (port ${DB_PORT}, db ${DB_SID}, user tad)"
        PSQL_RESULT=$(timeout 10 env PGPASSWORD="${DB_PASSWORD:-tad}" psql -h "$HOST_CAND" -p "${DB_PORT}" -U tad -d "${DB_SID}" -c "SELECT 1;" 2>&1)
        PSQL_EXIT_CODE=$?
        echo "     Exit code: $PSQL_EXIT_CODE"
        echo "     Output: $PSQL_RESULT"
        if [ $PSQL_EXIT_CODE -eq 0 ]; then
            echo "✅ Database is accessible using host: $HOST_CAND"
            export DOCKER_HOST_IP="$HOST_CAND"
            return 0
        fi
    done

    echo "❌ Database is not accessible with any candidate."
    return 1
}

# --- 1. Database Connectivity Check ---
echo "🔗 [STEP 0/3] Checking database connectivity..."
if ! check_database_connectivity; then
    echo "⚠️  Database is not accessible. Skipping source code generation and compilation."
    echo "   DAS will start but will show a 'compilation needed' message on port 8092."
    echo "   Please ensure the database is accessible and restart the container to compile."
    echo "   ⚡ To initialize resources, run: ./gradlew resources.build with the database installed."

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

# --- 1. Dynamic Entity Generation ---
echo "🧬 [STEP 1/3] Generating entities from the database..."
echo "  -> Setting dependency for the 'generate-entities' tool."

echo "  -> Downloading the tool with Gradle..."
# Pass dependencies as a project property to Gradle
export DEPENDENCIES="com.etendorx:generate-entities:${ETENDORX_VERSION}"
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
gradle --stop

echo "  -> Cleaning old source code (if exists)."
rm -rf /app/modules_gen/com.etendorx.entities/src

echo "  -> Creating gradle.properties file for entity generation."
# Use environment variables (passed as ARG in the Dockerfile) for connection
cat <<EOF > /app/gradle.properties
bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://${DOCKER_HOST_IP}:${DB_PORT}
bbdd.sid=${DB_SID}
bbdd.systemUser=postgres
bbdd.systemPassword=syspass
bbdd.user=tad
bbdd.password=tad
bbdd.sessionConfig=select
update_dateFormat('DD-MM-YYYY')
rx.generateCode=true
EOF

echo "  -> Content of gradle.properties:"
cat /app/gradle.properties
echo "-------------------------------"

echo "  -> Running the tool to generate entity source code..."
java -jar libs/app.jar

# --- 2. Compilation of Generated Entities ---
echo "🛠️ [STEP 2/3] Compiling generated entities..."
cd /app/modules_gen/com.etendorx.entities
echo "ETENDORX_VERSION=${ETENDORX_VERSION}" > gradle.properties
echo "  -> Compiling source code..."
gradle build ${GRADLE_FLAGS}

# --- 3. Downloading Application Dependencies ---
echo "📦 [STEP 3/3] Downloading main application dependencies..."
cd /app

echo "  -> Downloading main artifact (DAS)..."
export DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}"
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"

echo "  -> Downloading additional user dependencies..."
# Use the DEPENDENCIES variable defined in the Dockerfile
export DEPENDENCIES=${USER_DEPENDENCIES}
gradle "${TASK}" ${GRADLE_FLAGS} -PDEPENDENCIES="${DEPENDENCIES}"
gradle --stop

echo "✅ [BUILD STAGE] Artifact generation has completed."
