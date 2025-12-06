# Required Workflows Implementation Guide

This guide explains how to implement organization-level required workflows that automatically run across all repositories in your GitHub organization.

## Table of Contents

- [Overview](#overview)
- [Centralized Workflows Repository](#centralized-workflows-repository)
- [Creating Required Workflows](#creating-required-workflows)
- [Configuring Organization Rulesets](#configuring-organization-rulesets)
- [Example Required Workflows](#example-required-workflows)
- [Testing and Validation](#testing-and-validation)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

## Overview

Required workflows are organization-level workflows that automatically run in specified repositories. They enable centralized enforcement of:

- Security scanning
- Compliance checks
- Code quality standards
- License validation
- Dependency auditing

### Benefits

- **Centralized Management**: Update workflows once, apply everywhere
- **Consistent Enforcement**: All repositories follow same standards
- **Reduced Duplication**: No need to copy workflows to each repo
- **Automatic Compliance**: New repositories automatically inherit policies
- **Audit Trail**: Centralized logs for all compliance checks

### Requirements

- GitHub Enterprise Cloud or GitHub Enterprise Server 3.7+
- Organization admin access
- Public or internal repository for centralized workflows

## Centralized Workflows Repository

### Repository Structure

Create a dedicated repository: `organization/.github-workflows-centralized`

```
.github-workflows-centralized/
├── .github/
│   └── workflows/
│       ├── required-security-scan.yml      # CodeQL + secret scanning
│       ├── required-dependency-check.yml   # OWASP dependency scanning
│       ├── required-license-check.yml      # License compliance
│       ├── required-sbom-generation.yml    # SBOM creation
│       └── required-code-quality.yml       # Linting and style checks
├── README.md
└── SETUP.md
```

### Creating the Repository

```bash
# Create repository
gh repo create your-org/.github-workflows-centralized \
  --internal \
  --description "Organization-level required workflows" \
  --gitignore "Node" \
  --license "MIT"

# Clone and set up structure
git clone https://github.com/your-org/.github-workflows-centralized
cd .github-workflows-centralized
mkdir -p .github/workflows
```

## Creating Required Workflows

Required workflows must be reusable workflows with specific characteristics.

### Required Workflow Template

```yaml
# .github/workflows/required-security-scan.yml
name: Required Security Scan

on:
  workflow_call:
    inputs:
      # Optional inputs for customization
      severity-threshold:
        description: 'Minimum severity to fail build'
        type: string
        default: 'high'
        required: false
      
      language:
        description: 'Programming language for CodeQL'
        type: string
        default: 'java'
        required: false

permissions:
  contents: read
  security-events: write
  actions: read

jobs:
  codeql:
    name: CodeQL Security Analysis
    runs-on: ubuntu-latest
    timeout-minutes: 30
    
    steps:
      - name: Checkout repository
        uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11  # v4.1.1
      
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: ${{ inputs.language }}
          queries: +security-and-quality
      
      - name: Autobuild
        uses: github/codeql-action/autobuild@v3
      
      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
        with:
          category: "/language:${{ inputs.language }}"

  dependency-scan:
    name: Dependency Vulnerability Scan
    runs-on: ubuntu-latest
    timeout-minutes: 20
    
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: ${{ inputs.severity-threshold }}
          exit-code: '1'
      
      - name: Upload Trivy results
        if: always()
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'
          category: 'dependency-scan'

  secret-scan:
    name: Secret Scanning
    runs-on: ubuntu-latest
    timeout-minutes: 15
    
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for secret scanning
      
      - name: TruffleHog Secret Scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: ${{ github.event.repository.default_branch }}
          head: HEAD
          extra_args: --only-verified
```

### Key Characteristics

1. **Uses `workflow_call` trigger**: Required for reusable workflows
2. **Minimal required inputs**: Maximizes compatibility
3. **Explicit permissions**: Security best practice
4. **Timeout limits**: Prevents hanging workflows
5. **Pinned actions**: Security hardening

## Configuring Organization Rulesets

Organization rulesets enforce required workflows across repositories.

### Step 1: Create Organization Ruleset

```
Organization Settings → Rules → Rulesets → New ruleset → New branch ruleset
```

**Basic Configuration:**
- **Ruleset Name**: "Required Security Workflows"
- **Enforcement Status**: Active
- **Bypass List**: Add admin users/teams who can bypass (use sparingly)

### Step 2: Define Target Repositories

Choose which repositories must comply:

**Option A: All Repositories**
```json
{
  "target": "all"
}
```

**Option B: Specific Repositories**
```json
{
  "target": "repositories",
  "repository_ids": [12345, 67890]
}
```

**Option C: Dynamic Targeting (Properties)**
```json
{
  "target": "dynamic",
  "repository_property": {
    "name": "compliance_level",
    "values": ["high", "critical"]
  }
}
```

### Step 3: Configure Branch Targeting

Apply to specific branches:

```json
{
  "branch_targeting": {
    "patterns": [
      {
        "pattern": "main",
        "type": "exact"
      },
      {
        "pattern": "release/*",
        "type": "prefix"
      }
    ]
  }
}
```

### Step 4: Add Required Workflows

Add workflows that must complete successfully:

```json
{
  "required_workflows": [
    {
      "path": "your-org/.github-workflows-centralized/.github/workflows/required-security-scan.yml@main",
      "ref": "main"
    },
    {
      "path": "your-org/.github-workflows-centralized/.github/workflows/required-dependency-check.yml@main",
      "ref": "main"
    }
  ]
}
```

### Using GitHub CLI

```bash
# Create ruleset with required workflows
gh api orgs/:org/rulesets -X POST \
  --input - <<'EOF'
{
  "name": "Required Security Workflows",
  "target": "branch",
  "enforcement": "active",
  "bypass_actors": [
    {
      "actor_id": 5,
      "actor_type": "RepositoryRole",
      "bypass_mode": "always"
    }
  ],
  "conditions": {
    "ref_name": {
      "include": [
        "refs/heads/main",
        "refs/heads/release/*"
      ],
      "exclude": []
    }
  },
  "rules": [
    {
      "type": "required_workflows",
      "parameters": {
        "required_workflows": [
          {
            "repository_id": 123456,
            "path": ".github/workflows/required-security-scan.yml",
            "ref": "main"
          }
        ]
      }
    }
  ]
}
EOF
```

### Using GitHub API with Script

```bash
#!/bin/bash
# scripts/configure-required-workflows.sh

ORG="your-org"
WORKFLOWS_REPO="your-org/.github-workflows-centralized"

# Get workflows repo ID
REPO_ID=$(gh api repos/$WORKFLOWS_REPO --jq '.id')

# Define required workflows
REQUIRED_WORKFLOWS=$(cat <<EOF
[
  {
    "repository_id": $REPO_ID,
    "path": ".github/workflows/required-security-scan.yml",
    "ref": "main"
  },
  {
    "repository_id": $REPO_ID,
    "path": ".github/workflows/required-dependency-check.yml",
    "ref": "main"
  }
]
EOF
)

# Create ruleset
gh api orgs/$ORG/rulesets -X POST \
  --input - <<EOF
{
  "name": "Required Security Workflows",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": {
      "include": ["refs/heads/main", "refs/heads/develop"],
      "exclude": []
    },
    "repository_name": {
      "include": ["*"],
      "exclude": ["*.archived", "test-*"],
      "protected": true
    }
  },
  "rules": [
    {
      "type": "required_workflows",
      "parameters": {
        "required_workflows": $REQUIRED_WORKFLOWS
      }
    }
  ]
}
EOF

echo "Required workflows configured successfully"
```

## Example Required Workflows

### 1. Security Scanning

```yaml
# required-security-scan.yml
name: Required Security Scan

on:
  workflow_call:
    inputs:
      languages:
        description: 'CodeQL languages (comma-separated)'
        type: string
        default: 'java'
        required: false

permissions:
  contents: read
  security-events: write

jobs:
  codeql:
    name: CodeQL Analysis
    runs-on: ubuntu-latest
    strategy:
      matrix:
        language: ${{ fromJSON(format('["{0}"]', inputs.languages)) }}
    steps:
      - uses: actions/checkout@v4
      
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: ${{ matrix.language }}
      
      - name: Autobuild
        uses: github/codeql-action/autobuild@v3
      
      - name: Analyze
        uses: github/codeql-action/analyze@v3
```

### 2. Dependency Checking

```yaml
# required-dependency-check.yml
name: Required Dependency Check

on:
  workflow_call:
    inputs:
      fail-on-cvss:
        description: 'Minimum CVSS score to fail (0-10)'
        type: number
        default: 7
        required: false

permissions:
  contents: read
  issues: write

jobs:
  dependency-check:
    name: OWASP Dependency Check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: ${{ github.repository }}
          path: '.'
          format: 'HTML'
          args: >
            --failOnCVSS ${{ inputs.fail-on-cvss }}
            --enableRetired
      
      - name: Upload results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: reports/
      
      - name: Create issue on failure
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.create({
              owner: context.repo.owner,
              repo: context.repo.repo,
              title: '🔒 Dependency vulnerabilities detected',
              body: `Dependency check failed. CVSS threshold: ${{ inputs.fail-on-cvss }}\n\nSee workflow run: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`,
              labels: ['security', 'dependencies']
            });
```

### 3. License Compliance

```yaml
# required-license-check.yml
name: Required License Compliance

on:
  workflow_call:
    inputs:
      allowed-licenses:
        description: 'Allowed licenses (comma-separated)'
        type: string
        default: 'MIT,Apache-2.0,BSD-3-Clause'
        required: false

permissions:
  contents: read
  pull-requests: write

jobs:
  license-check:
    name: License Compliance
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install license checker
        run: npm install -g license-checker
      
      - name: Check licenses
        run: |
          ALLOWED="${{ inputs.allowed-licenses }}"
          license-checker \
            --onlyAllow "$ALLOWED" \
            --production \
            --json > licenses.json
          
          if [ $? -ne 0 ]; then
            echo "::error::Unapproved licenses detected"
            cat licenses.json
            exit 1
          fi
      
      - name: Upload license report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: license-report
          path: licenses.json
```

### 4. SBOM Generation

```yaml
# required-sbom-generation.yml
name: Required SBOM Generation

on:
  workflow_call:
    inputs:
      format:
        description: 'SBOM format (spdx or cyclonedx)'
        type: string
        default: 'spdx'
        required: false

permissions:
  contents: write
  packages: write

jobs:
  generate-sbom:
    name: Generate SBOM
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Generate SBOM with Syft
        uses: anchore/sbom-action@v0
        with:
          format: ${{ inputs.format }}
          output-file: sbom.${{ inputs.format }}.json
          upload-artifact: true
          upload-release-assets: true
      
      - name: Scan SBOM with Grype
        uses: anchore/scan-action@v3
        with:
          sbom: sbom.${{ inputs.format }}.json
          fail-build: true
          severity-cutoff: high
      
      - name: Commit SBOM to repository
        if: github.ref == 'refs/heads/main'
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add sbom.${{ inputs.format }}.json
          git commit -m "chore: update SBOM [skip ci]" || echo "No changes"
          git push
```

## Testing and Validation

### Test in a Single Repository First

Before rolling out organization-wide:

1. Create test repository
2. Configure ruleset targeting only test repo
3. Trigger workflows and verify execution
4. Check for false positives
5. Adjust thresholds and configurations

### Manual Trigger for Testing

Add manual trigger to required workflows:

```yaml
on:
  workflow_call:
    # ...
  workflow_dispatch:  # Enable manual testing
    inputs:
      test-mode:
        description: 'Test mode (will not fail build)'
        type: boolean
        default: true
```

### Validation Script

```bash
#!/bin/bash
# scripts/validate-required-workflows.sh

ORG="your-org"
RULESET_ID="12345"

# Get ruleset configuration
echo "Fetching ruleset configuration..."
gh api orgs/$ORG/rulesets/$RULESET_ID > ruleset.json

# Extract required workflows
echo "Required workflows:"
jq -r '.rules[] | select(.type=="required_workflows") | .parameters.required_workflows[] | "\(.repository_id):\(.path)@\(.ref)"' ruleset.json

# Test each workflow
jq -r '.rules[] | select(.type=="required_workflows") | .parameters.required_workflows[] | @base64' ruleset.json | \
while read workflow; do
  WORKFLOW_DATA=$(echo "$workflow" | base64 -d)
  REPO_ID=$(echo "$WORKFLOW_DATA" | jq -r '.repository_id')
  PATH=$(echo "$WORKFLOW_DATA" | jq -r '.path')
  REF=$(echo "$WORKFLOW_DATA" | jq -r '.ref')
  
  # Get repository name from ID
  REPO=$(gh api /repositories/$REPO_ID --jq '.full_name')
  
  echo "Testing: $REPO/$PATH@$REF"
  
  # Verify workflow file exists
  if gh api repos/$REPO/contents/$PATH --jq '.name' > /dev/null 2>&1; then
    echo "  ✓ Workflow file exists"
  else
    echo "  ✗ Workflow file not found"
    exit 1
  fi
  
  # Validate workflow syntax (basic check)
  gh api repos/$REPO/contents/$PATH --jq '.content' | base64 -d | \
    grep -q "workflow_call" && echo "  ✓ Uses workflow_call trigger" || \
    echo "  ✗ Missing workflow_call trigger"
done

echo "Validation complete"
```

## Troubleshooting

### Common Issues

#### 1. Workflow Not Running

**Symptoms**: Required workflow doesn't execute on push/PR

**Solutions**:
- Verify ruleset is "Active" not "Evaluate"
- Check branch targeting patterns match actual branches
- Confirm repository is included in ruleset target
- Ensure workflows repo is accessible (public/internal)

```bash
# Check ruleset status
gh api orgs/:org/rulesets/:ruleset-id --jq '.enforcement'

# List repositories affected by ruleset
gh api orgs/:org/rulesets/:ruleset-id/repositories
```

#### 2. Workflow Fails in Target Repos

**Symptoms**: Required workflow works in centralized repo but fails elsewhere

**Solutions**:
- Check language/framework compatibility
- Verify required secrets are available
- Ensure adequate timeout limits
- Test with `workflow_dispatch` trigger

```yaml
# Add debugging
- name: Debug repository context
  run: |
    echo "Repository: ${{ github.repository }}"
    echo "Branch: ${{ github.ref }}"
    echo "Language: ${{ inputs.language }}"
    ls -la
```

#### 3. Permission Errors

**Symptoms**: `Resource not accessible by integration`

**Solutions**:
- Add required permissions to workflow
- Check organization actions permissions
- Verify GITHUB_TOKEN has necessary scopes

```yaml
# Explicit permissions
permissions:
  contents: read
  security-events: write
  issues: write
  pull-requests: write
```

#### 4. Cannot Bypass Required Workflow

**Symptoms**: Even admins cannot merge without workflow passing

**Solutions**:
- Add admin to bypass list in ruleset
- Create organization-level bypass permission
- Use emergency merge process

```bash
# Add bypass actor to ruleset
gh api orgs/:org/rulesets/:ruleset-id -X PATCH \
  --input - <<'EOF'
{
  "bypass_actors": [
    {
      "actor_id": 5,
      "actor_type": "RepositoryRole",
      "bypass_mode": "always"
    }
  ]
}
EOF
```

### Debugging Commands

```bash
# View all organization rulesets
gh api orgs/:org/rulesets --jq '.[] | {id, name, enforcement, target}'

# Get specific ruleset details
gh api orgs/:org/rulesets/:ruleset-id

# List repositories targeted by ruleset
gh api orgs/:org/rulesets/:ruleset-id/repositories --jq '.[] | .name'

# Check workflow runs across organization
gh api orgs/:org/actions/runs --jq '.workflow_runs[] | select(.name=="Required Security Scan") | {repo: .repository.name, status, conclusion}'
```

## Best Practices

### 1. Start Small

- Begin with 1-2 non-blocking workflows
- Test in pilot repositories
- Gradually expand scope
- Monitor failure rates

### 2. Make Workflows Flexible

```yaml
# Provide customization options
inputs:
  skip-tests:
    description: 'Skip test execution'
    type: boolean
    default: false
  
  severity-threshold:
    type: string
    default: 'high'
  
  timeout-minutes:
    type: number
    default: 30
```

### 3. Handle Different Languages

```yaml
# Detect language automatically
- name: Detect languages
  id: detect
  run: |
    if [ -f "pom.xml" ] || [ -f "build.gradle" ]; then
      echo "language=java" >> $GITHUB_OUTPUT
    elif [ -f "package.json" ]; then
      echo "language=javascript" >> $GITHUB_OUTPUT
    elif [ -f "requirements.txt" ] || [ -f "setup.py" ]; then
      echo "language=python" >> $GITHUB_OUTPUT
    fi

- name: Run language-specific checks
  if: steps.detect.outputs.language == 'java'
  run: mvn verify
```

### 4. Provide Clear Failure Messages

```yaml
- name: Security scan failed
  if: failure()
  run: |
    echo "::error::Security vulnerabilities detected"
    echo "Please review the security scan results and address all HIGH/CRITICAL findings."
    echo "For assistance, contact the security team: security@example.com"
    echo "Documentation: https://wiki.example.com/security-policies"
```

### 5. Version Control Workflows

```yaml
# Reference specific versions
required_workflows: [
  {
    "path": ".github/workflows/required-security-scan.yml",
    "ref": "v1.2.0"  # Use tags, not branches
  }
]
```

### 6. Monitor and Iterate

```bash
# Weekly report on required workflow compliance
#!/bin/bash

echo "Required Workflow Compliance Report - $(date)"
echo "=============================================="

gh api orgs/:org/repos --paginate --jq '.[].name' | \
while read repo; do
  RUNS=$(gh api repos/:org/$repo/actions/runs \
    --jq '.workflow_runs[] | select(.name=="Required Security Scan") | .conclusion')
  
  TOTAL=$(echo "$RUNS" | wc -l)
  PASSED=$(echo "$RUNS" | grep -c "success")
  
  if [ $TOTAL -gt 0 ]; then
    RATE=$(( PASSED * 100 / TOTAL ))
    echo "$repo: $RATE% pass rate ($PASSED/$TOTAL)"
  fi
done
```

## Additional Resources

- [GitHub Rulesets Documentation](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-organization-settings/managing-rulesets-for-repositories-in-your-organization)
- [Required Workflows](https://docs.github.com/en/actions/using-workflows/required-workflows)
- [Reusable Workflows](https://docs.github.com/en/actions/using-workflows/reusing-workflows)
- [Organization Policies](ORGANIZATION_POLICIES.md)
- [Security Hardening](SECURITY_HARDENING.md)
