# Composite Build Action

A reusable composite action for building Java applications with standardized caching and artifact management.

## Features

- Automatic setup of Java Development Kit (JDK)
- Intelligent dependency caching for Maven and Gradle
- Support for both Maven and Gradle build tools
- Automated artifact path detection
- Build status reporting

## Usage

### Basic Usage

```yaml
- name: Build application
  uses: ./.github/actions/composite-build-action
  with:
    java-version: '17'
    build-tool: 'maven'
```

### Advanced Usage with Caching

```yaml
- name: Build application with custom cache
  uses: ./.github/actions/composite-build-action
  with:
    java-version: '21'
    build-tool: 'maven'
    cache-key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `java-version` | Java version to use for building | No | `17` |
| `build-tool` | Build tool to use (maven or gradle) | No | `maven` |
| `cache-key` | Cache key for dependency caching | No | `''` |

## Outputs

| Output | Description |
|--------|-------------|
| `build-status` | Status of the build (success/failure) |
| `artifact-path` | Path to build artifacts (target/ or build/libs/) |

## Example Workflows

### Pull Request Checks

```yaml
name: PR Checks
on: pull_request

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Build with composite action
        uses: ./.github/actions/composite-build-action
        with:
          java-version: '17'
          build-tool: 'maven'
          cache-key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: target/*.jar
```

## Best Practices

1. **Use cache-key**: Always provide a cache key to improve build performance
2. **Pin versions**: Specify exact Java versions for reproducible builds
3. **Multiple environments**: Use this action consistently across dev, staging, and production builds

## License

MIT
