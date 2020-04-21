#!/usr/bin/python3

import sys, json;
import datetime;

data = json.load(sys.stdin);

data["timestamp"]=datetime.datetime.utcnow().isoformat();


print(json.dumps(data));




