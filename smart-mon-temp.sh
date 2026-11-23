#!/usr/local/bin/bash

export PATH=$PATH:/root/es-stats.git

cd /root/es-stats.git


passed=0

for dev in $(smart-mon-list.sh)
do
  echo -n "$dev - "
  smartctl -j -x $dev |jq .temperature.current
done

