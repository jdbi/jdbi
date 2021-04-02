#!/bin/sh
set -xe

OPTS="-DskipTests=true -B"

exec mvn ${OPTS} verify
