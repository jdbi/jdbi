#!/bin/sh
set -xe

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -Dbasepom.check.fail-javadoc=false -B"

exec mvn ${OPTS} verify
