"""Export the immutable approved Birch captures; never reads or modifies a live world.

Run with the approved directory and an output asset directory. Definitions are printed as
an apply_patch patch; native Minecraft NBT writes only derived binary structure assets.
"""
from pathlib import Path
from collections import deque
import hashlib
import json
import subprocess
import sys
from nbt import read

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(sys.argv[1]).resolve()
OUTPUT = Path(sys.argv[2]).resolve()
assets_only = '--assets-only' in sys.argv[3:]
only = next((set(arg.removeprefix('--only=').split(',')) for arg in sys.argv[3:] if arg.startswith('--only=')), None)
assert only is None or assets_only, '--only requires --assets-only'
inventory = json.loads((SOURCE / 'captures/inventory.json').read_text())
old = {e['name']: e for e in json.loads((SOURCE / 'gallery-selection.json').read_text())['exhibits']}
amenities = json.loads((SOURCE / 'previous-amenities-reference.json').read_text())
definitions = ROOT / 'src/main/resources/data/kithkyn/kithkyn/buildings'
work = ROOT / 'run/birch-integration'
work.mkdir(parents=True, exist_ok=True)

# These are actual final-capture coordinates, not offsets from the earlier candidate gallery.
STATIONS = {
 'village_center_1': [('BUILDER',[12,1,12]),('BUILDER',[13,1,12]),('BUILDER',[15,1,12]),('GUARD',[14,5,5])],
 'storehouse_1': [('QUARTERMASTER',[8,1,8])],
 'watchtower_1': [('GUARD',[9,10,7])],
 'watchtower_2': [('GUARD',[7,10,8]),('GUARD',[9,10,8])],
 'farm_1': [('FARMER',[9,2,8])], 'farm_2': [('FARMER',[9,2,8])],
 'lumberjack_1': [('LUMBERJACK',[3,1,4])],
 'stoneworks_1': [('MASON',[6,4,11])],
 'mine_1': [('MINER',[8,0,7])],
 'hunting_lodge_1': [('HUNTER',[7,1,8])],
 'fishery_1': [('FISHER',[10,2,10])],
 'bakery_1': [('BAKER',[7,1,6])],
 'butchery_1': [('BUTCHER',[8,1,9]),('HERDER',[11,1,9])],
 'blacksmith_1': [('BLACKSMITH',[9,1,11])],
 'market_1': [('MERCHANT',[8,1,7])],
 'market_2': [('MERCHANT',[7,1,7]),('MERCHANT',[15,1,7])],
 'market_3': [('MERCHANT',[10,1,8]),('MERCHANT',[7,1,15]),('MERCHANT',[15,1,15])],
 'church_1': [('CLERIC',[8,3,10])],
}
WALLS={'wall_straight':'straight','wall_diagonal':'diagonal','wall_terrace':'terrace','wall_corner':'corner_tower','gatehouse':'gatehouse'}
WALL_REVISIONS=json.loads(Path(__file__).with_name('birch-walls-20260908.json').read_text())['pieces']
REVISIONS={'village_center_1':'birch-center-20260908.json',
           'storehouse_1':'birch-storehouse-20260908.json',
           'fishery_1':'birch-fishery-20260908.json',
           'mine_1':'birch-mine-20260908.json',
           'bakery_1':'birch-bakery-20260909.json'}

# The outward approach in the approved capture, not the door block's facing,
# which often points into the room. Open compounds use their public entry edge.
ENTRANCE_FRONTS = {
 'house_1':'west', 'house_2':'west', 'couples_cottage_1':'west',
 'bakery_1':'west', 'blacksmith_1':'west', 'butchery_1':'west', 'church_1':'west',
 'fishery_1':'west', 'hunting_lodge_1':'west', 'lumberjack_1':'north',
 'stoneworks_1':'west', 'watchtower_1':'west', 'watchtower_2':'south',
 'market_1':'west', 'market_2':'north', 'market_3':'south',
 'storehouse_1':'west', 'mine_1':'west',
}

def enclosed_air(grid, width, depth, y):
    """Only carve enclosed basement rooms, leaving surrounding subterranean terrain intact."""
    vacant={(x,y,z) for x in range(width) for z in range(depth) if (x,y,z) not in grid}
    exterior={p for p in vacant if p[0] in (0,width-1) or p[2] in (0,depth-1)}
    queue=deque(exterior)
    while queue:
        x,_,z=queue.popleft()
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            p=(x+dx,y,z+dz)
            if p in vacant and p not in exterior:
                exterior.add(p);queue.append(p)
    return vacant-exterior

