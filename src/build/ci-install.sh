#!/usr/bin/env bash

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dmaven.javadoc.skip=true -B"

PROFILES="toolchains"

exec mvn ${OPTS} -P${PROFILES} verify
