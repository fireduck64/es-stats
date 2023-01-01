#!/bin/bash

export PATH=$PATH:/home/nerd/es-stats.git:/usr/sbin

passed=0

for dev in $(smart-mon-list.sh)
do
  #status=$(smartctl -j -a $dev|jq .smart_status.passed)
  status=$(smartctl -a /dev/sdb -j|jq .smartctl.exit_status)
  if [ "$status" == "0" ]
  then
    passed=$((passed+1))
  fi
  echo "$dev - $status"
done

echo "{}" | addtag.py passed $passed host $HOSTNAME | addtime.py | sendjson.sh smartmon



