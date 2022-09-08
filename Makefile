#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

#
# Makefile for jdbi build targets
#

SHELL = /bin/sh
.SUFFIXES:
.PHONY: help clean install install-nodocker docs tests tests-nodocker publish-docs deploy release

# replace JDBI_MAVEN_OPTS with implicit MAVEN_OPTS, once 3.9.x or later has been released
MAVEN = ./mvnw ${JDBI_MAVEN_OPTS}

default: help

clean:
	${MAVEN} clean

install:
	${MAVEN} clean install

install-nodocker: JDBI_MAVEN_OPTS += -Dno-docker
install-nodocker: install

docs: JDBI_MAVEN_OPTS += -Dbasepom.check.skip-all=true -Dbasepom.javadoc.skip=false -DskipTests=true
docs: install
	${MAVEN} -pl :jdbi3-docs clean install

tests: JDBI_MAVEN_OPTS += -Dbasepom.it.skip=false
tests:
	${MAVEN} surefire:test invoker:integration-test invoker:verify

tests-nodocker: JDBI_MAVEN_OPTS += -Dno-docker
tests-nodocker: tests

publish-docs: JDBI_MAVEN_OPTS += -Dbasepom.check.skip-all=true -Dbasepom.javadoc.skip=false -DskipTests=true
publish-docs: install
	${MAVEN} -Ppublish-docs -pl :jdbi3-docs clean deploy

deploy:
	${MAVEN} clean deploy

release:
	${MAVEN} clean release:clean release:prepare release:perform

help:
	@echo " * clean            - clean local build tree"
	@echo " * install          - builds and installs the current version in the local repository"
	@echo " * install-nodocker - same as `install`, but skip all tests that require a local docker installation"
	@echo " * docs             - build up-to-date documentation in docs/target/generated-docs/"
	@echo " * tests            - run all unit and integration tests"
	@echo " * tests-nodocker   - same as `tests`, but skip all tests that require a local docker installation"
	@echo " *"
	@echo " ***********************************************************************"
	@echo " *"
	@echo " * privileged targets (require project privileges)"
	@echo " *"
	@echo " ***********************************************************************"
	@echo " *"
	@echo " * publish-docs     - build up-to-date documentation and deploy to jdbi.org"
	@echo " * deploy           - builds and deploys the current version to the Sonatype OSS repository"
	@echo " * release          - create and deploy a Jdbi release"
