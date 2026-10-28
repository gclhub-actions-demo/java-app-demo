#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '::error::%s\n' "$*" >&2
  exit 1
}

[[ -f pom.xml ]] || fail "pom.xml is required at the project root."
[[ -f checkstyle.xml ]] || fail "checkstyle.xml is required at the project root."
command -v python3 >/dev/null 2>&1 || fail "Python 3 is required to validate Maven configuration."

python3 - <<'PY'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def fail(message):
    print(f"::error::{message}", file=sys.stderr)
    raise SystemExit(1)


try:
    pom = ET.parse("pom.xml").getroot()
except ET.ParseError as error:
    fail(f"Could not parse pom.xml: {error}")

ns = {"m": "http://maven.apache.org/POM/4.0.0"}


def text(element, path):
    child = element.find(path, ns)
    return child.text.strip() if child is not None and child.text else None


properties = pom.find("m:properties", ns)
if properties is None:
    fail("pom.xml has no Maven properties.")
for prop in ("java.version", "maven.compiler.source", "maven.compiler.target"):
    value = text(properties, f"m:{prop}")
    if value != "17":
        fail(f"{prop} must remain Java 17; found {value!r}.")

plugins = pom.findall("m:build/m:plugins/m:plugin", ns)
plugin_by_artifact = {text(plugin, "m:artifactId"): plugin for plugin in plugins}

checkstyle = plugin_by_artifact.get("maven-checkstyle-plugin")
if checkstyle is None:
    fail("maven-checkstyle-plugin is required.")
if text(checkstyle, "m:configuration/m:failsOnError") != "true":
    fail("Checkstyle must continue to fail on errors.")
if text(checkstyle, "m:configuration/m:configLocation") != "checkstyle.xml":
    fail("Maven must use the repository's checkstyle.xml.")
if not any(
    text(execution, "m:phase") == "validate"
    and any(text(goal, ".") == "check" for goal in execution.findall("m:goals/m:goal", ns))
    for execution in checkstyle.findall("m:executions/m:execution", ns)
):
    fail("Checkstyle must run during Maven validate.")

if "maven-surefire-plugin" not in plugin_by_artifact:
    fail("maven-surefire-plugin is required for unit tests.")

failsafe = plugin_by_artifact.get("maven-failsafe-plugin")
if failsafe is None:
    fail("maven-failsafe-plugin is required for integration tests.")
failsafe_goals = {
    text(goal, ".")
    for execution in failsafe.findall("m:executions/m:execution", ns)
    for goal in execution.findall("m:goals/m:goal", ns)
}
if not {"integration-test", "verify"}.issubset(failsafe_goals):
    fail("Maven Failsafe must retain both integration-test and verify goals.")

jacoco = plugin_by_artifact.get("jacoco-maven-plugin")
if jacoco is None:
    fail("jacoco-maven-plugin is required.")
coverage_check = next(
    (
        execution
        for execution in jacoco.findall("m:executions/m:execution", ns)
        if text(execution, "m:id") == "jacoco-check"
    ),
    None,
)
if coverage_check is None:
    fail("The jacoco-check execution is required.")
if not any(
    text(goal, ".") == "check"
    for goal in coverage_check.findall("m:goals/m:goal", ns)
):
    fail("The JaCoCo check goal must remain enabled.")
rules = coverage_check.findall(
    "m:configuration/m:rules/m:rule",
    ns,
)
if not any(
    text(rule, "m:element") == "PACKAGE"
    and any(
        text(limit, "m:counter") == "LINE"
        and text(limit, "m:value") == "COVEREDRATIO"
        and text(limit, "m:minimum") == "0.70"
        for limit in rule.findall("m:limits/m:limit", ns)
    )
    for rule in rules
):
    fail("JaCoCo must retain the 70% package-level line-coverage minimum.")

aggregate_report = next(
    (
        execution
        for execution in jacoco.findall("m:executions/m:execution", ns)
        if text(execution, "m:id") == "report-aggregate"
    ),
    None,
)
if aggregate_report is None or text(aggregate_report, "m:phase") != "verify":
    fail("JaCoCo aggregate reporting must remain bound to verify.")
if not any(
    text(goal, ".") == "report"
    for goal in aggregate_report.findall("m:goals/m:goal", ns)
):
    fail("The JaCoCo aggregate report goal must remain enabled.")
if text(aggregate_report, "m:configuration/m:outputDirectory") != (
    "${project.reporting.outputDirectory}/jacoco-aggregate"
):
    fail("JaCoCo aggregate report must be written to target/site/jacoco-aggregate/.")
formats = {
    text(format_element, ".")
    for format_element in aggregate_report.findall("m:configuration/m:formats/m:format", ns)
}
if "XML" not in formats:
    fail("JaCoCo aggregate report must explicitly generate XML.")

print(
    "Postflight passed: Java 17, strict Checkstyle, Failsafe integration tests, "
    "package line-coverage threshold, and aggregate XML reporting remain configured."
)
PY
