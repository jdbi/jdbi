#!/bin/sh

#
# .github/workflows/doc.yml

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -Dbasepom.check.fail-javadoc=true -B"

mvn ${OPTS} -Ppublish-docs clean install
