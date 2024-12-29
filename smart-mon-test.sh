#!/bin/bash

export PATH=$PATH:/home/clash/es-stats.git:/usr/sbin

for dev in $(smart-mon-list.sh)
do
  smartctl -t short $dev
done

