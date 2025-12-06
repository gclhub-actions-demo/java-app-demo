# Centralized GitHub Workflows Repository

This directory contains the structure for a centralized workflows repository that should be created at the organization level.

## Repository Setup

Create a repository named `.github` in your organization (e.g., `your-org/.github`). This is a special repository that GitHub recognizes for organization-wide configurations.

## Structure

```
.github/
├── workflow-templates/        # Reusable workflow templates
├── workflows/                 # Required workflows
│   ├── required-security-scan.yml
│   └── required-dependency-check.yml
└── README.md
```

## Required Workflows

The workflows in this directory are designed to be enforced across all repositories in your organization using GitHub organization rulesets.

### Setting Up Organization Rulesets

1. Navigate to your organization settings
2. Go to "Rules" > "Rulesets"
3. Create a new ruleset
4. Configure the following:
   - **Name**: "Required Security Workflows"
   - **Target**: All repositories (or specific repositories)
   - **Bypass list**: Add organization admins if needed
5. Add required workflows:
   - Select "Require workflows to pass before merging"
   - Add the workflows from this repository

### Files in this Directory

- `required-security-scan.yml` - Organization-wide security scanning
- `required-dependency-check.yml` - Dependency vulnerability checking
- `SETUP.md` - Detailed setup instructions
- `CONFIGURATION.md` - Configuration options
- `TROUBLESHOOTING.md` - Common issues and solutions

## Implementation Steps

1. Create the `.github` repository in your organization
2. Copy the workflows from the `centralized-workflows/` directory
3. Configure organization rulesets to require these workflows
4. Test with a pilot repository
5. Roll out to all repositories

## See Also

- Main repository documentation: `/docs/REQUIRED_WORKFLOWS.md`
- Organization policies: `/docs/ORGANIZATION_POLICIES.md`
