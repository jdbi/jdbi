#!/bin/sh

#
# .github/workflows/doc.yml

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -Dbasepom.check.fail-javadoc=true -B"

# required for kotlin doc builds
MAVEN_OPTS=--illegal-access=permit mvn -q ${OPTS} clean verify
