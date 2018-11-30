#!/usr/bin/env bash

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -Dmaven.javadoc.skip=true -B"

exec mvn ${OPTS} verify
