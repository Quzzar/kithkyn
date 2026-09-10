"""Flag catalog buildings that are probably seated one block too low.

A template's layer 0 replaces the terrain's top block; ``sink`` buries that many
more layers and ``-1`` lifts layer 0 onto the ground (BuildingInfo.getSink).
The tell Aaron uses in the world is a flight of steps swallowed by the ground:
a bottom-half stair in the seated layer sits level with the surface, so the
step leads nowhere. This reads every definition of the given packs (a datapack
root or the bundled resources) and lists the buildings whose seated layer holds
such stairs, with what else that layer is made of, so a person can tell an
entrance step from a farm's edging.

Usage: seating-check.py <data-root> [<data-root> ...]
  where a data root is the directory holding kithkyn/buildings and structure,
  e.g. run/world/datapacks/kithkyn-badlands/data/kithkyn or src/main/resources/data/kithkyn
"""
from collections import Counter
from pathlib import Path
import json
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nbt import read

GROUND_BLOCKS = {'minecraft:' + name for name in (
    'dirt', 'coarse_dirt', 'rooted_dirt', 'grass_block', 'podzol', 'mycelium', 'dirt_path', 'farmland', 'moss_block',
    'mud', 'packed_mud', 'muddy_mangrove_roots', 'sand', 'red_sand', 'gravel', 'clay', 'water', 'snow_block')}


def seated_layer(structure, sink):
    """The blocks of the layer that meets the surface: the blocks at y == sink."""
    palette = structure['palette']
    return [(block['pos'], palette[block['state']]) for block in structure['blocks']
            if block['pos'][1] == sink and palette[block['state']]['Name'] != 'minecraft:air']


def looks_like_ground(layer):
    """A course of earth, sand or crops rather than a floor: most of the layer is ground-like."""
    ground = sum(1 for _, state in layer if state['Name'] in GROUND_BLOCKS)
    return layer and ground * 10 >= len(layer) * 7


def check(root):
    root = Path(root)
    findings = []
    for definition in sorted((root / 'kithkyn/buildings').glob('*.json')):
        info = json.loads(definition.read_text())
        template = root / 'structure' / (info['structure'] + '.nbt')
        if not template.exists():
            continue
        sink = info.get('sink', 0)
        layer = seated_layer(read(template), sink)
        stairs = [pos for pos, state in layer if state['Name'].endswith('_stairs')
                  and state.get('Properties', {}).get('half') == 'bottom']
        if not stairs:
            continue
        makeup = Counter(state['Name'].split(':')[1] for _, state in layer).most_common(3)
        findings.append((info['structure'], sink, len(stairs), len(layer), looks_like_ground(layer), makeup))
    return findings


if __name__ == '__main__':
    for root in sys.argv[1:]:
        print('==', root)
        for structure, sink, steps, size, ground, makeup in check(root):
            verdict = 'ground course with edging; judge by eye' if ground else 'STEPS BURIED: probably one block too low'
            print(f'  {structure:<36} sink {sink:>2}  {steps:>2} buried stair(s) in a layer of {size:>3}  {makeup}  -> {verdict}')
