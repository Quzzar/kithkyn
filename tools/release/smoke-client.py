#!/usr/bin/env python3
"""Join the production smoke server with the packaged mod in the prepared native dev client."""
import argparse
from pathlib import Path
import os
import subprocess
import shutil

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--jar', type=Path, required=True)
p.add_argument('--directory', type=Path, required=True)
p.add_argument('--java', default='java')
p.add_argument('--port', type=int, default=25680)
p.add_argument('--mode', choices=['chat', 'trade', 'age-lineup', 'undead-lineup'], default='chat')
p.add_argument("--curios", type=Path)
args = p.parse_args()
root = Path(__file__).resolve().parents[2]
neo_version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('neo_version='))
work = args.directory.resolve()
work.mkdir(parents=True, exist_ok=True)
if args.curios:
    (work / "mods").mkdir(exist_ok=True)
    shutil.copyfile(args.curios, work / "mods" / args.curios.name)
runtime = root / 'build/moddev'
if not (runtime / 'clientJoinLocalRunVmArgs.txt').exists():
    p.error('Run ./gradlew prepareClientJoinLocal first')
program = (runtime / 'clientJoinLocalRunProgramArgs.txt').read_text().replace('localhost:25565', f'127.0.0.1:{args.port}')
(work / 'client-arguments.txt').write_text(program + '\n--username\nDev\n')
(work / 'options.txt').write_text('onboardAccessibility:false\npauseOnLostFocus:false\nguiScale:2\nrenderDistance:8\nmaxFps:60\n')
classpath = os.pathsep.join([str(args.jar.resolve()), str(runtime / f'artifacts/neoforge-{neo_version}-minecraft.jar'),
    *(runtime / 'clientJoinLocalLegacyClasspath.txt').read_text().splitlines()])
command = [args.java, '-Xmx2G', '-Dfml.modFolders=kithkyn%%' + str(args.jar.resolve()),
    '@' + str(runtime / 'clientJoinLocalRunVmArgs.txt'), '-Dkithkyn.uipreview=' + args.mode,
    '-cp', classpath, 'net.neoforged.devlaunch.Main', '@' + str(work / 'client-arguments.txt')]
with (work / 'client.log').open('w') as log:
    subprocess.run(command, cwd=work, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=300)
shot = work / 'screenshots' / ('ui-' + args.mode + '.png')
if not shot.exists(): raise RuntimeError('Native screenshot was not produced; inspect client.log')
print('PASS packaged-jar client connected and rendered:', shot)
