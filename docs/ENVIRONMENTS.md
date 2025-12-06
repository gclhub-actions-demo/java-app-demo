# GitHub Environments Configuration Guide

This guide covers setting up and managing GitHub environments for QA, Staging, and Production deployments with deployment protection rules.

## Table of Contents

- [Overview](#overview)
- [Environment Strategy](#environment-strategy)
- [Creating Environments](#creating-environments)
- [Deployment Protection Rules](#deployment-protection-rules)
- [Environment Secrets](#environment-secrets)
- [Environment Variables](#environment-variables)
- [Branch Protection Integration](#branch-protection-integration)
- [Approval Workflows](#approval-workflows)
- [Monitoring Deployments](#monitoring-deployments)
- [Best Practices](#best-practices)

## Overview

GitHub Environments provide deployment protection and configuration for workflows. They enable:

- **Deployment approvals**: Require human approval before deploying
- **Wait timers**: Delay deployments automatically
- **Environment secrets**: Isolate sensitive data per environment
- **Deployment branches**: Control which branches can deploy to each environment
- **Custom protection rules**: GitHub Apps can add custom deployment gates

## Environment Strategy

### Three-Tier Environment Model

```
┌─────────────────────────────────────────────────────────────┐
│                        Git Branches                         │
└──────────┬───────────────────┬──────────────────┬───────────┘
           │                   │                  │
       develop              main             release/*
           │                   │                  │
           ▼                   ▼                  ▼
    ┌──────────┐        ┌──────────┐      ┌──────────┐
    │    QA    │        │ Staging  │      │Production│
    │          │        │          │      │          │
    │ Auto-    │        │ Auto-    │      │ Manual   │
    │ Deploy   │        │ Deploy   │      │ Approval │
    │          │        │          │      │ Required │
    └──────────┘        └──────────┘      └──────────┘
         │                   │                  │
         └───────────────────┴──────────────────┘
                            │
                            ▼
                    Azure Container Apps
```

### Environment Characteristics

| Environment | Purpose | Branch | Approval | Auto-Deploy | Data |
|-------------|---------|--------|----------|-------------|------|
| **QA** | Feature testing, integration testing | `develop` | None | Yes | Test data, may reset |
| **Staging** | Pre-production validation, UAT | `main` | Optional | Yes | Production-like data |
| **Production** | Live customer-facing application | `main`, `release/*` | Required | No | Real customer data |

## Creating Environments

### Using GitHub UI

1. Navigate to repository **Settings** → **Environments**
2. Click **New environment**
3. Enter environment name (e.g., `production`)
4. Click **Configure environment**

### Using GitHub CLI

```bash
# Create environments
gh api repos/:owner/:repo/environments/qa -X PUT
gh api repos/:owner/:repo/environments/staging -X PUT
gh api repos/:owner/:repo/environments/production -X PUT
```

### Using GitHub API

```bash
OWNER="your-org"
REPO="java-app-demo"
TOKEN="your-github-token"

# Create QA environment
curl -X PUT \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$OWNER/$REPO/environments/qa"

# Create Staging environment
curl -X PUT \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$OWNER/$REPO/environments/staging"

# Create Production environment
curl -X PUT \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$OWNER/$REPO/environments/production"
```

## Deployment Protection Rules

### QA Environment

**No protection rules** - Deploys automatically from `develop` branch.

```json
{
  "wait_timer": 0,
  "reviewers": [],
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
```

**Configuration:**
```bash
gh api repos/:owner/:repo/environments/qa -X PUT -f deployment_branch_policy[custom_branch_policies]=true

# Allow only develop branch
gh api repos/:owner/:repo/environments/qa/deployment-branch-policies -X POST -f name="develop"
```

### Staging Environment

**Optional wait timer** - Deploys automatically from `main` branch after a brief delay.

```json
{
  "wait_timer": 5,
  "reviewers": [],
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
```

**Configuration:**
```bash
# Set 5-minute wait timer
gh api repos/:owner/:repo/environments/staging -X PUT -f wait_timer=5

# Allow main branch
gh api repos/:owner/:repo/environments/staging/deployment-branch-policies -X POST -f name="main"
```

### Production Environment

**Required approvals + wait timer** - Requires manual approval before deployment.

```json
{
  "wait_timer": 10,
  "reviewers": [
    {
      "type": "User",
      "reviewer": {"id": 12345}
    },
    {
      "type": "Team", 
      "reviewer": {"id": 67890}
    }
  ],
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  },
  "prevent_self_review": true
}
```

**Configuration:**

```bash
# Get user IDs
USER_ID=$(gh api users/deployment-manager --jq '.id')
TEAM_ID=$(gh api orgs/:org/teams/platform-team --jq '.id')

# Configure production environment with approvers
gh api repos/:owner/:repo/environments/production -X PUT \
  --input - <<EOF
{
  "wait_timer": 10,
  "prevent_self_review": true,
  "reviewers": [
    {
      "type": "User",
      "id": $USER_ID
    },
    {
      "type": "Team",
      "id": $TEAM_ID
    }
  ],
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
EOF

# Allow main and release branches
gh api repos/:owner/:repo/environments/production/deployment-branch-policies -X POST -f name="main"
gh api repos/:owner/:repo/environments/production/deployment-branch-policies -X POST -f name="release/*"
```

### Deployment Branch Policies

#### Protected Branches Only

Restrict deployments to protected branches:

```bash
gh api repos/:owner/:repo/environments/production -X PUT \
  --input - <<EOF
{
  "deployment_branch_policy": {
    "protected_branches": true,
    "custom_branch_policies": false
  }
}
EOF
```

#### Custom Branch Patterns

Allow specific branches or patterns:

```bash
# Allow main branch
gh api repos/:owner/:repo/environments/production/deployment-branch-policies \
  -X POST -f name="main"

# Allow release branches (wildcard)
gh api repos/:owner/:repo/environments/production/deployment-branch-policies \
  -X POST -f name="release/*"

# List configured policies
gh api repos/:owner/:repo/environments/production/deployment-branch-policies
```

## Environment Secrets

Environment-specific secrets override repository secrets.

### Secret Hierarchy

```
Organization Secrets (lowest priority)
    ↓
Repository Secrets
    ↓
Environment Secrets (highest priority)
```

### Adding Secrets via GitHub CLI

```bash
# QA environment secrets
gh secret set AZURE_CLIENT_ID --env qa --body "qa-app-id"
gh secret set DATABASE_URL --env qa --body "jdbc:postgresql://qa-db..."

# Staging environment secrets
gh secret set AZURE_CLIENT_ID --env staging --body "staging-app-id"
gh secret set DATABASE_URL --env staging --body "jdbc:postgresql://staging-db..."

# Production environment secrets
gh secret set AZURE_CLIENT_ID --env production --body "prod-app-id"
gh secret set DATABASE_URL --env production --body "jdbc:postgresql://prod-db..."
gh secret set API_KEY --env production --body "prod-api-key-xyz"
```

### Adding Secrets via GitHub API

```bash
# Helper function to encrypt secret
encrypt_secret() {
  local secret_value=$1
  local public_key=$2
  
  # Use libsodium or similar
  echo "$secret_value" | base64
}

# Get environment public key
PUBLIC_KEY=$(gh api repos/:owner/:repo/environments/production/secrets/public-key --jq '.key')
KEY_ID=$(gh api repos/:owner/:repo/environments/production/secrets/public-key --jq '.key_id')

# Add encrypted secret
gh api repos/:owner/:repo/environments/production/secrets/DATABASE_URL -X PUT \
  --input - <<EOF
{
  "encrypted_value": "$(encrypt_secret 'jdbc:postgresql://...' '$PUBLIC_KEY')",
  "key_id": "$KEY_ID"
}
EOF
```

### Using Secrets in Workflows

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production  # Loads production environment secrets
    steps:
      - name: Deploy
        run: |
          echo "Database: ${{ secrets.DATABASE_URL }}"
          echo "Client ID: ${{ secrets.AZURE_CLIENT_ID }}"
```

## Environment Variables

Non-sensitive configuration that doesn't require encryption.

### Setting Variables

```bash
# Set environment variables
gh variable set LOG_LEVEL --env production --body "INFO"
gh variable set CACHE_TTL --env production --body "3600"
gh variable set ENVIRONMENT_NAME --env production --body "production"

# QA uses different settings
gh variable set LOG_LEVEL --env qa --body "DEBUG"
gh variable set CACHE_TTL --env qa --body "300"
```

### Using Variables in Workflows

```yaml
jobs:
  deploy:
    environment: production
    steps:
      - name: Configure app
        run: |
          echo "Log Level: ${{ vars.LOG_LEVEL }}"
          echo "Cache TTL: ${{ vars.CACHE_TTL }}"
          echo "Environment: ${{ vars.ENVIRONMENT_NAME }}"
```

## Branch Protection Integration

Combine environment protection with branch protection for defense in depth.

### Branch Protection Configuration

```bash
# Protect main branch
gh api repos/:owner/:repo/branches/main/protection -X PUT \
  --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["build", "test", "security-scan"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 2,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF
```

### Coordinated Protection

```
Pull Request → main
    ↓
  ✓ 2 code reviews required (branch protection)
  ✓ Status checks pass (branch protection)
  ✓ Code owner approval (branch protection)
    ↓
  Merge to main
    ↓
  Trigger deployment workflow
    ↓
  ✓ Wait 5 minutes (staging environment)
  ✓ Deploy to staging
    ↓
  ✓ Manual approval required (production environment)
  ✓ Deployment team approval (production environment)
  ✓ Wait 10 minutes (production environment)
  ✓ Deploy to production
```

## Approval Workflows

### Requesting Approval

When a workflow reaches a job with `environment: production`:

```yaml
jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment: production  # Triggers approval request
    steps:
      - name: Deploy
        run: echo "Deploying to production"
```

**What happens:**
1. Workflow pauses at the job
2. GitHub creates a deployment review request
3. Configured reviewers receive notification
4. Reviewers can approve or reject via UI or API

### Approving via GitHub UI

1. Navigate to **Actions** → Select workflow run
2. Click **Review deployments**
3. Select environment(s) to approve
4. Add comment (optional)
5. Click **Approve and deploy**

### Approving via GitHub CLI

```bash
# List pending deployments
gh api repos/:owner/:repo/actions/runs/:run-id/pending_deployments

# Approve deployment
gh api repos/:owner/:repo/actions/runs/:run-id/pending_deployments -X POST \
  --input - <<EOF
{
  "environment_ids": [123456],
  "state": "approved",
  "comment": "Approved by DevOps team"
}
EOF
```

### Rejecting Deployment

```bash
gh api repos/:owner/:repo/actions/runs/:run-id/pending_deployments -X POST \
  --input - <<EOF
{
  "environment_ids": [123456],
  "state": "rejected",
  "comment": "Failed smoke tests, needs investigation"
}
EOF
```

### Approval Best Practices

1. **Multiple Reviewers**: Require at least 2 approvers for production
2. **Prevent Self-Review**: Enable to ensure different person approves
3. **Team-Based**: Use teams rather than individuals for better coverage
4. **Documented Process**: Maintain runbook for approval criteria
5. **Audit Trail**: Always add meaningful comments to approvals

```json
{
  "reviewers": [
    {"type": "Team", "id": 67890}
  ],
  "prevent_self_review": true,
  "required_reviewers_count": 2
}
```

## Monitoring Deployments

### Deployment History

```bash
# View deployment history
gh api repos/:owner/:repo/deployments \
  --jq '.[] | {id, environment, created_at, creator: .creator.login}'

# Get deployment status
gh api repos/:owner/:repo/deployments/:deployment-id/statuses
```

### Deployment Events

Track deployments via webhook events:

```json
{
  "action": "created",
  "deployment": {
    "id": 123456,
    "environment": "production",
    "ref": "main",
    "sha": "abc123def456",
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

### Workflow Monitoring

```yaml
- name: Report deployment status
  if: always()
  uses: actions/github-script@v7
  with:
    script: |
      const deployment = await github.rest.repos.createDeploymentStatus({
        owner: context.repo.owner,
        repo: context.repo.repo,
        deployment_id: context.payload.deployment.id,
        state: '${{ job.status }}',
        log_url: `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`,
        description: 'Deployment ${{ job.status }}'
      });
```

### Metrics and Reporting

```bash
# Deployment frequency
gh api repos/:owner/:repo/deployments \
  --jq '[.[] | select(.environment=="production")] | group_by(.created_at | split("T")[0]) | map({date: .[0].created_at | split("T")[0], count: length})'

# Approval time (requires workflow tracking)
gh api repos/:owner/:repo/actions/runs \
  --jq '.workflow_runs[] | select(.name=="Deploy to Azure") | {run: .id, started: .created_at, completed: .updated_at}'
```

## Best Practices

### 1. Environment Naming

Use clear, consistent names:
- ✅ `production`, `staging`, `qa`, `development`
- ❌ `prod`, `stg`, `live`, `env-1`

### 2. Secret Management

```yaml
# ✅ Good: Use environment-specific secrets
environment: production
steps:
  - run: deploy.sh
    env:
      API_KEY: ${{ secrets.API_KEY }}  # production API key

# ❌ Bad: Hardcoded environment selection
steps:
  - run: deploy.sh
    env:
      API_KEY: ${{ secrets.PROD_API_KEY }}  # Bypasses environment protection
```

### 3. Deployment Branches

```bash
# ✅ Good: Strict branch policies per environment
# QA: develop
# Staging: main
# Production: main, release/*

# ❌ Bad: Allow any branch to deploy anywhere
# All environments: *
```

### 4. Progressive Deployment

Always deploy through environments in order:

```yaml
jobs:
  deploy-qa:
    environment: qa
    # Auto-deploy from develop
  
  deploy-staging:
    needs: deploy-qa
    environment: staging
    # Auto-deploy from main
  
  deploy-production:
    needs: deploy-staging
    environment: production
    # Manual approval required
```

### 5. Environment Parity

Keep environments as similar as possible:

| Aspect | Recommendation |
|--------|----------------|
| Infrastructure | Use same Azure resources type, different SKUs |
| Configuration | Same variables, different values |
| Data | Production-like data in staging |
| Secrets | Same structure, different credentials |

### 6. Documentation

Document environment-specific information:

```markdown
## Production Environment

- **Azure Subscription**: `prod-subscription-id`
- **Resource Group**: `rg-java-app-prod`
- **Container App**: `java-app-demo-prod`
- **Approvers**: @platform-team, @devops-leads
- **Deployment Windows**: Mon-Thu 10am-2pm EST
- **Rollback SLA**: 15 minutes
```

### 7. Testing Protection Rules

Test environment protection before production:

```yaml
# Test workflow for verifying environment config
name: Test Environment Protection

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to test'
        required: true
        type: choice
        options:
          - qa
          - staging
          - production

jobs:
  test-protection:
    environment: ${{ inputs.environment }}
    runs-on: ubuntu-latest
    steps:
      - run: echo "Successfully accessed ${{ inputs.environment }}"
```

### 8. Audit and Compliance

Enable audit logging for deployments:

```bash
# Query audit log
gh api orgs/:org/audit-log \
  --jq '.[] | select(.action=="deployment_protection_rule.create")'

# Track who approved what
gh api repos/:owner/:repo/actions/runs/:run-id/approvals
```

## Complete Configuration Example

### Script to Configure All Environments

```bash
#!/bin/bash
set -e

OWNER="your-org"
REPO="java-app-demo"

# Get IDs
PLATFORM_TEAM_ID=$(gh api orgs/$OWNER/teams/platform-team --jq '.id')
DEVOPS_TEAM_ID=$(gh api orgs/$OWNER/teams/devops --jq '.id')

echo "Configuring QA environment..."
gh api repos/$OWNER/$REPO/environments/qa -X PUT \
  --input - <<'EOF'
{
  "wait_timer": 0,
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
EOF

gh api repos/$OWNER/$REPO/environments/qa/deployment-branch-policies \
  -X POST -f name="develop"

echo "Configuring Staging environment..."
gh api repos/$OWNER/$REPO/environments/staging -X PUT \
  --input - <<'EOF'
{
  "wait_timer": 5,
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
EOF

gh api repos/$OWNER/$REPO/environments/staging/deployment-branch-policies \
  -X POST -f name="main"

echo "Configuring Production environment..."
gh api repos/$OWNER/$REPO/environments/production -X PUT \
  --input - <<EOF
{
  "wait_timer": 10,
  "prevent_self_review": true,
  "reviewers": [
    {
      "type": "Team",
      "id": $PLATFORM_TEAM_ID
    },
    {
      "type": "Team",
      "id": $DEVOPS_TEAM_ID
    }
  ],
  "deployment_branch_policy": {
    "protected_branches": false,
    "custom_branch_policies": true
  }
}
EOF

gh api repos/$OWNER/$REPO/environments/production/deployment-branch-policies \
  -X POST -f name="main"
gh api repos/$OWNER/$REPO/environments/production/deployment-branch-policies \
  -X POST -f name="release/*"

echo "Environment configuration complete!"
```

## Additional Resources

- [GitHub Environments Documentation](https://docs.github.com/en/actions/deployment/targeting-different-environments)
- [Deployment Protection Rules](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment#deployment-protection-rules)
- [Environment Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets#creating-encrypted-secrets-for-an-environment)
- [Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches)
