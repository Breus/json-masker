#!/usr/bin/env python3
"""
Compares two JMH results.txt files (master vs PR) and produces a Markdown
comment with per-benchmark-class tables showing ops/s and allocation rate
side-by-side, with a collapsed full-results section per class.

Usage:
    python3 compare_benchmarks.py [options] <master-results.txt> <pr-results.txt>

Options:
    --master-sha SHA      Git SHA of the master commit (for display)
    --pr-sha SHA          Git SHA of the PR commit (for display)
    --base-ref REF        Base branch name (for display, default: master)
    --threshold FLOAT     Percentage threshold for regression/improvement
                          indicators and for hiding unchanged rows (default: 1.0)
"""

import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Optional


# Matches any JMH GC secondary-metric line, e.g.:
#   BaselineBenchmark.countBytes:gc.alloc.rate.norm
#   BaselineBenchmark.countBytes:gc.alloc.rate
#   BaselineBenchmark.countBytes:gc.count
#   BaselineBenchmark.countBytes:gc.time
GC_ALLOC_NORM_PATTERN = re.compile(r":gc\.alloc\.rate\.norm\b")
GC_OTHER_PATTERN = re.compile(r":gc\.(alloc\.rate\b|count\b|time\b)")


def _is_float(s: str) -> bool:
    try:
        float(s.replace(",", "."))
        return True
    except ValueError:
        return False


@dataclass
class BenchmarkResult:
    benchmark: str       # e.g. "BaselineBenchmark.countBytes"
    class_name: str      # e.g. "BaselineBenchmark"
    method_name: str     # e.g. "countBytes"
    params: dict
    score: float
    error: Optional[float] = field(default=None)
    unit: str = ""
    alloc_score: Optional[float] = field(default=None)   # gc.alloc.rate.norm score (B/op)


def _parse_score_line(parts: list[str], n_params: int) -> Optional[tuple[float, Optional[float], str]]:
    """
    Given already-split parts of a JMH data line, extract (score, error, unit).
    Returns None if the line can't be parsed.
    Trailing fixed JMH fields: Mode [Cnt] Score [± Error] Units
    """
    trailing = parts[1 + n_params:]
    if len(trailing) < 2:
        return None
    unit = trailing[-1]
    trailing_str = " ".join(trailing[:-1])

    error: Optional[float] = None
    if "±" in trailing_str:
        before_pm, after_pm = trailing_str.split("±", 1)
        score_candidates = [
            float(t.replace(",", ".")) for t in before_pm.split() if _is_float(t)
        ]
        error_candidates = [
            float(t.replace(",", ".")) for t in after_pm.split() if _is_float(t)
        ]
        if not score_candidates:
            return None
        score = score_candidates[-1]
        if error_candidates:
            error = error_candidates[0]
    else:
        score_candidates = [
            float(t.replace(",", ".")) for t in trailing_str.split() if _is_float(t)
        ]
        if not score_candidates:
            return None
        score = score_candidates[-1]

    return score, error, unit


def parse_results(path: str) -> tuple[list[str], list[BenchmarkResult]]:
    """
    Parse a JMH results.txt file.
    Returns (raw_lines, results).

    - Regular benchmark lines become BenchmarkResult entries.
    - :gc.alloc.rate.norm lines are stored temporarily and later merged onto
      the matching BenchmarkResult as alloc_score.
    - All other :gc.* lines are discarded.
    """
    results: list[BenchmarkResult] = []
    # key -> alloc score for later merging
    alloc_map: dict[tuple, float] = {}
    raw_lines: list[str] = []
    param_cols: list[str] = []
    header_seen = False

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            raw_lines.append(line.rstrip("\n"))
            line_stripped = line.strip()

            if not line_stripped:
                continue

            # Detect header lines
            if "Mode" in line_stripped and "Units" in line_stripped:
                header_seen = True
                param_cols = re.findall(r"\((\w+)\)", line_stripped)
                continue

            if not header_seen:
                continue

            parts = re.split(r"\s{2,}", line_stripped)
            min_cols = 1 + len(param_cols) + 2  # benchmark + params + at least score + unit
            if len(parts) < min_cols:
                continue

            name = parts[0]

            # --- gc.alloc.rate.norm: capture into alloc_map ---
            if GC_ALLOC_NORM_PATTERN.search(name):
                parsed = _parse_score_line(parts, len(param_cols))
                if parsed:
                    alloc_score, _, _ = parsed
                    # Build the key using the base benchmark name (strip :gc.* suffix)
                    base_name = name.split(":")[0]
                    params = {}
                    for i, col in enumerate(param_cols):
                        params[col] = parts[1 + i]
                    alloc_key = (base_name,) + tuple(sorted(params.items()))
                    alloc_map[alloc_key] = alloc_score
                continue

            # --- Other gc.* metrics: skip ---
            if GC_OTHER_PATTERN.search(name):
                continue

            # --- Regular benchmark line ---
            parsed = _parse_score_line(parts, len(param_cols))
            if not parsed:
                continue
            score, error, unit = parsed

            params = {}
            for i, col in enumerate(param_cols):
                params[col] = parts[1 + i]

            # Derive class/method names
            name_parts = name.split(".")
            if len(name_parts) >= 2:
                class_name = name_parts[-2]
                method_name = name_parts[-1]
            else:
                class_name = name
                method_name = name

            results.append(BenchmarkResult(
                benchmark=name,
                class_name=class_name,
                method_name=method_name,
                params=params,
                score=score,
                error=error,
                unit=unit,
            ))

    # Merge alloc scores onto their parent results
    for result in results:
        alloc_key = (result.benchmark,) + tuple(sorted(result.params.items()))
        if alloc_key in alloc_map:
            result.alloc_score = alloc_map[alloc_key]

    return raw_lines, results


