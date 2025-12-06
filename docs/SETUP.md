# Setup Guide

This guide will help you set up the Java App Demo project for local development.

## Prerequisites

### Required Software

- **Java Development Kit (JDK)**: Version 17 or 21
  - Download from [Eclipse Temurin](https://adoptium.net/)
  - Verify: `java -version`

- **Apache Maven**: Version 3.8 or higher
  - Download from [Maven website](https://maven.apache.org/)
  - Verify: `mvn -version`

- **Git**: Latest version
  - Download from [git-scm.com](https://git-scm.com/)
  - Verify: `git --version`

### Optional Software

- **Docker**: For containerization
  - Download from [Docker website](https://www.docker.com/)
  - Verify: `docker --version`

- **Azure CLI**: For Azure deployments
  - Install: `brew install azure-cli` (macOS) or download from [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
  - Verify: `az --version`

- **IDE**: IntelliJ IDEA or VS Code recommended
  - [IntelliJ IDEA](https://www.jetbrains.com/idea/)
  - [VS Code](https://code.visualstudio.com/) with Java Extension Pack

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/java-app-demo.git
cd java-app-demo
```

### 2. Verify Java Installation

```bash
java -version
# Should show Java 17 or 21

echo $JAVA_HOME
# Should point to your JDK installation
```

If `JAVA_HOME` is not set:

**macOS/Linux:**
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

**Windows:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

### 3. Build the Project

```bash
# Download dependencies and build
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

### 4. Run Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=CustomerServiceTest

# Run integration tests
mvn verify
```

### 5. Run the Application

```bash
# Run with Maven
mvn spring-boot:run

# Run the JAR directly
java -jar target/java-app-demo-1.0.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 6. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8080/api/health

# Expected response:
# {"status":"UP","timestamp":"2024-12-05T10:30:00","application":"java-app-demo","version":"1.0.0"}
```

## IDE Setup

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open → Select `java-app-demo` directory
3. Wait for Maven import to complete
4. Right-click `Application.java` → Run
5. Install recommended plugins:
   - Lombok
   - CheckStyle-IDEA

### VS Code

1. Open VS Code
2. File → Open Folder → Select `java-app-demo`
3. Install recommended extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support
4. Press F5 to run/debug

## Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# Logging
logging.level.com.example=DEBUG
```

### Environment-Specific Configuration

Create profile-specific files:
- `application-dev.properties`
- `application-test.properties`
- `application-prod.properties`

Run with specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Docker Setup

### Build Docker Image

```bash
# Build image
docker build -t java-app-demo:latest .

# Verify image
docker images | grep java-app-demo
```

### Run Docker Container

```bash
# Run container
docker run -p 8080:8080 --name java-app-demo java-app-demo:latest

# Run with environment variables
docker run -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  java-app-demo:latest

# Run in detached mode
docker run -d -p 8080:8080 --name java-app-demo java-app-demo:latest
```

### Docker Compose (Optional)

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
```

Run: `docker-compose up`

## Database Setup

The application uses H2 in-memory database by default. To use a different database:

### PostgreSQL

1. Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/javaappdemo
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use a different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Maven Build Fails

```bash
# Clean Maven cache
mvn clean
rm -rf ~/.m2/repository

# Rebuild
mvn clean install
```

### Tests Fail

```bash
# Run with debug logs
mvn test -X

# Skip tests temporarily
mvn clean install -DskipTests
```

### Out of Memory

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx1024m"

# Increase application memory
java -Xmx512m -jar target/java-app-demo-1.0.0-SNAPSHOT.jar
```

## Next Steps

- Review [Workflows Documentation](WORKFLOWS.md)
- Set up [GitHub Actions locally](#github-actions-setup)
- Configure [Development Environment](#development-environment)
- Read [Contributing Guidelines](../CONTRIBUTING.md)

## GitHub Actions Setup

### Act (Local Workflow Testing)

Install [act](https://github.com/nektos/act) to test workflows locally:

```bash
# macOS
brew install act

# Test workflow
act -j build
```

## Development Environment

### Code Style

The project uses Checkstyle. Configure your IDE:

**IntelliJ:**
1. Settings → Editor → Code Style → Java
2. Import `checkstyle.xml`

**VS Code:**
1. Install "Checkstyle for Java" extension
2. Set checkstyle configuration file path

### Git Hooks (Optional)

Set up pre-commit hooks:

```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/sh
mvn checkstyle:check
EOF

# Make executable
chmod +x .git/hooks/pre-commit
```

## Support

For additional help:
- Check [Troubleshooting Guide](TROUBLESHOOTING.md)
- Open an issue on GitHub
- Contact the development team
