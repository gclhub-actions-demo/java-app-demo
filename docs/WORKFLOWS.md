# GitHub Actions Workflows Documentation

This document provides detailed information about all GitHub Actions workflows in this repository.

## Overview

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| PR Checks | `pr-checks.yml` | Pull Request | Validate code quality and tests |
| CI Main | `ci-main.yml` | Push to main | Build and package for release |
| Security Scan | `scheduled-security-scan.yml` | Schedule/Manual | Security analysis |
| Manual Release | `manual-release.yml` | Manual | Create releases |
| Deploy Azure | `deploy-azure.yml` | Push/Manual | Multi-environment deployment |
| Reusable Build | `reusable-build.yml` | Workflow Call | Shared build logic |

## Pull Request Checks (`pr-checks.yml`)

### Purpose
Comprehensive validation of pull requests before merging.

### Triggers
```yaml
on:
  pull_request:
    branches: [main, develop]
    types: [opened, synchronize, reopened]
```

### Jobs

#### 1. Lint
- Runs Checkstyle validation
- Uploads linting results as artifacts
- Fails on style violations

#### 2. Build
- Matrix strategy: Java 17 and 21
- Compiles application
- Caches dependencies for performance
- Validates across multiple Java versions

#### 3. Unit Tests
- Executes JUnit tests
- Generates JaCoCo coverage report (minimum 70%)
- Publishes test results to GitHub UI
- Uploads coverage artifacts

#### 4. Integration Tests
- Runs integration tests with `mvn verify`
- Tests full application stack
- Uploads test results

#### 5. Security Scan
- OWASP Dependency Check
- Identifies vulnerable dependencies
- Uploads security reports

#### 6. Auto Label
- Uses custom JavaScript action
- Labels PR based on changed files
- Automates PR organization

### Concurrency Control
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```
Cancels outdated workflow runs when new commits are pushed.

### Permissions
```yaml
permissions:
  contents: read
  pull-requests: write
  checks: write
```
Follows principle of least privilege.

## CI Main Branch (`ci-main.yml`)

### Purpose
Build production-ready artifacts when code is merged to main.

### Triggers
```yaml
on:
  push:
    branches: [main]
```

### Jobs

#### 1. Build and Test
- Full test suite execution
- Coverage report generation
- JAR artifact creation
- 30-day artifact retention

#### 2. Build Docker Image
- Multi-stage Docker build
- Image tagged with commit SHA
- Exports image as artifact
- Uses composite build action

#### 3. Create Release Draft
- Auto-generates release drafts
- Attaches JAR artifacts
- Includes commit information
- Ready for manual approval

### Artifact Management
- **JAR Files**: 30-day retention
- **Docker Images**: 7-day retention
- **Coverage Reports**: 30-day retention

## Scheduled Security Scan (`scheduled-security-scan.yml`)

### Purpose
Nightly security scanning to detect new vulnerabilities.

### Triggers
```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC daily
  workflow_dispatch:      # Manual trigger option
```

### Jobs

#### 1. CodeQL Analysis
- Static application security testing (SAST)
- Security and quality queries
- Multi-language support
- Results published to Security tab

#### 2. Dependency Scan
- OWASP Dependency Check
- Scans for known vulnerabilities (CVEs)
- Checks both active and retired vulnerabilities
- Fails on critical/high severity issues

#### 3. License Compliance
- Generates dependency tree
- Creates third-party license report
- Identifies license conflicts
- Uploads compliance reports

#### 4. Create Security Issue
- Runs if vulnerabilities found
- Auto-creates GitHub issue
- Tags with 'security' label
- Assigns to security team

### Security Thresholds
```yaml
CRITICAL > 0: FAIL
HIGH > 5: FAIL
MEDIUM: WARN
```

## Manual Release (`manual-release.yml`)

### Purpose
Create versioned releases with manual control.

### Triggers
```yaml
on:
  workflow_dispatch:
    inputs:
      version: string  # e.g., "1.0.0"
      environment: choice [staging, production]
      create-github-release: boolean
```

### Jobs

#### 1. Validate Version
- Ensures semantic versioning (X.Y.Z)
- Prevents invalid version formats
- Fails fast on validation errors

#### 2. Build Release
- Updates pom.xml version
- Builds release artifacts
- Runs full test suite
- 90-day artifact retention

#### 3. Build and Push Docker
- Authenticates to GitHub Container Registry
- Builds multi-platform image
- Tags with version and 'latest'
- Pushes to registry

#### 4. Create GitHub Release
- Generates release notes
- Attaches artifacts
- Creates Git tag
- Publishes release

### Usage Example
```bash
# Via GitHub UI: Actions → Manual Release → Run workflow
# Inputs:
#   version: 1.2.3
#   environment: production
#   create-github-release: true
```

## Deploy Azure (`deploy-azure.yml`)

### Purpose
Multi-environment deployment to Azure infrastructure.

### Triggers
```yaml
on:
  push:
    branches: [develop, main]  # Auto-deploy
  workflow_dispatch:            # Manual deploy
