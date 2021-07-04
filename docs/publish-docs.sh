#!/bin/sh
DOC_OPTS="-Dbasepom.check.skip-all=true -Dbasepom.check.skip-javadoc=false -DskipTests"
# mvn ${DOC_OPTS} clean install
mvn ${DOC_OPTS} -Ppublish-docs -pl :jdbi3-docs clean deploy
