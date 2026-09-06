# Redevelopment exterior access and planner follow-up

Status: complete. All twelve autonomous runs reached twelve village-days (144 village-days total).

The preceding study showed two voluntary selections, one cancelled house replacement because
its builder could not enter the old house, and one completed farm replacement that removed
both wells even though one-well alternatives existed. This follow-up addresses those observed
failures without restoring spare-bed, storage-capacity, surviving-food or outside-builder gates.

## Changes under test

Builders deliver the complete recipe and work from reachable positions outside the replacement,
demolition victims and changed ground. Routes are specific to the worker, checked again during
work and recomputed after reload. A failed physical approach yields to another. Gathering keeps
chest fetching distinct from project delivery and cancellation reports observed access failure.

Shared impact facts distinguish general beds from reserved worker beds and report crop capacity,
service interruptions, permanent service losses, displacement and net costs after salvage.
Equivalent placements for the same project can collapse when their effects and salvage are equal
and one needs no more material or work. Different economic tradeoffs remain visible. Affordable
placements preserving services receive an explicit preference, not an eligibility veto. The
last-fresh-water loss of the two-well farm expansion is spelled out.

## Validation before autonomous scoring

The shared workspace passed 236 tests. A separate frozen copy of production commit
`73b5b4d19b88858ab094d95a71c5fd7f40e14172` plus this task's changes passed 216 tests, including
19 new geometry, impact and comparison cases. Other ongoing mining and identity changes are
excluded from that runtime.

The frozen real-world lifecycle verifier passed zero-spare-bed displacement, full-storage overflow,
worker continuity, player protection, commitment, halfway save/reload, construction and exact
single delivery. It preserved 3,456 cobblestone and a named damaged pickaxe; this verifier drives
the construction lifecycle directly and is not autonomous adoption evidence.

A separate physical regression uses the saved builder and old house that previously stalled.
Only the project selection and initial materials are prepared; the actual GatherStep, BuildStep,
WorkLoopGoal and Minecraft navigation perform delivery, demolition and rebuilding. The doorway
is untouched. A second case reloads village state midway through demolition. These are execution
regressions, not model-choice evidence. The final frozen build completed the intact-door case in 10,789 ticks and the halfway reload case in 10,780 ticks. Final result logs are under the study's diagnostics folder.

## Fixed comparison protocol

Persistent root: `run/redevelopment-performance-2026-09-06/`. `manifest.json` records source and
configuration hashes, saved seed hashes, exact run order and the model/runtime setup.

Twelve sequential runs each cover 288,000 ticks, or twelve village-days. There are two enabled/
disabled pairs from each of these initial states:

- The prior occupied-house upgrade fixture, with no spare general beds.
- The prior food-expansion fixture with one-well and two-well alternatives.
- The saved endpoint of the preceding housing-off run: a village that autonomously grew for twelve
  days, built five projects and ended with 23 residents and positive food stocks. This is an
  established continuation of a prepared village, not a new natural-founding control.

The second repetition reverses enabled/disabled order. All use the shipped default Llama 3.2 3B
Instruct Q4_K_M model, llama.cpp b10653, 8,192 total context tokens divided into two 4,096-token
slots, unchanged model sampling and parsing, isolated game port 25696 and model port 8131.
The model-startup freeze prevents a fallback decision from consuming the initial opportunity.
No scored run forces choices, payments, demolition or worker progress. The user's game and
main world are untouched. No scored runtime or initial state changes after launch.

Daily observations record the full offer/choice/start/commit/complete funnel, ordinary construction,
housing, food, storage queues, retained work, water capability and active-project blockers. Additional
stage-boundary observations capture disruptions that could fall between daily samples. Exact model
requests and replies, victim ages, removal counts, cancellation reasons, prompt truncation and
search/grouping elapsed times remain in logs. Score repeated removal/rebuilding and useful completed
capacity rather than treating more demolitions as inherently better.

The previous study is a historical comparison; it is not a matched causal control for this code
change. Within this study the new prompt applies to both enabled and disabled arms. Two repetitions
per scenario can reveal concrete failures and repeated patterns but do not establish an optimal
teardown rate. Conditional capability comparisons hold observed supplies fixed and do not predict
future production. Daily and stage-boundary samples do not measure every transient state.

## Results

