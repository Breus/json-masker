#!/usr/bin/env python3
"""
Compares two JMH results.txt files (master vs PR) and produces a Markdown
comment with a side-by-side table and collapsed raw output sections.

Usage:
    python3 compare_benchmarks.py [options] <master-results.txt> <pr-results.txt>

Options:
    --master-sha SHA      Git SHA of the master commit (for display)
    --pr-sha SHA          Git SHA of the PR commit (for display)
    --base-ref REF        Base branch name (for display, default: master)
    --threshold FLOAT     Percentage threshold for regression/improvement
                          indicators (default: 3.0)
"""

import argparse
import re
import sys
from dataclasses import dataclass, field
from typing import Optional


GC_LINE_PATTERN = re.compile(r":gc\.(alloc\.rate|count|time)\s")


def _is_float(s: str) -> bool:
    try:
        float(s.replace(",", "."))
        return True
    except ValueError:
        return False


@dataclass
class BenchmarkResult:
    benchmark: str
    params: dict
    score: float
    error: Optional[float] = field(default=None)
    unit: str = ""


def parse_results(path: str) -> tuple[list[str], list[BenchmarkResult]]:
    """
    Parse a JMH results.txt file.
    Returns (raw_lines, results).
    Each results.txt may contain multiple benchmark classes with different
    parameter columns, so we re-parse the header each time we see one.
    """
    results: list[BenchmarkResult] = []
    raw_lines: list[str] = []
    param_cols: list[str] = []
    header_seen = False

    with open(path, "r") as f:
        for line in f:
            raw_lines.append(line.rstrip("\n"))
            line_stripped = line.strip()

            # Skip empty lines and GC metric lines
            if not line_stripped or GC_LINE_PATTERN.search(line_stripped):
                continue

            # Detect header lines: they contain "Mode" and "Units" and no numeric score
            if "Mode" in line_stripped and "Units" in line_stripped:
                # Parse parameter column names: everything between the benchmark
                # name column and the fixed trailing columns (Mode Cnt Score Error Units)
                # Header format: "Benchmark  (param1)  (param2)  ...  Mode  Cnt  Score  Error  Units"
                header_seen = True
                param_cols = re.findall(r"\((\w+)\)", line_stripped)
                continue

            if not header_seen:
                continue

            # Data lines: split on 2+ spaces to handle values that contain spaces
            parts = re.split(r"\s{2,}", line_stripped)

            # Minimum expected columns: benchmark + params + Mode + Cnt/blank + Score + [Error] + Units
            # JMH omits Cnt and Error when there's only 1 iteration, so be flexible.
            # We expect at least: benchmark, *params, mode, score, units (3 trailing fixed fields minimum)
            min_cols = 1 + len(param_cols) + 3
            if len(parts) < min_cols:
                continue

            benchmark = parts[0]
            params = {}
            for i, col in enumerate(param_cols):
                params[col] = parts[1 + i]

            # The trailing fixed fields from JMH are:
            #   Mode  Cnt  Score  ±  Error  Units
            # Units is always last; ± is a literal separator token.
            trailing = parts[1 + len(param_cols):]
            unit = trailing[-1]
            error: Optional[float] = None

            # Rejoin the trailing fields (minus units) so we can split on ±
            trailing_str = " ".join(trailing[:-1])
            if "±" in trailing_str:
                before_pm, after_pm = trailing_str.split("±", 1)
                # Score is the last float in the before-± portion
                score_candidates = [
                    float(t.replace(",", "."))
                    for t in before_pm.split()
                    if _is_float(t)
                ]
                error_candidates = [
                    float(t.replace(",", "."))
                    for t in after_pm.split()
                    if _is_float(t)
                ]
                if not score_candidates:
                    continue
                score: float = score_candidates[-1]
                if error_candidates:
                    error = error_candidates[0]
            else:
                # No error column — score is the last float in trailing
                score_candidates = [
                    float(t.replace(",", "."))
                    for t in trailing_str.split()
                    if _is_float(t)
                ]
                if not score_candidates:
                    continue
                score = score_candidates[-1]

            results.append(BenchmarkResult(
                benchmark=benchmark,
                params=params,
                score=score,
                error=error,
                unit=unit,
            ))

    return raw_lines, results


def make_key(result: BenchmarkResult) -> tuple:
    """Stable dict key for joining master and PR results."""
    return (result.benchmark,) + tuple(sorted(result.params.items()))


def format_score(score: float) -> str:
    if score >= 1_000:
        return f"{score:,.0f}"
    elif score >= 1:
        return f"{score:,.2f}"
    else:
        return f"{score:.4f}"


