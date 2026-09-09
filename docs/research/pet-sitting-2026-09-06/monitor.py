"""Read live village logs; persist observations only, without controlling the server."""
from pathlib import Path
from collections import Counter, defaultdict
import datetime,json,re,subprocess,hashlib
ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[1]
LOGS=REPO/'run/logs'
state_path=ROOT/'monitor-state.json'
old=json.loads(state_path.read_text()) if state_path.exists() else {}
raw=(LOGS/'llm.log').read_text(errors='replace')
blocks=re.split(r'(?=\[\d\d:\d\d:\d\d\.\d+\] ==== #)',raw)
records=[];pending={}
for block in blocks:
 if not block.strip() or 'sees to their' not in block.splitlines()[0]:continue
 match=re.search(r'#(\d+) (REQUEST|REPLY) decide \| (.*?) \|',block.splitlines()[0])
 if not match:continue
 number,kind,purpose=match.groups();key=(number,purpose)
 if kind=='REQUEST':
  record={'purpose':purpose,'request':block.splitlines()[0],'sittingBefore':' is sitting where you left it.' in block,'reply':None}
  records.append(record);pending[key]=record
 else:
  record=pending.pop(key,None)
  if record is not None:
   try:record['reply']=json.loads(block.split('\n',1)[1].strip())
   except (ValueError,IndexError):record['reply']={'unparsed':True}
summary={}
for r in records:
 c=summary.setdefault(r['purpose'],Counter());c['requests']+=1
 if not r['reply']:c['pendingOrMissing']+=1;continue
 choice=r['reply'].get('choice');c['replies']+=1
 if choice==1:c['recall' if r['sittingBefore'] else 'sit']+=1
 elif choice==2:c['keep_sitting' if r['sittingBefore'] else 'keep_following']+=1
 else:c['invalid']+=1
latest=(LOGS/'latest.log').read_text(errors='replace')
interesting=[l for l in latest.splitlines() if any(s in l for s in ['[redevelopment]','sits down','calls back','[companion pet]'])]
seen=set(old.get('seenEvents',[]));events=[l for l in interesting if hashlib.sha256(l.encode()).hexdigest() not in seen]
processes=[]
for line in subprocess.check_output(['ps','-axo','pid=,ppid=,command='],text=True).splitlines():
 parts=line.split(None,2)
 if len(parts)==3 and re.match(r'\S*/java ',parts[2]) and '-Dfml.modFolders=' in parts[2] and str(REPO/'build/moddev/serverRunVmArgs.txt') in parts[2]:processes.append({'pid':int(parts[0]),'ppid':int(parts[1]),'identity':'Kithkyn development server from this project'})
redev=Counter()
for l in interesting:
 if '[redevelopment]' in l and (m:=re.search(r' stage=(\w+) count=(\d+)',l)):redev[m[1]]+=int(m[2])
snapshot={'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'serverProcesses':processes,'logModifiedAt':datetime.datetime.fromtimestamp((LOGS/'latest.log').stat().st_mtime,datetime.timezone.utc).isoformat(),'petChoices':summary,'redevelopmentEventsInCurrentLog':redev,'newEvents':events[-30:],'baseline':not bool(old),'limits':'Pet choices describe ordered state, not observed physical pose. Log rotation resets current-log counts; new events use content hashes. No game mutation.','seenEvents':list(dict.fromkeys([*old.get('seenEvents',[]),*(hashlib.sha256(l.encode()).hexdigest() for l in interesting)]))[-5000:]}
state_path.write_text(json.dumps(snapshot,indent=2)+'\n')
print(json.dumps({k:v for k,v in snapshot.items() if k!='seenEvents'},indent=2))
