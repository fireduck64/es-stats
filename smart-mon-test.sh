#!/bin/bash

export PATH=$PATH:/home/nerd/es-stats.git:/usr/sbin

for dev in $(smart-mon-list.sh)
do
  smartctl -t short $dev
done

