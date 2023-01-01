#!/bin/bash

for dev in `smartctl --scan|cut -d " " -f 1`
do
  smartctl -i $dev|grep "SMART support is: Enabled" >>/dev/null
  f=$?
  if [ $f -eq 0 ]
  then
    echo $dev is smart
    smartctl -a $dev | grep Reallocated_Event_Count

  fi
done
