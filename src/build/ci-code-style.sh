#!/bin/sh
set -xe

OPTS="-DskipTests=true -Dmaven.javadoc.skip=true -B"

PROFILES="toolchains"

exec mvn ${OPTS} -P${PROFILES} verify
