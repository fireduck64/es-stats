#!/bin/bash

cd ~/es-stats.git

/usr/sbin/zpool status | /home/nerd/bin/zfs_scan | ./addtime.py | ./addtag.py host $(hostname) | ./sendjson.sh zfs





