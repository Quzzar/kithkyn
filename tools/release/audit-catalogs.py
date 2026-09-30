#!/usr/bin/env python3
"""Verify the approved catalog manifest against source resources or the installable jar."""

import hashlib
import json
from pathlib import Path
import re
import sys
import tomllib
import zipfile


def audit(jar=None):
    root = Path(__file__).resolve().parents[2]
    manifest = json.loads((root / 'tools/release/bundled-catalogs.json').read_text())
    resource_root = root / 'src/main/resources'
    if jar and Path(jar).is_dir():
        candidates = [path for path in Path(jar).glob('kithkyn-*.jar') if not path.name.endswith(('-sources.jar', '-javadoc.jar'))]
        if len(candidates) != 1: raise ValueError('Expected exactly one installable Kithkyn jar')
        jar = candidates[0]
    archive = zipfile.ZipFile(jar) if jar else None
    failures = []
    names = set(archive.namelist()) if archive else {
        path.relative_to(resource_root).as_posix() for path in resource_root.rglob('*') if path.is_file()
    }
    declared = set()
    styles = {catalog['style'] for catalog in manifest['catalogs']}
    runtime = (root / 'src/main/java/com/quzzar/kithkyn/village/buildings/VillageStyle.java').read_text()
    enum_values = runtime.split('public enum VillageStyle {', 1)[1].split(';', 1)[0]
    runtime_styles = {value.lower() for value in re.findall(r'[A-Z][A-Z_]+', enum_values)}
    if styles != runtime_styles:
        failures.append(f'Manifest styles differ from runtime: {styles ^ runtime_styles}')
    for catalog in manifest['catalogs']:
        ids = {building['id'] for building in catalog['buildings']}
        if f"village_center_{catalog['style']}_1" not in ids:
            failures.append(f"Missing center for {catalog['style']}")
        for building in catalog['buildings']:
            for kind in ['definition', 'template']:
                path = building[kind].removeprefix('src/main/resources/')
                declared.add(path)
                if path not in names:
                    failures.append(f'Missing {path}')
                    continue
                content = archive.read(path) if archive else (resource_root / path).read_bytes()
                if hashlib.sha256(content).hexdigest() != building[kind + '_sha256']:
                    failures.append(f'Hash mismatch: {path}')
                if kind == 'definition':
                    data = json.loads(content)
                    template = 'data/kithkyn/structure/' + data['structure'].removeprefix('kithkyn:') + '.nbt'
                    if template not in names:
                        failures.append(f'Missing referenced template: {template}')
                    for dependency in data.get('starting_buildings', []) + ([data['upgrades_from']] if data.get('upgrades_from') else []):
                        if dependency not in ids:
                            failures.append(f"{building['id']} references absent regional building {dependency}")
    actual = {name for name in names if name.startswith('data/kithkyn/kithkyn/buildings/') and name.endswith('.json')}
    expected = {name for name in declared if name.endswith('.json')}
    if actual != expected:
        failures.append(f'Unrecorded or missing building definitions: {actual ^ expected}')
    if archive:
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
        mod = metadata['mods'][0]
        for key in ('credits', 'authors', 'logoFile', 'updateJSONURL', 'displayURL'):
            if not mod.get(key): failures.append(f'Missing mod metadata: {key}')
        if mod.get('logoFile') not in names: failures.append('Missing mod-list logo')
        for notice in ('META-INF/LICENSE', 'META-INF/README.md'):
            if notice not in names: failures.append(f'Missing bundled notice: {notice}')
    if archive:
        forbidden = [name for name in names if '.DS_Store' in name or name.startswith(('run/', 'tools/', 'Website/'))]
        failures.extend(f'Unexpected jar entry: {name}' for name in forbidden)
        archive.close()
    if failures:
        print('\n'.join(f'FAIL: {failure}' for failure in failures))
        return 1
    print(f'PASS: {len(styles)} bundled styles, {len(actual)} building definitions and all referenced templates' + (f' in {jar}' if jar else ''))
    return 0


if __name__ == '__main__':
    sys.exit(audit(sys.argv[1] if len(sys.argv) > 1 else None))
