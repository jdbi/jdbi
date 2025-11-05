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

MAVEN = ./mvnw

export MAVEN_OPTS
export MAVEN_ARGS

# must be the first target
default:: help

Makefile:: ;

clean::
	${MAVEN} clean

install:: MAVEN_ARGS += -Dbasepom.it.skip-install=false
install::
	${MAVEN} clean install

tests:: install-notests run-tests

install-notests:: MAVEN_ARGS += -Dbasepom.test.skip=true
install-notests:: install

install-nodocker:: MAVEN_ARGS += -Dno-docker=true
install-nodocker:: install

install-fast:: MAVEN_ARGS += -Pfast
install-fast:: install

compare-reproducible:: MAVEN_ARGS += -Dbasepom.test.skip=true -Djdbi.check.skip-japicmp=true
compare-reproducible::
	${MAVEN} clean verify artifact:compare

docs:: MAVEN_ARGS += -Ppublish-docs -Dbasepom.javadoc.skip=false
docs:: install-fast

run-tests:: MAVEN_ARGS += -Dbasepom.it.skip=false
run-tests::
	${MAVEN} surefire:test invoker:integration-test invoker:verify

run-slow-tests:: MAVEN_ARGS += -Pslow-tests
run-slow-tests:: run-tests

run-tests-nodocker:: MAVEN_ARGS += -Dno-docker=true
run-tests-nodocker:: run-tests

native-tests:: MAVEN_ARGS += -Pslow-tests,native
native-tests:: install

publish-docs:: MAVEN_ARGS += -Dbasepom.javadoc.skip=false
publish-docs:: install-fast
	${MAVEN} -Ppublish-docs -pl :jdbi3-docs clean deploy

deploy:: MAVEN_ARGS += -Dbasepom.it.skip=false
deploy::
	${MAVEN} clean deploy

release::
	${MAVEN} clean release:clean release:prepare release:perform

release-docs:: MAVEN_ARGS += -Pjdbi-release
release-docs:: publish-docs

help::
	@echo " * clean                - clean local build tree"
	@echo " * install              - build, run static analysis and unit tests, then install in the local repository"
	@echo " * install-notests      - same as 'install', but skip unit tests"
	@echo " * install-nodocker     - same as 'install', but skip unit tests that require a local docker installation"
	@echo " * install-fast         - same as 'install', but skip unit tests and static analysis"
	@echo " * compare-reproducible - compare against installed jars to ensure reproducible build"
	@echo " * tests                - build code and run unit and integration tests except really slow tests"
	@echo " * docs                 - build up-to-date documentation in docs/target/generated-docs/"
	@echo " * run-tests            - run all unit and integration tests except really slow tests"
	@echo " * run-slow-tests       - run all unit and integration tests"
	@echo " * run-native-tests     - run native unit tests"
	@echo " * run-tests-nodocker   - same as 'run-tests', but skip all tests that require a local docker installation"
	@echo " *"
	@echo " ***********************************************************************"
	@echo " *"
	@echo " * privileged targets (require project privileges)"
	@echo " *"
	@echo " ***********************************************************************"
	@echo " *"
	@echo " * deploy               - builds and deploys the current version to the Sonatype OSS repository"
	@echo " * publish-docs         - build up-to-date documentation and deploy to jdbi.org"
	@echo " * release              - create and deploy a Jdbi release"
	@echo " * release-docs         - deploy release documentation"
