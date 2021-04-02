#!/bin/sh
set -xe

JDK_VERSION=$1
OPTS="-Dbasepom.check.skip-all=true -B"

if [ "$JDK_VERSION" = "8" ]; then
    if [ "$TRAVIS" = "true" ]; then
        # build must be done with a JDK > 8
        OLD_JAVA_HOME=${JAVA_HOME}
        OLD_PATH=${PATH}
        JAVA_HOME=/usr/local/lib/jvm/openjdk11
        PATH=${JAVA_HOME}/bin:$PATH
        export JAVA_HOME PATH
        # needs to be install as tests pick up deps from the local repo
        mvn ${OPTS} -DskipTests clean install

        # run the tests with JDK8
        PATH=${OLD_PATH}
        JAVA_HOME=${OLD_JAVA_HOME}
        export JAVA_HOME PATH
        exec mvn -DargLine= surefire:test
    fi
fi

exec mvn ${OPTS} clean verify
