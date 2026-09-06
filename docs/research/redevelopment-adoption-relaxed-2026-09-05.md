# Autonomous redevelopment after removing transition gates, 2026-09-05

Six twelve-day runs completed: 72 village-days, including 36 with redevelopment enabled.
The default Llama voluntarily selected two replacements. A farm expansion completed after
removing two wells; a house expansion failed to reach commitment because the builder could
not enter the old house. No further demolition occurred. The successful farm choice still
removed more than necessary and was followed by rebuilding a well. Normal founding never
progressed far enough to test meaningful adoption.

The study finished September 5 at 20:29 EDT. The [verified summary](redevelopment-adoption-relaxed-2026-09-05/summary.json)
and [manifest](redevelopment-adoption-relaxed-2026-09-05/manifest.json) retain exact counts,
run horizons, baseline and harness hashes. This measures behavior after removing transition
capacity gates; it is not a replicated before/after causal estimate.

## Results

All six runs have thirteen observations at ticks 0, 24000, ..., 288000. Lifecycle log counts
match each final persisted strategy counter. Across the enabled conditions, 18 proposals were
offered in eight planning calls, 16 were declined, two were chosen and started, one cancelled,
and one committed and completed. Two buildings were removed. There were no redevelopment
saving choices or no-answer outcomes. One call may offer multiple placements, and later calls
can repeat the same trade; these are not eighteen independent decisions.

| Enabled condition | Examined placements | Offers (calls) | Declined | Chosen / started | Committed / completed | Cancelled | Removed buildings |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Occupied housing | 22 | 14 (6) | 13 | 1 / 1 | 0 / 0 | 1 | 0 |
| Prepared food opportunity | 107 | 4 (2) | 3 | 1 / 1 | 1 / 1 | 0 | 2 |
| Normal founding | 17 | 0 (0) | 0 | 0 / 0 | 0 / 0 | 0 | 0 |

Disabled copies generate no redevelopment proposals. The housing condition had zero spare
general beds at all twelve post-initial daily samples and nevertheless received offers and
selected a replacement. The old spare-bed gate therefore no longer prevents this prepared
housing trade. Its execution failure is a separate access problem.

### The completed farm expansion and avoidable well replacement

Llama first chose an ordinary upgrade of the surviving larger farm to level 3. In a later
planning call, it chose `redevelop at 155,128 for level 2 farm`, explaining that food was
needed and the upgrade was efficient. At 19:50:41 EDT, normal workers started and committed
plan `a8ab59ec-e687-4d50-bbf1-91bb06d0b924`; it completed at 19:50:43 under accelerated
simulation. No diagnostic code selected, paid for, or advanced this scored project.

The completed project removed two blocking level-1 wells, added 26 crop plots and two shared
containers, recovered four logs and eight cobblestone, and paid two logs plus 16 dirt after
salvage. No occupants were displaced by well removal. The existing farmer could work the
upgraded field; three food jobs remained staffed in later daily samples.

Two alternatives in the same call each removed just one well while adding the same 26 crop
plots and two containers. Each alternative cost four logs and three cobblestone after salvage
and required 52 blocks of preparation, compared with 120 preparation blocks and 16 dirt for
the chosen plan. The selected plan was cheaper in logs/cobblestone but removed both wells.
The village then chose a new well, citing lack of clean water, and it was standing by day 6.
There was no additional demolition through day 12.

This is an observed removal/rebuilding loop with an offered way to preserve the service.
The prompt identified the wells being removed and the target's GRAIN capability, but did not
explicitly say the last fresh-water capability would disappear. This is an actionable
presentation/choice-quality concern for a small model. The evidence establishes neither a
general demolition rate nor economic optimality of the chosen placement.

### The selected housing replacement and access failure

The first housing response incorrectly claimed an ordinary farm upgrade would provide
independent housing, despite facts showing only two extra containers and separate-house
options being available. The village completed that farm upgrade. At 18:37:18 EDT, Llama
then correctly selected the replacement-house action and cited a shortage of five beds.
That project would add one bed while removing both blocking farms, including the recently
upgraded one. It required 17 logs, 22 cobblestone and 166 dirt after salvage.

The project stayed in GATHERING and cancelled at 18:40:27 with "its recipe never came
together." There were 18 explicit reports that the builder could not reach the gathering
destination at 131,6,131. Nothing was demolished and no redevelopment contents were queued.
Later choices repeatedly built stoneworks while citing housing or construction needs; the
village still ended with five unhoused adults.

