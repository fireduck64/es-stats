#!/bin/bash

sensors -j | ./addtime.py | ./addtag.py host `hostname` | tee /dev/stderr | ./sendjson.sh lmsensors 
