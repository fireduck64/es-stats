#!/usr/bin/env bash
set -eu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="$SCRIPT_DIR:$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

for dev in $(smartctl --scan | cut -d " " -f 1)
do
  if smartctl -i "$dev" 2>/dev/null | grep "SMART support is: Enabled" >/dev/null
  then
    echo "$dev"
  fi
done
