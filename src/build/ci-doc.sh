#!/bin/sh
set -xe

OPTS="-Dbasepom.javadoc.skip=false -DskipTests=true -Dbasepom.check.skip-all=true -B"

PROFILES="toolchains"

exec mvn ${OPTS} -P${PROFILES} verify
