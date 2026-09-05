# Redevelopment safeguard practicality, 2026-09-05

The spare-bed requirement is an observed bottleneck. Several transition rules also prevent
useful classes of redevelopment until the village already has redundant infrastructure.
This records the audit before the subsequent rule change. The user then directed removal of
the four conditions; the resulting behavior is documented in [redevelopment](../redevelopment.md).
The autonomous measurements below remain results from the earlier conservative implementation.

## Evidence and scope

This uses the completed [eight-run autonomous experiment](redevelopment-natural-adoption-2026-09-04.md),
current production code and building definitions, plus read-only inspection of Mosswood's
saved village and chunk inventories during this audit. The running server was not paused or
modified. Its saved files changed between reads, so the inventory and assignment readings
are observations during the audit, not an atomic capture of the live village.
The [derived measurements](redevelopment-practicality-2026-09-05.json) retain per-run counts
and the aggregate inventory checks. No model behavior is inferred from those offline checks.

## Beds: a confirmed bottleneck

- There were zero free general beds in 68 of 96 post-start daily samples across all eight runs.
- The two prepared, redevelopment-enabled runs had zero in 22 of 24 such samples.
- Their placement surveys recorded 67 first refusals for insufficient general beds.
- Mosswood's saved assignments had no free beds. Each of its two level-3 houses housed four
  bed holders; its lumberjack lodge housed one; the center housed four. Neither farm housed anyone.

These are daily observations and repeated placement surveys, not 67 distinct useful upgrades.
One spare bed would still be insufficient for a source building with two or four occupants.

`Village.canRehouseForRedevelopment` counts bed holders in every affected building, including
the upgrade source, and requires that many free general beds outside the entire affected set.
Reserved workplace beds do not count. `reconcileBeds` routinely gives available general beds
to residents needing homes. Meanwhile, `RedevelopmentDemand` normally justifies more housing
only when unhoused adults and pending arrivals exceed the free general beds. Those conditions
work against each other: the village most in need of a larger occupied house often cannot
qualify to rebuild it. Pending arrivals can create exceptions; this is not a formal impossibility
for every housing proposal.

There is no project-specific reserve of beds or temporary accommodation. Simply allowing
homelessness would have another consequence: `JobClaiming.releaseUnhousedWorkers` releases
workers the village cannot house. Any temporary-housing solution must remain valid for work
eligibility as well as sleep and household assignment.

This gate does not block bedless, unoccupied victims by itself. The offered farm/well plans
already passed it, so it does not explain why the model declined those offers.

## Storage: plausible for small removals, restrictive for major storage losses

The implementation scans all affected container contents, including personal containers and
the upgrade source, and requires them to fit into registered shared containers elsewhere.
It simulates merging and slot rules. The need to preserve items is valid; requiring all storage
to exist elsewhere before the project is offered makes some expansions difficult.

In a later inventory read during this audit, Mosswood's small farm held 27 stacks with 92
empty shared chest/barrel slots outside it. Its storehouse held 81 stacks with 92 such slots
outside it. Either building's contents alone could fit without merging at that reading.

The large farm held 97 stacks. Outside it were only 27 empty shared slots. Even an optimistic
calculation allowing every matching stack to fill to 64 required at least 75 new slots, so its
contents could not fit elsewhere. Actual item limits cannot improve that optimistic result.
This is a conditional inventory check for affecting that building, not evidence of a surveyed,
otherwise-beneficial upgrade: it is already a level-3 farm. A multi-building plan can lose still
more destination capacity. Inventories can change before commitment.

Expanding a full main storehouse can face the same problem: storage demand recognizes actual
overflow, while the transition requires room for the old storehouse's contents elsewhere.
Ordinary upgrades also evacuate source storage, so this restriction is broader than redevelopment.
There is no temporary project inventory or planned staging-container phase.

## Builders: practical for ordinary sites, incompatible with a future center upgrade

Mosswood had three assigned builders, all in its center. A project affecting ordinary buildings
retains them. All five biome-specific center definitions contain builder posts; no other current
building definition does. Because the upgrade source is included in the affected set, a center
redevelopment upgrade would fail the surviving-builder test under the current catalog and rule.
Only level-1 centers currently exist, so this is a future implementation obstacle, not the
explanation for a refused higher-center proposal in the experiment.

Follow-up implementation inspection found that construction work steps target the project
site directly, without consulting the rebuilding workplace's location. Retaining employment
through housing displacement therefore addresses work dispatch without a new temporary
workplace. A physical higher-center construction test still awaits those templates.

## Food: passes in Mosswood for one farm, rejects a single-producer transition

Mosswood had one assigned farmer in the small farm and two in the large farm. Affecting one
farm while retaining the other passes this staffing condition; affecting both fails it.
A village with only one staffed food building cannot redevelop that building even if it has
substantial food reserves. The rule includes the source building being upgraded.

The condition ignores stored food, construction downtime and actual output. Conversely, any
assigned job in a surviving food-granting building satisfies it, which does not establish enough
production. A measured food reserve covering conservative downtime could support more useful
transitions, with an actual second-producer preparation step when reserves are insufficient.
The game currently does not provide a reliable production or completion-time forecast, so this
must be implemented and validated rather than replaced with an arbitrary optimistic constant.

## What the refusal counts establish

Across four enabled runs there were 1,367 surveyed placements. Logged first refusals were:

| First refusal | Placements |
| --- | ---: |
| Protected center or mine | 1,098 |
| No building needs removal | 160 |
| Insufficient general beds | 67 |
| No current need | 22 |

There were no logged first refusals for surviving builders, surviving food production or
contents capacity in this cohort. Checks return at the first failure, so later failures can
be hidden. The counts do not prove that removing the bed rule would make those 67 placements
valid. Nor do they establish that these four safeguards caused all non-adoption: 12 proposals
passed the gates and reached the model, yet none reached construction.

## Recommended next implementation and validation

1. Make a redevelopment choice include the preparation needed to execute it: temporary
   accommodation and inventory staging, with their real cost and space requirements.
2. Reserve that capacity for the project so arrivals and ordinary stock deliveries cannot
   consume it before demolition. Preserve household and worker eligibility during the move.
3. Support a project crew while the center is rebuilt before authoring higher centers.
4. Replace the unconditional second-food-building requirement only once food reserves and
   conservative construction downtime can be checked, or plan another producer first.
5. Keep the small model's choice simple: one priced plan with its preparation, interruption
   and resulting capacity calculated by the game. Do not ask it to invent a relocation sequence.

Test zero-spare-bed housing, crowded shared storage, a sole food producer, and center crew
continuity as targeted transitions first, including save/reload and changed capacity before
commitment. Then freeze a new baseline and repeat autonomous runs, reporting every stage from
opportunity to completion plus homelessness, lost contents, food interruptions and repeated
teardowns. Record all relevant failed transition conditions during diagnostic surveys, so one
gate does not hide the others. Keep natural adoption separate from forced execution tests.
