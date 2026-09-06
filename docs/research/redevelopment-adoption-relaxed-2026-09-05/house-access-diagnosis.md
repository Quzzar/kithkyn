# Why the selected replacement could not commit

The saved builder opens the house door but cannot jump through the raised entrance under
its low lintel. The outside floor is y=5, inside floor is y=6, and the doorway ceiling begins
at y=8. The builder is 1.989 blocks tall with a 0.6-block automatic step height. A one-block
rise requires jumping; that jump conflicts with the doorway clearance. The pathfinder can
plan this route while actual movement remains stuck outside, near x=128.694.

This is exposed during redevelopment because GatherStep requires a complete recipe to be
carried near the existing building center (131,6,131) before commitment. The old house is
still standing. The ordinary gathering timeout eventually cancels the project with a generic
recipe message. This was not a spare-bed refusal or a model declining the selected action.

## Reproduction and controls

A development-only probe uses the original saved housing seed and builder, the real
GatherStep, WorkLoopGoal and OpenDoorGoal, and the frozen production baseline. Other mobs'
AI is disabled and unrelated goals are removed from the builder. The project is selected
and a recipe is supplied solely to isolate execution; these runs do not count as autonomous
adoption. The builder can still stash/refetch materials through ordinary entity bookkeeping.
The construction clock is accelerated for 1200 ticks after setup, and the assertion is whether
the selected project exits GATHERING through normal worker commitment. Server startup costs
roughly 30–45 seconds; the measured movement loop takes a few seconds. Each trial uses a
fresh copy of the same seed, LLM disabled, separate loopback port 25697, and separate classes.
The six scored runs retain their original runtime, worlds, model and horizon. The diagnostic
runs briefly share host CPU with the scored comparison and are excluded from scored counts.

First command executed from the repository:

```sh
python3 run/redevelopment-adoption-2026-09-05/diagnostics/house-access/run_probe.py baseline
```

It reported `RESULT FAIL`, `ticks=1200`, `progress=GATHERING`. The minimal work/door loop
preserves the observed stuck entrance; removing the language model and ordinary work goals
did not remove the failure. The ranked checks were raised-threshold/overhead geometry,
door-opening timing, and the interior commitment target. Controls changed one environmental
factor at a time from a fresh seed. The repeated baseline and controls start one block farther
outside than the first baseline; the repeated baseline fails at the same final position.

| Trial | Result | Evidence |
| --- | --- | --- |
| Original approach | FAIL after 1200 ticks | Door opens; builder remains outside. |
| Repeated baseline | FAIL after 1200 ticks | Same stuck x=128.694 position. |
| Door initially open | FAIL after 1200 ticks | Opening the door alone does not resolve movement. |
| One oak stair at the entrance | PASS at 380 ticks | Builder enters and project commits to DEMOLISHING. |
| Remove the overhead obstruction | PASS at 380 ticks | Builder enters and project commits to DEMOLISHING. |

The entrance stair is at 128,5,131 facing east. The overhead control removes blocks at
x=128..129, y=8..9, z=131; it is a diagnostic control, not a proposed template change.
The two successful controls establish the threshold/clearance interaction. They stop upon
commitment and do not claim completed replacement construction.

## Scope and next correction

This turn diagnoses the user's question. No production fix or scored-world intervention was
made. A proper entrance step addresses this doorway. Redevelopment should also choose a
reachable exterior gathering/commit point so removing an obstructing building does not first
require entering it. Any such change needs a separate corrected-runtime test, leaving the
fixed six-run cohort intact. Probe sources and filtered logs are retained in this clearly
marked diagnostic evidence directory; no diagnostic logs were added to production code.
