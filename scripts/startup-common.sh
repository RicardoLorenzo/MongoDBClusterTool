#!/bin/bash

function runCommand() {
  for i in $(seq 1 1 10); do
    $@;
    if [ "$?" -eq 0 ]; then
      break;
    fi
  done
}

function runCommandAsUser() {
  for i in $(seq 1 1 10); do
    local CMD="${@:2}"
    su - $1 -c "$CMD"
    if [ "$?" -eq 0 ]; then
      break;
    fi
  done
}

function checkConnection() {
  for i in $(seq 1 1 100); do
    wget -O /dev/null $1;
    if [ "$?" -eq 0 ]; then
      break;
    fi
    sleep 4;
  done
  sleep 4;
}

function installPackage() {
  PKG=$(dpkg -l | awk '{ print $2 }' | grep "^$1:*")
  if [ -z "$PKG" ]; then
    export DEBIAN_FRONTEND=noninteractive
    runCommand apt-get update
    runCommand apt-get --no-install-recommends install -o DPkg::options::="--force-confdef" \
     -o DPkg::options::="--force-confold" -o Dpkg::Options::="--force-overwrite" -y $1
  fi
  if [ -n "$2" ]; then
    $2
  fi
}

function setProxy() {
  export http_proxy=http://$1
  echo "Acquire::http::Proxy \"http://$1\";
" > /etc/apt/apt.conf.d/90proxy
}

function makeDirectory() {
  if ! [ -e "$1" ]; then
    mkdir -p $1
  fi
}