def make_key(result: BenchmarkResult) -> tuple:
    """Stable dict key for joining master and PR results."""
    return (result.benchmark,) + tuple(sorted(result.params.items()))


def format_score(score: float, unit: str = "") -> str:
    """Format a score value, using B/op style for allocation rates."""
    if unit == "B/op":
        if score >= 1_000_000:
            return f"{score:,.0f}"
        elif score >= 1_000:
            return f"{score:,.1f}"
        elif score >= 1:
            return f"{score:.3f}"
        else:
            return f"{score:.4f}"
    # ops/s style
    if score >= 1_000:
        return f"{score:,.0f}"
    elif score >= 1:
        return f"{score:,.2f}"
    else:
        return f"{score:.4f}"


def change_cell(master_score: float, pr_score: float, threshold: float, inverted: bool = False) -> str:
    """
    Returns a formatted change cell.
    inverted=True means lower is better (used for allocation rate).
    """
    pct = (pr_score - master_score) / master_score * 100
    if inverted:
        improved = pct <= -threshold
        regressed = pct >= threshold
    else:
        improved = pct >= threshold
        regressed = pct <= -threshold

    if improved:
        icon = "🟢"
    elif regressed:
        icon = "🔴"
    else:
        icon = "⚪"
    sign = "+" if pct >= 0 else ""
    return f"{icon} {sign}{pct:.1f}%"


def _is_significant(
    master_r: Optional[BenchmarkResult],
    pr_r: Optional[BenchmarkResult],
    threshold: float,
) -> bool:
    """Returns True if this row has a change strictly exceeding the threshold in either metric."""
    if master_r is None or pr_r is None:
        return True  # added/removed benchmarks are always significant

    ops_pct = abs((pr_r.score - master_r.score) / master_r.score * 100)
    if ops_pct > threshold:
        return True

    if master_r.alloc_score is not None and pr_r.alloc_score is not None:
        alloc_pct = abs((pr_r.alloc_score - master_r.alloc_score) / master_r.alloc_score * 100)
        if alloc_pct > threshold:
            return True

    return False


def _build_class_table(
    class_keys: list[tuple],
    master_map: dict,
    pr_map: dict,
    param_cols: list[str],
    threshold: float,
    significant_only: bool,
) -> str:
    """Build a markdown table for a single benchmark class."""
    # Determine which param columns actually have non-N/A values in this class
    active_params: list[str] = []
    for col in param_cols:
        for key in class_keys:
            param_dict = dict(key[1:])
            val = param_dict.get(col, "N/A")
            if val != "N/A":
                active_params.append(col)
                break

    # Determine unit from any result in this class
    sample = next(
        (master_map.get(k) or pr_map.get(k) for k in class_keys),
        None,
    )
    unit = sample.unit if sample else "ops/s"

    has_alloc = any(
        (master_map.get(k) and master_map[k].alloc_score is not None)
        or (pr_map.get(k) and pr_map[k].alloc_score is not None)
        for k in class_keys
    )

    # Header
    param_header = " | ".join(active_params)
    alloc_header = (
        f" | master alloc (B/op) | PR alloc (B/op) | alloc change" if has_alloc else ""
    )
    header = (
        f"| Method | {param_header} | master ({unit}) | PR ({unit}) | change"
        f"{alloc_header} |"
    )
    sep_params = " | ".join(["---"] * len(active_params))
    alloc_sep = " | ---: | ---: | ---:" if has_alloc else ""
    separator = f"| --- | {sep_params} | ---: | ---: | ---:{alloc_sep} |"

    rows = [header, separator]
    for key in class_keys:
        master_r = master_map.get(key)
        pr_r = pr_map.get(key)

        if significant_only and not _is_significant(master_r, pr_r, threshold):
            continue

        param_dict = dict(key[1:])
        param_cells = " | ".join(f"`{param_dict.get(col, 'N/A')}`" for col in active_params)

        method_name = key[0].split(".")[-1] if "." in key[0] else key[0]

        master_cell = f"`{format_score(master_r.score, unit)}`" if master_r else "N/A"
        pr_cell = f"`{format_score(pr_r.score, unit)}`" if pr_r else "N/A"

        if master_r and pr_r:
            change = change_cell(master_r.score, pr_r.score, threshold)
        elif master_r:
            change = "➖ removed"
        else:
            change = "➕ added"

        alloc_cells = ""
        if has_alloc:
            m_alloc = master_r.alloc_score if master_r else None
            p_alloc = pr_r.alloc_score if pr_r else None
            m_alloc_cell = f"`{format_score(m_alloc, 'B/op')}`" if m_alloc is not None else "N/A"
            p_alloc_cell = f"`{format_score(p_alloc, 'B/op')}`" if p_alloc is not None else "N/A"
            if m_alloc is not None and p_alloc is not None:
                alloc_ch = change_cell(m_alloc, p_alloc, threshold, inverted=True)
            elif m_alloc is not None or p_alloc is not None:
                alloc_ch = "N/A"
            else:
                alloc_ch = "N/A"
            alloc_cells = f" | {m_alloc_cell} | {p_alloc_cell} | {alloc_ch}"

        rows.append(f"| `{method_name}` | {param_cells} | {master_cell} | {pr_cell} | {change}{alloc_cells} |")

    if len(rows) == 2:
        # Only header + separator, no data rows matched
        return ""

    return "\n".join(rows) + "\n"


