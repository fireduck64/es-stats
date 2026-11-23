#!/usr/bin/env python3

import sys

sys.dont_write_bytecode = True

import copy
import importlib.util
import json
import os
import stat
import subprocess
import tempfile
import unittest

REPO_DIR = os.path.dirname(os.path.abspath(__file__))
CHECK_PATH = os.path.join(REPO_DIR, "smart-mon-check.py")

spec = importlib.util.spec_from_file_location("smart_mon_check", CHECK_PATH)
smart_mon_check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smart_mon_check)


def make_healthy_ata_drive(temp=38):
    return {
        "smartctl": {"exit_status": 0},
        "smart_status": {"passed": True},
        "temperature": {"current": temp},
        "ata_smart_attributes": {
            "table": [
                {
                    "id": 1,
                    "name": "Raw_Read_Error_Rate",
                    "value": 80,
                    "worst": 64,
                    "thresh": 44,
                    "when_failed": "",
                    "raw": {"value": 107979040},
                },
                {
                    "id": 5,
                    "name": "Reallocated_Sector_Ct",
                    "value": 100,
                    "worst": 100,
                    "thresh": 10,
                    "when_failed": "",
                    "raw": {"value": 0},
                },
                {
                    "id": 10,
                    "name": "Spin_Retry_Count",
                    "value": 100,
                    "worst": 100,
                    "thresh": 97,
                    "when_failed": "",
                    "raw": {"value": 0},
                },
                {
                    "id": 187,
                    "name": "Reported_Uncorrect",
                    "value": 100,
                    "worst": 100,
                    "thresh": 0,
                    "when_failed": "",
                    "raw": {"value": 0},
                },
                {
                    "id": 190,
                    "name": "Airflow_Temperature_Cel",
                    "value": 100 - temp,
                    "worst": 50,
                    "thresh": 40,
                    "when_failed": "",
                    "raw": {"value": temp},
                },
                {
                    "id": 197,
                    "name": "Current_Pending_Sector",
                    "value": 100,
                    "worst": 100,
                    "thresh": 0,
                    "when_failed": "",
                    "raw": {"value": 0},
                },
                {
                    "id": 198,
                    "name": "Offline_Uncorrectable",
                    "value": 100,
                    "worst": 100,
                    "thresh": 0,
                    "when_failed": "",
                    "raw": {"value": 0},
                },
            ]
        },
        "ata_pending_defects_log": {"count": 0},
    }


