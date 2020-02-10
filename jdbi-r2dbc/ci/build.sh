#!/usr/bin/env bash

set -euo pipefail

source r2dbc-client/ci/docker-lib.sh
start_docker "3" "3" "" ""

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

r2dbc_client_artifactory=$(pwd)/r2dbc-client-artifactory
r2dbc_h2_artifactory=$(pwd)/r2dbc-h2-artifactory
r2dbc_mssql_artifactory=$(pwd)/r2dbc-mssql-artifactory
r2dbc_pool_artifactory=$(pwd)/r2dbc-pool-artifactory
r2dbc_postgresql_artifactory=$(pwd)/r2dbc-postgresql-artifactory
r2dbc_spi_artifactory=$(pwd)/r2dbc-spi-artifactory

rm -rf $HOME/.m2/repository/io/r2dbc 2> /dev/null || :

cd r2dbc-client
./mvnw deploy \
    -DaltDeploymentRepository=distribution::default::file://${r2dbc_client_artifactory} \
    -Dr2dbcH2Artifactory=file://${r2dbc_h2_artifactory} \
    -Dr2dbcMssqlArtifactory=file://${r2dbc_mssql_artifactory} \
    -Dr2dbcPoolArtifactory=file://${r2dbc_pool_artifactory} \
    -Dr2dbcPostgresqlArtifactory=file://${r2dbc_postgresql_artifactory} \
    -Dr2dbcSpiArtifactory=file://${r2dbc_spi_artifactory}
