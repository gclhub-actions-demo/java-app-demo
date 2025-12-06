# GitHub Actions CI/CD Reference Implementation - Agent Prompt

## Project Overview

Create a comprehensive reference implementation demonstrating GitHub Actions best practices for CI/CD workflows, security hardening, and organizational governance. The project should serve as a production-ready example for enterprise teams adopting GitHub Actions.

## Core Application Requirements

### Java Application
- Build a moderately complex Java application (Spring Boot recommended)
- Application should include:
  - RESTful API endpoints (at least 3-4 resources)
  - Service layer with business logic
  - Data persistence layer (can use in-memory H2 database)
  - Input validation and error handling
  - Configuration management (application.properties/yml)
  - Logging framework integration
- Include unit tests with JUnit 5 and Mockito
- Include integration tests
- Target Java 17 or 21 LTS
- Use Maven or Gradle as the build tool
- Application should be containerizable (include Dockerfile)

## GitHub Actions CI Workflows

### Workflow Triggers to Demonstrate
Create workflows that showcase various trigger types:

1. **Pull Request Workflow** (`pr-checks.yml`)
   - Trigger on: `pull_request` (opened, synchronize, reopened)
   - Trigger on: `pull_request_target` for external contributions (with security considerations documented)
   - Target branches: `main`, `develop`
   - Jobs: linting, build, unit tests, integration tests
   - Include path filters to optimize workflow execution

2. **Push/Merge Workflow** (`ci-main.yml`)
   - Trigger on: `push` to `main` branch
   - Jobs: full test suite, build artifact, create container image
   - Upload artifacts for downstream consumption

3. **Scheduled Workflow** (`scheduled-security-scan.yml`)
   - Trigger on: `schedule` (cron expression for nightly runs)
   - Jobs: dependency scanning, SAST scanning, license compliance check

4. **Manual Workflow** (`manual-release.yml`)
   - Trigger on: `workflow_dispatch` with inputs
   - Allow manual triggering with version number input
   - Create release artifacts and GitHub release

5. **Workflow Call (Reusable)** (`reusable-build.yml`)
   - Trigger on: `workflow_call`
   - Parameterized inputs for flexibility
   - Demonstrate how to create reusable workflows

### CI Pipeline Jobs

Implement comprehensive CI checks:

1. **Code Linting**
   - Java linting with Checkstyle or PMD
   - Fail on style violations (configurable)
   - Upload results as artifacts

2. **Build Job**
   - Compile Java application
   - Run Maven/Gradle build
   - Cache dependencies for performance
   - Build Docker image
   - Matrix strategy for multiple Java versions (17, 21)

3. **Testing Jobs**
   - Unit tests with coverage reporting
   - Integration tests
   - Upload test results and coverage reports
   - Generate JaCoCo coverage reports
   - Publish test results to GitHub UI

4. **Security Scanning**
   - SAST scanning (CodeQL)
   - Dependency scanning (Dependabot or Snyk)
   - Container image scanning
   - Secret scanning validation

## Organization-Level Required Workflows

### Centralized Security Repository

Create a separate repository structure: `.github-workflows` (or similar name) that contains:

1. **Required Security Workflow** (`required-security-scan.yml`)
   - Organization-level security scanning
   - CodeQL analysis
   - License compliance checking
   - SBOM generation
   - Must run on all repositories

2. **Required Dependency Check** (`required-dependency-check.yml`)
   - Vulnerability scanning for dependencies
   - Auto-create issues for critical vulnerabilities
   - Block PRs with critical vulnerabilities (optional configuration)

### Documentation for Required Workflows

Include comprehensive documentation covering:
- How to create the `.github` repository in the organization
- Setting up organization rulesets to enforce required workflows
- Configuration options for requiring workflows on specific repository types
- Exemption processes for specific repositories if needed
- Troubleshooting common issues with required workflows

## Custom Actions

### Composite Action (`composite-build-action`)

