#!/bin/sh

#
# .github/workflows/style.yml

OPTS="-DskipTests=true -B"

mvn -q ${OPTS} clean install
