#!/usr/bin/python3

import sys, json;
import datetime;


if (len(sys.argv) % 2) != 1:
  print("Must give even number of arguments.  tag value tag value");
  sys.exit(1);


data = json.load(sys.stdin);

for x in range(1, len(sys.argv), 2):
  key=sys.argv[x];
  value=sys.argv[x+1];
  data[key]=value;



print(json.dumps(data));