A [separate controlled access reproduction](redevelopment-adoption-relaxed-2026-09-05/house-access-diagnosis.md)
confirmed the physical cause. The original builder opens the door but cannot jump up the
one-block raised threshold under its low lintel. Original and repeated baseline trials failed
after 1200 ticks; opening the door beforehand also failed. Adding one entrance stair or
removing the overhead obstruction let the same builder enter and commit at 380 ticks.
Those diagnostic trials are not autonomous adoption and stop at commitment, not completion.

The deeper redevelopment issue is that GatherStep sends the full recipe to the old building's
interior center before demolition may begin. A reachable exterior commitment point should
avoid requiring entry into the structure being removed. No production correction or scored
world intervention was made during this study.

### Normal founding was not an adoption test with opportunities

Both normal-founding copies completed zero additional buildings and ended with four residents
in the initial center, mine and storehouse. They had no staffed food producer, no shared
edible stock, and never accumulated construction stock in the daily samples. The disabled
copy briefly stored five snowballs. Both models repeatedly chose to save for a lumberjack
because logs were missing, but construction never began.

The enabled search examined 17 placements and recorded 17 first refusals for a protected
center or mine, producing no offer. This condition cannot show model reluctance to redevelop:
it stagnated before suitable opportunities arose. Longer healthy-growth or live-world
observations remain needed to establish adoption after ordinary settlement development.

### Housing, food and construction outcomes

All-build counts include the one completed redevelopment. Unhoused figures are final / highest
daily sample; food is the final count of shared edible items per resident, not consumption or
production throughput. Enabled and disabled copies share their saved starting world, but
asynchronous inference and random simulation are not identical trajectories.

| Condition | All builds | Population end | Unhoused end / sampled peak | Food per resident | Staffed food posts | Final project |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Housing enabled | 4 | 11 | 5 / 5 | 36.45 | 1 | Stoneworks, working |
| Housing disabled | 5 | 23 | 4 / 4 | 12.78 | 1 | None |
| Food enabled | 5 | 26 | 15 / 17 | 2.58 | 3 | None |
| Food disabled | 5 | 18 | 9 / 9 | 0.00 | 2 | None |
| Founding enabled | 0 | 4 | 0 / 1 | 0.00 | 0 | None |
| Founding disabled | 0 | 4 | 0 / 1 | 0.00 | 0 | None |

All daily samples had zero queued redevelopment items and zero protected displaced residents.
The successful removal affected wells, while the occupied-house plan never committed, so
these runs do not test actual temporary homelessness or storage-overflow recovery. Those
mechanics remain covered by the earlier isolated lifecycle verification. Daily sampling also
cannot prove item-component conservation or exclude brief transitions between observations.
The enabled food trajectory's housing shortage accompanied population growth; no beds were
removed by its completed project. One pair per condition cannot assign causal performance
effects to redevelopment.

## Fixed protocol

Use production commit `73b5b4d19b88858ab094d95a71c5fd7f40e14172`, which permits temporary
homelessness, queues storage overflow, and permits work without a surviving staffed food
producer or a builder assigned outside the affected buildings. Concurrent mining changes are
excluded. The default local Llama 3.2 3B Instruct Q4_K_M model, llama.cpp b10653, production
prompts/parser/sampling, 8192 total context and two slots are unchanged. The isolated runtime
only overrides the model port to 8131; the test server binds loopback port 25696.

Run three paired scenarios for exactly 288,000 scored game ticks (12 village-days) each.
Each enabled/disabled pair copies the same saved seed world. Run one server at a time in
this order: housing enabled, housing disabled, prepared food opportunity enabled, prepared
food opportunity disabled, normal growth enabled, normal growth disabled. Total
exposure is 72 village-days, half with redevelopment enabled. These are three distinct
conditions with one pair each, not replicated estimates of an overall adoption rate.

- **Occupied housing:** seven residents, a housing shortage of one bed, zero spare general
  beds, and an occupied level-1 house whose level-2 expansion requires removing two farms.
  A third staffed farm survives. The initial surveyed proposal adds one bed after completion,
  removes two workplaces and 52 crop plots, displaces one resident during work, recovers two
  logs and four cobblestone, and needs 20 logs, 25 cobblestone and 112 dirt after salvage.
  Existing stock covers that cost. This case directly exercises the former spare-bed barrier.
- **Prepared food opportunity:** the previous established camp with a small farm expansion
  blocked by redundant wells, low edible stocks, a surviving larger farm, staffed jobs,
  homes and a construction budget. Its proposal must pass production preflight before scoring.