Exterior access is demonstrated through autonomous play: the default 3B model chose four
replacements, and all four committed and completed through real builder navigation. None was
cancelled or became stuck in demolition. This does not establish good redevelopment judgment.
One farm expansion removed both wells without restoring water, and one mine expansion removed
occupied housing while the model incorrectly claimed it would solve the housing shortage.

### Exact funnel

These are proposal occurrences across planning calls, not distinct geometries or villages.
Generated means a proposal passed the implemented survey and demand checks; it does not mean
its benefit was correctly assessed. Search budget limits prevent interpreting this as all
physically feasible options in the world. There were 35 enabled planning calls and 42 disabled
calls; all 77 have archived request/reply pairs.

| Enabled run | Surveys | Generated | Material viable | Retained | Offered / calls with offers | Chosen / started / committed / completed | Declined | No valid answer | Victims removed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| housing-on-1 | 1 | 1 | 1 | 1 | 1 / 1 | 1 / 1 / 1 / 1 | 0 | 0 | 2 |
| opportunity-on-1 | 2 | 1 | 1 | 1 | 1 / 1 | 0 / 0 / 0 / 0 | 1 | 0 | 0 |
| established-on-1 | 786 | 11 | 11 | 11 | 11 / 11 | 0 / 0 / 0 / 0 | 1 | 10 | 0 |
| housing-on-2 | 1 | 1 | 1 | 1 | 1 / 1 | 1 / 1 / 1 / 1 | 0 | 0 | 2 |
| opportunity-on-2 | 194 | 4 | 4 | 2 | 2 / 2 | 1 / 1 / 1 / 1 | 1 | 0 | 2 |
| established-on-2 | 672 | 5 | 5 | 5 | 5 / 5 | 1 / 1 / 1 / 1 | 3 | 1 | 1 |
| Total | 1,656 | 23 | 23 | 21 | 21 / 21 | 4 / 4 / 4 / 4 | 6 | 11 | 7 |

All disabled arms had zero redevelopment proposals and removals. No redevelopment saving choices
or cancellations occurred. Three ordinary fallback decisions occurred in disabled runs after
inconsistent replies: housing-off-1 selected a butchery fallback, housing-off-2 a mine upgrade,
and opportunity-off-2 a lumberjack. Those are not autonomous model choices. The four destructive
choices each have a valid exact reply matching the offer.

### Final outcomes at the same horizon

Completed projects include ordinary construction and redevelopment. Food is the measured edible
stock per resident, not production per day. Housing is unhoused adults. Water is the implemented
fresh-water capability. Housing seeds began without water; established seeds also began without it.

| Run | Completed projects | Population | Unhoused adults | Food / resident | Staffed food posts | Water |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| housing-on-1 | 5 | 15 | 4 | 23.20 | 1 | no |
| housing-off-1 | 5 | 11 | 4 | 34.36 | 1 | no |
| opportunity-on-1 | 0 | 4 | 0 | 0.00 | 0 | yes |
| opportunity-off-1 | 5 | 4 | 0 | 0.00 | 2 | yes |
| established-on-1 | 1 | 27 | 8 | 11.81 | 1 | no |
| established-off-1 | 1 | 27 | 8 | 10.89 | 1 | yes |
| housing-off-2 | 5 | 15 | 8 | 25.20 | 1 | no |
| housing-on-2 | 5 | 15 | 4 | 22.80 | 1 | no |
| opportunity-off-2 | 5 | 13 | 4 | 7.23 | 2 | yes |
| opportunity-on-2 | 5 | 15 | 3 | 1.13 | 2 | no |
| established-off-2 | 0 | 27 | 8 | 10.89 | 1 | no |
| established-on-2 | 3 | 23 | 6 | 12.78 | 1 | yes |

Opportunity-on-1 ended with an ordinary level-2 storehouse paused; opportunity-off-1 had another
ordinary level-1 farm still under construction. All other runs ended with no active project.
Every run ended with zero queued contents and zero project-protected displaced residents.
The latter counter ends with the project: it does **not** mean every resident has been rehoused.

### Housing replacements: execution recovered, value remains uncertain

Both enabled housing runs selected the blocked house upgrade on their first planning call.
The first started at tick 243, committed at 560, physically completed at 11020 and registered
at 12040. The second started at 189, committed at 559, physically completed at 11019 and
registered at 12040. The old doorway stayed intact until demolition; builders worked outside.

