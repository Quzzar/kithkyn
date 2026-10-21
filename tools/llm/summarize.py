"""Summarize synthetic Kithkyn replays without treating parse success as NPC quality."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
from pathlib import Path
from typing import Any

ACTIONS = {"follow", "wait", "go_to_point", "resume"}
STOCK = {"minecraft:bread": 3, "minecraft:oak_log": 4, "minecraft:stone_axe": 1}


def unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    """Reject duplicate keys instead of silently selecting one instruction."""
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def invalid_constant(value: str) -> None:
    raise ValueError(f"Non-JSON numeric constant: {value}")


def reply(raw: str) -> dict[str, Any] | None:
    """Require a single strict object, spoken text, and a supported optional action."""
    try:
        value = json.loads(raw, object_pairs_hook=unique_object, parse_constant=invalid_constant)
    except (ValueError, TypeError):
        return None
    if not isinstance(value, dict) or not isinstance(value.get("say"), str) or not value["say"].strip():
        return None
    if "action" in value and (not isinstance(value["action"], str) or value["action"] not in ACTIONS):
        return None
    return value


def legal_inventory(value: dict[str, Any]) -> bool:
    """Score claimed stock/quantity, not a physical transfer, using Minecraft's default namespace."""
    if "give" not in value:
        return True
    item = value["give"]
    if not isinstance(item, str):
        return False
    if ":" not in item:
        item = f"minecraft:{item}"
    count = value.get("give_count", 1)
    return type(count) is int and 0 < count <= STOCK.get(item, 0)


def percentile(values: list[float], fraction: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lo, hi = math.floor(position), math.ceil(position)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (position - lo)


def ratio(values: list[bool]) -> dict[str, int]:
    return {"passed": sum(values), "total": len(values)}


def summarize(report: dict[str, Any], corpus: dict[str, Any]) -> dict[str, Any]:
    """Keep request fulfillment, refusals, tool legality, and subjective review separate."""
    if report.get("status") != "complete" or not report.get("chat_format"):
        raise ValueError(f"{report.get('candidate')}: comparison is incomplete or has no explicit chat format")
    samples = report["samples"]
    expected = len(corpus["scenarios"]) * report["repeats"] + len(corpus["conversation"])
    keys = [(row["id"], row["repeat"]) for row in samples]
    if len(samples) != expected or len(set(keys)) != expected:
        raise ValueError(f"{report['candidate']}: missing or duplicate samples")
    chats = [row for row in samples if row["category"] != "decision"]
    decoded = {id(row): reply(row.get("raw", "")) for row in chats}

    def matches(row: dict[str, Any]) -> bool:
        value = decoded[id(row)]
        return value is not None and value.get("action", "none") == row["expected_action"]

    positive = [row for row in chats if row["category"] == "action" and row["expected_action"] != "none"]
    negative = [row for row in chats if row["category"] == "action" and row["expected_action"] == "none"]
    items = [row for row in chats if row["category"] == "item"]
    decisions = [row for row in samples if row["category"] == "decision"]
    conversation = [row for row in chats if row["category"] == "conversation"]
    says = [decoded[id(row)]["say"] for row in conversation if decoded[id(row)] is not None]
    openings = [line.split(".", 1)[0].strip().casefold() for line in says]
    rss = [row["rss_kib"] for row in samples if row.get("rss_kib") is not None]
    return {
        "candidate": report["candidate"],
        "samples": len(samples),
        "response_present": ratio([row["response_present"] for row in samples]),
        "usable_chat_structure": ratio([decoded[id(row)] is not None for row in chats]),
        "requested_action_match": ratio([matches(row) for row in positive]),
        "negative_action_cases": ratio([matches(row) for row in negative]),
        "item_claims_legal": ratio([decoded[id(row)] is not None and legal_inventory(decoded[id(row)]) for row in items]),
        "decision_format_valid": ratio([row.get("decision_valid", False) for row in decisions]),
        "decision_correct": ratio([row.get("decision_correct", False) for row in decisions]),
        "extra_actions_outside_action_cases": sum(
            decoded[id(row)] is not None and "action" in decoded[id(row)]
            for row in chats if row["category"] != "action"
        ),
        "unrequested_fights": sum(
            decoded[id(row)] is not None and decoded[id(row)].get("fight") is True
            for row in chats if row["id"] != "threat"
        ),
        "conversation_usable": len(says),
        "conversation_distinct_openings": len(set(openings)),
        "median_latency_ms": statistics.median(row["latency_ms"] for row in samples),
        "p95_latency_ms": percentile([row["latency_ms"] for row in samples], 0.95),
        "max_sampled_rss_mib": max(rss) / 1024 if rss else None,
        "manual_review": [
            {"id": row["id"], "repeat": row["repeat"], "raw": row.get("raw")}
            for row in chats if row["category"] in {"memory", "roleplay", "conversation"}
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reports", nargs="+", type=Path)
    parser.add_argument("--corpus", type=Path, default=Path(__file__).with_name("model-cases.json"))
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    corpus = json.loads(args.corpus.read_text())
    reports = [json.loads(path.read_text()) for path in args.reports]
    prompts: dict[tuple[str, int], str] = {}
    for report in reports:
        for row in report["samples"]:
            if row["category"] == "conversation":
                continue  # Each model builds its own transcript.
            key = (row["id"], row["repeat"])
            if key in prompts and prompts[key] != row["prompt_sha256"]:
                raise ValueError(f"Unpaired prompts for {report['candidate']}: {key}")
            prompts[key] = row["prompt_sha256"]
    output = {
        "corpus_sha256": hashlib.sha256(args.corpus.read_bytes()).hexdigest(),
        "source_report_sha256": {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in args.reports},
        "notes": [
            "Positive matches measure literal request fulfillment; a grounded refusal can be legitimate.",
            "Valid JSON and distinct openings do not establish personality, truthfulness, or conversation quality.",
            "Memory, roleplay and full conversations require human reading; no automated quality judge is used.",
            "RSS is sampled process memory, not peak total RAM or GPU/unified memory.",
            "Strict decision scoring can be stricter than the production generic decision parser.",
        ],
        "models": [summarize(report, corpus) for report in reports],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(output, indent=2, ensure_ascii=False) + "\n")
    for result in output["models"]:
        print(json.dumps({key: value for key, value in result.items() if key != "manual_review"}))


if __name__ == "__main__":
    main()
