#!/usr/bin/env bash

export PATH="$PATH:/usr/local/bin:/usr/bin:/bin"


zone="$1"
if [ -z "$zone" ]
then
  echo "syntax: $0 zone"
  exit 1
fi

set -eu

d=$(date +%Y.%m.%d)

curl -X POST "http://metrics.1209k.com:9200/${zone}-${d}/_doc" -H 'Content-Type: application/json' -d@/dev/stdin |jq .

echo ""



