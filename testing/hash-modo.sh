#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q dependency:build-classpath \
    -DincludeArtifactIds=spring-security-crypto,spring-jcl \
    -Dmdep.outputFile=target/modo-classpath.txt
java --class-path "$(cat target/modo-classpath.txt)" testing/ClaveModo.java
