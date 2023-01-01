#!/bin/bash

export PATH=$PATH:/home/nerd/es-stats.git

passed=0

for dev in $(smart-mon-list.sh)
do
  status=$(smartctl -j -a $dev|jq .smart_status.passed)
  if [ "$status" == "true" ]
  then
    passed=$((passed+1))
  fi
done

echo "{}" | addtag.py passed $passed

