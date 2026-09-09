from pathlib import Path
import subprocess,shutil,socket,sys,time
ROOT=Path(__file__).resolve().parent
STUDY=ROOT.parent/'redevelopment-performance-2026-09-06'
MODE=sys.argv[1]
classes=ROOT/'classes'
if not classes.exists():shutil.copytree(STUDY/'runtime/build/classes/java/main',classes)
ns={'__file__':str(STUDY/'runner.py')};exec((STUDY/'runner.py').read_text().split("lock=(ROOT/")[0],ns)
cmd=ns['command']('',False)
cp=cmd[cmd.index('-cp')+1]
java=Path(cmd[0]).parent
subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(ROOT/'PetSittingProbe.java')],check=True)
run=ROOT/MODE
if run.exists():raise SystemExit('Diagnostic label already exists; preserve it and use another.')
run.mkdir();shutil.copytree(STUDY/'seed-housing/world',run/'world');ns['config'](run,False,True)
p=run/'server.properties';p.write_text(p.read_text().replace('server-port=25696','server-port=25698'))
with socket.socket() as sock:sock.bind(('127.0.0.1',25698))
cmd=[c.replace(str(STUDY/'runtime/build/classes/java/main'),str(classes)) for c in cmd]
cmd.insert(1,'-Dkithkyn.petSittingProbe=true')
if MODE.startswith('control'):cmd.insert(1,'-Dkithkyn.petSittingProbe.control=true')
logpath=ROOT/(MODE+'.log')
with logpath.open('w') as log:
 proc=subprocess.Popen(cmd,cwd=run,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT)
 print('Isolated diagnostic PID',proc.pid,flush=True);saved=None
 while proc.poll() is None:
  time.sleep(1)
  text=logpath.read_text(errors='replace')
  if 'All dimensions are saved' in text:
   if saved is None:saved=time.monotonic()
   elif time.monotonic()-saved>5:proc.terminate();break
 proc.wait(timeout=20)
text=logpath.read_text(errors='replace')
for line in text.splitlines():
 if '[pet-sitting-probe]' in line:print(line)
raise SystemExit(0 if '[pet-sitting-probe] RESULT PASS' in text else 1)
