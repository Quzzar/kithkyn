#!/usr/bin/env python3
"""Validate the authored entrance carpets in market structure templates."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))

from nbt import read  # noqa: E402


MARKET_NAME = re.compile(r"^.*market(?:_.+)?_(\d+)\.nbt$")


def entrance_carpets(path: Path) -> tuple[int, list[tuple[int, int, int]], list[str]]:
    match = MARKET_NAME.match(path.name)
    if match is None:
        raise ValueError(f"Not a leveled market structure: {path}")

    expected = int(match.group(1))
    structure = read(path)
    palette = structure["palette"]
    blocks = {
        tuple(block["pos"]): palette[block["state"]]
        for block in structure["blocks"]
    }
    entrances = sorted(
        position
        for position, state in blocks.items()
        if position[1] == 1 and state["Name"].endswith("_carpet")
    )

    errors: list[str] = []
    if len(entrances) != expected:
        errors.append(f"expected {expected} entrance carpets, found {len(entrances)}")

    for position in entrances:
        x, y, z = position
        below = blocks.get((x, y - 1, z), {"Name": "minecraft:air"})["Name"]
        if below in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            errors.append(f"entrance carpet {position} has no authored support")

    entrance_set = set(entrances)
    for x, y, z in entrances:
        state = blocks[(x, y, z)]["Name"]
        for adjacent in ((x + 1, y, z), (x, y, z + 1)):
            if adjacent in entrance_set and blocks[adjacent]["Name"] == state:
                errors.append(
                    f"same-color entrance carpet projects two blocks at {(x, y, z)} and {adjacent}"
                )

    return expected, entrances, errors


def market_paths(arguments: list[str]) -> list[Path]:
    if arguments:
        paths = [Path(argument) for argument in arguments]
    else:
        root = SCRIPT_DIR.parents[1] / "src/main/resources/data/kithkyn/structure"
        paths = sorted(root.glob("market_*.nbt"))
    return [path for path in paths if MARKET_NAME.match(path.name)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="*")
    args = parser.parse_args()

    paths = market_paths(args.paths)
    if not paths:
        print("No leveled market structures found", file=sys.stderr)
        return 2

    failures = 0
    for path in paths:
        expected, entrances, errors = entrance_carpets(path)
        if errors:
            failures += 1
            print(f"FAIL {path}")
            for error in errors:
                print(f"  {error}")
        else:
            print(f"PASS {path}: {len(entrances)}/{expected} supported one-block entrances")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