- **Normal growth:** ordinary founding on generated terrain, with no fixture residents,
  extra buildings or construction supplies. This tests whether an opportunity arises during
  growth without arranging a blocked upgrade in advance.

Both prepared camps start at game tick 144,000 with a completed stone wall. Prepared terrain
has 64 stone layers above bedrock, then three dirt layers and grass. Normal growth uses world
seed 2468013579, with generated structures disabled. Chunks x/z 2 through 18 stay loaded in
all runs. Hostile spawning, weather cycling and ambient villager conversations are disabled
equally. Other planning, hiring, gathering, project cooldowns and worker construction are
ordinary behavior. Model startup pauses entity simulation and village bookkeeping; scoring
starts only when the local model is ready. Simulation sprints between pending model decisions.

The prepared layouts are controlled opportunities, not evidence that the same layout forms
naturally. All ordinary alternatives and waiting remain available. The model may reasonably
prefer a cheaper separate house to destroying productive land. A preflight proposal is not
counted as a live offer or selection. Saved-world pairing does not synchronize asynchronous
model responses or random simulation outcomes, so differences require qualitative review.

## Monitoring and interpretation

Read persistent state and the read-only snapshot script under
`run/redevelopment-adoption-2026-09-05/`. The runner serializes seed creation and all six scored
runs, retains complete server logs and worlds, and refuses to silently resume an incomplete
attempt. `manifest.json` records baseline, harness and isolated-provider hashes, startup
parameters and saved seed hashes. Setup and failed attempts remain separate from scored data.

Count offers, planning calls containing offers, immediate choices, saving goals, starts,
commitments, completed projects and removed buildings separately. Several placements in one
call and repeated offers are not independent decisions. Record declines, invalid/no-answer
responses, cancellations, goal expiry and ordinary alternatives with the original model trace.

Daily snapshots include population, unhoused adults, spare general beds, staffed food posts,
food stock per resident, all completed builds, standing building IDs and positions, current
project/blocker, queued item counts and residents whose work is protected during displacement.
Daily samples can miss short-lived changes; exact lifecycle events establish adoption and
removal. The protected-resident counter includes dependently housed working children and is
not a direct count of homeless people. Queue totals show pending storage, not proof of
component-level inventory conservation; that remains covered by the separate lifecycle test.

A useful adoption observation requires a model-selected project followed through normal worker
completion. A choice or saving goal alone is reported as such. Keep running to the fixed horizon
after any success. Review repeated teardown at the same parcel/category, ages of removed
buildings, resident recovery and food/storage consequences against its paired copy. Do not call
zero offers model reluctance, or zero completions proof that the village rejected every option.
One pair per scenario can reveal concrete failures but cannot establish an optimal demolition
rate or a causal change in overall village performance.


## Execution record and evidence

Production commit `73b5b4d19b88858ab094d95a71c5fd7f40e14172` stayed frozen throughout. The
benchmark harness adds the occupied-house seed, chunk loading and daily transition metrics;
only the isolated provider's model port differs. All seeds passed setup, the development
runtime compiled, and every scored run finished its exact horizon and saved all dimensions.
The runner terminated lingering JVM threads only after those completion/save markers, so
exit code 143 in the run-state record is expected and does not mean an interrupted run.
There were no excluded scored attempts or changed scored conditions. Five short access
reproductions briefly shared CPU with the housing-disabled run; their separate worlds,
classes and counts are excluded. Wall-clock speed and inference latency are descriptive only.

Each condition has `NAME.json`, `NAME-events.txt` and `NAME-model-trace.txt` in
[the evidence directory](redevelopment-adoption-relaxed-2026-09-05/). Initial seed briefings,
preflight descriptions, full run-state metadata, aggregate summary, tested harness source
and the original runner source are retained there. First-completion files are explicitly
interim captures; the unsuffixed condition files contain final twelve-day results. The runner
source is provenance for the original `run/redevelopment-adoption-2026-09-05` directory
layout, not a standalone script to execute from the archive folder. Saved worlds and full
server logs remain in that original ignored runtime directory. `checksums.json` covers the
archived artifacts.

The monitor is complete. Follow-up implementation priorities are a reachable exterior
redevelopment start point and clearer lost-service/net-benefit comparisons, including the
cost of rebuilding a service that another offered placement preserves. These address observed
failures without restoring blanket spare-bed, storage, food-staffing or builder-location gates.
