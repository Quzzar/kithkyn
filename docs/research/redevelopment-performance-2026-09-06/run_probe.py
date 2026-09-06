from pathlib import Path
import shutil,subprocess,sys,time,socket
ROOT=Path(__file__).resolve().parents[1]
DIAG=Path(__file__).resolve().parent
MODE=sys.argv[1]
ns={'__file__':str(ROOT/'runner.py')};exec((ROOT/'runner.py').read_text().split("lock=(ROOT/")[0],ns)
run=DIAG/MODE
if run.exists():raise SystemExit('Attempt exists; retain it and use a new diagnostic label.')
run.mkdir()
if not MODE.startswith('accounting'):shutil.copytree(ROOT/'seed-housing/world',run/'world')
ns['config'](run,False,True)
p=run/'server.properties';p.write_text(p.read_text().replace('server-port=25696','server-port=25697'))
with socket.socket() as s:s.bind(('127.0.0.1',25697))
cmd=ns['command']('',True);cmd=[c.replace(str(ROOT/'runtime/build/classes/java/main'),str(DIAG/'classes')) for c in cmd];cmd.insert(1,'-Dkithkyn.redevelopment.verify=true' if MODE.startswith('accounting') else '-Dkithkyn.accessProbe='+MODE)
with (DIAG/(MODE+'.log')).open('w') as log:
 proc=subprocess.Popen(cmd,cwd=run,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT)
 print('Diagnostic PID',proc.pid,flush=True);saved=None
 while proc.poll() is None:
  time.sleep(2)
  txt=(DIAG/(MODE+'.log')).read_text(errors='replace')
  if 'All dimensions are saved' in txt:
   if saved is None:saved=time.monotonic()
   elif time.monotonic()-saved>5:proc.terminate();break
 proc.wait(timeout=20)
txt=(DIAG/(MODE+'.log')).read_text(errors='replace')
for l in txt.splitlines():
 if '[house-access] RESULT' in l or '[house-access] RELOADED' in l or '[redevelopment-verify]' in l:print(l)
raise SystemExit(0 if ('[redevelopment-verify] PASS' in txt if MODE.startswith('accounting') else '[house-access] RESULT PASS' in txt) else 1)
