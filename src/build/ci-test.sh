#!/bin/sh
set -xe

JDK_VERSION=$1
OPTS="-Dbasepom.check.skip-all=true -B"

if [ "$JDK_VERSION" = "8" ]; then
    if [ "$TRAVIS" = "true" ]; then
        # build must be done with a JDK > 8
        jdk_switcher use openjdk11
        mvn ${OPTS} -DskipTests clean verify
        jdk_switcher use openjdk8
        exec mvn -DargLine= surefire:test
    fi
fi

exec mvn ${OPTS} clean verify
