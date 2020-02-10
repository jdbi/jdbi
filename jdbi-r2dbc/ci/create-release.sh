#!/usr/bin/env bash

set -euo pipefail

RELEASE=$1
SNAPSHOT=$2

./mvnw versions:set -DnewVersion=$RELEASE -DgenerateBackupPoms=false
git add .
git commit --message "v$RELEASE Release"
git tag -s v$RELEASE -m "v$RELEASE"

git reset --hard HEAD^1
./mvnw versions:set -DnewVersion=$SNAPSHOT -DgenerateBackupPoms=false
git add .
git commit --message "v$SNAPSHOT Development"
