# Central Required Workflow Examples

The organization-managed required-workflow implementation lives in the [central required-workflows repository](https://github.com/gclhub-actions-demo/required-workflows). This directory is legacy reference material only; its files do not define the workflow identity or policy required by the organization ruleset. Do not copy these files into a required workflow repository or register them as the required workflow.

The organization ruleset requires the central workflow on pull requests targeting `main`, including documentation-only changes. Its workflow identity is selected in organization ruleset settings. Repository administrators cannot bypass the rule; organization owners remain trusted administrators of organization-level policy.

The CodeQL sample is a standalone scanning example, not a ruleset-required workflow. CodeQL `init` and `analyze` must not run inside a workflow selected as required by a ruleset. Use a separate CodeQL workflow and protect merges with the appropriate code-scanning rule.

For this Java application, the strict repository contract and isolated hooks are documented in [`docs/REQUIRED_WORKFLOWS.md`](../docs/REQUIRED_WORKFLOWS.md), with configuration in [`.github/app-ci.json`](../.github/app-ci.json). That local config cannot set build commands, alter thresholds, or change which central workflow is enforced.

See the central repository for authoritative workflow definitions and organization-level setup: https://github.com/gclhub-actions-demo/required-workflows
