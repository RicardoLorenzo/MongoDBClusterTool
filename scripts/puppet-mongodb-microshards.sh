#!/bin/bash
### BEGIN INIT INFO
# Provides:          mongod-microshards
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: MongoDB database microshards
# Description:       This service provide the MongoDB database service
### END INIT INFO

# Author: Ricardo Lorenzo <ricardo.lorenzo@mongodb.com>
#

# Get lsb functions
. /lib/lsb/init-functions

MOUNT_DIRECTORY="/mnt/mongodb"
PROCESSES=1
RAID_TYPE="none"
FS_TYPE="ext4"
DESC="MongoDB database"
DAEMON=/usr/bin/mongod
DAEMON_USER=mongodb
DAEMON_PORT=27040
DAEMON_OPTS=''

if [ -e /etc/mongodb-shards.conf ]; then
. /etc/mongodb-shards.conf
fi

boolean() {
  if [ -z '$1' ]; then
    echo false
    return
  fi
  echo
  case "$1" in
    true|yes|1) echo true
      ;;
    false|no|0) echo false
      ;;
   esac
}

config_check() {
  if [ -n "$1" -a -n "$2" ]; then
    echo '$2 $1'
  fi
}

config_boolean_check() {
  if [ "$(boolean $1)" ]; then
    echo $2
  else
    echo ''
  fi
}

get_pid() {
  if [ -z "$1" ]; then
    return
  fi
  local PID=$(ps ax | grep "dbpath /mnt/mongodb/$1" | grep -v grep | awk '{ print $1 }')
  echo $PID
}

get_daemon_options() {
  if [ -z "$1" ]; then
    return
  fi

  local INDEX=1
  declare -a MOUNT_DIRS;
  MOUNT_DIRS[0]=$MOUNT_DIRECTORY
  MOUNT_DIRS[$INDEX]=
  for DIR in $(ls $MOUNT_DIRECTORY); do
    MOUNT_DIRS[$INDEX]=$DIR
    INDEX=$(( $INDEX + 1 ))
  done

  DAEMON_OPTS="--port $DAEMON_PORT"
  if [ "$RAID_TYPE" = "raid0" ]; then
     if [ "$FS_TYPE" = "btrfs" ]; then
       if ! [ -e "$MOUNT_DIRECTORY/${MOUNT_DIRS[1]}/$1" ]; then
         mkdir -p $MOUNT_DIRECTORY/${MOUNT_DIRS[1]}/$1
         chown $DAEMON_USER:$DAEMON_USER $MOUNT_DIRECTORY/${MOUNT_DIRS[1]}/$1
       fi
       DAEMON_OPTS="$DAEMON_OPTS --dbpath $MOUNT_DIRECTORY/${MOUNT_DIRS[1]}/$1"
     else
       if ! [ -e "$MOUNT_DIRECTORY/md0/$1" ]; then
         mkdir -p $MOUNT_DIRECTORY/md0/$1
         chown $DAEMON_USER:$DAEMON_USER $MOUNT_DIRECTORY/md0/$1
       fi
       DAEMON_OPTS="$DAEMON_OPTS --dbpath $MOUNT_DIRECTORY/md0/$1"
     fi
  else
     DAEMON_OPTS="$DAEMON_OPTS --dbpath $MOUNT_DIRECTORY/${MOUNT_DIRS[$1]}"
  fi
  DAEMON_OPTS="$DAEMON_OPTS --logpath /tmp/mongodb-shard-$1.log"
  DAEMON_OPTS="$DAEMON_OPTS --shardsvr"
  DAEMON_OPTS="$DAEMON_OPTS --journal"
  DAEMON_OPTS="$DAEMON_OPTS --directoryperdb --logappend"

  DAEMON_PORT=$(( $DAEMON_PORT + 1 ))
}

configure_cgroups() {
  if ! [ -e "/cgroups" ]; then
    mkdir -p /cgroups
  fi
  if [ $(cat /proc/cgroups | grep memory | awk '{ print $4 }') -eq 0 ]; then
    echo "Error: memory module is not enabled for cgroups"
    exit 1
  fi

  # Getting the memory size in GB
  MS=$(cat /proc/meminfo | grep MemTotal)
  MS=${MS##*\:}
  MS=$(echo $MS | awk '{ print $1 }')
  MS=$(( $MS / 1024 ))

  # Getting the memory per shard
  MS=$(( $MS - 1 ))
  MS=$(( $MS / $PROCESSES  ))

  MS=$(( $MS * 1024 * 1024 ))

  # Getting the number of processors
  local PC=$(cat /proc/cpuinfo | grep "^processor" | wc -l)

  # Getting the number of processors per shard
  local SC=$(( 1024 / $PROCESSES ))
  SC=${SC%.*}
  if [ "$SC" -lt 1 ]; then
    SC=1
  fi

  PC=$(( $PC - 1 ))

  local CGROUPS_CONF="{
  perm {
    admin {
      uid = root;
      gid = root;
    }
    task {
      uid = mongodb;
      gid = mongodb;
    }
  }

  memory {
    memory.limit_in_bytes = ${MS};
  }

  cpu {
    cpu.shares = ${SC};
  }

  cpuset {
    cpuset.cpus = 0-${PC};
    cpuset.mems = 0;
  }
}

mount {
  cpu = /cgroups/cpu;
  cpuset = /cgroups/cpuset;
  memory = /cgroups/memory;
}
"
  echo "" > /etc/cgconfig.conf
  for PROCESS in $(seq 1 1 $PROCESSES); do
    echo -n "group mongodb-$PROCESS " >> /etc/cgconfig.conf
    echo "$CGROUPS_CONF" >> /etc/cgconfig.conf
  done
  umount -t cgroup -a > /dev/null 2>&1
  cgconfigparser -l /etc/cgconfig.conf > /dev/null 2>&1
}

start_microshards() {
  configure_cgroups
  for PROCESS in $(seq 1 1 $PROCESSES); do
    local PIDFILE=/var/run/mongod_$PROCESS.pid
    if ! [ -e "$PIDFILE" ]; then
      echo "$RANDOM" > $PIDFILE
    fi
    log_daemon_msg "Starting $DESC [$PROCESS]" "mongod"
    get_daemon_options $PROCESS
    DAEMON_OPTS="-g *:mongodb-$PROCESS $DAEMON $DAEMON_OPTS"
    start-stop-daemon --start --background --chuid $DAEMON_USER --pidfile $PIDFILE --exec /usr/bin/cgexec -- $DAEMON_OPTS
    log_end_msg $?
    sleep 1
    PID=$(get_pid $DISK)
    if ! [ -z '$PID' ]; then
      echo $PID > $PIDFILE
    else
      echo "ERROR: Not started"
      tail -100 /tmp/mongodb-shard-$PROCESS.log
      exit 1
    fi
  done
}

stop_microshards() {
  for PIDFILE in $(ls /var/run/mongod_*.pid); do
    PID=$(cat $PIDFILE)
    if [ -z "$PID" ]; then
      continue
    fi
    log_daemon_msg "Stopping $DESC [pid: $PID]" "mongod"
    start-stop-daemon --stop --signal TERM --chuid $DAEMON_USER --pidfile $PIDFILE
    log_end_msg $?
  done
}

case "$1" in
start)
        start_microshards
        ;;
stop)
        stop_microshards
        ;;
restart)
        $0 stop
        $0 start
        ;;
*)      log_action_msg 'Usage: /etc/init.d/mongodb-microshards {start|stop|status|restart|reload|force-reload}'
        exit 2
        ;;
esac
exit 0