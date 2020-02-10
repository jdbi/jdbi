#!/usr/bin/env bash

set -euo pipefail

source r2dbc-client/ci/docker-lib.sh
start_docker "3" "3" "" ""

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

r2dbc_client_artifactory=$(pwd)/r2dbc-client-artifactory

rm -rf $HOME/.m2/repository/io/r2dbc 2> /dev/null || :

cd r2dbc-client
./mvnw deploy \
    -DaltDeploymentRepository=distribution::default::file://${r2dbc_client_artifactory}
