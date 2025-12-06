# Java App Demo - GitHub Actions CI/CD Reference Implementation

![Build Status](https://github.com/your-org/java-app-demo/workflows/CI%20Main%20Branch/badge.svg)
![Security Scan](https://github.com/your-org/java-app-demo/workflows/Scheduled%20Security%20Scan/badge.svg)

A comprehensive reference implementation demonstrating GitHub Actions best practices for CI/CD workflows, security hardening, and organizational governance.

## 🎯 Purpose

This repository serves as a production-ready example for enterprise teams adopting GitHub Actions, showcasing:

- **CI/CD Workflows**: Multiple workflow types with various triggers
- **Security Best Practices**: Automated security scanning and compliance checks
- **Custom Actions**: Reusable composite and JavaScript actions
- **Organization Governance**: Centralized required workflows and policies
- **Azure Deployment**: Multi-environment deployment strategies

## 🚀 Quick Start

### Prerequisites

- JDK 17 or 21
- Maven 3.8+
- Docker (for containerization)
- Azure CLI (for deployment)

### Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/java-app-demo.git
cd java-app-demo

# Build the application
mvn clean package

# Run tests
mvn test

# Run the application
mvn spring-boot:run

# Access the application
open http://localhost:8080/api/health
```

### Docker

```bash
# Build Docker image
docker build -t java-app-demo:latest .

# Run container
docker run -p 8080:8080 java-app-demo:latest

# Health check
curl http://localhost:8080/api/health
```

## 📋 Application Features

### REST API Endpoints

- **Customers**: `/api/customers` - CRUD operations for customer management
- **Products**: `/api/products` - Product catalog management
- **Orders**: `/api/orders` - Order processing and tracking
- **Health**: `/api/health` - Application health status

### API Documentation

Access interactive API documentation:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api-docs`

## 🔄 CI/CD Workflows

### Workflow Overview

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [PR Checks](.github/workflows/pr-checks.yml) | Pull Request | Linting, building, testing, security scanning |
| [CI Main](.github/workflows/ci-main.yml) | Push to main | Build artifacts, create Docker images |
| [Security Scan](.github/workflows/scheduled-security-scan.yml) | Schedule (nightly) | CodeQL, dependency scanning, license checks |
| [Manual Release](.github/workflows/manual-release.yml) | Manual trigger | Version release and artifact creation |
| [Deploy Azure](.github/workflows/deploy-azure.yml) | Push/Manual | Multi-environment Azure deployment |

### Workflow Triggers Demonstrated

- ✅ `pull_request` - PR validation
- ✅ `push` - Continuous integration on main branch
- ✅ `schedule` - Automated security scans
- ✅ `workflow_dispatch` - Manual releases
- ✅ `workflow_call` - Reusable workflows

## 🛠️ Custom Actions

### Composite Action

**Location**: `.github/actions/composite-build-action/`

Standardized build process with caching and artifact management.

```yaml
- uses: ./.github/actions/composite-build-action
  with:
    java-version: '17'
    build-tool: 'maven'
```

[📖 Composite Action Documentation](.github/actions/composite-build-action/README.md)

### JavaScript Action

**Location**: `.github/actions/javascript-label-action/`

Automatically labels PRs based on changed files.

```yaml
- uses: ./.github/actions/javascript-label-action
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

[📖 JavaScript Action Documentation](.github/actions/javascript-label-action/README.md)

## 🏢 Organization-Level Features

### Centralized Required Workflows

Located in `centralized-workflows/` directory. These should be deployed to your organization's `.github` repository.

- **Required Security Scan**: CodeQL, secret scanning, SBOM generation
- **Required Dependency Check**: Vulnerability scanning, license compliance

[📖 Setup Guide](centralized-workflows/SETUP.md)

### Organization Policies

See documentation for configuring:
- Actions allowlist policies
- Workflow permission restrictions
- Required workflows via rulesets
- Secret management strategies

[📖 Organization Policies](docs/ORGANIZATION_POLICIES.md)

## 🔒 Security Features

### Implemented Security Measures

- ✅ CodeQL security analysis
- ✅ Dependency vulnerability scanning
- ✅ Secret scanning (TruffleHog)
- ✅ SBOM (Software Bill of Materials) generation
- ✅ Container image scanning
- ✅ License compliance checking

### Workflow Security

- Minimal permissions (principle of least privilege)
- OIDC authentication for Azure
- Secrets management via Azure Key Vault
- Signed commits support

[📖 Security Hardening Guide](docs/SECURITY_HARDENING.md)

## ☁️ Azure Deployment

### Environments

| Environment | Branch | Approval | URL |
|-------------|--------|----------|-----|
| QA | develop | None | java-app-demo-qa.azurewebsites.net |
| Staging | main | None | java-app-demo-staging.azurewebsites.net |
| Production | Manual | Required | java-app-demo.azurewebsites.net |

### Deployment Features

- Multi-environment configuration
- Blue-green deployment strategy
- Automated health checks
- Azure Key Vault integration
- Infrastructure as Code (Bicep templates)

[📖 Deployment Guide](docs/DEPLOYMENT.md)
[📖 Environment Configuration](docs/ENVIRONMENTS.md)

## 📚 Documentation

### Complete Documentation Index

- [Setup Guide](docs/SETUP.md) - Initial setup and prerequisites
- [Workflows Documentation](docs/WORKFLOWS.md) - Detailed workflow explanations
- [Custom Actions Guide](docs/CUSTOM_ACTIONS.md) - Creating and using custom actions
- [Deployment Guide](docs/DEPLOYMENT.md) - Azure deployment configuration
- [Environments Guide](docs/ENVIRONMENTS.md) - GitHub Environments setup
- [Security Hardening](docs/SECURITY_HARDENING.md) - Security best practices
- [Organization Policies](docs/ORGANIZATION_POLICIES.md) - Governance and policies
- [Required Workflows](docs/REQUIRED_WORKFLOWS.md) - Centralized workflow setup
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions

## 🏗️ Architecture

### Technology Stack

- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Build Tool**: Maven 3.9
- **Database**: H2 (in-memory)
- **Testing**: JUnit 5, Mockito
- **Containerization**: Docker
- **Cloud Platform**: Azure Container Apps
- **CI/CD**: GitHub Actions

### Project Structure

```
java-app-demo/
├── .github/
│   ├── actions/              # Custom actions
│   └── workflows/            # CI/CD workflows
├── centralized-workflows/    # Organization workflows
├── docs/                     # Documentation
├── src/
│   ├── main/
│   │   ├── java/            # Application code
│   │   └── resources/       # Configuration files
│   └── test/                # Tests
├── Dockerfile               # Container definition
├── pom.xml                  # Maven configuration
└── README.md               # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean verify
open target/site/jacoco/index.html
```

## 📊 Code Quality

- **Code Coverage**: Minimum 70% (JaCoCo)
- **Linting**: Checkstyle (Google Java Style)
- **Static Analysis**: PMD, SpotBugs
- **Security Scanning**: OWASP Dependency Check

## 🔍 Monitoring

### Health Endpoints

- Application health: `/api/health`
- Database status: Included in health endpoint
- Build version: Included in health response

### Metrics

In production environments, integrate with:
- Azure Application Insights
- Prometheus/Grafana
- ELK Stack

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- GitHub Actions team for excellent CI/CD platform
- Spring Boot team for the framework
- Open source security tools (CodeQL, OWASP, etc.)

## 📞 Support

For questions or issues:
1. Check the [documentation](docs/)
2. Review [troubleshooting guide](docs/TROUBLESHOOTING.md)
3. Open an issue on GitHub
4. Contact the DevOps team

## 🗺️ Roadmap

- [ ] Add Kubernetes deployment examples
- [ ] Implement GitOps with ArgoCD
- [ ] Add performance testing workflows
- [ ] Implement chaos engineering tests
- [ ] Add multi-cloud deployment examples

---

**Note**: This is a reference implementation. Adapt the workflows and configurations to your organization's specific requirements and security policies.
