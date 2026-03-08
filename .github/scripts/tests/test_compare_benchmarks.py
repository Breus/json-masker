"""
Snapshot test for compare_benchmarks.py.

The fixtures and expected output live in tests/fixtures/:
  master.txt            — JMH output from a master run
  pr.txt                — JMH output from a PR run
  expected_comment.md   — committed snapshot of the script's expected output

If this test fails because the script output changed intentionally, update the
snapshot by running:

    python .github/scripts/compare_benchmarks.py \\
        --master-sha e3a00f208b781fc6946043d4527921f6d0e8f1f4 \\
        --pr-sha fa40aadbc0f7285ea77472d3a4e65adb3de865d4 \\
        --base-ref master \\
        --threshold 3.0 \\
        .github/scripts/tests/fixtures/master.txt \\
        .github/scripts/tests/fixtures/pr.txt \\
        > .github/scripts/tests/fixtures/expected_comment.md

(Run from the repository root.)
"""

import pathlib
import sys

# Allow importing compare_benchmarks from the parent scripts directory.
_SCRIPTS_DIR = pathlib.Path(__file__).parent.parent
sys.path.insert(0, str(_SCRIPTS_DIR))

import compare_benchmarks  # noqa: E402

_FIXTURES = pathlib.Path(__file__).parent / "fixtures"

MASTER_SHA = "e3a00f208b781fc6946043d4527921f6d0e8f1f4"
PR_SHA = "fa40aadbc0f7285ea77472d3a4e65adb3de865d4"
BASE_REF = "master"
THRESHOLD = 3.0


def test_comment_matches_snapshot() -> None:
    master_raw, master_results = compare_benchmarks.parse_results(
        str(_FIXTURES / "master.txt")
    )
    pr_raw, pr_results = compare_benchmarks.parse_results(
        str(_FIXTURES / "pr.txt")
    )

    actual = compare_benchmarks.build_comment(
        master_raw=master_raw,
        pr_raw=pr_raw,
        master_results=master_results,
        pr_results=pr_results,
        master_sha=MASTER_SHA,
        pr_sha=PR_SHA,
        base_ref=BASE_REF,
        threshold=THRESHOLD,
    )

    expected = (_FIXTURES / "expected_comment.md").read_text(encoding="utf-8")

    assert actual == expected, (
        "Script output no longer matches the committed snapshot.\n"
        "If this change is intentional, update the snapshot by running:\n\n"
        "    python .github/scripts/compare_benchmarks.py \\\n"
        f"        --master-sha {MASTER_SHA} \\\n"
        f"        --pr-sha {PR_SHA} \\\n"
        f"        --base-ref {BASE_REF} \\\n"
        f"        --threshold {THRESHOLD} \\\n"
        "        .github/scripts/tests/fixtures/master.txt \\\n"
        "        .github/scripts/tests/fixtures/pr.txt \\\n"
        "        > .github/scripts/tests/fixtures/expected_comment.md\n\n"
        "(Run from the repository root.)"
    )