Each removed two unstaffed level-1 farms, gained one general bed and lost 52 crop plots, two
workplaces and two shared containers. One resident was displaced. Unhoused adults rose from one
to two during work, then fell to zero after the replacement registered. One staffed food post
remained throughout the recorded boundaries. Both villages subsequently built four ordinary
houses, without another demolition or rebuilding the removed farms during the remaining horizon.

Each replacement's net payment was 20 logs, 25 cobblestone and 112 dirt, with two logs and four
cobblestone in salvage. An offered ordinary level-1 house also adds one general bed and has a
base recipe of 14 logs and 19 cobblestone. Its site/preparation was not yet selected, so this is
not proof of a strictly cheaper feasible placement. However, affordable ordinary choices omitted
costs while replacements displayed net payment. The model could not make the instructed cost
comparison from those facts. These completions demonstrate usable access, not optimal investment.

### Farm replacement: water loss and incomplete alternative search

Opportunity-on-2 chose the farm expansion for additional food. It paid two logs and 16 dirt net,
with four logs and eight cobblestone in salvage, and added 26 crop plots. Both wells were removed
at commitment (tick 24020). Physical construction completed at 29100 and registered at 29540.
A one-item overflow queue cleared by completion; no residents were marked displaced. Two staffed
food posts remained at the recorded boundaries. Fresh-water capability stayed absent for the
remaining 263,980 ticks, almost eleven village-days. No replacement well or fishery was built.

Both food runs' first searches performed only two surveys before the 50 ms starting budget
expired (52.2 and 58.6 ms), producing only the two-well plan. The one-well alternatives available
in the prepared geometry were never offered at those decisions. Thus this study cannot validate
the new preference among one-well and two-well placements. The search coverage problem occurs
before model selection. Later grouping did reduce three housing placements to one in
opportunity-on-2; that house plan would remove a butchery and was declined.

The accepted farm reply called it the most cost-effective option despite the missing ordinary
cost comparisons. Later the same village built a second butchery while claiming it would provide
independent housing; its facts explicitly offered zero general beds. Final food was 1.13 items
per person versus 7.23 in its disabled repetition, with different populations and intervening
choices. This pair does not isolate the causal effect of water loss or redevelopment.

Opportunity-on-1 instead declined the farm and chose an ordinary storehouse while food was low,
reasoning about missing crafting/writing materials and food even though the storehouse added no
crop plots. One arrival and five departures left four residents; builder Tam Moran was among
those who left, and the storehouse remained paused. No demolition occurred there. Its disabled
pair also ended with four residents and zero edible stock, though it completed five projects.

### Mine replacement: a concrete housing and employment regression

Established-on-2 first built a well, then eventually selected a mine upgrade that removed an
occupied level-2 house. The exact request stated **-2 general beds**, two residents displaced,
and one additional miner workplace. The reply nevertheless said redevelopment was the only
option meeting the need for housing. It was syntactically valid and therefore executed.

The plan started at tick 164605 and committed at 168904. It physically completed at 176804 and
registered at 177640. A 24-item overflow queue was visible after demolition and cleared before
construction. Water and the staffed food post survived. Unhoused adults rose from four to six
and stayed at six through the horizon. The village had 19 employed residents before the choice
and 17 at the end, with unchanged population 23. It later built a stoneworks but no replacement
housing. Completion ended the special displacement protection, not the homelessness.

Salvage from the occupied house was 18 logs and 24 cobblestone; net payment was 45 dirt. That
made the project affordable during a stone shortage, but affordability did not make it a housing
solution. The current-need gate accepted the additional miner post because idle people existed
and no miner vacancy did. It does not establish that those idle people can actually claim the
bedless job; the prompt itself said all four were unhoused. This is a specific gap in demand
recognition as well as a model comprehension failure.

Established-on-1 never redeveloped: eleven calls offered a mine replacement, one declined and
ten returned invalid choices. The model first completed an ordinary farm upgrade while claiming
it would add housing, despite zero general beds in the facts. Both disabled established runs
also struggled to grow: one built a well and one completed nothing. Persistent stone shortages
and ordinary planning errors mean the strict response failures do not explain all stalled growth.

### Churn, losses and recoverability

No run carried out more than one redevelopment, demolished a building constructed during that
run, or repeatedly tore down and rebuilt the same service or parcel. The seven victims were four
farms, two wells and one occupied house. Recorded victim ages at commitment were approximately
6.02, 7.00 and 10.89 village-days respectively; fixture ages are authored/saved history, not
independent evidence of how those buildings originally arose.

