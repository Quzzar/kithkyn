#!/usr/bin/env python3
"""Install the release jar into a disposable, real NeoForge dedicated server."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--jar', type=Path, required=True)
parser.add_argument('--directory', type=Path, required=True, help='Disposable installation directory; never use a player world')
parser.add_argument('--java', default='java', help='Java 21 executable')
parser.add_argument('--installer', type=Path)
parser.add_argument('--accept-eula', action='store_true', required=True)
parser.add_argument('--disable-llm', action='store_true', help='Catalog-only pass; does not qualify as an AI first-boot check')
parser.add_argument('--curios', type=Path)
parser.add_argument('--keep-alive', action='store_true', help='Leave the server available for a manual client check after catalog verification')
parser.add_argument('--timeout', type=int, default=2700)
parser.add_argument('--port', type=int, default=25679)
parser.add_argument('--jvm-arg', action='append', default=[])
parser.add_argument('--stop-file', type=Path, help='For a client session, creating this file requests a normal server stop')
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
neo_version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('neo_version='))
directory = args.directory.resolve()
directory.mkdir(parents=True, exist_ok=True)
if (directory / 'world').exists():
    sys.exit('Refusing to reuse a world. Choose a new disposable directory.')
jar = args.jar.resolve()
if jar.is_dir():
    candidates = [path for path in jar.glob("kithkyn-*.jar") if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if len(candidates) != 1: sys.exit("Expected exactly one installable Kithkyn jar")
    jar = candidates[0]
if not jar.is_file(): sys.exit('Mod jar does not exist')
if not args.disable_llm:
    with socket.socket() as probe:
        if probe.connect_ex(('127.0.0.1', 8127)) == 0:
            sys.exit('Port 8127 is already in use; stop the other local AI server before testing.')
install_log = directory / 'installer.log'
argument_file = directory / 'libraries/net/neoforged/neoforge' / neo_version / ('win_args.txt' if sys.platform == 'win32' else 'unix_args.txt')
if not argument_file.exists():
    installer = args.installer.resolve() if args.installer else directory / 'neoforge-installer.jar'
    if not installer.exists():
        urllib.request.urlretrieve(f'https://maven.neoforged.net/releases/net/neoforged/neoforge/{neo_version}/neoforge-{neo_version}-installer.jar', installer)
    with install_log.open('w') as log:
        subprocess.run([args.java, '-jar', str(installer), '--installServer', str(directory)], cwd=directory, stdout=log, stderr=subprocess.STDOUT, check=True)
mods = directory / 'mods'
mods.mkdir(exist_ok=True)
if list(mods.glob('*.jar')): sys.exit('Refusing an installation that already contains mods')
installed_jar = mods / jar.name
shutil.copy2(jar, installed_jar)
if args.curios: shutil.copy2(args.curios.resolve(), mods / args.curios.name)
(directory / 'eula.txt').write_text('eula=true\n')
(directory / 'server.properties').write_text('''server-ip=127.0.0.1
server-port=25679
online-mode=false
level-type=minecraft:flat
generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":143}],"structure_overrides":[]}
view-distance=2
simulation-distance=2
spawn-protection=0
max-tick-time=-1
'''.replace('server-port=25679', 'server-port=' + str(args.port)))
config = directory / 'config'
config.mkdir(exist_ok=True)
# Config is fixed for the lifetime of this fixture; it does not need live file watching.
(config / 'fml.toml').write_text('disableConfigWatcher=true\n')
(config / 'kithkyn-common.toml').write_text('[general]\n"Generate villages" = false\n[llm]\n"Enable LLM?" = ' + ('false' if args.disable_llm else 'true') + '\n')
command = [args.java, '-Xmx2G', '-Dkithkyn.bundledCatalog.verify=true', *args.jvm_arg]
if not args.disable_llm: command.append('-Dkithkyn.bundledCatalog.requireLlm=true')
if args.keep_alive: command.append('-Dkithkyn.bundledCatalog.keepAlive=true')
command += ['@' + str(argument_file), '--nogui']
log_path = directory / 'production-smoke.log'
print('Starting packaged server; log:', log_path, flush=True)
timed_out = False
with log_path.open('w') as log:
    process = subprocess.Popen(command, cwd=directory, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
    deadline = time.monotonic() + args.timeout
    saved_at = None
    while process.poll() is None and time.monotonic() < deadline:
        time.sleep(1)
        if args.stop_file and args.stop_file.exists():
            process.stdin.write("stop\n")
            process.stdin.flush()
            process.wait(timeout=45)
            break
        if "All dimensions are saved" in log_path.read_text(errors="replace"):
            if saved_at is None: saved_at = time.monotonic()
            elif time.monotonic() - saved_at > 45: break
    if process.poll() is None:
        timed_out = True
        process.stdin.write('stop\n')
        process.stdin.flush()
        try: process.wait(timeout=45)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
content = log_path.read_text(errors='replace')
checks = [line for line in content.splitlines() if '[bundled-catalog-verify]' in line]
errors = [line for line in content.splitlines() if '/ERROR]' in line]
with socket.socket() as probe:
    runtime_stopped = args.disable_llm or probe.connect_ex(('127.0.0.1', 8127)) != 0
passed = (not timed_out and process.returncode == 0 and 'Done (' in content
          and '[bundled-catalog-verify] RESULT PASS' in content and not errors and runtime_stopped
          and (config / 'kithkyn-advanced.toml').exists()
          and (args.disable_llm or 'Local runtime ready:' in content))
report = {'passed': passed, 'os': sys.platform, 'neoforge': neo_version, 'jar_sha256': hashlib.sha256(installed_jar.read_bytes()).hexdigest(),
          'llm_enabled': not args.disable_llm, 'curios': bool(args.curios), 'runtime_stopped': runtime_stopped,
          'exit_code': process.returncode, 'timed_out': timed_out, 'checks': checks, 'errors': errors,
          'log': str(log_path)}
(directory / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
print('\n'.join(checks[-5:] + errors[:10]))
print('PASS' if passed else 'FAIL', 'packaged server; report:', directory / 'verification.json')
sys.exit(0 if passed else 1)
