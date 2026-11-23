#!/usr/bin/env python3

import argparse
import json
import os
import sys

ZERO_TOLERANCE_ATA_ATTRS = {
    5: "Reallocated_Sector_Ct",
    187: "Reported_Uncorrect",
    197: "Current_Pending_Sector",
    198: "Offline_Uncorrectable",
}

EXIT_STATUS_BITS = (
    (1, "cmdline_parse"),
    (2, "device_open"),
    (4, "smart_cmd_error"),
    (8, "disk_failing"),
    (16, "prefail_below_thresh"),
    (64, "error_log"),
    (128, "selftest_error"),
)


def evaluate_drive(data, max_temp=50):
    if not isinstance(data, dict):
        return False, "FAIL (invalid JSON root)"

    reasons = []
    notes = []

    exit_status = data.get("smartctl", {}).get("exit_status")
    if not isinstance(exit_status, int) or isinstance(exit_status, bool):
        reasons.append(f"missing smartctl.exit_status ({exit_status})")
        exit_status = 0
    else:
        non_32 = exit_status & ~32
        if non_32 != 0:
            tripped = [name for bit, name in EXIT_STATUS_BITS if non_32 & bit]
            detail = "+".join(tripped) if tripped else str(non_32)
            reasons.append(f"exit_status={exit_status} ({detail})")

    passed = data.get("smart_status", {}).get("passed")
    if passed is not True:
        reasons.append(f"smart_status.passed={passed}")

    excused_past_190 = False
    other_attr_failed = False

    attrs = data.get("ata_smart_attributes", {}).get("table", [])
    if isinstance(attrs, list):
        for attr in attrs:
            if not isinstance(attr, dict):
                continue
            aid = attr.get("id")
            aname = attr.get("name") or ZERO_TOLERANCE_ATA_ATTRS.get(aid, f"id_{aid}")
            when_failed = (attr.get("when_failed") or "").strip()

            if when_failed:
                if aid == 190 and when_failed.lower() in ("past", "in_the_past"):
                    excused_past_190 = True
                else:
                    other_attr_failed = True
                    reasons.append(f"attr {aid} {aname} failed={when_failed}")

            if aid in ZERO_TOLERANCE_ATA_ATTRS:
                raw_val = attr.get("raw", {}).get("value", 0)
                if not isinstance(raw_val, int) or isinstance(raw_val, bool) or raw_val != 0:
                    reasons.append(f"attr {aid} {aname} raw={raw_val}")

    if (exit_status & 32) != 0 and not excused_past_190 and not other_attr_failed:
        reasons.append("exit_status bit 32 set without attr 190 past failure")

    pending_defects = data.get("ata_pending_defects_log", {}).get("count", 0)
    if not isinstance(pending_defects, int) or isinstance(pending_defects, bool) or pending_defects != 0:
        reasons.append(f"pending_defects={pending_defects}")

    nvme_health = data.get("nvme_smart_health_information_log")
    if isinstance(nvme_health, dict):
        crit_warn = nvme_health.get("critical_warning", 0)
        if crit_warn != 0:
            reasons.append(f"nvme critical_warning={crit_warn}")
        media_errs = nvme_health.get("media_errors", 0)
        if media_errs != 0:
            reasons.append(f"nvme media_errors={media_errs}")

    scsi_defects = data.get("scsi_grown_defect_list")
    if isinstance(scsi_defects, int) and not isinstance(scsi_defects, bool) and scsi_defects > 0:
        reasons.append(f"scsi_grown_defect_list={scsi_defects}")

    temp = data.get("temperature", {}).get("current")
    if isinstance(temp, (int, float)) and not isinstance(temp, bool):
        if temp > max_temp:
            reasons.append(f"temp {temp}C > {max_temp}C")
        else:
            notes.append(f"{temp}C")

    if excused_past_190:
        notes.append("ignored past Airflow_Temperature_Cel")

    if reasons:
        return False, f"FAIL ({'; '.join(reasons)})"

    if notes:
        return True, f"OK ({', '.join(notes)})"
    return True, "OK"


def main(argv=None):
    default_temp = int(os.environ.get("SMART_MAX_TEMP", "50"))
    parser = argparse.ArgumentParser(description="Evaluate smartctl -j output for a single drive.")
    parser.add_argument(
        "--max-temp",
        type=int,
        default=default_temp,
        help="Maximum allowed current temperature in Celsius (default: %(default)s)",
    )
    parser.add_argument(
        "json_file",
        nargs="?",
        help="Optional path to smartctl JSON file (defaults to stdin)",
    )
    args = parser.parse_args(argv)

    try:
        if args.json_file:
            with open(args.json_file, "r", encoding="utf-8") as fh:
                raw = fh.read()
        else:
            raw = sys.stdin.read()
        data = json.loads(raw)
    except Exception as exc:
        print(f"FAIL (invalid smartctl JSON: {exc})")
        return 1

    ok, message = evaluate_drive(data, max_temp=args.max_temp)
    print(message)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
