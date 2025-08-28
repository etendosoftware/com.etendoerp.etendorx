# Dynamic Docker Container

## Overview

The Dynamic Docker container is a lightweight component of the Etendo RX ecosystem designed to provide dynamic functionality and services. This container follows a multi-stage build approach to create an optimized, minimal runtime environment while maintaining all necessary capabilities.

## Architecture

### Multi-Stage Build Process

The Dynamic Docker container uses a **3-stage build process**:

1. **BUILDER Stage**: Downloads dependencies and application JAR using Gradle
2. **JLINK Stage**: Creates a minimal and customized JRE using Java's jlink tool
3. **RUNTIME Stage**: Final lightweight image with optimized JRE and application

### Key Features

- **Minimal Runtime Environment**: Optimized JRE with only required Java modules
- **Dependency Management**: Automated download of application and dependencies
- **Debug Support**: Configurable debug mode for development
- **Configuration Management**: Integration with Spring Cloud Config Server
- **Lightweight Base**: Alpine Linux for minimal footprint
- **Java 21 Support**: Modern Java runtime with latest features

## Build Process

### Stage 1: Builder (Gradle-based)
The builder stage uses a Gradle image with JDK 21 to:

- **Download Dependencies**: Fetches the main application JAR and user-specified dependencies
- **Repository Access**: Supports both public and private Maven repositories
- **Dependency Resolution**: Handles transitive dependencies automatically
- **Build Optimization**: Uses Gradle daemon disabled mode for container builds

Key characteristics:
- Base image: `gradle:8.8-jdk21-alpine`
- Gradle flags: `--no-daemon` for container optimization
- Supports custom repository URLs and credentials
- Downloads artifacts to `/app/libs/` directory

### Stage 2: JLink (JRE Optimization)
The jlink stage creates a customized Java Runtime Environment:

- **Dependency Analysis**: Uses `jdeps` to analyze JAR dependencies
- **Module Identification**: Identifies required Java modules
- **JRE Creation**: Creates minimal JRE with only necessary modules
- **Size Optimization**: Significantly reduces runtime image size

Essential Java modules included:
- `java.net.http`: HTTP client capabilities
- `jdk.crypto.ec`: Elliptic curve cryptography
- `java.security.jgss`: Security services
- `java.management`: JMX and management
- `jdk.unsupported`: Unsafe operations support
- `java.sql`: Database connectivity
- `java.naming`: JNDI services
- `java.instrument`: Instrumentation support
- `java.desktop`: Desktop integration (if needed)
- `jdk.localedata`: Internationalization support
- `jdk.jdwp.agent`: Debug agent for development

### Stage 3: Runtime (Alpine-based)
The final runtime stage:

- **Minimal Base**: Alpine Linux 3.20 for security and size
- **Custom JRE**: Uses the optimized JRE from jlink stage
- **Application Setup**: Configures the Spring Boot application
- **Environment Configuration**: Sets up all necessary environment variables

## Environment Variables

### Build Arguments
- `DEBUG_PORT`: Debug port for development (default: 8000)
- `SPRING_PROFILES_ACTIVE`: Active Spring profiles
- `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS`: Config server search locations
- `CONFIG_SERVER_URL`: Spring Cloud Config Server URL
- `DB_HOST`: Database host
- `DB_PORT`: Database port
- `DB_SID`: Database name/SID
- `DEPENDENCIES`: Additional user dependencies to include
- `REPO_URL`: Custom repository URL
- `REPO_USER`: Repository username
- `REPO_PASSWORD`: Repository password

### Runtime Environment Variables
The container sets up several environment variables for proper operation:

#### Spring Configuration
- `SPRING_PROFILES_ACTIVE`: Determines which Spring profiles to activate
- `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS`: Config search paths
- `CONFIG_SERVER_URL`: URL for centralized configuration management

#### Database Configuration
- `DB_HOST`: PostgreSQL database host
- `DB_PORT`: PostgreSQL database port
- `DB_SID`: Database name

#### Debug Configuration
- `DISABLE_DEBUG`: Disable debug mode (default: false)
- `DEBUG_MODE`: Enable verbose debug logging (default: false)

## Usage

### Basic Build
```bash
docker build -t dynamic-app \
  --build-arg DEPENDENCIES="com.example:my-app:1.0.0" \
  --build-arg REPO_URL="https://my-repo.com/maven" \
  .
```

### Build with Private Repository
```bash
docker build -t dynamic-app \
  --build-arg DEPENDENCIES="com.example:my-app:1.0.0" \
  --build-arg REPO_URL="https://private-repo.com/maven" \
  --build-arg REPO_USER="username" \
  --build-arg REPO_PASSWORD="password" \
  .
```

### Running the Container
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e CONFIG_SERVER_URL=http://config:8888 \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_SID=etendo \
  dynamic-app
```

### Development Mode
```bash
docker run -p 8080:8080 -p 8000:8000 \
  -e DEBUG_MODE=true \
  -e SPRING_PROFILES_ACTIVE=development \
  -e CONFIG_SERVER_URL=http://localhost:8888 \
  dynamic-app
