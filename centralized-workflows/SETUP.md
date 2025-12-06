# Setting Up Organization-Wide Required Workflows

This guide provides step-by-step instructions for setting up centralized, required workflows in your GitHub organization.

## Prerequisites

- Organization owner or admin access
- GitHub Enterprise Cloud or GitHub Enterprise Server 3.10+
- A `.github` repository in your organization

## Step 1: Create the Organization `.github` Repository

1. Navigate to your organization on GitHub
2. Click "New repository"
3. Name the repository `.github` (exactly)
4. Make it private (recommended for security policies)
5. Initialize with a README

## Step 2: Add Required Workflows

1. In the `.github` repository, create a `workflows/` directory
2. Copy the following files from this directory:
   - `required-security-scan.yml`
   - `required-dependency-check.yml`
3. Commit and push these files

## Step 3: Configure Organization Rulesets

### Creating a Ruleset

1. Go to your organization settings
2. Navigate to **Rules** > **Rulesets**
3. Click **New ruleset** > **New repository ruleset**

### Ruleset Configuration

**Basic Settings:**
- **Name**: `Required Security Workflows`
- **Enforcement status**: Active
- **Target repositories**: Choose one:
  - All repositories
  - Default branch only
  - Specific repositories (for testing)

**Bypass List (Optional):**
- Add organization administrators
- Add specific users or teams that can bypass requirements

### Add Required Workflows Rule

1. In the ruleset, scroll to **Rules**
2. Find and enable **Require workflows to pass before merging**
3. Click **Add workflow**
4. For each required workflow:
   - **Source**: `.github` (your organization's repository)
   - **Workflow**: Select the workflow file
   - **Required to pass**: Yes

Add both:
- `workflows/required-security-scan.yml`
- `workflows/required-dependency-check.yml`

## Step 4: Configure Additional Protection Rules (Recommended)

In the same ruleset, consider enabling:

- **Require status checks to pass before merging**
- **Require pull request reviews before merging** (minimum 1-2 reviewers)
- **Require conversation resolution before merging**
- **Require signed commits**
- **Block force pushes**
- **Restrict deletions**

## Step 5: Test the Configuration

1. Create a test repository in your organization
2. Make a change and create a pull request
3. Verify that required workflows appear in the PR checks
4. Confirm that the PR cannot be merged until workflows pass

## Step 6: Monitor and Maintain

### Monitoring

- Review workflow runs regularly in the Actions tab
- Check for failed required workflows across repositories
- Monitor security issues created by automated scans

### Maintenance

- Update workflow files as new security tools become available
- Review and update suppression files for dependencies
- Adjust vulnerability thresholds based on organizational risk tolerance

## Troubleshooting

### Workflows Not Running

**Issue**: Required workflows don't appear in PR checks

**Solutions**:
1. Verify ruleset is active and targeting correct repositories
2. Check that workflow files are in `.github` repository
3. Ensure workflow file syntax is correct
4. Verify organization has required features enabled

### Workflows Always Failing

**Issue**: Workflows fail on all repositories

**Solutions**:
1. Review workflow logs for specific errors
2. Check if required permissions are granted
3. Verify that workflows handle different project types correctly
4. Consider adding language detection logic

### Performance Impact

**Issue**: Required workflows slow down development

**Solutions**:
1. Optimize workflow efficiency (caching, parallel jobs)
2. Adjust when workflows run (e.g., only on main branch)
3. Use path filters to run only when relevant files change
4. Consider increasing runner capacity

## Security Considerations

1. **Least Privilege**: Workflows should request minimum necessary permissions
2. **Secret Management**: Use organization secrets for sensitive data
3. **Audit Logging**: Enable audit log for ruleset changes
4. **Regular Updates**: Keep workflow actions updated to latest versions
5. **Suppression Review**: Regularly review dependency suppressions

## Exemptions

If certain repositories need to be exempted:

1. Create a separate ruleset for exempted repositories
2. Document the reason for exemption
3. Schedule periodic review of exemptions
4. Consider using repository-specific workflow overrides

## Repository-Specific Suppressions

For dependency check suppressions:

1. Create `dependency-check-suppressions.xml` in repository root
2. Add specific CVEs with justification:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes><![CDATA[
        False positive - this vulnerability doesn't affect our usage
        ]]></notes>
        <cve>CVE-2023-12345</cve>
    </suppress>
</suppressions>
```

## Integration with GitHub Advanced Security

If using GitHub Advanced Security:

1. Enable Dependabot alerts
2. Enable Dependabot security updates
3. Enable code scanning (CodeQL)
4. Enable secret scanning
5. Required workflows complement these features

## Migration Strategy

For existing organizations:

1. **Phase 1**: Deploy to pilot repositories (2 weeks)
2. **Phase 2**: Expand to non-critical repositories (1 month)
3. **Phase 3**: Roll out to all repositories (2 months)
4. **Phase 4**: Enforce strict policies (3 months)

## Best Practices

1. **Communication**: Announce required workflows to development teams
2. **Documentation**: Provide clear guidance on resolving common issues
3. **Support**: Establish support channel for workflow questions
4. **Feedback Loop**: Gather developer feedback and iterate
5. **Training**: Conduct training sessions on security workflows

## Additional Resources

- [GitHub Actions documentation](https://docs.github.com/en/actions)
- [About repository rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- [Required workflows](https://docs.github.com/en/actions/using-workflows/required-workflows)
- Organization security best practices: See `/docs/SECURITY_HARDENING.md`

## Support

For issues with centralized workflows:
1. Check troubleshooting section above
2. Review workflow logs in failed runs
3. Contact organization security team
4. Create issue in `.github` repository
