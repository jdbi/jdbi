#!/bin/sh
DOC_OPTS="-Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -DskipTests"
MAVEN_OPTS="--illegal-access=warn" mvn ${DOC_OPTS} clean install
MAVEN_OPTS="--illegal-access=warn" mvn ${DOC_OPTS} -Ppublish-docs -pl :jdbi3-docs clean deploy
