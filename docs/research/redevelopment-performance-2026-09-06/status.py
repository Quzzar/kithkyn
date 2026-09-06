"""Read-only snapshot of the isolated autonomous redevelopment study."""
from pathlib import Path
from collections import Counter
import datetime,json,os,re,subprocess
ROOT=Path(__file__).resolve().parent
state=json.loads((ROOT/'state.json').read_text())
def alive(pid):
 if not pid:return False
 try:os.kill(pid,0);return True
 except ProcessLookupError:return False
summary={'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':state['status'],'active':state.get('active'),'runnerPid':state.get('runnerPid'),'runnerAlive':alive(state.get('runnerPid')),'runs':{}}
if 'error' in state:summary['error']=state['error']
for name in state['order']:
 job=state['jobs'].get(name,{})
 row={'status':job.get('status','pending')}
 if job.get('pid'):row['processAlive']=alive(job['pid'])
 result=ROOT/name/'redevelopment-timelapse-results.json'
 observations=[]
 if result.exists():
  try:observations=json.loads(result.read_text())
  except json.JSONDecodeError:row['observationWriteInProgress']=True
 if observations:
  last=observations[-1]
  row.update(day=last['elapsedTicks']/24000,samples=len(observations),builds=last['builds'],population=last['population'],project=last['project'],siteBlocker=last['siteBlocker'])
  for key in ['unhousedAdults','staffedFoodPosts','foodPerPerson','freeGeneralBeds','queuedItems','displacedResidents','freshWater']:
   row[key]={'last':last[key],'minSampled':min(o[key] for o in observations),'maxSampled':max(o[key] for o in observations)}
  row['sampledMetrics']=last['metrics']
  row['constructionAccessBlocker']=last.get('constructionAccessBlocker','')
  row['demolitionBlocksRemaining']=last.get('demolitionBlocksRemaining',0)
 transitionFile=ROOT/name/'redevelopment-transitions.json'
 if transitionFile.exists():
  try:row['transitions']=json.loads(transitionFile.read_text())
  except json.JSONDecodeError:row['transitionWriteInProgress']=True
 log=ROOT/(name+'.log')
 if log.exists():
  events=Counter();plans={};calls=0;begun=False;offer_in_call=False
  with log.open(errors='replace') as lines:
   for line in lines:
    if '[redevelopment-benchmark] begin ' in line:begun=True
    if not begun:continue
    event=re.search(r'\[redevelopment\] .*? plan=(\S+) stage=(\w+) count=(\d+) target=(\S+)',line)
    if event:
     plan,stage,count,target=event.groups();events[stage]+=int(count)
     if stage=='offered':offer_in_call=True
     if stage in ['chosen','saving','started','committed','completed','cancelled','buildings_removed']:
      entry=plans.setdefault(plan,{'target':target,'events':{}});entry['events'][stage]=entry['events'].get(stage,0)+int(count)
    if ' is choosing among ' in line:
     if offer_in_call:calls+=1
     offer_in_call=False
    if '[redevelopment-benchmark] COMPLETE' in line:break
  row['funnel']=dict(events);row['planningCallsWithOffers']=calls;row['adoptedPlans']=plans
  row['logLastModifiedAt']=datetime.datetime.fromtimestamp(log.stat().st_mtime,datetime.timezone.utc).isoformat()
 summary['runs'][name]=row
print(json.dumps(summary,indent=2))
