#!/usr/bin/env bash
set -eu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="$SCRIPT_DIR:$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

passed=0

for dev in $(smart-mon-list.sh)
do
  json_out=$(smartctl -a "$dev" -j || true)
  if status_msg=$(printf '%s\n' "$json_out" | smart-mon-check.py)
  then
    passed=$((passed + 1))
  fi
  echo "$dev - $status_msg"
done

echo "{}" | addtag.py passed "$passed" host "${HOSTNAME:-$(hostname)}" | addtime.py | sendjson.sh smartmon



