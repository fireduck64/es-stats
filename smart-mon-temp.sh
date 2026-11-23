#!/usr/bin/env bash
set -eu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="$SCRIPT_DIR:$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

for dev in $(smart-mon-list.sh)
do
  echo -n "$dev - "
  { smartctl -j -x "$dev" || true; } | jq .temperature.current
done

