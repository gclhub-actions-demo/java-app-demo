#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '::error::%s\n' "$*" >&2
  exit 1
}

[[ -f pom.xml ]] || fail "pom.xml is required at the project root."
[[ -f checkstyle.xml ]] || fail "checkstyle.xml is required at the project root."
[[ -d src/main/java ]] || fail "src/main/java is required."
[[ -d src/test/java ]] || fail "src/test/java is required."
command -v java >/dev/null 2>&1 || fail "Java is not available on PATH."
command -v mvn >/dev/null 2>&1 || fail "Maven is not available on PATH."

java_version="$(java -version 2>&1)"
if ! grep -Eq 'version "17([.]|")' <<<"$java_version"; then
  fail "The required workflow must provide Java 17; found: $(head -n 1 <<<"$java_version")"
fi

unit_test="$(find src/test/java -type f -name '*Test.java' -print -quit)"
integration_test="$(find src/test/java -type f -name '*IT.java' -print -quit)"
[[ -n "$unit_test" ]] || fail "No Maven unit-test source (*Test.java) was found."
[[ -n "$integration_test" ]] || fail "No Maven integration-test source (*IT.java) was found."

printf 'Preflight passed: Java 17 and Maven are available.\n'
printf 'Unit-test source: %s\n' "$unit_test"
printf 'Integration-test source: %s\n' "$integration_test"
mvn --version
