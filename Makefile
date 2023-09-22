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
.PHONY: help clean install install-notests install-nodocker install-fast docs tests run-tests run-slow-tests run-tests-nodocker publish-docs deploy release release-docs

MAVEN = ./mvnw ${JDBI_MAVEN_OPTS}

export MAVEN_OPTS MAVEN_CONFIG

default: help

clean:
	${MAVEN} clean

install:
	${MAVEN} clean install

tests: install-fast run-tests

install-notests: MAVEN_CONFIG += -Dbasepom.test.skip=true
install-notests: install

install-nodocker: MAVEN_CONFIG += -Dno-docker=true
install-nodocker: install

install-fast: MAVEN_CONFIG += -Pfast
install-fast: install

docs: MAVEN_CONFIG += -Ppublish-docs -Pfast -Dbasepom.javadoc.skip=false
docs: install

run-tests: MAVEN_CONFIG += -Dbasepom.it.skip=false
run-tests:
	${MAVEN} surefire:test invoker:install invoker:integration-test invoker:verify

run-slow-tests: MAVEN_CONFIG += -Pslow-tests
run-slow-tests: run-tests

run-tests-nodocker: MAVEN_CONFIG += -Dno-docker=true
run-tests-nodocker: run-tests

publish-docs: MAVEN_CONFIG += -Pfast -Dbasepom.javadoc.skip=false
publish-docs: install
	${MAVEN} -Ppublish-docs -pl :jdbi3-docs clean deploy

deploy: MAVEN_CONFIG += -Dbasepom.it.skip=false
deploy:
	${MAVEN} clean deploy

release:
	${MAVEN} clean release:clean release:prepare release:perform

release-docs: MAVEN_CONFIG += -Pjdbi-release
release-docs: publish-docs

help:
	@echo " * clean               - clean local build tree"
	@echo " * install             - build, run static analysis and unit tests, then install in the local repository"
	@echo " * install-notests     - same as 'install', but skip unit tests"
	@echo " * install-nodocker    - same as 'install', but skip unit tests that require a local docker installation"
	@echo " * install-fast        - same as 'install', but skip unit tests and static analysis"
	@echo " * tests               - build code and run unit and integration tests except really slow tests"
	@echo " * docs                - build up-to-date documentation in docs/target/generated-docs/"
	@echo " * run-tests           - run all unit and integration tests except really slow tests"
	@echo " * run-tests-nodocker  - same as 'run-tests', but skip all tests that require a local docker installation"
	@echo " * run-tests-container - run the full multi-database container test suite"
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
	@echo " * release-docs     - deploy release documentation"
