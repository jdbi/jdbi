#!/bin/sh
set -xe

OPTS="-Dmaven.javadoc.skip=true -Dbasepom.check.skip-all=true -B"

exec mvn ${OPTS} verify
