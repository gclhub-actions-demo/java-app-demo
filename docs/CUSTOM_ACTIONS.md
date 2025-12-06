# Custom Actions Guide

This document describes best practices for creating, managing, and using custom GitHub Actions in your organization.

## Table of Contents

- [Action Architecture](#action-architecture)
- [Repository Structure](#repository-structure)
- [Using Private Actions](#using-private-actions)
- [GitHub App Authentication](#github-app-authentication)
- [Action Types](#action-types)
- [Best Practices](#best-practices)
- [Versioning Strategy](#versioning-strategy)
- [Testing Actions](#testing-actions)
- [Distribution Methods](#distribution-methods)

## Action Architecture

### Why Separate Repositories?

Custom actions should be housed in their own dedicated repositories rather than stored in `.github/actions` within consuming repositories. This approach provides several benefits:

1. **Reusability**: Actions can be shared across multiple repositories and organizations
2. **Versioning**: Independent version control allows for semantic versioning and stable releases
3. **Access Control**: Granular permissions and access management per action
4. **Discoverability**: Easier to find and document in GitHub's action marketplace
5. **Testing**: Isolated testing environments without affecting consuming repositories
6. **Collaboration**: Separate issue tracking and pull request workflows per action

### Recommended Repository Naming

```
organization/action-<name>
organization/gha-<name>
organization/<name>-action
```

Examples:
- `acme-corp/action-build-java`
- `acme-corp/gha-security-scan`
- `acme-corp/docker-publish-action`

## Repository Structure

### Composite Action Repository

```
action-build-java/
├── action.yml                 # Action metadata and definition
├── README.md                  # Usage documentation
├── LICENSE                    # License file
├── CHANGELOG.md              # Version history
├── .github/
│   └── workflows/
│       ├── test.yml          # Action testing workflow
│       ├── release.yml       # Release automation
│       └── codeql.yml        # Security scanning
└── examples/
    └── basic-usage.yml       # Example workflow
```

### JavaScript Action Repository

```
javascript-label-action/
├── action.yml                # Action metadata
├── index.js                  # Main action code (or dist/index.js for compiled)
├── package.json              # Node dependencies
├── package-lock.json
├── README.md
├── LICENSE
├── CHANGELOG.md
├── .github/
│   └── workflows/
│       ├── test.yml
│       ├── check-dist.yml    # Verify dist/ is up to date
│       └── release.yml
├── src/                      # Source files (if using TypeScript)
│   └── main.ts
└── __tests__/               # Jest tests
    └── main.test.js
```

### Docker Action Repository

```
docker-build-action/
├── action.yml
├── Dockerfile
├── entrypoint.sh
├── README.md
├── LICENSE
├── CHANGELOG.md
└── .github/
    └── workflows/
        ├── test.yml
        └── publish.yml
```

## Using Private Actions

### Method 1: Personal Access Token (Classic)

**Not Recommended for Production**

```yaml
steps:
  - name: Checkout private action
    uses: actions/checkout@v4
    with:
      repository: my-org/private-action
      token: ${{ secrets.PAT_TOKEN }}
      path: .github/actions/private-action

  - name: Use private action
    uses: ./.github/actions/private-action
```

**Limitations:**
- PATs are tied to individual users
- Broad permissions scope
- No audit trail for action usage
- Token rotation requires manual updates

### Method 2: GitHub App Installation Token (Recommended)

**Best Practice for Organizations**

GitHub Apps provide secure, auditable access to private actions with fine-grained permissions.

#### Prerequisites

1. Create a GitHub App in your organization
2. Grant permissions: `Repository contents: Read-only`
3. Install the app in repositories containing actions
4. Store app credentials as secrets

#### Implementation

**Step 1: Create Token Generation Action**

Create a reusable action: `my-org/generate-app-token`

```yaml
# action.yml
name: 'Generate GitHub App Token'
description: 'Generate an installation token for a GitHub App'
inputs:
  app-id:
    description: 'GitHub App ID'
    required: true
  private-key:
    description: 'GitHub App Private Key'
    required: true
outputs:
  token:
    description: 'GitHub App installation token'
    value: ${{ steps.token.outputs.token }}
runs:
  using: 'composite'
  steps:
    - name: Generate token
      id: token
      uses: actions/create-github-app-token@v1
      with:
        app-id: ${{ inputs.app-id }}
        private-key: ${{ inputs.private-key }}
```

**Step 2: Use in Workflows**

```yaml
name: CI with Private Actions

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      # Generate GitHub App token
      - name: Generate token
        id: generate-token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.APP_ID }}
          private-key: ${{ secrets.APP_PRIVATE_KEY }}
          owner: ${{ github.repository_owner }}

      # Checkout private action using app token
      - name: Checkout private build action
        uses: actions/checkout@v4
        with:
          repository: my-org/action-build-java
          token: ${{ steps.generate-token.outputs.token }}
          path: .github/actions/build-java

      # Checkout another private action
      - name: Checkout private security action
        uses: actions/checkout@v4
        with:
          repository: my-org/action-security-scan
          token: ${{ steps.generate-token.outputs.token }}
          path: .github/actions/security-scan

      # Checkout application code
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          path: app

      # Use private actions
      - name: Build Java application
        uses: ./.github/actions/build-java
        with:
          working-directory: app
          java-version: '17'

      - name: Run security scan
        uses: ./.github/actions/security-scan
        with:
          target-directory: app
```

**Step 3: Advanced Pattern with Reusable Workflow**

```yaml
# .github/workflows/setup-actions.yml
name: Setup Private Actions
description: Checkout all private actions needed for builds

on:
  workflow_call:
    inputs:
      actions:
        description: 'List of private actions to checkout (JSON array)'
        required: true
        type: string
    secrets:
      app-id:
        required: true
      app-private-key:
        required: true

jobs:
  setup:
    runs-on: ubuntu-latest
    steps:
      - name: Generate token
        id: token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.app-id }}
          private-key: ${{ secrets.app-private-key }}

      - name: Checkout actions
        run: |
          echo '${{ inputs.actions }}' | jq -r '.[]' | while read action; do
            echo "Checking out $action"
            gh repo clone "$action" ".github/actions/$(basename $action)" -- --depth=1
          done
        env:
          GH_TOKEN: ${{ steps.token.outputs.token }}
```

**Usage:**

```yaml
jobs:
  build:
    uses: ./.github/workflows/setup-actions.yml
    with:
      actions: '["my-org/action-build-java", "my-org/action-security-scan"]'
    secrets:
      app-id: ${{ secrets.APP_ID }}
      app-private-key: ${{ secrets.APP_PRIVATE_KEY }}

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: ./.github/actions/action-build-java
      - uses: ./.github/actions/action-security-scan
```

## GitHub App Authentication

### Creating a GitHub App

1. Navigate to Organization Settings → Developer settings → GitHub Apps
2. Click "New GitHub App"
3. Configure the app:
   - **Name**: `Actions Access App`
   - **Homepage URL**: Your organization URL
   - **Webhook**: Uncheck "Active"
   - **Permissions**:
     - Repository permissions → Contents: Read-only
     - (Optional) Actions: Read if you need workflow run info
   - **Where can this app be installed**: Only on this account

4. After creation:
   - Note the **App ID**
   - Generate and download a **private key**
   - Install the app on repositories containing actions

### Storing Credentials

Store at organization or repository level:

```
Organization Secrets (Recommended):
- APP_ID: <your-app-id>
- APP_PRIVATE_KEY: <content-of-downloaded-key.pem>

Repository Secrets (Alternative):
- ACTIONS_APP_ID
- ACTIONS_APP_PRIVATE_KEY
```

### Token Scope and Lifetime

- **Scope**: Limited to repositories where app is installed
- **Lifetime**: 1 hour (automatically refreshed)
- **Permissions**: Only those granted to the app
- **Audit**: All access logged in organization audit log

### Benefits Over PATs

| Feature | PAT | GitHub App |
|---------|-----|------------|
| User-independent | ❌ | ✅ |
| Fine-grained permissions | Partial | ✅ |
| Audit trail | Limited | ✅ |
| Automatic expiry | ❌ | ✅ (1 hour) |
| Installation-scoped | ❌ | ✅ |
| Revocable per repo | ❌ | ✅ |

## Action Types

### 1. Composite Actions

**Best for**: Combining multiple steps with shell commands

```yaml
name: 'Build and Test Java'
description: 'Standard Java build with Maven or Gradle'
inputs:
  java-version:
    description: 'Java version'
    required: true
    default: '17'
  build-tool:
    description: 'maven or gradle'
    required: true
    default: 'maven'
outputs:
  artifact-path:
    description: 'Path to built artifact'
    value: ${{ steps.build.outputs.artifact }}

runs:
  using: 'composite'
  steps:
    - name: Setup Java
      uses: actions/setup-java@v4
      with:
        java-version: ${{ inputs.java-version }}
        distribution: 'temurin'
        cache: ${{ inputs.build-tool }}

    - name: Build with Maven
      if: inputs.build-tool == 'maven'
      shell: bash
      run: mvn clean package -DskipTests

    - name: Build with Gradle
      if: inputs.build-tool == 'gradle'
      shell: bash
      run: ./gradlew build -x test
```

### 2. JavaScript Actions

**Best for**: Complex logic, API interactions, GitHub API usage

```yaml
name: 'Auto Label PR'
description: 'Automatically label PRs based on changed files'
inputs:
  github-token:
    description: 'GitHub token'
    required: true
  config-path:
    description: 'Path to label configuration'
    default: '.github/labeler.yml'

runs:
  using: 'node20'
  main: 'dist/index.js'
```

### 3. Docker Actions

**Best for**: Specific runtime requirements, language-specific tools

```yaml
name: 'Python Code Quality'
description: 'Run pylint, black, and mypy'
inputs:
  source-directory:
    description: 'Source code directory'
    required: true
    default: 'src'

runs:
  using: 'docker'
  image: 'Dockerfile'
  args:
    - ${{ inputs.source-directory }}
```

## Best Practices

### 1. Action Metadata

Always include complete metadata in `action.yml`:

```yaml
name: 'Action Name'
description: 'Clear, concise description'
author: 'Organization Name'
branding:
  icon: 'check-circle'
  color: 'green'

inputs:
  required-input:
    description: 'What this input does'
    required: true
  optional-input:
    description: 'What this input does'
    required: false
    default: 'default-value'

outputs:
  output-name:
    description: 'What this output contains'
```

### 2. Documentation

Each action repository should include:

- **README.md**: Clear usage examples, input/output documentation
- **CHANGELOG.md**: Version history with breaking changes noted
- **LICENSE**: Open source license (MIT, Apache 2.0, etc.)
- **Examples**: Sample workflows in `examples/` directory

### 3. Input Validation

Always validate inputs in your action:

```javascript
// JavaScript action
const core = require('@actions/core');

try {
  const requiredInput = core.getInput('required-input', { required: true });
  
  if (!requiredInput.match(/^[a-z0-9-]+$/)) {
    throw new Error('Input must contain only lowercase letters, numbers, and hyphens');
  }
  
  // Process input
} catch (error) {
  core.setFailed(error.message);
}
```

```bash
# Composite action
if [ -z "${{ inputs.required-input }}" ]; then
  echo "::error::required-input is required"
  exit 1
fi
```

### 4. Error Handling

Provide clear error messages:

```javascript
try {
  // Action logic
} catch (error) {
  core.setFailed(`Action failed: ${error.message}`);
  core.error(`Stack trace: ${error.stack}`);
}
```

### 5. Security

- Never log sensitive data (tokens, passwords, keys)
- Use `core.setSecret()` to mask sensitive values
- Validate all external inputs
- Pin action dependencies to specific SHA or version
- Run security scanning (CodeQL, Dependabot)

```javascript
const token = core.getInput('github-token');
core.setSecret(token); // Masks token in logs
```

## Versioning Strategy

### Semantic Versioning

Use semantic versioning: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes (e.g., remove input, change behavior)
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes

### Git Tags and Releases

```bash
# Create new version
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3

# Update major version tag (v1)
git tag -fa v1 -m "Update v1 to v1.2.3"
git push origin v1 --force

# Create GitHub release
gh release create v1.2.3 \
  --title "v1.2.3" \
  --notes "See CHANGELOG.md for details"
```

### Usage in Workflows

```yaml
# Pin to specific version (recommended for production)
- uses: my-org/action-build-java@v1.2.3

# Use major version (gets latest compatible)
- uses: my-org/action-build-java@v1

# Use branch (for testing only)
- uses: my-org/action-build-java@main

# Pin to commit SHA (most secure)
- uses: my-org/action-build-java@a1b2c3d4e5f6
```

## Testing Actions

### Composite Actions

```yaml
# .github/workflows/test.yml
name: Test Action

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      
      - name: Test action
        uses: ./
        with:
          required-input: 'test-value'
      
      - name: Verify outputs
        run: |
          if [ -z "${{ steps.test.outputs.result }}" ]; then
            echo "Output missing"
            exit 1
          fi
```

### JavaScript Actions

```javascript
// __tests__/main.test.js
const process = require('process');
const cp = require('child_process');
const path = require('path');

test('test action runs', () => {
  process.env['INPUT_REQUIRED-INPUT'] = 'test';
  const ip = path.join(__dirname, '..', 'index.js');
  const result = cp.execSync(`node ${ip}`, {env: process.env}).toString();
  expect(result).toContain('Success');
});
```

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run tests
        run: npm test
      
      - name: Check dist is up to date
        run: |
          npm run build
          git diff --exit-code dist/
```

## Distribution Methods

### 1. GitHub Marketplace (Public)

Publish public actions to GitHub Marketplace:

1. Add marketplace metadata to `action.yml`
2. Create a release
3. Check "Publish to GitHub Marketplace"

### 2. Internal Organization (Private)

Keep actions private within organization:

- Use GitHub App tokens for access
- Document in internal wiki/portal
- Create organization-level action catalog

### 3. Hybrid Approach

- Open source generic actions
- Keep organization-specific logic private
- Use private actions as wrappers around public ones

## Example: Complete Action Lifecycle

### 1. Create Action Repository

```bash
# Create repository
gh repo create my-org/action-build-java --private --clone

cd action-build-java

# Initialize structure
mkdir -p .github/workflows examples
touch action.yml README.md LICENSE CHANGELOG.md
```

### 2. Implement Action

```yaml
# action.yml
name: 'Build Java Application'
description: 'Build Java apps with Maven or Gradle'
inputs:
  java-version:
    description: 'Java version'
    required: true
    default: '17'
  # ... other inputs

runs:
  using: 'composite'
  steps:
    # ... steps
```

### 3. Add Testing

```yaml
# .github/workflows/test.yml
name: Test Action
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./
        with:
          java-version: '17'
```

### 4. Create Release

```bash
# Update CHANGELOG.md
# Create tag and release
git tag -a v1.0.0 -m "Initial release"
git push origin v1.0.0

gh release create v1.0.0 \
  --title "v1.0.0 - Initial Release" \
  --notes-file CHANGELOG.md
```

### 5. Use in Workflows

```yaml
# Consumer repository
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Generate token
        id: token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.APP_ID }}
          private-key: ${{ secrets.APP_PRIVATE_KEY }}
      
      - name: Checkout action
        uses: actions/checkout@v4
        with:
          repository: my-org/action-build-java
          token: ${{ steps.token.outputs.token }}
          path: .github/actions/build-java
      
      - uses: ./.github/actions/build-java
        with:
          java-version: '17'
```

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Creating Actions](https://docs.github.com/en/actions/creating-actions)
- [GitHub Apps Documentation](https://docs.github.com/en/apps)
- [Action Toolkit (@actions/toolkit)](https://github.com/actions/toolkit)
- [Awesome Actions List](https://github.com/sdras/awesome-actions)
