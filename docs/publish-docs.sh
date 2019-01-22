#!/bin/sh
exec mvn clean deploy -Dpublish-docs -Dtoolchain -Dbasepom.javadoc.skip=false -DskipTests
