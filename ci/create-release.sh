#!/usr/bin/env sh

set -e -u

RELEASE=$1
SNAPSHOT=$2

git branch -f release
git checkout release
./mvnw versions:set -DnewVersion=$RELEASE -DgenerateBackupPoms=false
git add .
git commit --message "v$RELEASE Release"
git tag -s v$RELEASE -m "v$RELEASE"

git master
./mvnw versions:set -DnewVersion=$SNAPSHOT -DgenerateBackupPoms=false
git add .
git commit --message "v$SNAPSHOT Development"