plan=[];patch=['*** Begin Patch'];report=[]
for item in inventory:
    name=item['name']; source=SOURCE/'captures'/(name+'.nbt'); data=read(source)
    grid={tuple(b['pos']):data['palette'][b['state']] for b in data['blocks']}
    width,height,depth=data['size']; white=[]; air=set()
    spec={'source':str(source),'white':white,'air':[],'size':[width,height,depth]}
    revision=None
    if name in REVISIONS:
        revision=json.loads(Path(__file__).with_name(REVISIONS[name]).read_text())
        assert hashlib.sha256(source.read_bytes()).hexdigest()==revision['approved_source_sha256'], f'{name} revision requires its original approved capture; do not apply old edits to a newer source'
        width,height,depth=revision['size']
        spec['overrides']=revision['overrides']
        # Derive amenities and clearance from the edited shape, including removed
        # storage and new entrance steps, while the native writer preserves NBT.
        for edit in revision['overrides']:
            pos=tuple(edit['pos'])
            if edit['name']=='minecraft:air':
                grid.pop(pos,None)
            else:
                grid[pos]={'Name':edit['name'],'Properties':edit['properties']}
    if name in WALLS:
        wall_revision=WALL_REVISIONS[name]
        crop=wall_revision.get('crop',[4,0,4])
        spec.update(output=str(OUTPUT/'wall/birch_forest'/(WALLS[name]+'.nbt')),crop=crop,size=[width-2*crop[0],height,depth-2*crop[2]])
        assert hashlib.sha256(source.read_bytes()).hexdigest()==wall_revision['approved_source_sha256'], f'{name} wall edits require the original approved capture'
        assert spec['size']==wall_revision['size'], f'{name} cropped wall size changed'
        # The native writer crops first, then applies these cropped-local edits.
        spec['overrides']=wall_revision['overrides']
        plan.append(spec);continue
    category,level=name.rsplit('_',1)
    if category=='couples_cottage':category='couple_cottage'
    identifier=f'{category}_birch_forest_{level}'
    previous=amenities[name]
    delta=[old[name]['origin'][i]-item['worldOrigin'][i] for i in range(3)]
    def rebase(p): return [p[i]+delta[i] for i in range(3)]
    slots={key:[rebase(p) for p in values] for key,values in previous['village_identity'].items()}
    if revision is not None and 'banners' in revision:
        slots['banners']=revision['banners']
    if revision is not None and 'village_identity' in revision:
        slots=revision['village_identity']
    heads=sorted([list(p) for p,v in grid.items() if v['Name'].endswith('_bed') and v.get('Properties',{}).get('part')=='head'])
    declared=sorted(slots['primary_blocks']+slots['secondary_blocks'])
    assert heads==declared,(name,'bed roles drifted',heads,declared)
    for role in ('primary_blocks','secondary_blocks'):
        for p in slots[role]:
            white.append(p);dx,dz={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}[grid[tuple(p)]['Properties']['facing']]
            white.append([p[0]-dx,p[1],p[2]-dz])
    white.extend(slots['banners'])
    personal=[rebase(p) for p in previous['personal_containers']]
    containers=[list(p) for p,v in grid.items() if v['Name'] in ('minecraft:chest','minecraft:trapped_chest','minecraft:barrel')]
    assert all(p in containers for p in personal),(name,'personal container missing')
    base=json.loads((definitions/f'{category}_plains_{level}.json').read_text())
    sink=201-item['worldOrigin'][1]
    info={'structure':identifier,'category':category,'variant':'birch_forest','beds':heads,
          'work_stations':[{'occupation':o,'pos':p} for o,p in STATIONS.get(name,[])],
          'personal_containers':personal,'containers':[p for p in containers if p not in personal],
          'grants':base.get('grants',[]),'sink':sink,'village_identity':slots}
    if 'cost' in previous:info['cost']=previous['cost']
    if revision is not None and 'cost' in revision:info['cost']=revision['cost']
    if 'grants_if' in base:info['grants_if']=base['grants_if']
    if int(level)>1:info['upgrades_from']=f'{category}_birch_forest_{int(level)-1}'
    if name=='village_center_1':info['gathering_point']=[14,1,14]
    if name in ENTRANCE_FRONTS:info['entrance_facing']=ENTRANCE_FRONTS[name]
    if name=='storehouse_1':info.update(sink=-1)
    # Leave one ground-level landing after the authored stair before excavation descends.
    if name=='mine_1':info.update(mine_entrance={'facing':'east','offset':[1,0,1]})
    if category=='watchtower':info['grants']=list(dict.fromkeys(info['grants']+['RANGED_GUARD_POSTS']))
    if name=='bakery_1':info['grants']=['BREAD']
    if name=='butchery_1':info['grants']=[g for g in info['grants'] if g not in ('WOOL','CLOTH')]
    # Keep the saved local coordinate frame, but do not write the capture's empty
    # border. Gameplay bounds are derived from the same authored solid envelope.
    # The stoneworks' lead-in stair is deliberately outside its old wall line.
    occupied=list(grid)
    if name=='stoneworks_1':occupied.append((3,1,5))
    min_x,max_x=min(p[0] for p in occupied),max(p[0] for p in occupied)
    min_z,max_z=min(p[2] for p in occupied),max(p[2] for p in occupied)
    spec['horizontal_bounds']=[min_x,min_z,max_x,max_z]
    for y in range(sink+1,height):
        air.update((x,y,z) for x in range(min_x,max_x+1) for z in range(min_z,max_z+1) if (x,y,z) not in grid)
    if sink>0:
        for y in range(1,sink+1):air.update(enclosed_air(grid,width,depth,y))
    if name=='mine_1':air.update((x,0,z) for x in range(7,10) for z in range(7,10) if (x,0,z) not in grid)
    spec.update(output=str(OUTPUT/(identifier+'.nbt')),air=[list(p) for p in sorted(air)],size=[width,height,depth])
    if name=='stoneworks_1':
        # The approved raised entrance starts two navigation levels above the ground.
        # One matching lead-in stair makes its existing walkway accessible.
        spec['overrides']=[{'pos':[3,1,5],'name':'minecraft:birch_stairs',
            'properties':{'facing':'east','half':'bottom','shape':'straight','waterlogged':'false'}}]
    plan.append(spec)
    path=definitions/(identifier+'.json')
    if not assets_only:
        assert not path.exists(),f'Refusing to replace existing definition {path}'
        patch += ['*** Add File: '+str(path)]+['+'+line for line in json.dumps(info,indent=2).splitlines()]
    report.append({'id':identifier,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'source':str(source),'size':spec['size'],'sink':info['sink'],'beds':len(heads),'stations':info['work_stations'],'identity':slots,'carved_air':len(air)})
