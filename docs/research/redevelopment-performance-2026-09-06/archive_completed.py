"""Archive completed study runs and verify fixed horizons; never writes to scored worlds or runtime."""
from pathlib import Path
import datetime, hashlib, json, re, shutil, subprocess, sys
ROOT = Path(__file__).resolve().parent
OUT = ROOT.parents[1] / 'docs/research/redevelopment-performance-2026-09-06'
status = json.loads(subprocess.check_output([sys.executable, str(ROOT / 'status.py')], text=True))
(ROOT / 'latest-status.json').write_text(json.dumps(status, indent=2) + '\n')
summary = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'studyStatus': status['status'], 'runs': {}}
for name, row in status['runs'].items():
    if row['status'] != 'complete':
        continue
    log = (ROOT / (name + '.log')).read_text(errors='replace')
    observations = json.loads((ROOT / name / 'redevelopment-timelapse-results.json').read_text())
    assert [o['elapsedTicks'] for o in observations] == list(range(0, 288001, 24000)), name
    assert '[redevelopment-benchmark] COMPLETE' in log and 'All dimensions are saved' in log, name
    persisted = {k.removeprefix('redevelopment_'): int(v) for k, v in re.findall(r'(redevelopment_\w+)=(\d+)', observations[-1]['metrics'])}
    assert persisted == row['funnel'], (name, persisted, row['funnel'])
    for source, dest in [('redevelopment-timelapse-results.json', name + '.json'), ('redevelopment-transitions.json', name + '-transitions.json')]:
        shutil.copy2(ROOT / name / source, OUT / dest)
    events = [line for line in log.splitlines() if any(marker in line for marker in ['[redevelopment]', '[redevelopment-search]', '[redevelopment-transition]', '[construction-access]', '[redevelopment-benchmark] COMPLETE'])]
    (OUT / (name + '-events.txt')).write_text('\n'.join(events) + '\n')
    blocks = re.split(r'(?=\[\d\d:\d\d:\d\d\.\d+\] ==== #)', (ROOT / name / 'logs/llm.log').read_text())
    ids = {re.search(r'#(\d+) REQUEST', b).group(1) for b in blocks if 'REQUEST' in b and 'builds next' in b}
    selected = [b for b in blocks if (m := re.search(r'==== #(\d+) ', b)) and m.group(1) in ids]
    (OUT / (name + '-model-trace.txt')).write_text(''.join(selected))
    timings = [tuple(map(float, m)) for m in re.findall(r'budgetExhausted=\w+ ms=([\d.]+) groupingMs=([\d.]+)', log)]
    tokens = [int(n) for n in re.findall(r'prompt eval time\s*=.*?/\s*(\d+) tokens', log)]
    searches = [tuple(map(int, m)) for m in re.findall(r'examined=(\d+) generated=(\d+) materialViable=(\d+) retained=(\d+)', log)]
    errors = [line for line in log.splitlines() if any(pattern in line.lower() for pattern in ['context size exceeded', 'exceeds the available context', 'too many tokens', '[redevelopment-benchmark] fail']) or re.search(r'truncated = [1-9]', line)]
    summary['runs'][name] = {
        'funnel': row['funnel'], 'planningCallsWithOffers': row['planningCallsWithOffers'],
        'planningRequests': len(ids), 'planningReplies': sum(' REPLY ' in b.splitlines()[0] for b in selected),
        'examinedGeneratedMaterialViableRetained': [sum(r[i] for r in searches) for i in range(4)],
        'maxSearchMs': max((a for a, b in timings), default=0), 'maxGroupingMs': max((b for a, b in timings), default=0),
        'maxEvaluatedPromptTokens': max(tokens, default=0), 'contextOrBenchmarkErrors': errors,
        'final': {k: observations[-1][k] for k in ['builds', 'population', 'project', 'unhousedAdults', 'staffedFoodPosts', 'foodPerPerson', 'freeGeneralBeds', 'queuedItems', 'displacedResidents', 'freshWater']},
        'fixedHorizonAndCountersVerified': True}
(OUT / 'completed-run-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
(OUT / 'interim-status.json').write_text(json.dumps(status, indent=2) + '\n')
shutil.copy2(Path(__file__), OUT / Path(__file__).name)
(OUT / 'checksums.json').write_text(json.dumps({p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.iterdir() if p.is_file() and p.name != 'checksums.json'}, indent=2) + '\n')
checkpoint_path = ROOT / 'monitoring-checkpoint.json'
checkpoint = json.loads(checkpoint_path.read_text())
checkpoint.update(checkedAt=summary['checkedAt'], completedArchives=summary['runs'])
checkpoint_path.write_text(json.dumps(checkpoint, indent=2) + '\n')
print(json.dumps(summary, indent=2))
