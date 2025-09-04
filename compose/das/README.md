# DAS (Dynamic Application Service) Docker Container

## Overview

The DAS (Dynamic Application Service) Docker container is a key component of the Etendo RX ecosystem that provides dynamic data access and API services. This container follows a multi-stage build approach to optimize size and performance while ensuring all necessary components are properly configured.

## Architecture

### Multi-Stage Build Process

The DAS Docker container uses a **4-stage build process**:

1. **BUILDER Stage**: Handles the complete build process including entity generation, code compilation, and dependency management
2. **JLINKER Stage**: Creates a minimal and customized JRE for the application using Java's jlink tool
3. **OPENTELEMETRY Stage**: Downloads the OpenTelemetry agent for monitoring and observability
4. **RUNTIME Stage**: Final lightweight image using the customized JRE

### Key Features

- **Dynamic Entity Generation**: Automatically generates Java entities from database schema
- **Database Connectivity Validation**: Checks database accessibility before attempting compilation
- **Graceful Degradation**: Starts without compilation when database is not accessible
- **OpenTelemetry Support**: Built-in observability and monitoring capabilities
- **Debug Support**: Configurable debug mode for development
- **Optimized Runtime**: Minimal JRE with only required modules

## Build Process

### Stage 1: Builder
The builder stage performs three main operations:

#### 1. Database Connectivity Check (New Feature)
- Validates PostgreSQL database connection before proceeding
- If database is not accessible:
  - Skips entity generation and compilation
  - Downloads only the DAS application
  - Allows container to start and show "compilation needed" message on port 8092
  - Logs appropriate warnings for troubleshooting

#### 2. Dynamic Entity Generation
- Downloads the `generate-entities` tool
- Connects to PostgreSQL database using environment variables
- Generates Java entity classes from database schema
- Cleans up previous entity source code

#### 3. Entity Compilation
- Compiles the generated entity classes
- Creates JAR files for the generated entities

#### 4. Application Dependencies
- Downloads the main DAS application
- Downloads additional user-specified dependencies

### Stage 2: JLinker
- Analyzes application dependencies using `jdeps`
- Creates a minimal JRE with only required Java modules
- Significantly reduces final image size
- Includes essential modules like:
  - `java.net.http`
  - `jdk.crypto.ec`
  - `java.security.jgss`
  - `java.management`
  - `jdk.jdwp.agent` (for debugging)

### Stage 3: OpenTelemetry
- Downloads the OpenTelemetry Java agent
- Provides observability capabilities for monitoring and tracing

### Stage 4: Runtime
- Uses Alpine Linux base for minimal footprint
- Copies only necessary artifacts from previous stages
- Configures environment variables for Spring Boot application
- Sets up OpenTelemetry configuration for Jaeger integration

## Environment Variables

### Build Arguments
- `GRADLE_VERSION`: Gradle version for build process (default: 8.8)
- `ETENDORX_VERSION`: Version of Etendo RX components (default: 2.3.3)
- `DEPENDENCIES`: Additional user dependencies to include
- `REPO_URL`, `REPO_USER`, `REPO_PASSWORD`: Repository configuration for private dependencies

### Database Configuration
- `DB_HOST`: PostgreSQL database host
- `DB_PORT`: PostgreSQL database port
- `DB_SID`: PostgreSQL database name

### Runtime Configuration
- `CONFIG_SERVER_URL`: Spring Cloud Config Server URL
- `SPRING_PROFILES_ACTIVE`: Active Spring profiles
- `DEBUG_PORT`: Debug port for development (default: 5021)
- `ENABLE_OPEN_TELEMETRY`: Enable/disable OpenTelemetry (default: false)
- `DISABLE_DEBUG`: Disable debug mode (default: false)

### OpenTelemetry Configuration
- `OTEL_SERVICE_NAME`: Service name for tracing (default: dynamic-das)
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Jaeger endpoint (default: http://jaeger:4318)
- `OTEL_EXPORTER_OTLP_PROTOCOL`: Export protocol (default: http/protobuf)
- `OTEL_TRACES_EXPORTER`: Trace exporter type (default: otlp)

## Usage

### Basic Usage
```bash
docker build -t das-app \
  --build-arg DB_HOST=localhost \
  --build-arg DB_PORT=5432 \
  --build-arg DB_SID=etendo \
  .
```

### With Custom Dependencies
```bash
docker build -t das-app \
  --build-arg DB_HOST=localhost \
  --build-arg DB_PORT=5432 \
  --build-arg DB_SID=etendo \
  --build-arg DEPENDENCIES="com.example:custom-lib:1.0.0" \
  .
```

### Running the Container
```bash
docker run -p 8092:8092 \
  -e SPRING_PROFILES_ACTIVE=development \
  -e CONFIG_SERVER_URL=http://config:8888 \
  das-app
```

## Database Connectivity

The DAS container includes robust database connectivity validation:

### Successful Connection
When the database is accessible:
1. Entity generation proceeds normally
2. Code compilation completes successfully
3. Full DAS functionality is available

### Database Not Accessible
When the database cannot be reached:
1. A warning message is displayed
2. Entity generation and compilation are skipped
3. DAS starts in "compilation needed" mode
4. Port 8092 shows status indicating compilation is required
5. Container logs provide troubleshooting information

### Troubleshooting Database Issues
- Verify database host and port configuration
- Check network connectivity between containers
- Ensure database credentials are correct
- Review container logs for specific error messages

## Ports

- **8092**: Main DAS application port
- **5021**: Debug port (when debug mode is enabled)

## Volumes

The container doesn't require persistent volumes for normal operation, but you may want to mount:
- `/app/logs`: For persistent application logs
- `/app/config`: For external configuration files

## Development vs Production

### Development Mode
- Debug port enabled (5021)
- Verbose logging
- OpenTelemetry disabled by default
- Hot reload capabilities

### Production Mode
- Debug disabled
- Optimized JRE
- OpenTelemetry enabled for monitoring
- Minimal resource footprint

## Monitoring and Observability

The container includes comprehensive monitoring support:

### OpenTelemetry Integration
- Automatic trace collection
- Metrics export to monitoring systems
- Integration with Jaeger for distributed tracing

### Health Checks
- Database connectivity validation
- Application readiness probes
- Custom health endpoints

## Security Considerations

- Minimal base image (Alpine Linux)
- Custom JRE with only required modules
- No unnecessary tools in runtime image
- Secure handling of database credentials
- Network-level access controls

## Best Practices

1. **Database Preparation**: Ensure database is accessible before starting DAS
2. **Environment Variables**: Use secure methods for credential management
3. **Resource Limits**: Set appropriate CPU and memory limits
4. **Monitoring**: Enable OpenTelemetry in production environments
5. **Networking**: Use Docker networks for service communication
6. **Updates**: Regularly update base images and dependencies

## Common Issues and Solutions

### Entity Generation Fails
- **Cause**: Database connectivity issues
- **Solution**: Verify database configuration and network connectivity

### Memory Issues
- **Cause**: Insufficient memory allocation
- **Solution**: Increase container memory limits

### Build Failures
- **Cause**: Network issues or repository access problems
- **Solution**: Check network connectivity and repository credentials

### Debug Connection Issues
- **Cause**: Debug port not accessible
- **Solution**: Ensure debug port is properly exposed and mapped

## Related Services

The DAS container works in conjunction with:
- **Config Server**: Centralized configuration management
- **Auth Service**: Authentication and authorization
- **Edge Service**: API gateway and routing
- **Jaeger**: Distributed tracing and monitoring
- **PostgreSQL**: Primary database storage

For more information about the complete Etendo RX ecosystem, refer to the main documentation.