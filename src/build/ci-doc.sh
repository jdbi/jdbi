#!/bin/sh
set -xe

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -Dbasepom.check.fail-javadoc=true -B"

MAVEN_OPTS=--illegal-access=permit exec mvn ${OPTS} verify
