#!/bin/bash

python_script=$(cat <<'EOF'
import sys, json


disks=dict()

for line in sys.stdin.readlines():
  mount, total, used, avail = line.rstrip(';').split()
  disks[mount]=dict(spacetotal=int(total), spaceused=int(used), spaceavail=int(avail))
sys.stdout.write(json.dumps(disks))
EOF
)


df -P | awk '/^\// { print $6" "$2" "$3" "$4"" }' | python3 -c "$python_script"

