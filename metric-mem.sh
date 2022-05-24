#!/bin/bash

cd ~/es-stats.git

avail=$(free|grep "Mem"|tr -s " " " "|cut -d " " -f 7)

echo "{}" | ./addtime.py | ./addtag.py host $(hostname) mem_avail $avail
echo "{}" | ./addtime.py | ./addtag.py host $(hostname) mem_avail $avail | ./sendjson.sh memory





