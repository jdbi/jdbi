#!/bin/sh
exec mvn clean deploy -Ppublish-docs -Dbasepom.javadoc.skip=false -DskipTests
