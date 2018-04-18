#!/usr/bin/env bash

OPTS="-Dmaven.javadoc.skip=true -Dbasepom.check.skip-basic=true -Dbasepom.check.skip-findbugs=true -Dbasepom.check.skip-pmd=true -Dbasepom.check.skip-checkstyle=true -B"

PROFILES="toolchains"

# Oracle profile disabled until they fix their orai18n JAR artifact on maven.oracle.com repository.
#if [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
#  PROFILES="$PROFILES,oracle"
#fi

mvn ${OPTS} -P${PROFILES} verify