# Later approvals use their own immutable captures and explicit amenity metadata.
addition=json.loads(Path(__file__).with_name('birch-tavern-20260909.json').read_text())
source=ROOT/addition['source']
assert hashlib.sha256(source.read_bytes()).hexdigest()==addition['approved_source_sha256'], 'Tavern approval capture changed'
info=addition['definition'];data=read(source)
grid={tuple(b['pos']):data['palette'][b['state']] for b in data['blocks']}
occupied=[p for p,state in grid.items() if state['Name'] not in ('minecraft:air','minecraft:structure_void')]
white=[]
for position in info['village_identity']['primary_blocks']+info['village_identity']['secondary_blocks']:
    state=grid[tuple(position)]
    assert state['Name'].endswith('_bed') and state['Properties']['part']=='head'
    dx,dz={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}[state['Properties']['facing']]
    white += [position,[position[0]-dx,position[1],position[2]-dz]]
for position in info['containers']+info['personal_containers']:
    assert grid[tuple(position)]['Name'] in ('minecraft:chest','minecraft:barrel','minecraft:trapped_chest')
spec={'source':str(source),'output':str(OUTPUT/(info['structure']+'.nbt')),'white':white,'air':[],
      'size':data['size'],'ground_layer':info['sink'],
      'horizontal_bounds':[min(p[0] for p in occupied),min(p[2] for p in occupied),max(p[0] for p in occupied),max(p[2] for p in occupied)]}
plan.append(spec)
report.append({'id':info['structure'],'source':str(source),'source_sha256':addition['approved_source_sha256'],
               'size':data['size'],'sink':info['sink'],'beds':len(info['beds']),
               'stations':info['work_stations'],'identity':info['village_identity']})
if not assets_only:
    path=definitions/(info['structure']+'.json')
    assert not path.exists(),f'Refusing to replace existing definition {path}'
    patch += ['*** Add File: '+str(path)]+['+'+line for line in json.dumps(info,indent=2).splitlines()]
patch.append('*** End Patch')
if only is not None:
    available={Path(item['output']).stem for item in plan}
    assert only <= available, f'Unknown export names: {only-available}'
    plan=[item for item in plan if Path(item['output']).stem in only]
    report=[item for item in report if item['id'] in only]
planfile=work/'export-plan.json';planfile.write_text(json.dumps(plan))
classpath=(ROOT/'build/moddev/serverLegacyClasspath.txt').read_text().splitlines()+[str(ROOT/'build/moddev/artifacts/neoforge-21.1.72-minecraft.jar')]
subprocess.run(['java','-cp',':'.join(classpath),str(Path(__file__).with_name('VillageTemplateExport.java')),str(planfile)],check=True,stdout=sys.stderr)
(work/'export-report.json').write_text(json.dumps(report,indent=2))
if not assets_only:print('\n'.join(patch))
