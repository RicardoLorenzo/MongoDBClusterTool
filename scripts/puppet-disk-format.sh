#!/bin/bash
MOUNT_DIRECTORY="/mnt/mongodb"
RAID_TYPE="none"
FS_TYPE="ext4"

if [ -e /etc/mongodb-shards.conf ]; then
. /etc/mongodb-shards.conf
fi

getSystemDisks() {
  local DISKS=""
  for DISK in $(cat /proc/partitions | awk '{ print $4 }' | grep '^sd[b-z]$'); do
    DISK=${DISK//[$'\t\r\n']}
    if [ -z "$(lsblk -ln /dev/${DISK} | awk '{ print $6 }' | grep 'part')" ]; then
      parted -s /dev/$DISK mklabel gpt > /dev/null 2>&1
      parted -s /dev/$DISK mkpart primary 0 100% > /dev/null 2>&1
      parted -s /dev/$DISK align-check optimal 1 > /dev/null 2>&1
    fi
    DISKS="$DISKS${DISK} "
  done
  echo $DISKS
}

mountDisk() {
    local DISK=$1
    local DISK_DEV=$1
    local FSTYPE=$2
    if [ "md0" = "$DISK_DEV" ]; then
        DISK_DEV=/dev/${DISK_DEV}
    else
        DISK_DEV=/dev/${DISK_DEV}1
    fi
    if [ -z "$(mount | grep $MOUNT_DIRECTORY/$DISK)" ]; then
        udevadm test $(udevadm info -a -n ${DISK_DEV}) > /dev/null 2>&1
        local DISK_UUID=$(blkid -o export ${DISK_DEV} | grep '^UUID=')
        DISK_UUID=${DISK_UUID##*\=}
        if [ -z "$(cat /etc/fstab | grep UUID=$DISK_UUID)" ]; then
            echo "UUID=$DISK_UUID  $MOUNT_DIRECTORY/$DISK  $FSTYPE  defaults  0  0" >> /etc/fstab
        fi
        if ! [ -e "$MOUNT_DIRECTORY/$DISK" ]; then
            mkdir -p $MOUNT_DIRECTORY/$DISK > /dev/null 2>&1
        fi
        mount $MOUNT_DIRECTORY/$DISK
        if [ "$(stat -c %U $MOUNT_DIRECTORY/$DISK)" != "mongodb" ]; then
            chown mongodb:mongodb $MOUNT_DIRECTORY/$DISK -R > /dev/null 2>&1
        fi
    fi
}

createRaidDisk() {
    if ! [ -e /dev/md0 ]; then
        local DISKS=
        for DISK in $@; do
            DISKS="/dev/${DISK}1 "
        done
        mdadm --create /dev/md0 --level=stripe --raid-devices=$# $DISKS
    fi
}

formatDisks() {
    case "$FS_TYPE" in
        ext4)
            if [ "$RAID_TYPE" = "raid0" ]; then
                createRaidDisk $@
                if [ -z "$(file -s /dev/md0 | grep ext4)" ]; then
                    mkfs.ext4 /dev/md0
                fi
                mountDisk md0 ext4
            else
                for DISK in $@; do
                    if [ -z "$(file -s /dev/${DISK}1 | grep ext4)" ]; then
                        mkfs.ext4 /dev/${DISK}1
                    fi
                    mountDisk $DISK ext4
                done
            fi
            ;;
        xfs)
            if [ "$RAID_TYPE" = "raid0" ]; then
                createRaidDisk $@
                if [ -z "$(file -s /dev/md0 | grep XFS)" ]; then
                    mkfs.xfs /dev/md0
                fi
                mountDisk md0 xfs
            else
                for DISK in $@; do
                    if [ -z "$(file -s /dev/${DISK}1 | grep XFS)" ]; then
                        mkfs.xfs /dev/${DISK}1
                    fi
                    mountDisk $DISK xfs
                done
            fi
            ;;
        btrfs)
            if [ "$RAID_TYPE" = "raid0" ]; then
                local DISKS=""
                for DISK in $@; do
                    DISKS="$DISKS/dev/${DISK}1 "
                done
                mkfs.btrfs -d raid0 $DISKS
                mountDisk $1 btrfs
            else
                for DISK in $@; do
                    mkfs.btrfs /dev/${DISK}1
                    mountDisk $DISK btrfs
                done
            fi
            ;;
        *)
            exit 1
            ;;
    esac
}

DISKS=$(getSystemDisks)
formatDisks $DISKS