"""Run the production companion regression in a disposable, model-disabled Minecraft world."""
from pathlib import Path
import hashlib
import json
import shutil
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent.parent
STUDY = ROOT.parent / 'redevelopment-performance-2026-09-06'
label = sys.argv[1]
run = ROOT / label
if run.exists():
    raise SystemExit('Use a fresh label; existing evidence is preserved.')
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 25698))
namespace = {'__file__': str(STUDY / 'runner.py')}
exec((STUDY / 'runner.py').read_text().split('lock=(ROOT/')[0], namespace)
command = namespace['command']('', False)
run.mkdir()
shutil.copytree(STUDY / 'seed-housing/world', run / 'world')
namespace['config'](run, False, True)
properties = run / 'server.properties'
properties.write_text(properties.read_text().replace('server-port=25696', 'server-port=25698'))
classes = run / 'classes'
shutil.copytree(PROJECT / 'build/classes/java/main', classes)
old_classes = str(STUDY / 'runtime/build/classes/java/main')
command = [part.replace(old_classes, str(classes)) for part in command]
command.insert(1, '-Dkithkyn.companions.verify=true')
manifest = {str(path.relative_to(classes)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in classes.rglob('*.class')}
(run / 'class-checksums.json').write_text(json.dumps(manifest, indent=2) + '\n')
logpath = ROOT / (label + '.log')
with logpath.open('w') as log:
    process = subprocess.Popen(command, cwd=run, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)
    print('Isolated production-verification PID', process.pid, flush=True)
    saved = None
    deadline = time.monotonic() + 180
    try:
        while process.poll() is None:
            time.sleep(1)
            output = logpath.read_text(errors='replace')
            if 'All dimensions are saved' in output:
                if saved is None:
                    saved = time.monotonic()
                elif time.monotonic() - saved > 5:
                    process.terminate()
                    break
            if time.monotonic() > deadline:
                raise TimeoutError('Verification exceeded three minutes')
        process.wait(timeout=20)
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
output = logpath.read_text(errors='replace')
for line in output.splitlines():
    if '[companion-verify]' in line:
        print(line)
raise SystemExit(0 if '[companion-verify] RESULT PASS' in output else 1)
