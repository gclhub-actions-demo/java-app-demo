# Organization Required Workflow Setup

The authoritative required workflow and its setup documentation are maintained in the [central required-workflows repository](https://github.com/gclhub-actions-demo/required-workflows). The files in this directory are examples only; do not copy them into the central repository or select them as the workflow required by the organization ruleset.

## Organization ruleset

Configure the required workflow in organization settings:

1. Create or update the organization repository ruleset targeting the `main` branch.
2. Select the workflow identity from `gclhub-actions-demo/required-workflows`.
3. Enforce the ruleset for the intended repositories and require pull requests.
4. Verify the central workflow runs on every pull request to `main`, including documentation-only changes.

The ruleset is not enforced by this repository's local workflows. Repository administrators cannot bypass it; organization owners remain trusted administrators of organization-level policy. Do not configure a repository-admin bypass.

## Code scanning is separate

The [`security-scan-example.yml`](workflows/security-scan-example.yml) sample is a standalone CodeQL scanning workflow, not a required workflow. CodeQL `init` and `analyze` must not run inside any workflow selected as required by a ruleset. Protect merges separately with the appropriate code-scanning rule.

The Java application's central contract, build reports, and safe isolated extensions are documented in [`docs/REQUIRED_WORKFLOWS.md`](../docs/REQUIRED_WORKFLOWS.md). Organization ruleset changes must be made by organization administrators, not by adding a local caller workflow.
