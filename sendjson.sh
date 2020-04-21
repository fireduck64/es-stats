#!/bin/bash


zone="$1"
if [ -z "$zone" ]
then
  echo "syntax: $0 zone"
  exit 1
fi

set -eu

d=$(date +%Y.%m.%d)

curl -X POST "http://10.134.32.0:9200/${zone}-${d}/_doc" -H 'Content-Type: application/json' -d@/dev/stdin |jq .

echo ""



