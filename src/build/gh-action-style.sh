#!/bin/sh

#
# .github/workflows/style.yml

OPTS="-DskipTests=true -B"

mvn ${OPTS} clean install
