#!/bin/sh
set -xe

OPTS="-Dmaven.javadoc.skip=true -Dbasepom.check.skip-basic=true -Dbasepom.check.skip-findbugs=true -Dbasepom.check.skip-pmd=true -Dbasepom.check.skip-checkstyle=true -B"

exec mvn ${OPTS} verify
