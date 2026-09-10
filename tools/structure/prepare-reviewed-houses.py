"""Prepare neutral templates and identity-only previews from reviewed building metadata."""
from pathlib import Path
import argparse
import copy
import hashlib
import json
import subprocess

from nbt import read

ROOT = Path(__file__).resolve().parents[2]


def export_production(manifest, manifest_path, java):
    """Bind already reviewed neutral geometry to the shared production catalog."""
    entries = manifest.get('facilities', manifest.get('houses', [manifest] if 'info' in manifest else []))
    work = ROOT / 'run/badlands-integration' / manifest_path.stem
    work.mkdir(parents=True, exist_ok=True)
    recipes = ROOT / 'src/main/resources/data/kithkyn/kithkyn/buildings'
    datapack = ROOT / 'run/badlands-integration/datapack'
    definitions = datapack / 'data/kithkyn/kithkyn/buildings'
    structures = datapack / 'data/kithkyn/structure'
    datapack.mkdir(parents=True, exist_ok=True)
    (datapack / 'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 48,
        'description': 'Locally approved Badlands village catalog'}}, indent=2) + '\n')
    plan, report, patch = [], [], ['*** Begin Patch']
    for entry in entries:
        binding = entry.get('production')
        if entry.get('status', '').startswith('superseded'):
            assert binding is None, 'A superseded reference cannot enter the production catalog'
            continue
        assert binding, f"Missing production binding: {entry['exhibit']}"
        source = ROOT / entry['neutral_template']
        source_hash = hashlib.sha256(source.read_bytes()).hexdigest()
        assert source_hash == entry['neutral_template_sha256'], f"Reviewed neutral geometry changed: {source}"
        data = read(source)
        info = copy.deepcopy(entry.get('amenities', entry.get('info')))
        recipe = json.loads((recipes / (binding['recipe'] + '.json')).read_text())
        info.update(structure=binding['structure'], category=binding['category'],
                    variant=binding['variant'], cost=recipe['cost'])
        info.update(binding.get('definition_overrides', {}))
        if 'upgrades_from' in binding:
            info['upgrades_from'] = binding['upgrades_from']
        else:
            assert 'upgrades_from' not in info, 'An authored upgrade requires an explicit production predecessor'
        identity = info['village_identity']
        accents = {tuple(pos) for role in ('primary_blocks', 'secondary_blocks') for pos in identity[role]}
        white = []
        changed = []
        for block in data['blocks']:
            position = tuple(block['pos'])
            state = data['palette'][block['state']]
            name = state['Name']
            if position in accents and name.endswith(('_banner', '_wall_banner')):
                assert not block.get('nbt', {}).get('patterns'), 'Cloth accents must not contain heraldry'
                white.append(list(position))
                if not name.startswith('minecraft:white_'):
                    changed.append({'pos': list(position), 'from': name, 'to': 'minecraft:white_wall_banner'
                                    if name.endswith('_wall_banner') else 'minecraft:white_banner'})
        output = structures / (info['structure'] + '.nbt')
        plan.append({'source': str(source), 'output': str(output), 'white': white, 'air': [], 'size': data['size']})
        target = definitions / (info['structure'] + '.json')
        content = json.dumps(info, indent=2) + '\n'
        if target.exists():
            assert target.read_text() == content, f'Refusing to overwrite an edited definition: {target}'
        else:
            patch += ['*** Add File: ' + str(target)] + ['+' + line for line in content.splitlines()]
        report.append({'exhibit': entry['exhibit'], 'structure': info['structure'],
                       'review_manifest': str(manifest_path.relative_to(ROOT)),
                       'reviewed_neutral': entry['neutral_template'], 'reviewed_neutral_sha256': source_hash,
                       'recipe': binding['recipe'], 'definition': str(target.relative_to(ROOT)),
                       'asset': str(output.relative_to(ROOT)), 'identity_neutralization': changed})
    planfile = work / 'export-plan.json'
    planfile.write_text(json.dumps(plan, indent=2) + '\n')
    classpath = (ROOT / 'build/moddev/serverLegacyClasspath.txt').read_text().splitlines()
    classpath.append(str(ROOT / 'build/moddev/artifacts/neoforge-21.1.72-minecraft-merged.jar'))
    subprocess.run([java, '-cp', ':'.join(classpath), str(ROOT / 'tools/structure/VillageTemplateExport.java'),
                    str(planfile)], check=True)
    for exported in report:
        exported['asset_sha256'] = hashlib.sha256((ROOT / exported['asset']).read_bytes()).hexdigest()
    (work / 'export-report.json').write_text(json.dumps(report, indent=2) + '\n')
    patch.append('*** End Patch')
    (work / 'definitions.patch').write_text('\n'.join(patch) + '\n')
    print('Prepared', len(report), 'production templates; definitions:', work / 'definitions.patch')


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('manifest', type=Path)
parser.add_argument('--java', default='java', help='Java 21 executable; defaults to java on PATH')
parser.add_argument('--production', action='store_true', help='Export final reviewed neutral templates and production definition patches')
options = parser.parse_args()
manifest = json.loads(options.manifest.read_text())
if options.production:
    export_production(manifest, options.manifest.resolve(), options.java)
    raise SystemExit(0)
