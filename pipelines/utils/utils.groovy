/**
 * Checks the health status of a list of Docker containers and waits until all are healthy or a timeout is reached.
 *
 * This function is intended for Jenkins Pipelines (Groovy) and relies on the `sh` step and Docker CLI.
 *
 * Parameters:
 *  - containersToCheck: Iterable (List<String> or String[]) of container name fragments.
 *  Each is appended to CONTEXT_NAME and "-1" to form the full container name.
 *  - maxWaitMinutes: Maximum number of minutes to wait for all containers to become healthy.
 *
 * Returns:
 *  - null if all containers are healthy.
 */
def checkServiceHealthStatus(containersToCheck, maxWaitMinutes) {
    echo "-------------------- Checking Services Health Test --------------------"
    timeout(time: maxWaitMinutes, unit: 'MINUTES') {
        waitUntil {
            def pending = []
            def bad = []
            containersToCheck.each { name ->
                // health: healthy | unhealthy | starting | no-healthcheck | not-found
                def service = "${CONTEXT_NAME}-${name}-1"
                def health = sh(
                    script: "docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' ${service} 2>/dev/null || echo 'not-found'",
                    returnStdout: true
                ).trim()
                echo "${service} -> health=${health}"
                switch (health) {
                    case 'healthy':
                        // ok
                        break
                    case 'unhealthy':
                        // Show last few health logs for context (no jq dependency)
                        sh(
                            label: "Health logs (${service})",
                            script: """
                            echo '--- Inspect (summary) ${service} ---'
                            docker inspect -f 'ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Error={{.State.Error}} FinishedAt={{.State.FinishedAt}}' ${service} || true
                            echo '--- Last logs ${service} ---'
                            docker logs ${service} || true
                            echo '--- Health (if present) ${service} ---'
                            docker inspect -f '{{json .State.Health}}' ${service} 2>/dev/null || true
                            docker inspect --format='{{range .State.Health.Log}}{{println .Start \"|\" .ExitCode \"|\" (printf \"%q\" .Output)}}{{end}}' ${service} 2>/dev/null \
                            | tail -n 6 || true
                            """
                        )
                        bad << service
                        break
                    case 'starting':
                        pending << "${service}(starting)"
                        break
                    case 'no-healthcheck':
                        // Shouldn't happen since all have healthchecks, but keep it explicit
                        pending << "${service}(no-healthcheck)"
                        break
                    default:
                        // not-found or other
                        pending << "${service}(${health})"
                }
            }
            if (!bad.isEmpty()) {
                throw new Exception("Container(s) reported UNHEALTHY: ${bad.join(', ')}")
            }
            if (!pending.isEmpty()) {
                echo "Waiting for health: ${pending.join(', ')}"
                sleep time: 10, unit: 'SECONDS'
                return false
            }
            return true
        }
    }
    return null
}

/**
 * Verifies that a set of Docker containers are in the "running" state and fails the build if any are not.
 *
 * This function is intended to be used from a Jenkins Pipeline (Groovy) and relies on the `sh` step and the
 * Docker CLI being available on the agent running the step.
 *
 * Parameters:
 *  - containersToCheck: Iterable (e.g., List<String> or String[]) of container name fragments. Each element is the
 *    name portion appended to CONTEXT_NAME and "-1" to form the full container name.
 *
 * Return:
 *  - Returns null on success (all containers running). On failure an Exception is thrown and the pipeline will fail.
 */
def checkServiceStatus(containersToCheck) {
    echo "-------------------- Verifying Services Docker Containers --------------------"
    containersToCheck.each { name ->
        def service = "${CONTEXT_NAME}-${name}-1"
        def status = sh(
            script: "docker inspect -f '{{.State.Status}}' ${service} || echo 'not-found'",
            returnStdout: true
        ).trim()
        echo "${service} -> ${status}"
        if (status != "running") {
            sh """
            echo '--- Inspect (summary) ${service} ---'
            docker inspect -f 'ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Error={{.State.Error}} FinishedAt={{.State.FinishedAt}}' ${service} || true
            echo '--- Last logs ${service} ---'
            docker logs ${service} || true
            echo '--- Health (if present) ${service} ---'
            docker inspect -f '{{json .State.Health}}' ${service} 2>/dev/null || true
            """
            throw new Exception("Container ${service} is not in 'running' state (status=${status}).")
        }
    }
    echo "All Docker Containers are running"
    return null
}

/**
 * Verifies that a set of named Docker containers are stopped (or not present).
 *
 * Parameters:
 *  @param servicesToCheck Iterable of service name suffixes (e.g. ["db", "tomcat"]) used
 *                         to build container names as "<CONTEXT_NAME>-<name>-1".
 *
 * Return:
 *  @return null (this function is used for its side effects and throws on failure).
 */
def checkStoppedServices(servicesToCheck) {
    echo "-------------------- Verifying Stopping Services --------------------"
    servicesToCheck.each { name ->
        def service = "${CONTEXT_NAME}-${name}-1"
        def status = sh(script: "docker inspect -f '{{.State.Status}}' ${service} || echo 'not-found'", returnStdout: true).trim()
        echo "${service} -> ${status}"
        if (!(status in ['exited', 'stopping', 'not-found'])) {
            throw new Exception("The container ${service} is still in state '${status}'")
        }
    }
    return null
}

return this
