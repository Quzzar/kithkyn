#!/usr/bin/env python3
"""Reject production structure templates containing invalid authored geometry."""

from pathlib import Path
import re
import sys

from nbt import read


MARKET_NAME = re.compile(r"^.*market(?:_.+)?_(\d+)\.nbt$")
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


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
    blocks = {
        tuple(block["pos"]): palette[block["state"]]
        for block in root["blocks"]
    }
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

    market = MARKET_NAME.match(path.name)
    if market is not None:
        expected = int(market.group(1))
        entrances = sorted(
            position
            for position, state in blocks.items()
            if position[1] == 1 and state["Name"].endswith("_carpet")
        )
        if len(entrances) != expected:
            failures.append((
                "market entrances",
                "carpet",
                f"expected {expected} one-block entrance carpets, found {len(entrances)}",
            ))

        entrance_set = set(entrances)
        for position in entrances:
            x, y, z = position
            below = blocks.get((x, y - 1, z), {"Name": "minecraft:air"})["Name"]
            if below in AIR:
                failures.append((position, blocks[position]["Name"], "entrance carpet has no authored support"))

            for adjacent in ((x + 1, y, z), (x, y, z + 1)):
                if adjacent in entrance_set and blocks[adjacent]["Name"] == blocks[position]["Name"]:
                    failures.append((
                        position,
                        blocks[position]["Name"],
                        f"same-color entrance carpet projects two blocks through {adjacent}",
                    ))
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
    print(
        f"PASS {len(paths)} templates: no barrier states, out-of-bounds blocks, "
        "or malformed market entrances"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
