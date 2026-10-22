"""Score captured legal-choice replays without fitting thresholds or changing responses."""
from __future__ import annotations

import argparse
from collections import defaultdict
import hashlib
import json
import math
from pathlib import Path
import re
import statistics

ROOT = Path(__file__).resolve().parent
THRESHOLD = 0.8
SOURCE_SHA256 = "ccb19fa59c03a2236c11454e20c8789c77697b6ad6f2c80eaf2e5e3497e2690a"


def number(value: object) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)]


def summarize(rows: list[dict]) -> dict:
    accepted = [row for row in rows if row["valid"] and row["confidence"] >= THRESHOLD]
    raw_accepted = [row for row in rows if row["valid"] and row["selected_probability"] >= THRESHOLD]
    def acceptance(items: list[dict]) -> dict:
        errors = sum(not row["correct"] for row in items)
        return {"accepted": len(items), "coverage": len(items) / len(rows), "errors": errors,
                "error_rate": errors / len(items) if items else None}
    return {
        "requests": len(rows), "states": len({row["state_id"] for row in rows}),
        "valid": sum(row["valid"] for row in rows), "correct": sum(row["correct"] for row in rows),
        "accuracy": sum(row["correct"] for row in rows) / len(rows),
        "confidence_at_0_8": acceptance(accepted), "raw_probability_at_0_8": acceptance(raw_accepted),
        "wall_ms": {"median": statistics.median(row["wall_ms"] for row in rows),
                    "p95_nearest_rank": percentile([row["wall_ms"] for row in rows], .95),
                    "max": max(row["wall_ms"] for row in rows)},
        "model_ms_median": statistics.median(row["model_ms"] for row in rows),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw", type=Path, default=ROOT / "reaction-raw.jsonl",
                        help="Recorded replay JSONL, including each exact source_line")
    parser.add_argument("--output", type=Path, default=ROOT / "reaction-analysis.json",
                        help="Analysis JSON destination")
    parser.add_argument("--source", type=Path,
                        help="Optional original request JSONL to verify against embedded source lines")
    args = parser.parse_args()
    raw_rows = [json.loads(line) for line in args.raw.read_text().splitlines()]
    source_bytes = ("\n".join(row["source_line"] for row in raw_rows) + "\n").encode()
    source_hash = hashlib.sha256(source_bytes).hexdigest()
    if args.source is None:
        if len(raw_rows) != 48 or source_hash != SOURCE_SHA256:
            raise RuntimeError("Historical replay source changed; pass --source for another corpus")
    elif args.source.read_bytes() != source_bytes:
        raise RuntimeError("Original replay source differs from embedded source lines")
    if not raw_rows or len({row["id"] for row in raw_rows}) != len(raw_rows):
        raise RuntimeError("Replay must contain unique, nonempty request ids")
    rows = []
    for raw in raw_rows:
        original = json.loads(raw["source_line"])
        payload = json.loads(raw["request_body"])
        criteria = payload["questions"]["reaction"]["criteria"]
        errors = []
        if payload != original["payload"] or list(criteria) != list(original["payload"]["questions"]["reaction"]["criteria"]):
            errors.append("request_payload_changed")
        if raw["criteria_order"] != list(criteria): errors.append("criteria_order_changed")
        if raw["http_status"] != 200: errors.append("http_failure")
        body = json.loads(raw["response_body"])
        answer = body["answers"]["reaction"]
        if answer.get("type") != "choice": errors.append("answer_type")
        choice = answer.get("choice")
        if choice not in criteria: errors.append("choice_not_offered")
        probabilities = answer.get("probabilities", {})
        if set(probabilities) != set(criteria): errors.append("probability_keys")
        if not all(number(p) and 0 <= p <= 1 for p in probabilities.values()): errors.append("probability_value")
        total = sum(probabilities.values())
        if abs(total - 1) > len(criteria) * 0.00005 + 0.000001: errors.append("probability_sum")
        confidence = answer.get("confidence")
        if not number(confidence) or not 0 <= confidence <= 1: errors.append("confidence_value")
        expected_confidence = 1 if len(criteria) == 1 else (max(probabilities.values()) / total - 1 / len(criteria)) / (1 - 1 / len(criteria))
        confidence_error = abs(confidence - expected_confidence)
        if confidence_error > .0002: errors.append("confidence_formula")
        selected_probability = probabilities.get(choice, 0)
        if selected_probability < max(probabilities.values()) - .0001: errors.append("choice_not_argmax")
        match = re.fullmatch(r"(.+)_order_(\d+)", raw["id"])
        if match is None: raise RuntimeError(f"Unexpected request ID {raw['id']}")
        rows.append({
            "id": raw["id"], "state_id": match.group(1), "order": int(match.group(2)),
            "split": raw["split"], "pair": raw["pair"], "expected": raw["expected"],
            "choice": choice, "correct": not errors and choice in raw["expected"],
            "valid": not errors, "validation_errors": errors,
            "confidence": confidence, "confidence_formula_error": confidence_error,
            "selected_probability": selected_probability, "probabilities": probabilities,
            "wall_ms": raw["wall_ms"], "model_ms": body["latency_ms"],
            "criteria_order": raw["criteria_order"],
        })
    states = defaultdict(list)
    pairs = defaultdict(lambda: defaultdict(list))
    for row in rows:
        states[row["state_id"]].append(row)
        pairs[row["pair"]][row["state_id"]].append(row)
    state_details = []
    for state_id, group in states.items():
        group.sort(key=lambda row: row["order"])
        state_details.append({
            "state_id": state_id, "split": group[0]["split"], "expected": group[0]["expected"],
            "choices": [row["choice"] for row in group], "confidences": [row["confidence"] for row in group],
            "all_choices_same": len({row["choice"] for row in group}) == 1,
            "confidence_acceptance_same": len({row["confidence"] >= THRESHOLD for row in group}) == 1,
            "max_probability_spread": max(max(row["probabilities"][key] for row in group) - min(row["probabilities"][key] for row in group) for key in group[0]["probabilities"]),
        })
    pair_details = []
    for pair, members in pairs.items():
        if len(members) == 1: continue  # Independent scenarios are not personality pairs.
        if len(members) != 2: raise RuntimeError(f"Expected two states in pair {pair}")
        names = list(members)
        left, right = [sorted(members[name], key=lambda row: row["order"]) for name in names]
        pair_details.append({
            "pair": pair, "split": left[0]["split"], "states": names,
            "expected": [left[0]["expected"], right[0]["expected"]],
            "choices": [[row["choice"] for row in left], [row["choice"] for row in right]],
            "different_choices": sum(a["choice"] != b["choice"] for a, b in zip(left, right)),
            "both_correct": sum(a["correct"] and b["correct"] for a, b in zip(left, right)),
        })
    result = {
        "threshold": THRESHOLD, "threshold_tuned": False,
        "source_sha256": source_hash, "overall": summarize(rows),
        "by_split": {split: summarize([row for row in rows if row["split"] == split]) for split in dict.fromkeys(row["split"] for row in rows)},
        "by_order": {str(order): summarize([row for row in rows if row["order"] == order]) for order in range(3)},
        "order_consistency": {"states": len(states), "stable_choices": sum(row["all_choices_same"] for row in state_details),
                              "stable_confidence_acceptance": sum(row["confidence_acceptance_same"] for row in state_details)},
        "order_consistency_by_split": {
            split: {"states": sum(row["split"] == split for row in state_details),
                    "stable_choices": sum(row["split"] == split and row["all_choices_same"] for row in state_details)}
            for split in dict.fromkeys(row["split"] for row in rows)
        },
        "pair_summary": {"pairs": len(pair_details), "matched_order_comparisons": sum(len(pair["choices"][0]) for pair in pair_details),
                         "different_choices": sum(row["different_choices"] for row in pair_details),
                         "both_correct": sum(row["both_correct"] for row in pair_details)},
        "pair_summary_by_split": {
            split: {"pairs": sum(row["split"] == split for row in pair_details),
                    "matched_order_comparisons": sum(3 for row in pair_details if row["split"] == split),
                    "different_choices": sum(row["different_choices"] for row in pair_details if row["split"] == split),
                    "both_correct": sum(row["both_correct"] for row in pair_details if row["split"] == split)}
            for split in dict.fromkeys(row["split"] for row in rows)
        },
        "states": state_details, "pairs": pair_details, "rows": rows,
    }
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({key: result[key] for key in ["overall", "by_split", "by_order", "order_consistency", "pair_summary", "states", "pairs"]}, indent=2))


if __name__ == "__main__":
    main()
