#!/bin/sh
#
# .github/workflows/cd.yml

set -xe

OPTS="-B -fae"

PATH=${JAVA_HOME}/bin:$PATH
export PATH
mvn ${OPTS} clean deploy
