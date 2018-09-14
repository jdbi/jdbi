#!/bin/sh
set -xe

export DOCUMENTATION=Y

OPTS="-DskipTests=true -Dbasepom.check.skip-all=true -B"

PROFILES="toolchains"

# Oracle profile disabled until they fix their orai18n JAR artifact on maven.oracle.com repository.
#if [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
#  PROFILES="$PROFILES,oracle"
#fi

exec mvn ${OPTS} -P${PROFILES} verify
