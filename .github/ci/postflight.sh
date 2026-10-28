#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '::error::%s\n' "$*" >&2
  exit 1
}

python3 - <<'PY'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def fail(message):
    print(f"::error::{message}", file=sys.stderr)
    raise SystemExit(1)


def read_test_reports(directory, label):
    reports = sorted(Path(directory).glob("TEST-*.xml"))
    if not reports:
        fail(f"No {label} test reports were generated in {directory}.")

    totals = {"tests": 0, "failures": 0, "errors": 0}
    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError as error:
            fail(f"Could not parse {report}: {error}")
        suites = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
        for suite in suites:
            for key in totals:
                try:
                    totals[key] += int(suite.attrib.get(key, "0"))
                except ValueError:
                    fail(f"Invalid {key} count in {report}.")

    if totals["tests"] == 0:
        fail(f"The {label} test reports contain no executed tests.")
    if totals["failures"] or totals["errors"]:
        fail(
            f"The {label} reports contain {totals['failures']} failures and "
            f"{totals['errors']} errors."
        )
    print(f"{label}: {totals['tests']} tests passed across {len(reports)} report(s).")


checkstyle_report = Path("target/checkstyle-result.xml")
if not checkstyle_report.is_file():
    fail(f"Checkstyle XML report is missing: {checkstyle_report}")
try:
    checkstyle = ET.parse(checkstyle_report).getroot()
except ET.ParseError as error:
    fail(f"Could not parse {checkstyle_report}: {error}")
checkstyle_errors = checkstyle.findall(".//error")
if checkstyle_errors:
    fail(f"Checkstyle report contains {len(checkstyle_errors)} violation(s).")
print("Checkstyle: report generated with no violations.")

read_test_reports("target/surefire-reports", "Unit tests")
read_test_reports("target/failsafe-reports", "Integration tests")

coverage_report = Path("target/site/jacoco-aggregate/jacoco.xml")
if not coverage_report.is_file():
    fail(f"JaCoCo aggregate XML report is missing: {coverage_report}")
try:
    report = ET.parse(coverage_report).getroot()
except ET.ParseError as error:
    fail(f"Could not parse {coverage_report}: {error}")

packages = report.findall("package")
if not packages:
    fail(f"JaCoCo report contains no package-level coverage data: {coverage_report}")

covered_packages = 0
for package in packages:
    line_counter = next(
        (counter for counter in package.findall("counter")
         if counter.attrib.get("type") == "LINE"),
        None,
    )
    if line_counter is None:
        continue
    try:
        missed = int(line_counter.attrib["missed"])
        covered = int(line_counter.attrib["covered"])
    except (KeyError, ValueError):
        fail(f"JaCoCo report has an invalid LINE counter for package {package.attrib.get('name')}.")

    total = missed + covered
    if total == 0:
        fail(f"JaCoCo report contains no measured lines for package {package.attrib.get('name')}.")
    covered_packages += 1
    percentage = covered / total
    print(
        f"Package {package.attrib.get('name')}: "
        f"{percentage:.1%} line coverage ({covered}/{total} lines)."
    )
    if percentage < 0.70:
        fail(
            f"Package {package.attrib.get('name')} is below the project's "
            "existing 70% line-coverage minimum."
        )
if covered_packages == 0:
    fail("JaCoCo aggregate report contains no package-level LINE counters.")
PY

printf '%s\n' "Postflight passed: unit, integration, and aggregate coverage reports are present and valid."