work = ROOT / manifest['work']
patches, exports, guards, wanted = [], [], [], []
directions = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
entries = manifest.get('facilities', manifest.get('houses', []))
for house in entries:
    source = ROOT / house['source_capture']
    assert hashlib.sha256(source.read_bytes()).hexdigest() == house['source_sha256']
    data = read(source)
    states = {tuple(b['pos']): data['palette'][b['state']] for b in data['blocks']}
    amenities = house['amenities']
    identity = amenities['village_identity']
    roles = {tuple(pos): role for role in ['primary', 'secondary'] for pos in identity[role + '_blocks']}
    assert {tuple(pos) for pos in amenities['beds']} <= set(roles)
    halves = {}
    for head in amenities['beds']:
        point = tuple(head)
        state = states[point]
        assert state['Name'].endswith('_bed') and state['Properties']['part'] == 'head'
        dx, dz = directions[state['Properties']['facing']]
        foot = (head[0] - dx, head[1], head[2] - dz)
        assert states[foot]['Name'] == state['Name'] and states[foot]['Properties']['part'] == 'foot'
        halves[point] = roles[point]
        halves[foot] = roles[point]
    for pair in amenities['couple_beds']:
        assert len(pair) == 2 and roles[tuple(pair[0])] == roles[tuple(pair[1])]
    accents = {pos: role for pos, role in roles.items() if pos not in halves}
    assert all(states[pos]['Name'].endswith('_wool') for pos in accents)
    colored = {**halves, **accents}
    edits = []
    for position, state in states.items():
        if position not in colored:
            edits.append({'pos': list(position), 'remove': True})
            continue
        color = manifest['preview_colors'][colored[position]]
        suffix = '_bed' if position in halves else '_wool'
        target = {'Name': 'minecraft:' + color + suffix}
        if state.get('Properties'): target['Properties'] = dict(state['Properties'])
        world = [position[i] + house['template_origin'][i] for i in range(3)]
        guards.append({'world': world, 'state': state, 'exhibit': house['exhibit']})
        wanted.append({'world': world, 'state': target, 'exhibit': house['exhibit']})
        edits.append({'pos': list(position), 'state': json.dumps(target, separators=(',', ':'))})
    patches.append({'name': house['exhibit'].lower() + '-beds', 'source': str(source), 'edits': edits, 'entities': '[]'})
    export_options = house.get('export_options', {})
    if house.get('export_options_file'):
        export_options = json.loads((ROOT / house['export_options_file']).read_text())
    export = {'source': str(ROOT / house.get('prepared_source', house['source_capture'])),
              'output': str(ROOT / house['neutral_template']),
              'white': [list(pos) for pos in halves] + identity['banners'], 'air': [], 'size': house['size'],
              **export_options}
    export['overrides'] = export.get('overrides', []) + [
        {'pos': list(pos), 'name': 'minecraft:white_wool', 'properties': {}} for pos in accents]
    exports.append(export)
for name, value in [('patch-plan', {'output': str(work / 'patches'), 'templates': patches}),
                    ('export-plan', exports), ('guards', guards), ('wanted', wanted)]:
    (work / (name + '.json')).write_text(json.dumps(value, indent=2) + '\n')
classpath = (ROOT / 'build/moddev/serverLegacyClasspath.txt').read_text().splitlines()
classpath.append(str(ROOT / 'build/moddev/artifacts/neoforge-21.1.72-minecraft-merged.jar'))
for source, arguments in [(ROOT / 'run/valecraft-gallery/GallerySnapshot.java', ['patch', str(work / 'patch-plan.json')]),
                           (ROOT / 'tools/structure/VillageTemplateExport.java', [str(work / 'export-plan.json')])]:
    subprocess.run([options.java, '-cp', ':'.join(classpath), str(source), *arguments], check=True)
print('Prepared', len(entries), 'buildings and', len(wanted), 'identity blocks.')
