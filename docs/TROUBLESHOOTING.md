# Troubleshooting Guide

Common issues, solutions, and debugging techniques for the Java Spring Boot application and GitHub Actions workflows.

## Table of Contents

- [Application Issues](#application-issues)
- [Build Issues](#build-issues)
- [Test Failures](#test-failures)
- [Docker Issues](#docker-issues)
- [GitHub Actions Issues](#github-actions-issues)
- [Azure Deployment Issues](#azure-deployment-issues)
- [Database Issues](#database-issues)
- [Performance Issues](#performance-issues)
- [Security Issues](#security-issues)

## Application Issues

### Application Won't Start

#### Symptom
```
Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
```

#### Solutions

1. **Check port availability**
```bash
# See if port 8080 is in use
lsof -i :8080
netstat -an | grep 8080

# Kill process using port
kill -9 $(lsof -t -i:8080)
```

2. **Enable debug logging**
```bash
# Add to application.properties
logging.level.root=DEBUG
logging.level.org.springframework=DEBUG

# Or run with debug flag
mvn spring-boot:run -Dspring-boot.run.arguments=--debug
```

3. **Check for bean configuration errors**
```bash
# Look for circular dependencies or missing beans
grep -i "circular" logs/application.log
grep -i "required a bean" logs/application.log
```

### Health Check Fails

#### Symptom
```
curl http://localhost:8080/actuator/health
# Returns 503 Service Unavailable
```

#### Solutions

1. **Check actuator configuration**
```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
management.health.defaults.enabled=true
```

2. **Verify dependencies are healthy**
```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

3. **Check application logs**
```bash
tail -f logs/spring-boot-application.log | grep -i error
```

### API Returns 500 Internal Server Error

#### Symptom
```bash
curl -X POST http://localhost:8080/api/customers -H "Content-Type: application/json" -d '{...}'
# {"timestamp":"2024-01-15T10:30:00.000+00:00","status":500,"error":"Internal Server Error"}
```

#### Solutions

1. **Check for validation errors**
```java
// Ensure request body has required fields
{
  "name": "Customer Name",  // Required
  "email": "email@example.com"  // Required and valid format
}
```

2. **Review exception logs**
```bash
# Find the actual exception
grep -A 10 "Internal Server Error" logs/spring-boot-application.log
```

3. **Enable SQL logging**
```properties
# application.properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## Build Issues

### Maven Build Fails

#### Symptom
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
```

#### Solutions

1. **Clean and rebuild**
```bash
mvn clean install -U
# -U forces update of dependencies
```

2. **Check Java version**
```bash
java -version
# Should be 17 or higher

# Set JAVA_HOME if needed
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

3. **Verify Maven version**
```bash
mvn --version
# Should be 3.6.0 or higher
```

4. **Clear Maven cache**
```bash
rm -rf ~/.m2/repository
mvn clean install
```

### Checkstyle Failures

#### Symptom
```
[ERROR] src/main/java/com/example/demo/model/Customer.java:[15,1] (javadoc) JavadocType: Missing a Javadoc comment.
```

#### Solutions

1. **Run Checkstyle separately**
```bash
mvn checkstyle:check
```

2. **View Checkstyle report**
```bash
open target/checkstyle-result.xml
```

3. **Temporarily skip Checkstyle**
```bash
mvn clean package -Dcheckstyle.skip=true
```

4. **Fix common issues**
```java
// Add missing Javadoc
/**
 * Customer entity class.
 */
public class Customer {
    // ...
}

// Fix line length (max 100 characters)
// Break long lines

// Fix imports (remove unused)
```

### JaCoCo Coverage Fails

#### Symptom
```
[ERROR] Rule violated for bundle java-app-demo: instructions covered ratio is 0.65, but expected minimum is 0.70
```

#### Solutions

1. **Check current coverage**
```bash
mvn clean verify
open target/site/jacoco/index.html
```

2. **Add more tests**
```bash
# Identify uncovered code
# Write unit tests for uncovered branches
# Focus on service and controller layers
```

3. **Temporarily lower threshold**
```xml
<!-- pom.xml -->
<limit>
  <minimum>0.60</minimum>
</limit>
```

4. **Exclude classes from coverage**
```xml
<configuration>
  <excludes>
    <exclude>**/Application.class</exclude>
    <exclude>**/config/**</exclude>
  </excludes>
</configuration>
```

## Test Failures

### Unit Test Fails

#### Symptom
```
[ERROR] CustomerServiceTest.testCreateCustomer:45 expected: <Customer@123> but was: <null>
```

#### Solutions

1. **Run specific test**
```bash
mvn test -Dtest=CustomerServiceTest#testCreateCustomer
```

2. **Enable test debugging**
```bash
mvn test -Dmaven.surefire.debug
# Then attach debugger to port 5005
```

3. **Check mock setup**
```java
// Ensure mocks are properly configured
@Test
void testCreateCustomer() {
    Customer customer = new Customer("John", "john@example.com");
    
    // Verify mock returns expected value
    when(customerRepository.save(any(Customer.class)))
        .thenReturn(customer);
    
    Customer result = customerService.createCustomer(customer);
    assertNotNull(result);
}
```

### Integration Test Fails

#### Symptom
```
[ERROR] CustomerControllerIT.testCreateCustomer:60 Status expected:<201> but was:<400>
```

#### Solutions

1. **Check request body**
```java
mockMvc.perform(post("/api/customers")
    .contentType(MediaType.APPLICATION_JSON)
    .content("""
        {
          "name": "John Doe",
          "email": "john@example.com"
        }
        """))
    .andDo(print())  // Print request/response
    .andExpect(status().isCreated());
```

2. **Verify database state**
```java
@Test
void testCreateCustomer() {
    // Clear database before test
    customerRepository.deleteAll();
    
    // Run test
    
    // Verify database after test
    List<Customer> customers = customerRepository.findAll();
    assertEquals(1, customers.size());
}
```

3. **Check H2 console**
```properties
# application-test.properties
spring.h2.console.enabled=true

# Then visit: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
```

## Docker Issues

### Docker Build Fails

#### Symptom
```
ERROR [stage-1 3/4] COPY --from=build /app/target/*.jar app.jar
failed to compute cache key: not found
```

#### Solutions

1. **Verify JAR file exists**
```bash
# Build JAR first
mvn clean package -DskipTests

# Check target directory
ls -lh target/*.jar
```

2. **Check Dockerfile COPY path**
```dockerfile
# Ensure path matches pom.xml finalName
COPY --from=build /app/target/java-app-demo-1.0.0.jar app.jar
# Or use wildcard
COPY --from=build /app/target/*.jar app.jar
```

3. **Build with no cache**
```bash
docker build --no-cache -t java-app-demo:latest .
```

### Container Exits Immediately

#### Symptom
```bash
docker run -p 8080:8080 java-app-demo:latest
# Container starts then exits
```

#### Solutions

1. **Check container logs**
```bash
docker logs <container-id>
```

2. **Run interactively**
```bash
docker run -it --entrypoint /bin/sh java-app-demo:latest
# Then try running the app manually
java -jar app.jar
```

3. **Verify ENTRYPOINT**
```dockerfile
# Dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
# Ensure app.jar exists at /app/app.jar
```

### Container Networking Issues

#### Symptom
```bash
curl http://localhost:8080/actuator/health
# Connection refused
```

#### Solutions

1. **Check port mapping**
```bash
docker ps
# Look for PORT column: 0.0.0.0:8080->8080/tcp

# Map port explicitly
docker run -p 8080:8080 java-app-demo:latest
```

2. **Check application port configuration**
```properties
# application.properties
server.port=8080
```

3. **Test from within container**
```bash
docker exec -it <container-id> sh
wget -O - http://localhost:8080/actuator/health
```

## GitHub Actions Issues

### Workflow Not Triggering

#### Symptom
Push to branch doesn't trigger workflow

#### Solutions

1. **Check workflow triggers**
```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
```

2. **Verify workflow is enabled**
```
Repository → Actions → Select workflow → Enable workflow
```

3. **Check workflow syntax**
```bash
# Install actionlint
brew install actionlint

# Validate workflow
actionlint .github/workflows/*.yml
```

4. **Look for workflow errors**
```
Repository → Actions → All workflows → Check for error messages
```

### Workflow Fails at Checkout

#### Symptom
```
Error: fatal: could not read Username for 'https://github.com': terminal prompts disabled
```

#### Solutions

1. **Verify GITHUB_TOKEN permissions**
```yaml
permissions:
  contents: read  # Required for checkout
```

2. **Use HTTPS instead of SSH**
```yaml
- uses: actions/checkout@v4
  with:
    token: ${{ secrets.GITHUB_TOKEN }}
```

3. **Check repository access**
```yaml
# For private repositories
- uses: actions/checkout@v4
  with:
    token: ${{ secrets.PAT_TOKEN }}
```

### Maven Build Fails in CI

#### Symptom
```
[ERROR] No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?
```

#### Solutions

1. **Verify Java setup**
```yaml
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
    cache: 'maven'
```

2. **Check Maven configuration**
```yaml
- name: Build with Maven
  run: mvn -B package --file pom.xml
```

3. **Enable debug output**
```yaml
- name: Build with Maven
  run: mvn -X -B package --file pom.xml
```

### Test Failures Only in CI

#### Symptom
Tests pass locally but fail in GitHub Actions

#### Solutions

1. **Check environment differences**
```yaml
# Ensure consistent environment
env:
  TZ: UTC
  LANG: en_US.UTF-8
```

2. **Increase timeout**
```yaml
- name: Run tests
  run: mvn test
  timeout-minutes: 30
```

3. **Check for race conditions**
```java
// Use @Timeout annotation
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testAsync() {
    // ...
}
```

### Docker Login Fails

#### Symptom
```
Error: Username and password required
```

#### Solutions

1. **Check secrets configuration**
```yaml
- name: Login to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

2. **Verify GITHUB_TOKEN permissions**
```yaml
permissions:
  packages: write
```

3. **Use PAT for authentication**
```yaml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

## Azure Deployment Issues

### OIDC Authentication Fails

#### Symptom
```
Error: OIDC token validation failed
```

#### Solutions

1. **Verify federated credentials**
```bash
az ad app federated-credential list --id $APP_ID
```

2. **Check subject claim format**
```bash
# Format must match exactly
# For branch: repo:org/repo:ref:refs/heads/branch-name
# For environment: repo:org/repo:environment:env-name
```

3. **Verify permissions**
```yaml
permissions:
  id-token: write  # Required for OIDC
  contents: read
```

4. **Check secret values**
```bash
gh secret list
# Ensure AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID are set
```

### Container App Update Fails

#### Symptom
```
ERROR: (ContainerAppUpdateError) The resource cannot be updated
```

#### Solutions

1. **Check image exists**
```bash
az acr repository show-tags \
  --name $ACR_NAME \
  --repository java-app-demo \
  --output table
```

2. **Verify ACR permissions**
```bash
az role assignment list \
  --assignee $APP_ID \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerRegistry/registries/$ACR_NAME
```

3. **Check Container App status**
```bash
az containerapp show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --query "properties.provisioningState"
```

4. **Review Container App logs**
```bash
az containerapp logs show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --follow
```

### Deployment Hangs

#### Symptom
GitHub Actions workflow stuck at "Deploy to Container App" step

#### Solutions

1. **Add timeout**
```yaml
- name: Deploy to Container App
  timeout-minutes: 15
  run: |
    az containerapp update ...
```

2. **Check Azure service health**
```bash
az resource health availability-statuses list \
  --resource-group $RESOURCE_GROUP
```

3. **Cancel and retry**
```bash
gh run cancel $RUN_ID
gh workflow run deploy-azure.yml
```

## Database Issues

### H2 Database Connection Fails

#### Symptom
```
org.h2.jdbc.JdbcSQLException: Database may be already in use
```

#### Solutions

1. **Check for running instances**
```bash
lsof | grep h2
pkill -f h2
```

2. **Use in-memory database**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

3. **Clear database file**
```bash
rm ~/testdb.mv.db
```

### Migration Fails

#### Symptom
```
Schema validation failed: Table 'customer' doesn't exist
```

#### Solutions

1. **Enable schema generation**
```properties
spring.jpa.hibernate.ddl-auto=update
```

2. **Check entity annotations**
```java
@Entity
@Table(name = "customers")  // Verify table name
public class Customer {
    // ...
}
```

3. **View generated SQL**
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

## Performance Issues

### Slow Application Startup

#### Solutions

1. **Profile startup**
```properties
logging.level.org.springframework.boot.autoconfigure=DEBUG
```

2. **Exclude unused auto-configurations**
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class
})
```

3. **Enable lazy initialization**
```properties
spring.main.lazy-initialization=true
```

### High Memory Usage

#### Solutions

1. **Adjust JVM heap**
```bash
java -Xms512m -Xmx1024m -jar app.jar
```

2. **Enable memory profiling**
```bash
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -jar app.jar
```

3. **Check for memory leaks**
```bash
jmap -heap <pid>
jmap -histo <pid> | head -20
```

## Security Issues

### Secrets Exposed in Logs

#### Solutions

1. **Mask secrets**
```yaml
- name: Get secret
  run: echo "::add-mask::${{ secrets.API_KEY }}"
```

2. **Use environment variables**
```yaml
- name: Use secret
  env:
    API_KEY: ${{ secrets.API_KEY }}
  run: |
    echo "Using API key"
    # Never echo $API_KEY directly
```

3. **Check logs**
```bash
# Search for exposed patterns
grep -r "api_key\|password\|secret" logs/
```

### Dependency Vulnerabilities

#### Solutions

1. **Update dependencies**
```bash
mvn versions:display-dependency-updates
mvn versions:use-latest-releases
```

2. **Run security scan**
```bash
mvn org.owasp:dependency-check-maven:check
```

3. **Check Dependabot alerts**
```
Repository → Security → Dependabot alerts
```

## Getting Help

### Enable Debug Logging

```properties
# application.properties
logging.level.root=DEBUG
logging.level.com.example.demo=TRACE
logging.file.name=logs/application.log
```

### Collect Diagnostic Information

```bash
#!/bin/bash
# diagnostics.sh

echo "=== System Information ==="
uname -a
java -version
mvn --version
docker --version

echo "=== Application Status ==="
curl -s http://localhost:8080/actuator/health | jq .

echo "=== Recent Logs ==="
tail -n 50 logs/spring-boot-application.log

echo "=== Maven Dependencies ==="
mvn dependency:tree

echo "=== Docker Images ==="
docker images | grep java-app-demo

echo "=== Running Containers ==="
docker ps -a | grep java-app-demo
```

### Contact

- **GitHub Issues**: https://github.com/your-org/java-app-demo/issues
- **Team Slack**: #platform-support
- **Email**: platform-team@example.com

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Maven Troubleshooting](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Docker Documentation](https://docs.docker.com/)
- [GitHub Actions Debugging](https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows)
- [Azure Container Apps Troubleshooting](https://learn.microsoft.com/en-us/azure/container-apps/troubleshooting)
