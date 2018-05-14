#!/usr/bin/env bash

cp src/build/travis-toolchains.xml ~/.m2/toolchains.xml

if [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
  openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in src/build/travis-settings.xml.enc -out ~/.m2/settings.xml -d
  openssl aes-256-cbc -K $secure_files_key -iv $secure_files_iv -in src/build/travis-settings-security.xml.enc -out ~/.m2/settings-security.xml -d
fi
