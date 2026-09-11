#!/usr/bin/env python3
"""Reject production structure templates containing gallery fixtures or invalid cells."""

from pathlib import Path
import sys

from nbt import read


def templates(arguments):
    for value in arguments:
        path = Path(value)
        if path.is_dir():
            yield from sorted(path.rglob("*.nbt"))
        elif path.suffix == ".nbt":
            yield path


def problems(path):
    root = read(path)
    size = root["size"]
    palette = root["palette"]
    failures = [
        (f"palette[{index}]", state["Name"], "barrier state in production template")
        for index, state in enumerate(palette)
        if state["Name"] == "minecraft:barrier"
    ]
    for block in root["blocks"]:
        position = tuple(block["pos"])
        name = palette[block["state"]]["Name"]
        reasons = []
        if any(coordinate < 0 or coordinate >= size[axis]
               for axis, coordinate in enumerate(position)):
            reasons.append("outside declared size")
        if name == "minecraft:barrier":
            reasons.append("gallery barrier in production template")
        if reasons:
            failures.append((position, name, "; ".join(reasons)))
    return failures


def main(arguments):
    paths = list(templates(arguments))
    if not paths:
        raise SystemExit("Pass one or more structure .nbt files or directories")
    failed = 0
    for path in paths:
        failures = problems(path)
        if not failures:
            continue
        failed += 1
        print(f"FAIL {path}")
        for position, name, reason in failures:
            print(f"  {position}: {name}: {reason}")
    if failed:
        print(f"{failed} of {len(paths)} templates failed")
        return 1
    print(f"PASS {len(paths)} templates: no barrier states or out-of-bounds blocks")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
