#!/usr/bin/env bash
set -eu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="$SCRIPT_DIR:$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

for dev in $(smartctl --scan | cut -d " " -f 1)
do
  # On FreeBSD 14+, smartctl --scan lists each NVMe drive twice: /dev/ndaN (CAM
  # wrapper, which fails smartctl INQUIRY) and /dev/nvmeN (native controller).
  case "$dev" in
    /dev/nda[0-9]*) continue ;;
  esac
  echo "$dev"
done
