#!/bin/sh
set -xe

JDK_VERSION=$1
OPTS="-Dbasepom.check.skip-all=true -Dbasepom.check.skip-enforcer=false -B"

BUILD_JDK_HOME=/home/travis/openjdk16

echo "JAVA_HOME: $JAVA_HOME"
if [ $JDK_VERSION -lt 12 ]; then
    if [ "$TRAVIS" = "true" ]; then
        OLD_JAVA_HOME=${JAVA_HOME}
        OLD_PATH=${PATH}

        # build must be done with a JDK > 11, install JDK 16
        ${TRAVIS_BUILD_DIR}/src/build/install-jdk.sh --target ${BUILD_JDK_HOME} --workspace "/home/travis/.cache/install-jdk-16" --feature "16" --cacerts

        JAVA_HOME=${BUILD_JDK_HOME}
        PATH=${JAVA_HOME}/bin:$PATH
        export JAVA_HOME PATH
        # needs to be install as tests pick up deps from the local repo
        mvn ${OPTS} -DskipTests clean install

        # run the tests with the old JDK
        PATH=${OLD_PATH}
        JAVA_HOME=${OLD_JAVA_HOME}
        export JAVA_HOME PATH
        exec mvn -DargLine= surefire:test
    fi
fi

exec mvn ${OPTS} clean verify
