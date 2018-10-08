LOG_FILE=${LOG_FILE:-/tmp/docker.log}
SKIP_PRIVILEGED=${SKIP_PRIVILEGED:-false}
STARTUP_TIMEOUT=${STARTUP_TIMEOUT:-120}

sanitize_cgroups() {
  mkdir -p /sys/fs/cgroup
  mountpoint -q /sys/fs/cgroup || \
    mount -t tmpfs -o uid=0,gid=0,mode=0755 cgroup /sys/fs/cgroup

  mount -o remount,rw /sys/fs/cgroup

  sed -e 1d /proc/cgroups | while read sys hierarchy num enabled; do
    if [ "$enabled" != "1" ]; then
      # subsystem disabled; skip
      continue
    fi

    grouping="$(cat /proc/self/cgroup | cut -d: -f2 | grep "\\<$sys\\>")" || true
    if [ -z "$grouping" ]; then
      # subsystem not mounted anywhere; mount it on its own
      grouping="$sys"
    fi

    mountpoint="/sys/fs/cgroup/$grouping"

    mkdir -p "$mountpoint"

    # clear out existing mount to make sure new one is read-write
    if mountpoint -q "$mountpoint"; then
      umount "$mountpoint"
    fi

    mount -n -t cgroup -o "$grouping" cgroup "$mountpoint"

    if [ "$grouping" != "$sys" ]; then
      if [ -L "/sys/fs/cgroup/$sys" ]; then
        rm "/sys/fs/cgroup/$sys"
      fi

      ln -s "$mountpoint" "/sys/fs/cgroup/$sys"
    fi
  done

  if ! test -e /sys/fs/cgroup/systemd ; then
    mkdir /sys/fs/cgroup/systemd
    mount -t cgroup -o none,name=systemd none /sys/fs/cgroup/systemd
  fi
}

start_docker() {
  mkdir -p /var/log
  mkdir -p /var/run

  if [ "$SKIP_PRIVILEGED" = "false" ]; then
    sanitize_cgroups

    # check for /proc/sys being mounted readonly, as systemd does
    if grep '/proc/sys\s\+\w\+\s\+ro,' /proc/mounts >/dev/null; then
      mount -o remount,rw /proc/sys
    fi
  fi

  local mtu=$(cat /sys/class/net/$(ip route get 8.8.8.8|awk '{ print $5 }')/mtu)
  local server_args="--mtu ${mtu}"
  local registry=""

  server_args="${server_args} --max-concurrent-downloads=$1 --max-concurrent-uploads=$2"

  for registry in $3; do
    server_args="${server_args} --insecure-registry ${registry}"
  done

  if [ -n "$4" ]; then
    server_args="${server_args} --registry-mirror $4"
  fi

  try_start() {
    dockerd --data-root /scratch/docker ${server_args} >$LOG_FILE 2>&1 &
    echo $! > /tmp/docker.pid

    sleep 1

    echo waiting for docker to come up...
    until docker info >/dev/null 2>&1; do
      sleep 1
      if ! kill -0 "$(cat /tmp/docker.pid)" 2>/dev/null; then
        return 1
      fi
    done
  }

  export server_args LOG_FILE
  declare -fx try_start
  trap stop_docker EXIT

  if ! timeout ${STARTUP_TIMEOUT} bash -ce 'while true; do try_start && break; done'; then
    echo Docker failed to start within ${STARTUP_TIMEOUT} seconds.
    return 1
  fi
}

stop_docker() {
  local pid=$(cat /tmp/docker.pid)
  if [ -z "$pid" ]; then
    return 0
  fi

  kill -TERM $pid
}

log_in() {
  local username="$1"
  local password="$2"
  local registry="$3"

  if [ -n "${username}" ] && [ -n "${password}" ]; then
    echo "${password}" | docker login -u "${username}" --password-stdin ${registry}
  else
    mkdir -p ~/.docker
    echo '{"credsStore":"ecr-login"}' >> ~/.docker/config.json
  fi
}

private_registry() {
  local repository="${1}"

  if echo "${repository}" | fgrep -q '/' ; then
    local registry="$(extract_registry "${repository}")"
    if echo "${registry}" | fgrep -q '.' ; then
      return 0
    fi
  fi

  return 1
}

extract_registry() {
  local repository="${1}"

  echo "${repository}" | cut -d/ -f1
}

extract_repository() {
  local long_repository="${1}"

  echo "${long_repository}" | cut -d/ -f2-
}

image_from_tag() {
  docker images --no-trunc "$1" | awk "{if (\$2 == \"$2\") print \$3}"
}

image_from_digest() {
  docker images --no-trunc --digests "$1" | awk "{if (\$3 == \"$2\") print \$4}"
}

certs_to_file() {
  local raw_ca_certs="${1}"
  local cert_count="$(echo $raw_ca_certs | jq -r '. | length')"

  for i in $(seq 0 $(expr "$cert_count" - 1));
  do
    local cert_dir="/etc/docker/certs.d/$(echo $raw_ca_certs | jq -r .[$i].domain)"
    mkdir -p "$cert_dir"
    echo $raw_ca_certs | jq -r .[$i].cert >> "${cert_dir}/ca.crt"
  done
}

set_client_certs() {
  local raw_client_certs="${1}"
  local cert_count="$(echo $raw_client_certs | jq -r '. | length')"

  for i in $(seq 0 $(expr "$cert_count" - 1));
  do
    local cert_dir="/etc/docker/certs.d/$(echo $raw_client_certs | jq -r .[$i].domain)"
    [ -d "$cert_dir" ] || mkdir -p "$cert_dir"
    echo $raw_client_certs | jq -r .[$i].cert >> "${cert_dir}/client.cert"
    echo $raw_client_certs | jq -r .[$i].key >> "${cert_dir}/client.key"
  done
}

docker_pull() {
  GREEN='\033[0;32m'
  RED='\033[0;31m'
  NC='\033[0m' # No Color

  pull_attempt=1
  max_attempts=3
  while [ "$pull_attempt" -le "$max_attempts" ]; do
    printf "Pulling ${GREEN}%s${NC}" "$1"

    if [ "$pull_attempt" != "1" ]; then
      printf " (attempt %s of %s)" "$pull_attempt" "$max_attempts"
    fi

    printf "...\n"

    if docker pull "$1"; then
      printf "\nSuccessfully pulled ${GREEN}%s${NC}.\n\n" "$1"
      return
    fi

    echo

    pull_attempt=$(expr "$pull_attempt" + 1)
  done

  printf "\n${RED}Failed to pull image %s.${NC}" "$1"
  exit 1
}
