#!/usr/local/bin/bash

export PATH=$PATH:/root/es-stats.git

cd /root/es-stats.git



for dev in $(smart-mon-list.sh)
do
  echo "------------------------------------------"
  echo "Report for $dev"
  smartctl -j -x $dev
done