```

## Configuration Management

### Spring Cloud Config Integration
The Dynamic container integrates with Spring Cloud Config Server for centralized configuration:

- **Automatic Configuration**: Fetches configuration from Config Server on startup
- **Profile-based Configuration**: Supports multiple environments (dev, test, prod)
- **Dynamic Refresh**: Can refresh configuration without restart
- **Fallback Configuration**: Uses local configuration if Config Server is unavailable

### Configuration Priority
1. Environment variables (highest priority)
2. Spring Cloud Config Server
3. Application properties (lowest priority)

## Startup Process

The container startup process follows these steps:

1. **Environment Setup**: Configures Java environment and PATH
2. **Configuration Wait**: Optionally waits for Config Server (in debug mode)
3. **Application Start**: Launches the Spring Boot application with configured parameters

### Startup Script Features (`run.sh`)
- **Debug Mode Support**: Provides verbose logging when `DEBUG_MODE=true`
- **Config Server Wait**: Waits for Config Server availability in debug mode
- **Java Options Configuration**: Dynamically configures JVM options
- **Error Handling**: Proper error handling and exit codes

## Debug and Development

### Debug Mode
When debug mode is enabled:
- Debug agent listens on specified port (default: 8000)
- Verbose logging provides detailed startup information
- Config server connection details are logged
- Java options are displayed for troubleshooting

### Development Best Practices
1. **Use Debug Mode**: Enable debug mode for development environments
2. **Volume Mounts**: Mount source code for rapid development cycles
3. **Port Mapping**: Expose debug ports for IDE connection
4. **Environment Variables**: Use environment-specific configurations

## Optimization and Performance

### Image Size Optimization
- **Multi-stage Build**: Separates build tools from runtime
- **Custom JRE**: Includes only necessary Java modules
- **Alpine Base**: Minimal Linux distribution
- **Layer Optimization**: Efficient Docker layer usage

### Runtime Performance
- **JRE Optimization**: Faster startup with minimal modules
- **Memory Efficiency**: Reduced memory footprint
- **Startup Time**: Optimized for quick application startup

## Security Considerations

### Image Security
- **Minimal Base**: Alpine Linux with security updates
- **No Build Tools**: Runtime image contains no build tools
- **Least Privilege**: Runs with minimal required permissions
- **Dependency Scanning**: Build-time dependency analysis

### Runtime Security
- **Non-root Execution**: Can run as non-root user
- **Network Isolation**: Uses Docker networks for service communication
- **Credential Management**: Secure handling of repository credentials
- **Environment Isolation**: Proper environment variable management

## Monitoring and Observability

### Application Monitoring
- **Health Endpoints**: Spring Boot Actuator endpoints
- **Metrics Collection**: Application metrics via Micrometer
- **Logging**: Structured logging for monitoring systems
- **Tracing**: Distributed tracing support

### Container Monitoring
- **Resource Usage**: CPU and memory monitoring
- **Container Health**: Docker health checks
- **Log Aggregation**: Centralized log collection

## Common Use Cases

### Microservice Deployment
- **Service Discovery**: Integration with service discovery systems
- **Load Balancing**: Support for load balancer integration
- **Circuit Breakers**: Resilience patterns implementation

### Development Environment
- **Local Development**: Easy local development setup
- **Integration Testing**: Support for integration test environments
- **Debugging**: Remote debugging capabilities

## Troubleshooting

### Common Issues

#### Build Failures
- **Cause**: Repository access issues or dependency resolution failures
- **Solution**: Verify repository credentials and network connectivity

#### Startup Issues
- **Cause**: Configuration problems or missing dependencies
- **Solution**: Check environment variables and Config Server connectivity

#### Memory Issues
- **Cause**: Insufficient memory allocation
- **Solution**: Increase container memory limits or optimize JVM settings

#### Debug Connection Issues
- **Cause**: Debug port not accessible
- **Solution**: Ensure debug port is properly exposed and mapped

### Debugging Steps
1. **Enable Debug Mode**: Set `DEBUG_MODE=true` for verbose logging
2. **Check Environment Variables**: Verify all required variables are set
3. **Verify Config Server**: Ensure Config Server is accessible
4. **Review Logs**: Check container logs for error messages
5. **Test Connectivity**: Verify network connectivity to dependencies

## Integration with Etendo RX Ecosystem

### Related Services
The Dynamic container typically works with:
- **Config Server**: Centralized configuration management
- **Service Registry**: Service discovery and registration
- **API Gateway**: Request routing and load balancing
- **Database**: Data persistence layer
- **Monitoring Services**: Observability and monitoring

### Service Communication
- **HTTP/REST**: Primary communication protocol
- **Service Discovery**: Automatic service location
- **Load Balancing**: Client-side load balancing
- **Circuit Breakers**: Fault tolerance patterns

## Best Practices

### Development
1. **Use Debug Mode**: Enable debug mode for development
2. **Environment Separation**: Use different profiles for different environments
3. **Configuration Management**: Externalize configuration using Config Server
4. **Logging**: Use structured logging for better observability

### Production
1. **Resource Limits**: Set appropriate CPU and memory limits
2. **Health Checks**: Configure proper health check endpoints
3. **Monitoring**: Enable monitoring and alerting
4. **Security**: Follow security best practices for container deployment

### CI/CD
1. **Build Optimization**: Use build cache for faster builds
2. **Security Scanning**: Scan images for vulnerabilities
3. **Automated Testing**: Include container tests in CI pipeline
4. **Rollback Strategy**: Implement proper rollback mechanisms

For more information about the complete Etendo RX ecosystem and advanced configuration options, refer to the main documentation.