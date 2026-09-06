"""Sequential isolated autonomous comparisons; state and all evidence survive outside /tmp."""
from pathlib import Path
import datetime,fcntl,hashlib,json,os,re,shutil,signal,socket,subprocess,time,traceback
ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[1]
RUNTIME=ROOT/'runtime'
GAME_PORT=25696
MODEL_PORT=8131
BASELINE='73b5b4d19b88858ab094d95a71c5fd7f40e14172'
ORDER=json.loads((ROOT/'plan.json').read_text())['runOrder']

def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def save(state):
 state['updatedAt']=now();p=ROOT/'state.next.json';p.write_text(json.dumps(state,indent=2)+'\n');p.replace(ROOT/'state.json')
def port_free(port):
 with socket.socket() as probe:probe.bind(('127.0.0.1',port))
def digest(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for part in iter(lambda:f.read(1024*1024),b''):h.update(part)
 return h.hexdigest()

def config(run,enabled,flat):
 (run/'config').mkdir(parents=True,exist_ok=True)
 common=(ROOT/'base-common.toml').read_text()
 values={'Enable LLM?':'true' if enabled else 'false','LLM provider':'"local"','LLM API key':'""','LLM local model':'"llama-3b"','Villagers talk to each other':'false'}
 for key,value in values.items():
  common,n=re.subn(r'(?m)^(\s*"'+re.escape(key)+r'"\s*=\s*).*$',lambda m:m[1]+value,common)
  if n!=1:raise RuntimeError('Missing or duplicate config key '+key)
 (run/'config/kithkyn-common.toml').write_text(common)
 shutil.copy2(ROOT/'base-advanced.toml',run/'config/kithkyn-advanced.toml')
 shutil.copy2(REPO/'run/eula.txt',run/'eula.txt')
 settings={'biome':'minecraft:plains','layers':[{'block':'minecraft:bedrock','height':1},{'block':'minecraft:stone','height':64},{'block':'minecraft:dirt','height':3},{'block':'minecraft:grass_block','height':1}],'features':False,'lakes':False,'structure_overrides':[]}
 properties={'server-ip':'127.0.0.1','server-port':str(GAME_PORT),'online-mode':'false','enable-rcon':'false','level-name':'world','level-type':'minecraft:flat' if flat else 'minecraft:normal','level-seed':'2468013579','generate-structures':'false','max-tick-time':'0','view-distance':'6','simulation-distance':'6','spawn-protection':'0','generator-settings':json.dumps(settings,separators=(',',':')) if flat else '{}'}
 (run/'server.properties').write_text('\n'.join(k+'='+v for k,v in properties.items())+'\n')
 (run/'kithkyn/models').mkdir(parents=True,exist_ok=True)
 model=run/'kithkyn/models/Llama-3.2-3B-Instruct-Q4_K_M.gguf'
 if not model.exists():model.symlink_to(REPO/'run/kithkyn/models'/model.name)
 binary=run/'kithkyn/runtime'
 if not binary.exists():binary.symlink_to(ROOT/'runtime-cache')

def command(mode,enabled):
 cp=[str(RUNTIME/'build/classes/java/main'),str(RUNTIME/'build/resources/main'),str(RUNTIME/'build/moddev/artifacts/neoforge-21.1.72-minecraft.jar')]+(RUNTIME/'build/moddev/serverLegacyClasspath.txt').read_text().splitlines()
 cp.append(str(next(Path('/Users/quzzar/.gradle/caches/modules-2/files-2.1/net.neoforged/DevLaunch/1.0.1').glob('*/*.jar'))))
 java='/Users/quzzar/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home/bin/java'
 return [java,'-Xmx2G','-Dfml.modFolders=kithkyn%%'+str(RUNTIME/'build/classes/java/main')+':kithkyn%%'+str(RUNTIME/'build/resources/main'),'@'+str(RUNTIME/'build/moddev/serverRunVmArgs.txt'),'-Dkithkyn.redevelopment.benchmark='+mode,'-Dkithkyn.redevelopment.days=12','-Dkithkyn.redevelopment.enabled='+str(enabled).lower(),'-Dkithkyn.benchmark.modelPort='+str(MODEL_PORT),'-cp',':'.join(dict.fromkeys(cp)),'net.neoforged.devlaunch.Main','@'+str(RUNTIME/'build/moddev/serverRunProgramArgs.txt')]

def execute(state,name,run,mode,enabled):
 port_free(GAME_PORT);port_free(MODEL_PORT)
 logpath=ROOT/(name+'.log')
 row=state['jobs'].setdefault(name,{})
 if row.get('status')=='complete':return
 if logpath.exists():raise RuntimeError('Existing incomplete attempt must be reviewed before retry: '+name)
 with logpath.open('w') as log:
  proc=subprocess.Popen(command(mode,enabled),cwd=run,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
  row.update(status='running',pid=proc.pid,startedAt=now(),runDirectory=str(run),log=str(logpath));state['active']=name;save(state)
  print('START',name,'pid',proc.pid,flush=True)
  saved=False;finished_at=None
  while proc.poll() is None:
   time.sleep(3)
   with logpath.open(errors='replace') as f:
    f.seek(max(0,logpath.stat().st_size-40000));tail=f.read()
   if '[redevelopment-benchmark] FAIL' in tail:row['failureObserved']=True
   if 'All dimensions are saved' in tail:
    saved=True
    if finished_at is None:finished_at=time.monotonic()
    if time.monotonic()-finished_at>15:
     proc.terminate();break
  try:code=proc.wait(timeout=20)
  except subprocess.TimeoutExpired:proc.kill();code=proc.wait()
 logtext=logpath.read_text(errors='replace')
 passed='[redevelopment-benchmark] COMPLETE' in logtext if mode=='timelapse' else '[redevelopment-benchmark] seeded scenario=' in logtext
 passed=passed and '[redevelopment-benchmark] FAIL' not in logtext and 'All dimensions are saved' in logtext
 row.update(status='complete' if passed else 'failed',finishedAt=now(),exitCode=code)
 state['active']=None;save(state)
 print('FINISH',name,row['status'],code,flush=True)
 if not passed:raise RuntimeError('Benchmark failed: '+name)
 # A clean JVM shutdown owns termination of its model; verify that the next run can use both ports.
 for attempt in range(20):
  try:port_free(GAME_PORT);port_free(MODEL_PORT);break
  except OSError:
   if attempt==19:raise
   time.sleep(1)

lock=(ROOT/'runner.lock').open('w')
fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
state=json.loads((ROOT/'state.json').read_text()) if (ROOT/'state.json').exists() else {'baseline':BASELINE,'order':ORDER,'jobs':{},'createdAt':now(),'status':'preparing'}
state['runnerPid']=os.getpid();save(state)
try:
 manifest=json.loads((ROOT/'manifest.json').read_text())
 for scenario in ('housing','opportunity','established'):
  if digest(ROOT/('seed-'+scenario)/'world/level.dat') != manifest['seedLevelDataSha256'][scenario]:
   raise RuntimeError('Seed changed: '+scenario)
 state['status']='running';save(state)
 for name in ORDER:
  if state['jobs'].get(name,{}).get('status')=='complete':continue
  scenario,flag,repeat=name.split('-');run=ROOT/name
  if not run.exists():
   run.mkdir();shutil.copytree(ROOT/('seed-'+scenario)/'world',run/'world')
  config(run,True,True);execute(state,name,run,'timelapse',flag=='on')
 state['status']='complete';save(state)
except BaseException as error:
 state['status']='needs_attention';state['error']=str(error);save(state);traceback.print_exc();raise
