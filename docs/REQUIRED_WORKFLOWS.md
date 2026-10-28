# Central Required Workflows

This repository opts into the organization-managed required-workflow policy. The policy and its workflow definitions live in the [central required-workflows repository](https://github.com/gclhub-actions-demo/required-workflows); this repository does not copy or call those workflows locally.

## What is enforced

The organization ruleset requires the central workflow identity on every pull request targeting `main`, including documentation-only changes. Its required workflow identity comes from the workflow in the central repository, not from a similarly named workflow in this repository. Do not add path filters that skip required checks, or a local caller workflow as a substitute for the organization ruleset.

The rule is enforced by organization settings, not by this repository's administrators. Repository administrators cannot bypass it. Organization owners remain trusted administrators of the organization-level policy.

Required workflows are for deterministic build and policy gates. Do not run CodeQL `init` or `analyze` actions inside workflows selected as required by a ruleset. Run CodeQL in a separate scanning workflow and protect merging with the appropriate code-scanning rule.

## Repository contract

The central workflow reads `.github/app-ci.json`. Its schema is strict: unknown properties, project identifiers, or paths are rejected. Keep this file limited to the agreed contract; it cannot select commands, runners, thresholds, or disable required stages.

```json
{
  "version": 1,
  "projects": [
    {
      "id": "java",
      "path": "."
    }
  ],
  "extensions": {
    "preflight": ".github/ci/preflight.sh",
    "postflight": ".github/ci/postflight.sh"
  }
}
```

The optional preflight and postflight extensions are repository-owned examples that execute independently in fresh hosted runners. They are read-only and receive no secrets. They cannot inject commands between required workflow steps or modify the central policy, its commands, thresholds, or toolchain. Keep them limited to validating repository prerequisites and reporting results.

`preflight.sh` checks the Java 17/Maven environment and confirms the expected Maven project and test source layout. `postflight.sh` checks the actual Surefire and Failsafe XML reports and aggregate JaCoCo report produced by the required build; it fails if reports are missing, malformed, contain no executed tests, contain test failures/errors, or report less than 70% aggregate line coverage. This mirrors rather than weakens the Maven gates.

The Maven configuration in [`pom.xml`](../pom.xml) runs fail-on-error Checkstyle during `validate`, unit tests with Surefire, integration tests with Failsafe, and JaCoCo reporting/checking during `verify`. Reports are written to:

| Report | Path |
| --- | --- |
| Checkstyle | `target/checkstyle-result.xml` |
| Unit tests | `target/surefire-reports/` |
| Integration tests | `target/failsafe-reports/` |
| JaCoCo unit report | `target/site/jacoco/` |
| JaCoCo aggregate report | `target/site/jacoco-aggregate/` |

JaCoCo enforces a 70% package-level line-coverage minimum. Do not skip tests, relax Checkstyle, or lower coverage thresholds to make a pull request pass.

## Local workflows remain separate

The repository's workflows under [`.github/workflows/`](../.github/workflows/) are examples for ordinary CI, scheduled security scanning, deployment, and manual release tasks. They are not the organization-required workflow identity. Keep their triggers and responsibilities separate from the central main-branch rule; changes here do not create or modify organization rulesets.

The local pull-request example currently filters documentation paths, and its Java matrix and individual jobs are demonstrations. Neither those filters nor those jobs control the central required run. Organization settings require the central check for every pull request to `main`.

For central workflow setup, policy examples, and organization ruleset operations, see the [central required-workflows repository](https://github.com/gclhub-actions-demo/required-workflows). Make ruleset changes through organization administration; do not introduce repository-admin bypasses or workflow-copy instructions here.
