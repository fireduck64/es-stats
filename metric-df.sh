#!/bin/bash

cd ~/es-stats.git

./df-json.sh | ./addtime.py | ./addtag.py host $(hostname) | ./sendjson.sh diskspace




