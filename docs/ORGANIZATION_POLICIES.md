# Organization Policies and Governance

This document outlines recommended organization-level policies for securing and standardizing GitHub Actions usage.

## Actions Usage Policies

### Restricting Actions

Configure organization-wide action policies to control which actions can be used.

#### Policy Options

**Option 1: Allow All Actions (Not Recommended)**
- Any action from GitHub Marketplace can be used
- Highest flexibility, lowest security

**Option 2: Allow Verified Creators Only (Recommended for Most)**
- Actions from GitHub, verified creators, and your organization
- Good balance of security and flexibility

**Option 3: Allow Specific Actions (Highest Security)**
- Explicit allowlist of approved actions
- Highest security, requires maintenance

### Configuration Steps

1. Navigate to Organization Settings
2. Go to Actions → General
3. Under "Actions permissions", select policy
4. Configure allowed actions

### Recommended Allowlist

```
# GitHub-owned actions
actions/*

# Azure actions
azure/*

# Common trusted actions
docker/build-push-action@*
docker/login-action@*
docker/setup-buildx-action@*
github/codeql-action/*
anchore/scan-action@*

# Your organization's actions
your-org/*
```

### Wildcard Patterns

- `owner/*` - All actions from owner
- `owner/repo@*` - All versions of specific action
- `owner/repo@v1` - Specific version only

## Workflow Permissions

### Default Token Permissions

Set GITHUB_TOKEN to read-only by default:

**Configuration:**
1. Organization Settings → Actions → General
2. Under "Workflow permissions"
3. Select "Read repository contents and packages permissions"
4. Check "Allow GitHub Actions to create and approve pull requests" (if needed)

### Explicit Permissions in Workflows

Always declare required permissions explicitly:

```yaml
permissions:
  contents: read        # Read code
  pull-requests: write  # Comment on PRs
  checks: write         # Publish test results
  packages: write       # Publish packages
  id-token: write       # OIDC authentication
```

### Permission Reference

| Permission | Read | Write | Use Cases |
|------------|------|-------|-----------|
| `actions` | View workflow runs | Cancel runs | Workflow management |
| `checks` | View checks | Create check runs | Test reporting |
| `contents` | Read code | Push commits, tags | Code changes, releases |
| `deployments` | View deployments | Create deployments | Deployment tracking |
| `id-token` | N/A | Request OIDC token | Azure/AWS auth |
| `issues` | View issues | Create/edit issues | Issue management |
| `packages` | Download packages | Publish packages | Package registry |
| `pull-requests` | View PRs | Create/edit PRs | PR automation |
| `security-events` | View alerts | Upload SARIF | Security scanning |

## Repository Settings

### Required Settings for All Repositories

#### Actions Configuration

**Settings → Actions → General:**
- ✅ Allow all actions and reusable workflows (with allowlist)
- ✅ Require approval for all outside collaborators
- ✅ Allow GitHub Actions to create and approve pull requests (if using automation)

**Settings → Actions → General → Workflow permissions:**
- ✅ Read repository contents and packages permissions (default)
- Explicit permissions required in workflows

#### Branch Protection Rules

**For main/master branch:**
- ✅ Require pull request before merging
- ✅ Require approvals (minimum 1-2)
- ✅ Dismiss stale approvals
- ✅ Require review from code owners
- ✅ Require status checks to pass
  - Add required workflows
  - Add required tests
- ✅ Require branches to be up to date
- ✅ Require conversation resolution
- ✅ Include administrators (recommended)
- ✅ Restrict who can push (optional)
- ✅ Allow force pushes: Disabled
- ✅ Allow deletions: Disabled

#### Security Settings

**Settings → Security:**
- ✅ Enable Dependabot alerts
- ✅ Enable Dependabot security updates
- ✅ Enable Dependabot version updates
- ✅ Enable code scanning (CodeQL)
- ✅ Enable secret scanning
- ✅ Enable push protection for secrets

### Dependabot Configuration

Create `.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "security-team"
    labels:
      - "dependencies"
      - "automated"
    
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "monthly"
    reviewers:
      - "devops-team"
```

## Secret Management

### Secret Hierarchy

1. **Organization Secrets** - Available to all repos
   - Azure credentials
   - Cloud service principals
   - External service tokens

2. **Repository Secrets** - Repository-specific
   - Repository-specific API keys
   - Deployment credentials

3. **Environment Secrets** - Environment-specific
   - Production database credentials
   - Environment-specific API keys

### Secret Naming Convention

```
# Format: [SCOPE]_[SERVICE]_[PURPOSE]
AZURE_CLIENT_ID
AZURE_TENANT_ID
AWS_ACCESS_KEY_ID
GITHUB_APP_PRIVATE_KEY
PROD_DATABASE_URL
```

### Secret Rotation Policy

- **Critical Secrets** (Production): Rotate every 30 days
- **High-Priority Secrets**: Rotate every 90 days
- **Standard Secrets**: Rotate every 180 days
- Rotate immediately if compromised

### Secret Management Best Practices

1. **Never log secrets**: Use `::add-mask::` if needed
2. **Use secret scanning**: Enable push protection
3. **Limit secret access**: Use environment secrets
4. **Prefer OIDC**: Use keyless authentication when possible
5. **Document secrets**: Maintain secret inventory
6. **Use secret managers**: Azure Key Vault, AWS Secrets Manager, HashiCorp Vault

### OIDC Authentication (Recommended)

Replace long-lived credentials with OIDC tokens:

```yaml
- name: Azure Login
  uses: azure/login@v1
  with:
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

**Setup:**
1. Create Azure AD App Registration
2. Configure federated credentials
3. Grant required permissions
4. Store app ID and tenant ID as secrets

## Runner Management

### Self-Hosted Runners

#### Security Considerations

**Risks:**
- Code execution on your infrastructure
- Potential secret exposure
- Maintenance overhead

**Best Practices:**
- ✅ Use ephemeral runners (auto-destroyed after job)
- ✅ Isolate runners in dedicated infrastructure
- ✅ Don't use for public repositories
- ✅ Implement network restrictions
- ✅ Regular security updates
- ✅ Monitor runner activity

#### Runner Groups

Create runner groups for access control:

**Configuration:**
1. Organization Settings → Actions → Runner groups
2. Create group (e.g., "Production Runners")
3. Add runners to group
4. Restrict repository access

**Recommended Groups:**
- **Production Runners**: Critical deployments only
- **Staging Runners**: Pre-production testing
- **Development Runners**: General development
- **Public Runners**: Open source projects (if needed)

### GitHub-Hosted Runners

**Advantages:**
- ✅ No maintenance required
- ✅ Clean environment for each job
- ✅ Multiple OS options
- ✅ Automatically updated

**Limitations:**
- ❌ Limited to public endpoints
- ❌ No persistent storage
- ❌ Resource limitations
- ❌ Cost per minute

## Required Workflows

### Organization-Level Requirements

Use rulesets to enforce required workflows:

**Configuration:**
1. Create `.github` repository in organization
2. Add required workflows to `workflows/` directory
3. Create repository ruleset
4. Add "Require workflows to pass" rule

**Recommended Required Workflows:**
- Security scanning (CodeQL, dependency check)
- License compliance checking
- SBOM generation
- Secret scanning validation

See [Required Workflows Guide](REQUIRED_WORKFLOWS.md) for details.

## Audit and Compliance

### Audit Logging

**Organization Settings → Audit log:**
- Review workflow runs
- Monitor permission changes
- Track secret access
- Identify security events

### Compliance Requirements

**For regulated industries:**
- ✅ Enable audit log streaming
- ✅ Require signed commits
- ✅ Implement approval workflows
- ✅ Maintain workflow run history
- ✅ Document change management process

### Monitoring

**Key Metrics to Track:**
- Workflow success/failure rates
- Average workflow duration
- Security scan findings
- Dependency vulnerabilities
- Secret rotation compliance

## Access Control

### Team-Based Permissions

**Organization Settings → Teams:**
- **Admin Team**: Full repository access
- **Security Team**: Security settings, required workflows
- **DevOps Team**: Actions, workflows, runners
- **Developer Team**: Code, pull requests
- **Read-Only Team**: View access only

### Repository Roles

| Role | Access Level | Use Case |
|------|--------------|----------|
| Read | View code | External contributors |
| Triage | Manage issues/PRs | Support team |
| Write | Push code | Developers |
| Maintain | Manage settings | Tech leads |
| Admin | Full control | Repository owners |

### Custom Repository Roles

Create organization-specific roles:

**Example: "Release Manager"**
- Read code
- Create releases
- Manage deployments
- Cannot merge PRs

## Enforcement Strategy

### Phase 1: Assessment (Week 1-2)
- Audit current state
- Identify gaps
- Document exceptions

### Phase 2: Policy Development (Week 3-4)
- Define policies
- Get stakeholder approval
- Create documentation

### Phase 3: Pilot Program (Month 2)
- Apply to 2-3 repositories
- Gather feedback
- Refine policies

### Phase 4: Gradual Rollout (Month 3-4)
- Apply to non-critical repos
- Monitor and adjust
- Provide training

### Phase 5: Full Enforcement (Month 5-6)
- Apply to all repositories
- Strict enforcement
- Regular audits

## Training and Education

### Required Training Topics

1. **GitHub Actions Basics**
   - Workflow syntax
   - Security best practices
   - Debugging workflows

2. **Security Awareness**
   - Secret management
   - Secure coding practices
   - Incident response

3. **Compliance**
   - Organizational policies
   - Required workflows
   - Audit procedures

### Resources

- Internal documentation wiki
- Lunch-and-learn sessions
- Hands-on workshops
- Office hours for questions

## Policy Exceptions

### Request Process

1. Submit exception request to security team
2. Provide business justification
3. Propose compensating controls
4. Get approval from security and management
5. Document exception
6. Schedule periodic review

### Exception Documentation

```markdown
## Exception: Allow custom action from external org

**Repository**: java-app-demo
**Policy**: Actions allowlist
**Requested by**: John Doe
**Reason**: Required for specialized deployment
**Compensating Controls**: 
  - Pin to specific version
  - Code review of action
  - Monthly security review
**Approval**: Jane Smith (CISO)
**Expiration**: 2025-06-01
**Review Date**: 2025-05-01
```

## Continuous Improvement

### Quarterly Review

- Assess policy effectiveness
- Review exceptions
- Update based on new threats
- Incorporate feedback

### Metrics for Success

- Reduction in security incidents
- Workflow success rates
- Developer satisfaction
- Compliance audit results

## Additional Resources

- [GitHub Actions Security Hardening](SECURITY_HARDENING.md)
- [Required Workflows Setup](REQUIRED_WORKFLOWS.md)
- [Troubleshooting Guide](TROUBLESHOOTING.md)
- [GitHub Security Best Practices](https://docs.github.com/en/actions/security-guides)