Create a composite action in `.github/actions/composite-build-action/`:
- Purpose: Standardized build process with caching and artifact upload
- Inputs: Java version, build tool, cache key
- Steps: Setup Java, restore cache, build, save cache, upload artifact
- Include comprehensive README with usage examples

### JavaScript Action (`javascript-label-action`)

Create a JavaScript action in `.github/actions/javascript-label-action/`:
- Purpose: Automatically label PRs based on files changed
- Implementation: Node.js with @actions/core and @actions/github
- Inputs: Label mapping configuration (JSON)
- Include: action.yml, index.js, package.json, README
- Build and commit the dist/ folder (compiled with ncc)

### Custom Actions Documentation

Include detailed documentation covering:
- **Separate Repository Best Practice**: Explain why custom actions should typically be in separate repositories
- **Repository Structure**: Recommended structure for action repositories
- **Versioning Strategy**: Using tags and releases for action versions
- **Private Action Access**: How to use GitHub App installation tokens to access private action repositories
  - Create documentation with step-by-step guide for:
    - Creating a GitHub App with appropriate permissions
    - Installing the app on the organization
    - Generating installation tokens in workflows
    - Using tokens to check out private action repositories
  - Include example workflow snippet:
    ```yaml
    - name: Generate GitHub App Token
      id: generate-token
      uses: actions/create-github-app-token@v1
      with:
        app-id: ${{ secrets.APP_ID }}
        private-key: ${{ secrets.APP_PRIVATE_KEY }}
    
    - name: Use Private Action
      uses: organization/private-action@v1
      with:
        github-token: ${{ steps.generate-token.outputs.token }}
    ```

## Mock CD (Continuous Delivery) Pipeline

### Azure Deployment Workflow (`deploy-azure.yml`)

Create a realistic deployment workflow showcasing:

1. **Deployment Jobs per Environment**
   - QA Environment: Auto-deploy on merge to `develop`
   - Staging Environment: Auto-deploy on merge to `main`
   - Production Environment: Manual approval required

2. **Azure Integration** (mock implementation with documentation)
   - Use Azure Login action
   - Deploy to Azure Container Apps or Azure App Service
   - Include infrastructure deployment (Azure Bicep or Terraform)
   - Use Azure Key Vault for secrets management
   - Include health check validation after deployment

3. **Environment-Specific Configuration**
   - Different environment variables per environment
   - Different Azure resource groups per environment
   - Different service connection/authentication per environment

### GitHub Environments Documentation

Create comprehensive documentation for:

1. **Environment Setup**
   - Creating QA, Staging, and Production environments in GitHub
   - Environment-specific secrets configuration
   - Environment-specific variables configuration

2. **Deployment Protection Rules**
   - Required reviewers for production environment (minimum 2 reviewers recommended)
   - Wait timer before deployment (e.g., 5 minutes)
   - Allowed branches for deployment (e.g., only `main` can deploy to production)
   - Environment-specific deployment branches configuration

3. **Azure Service Connections**
   - Setting up Azure Service Principal for GitHub Actions
   - Federated identity credentials (OIDC) setup for Azure
   - Storing Azure credentials as environment secrets
   - Least privilege access configuration

4. **Production Protection Best Practices**
   - Branch protection rules for `main` branch
   - Required status checks before merge
   - Required pull request reviews
   - Dismiss stale approvals
   - Restrict who can push to `main`
   - Require signed commits (optional)
   - Enable deployment frequency limits

## Organization and Repository Security Hardening

### Actions Policy Documentation

Create detailed documentation covering:

1. **Organization-Level Actions Policies**
   - Restricting actions to verified creators only
   - Allowing specific actions with explicit allowlist:
     - `actions/*` (GitHub-owned actions)
     - `azure/*` (Azure-owned actions)
     - `organization-name/*` (Internal actions)
   - Blocking specific actions patterns
   - Disabling local (`.github/workflows`) actions if desired

2. **Workflow Permission Restrictions**
   - Default `GITHUB_TOKEN` permissions set to read-only
   - Explicit permissions declaration in workflows
   - Principle of least privilege for token scopes
   - Example workflow with minimal permissions:
     ```yaml
     permissions:
       contents: read
       pull-requests: write
     ```