def build_sections(
    master_results: list[BenchmarkResult],
    pr_results: list[BenchmarkResult],
    threshold: float,
) -> str:
    master_map = {make_key(r): r for r in master_results}
    pr_map = {make_key(r): r for r in pr_results}
    all_keys = sorted(master_map.keys() | pr_map.keys())

    if not all_keys:
        return "_No benchmark results found._\n"

    # Collect all parameter column names (preserving first-seen order)
    all_param_names: list[str] = []
    seen_params: set[str] = set()
    for r in master_results + pr_results:
        for k in r.params:
            if k not in seen_params:
                all_param_names.append(k)
                seen_params.add(k)

    # Group keys by class name
    class_keys: dict[str, list[tuple]] = defaultdict(list)
    for key in all_keys:
        # class name is second-to-last dot-segment of the benchmark name
        benchmark_name = key[0]
        parts = benchmark_name.split(".")
        cls = parts[-2] if len(parts) >= 2 else benchmark_name
        class_keys[cls].append(key)

    sections: list[str] = []
    for cls, keys in class_keys.items():
        sig_table = _build_class_table(
            keys, master_map, pr_map, all_param_names, threshold, significant_only=True
        )
        full_table = _build_class_table(
            keys, master_map, pr_map, all_param_names, threshold, significant_only=False
        )

        section_lines = [f"### {cls}\n"]

        if sig_table:
            section_lines.append(sig_table)
        else:
            section_lines.append(f"_No significant changes (all within {threshold}%)._\n")

        section_lines.append(
            f"<details>\n"
            f"<summary>Full results — {cls}</summary>\n\n"
            f"{full_table}\n"
            f"</details>\n"
        )

        sections.append("\n".join(section_lines))

    return "\n---\n\n".join(sections)


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
    sections = build_sections(master_results, pr_results, threshold)

    master_display = f"{base_ref} @ {master_sha[:7]}" if master_sha else base_ref
    pr_display = f"PR @ {pr_sha[:7]}" if pr_sha else "PR"

    # Raw output: filter out gc.* lines for readability
    def filter_raw(lines: list[str]) -> str:
        return "\n".join(
            l for l in lines
            if not GC_ALLOC_NORM_PATTERN.search(l) and not GC_OTHER_PATTERN.search(l)
        )

    master_raw_text = filter_raw(master_raw)
    pr_raw_text = filter_raw(pr_raw)

    comment = f"""> [!NOTE]
> These results are affected by shared workloads on GitHub runners. Use the results only to detect possible regressions, but always rerun on a more stable machine before drawing conclusions!
> Regressions/improvements are highlighted when the difference exceeds {threshold}%.

### Benchmark results

{sections}
<details>
<summary>Raw output ({pr_display})</summary>

```text
{pr_raw_text}
```

</details>

<details>
<summary>Raw output ({master_display})</summary>

```text
{master_raw_text}
```

</details>
"""
    return comment


def main() -> None:
    # Ensure stdout can handle Unicode (e.g. emoji) regardless of terminal encoding.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Compare JMH benchmark results.")
    parser.add_argument("master", help="Path to master results.txt")
    parser.add_argument("pr", help="Path to PR results.txt")
    parser.add_argument("--master-sha", default="", help="Git SHA of master commit")
    parser.add_argument("--pr-sha", default="", help="Git SHA of PR commit")
    parser.add_argument("--base-ref", default="master", help="Base branch name")
    parser.add_argument(
        "--threshold",
        type=float,
        default=1.0,
        help="Percentage threshold for regression/improvement indicators and row filtering",
    )
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
