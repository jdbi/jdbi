#!/usr/bin/env bash

# This script will "patch" an issue with Concourse (https://github.com/concourse/concourse/issues/403)
# where the resources provided to a job/task are owned by the `root` user. However, it is best to run NodeJS
# with a non-root user as there are issues where it will refuse to run lifecycle scripts like `pre` and `post`.

if [ -z "$1" ]; then
  echo ERROR: You must provide the script to run with permission-patch as a parameter.
  exit 1
fi

chown -R test:test /tmp/build

exec sudo -u test $1