def change_cell(master_score: float, pr_score: float, threshold: float) -> str:
    pct = (pr_score - master_score) / master_score * 100
    if pct >= threshold:
        icon = "🟢"
    elif pct <= -threshold:
        icon = "🔴"
    else:
        icon = "⚪"
    sign = "+" if pct >= 0 else ""
    return f"{icon} {sign}{pct:.1f}%"


def build_table(
    master_results: list[BenchmarkResult],
    pr_results: list[BenchmarkResult],
    threshold: float,
) -> str:
    master_map = {make_key(r): r for r in master_results}
    pr_map = {make_key(r): r for r in pr_results}
    all_keys = list(master_map.keys() | pr_map.keys())
    all_keys.sort()

    if not all_keys:
        return "_No benchmark results found._\n"

    # Discover all param column names across all results
    all_param_names: list[str] = []
    seen: set[str] = set()
    for r in master_results + pr_results:
        for k in r.params:
            if k not in seen:
                all_param_names.append(k)
                seen.add(k)

    # Determine unit (assume consistent across results)
    sample = next(iter(master_map.values()), None) or next(iter(pr_map.values()))
    unit = sample.unit

    # Header
    param_headers = " | ".join(all_param_names)
    header = f"| Benchmark | {param_headers} | master ({unit}) | PR ({unit}) | change |"
    separator = "| --- | " + " | ".join(["---"] * len(all_param_names)) + " | ---: | ---: | ---: |"

    rows = [header, separator]
    for key in all_keys:
        master_r = master_map.get(key)
        pr_r = pr_map.get(key)

        # Benchmark name: strip common package prefix for readability
        benchmark_name = key[0]
        benchmark_name = benchmark_name.split(".")[-1] if "." in benchmark_name else benchmark_name
        # Re-add the class prefix (second-to-last segment) for context
        parts = key[0].split(".")
        if len(parts) >= 2:
            benchmark_name = f"{parts[-2]}.{parts[-1]}"

        param_values = dict(key[1:])
        param_cells = " | ".join(param_values.get(col, "") for col in all_param_names)

        master_cell = format_score(master_r.score) if master_r else "N/A"
        pr_cell = format_score(pr_r.score) if pr_r else "N/A"

        if master_r and pr_r:
            change = change_cell(master_r.score, pr_r.score, threshold)
        elif master_r:
            change = "➕ added"
        else:
            change = "➖ removed"

        rows.append(f"| {benchmark_name} | {param_cells} | {master_cell} | {pr_cell} | {change} |")

    return "\n".join(rows) + "\n"


def build_comment(
    master_raw: list[str],
    pr_raw: list[str],
    master_results: list[BenchmarkResult],
    pr_results: list[BenchmarkResult],
    master_sha: str,
    pr_sha: str,
    base_ref: str,
    threshold: float,
) -> str:
    table = build_table(master_results, pr_results, threshold)

    master_raw_filtered = "\n".join(
        l for l in master_raw if not GC_LINE_PATTERN.search(l)
    )
    pr_raw_filtered = "\n".join(
        l for l in pr_raw if not GC_LINE_PATTERN.search(l)
    )

    master_display = f"{base_ref} @ {master_sha[:7]}" if master_sha else base_ref
    pr_display = f"PR @ {pr_sha[:7]}" if pr_sha else "PR"

    comment = f"""> [!NOTE]
> These results are affected by shared workloads on GitHub runners. Use the results only to detect possible regressions, but always rerun on a more stable machine before drawing conclusions!
> Regressions/improvements are highlighted when the difference exceeds {threshold:.0f}%.

### Benchmark results

{table}
<details>
<summary>Raw output ({pr_display})</summary>

```text
{pr_raw_filtered}
```

</details>

<details>
<summary>Raw output ({master_display})</summary>

```text
{master_raw_filtered}
```

</details>
"""
    return comment


def main() -> None:
    parser = argparse.ArgumentParser(description="Compare JMH benchmark results.")
    parser.add_argument("master", help="Path to master results.txt")
    parser.add_argument("pr", help="Path to PR results.txt")
    parser.add_argument("--master-sha", default="", help="Git SHA of master commit")
    parser.add_argument("--pr-sha", default="", help="Git SHA of PR commit")
    parser.add_argument("--base-ref", default="master", help="Base branch name")
    parser.add_argument("--threshold", type=float, default=5.0,
                        help="Percentage threshold for regression/improvement indicators")
    args = parser.parse_args()

    master_raw, master_results = parse_results(args.master)
    pr_raw, pr_results = parse_results(args.pr)

    comment = build_comment(
        master_raw=master_raw,
        pr_raw=pr_raw,
        master_results=master_results,
        pr_results=pr_results,
        master_sha=args.master_sha,
        pr_sha=args.pr_sha,
        base_ref=args.base_ref,
        threshold=args.threshold,
    )
    sys.stdout.write(comment)


if __name__ == "__main__":
    main()
