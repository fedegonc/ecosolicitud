#!/usr/bin/env bash
# Compila el scope test y corre la simulación en memoria: no toca data/ ni prod.
cd "$(dirname "$0")/.." || exit 1
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/eco-cp.txt
java -cp "target/test-classes:target/classes:$(cat /tmp/eco-cp.txt)" \
    scratch3.SimulacionCasos
