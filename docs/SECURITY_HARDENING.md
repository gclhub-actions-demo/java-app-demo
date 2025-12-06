# Security Hardening Guide for GitHub Actions

This guide provides comprehensive security hardening recommendations for GitHub Actions at both the organization and repository levels.

## Table of Contents

- [Organization-Level Security](#organization-level-security)
- [Repository-Level Security](#repository-level-security)
- [Workflow Security](#workflow-security)
- [Action Restrictions](#action-restrictions)
- [Secret Management](#secret-management)
- [Runner Security](#runner-security)
- [Audit and Compliance](#audit-and-compliance)
- [Security Scanning](#security-scanning)
- [Incident Response](#incident-response)

## Organization-Level Security

### 1. Actions Permissions Policy

Control which actions can be used across your organization.

#### Recommended Configuration: Allow Select Actions

```
Organization Settings → Actions → General → Actions permissions
```

**Settings:**
- ✅ **Allow select actions and reusable workflows**
- ☑️ Allow actions created by GitHub
- ☑️ Allow actions by Marketplace verified creators
- ☑️ Allow specified actions and reusable workflows

**Rationale**: Provides security while maintaining flexibility.

#### Implementation via API

```bash
# Set organization actions policy
gh api orgs/:org -X PATCH \
  --input - <<'EOF'
{
  "members_can_create_public_repositories": false,
  "members_can_create_private_repositories": true,
  "members_can_create_internal_repositories": true
}
EOF

# Configure allowed actions
gh api orgs/:org/actions/permissions -X PUT \
  --input - <<'EOF'
{
  "enabled_repositories": "all",
  "allowed_actions": "selected"
}
EOF

# Set allowed actions list
gh api orgs/:org/actions/permissions/selected-actions -X PUT \
  --input - <<'EOF'
{
  "github_owned_allowed": true,
  "verified_allowed": true,
  "patterns_allowed": [
    "actions/*",
    "azure/*",
    "docker/*",
    "your-org/*"
  ]
}
EOF
```

### 2. Approved Actions Allowlist

Maintain a curated list of approved actions.

#### Recommended Allowlist

```yaml
# GitHub-owned actions (always safe)
actions/checkout@v4
actions/setup-java@v4
actions/setup-node@v4
actions/cache@v4
actions/upload-artifact@v4
actions/download-artifact@v4
github/codeql-action/*

# Azure actions
azure/login@v1
azure/docker-login@v1
azure/webapps-deploy@v3

# Docker actions
docker/build-push-action@v5
docker/login-action@v3
docker/metadata-action@v5

# Security scanning
aquasecurity/trivy-action@master
anchore/sbom-action@v0
github/super-linter@v5

# Organization actions (your custom actions)
your-org/*

# Specific third-party actions (pinned to SHA)
codecov/codecov-action@54bcd8715eee62d40e33596ef5e8f0f48dbbccab  # v4.1.0
```

#### Wildcard Patterns

```yaml
# Allow all actions from organization
your-org/*

# Allow specific action families
actions/*           # All GitHub actions
azure/login@*      # Azure login, all versions

# Allow by path
*/setup-*          # All setup actions
```

### 3. Organization Secrets Policy

Configure secrets access and permissions.

```bash
# Set organization secret visibility
gh api orgs/:org/actions/secrets/SECRET_NAME -X PUT \
  --input - <<'EOF'
{
  "encrypted_value": "...",
  "visibility": "selected",
  "selected_repository_ids": [12345, 67890]
}
EOF
```

**Visibility Options:**
- `private`: Private repositories only
- `all`: All repositories
- `selected`: Specific repositories only (recommended)

### 4. Workflow Permissions

Set default permissions for `GITHUB_TOKEN`.

```
Organization Settings → Actions → General → Workflow permissions
```

**Recommended Settings:**
- ✅ **Read repository contents and packages permissions**
- ☑️ **Allow GitHub Actions to create and approve pull requests** (unchecked for security)

```bash
# Set via API
gh api orgs/:org/actions/permissions/workflow -X PUT \
  --input - <<'EOF'
{
  "default_workflow_permissions": "read",
  "can_approve_pull_request_reviews": false
}
EOF
```

### 5. Required Workflows

Enforce security scans and compliance checks across all repositories.

```
Organization Settings → Actions → General → Required workflows
```

**Recommended Required Workflows:**
- Security scanning (CodeQL, dependency check)
- License compliance
- Secret scanning
- SBOM generation

See [REQUIRED_WORKFLOWS.md](REQUIRED_WORKFLOWS.md) for implementation details.

### 6. Runner Groups

Isolate self-hosted runners by environment or security level.

```bash
# Create runner group
gh api orgs/:org/actions/runner-groups -X POST \
  --input - <<'EOF'
{
  "name": "production-runners",
  "visibility": "selected",
  "selected_repository_ids": [12345],
  "runners": [],
  "allows_public_repositories": false
}
EOF
```

**Security Considerations:**
- Separate runner groups for public vs. private repos
- Isolated runners for production deployments
- Dedicated runners for sensitive workloads

## Repository-Level Security

### 1. Actions Permissions

```
Repository Settings → Actions → General → Actions permissions
```

**Recommended:**
- ✅ **Allow select actions and reusable workflows**
- Inherit from organization or customize per repo

```bash
# Repository-specific allowlist
gh api repos/:owner/:repo/actions/permissions/selected-actions -X PUT \
  --input - <<'EOF'
{
  "github_owned_allowed": true,
  "verified_allowed": false,
  "patterns_allowed": [
    "actions/*",
    "your-org/*",
    "specific-vendor/action@v1.2.3"
  ]
}
EOF
```

### 2. Workflow Permissions

Minimize `GITHUB_TOKEN` permissions per repository.

```bash
gh api repos/:owner/:repo/actions/permissions/workflow -X PUT \
  --input - <<'EOF'
{
  "default_workflow_permissions": "read",
  "can_approve_pull_request_reviews": false
}
EOF
```

### 3. Fork PR Workflows

Control workflow execution for PRs from forks.

```
Repository Settings → Actions → General → Fork pull request workflows
```

**Recommended Settings:**
- ☑️ **Run workflows from fork pull requests** (checked, but with restrictions)
- ✅ **Require approval for first-time contributors** (checked)
- ✅ **Require approval for all outside collaborators** (checked)

```bash
gh api repos/:owner/:repo/actions/permissions -X PUT \
  --input - <<'EOF'
{
  "enabled": true,
  "allowed_actions": "selected"
}
EOF
```

**Security Note**: Workflows from forks do NOT have access to secrets.

### 4. Branch Protection Rules

Enforce security checks before merging.

```bash
gh api repos/:owner/:repo/branches/main/protection -X PUT \
  --input - <<'EOF'
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "build",
      "test", 
      "security-scan",
      "code-quality"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismissal_restrictions": {
      "users": [],
      "teams": ["security-team"]
    },
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true,
    "required_approving_review_count": 2
  },
  "restrictions": {
    "users": [],
    "teams": ["platform-team"],
    "apps": []
  },
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true,
  "lock_branch": false,
  "allow_fork_syncing": true
}
EOF
```

### 5. Rulesets (New Alternative to Branch Protection)

Modern alternative with more flexible policies.

```bash
# Create ruleset
gh api repos/:owner/:repo/rulesets -X POST \
  --input - <<'EOF'
{
  "name": "Main branch protection",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": {
      "include": ["refs/heads/main"],
      "exclude": []
    }
  },
  "rules": [
    {
      "type": "required_status_checks",
      "parameters": {
        "strict_required_status_checks_policy": true,
        "required_status_checks": [
          {
            "context": "security-scan",
            "integration_id": null
          },
          {
            "context": "build",
            "integration_id": null
          }
        ]
      }
    },
    {
      "type": "pull_request",
      "parameters": {
        "required_approving_review_count": 2,
        "dismiss_stale_reviews_on_push": true,
        "require_code_owner_review": true,
        "require_last_push_approval": false,
        "required_review_thread_resolution": true
      }
    },
    {
      "type": "required_deployments",
      "parameters": {
        "required_deployment_environments": ["staging"]
      }
    }
  ]
}
EOF
```

## Workflow Security

### 1. Explicit Permissions

Always declare minimum necessary permissions.

```yaml
# ✅ Good: Minimal permissions per job
name: CI

on: [push, pull_request]

permissions:
  contents: read  # Default for all jobs

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: mvn clean package

  upload-artifacts:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write  # Only for this job
    steps:
      - run: mvn deploy

# ❌ Bad: Overly permissive
permissions: write-all
```

### 2. Pin Actions to SHA

Pin third-party actions to full commit SHA.

```yaml
# ✅ Good: SHA pinned
- uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11  # v4.1.1

# ⚠️ Acceptable: Semantic version (for trusted sources)
- uses: actions/checkout@v4

# ❌ Bad: Mutable reference
- uses: actions/checkout@main
```

**Rationale**: Tags and branches can be moved; SHAs are immutable.

### 3. Validate Inputs

Always validate workflow inputs and parameters.

```yaml
# ✅ Good: Input validation
on:
  workflow_dispatch:
    inputs:
      environment:
        type: choice
        options: [qa, staging, production]
        required: true
      version:
        type: string
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Validate version format
        run: |
          if ! echo "${{ inputs.version }}" | grep -qE '^v[0-9]+\.[0-9]+\.[0-9]+$'; then
            echo "Invalid version format. Expected: vX.Y.Z"
            exit 1
          fi
      
      - name: Deploy
        run: deploy.sh "${{ inputs.environment }}" "${{ inputs.version }}"
```

### 4. Avoid Script Injection

Never interpolate user input directly into shell commands.

```yaml
# ❌ Dangerous: Script injection vulnerability
- name: Greet user
  run: echo "Hello ${{ github.event.issue.title }}"
  # If title is: "; rm -rf / #"
  # Executes: echo "Hello "; rm -rf / #"

# ✅ Safe: Use environment variables
- name: Greet user
  env:
    TITLE: ${{ github.event.issue.title }}
  run: echo "Hello $TITLE"

# ✅ Safe: Use intermediate step
- name: Set title
  id: set-title
  run: echo "title=${{ github.event.issue.title }}" >> $GITHUB_OUTPUT

- name: Greet user
  run: echo "Hello ${{ steps.set-title.outputs.title }}"
```

### 5. Secret Masking

Ensure secrets are masked in logs.

```yaml
# ✅ Good: Secrets automatically masked
- name: Login
  run: |
    echo "Logging in..."
    curl -H "Authorization: Bearer ${{ secrets.API_TOKEN }}" api.example.com

# ✅ Good: Mask custom secrets
- name: Get dynamic secret
  run: |
    SECRET=$(fetch-secret.sh)
    echo "::add-mask::$SECRET"
    echo "secret=$SECRET" >> $GITHUB_OUTPUT

# ❌ Bad: Secret exposed
- name: Debug
  run: echo ${{ secrets.API_TOKEN }}  # Visible in logs
```

### 6. Minimize Secrets Scope

Use environment secrets instead of repository secrets.

```yaml
# ✅ Good: Environment-scoped secrets
jobs:
  deploy:
    environment: production  # Only has access to production secrets
    steps:
      - run: deploy.sh
        env:
          API_KEY: ${{ secrets.API_KEY }}

# ❌ Bad: Repository-scoped secrets accessible everywhere
jobs:
  any-job:
    steps:
      - run: |
          echo ${{ secrets.PROD_API_KEY }}  # Accessible in any job
```

### 7. Restrict Pull Request Secrets

Use `pull_request_target` carefully or not at all.

```yaml
# ❌ Dangerous: Fork can access secrets
on: pull_request_target
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.pull_request.head.sha }}  # Checks out fork code
      - run: npm install && npm build  # Fork code runs with secrets

# ✅ Safe: Review fork changes, secrets not exposed
on: pull_request
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4  # Checks out fork
      - run: npm install && npm build  # No secrets available

# ✅ Safe: Approve fork PRs manually
on: pull_request_target
jobs:
  label:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
    steps:
      - uses: actions/labeler@v5  # Uses base repo code only
```

## Action Restrictions

### Creating Allowlist Policy

```bash
#!/bin/bash
# scripts/update-actions-allowlist.sh

ALLOWLIST=(
  # GitHub official
  "actions/*"
  "github/*"
  
  # Azure
  "azure/login@v1"
  "azure/webapps-deploy@v3"
  
  # Docker
  "docker/build-push-action@v5"
  "docker/login-action@v3"
  
  # Security
  "aquasecurity/trivy-action@master"
  "github/super-linter@v5"
  
  # Organization
  "your-org/*"
)

# Convert to JSON array
PATTERNS=$(printf '%s\n' "${ALLOWLIST[@]}" | jq -R . | jq -s .)

# Update organization policy
gh api orgs/:org/actions/permissions/selected-actions -X PUT \
  --input - <<EOF
{
  "github_owned_allowed": true,
  "verified_allowed": true,
  "patterns_allowed": $PATTERNS
}
EOF

echo "Actions allowlist updated successfully"
```

### Verifying Action Usage

```bash
# Find all actions used in workflows
find .github/workflows -name "*.yml" -o -name "*.yaml" | \
  xargs grep -h "uses:" | \
  sed 's/.*uses: //' | \
  sed 's/@.*//' | \
  sort -u

# Check against allowlist
find .github/workflows -name "*.yml" | \
  xargs grep -h "uses:" | \
  grep -v -E "(actions/|azure/|docker/|your-org/)" | \
  sed 's/.*uses: //' || echo "All actions approved"
```

### GitHub App for Action Approval

Custom GitHub App to enforce action policies:

```javascript
// app.js - GitHub App for action approval
module.exports = (app) => {
  app.on(['pull_request.opened', 'pull_request.synchronize'], async (context) => {
    const files = await context.octokit.pulls.listFiles(context.pullRequest());
    
    const workflowFiles = files.data.filter(f => 
      f.filename.startsWith('.github/workflows/')
    );
    
    for (const file of workflowFiles) {
      const content = await context.octokit.repos.getContent({
        ...context.repo(),
        path: file.filename
      });
      
      const yaml = Buffer.from(content.data.content, 'base64').toString();
      const actions = yaml.match(/uses: (.*)/g) || [];
      
      const allowedActions = [
        /^actions\//,
        /^azure\//,
        /^your-org\//
      ];
      
      for (const action of actions) {
        const actionName = action.replace('uses: ', '').trim();
        const allowed = allowedActions.some(pattern => 
          pattern.test(actionName)
        );
        
        if (!allowed) {
          await context.octokit.checks.create({
            ...context.repo(),
            name: 'Action Security Check',
            head_sha: context.payload.pull_request.head.sha,
            status: 'completed',
            conclusion: 'failure',
            output: {
              title: 'Unapproved action detected',
              summary: `Action ${actionName} is not in the approved allowlist.`,
              text: 'Please use only approved actions or request approval.'
            }
          });
          
          return;
        }
      }
    }
  });
};
```

## Secret Management

See also: [ORGANIZATION_POLICIES.md - Secret Management](ORGANIZATION_POLICIES.md#secret-management)

### 1. Secret Rotation Policy

```bash
# Script to rotate Azure service principal credentials
#!/bin/bash

APP_ID="your-app-id"
SECRET_NAME="AZURE_CLIENT_SECRET"

# Create new credential
NEW_SECRET=$(az ad app credential reset --id $APP_ID --query password -o tsv)

# Update in GitHub
gh secret set $SECRET_NAME --body "$NEW_SECRET"

echo "Secret rotated successfully"
```

**Rotation Schedule:**
- Service principals: Every 90 days
- API keys: Every 180 days
- Personal access tokens: Every 30 days

### 2. Use OIDC Instead of Secrets

Prefer OIDC for cloud authentication:

```yaml
# ✅ Good: OIDC (no long-lived secrets)
- name: Azure login
  uses: azure/login@v1
  with:
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

permissions:
  id-token: write  # Required for OIDC

# ❌ Avoid: Long-lived service principal secret
- name: Azure login
  uses: azure/login@v1
  with:
    creds: ${{ secrets.AZURE_CREDENTIALS }}  # Contains long-lived password
```

### 3. External Secret Management

Integrate with external secret stores:

```yaml
- name: Get secrets from Vault
  uses: hashicorp/vault-action@v2
  with:
    url: https://vault.example.com
    token: ${{ secrets.VAULT_TOKEN }}
    secrets: |
      secret/data/production database_url | DATABASE_URL ;
      secret/data/production api_key | API_KEY
```

## Runner Security

### Self-Hosted Runner Security

**Never use self-hosted runners for public repositories** - they can execute arbitrary code from forks.

#### Secure Configuration

```bash
# Run runner as non-root user
sudo useradd -m -s /bin/bash github-runner
sudo -u github-runner ./config.sh --url https://github.com/org/repo --token TOKEN

# Use runner groups for isolation
./config.sh \
  --url https://github.com/org \
  --token TOKEN \
  --runnergroup production-runners \
  --labels production,secure

# Enable auto-updates
./run.sh --disableupdate false
```

#### Container Isolation

```dockerfile
# Dockerfile for ephemeral self-hosted runner
FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    curl \
    git \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m runner

USER runner
WORKDIR /home/runner

# Runner will be configured at container start
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
```

```bash
# entrypoint.sh
#!/bin/bash
set -e

# Register runner
./config.sh \
  --url "$GITHUB_URL" \
  --token "$RUNNER_TOKEN" \
  --ephemeral \
  --unattended

# Run once and exit
./run.sh

# Cleanup
./config.sh remove --token "$RUNNER_TOKEN"
```

## Audit and Compliance

### Enable Audit Logging

```bash
# Query audit log (Enterprise only)
gh api orgs/:org/audit-log \
  --jq '.[] | select(.action | startswith("workflows"))'

# Export audit logs
gh api orgs/:org/audit-log?per_page=100 > audit-$(date +%Y%m%d).json
```

### Monitor Security Events

```yaml
# .github/workflows/security-monitoring.yml
name: Security Monitoring

on:
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours

jobs:
  monitor:
    runs-on: ubuntu-latest
    steps:
      - name: Check for secret leaks
        run: |
          # Query recent workflow runs for potential secret exposure
          gh api repos/:owner/:repo/actions/runs \
            --jq '.workflow_runs[] | select(.conclusion=="failure") | .logs_url' | \
            while read log_url; do
              # Download and scan logs for patterns
              curl -H "Authorization: token ${{ secrets.GITHUB_TOKEN }}" "$log_url" | \
                grep -i "secret\|password\|token" || true
            done
```

## Security Scanning

### Required Security Checks

Enforce these checks in all workflows:

1. **CodeQL** - Semantic code analysis
2. **Dependency Scanning** - Known vulnerabilities
3. **Secret Scanning** - Hardcoded secrets
4. **SBOM Generation** - Software Bill of Materials
5. **Container Scanning** - Image vulnerabilities

### Example Comprehensive Security Workflow

```yaml
name: Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
  schedule:
    - cron: '0 0 * * 0'  # Weekly

permissions:
  contents: read
  security-events: write

jobs:
  codeql:
    uses: ./.github/workflows/reusable-codeql.yml
  
  dependency-scan:
    uses: ./.github/workflows/reusable-dependency-scan.yml
  
  secret-scan:
    uses: ./.github/workflows/reusable-secret-scan.yml
  
  container-scan:
    if: github.event_name != 'pull_request'
    uses: ./.github/workflows/reusable-container-scan.yml
```

## Incident Response

### Security Incident Playbook

1. **Detection**: Identify the security issue
2. **Containment**: Disable compromised workflows/secrets
3. **Investigation**: Determine scope of breach
4. **Remediation**: Fix vulnerabilities, rotate secrets
5. **Recovery**: Resume normal operations
6. **Review**: Post-mortem and improvements

### Emergency Response Actions

```bash
# Disable all actions in repository
gh api repos/:owner/:repo/actions/permissions -X PUT \
  --input - <<'EOF'
{
  "enabled": false
}
EOF

# Rotate all secrets immediately
for secret in $(gh secret list --json name -q '.[].name'); do
  echo "Rotate: $secret"
  # Rotate each secret based on type
done

# Disable self-hosted runners
gh api repos/:owner/:repo/actions/runners | \
  jq -r '.runners[] | .id' | \
  while read runner_id; do
    gh api repos/:owner/:repo/actions/runners/$runner_id -X DELETE
  done
```

## Security Checklist

Use this checklist for security reviews:

- [ ] Organization actions policy set to "Allow select actions"
- [ ] Approved actions allowlist maintained and documented
- [ ] Default workflow permissions set to read-only
- [ ] Required workflows enforcing security scans
- [ ] All secrets use environment-scoping where possible
- [ ] OIDC used instead of long-lived credentials
- [ ] Secret rotation policy defined and automated
- [ ] Branch protection rules enforced on main/production branches
- [ ] Required status checks include security scans
- [ ] Pull request reviews required (minimum 2)
- [ ] Self-hosted runners not used for public repos
- [ ] Self-hosted runners run in ephemeral containers
- [ ] Runner groups configured for environment isolation
- [ ] Audit logging enabled and monitored
- [ ] All workflows use explicit permissions
- [ ] Third-party actions pinned to SHA
- [ ] No script injection vulnerabilities in workflows
- [ ] Fork PR workflows require approval
- [ ] Environment protection rules configured
- [ ] Deployment approvals required for production
- [ ] Incident response playbook documented

## Additional Resources

- [GitHub Actions Security Guides](https://docs.github.com/en/actions/security-guides)
- [Security Hardening for GitHub Actions](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [OIDC with Cloud Providers](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments)
- [Managing GitHub Actions Policies](https://docs.github.com/en/organizations/managing-organization-settings)
