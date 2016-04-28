#!/bin/sh
exec mvn clean deploy -Dmaven.deploy.skip=true -Ppublish-docs