There is no observed teardown loop, but unnecessary or harmful teardown remains credible:
the house choices lacked a fair cost comparison, the farm lost both water sources, and the mine
choice made its stated housing problem worse. More completed replacements is not a behavioral
pass. The comparison is too small to determine a desirable demolition rate.

Queued contents drained in the two autonomous cases that exposed overflow (one and 24 items).
The separate accounting verifier, rather than these sparse observations, supports exact item
conservation and save/reload claims. No new transition-capacity gate was introduced or restored.

## Runtime performance and evidence integrity

Across 35 redevelopment searches, median measured search elapsed time was 26.36 ms and maximum
355.06 ms. Grouping was measured separately: median 0.14 ms, maximum 65.85 ms. These are elapsed
in-process measurements, not isolated CPU utilization. The 355 ms search recorded zero demolition
surveys, so it cannot be attributed to proposal grouping or demolition geometry alone. Search
setup/ordinary-placement work and indivisible cold surveys can overrun the nominal 50 ms budget.
Both incomplete food searches demonstrate that elapsed-budget coverage also changes the menu.

The 77 logged construction replies had median request-to-reply latency 6.73 seconds and maximum
13.36 seconds. The largest prompt-evaluation counter was 2,408 tokens; caching means this is not
necessarily the full prompt size. The largest observed llama sequence at release was 2,493 tokens.
All observed truncation flags were zero and no context overflow was found within the 4,096-token
slot. Eleven of 21 calls containing a redevelopment offer had no valid strict selection, all in
the established enabled runs. Exact-copy errors and incorrect option numbers remained a substantial
problem even when context fit.

The broader runtime logs contain 17 HTTP timeouts and one empty-header HTTP failure across seven
runs. These are request-attempt errors, not eighteen failed villages or necessarily eighteen
failed logical requests. Every logged construction request has a reply, and the four completed
redevelopments have exact successful choices; other simulated decisions and timing could still
be affected. Three ordinary inconsistent replies triggered the fallback decisions noted above.
The full warnings are retained in `final-outcome-analysis.json`; do not describe this study as
having no model/runtime errors.

All twelve original attempts have thirteen daily samples at ticks 0 through 288000, a COMPLETE
marker, and an all-dimensions-saved marker. Persisted redevelopment counters match the event logs.
No scored attempt failed, was excluded, restarted or had its horizon extended. Source/provider,
config, runner and seed `level.dat` hashes still match the launch manifest. The runner ended in
`needs_attention` only after the final successful saved run because its post-run model-port check
did not clear within twenty seconds. At finalization both isolated ports were free and all owned
JVMs/runner had exited. No process was killed or run repeated; the original error and state are
archived, and the monitoring state was marked complete after verification.

Final evidence is under [the evidence directory](redevelopment-performance-2026-09-06/):
`completed-run-summary.json`, `final-status.json`, per-run observations/transitions/events and exact
model traces, `final-outcome-analysis.json`, `manifest.json`, `state.json`, the frozen source patch,
input verification, harness sources and `checksums.json`. Files explicitly named `interim` or
`first-completion` preserve earlier checkpoints and are not final endpoints. Full saved worlds and
unabridged logs remain under the persistent run root. The checksums cover archived evidence, not
the mutable external workspace or a full byte-for-byte model/world snapshot.

## Follow-up priorities

1. Make proposed benefits match actual usable capacity, including whether idle residents can
   take the added job under existing housing rules. Show housing loss prominently for a mine.
2. Ensure the bounded search fairly covers lower-removal placements before finalizing its menu;
   repeat the well case only after confirming both meaningful alternatives reach the model.
3. Give ordinary and replacement choices comparable costs and brief benefit/loss facts. Use
   stable choice identifiers with unambiguous validation instead of copying long changing labels.
4. Rerun fresh matched cases after those changes, retaining the current results as a baseline.
   Preserve the exterior regression and item-conservation checks. Do not tune a demolition
   cooldown based on these four choices: no repeated-teardown failure was observed.

The exterior fix and lifecycle checks pass this study. Small-model judgment and complete
alternative coverage do not yet pass. No higher village-center template was available in the
frozen baseline, so this study does not demonstrate a higher-level center upgrade specifically.