3. **Runner Group Policies**
   - Organization-level runner groups
   - Repository access restrictions for runner groups
   - Self-hosted runner security considerations

4. **Secret Management**
   - Organization-level secrets vs repository secrets
   - Environment secrets for deployment credentials
   - Secret scanning enablement
   - Rotation policies for secrets
   - Using Azure Key Vault or HashiCorp Vault for secret management

5. **Repository-Level Settings**
   - Fork pull request workflows from outside collaborators
   - Require approval for first-time contributors
   - Token permissions for workflows
   - Allow GitHub Actions to create pull requests
   - Workflow artifact and log retention policies

### Security Scanning Configuration

Include configuration files for:
- **CodeQL**: `.github/codeql-config.yml` with custom queries
- **Dependabot**: `.github/dependabot.yml` for automated dependency updates
- **Secret Scanning**: Push protection enablement documentation

## Project Documentation Structure

Create the following documentation files:

1. **README.md** - Project overview and quick start guide
2. **docs/SETUP.md** - Initial setup and prerequisites
3. **docs/WORKFLOWS.md** - Detailed explanation of each workflow
4. **docs/CUSTOM_ACTIONS.md** - Guide to creating and using custom actions
5. **docs/DEPLOYMENT.md** - Azure deployment setup and configuration
6. **docs/ENVIRONMENTS.md** - GitHub Environments configuration guide
7. **docs/SECURITY_HARDENING.md** - Comprehensive security hardening guide
8. **docs/ORGANIZATION_POLICIES.md** - Organization-level policies and governance
9. **docs/REQUIRED_WORKFLOWS.md** - Setting up centralized required workflows
10. **docs/TROUBLESHOOTING.md** - Common issues and solutions

## Additional Requirements

1. **Code Quality**
   - Include `.gitignore` for Java/Maven/Gradle
   - Include `.editorconfig` for consistent coding style
   - Include `checkstyle.xml` or `pmd.xml` for linting rules

2. **Workflow Best Practices**
   - Use concurrency groups to cancel in-progress workflows
   - Implement proper error handling and job dependencies
   - Use matrix strategies where applicable
   - Cache dependencies for faster builds
   - Use environment files for setting output variables
   - Implement proper artifact retention policies

3. **Monitoring and Observability**
   - Include workflow status badges in README
   - Document how to monitor workflow runs
   - Include examples of workflow notifications (Slack, Teams, email)

4. **Examples and Templates**
   - Include pull request template (`.github/PULL_REQUEST_TEMPLATE.md`)
   - Include issue templates (`.github/ISSUE_TEMPLATE/`)
   - Include contributing guidelines (`CONTRIBUTING.md`)

## Implementation Instructions for Copilot Agent

Please implement this reference repository with the following approach:

1. Start by creating the Java Spring Boot application with appropriate structure
2. Add comprehensive tests (unit and integration)
3. Create the GitHub Actions workflows incrementally, testing each one
4. Implement the custom actions (composite and JavaScript)
5. Create all documentation files with practical examples
6. Include inline comments in workflows explaining each step
7. Ensure all file paths and references are correct
8. Make the repository production-ready and enterprise-grade

The final deliverable should be a complete, working reference implementation that can be cloned and used immediately by development teams, with clear documentation for customization and extension.

## Success Criteria

- [ ] Java application builds and runs successfully
- [ ] All tests pass
- [ ] All workflows are syntactically correct and functional
- [ ] Custom actions work as expected
- [ ] Documentation is clear, comprehensive, and accurate
- [ ] Security best practices are implemented throughout
- [ ] Example configurations are provided for all recommended features
- [ ] Repository serves as a production-ready template for enterprise use

---

**Note**: This is a comprehensive prompt. The agent should implement all components systematically, ensuring each part is complete and tested before moving to the next. The focus should be on quality, clarity, and real-world applicability.