```

### Environments

| Environment | Auto-Deploy | Approval | Branch |
|-------------|-------------|----------|--------|
| QA | Yes | No | develop |
| Staging | Yes | No | main |
| Production | Manual only | Required | N/A |

### Jobs

#### 1. Build
- Compiles application
- Runs tests
- Creates JAR artifact

#### 2. Build Container
- Downloads JAR
- Builds Docker image
- Exports as artifact

#### 3. Deploy QA
- Auto-deploys from develop branch
- Uses OIDC authentication
- Runs health checks
- No approval required

#### 4. Deploy Staging
- Auto-deploys from main branch
- Runs smoke tests
- Validates deployment
- No approval required

#### 5. Deploy Production
- Manual trigger only
- Requires approval (configured in Environment)
- Retrieves secrets from Azure Key Vault
- Implements blue-green deployment
- Comprehensive health checks
- Gradual traffic shifting

### Azure Authentication
Uses OIDC (OpenID Connect) for secure, keyless authentication:

```yaml
- uses: azure/login@v1
  with:
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

### Deployment Strategies

**QA & Staging**: Direct deployment
**Production**: Blue-green with traffic shifting

## Reusable Build Workflow (`reusable-build.yml`)

### Purpose
Shared build logic for consistency across workflows.

### Triggers
```yaml
on:
  workflow_call:
    inputs:
      java-version: string
      build-tool: string
      run-tests: boolean
```

### Usage Example
```yaml
jobs:
  build:
    uses: ./.github/workflows/reusable-build.yml
    with:
      java-version: '17'
      build-tool: 'maven'
      run-tests: true
```

## Workflow Best Practices

### 1. Caching
All workflows cache dependencies:
```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'maven'
```

### 2. Concurrency Control
Prevents resource waste:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

### 3. Conditional Execution
```yaml
if: github.ref == 'refs/heads/main'
```

### 4. Matrix Strategy
Test across multiple versions:
```yaml
strategy:
  matrix:
    java-version: [17, 21]
```

### 5. Artifact Management
Appropriate retention periods:
- Development: 7 days
- Production: 30-90 days

## Monitoring Workflows

### View Workflow Runs
```
Repository → Actions → Select workflow → View runs
```

### Workflow Badges
Add to README.md:
```markdown
![CI Status](https://github.com/org/repo/workflows/CI%20Main/badge.svg)
```

### Notifications
Configure in repository settings:
- Settings → Notifications
- Enable email/Slack notifications
- Configure for specific events

## Troubleshooting

### Workflow Fails on Cache
```yaml
# Add fallback restore keys
restore-keys: |
  ${{ runner.os }}-maven-
```

### Permission Denied
Check workflow permissions:
```yaml
permissions:
  contents: write  # Needed for releases
```

### Secrets Not Available
Verify in Settings → Secrets and variables → Actions

### Matrix Build Failures
Use `fail-fast: false` to see all results:
```yaml
strategy:
  fail-fast: false
  matrix:
    java-version: [17, 21]
```

## Customization

### Adding New Workflows
1. Create `.github/workflows/new-workflow.yml`
2. Define triggers and jobs
3. Test with workflow_dispatch
4. Add documentation

### Modifying Existing Workflows
1. Create feature branch
2. Modify workflow file
3. Test in PR
4. Merge to main

### Environment-Specific Configuration
Use GitHub Environments:
1. Settings → Environments
2. Add environment variables
3. Configure protection rules
4. Reference in workflows

## Security Considerations

### Token Permissions
Always use minimal permissions:
```yaml
permissions:
  contents: read
```

### Secret Management
- Never log secrets
- Use GitHub Secrets or Azure Key Vault
- Rotate secrets regularly

### Third-Party Actions
- Pin to specific versions
- Use verified creators only
- Review action code regularly

## Performance Optimization

### Cache Everything
- Dependencies (Maven, npm)
- Build outputs
- Docker layers

### Parallel Execution
```yaml
jobs:
  test-1:
    runs-on: ubuntu-latest
  test-2:
    runs-on: ubuntu-latest
```

### Path Filters
Skip workflows when irrelevant files change:
```yaml
on:
  push:
    paths-ignore:
      - '**.md'
      - 'docs/**'
```

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax Reference](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
- [Security Hardening Guide](SECURITY_HARDENING.md)
- [Custom Actions Guide](CUSTOM_ACTIONS.md)
