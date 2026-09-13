# Village auditing: supervising autonomous variants

The developer audit harness keeps a chosen set of villages running without a player nearby,
suspends other village simulations, accelerates the real server when asked, and reports enough
state to distinguish ordinary waiting from a stalled settlement. It is available only when
`Developer commands` is enabled in `kithkyn-advanced.toml`.

## The operating loop

1. Travel to a suitable biome and found the village normally with
   `/kithkyn create-village <pos>`. Give the command an explicit style only when testing a style
   independently of biome selection.
2. Stand in or near the new village and run `/kkdev village audit monitor`. A position may be
   supplied when operating remotely.
3. Repeat for each variant under test. `/kkdev village audit` shows all eight style slots, which
   monitored village fills each slot, whether its founding content is loaded, and how many
   unmonitored villages are suspended.
4. Let the villages run normally, or run
   `/kkdev village timelapse monitored <builds-each> [max-days]`. The time-lapse advances ordinary
   logical ticks and stops when every monitored village reaches its build target or the time limit.
5. Use `/kkdev village audit report [pos]` for the planner and resident briefings, current work,
   missing materials, open posts, housing, attractiveness, and build choices. Use
   `/kkdev village audit villagers [pos]` for each resident's occupation, life stage, personality,
   health, position, recent work activity, and standing operational blocker.
6. Run `/kkdev village audit unmonitor [pos]` to remove one target, or
   `/kkdev village audit clear` to end isolation and return to the configured village-loading mode.

The monitored set is saved with the world. It resumes after a server restart as long as developer
commands remain enabled. A time-lapse session is deliberately runtime-only: a stopped server has
no tick sprint to resume.

## Isolation and loading

A nonempty monitored set is an allowlist. Only monitored villages receive their once-per-second
`Village.update`, unattended trade, population, relationship, labor, planning, and LLM passes.
Natural village generation is paused for the same period. Unmonitored villages release any
Kithkyn chunk tickets and retain their saved state unchanged.

Each monitored village ignores the normal `OFF`, `HYBRID`, or `ALL` loading decision and holds the
same full entity-ticking chunk set described in [village-loading.md](village-loading.md): the built
footprint and perimeter, plus bubbles around residents and auxiliaries. Its villagers therefore
walk, work, defend themselves, and build while no player is present.

Minecraft still ticks anything loaded for another reason. If a player stands beside an
unmonitored village, the entities in those player-loaded chunks remain ordinary live entities,
although that village's Kithkyn bookkeeping and brain are suspended. Delete unwanted test
villages with the existing cleanup tooling when complete physical isolation is required.

Tick sprinting advances the whole server clock. Crops, weather, hostile mobs, and every other
loaded system advance too. Audit isolation limits village work and chunk tickets; it does not
create a private clock for each village.

## Findings

The watcher samples monitored villages once per game minute. Its progress mark includes completed
builds, population and employment, the active project stage and structure cursor, remaining ground
and wall work, and the current construction-material shortfall.

It broadcasts a `NEEDS ATTENTION` finding when:

- a villager has continuously reported the same operational blocker for at least one game day;
- a persisted resident remains unloaded for several game minutes after monitoring begins;
- an active build, wall, or saved construction goal makes no observable progress for two game
  days; or
- a village is out of its post-build cooldown and produces no project or saving goal for one game
  day.

A pending LLM verdict is reported as `waiting`, not stalled. The monitored time-lapse stops its
tick sprint while such a wall-clock answer is pending and resumes when the answer lands. A recovery
message is broadcast when a village previously needing attention moves again.

These findings are leads, not automatic repairs. The harness never gives a village resources,
teleports a worker, changes a job, or chooses a project. The report names the missing material or
resident blocker so a developer can inspect the autonomous failure before deciding whether the
simulation or the test world needs intervention.