class SmartMonCheckTests(unittest.TestCase):
    def test_healthy_drive_passes(self):
        ok, msg = smart_mon_check.evaluate_drive(make_healthy_ata_drive(temp=38))
        self.assertTrue(ok)
        self.assertEqual(msg, "OK (38C)")

    def test_temperature_boundary_50c(self):
        ok_50, msg_50 = smart_mon_check.evaluate_drive(make_healthy_ata_drive(temp=50))
        self.assertTrue(ok_50)
        self.assertEqual(msg_50, "OK (50C)")

        ok_51, msg_51 = smart_mon_check.evaluate_drive(make_healthy_ata_drive(temp=51))
        self.assertFalse(ok_51)
        self.assertIn("temp 51C > 50C", msg_51)

    def test_historical_airflow_temp_scar_passes_when_cool(self):
        drive = make_healthy_ata_drive(temp=38)
        drive["smartctl"]["exit_status"] = 32
        for attr in drive["ata_smart_attributes"]["table"]:
            if attr["id"] == 190:
                attr["worst"] = 39
                attr["when_failed"] = "past"

        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertTrue(ok)
        self.assertEqual(msg, "OK (38C, ignored past Airflow_Temperature_Cel)")

    def test_historical_airflow_temp_scar_still_fails_when_hot(self):
        drive = make_healthy_ata_drive(temp=56)
        drive["smartctl"]["exit_status"] = 32
        for attr in drive["ata_smart_attributes"]["table"]:
            if attr["id"] == 190:
                attr["worst"] = 39
                attr["when_failed"] = "past"

        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("temp 56C > 50C", msg)

    def test_active_airflow_temp_failure_now_fails(self):
        drive = make_healthy_ata_drive(temp=60)
        drive["smartctl"]["exit_status"] = 32
        for attr in drive["ata_smart_attributes"]["table"]:
            if attr["id"] == 190:
                attr["value"] = 40
                attr["worst"] = 40
                attr["when_failed"] = "now"

        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("attr 190 Airflow_Temperature_Cel failed=now", msg)
        self.assertIn("temp 60C > 50C", msg)

    def test_non_190_past_failure_is_not_ignored(self):
        drive = make_healthy_ata_drive(temp=38)
        drive["smartctl"]["exit_status"] = 32
        for attr in drive["ata_smart_attributes"]["table"]:
            if attr["id"] == 10:
                attr["worst"] = 95
                attr["when_failed"] = "past"

        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("attr 10 Spin_Retry_Count failed=past", msg)

    def test_unexplained_exit_32_fails(self):
        drive = make_healthy_ata_drive(temp=38)
        drive["smartctl"]["exit_status"] = 32
        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("bit 32", msg)

    def test_zero_tolerance_sector_attributes_fail_even_with_exit_0(self):
        for bad_id in (5, 187, 197, 198):
            with self.subTest(attr_id=bad_id):
                drive = make_healthy_ata_drive(temp=38)
                for attr in drive["ata_smart_attributes"]["table"]:
                    if attr["id"] == bad_id:
                        attr["raw"]["value"] = 1
                ok, msg = smart_mon_check.evaluate_drive(drive)
                self.assertFalse(ok)
                self.assertIn(f"attr {bad_id}", msg)

    def test_pending_defects_log_fails(self):
        drive = make_healthy_ata_drive(temp=38)
        drive["ata_pending_defects_log"]["count"] = 2
        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("pending_defects=2", msg)

    def test_other_exit_status_bits_fail(self):
        for code, expected in ((4, "smart_cmd_error"), (64, "error_log"), (128, "selftest_error"), (96, "error_log")):
            with self.subTest(exit_status=code):
                drive = make_healthy_ata_drive(temp=38)
                drive["smartctl"]["exit_status"] = code
                ok, msg = smart_mon_check.evaluate_drive(drive)
                self.assertFalse(ok)
                self.assertIn(expected, msg)

    def test_smart_status_failed(self):
        drive = make_healthy_ata_drive(temp=38)
        drive["smart_status"]["passed"] = False
        ok, msg = smart_mon_check.evaluate_drive(drive)
        self.assertFalse(ok)
        self.assertIn("smart_status.passed=False", msg)

    def test_cli_invalid_json_exits_nonzero(self):
        proc = subprocess.run(
            [CHECK_PATH],
            input="not json",
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(proc.returncode, 1)
        self.assertIn("FAIL (invalid smartctl JSON", proc.stdout)

    def test_end_to_end_smart_mon_count_pipeline(self):
        base_tmp = "/tmp/opencode" if os.path.isdir("/tmp/opencode") else None
        with tempfile.TemporaryDirectory(dir=base_tmp) as tmpdir:
            payload_out = os.path.join(tmpdir, "posted.json")
            url_out = os.path.join(tmpdir, "url.txt")

            drives = {
                "/dev/da0": (0, make_healthy_ata_drive(temp=38)),
                # Cooled drive with historical Airflow_Temperature_Cel scar -> should PASS
                "/dev/da1": (32, None),
                # Actively hot drive -> should FAIL
                "/dev/da2": (32, make_healthy_ata_drive(temp=56)),
                # Drive with 1 pending sector despite exit 0 -> should FAIL
                "/dev/da3": (0, None),
            }

            scarred_cool = make_healthy_ata_drive(temp=39)
            scarred_cool["smartctl"]["exit_status"] = 32
            for a in scarred_cool["ata_smart_attributes"]["table"]:
                if a["id"] == 190:
                    a["worst"] = 39
                    a["when_failed"] = "past"
            drives["/dev/da1"] = (32, scarred_cool)

            scarred_hot = drives["/dev/da2"][1]
            scarred_hot["smartctl"]["exit_status"] = 32
            for a in scarred_hot["ata_smart_attributes"]["table"]:
                if a["id"] == 190:
                    a["worst"] = 39
                    a["when_failed"] = "past"

            pending_sec = make_healthy_ata_drive(temp=38)
            for a in pending_sec["ata_smart_attributes"]["table"]:
                if a["id"] == 197:
                    a["raw"]["value"] = 1
            drives["/dev/da3"] = (0, pending_sec)

            drives_file = os.path.join(tmpdir, "drives.json")
            with open(drives_file, "w", encoding="utf-8") as fh:
                json.dump(drives, fh)

            mock_smartctl = os.path.join(tmpdir, "smartctl")
            with open(mock_smartctl, "w", encoding="utf-8") as fh:
                fh.write(
                    "#!/usr/bin/env python3\n"
                    "import json, sys\n"
                    f"with open({drives_file!r}) as f:\n"
                    "    drives = json.load(f)\n"
                    "args = sys.argv[1:]\n"
                    "if '--scan' in args:\n"
                    "    for d in drives:\n"
                    "        print(f'{d} -d scsi # mock')\n"
                    "    print('/dev/cd0 -d scsi # non-smart device')\n"
                    "    sys.exit(0)\n"
                    "if '-i' in args:\n"
                    "    dev = args[-1]\n"
                    "    if dev in drives:\n"
                    "        print('SMART support is: Enabled')\n"
                    "        sys.exit(0)\n"
                    "    print('SMART support is: Unavailable')\n"
                    "    sys.exit(1)\n"
                    "dev = [a for a in args if a.startswith('/dev/')][0]\n"
                    "code, body = drives[dev]\n"
                    "print(json.dumps(body))\n"
                    "sys.exit(code)\n"
                )
            os.chmod(mock_smartctl, stat.S_IRWXU)

            mock_curl = os.path.join(tmpdir, "curl")
            with open(mock_curl, "w", encoding="utf-8") as fh:
                fh.write(
                    "#!/usr/bin/env python3\n"
                    "import sys\n"
                    "data = sys.stdin.read()\n"
                    f"with open({payload_out!r}, 'w') as f:\n"
                    "    f.write(data)\n"
                    "url = [a for a in sys.argv[1:] if a.startswith('http')][0]\n"
                    f"with open({url_out!r}, 'w') as f:\n"
                    "    f.write(url)\n"
                    "print('{\"result\": \"created\"}')\n"
                )
            os.chmod(mock_curl, stat.S_IRWXU)

            env = copy.deepcopy(os.environ)
            env["PATH"] = f"{tmpdir}:{env.get('PATH', '')}"
            env["HOSTNAME"] = "brick-test"

            proc = subprocess.run(
                [os.path.join(REPO_DIR, "smart-mon-count.sh")],
                env=env,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(proc.returncode, 0, msg=f"stderr={proc.stderr}\nstdout={proc.stdout}")
            self.assertIn("/dev/da0 - OK (38C)", proc.stdout)
            self.assertIn("/dev/da1 - OK (39C, ignored past Airflow_Temperature_Cel)", proc.stdout)
            self.assertIn("/dev/da2 - FAIL (temp 56C > 50C)", proc.stdout)
            self.assertIn("/dev/da3 - FAIL (attr 197 Current_Pending_Sector raw=1)", proc.stdout)

            with open(payload_out, "r", encoding="utf-8") as fh:
                posted = json.load(fh)
            self.assertEqual(posted["passed"], 2)
            self.assertEqual(posted["host"], "brick-test")
            self.assertIn("timestamp", posted)


if __name__ == "__main__":
    unittest.main()
