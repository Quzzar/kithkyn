# Building spec: every building, variant, level, recipe, and unlock

**Regional integrations, revised 2026-09-10:** [birch-village.md](birch-village.md) is the current
approved catalog for `birch_forest`. Its 23 templates and exact amenities supersede this
document's older generic tier counts, founding contents and candidate choices for that family.
In particular it has no tier-3 house/farm or higher center/storehouse/mine/church. The bakery
and tavern are separate buildings, each with one worker bed and private storage. The approved
private catalogs for Desert, Badlands, Floodplain and Jungle are documented separately; Jungle's
22-template catalog and four-home start are specified in [jungle-village.md](jungle-village.md).

**The catalogue below enumerates 36 categories; 22 of them survived the cut.** The totals in
this document count the full map of the possible, not the shipping set — see
[The cut](#the-cut) for which categories stand and why the rest went.

**Proposed, not yet decided.** Reality check before reading: the jar bundles one family, the
23-template Birch Forest catalog of [birch-village.md](birch-village.md), and that is the whole
shipped set. It covers every founding and phase 1 category at level 1 (`house` and `farm` also
at level 2, `watchtower` at level 2, `market` at levels 1 to 3), with real MASON, HUNTER,
FISHER, BAKER, BUTCHER and INNKEEPER occupations behind the workplaces. The old Village Life
families (plains, taiga, snowy, savanna and the bundled desert set), which carried the
level-3 houses, level-2 mines and level-3 farms this document once counted, were removed on
2026-09-10. Desert, Badlands, Floodplain and Jungle ship locally as private datapacks with their
own enumerations ([desert-village.md](desert-village.md), [badlands-village.md](badlands-village.md),
[floodplain-village.md](floodplain-village.md), [jungle-village.md](jungle-village.md)).

**One occupation exists only as a name in these tables**: HERDER is not in the
Occupation enum, and a definition naming one fails the codec
outright, so such a building cannot load at all. MASON and TANNER were added when
their buildings shipped. Anything below that names a worker should be checked
against the enum before it is authored.

**A mine cannot ship its shaft network.** The template provides the headframe and entrance;
the miner excavates the underground ramp and branches at runtime. `sink` specifies how the
lowest authored layer seats against the terrain: zero replaces the surface block, while -1
places that layer above it. `mine_entrance` specifies a cardinal descent direction, a mouth
offset from each MINER station, and a width of two, three or five blocks (default five). The
building's rotation applies to both direction and offset. A two-wide corridor occupies local
x -1 and 0 so its mouth remains a real walk cell instead of a half-block center. Width changes the side walls and
branch mouths; it does not change the one-down-per-forward-block slope, headroom, or entrance
step. Children inherit their root's width. The top entrance remains open rather than being
lined as though its outdoor air were a cave. Existing shafts retain their saved entrance
geometry when a definition changes. The 7x7 footprint in the table is the headframe, not the mine.
`mine_*_2` (2026-09-02) is that headframe grown toward local +X into a 13x7 pavilion over
TWO mouths, stations at [3,0,3] and [9,0,3]. An upgrade keeps the origin corner, and the
root shaft ramps toward local +Z from its mouth (`MineStep`), so a second mouth behind the first
would have run one ramp a block over the other; six blocks to the side leaves one block of
rock between two five-wide roots. After a root reaches bedrock and finishes its ordinary ribs,
eligible full ribs may seed one bounded generation of outward child shafts. Their identities are
saved on the `Building`; their dug progress remains world geometry. It is not authored by hand:
`tools/structure/mine-level-2.py`
derives all five families from their level-1 files, so re-run it after touching any of them
([structure-authoring.md](structure-authoring.md)).

**No house or farm has a `cost` yet**, so nothing gates building one. The recipes, and the
sprawl-versus-upgrade pricing the section below argues about, are still unset.

**Crop variety is not a capability, and must not become one.** A farmer plants whatever
seeds the village holds (`TillSoilGoal.PLANTABLES` covers wheat, beetroot, pumpkin, melon,
carrot, potato and sweet berries), so a player handing a level-1 farm carrots gets carrots.
What a farm level changes is physical: field size, and which seeds its barrels arrive
stocked with. The tables below read as though a level unlocks vegetables; it does not.
One consequence worth keeping: `TillSoilGoal` treats bare farmland as somewhere to plant,
so melon and pumpkin need a fruiting lane the farmer will leave alone. Podzol is that lane
— it is valid ground for fruit and is the one dirt-family block absent from `TILLABLES`.

`upgrades_from` is live: a village offers an upgrade of something it owns alongside new
buildings, rebuilds it on site, and keeps the building's identity so its workers keep their
jobs — see [How upgrading works](#how-upgrading-works). Several
shipped files contradict the rules below (a level-1 church granting ENCHANTING alone, a
watchtower with two guard stations, a storehouse with eleven containers). The footprint
size classes are invented numbers and measured 1.4x to 20.5x off against real candidates.

The complete enumeration behind [buildings.md](buildings.md),
which holds the reasoning. This file is the reference: what exists, what it costs, and what the
village can do once it stands. It is the input to sourcing structure files, so the manifest at
the bottom is the working checklist.

Every count in the manifest is derived from the tables in this file, so the two cannot drift
apart: if you add a variant above, the totals below are what change.

## How a village starts

Every village starts with its center, mine and storehouse, placed free. A center can also
provide an ordered `starting_buildings` list of exact building ids, including its mine,
storehouse and any starting homes. Repeat a home id to request several copies. Omission
uses that style's mine and storehouse. Every named definition must be loaded and belong
to the village's regional catalog; a missing home refuses the complete founding set.

Starting beds and jobs come from those authored buildings. A bedless Jungle center uses
four separate one-person huts for its four founding workers. Centers with accommodation
can keep it inside the center. Normal bed registration, job assignment and campfire arrivals
apply; founding does not spawn a separate crew or simulate paid construction projects.

A founding job may belong to the center while its physical workplace sits in another starting
building. The center's `work_stations` entry declares `worksite_category`; the matching building
declares a same-occupation position in `worksites`. A `worksites` entry is a destination only and
never creates a second vacancy. Job location, work area and workplace storage resolve through that
physical building. If no matching completed building exists, the worker keeps the center-owned job
and waits. A routed miner station does not create a shaft below the center; the physical mine
worksite owns the shaft geometry.

### Founding uses ordinary construction placement

The requested location anchors the center's authored meeting point. After the center fits,
each additional starting building runs the same location search as later construction.
The survey reserves its footprint and offers it as an anchor for the next building, so
homes and services can form streets and courtyards with natural sprawl. Buildings prefer
two-block gaps and inward fronts, with one-block gaps and other rotations when needed.
They can align with any previously planned building rather than fixed sides of the center.

Each building uses its own local ground elevation and applies its authored sink once. A template
with negative `sink` also reads the ground immediately outside its rotated public front. When
that approach is higher than the footprint's modal plane, the building follows it so an exposed
doorstep does not sit in a cut trench.
The same terrain-preparation queues as normal construction clear and fill only its footprint.
Founding applies this work immediately, without charging the recipe. Ground between buildings
stays natural. Normal completion hooks register amenities and clear natural overhead vegetation.

The site qualifies only when the center and **every** starting building fit. Planning writes
no terrain, buildings or claims. Immediately before committing, all footprints and preparation
queues are checked again, including ownership and newly placed containers. Failure leaves the
whole site unchanged. Manual founding may load the bounded search area; natural founding
and delayed commits only inspect already loaded terrain. Existing villages are not relocated.

## What is actually needed

36 categories is a map of the possible, not a plan. The practical core is much smaller, and it
is the only part that has to exist for the game to be a game:

> `village_center`, `house`, `storehouse`, `mine`, `lumberjack`, `stoneworks`, `farm`,
> `hunting_lodge`, `fishery`, `well`, `watchtower`, `blacksmith`, `market`

Thirteen categories, and a village that founds, feeds, houses, gathers, arms, defends, and trades.
Everything after that in this file is **sketched, not committed**: it is here so the shape of the
system is visible and so nothing gets designed into a corner, not because it is scheduled. Treat
phases 3 and 4 in particular as a record of what would fit, to be cut freely.

## Id scheme

`<category>_<variant>_<level>[__<design>]`, all lowercase, level always explicit:

```
house_desert_1        cottage, desert variant
house_desert_2        upgraded on site to a house
market_birch_forest_3   trade hall
mill_watermill_1      variant is a name, not a family, where the shape differs
house_badlands_2__large_house_2   a second layout in the same regional tier
```

The structure file at `data/kithkyn/structure/<id>.nbt` shares the id exactly, so a definition
and its structure are never out of step.

The optional `__<design>` suffix distinguishes layouts without inventing categories or upgrade
levels. A design is a lowercase word or words joined by single underscores. The unsuffixed id
remains the canonical choice for callers asking for one building. The construction planner sees
every regional alternative in the village's own family and nothing from another family. A saved
goal keeps its exact design id.

**This supersedes the `house_wood_s` sketch in [village-tiers.md](village-tiers.md).** That form
encoded material and size; this one encodes variant and level, which is what the two axes actually
are.

Levels describe the completed building, not the route used to reach it. A village may either
upgrade a compatible lower-level building on site or construct the higher level on a separate
site. The village prefers reuse when it fits because that route is cheaper.

An independent higher-level design with no compatible predecessor declares `"standalone": true`
and omits `upgrades_from`. It is built on a fresh site and pays its own complete recipe. The flag
cannot accompany `upgrades_from`; existing higher-level definitions still require an explicit
predecessor unless they opt into standalone construction. A level indicates the finished building,
so several standalone layouts can occupy the same tier despite different footprints.

Housing goals inspect the actual rooms across those alternatives. A single resident's home must
provide a general bed; a married couple's goal can use a declared pair in an ordinary or mixed
house, including at a higher tier. A dedicated couple cottage remains preferred where available.
Homes containing only couple rooms are offered through marriage goals, while mixed homes also
remain ordinary construction choices.

## Cost, and space

These are two separate questions and the spec keeps them separate.

**Cost is a shared recipe.** One datapack file per category and level under
`data/kithkyn/kithkyn/construction_recipes/<category>_<level>.json` contains a `cost` array
of item IDs and positive counts. For example, `watchtower_1.json` contains
`{"cost":[{"item":"minecraft:oak_log","count":24},{"item":"minecraft:cobblestone","count":40}]}`.
A building definition normally omits `cost`; its category and level select the default recipe
for all named designs and regional styles. Override this shared recipe once in a datapack to
change every building that uses the default. Geometry, beds, workstations and identity remain regional.
A deliberate per-building exception may include its own `cost` array in the building JSON. This
replaces the complete recipe; it does not add to or merge with the default. It uses exactly the same
validation and construction payment path. An explicit override can price a category with no default.
All shipped definitions currently use the shared defaults.
The loader resolves buildings and recipes together before publishing a reload. A missing default
rejects a building that has no override. An invalid explicit override rejects the building instead
of falling back to the default or becoming free. Recipe lists must be nonempty and cannot contain
air, unknown items, repeated items, or nonpositive counts.
There are no points or derived currency.

**Each recipe describes its completed structure.** Reusing the exact building named by
`upgrades_from` pays only the positive material increase from predecessor to target. Constructing
that level on a new site follows the chain and combines the level-1 recipe with each calculated
upgrade cost: fresh level 2 pays level 1 plus its upgrade, and fresh level 3 pays level 1 plus both
upgrades. A decrease between recipes never becomes a rebate; shipped blacksmith and watchtower
definitions contain such decreases, so fresh cost must be derived from the chain rather than copied
blindly from the target.

Standalone definitions have no predecessor chain, so their fresh cost is exactly their own recipe.

**Every building is priced.** The tables below are historical design sketches; the 29 shared
recipe files provide the shipped defaults. The 2026-09-10 consolidation preserved all quantities and changed
the church, bakery and tavern's stone-brick demands to generic cobblestone. Their decorative
masonry no longer requires a stoneworks to build, and changing a regional template never changes
its price. Stoneworks production and ordinary crafting recipes are unaffected. Three rules keep
construction reachable:

- **Only name what a worker puts into storage.** The miner (stone pickaxe) yields
  cobblestone, sand, sandstone and iron; the lumberjack yields logs and oak
  planks; the mason turns cobblestone into stone and stone brick; the herder
  shears wool; the forge smelts iron. **Nothing produces glass, dyed wool, or
  non-oak planks**, so a recipe naming them can never be paid however honestly it
  describes the building: the church's 66 glass panes and the market's red wool
  came off on 2026-09-02 for exactly that reason. **Wood is wood, stone is stone
  and wool is wool** (`village/buildings/Materials.java`): a recipe's "oak log" is
  paid by any log, its "oak planks" by any plank, its "cobblestone" by any
  cobblestone, cobbled deepslate or sandstone, whatever the guard felled or the
  miner dug, and its "white wool" by wool of any colour, whatever the flock
  happened to be. A mangrove-swamp camp with a store full of mangrove was
  otherwise stuck on its first house for good. Logs also pay for planks, four to a
  log, with nobody sawing; planks never pay for logs. Because one recipe can ask
  for both, the recipe is settled as a whole, log lines first, so a log is never
  counted twice.
- **Never price a building in what it alone produces.** The lumberjack is the only
  source of planks, so it costs cobblestone and nothing else: a village that has
  only founded, and so has only a miner, must be able to build it. The stoneworks
  costs 9 logs and 12 cobblestone in every variant, including a sandstone one built from
  the very blocks it exists to make. It needs no cut stone of its own.
- **Share the default recipe across families.** Variants of a category and level normally
  resolve one recipe in generic wood and stone. A deliberate authored exception may replace
  that price for an individual building; regional exports must preserve that explicit choice.
  Do not copy default recipes into every building, because those copies would stop following tuning.

Which gives the bootstrap order a village actually follows: found (centre, mine,
storehouse, free) → miner digs cobblestone → **lumberjack**, in cobblestone alone
→ logs and planks → everything timber → **stoneworks**, in logs and cobblestone, for
cut-stone production. Basic construction no longer depends on its decorative stone bricks.
A desert mine cuts through sand and sandstone before it reaches stone, and sandstone pays a stone cost like any
cobblestone, so a desert camp bootstraps on the same schedule.

**Upgrading costs more than sprawling**, by construction. Two level-1 houses come
to 94 units for two beds where one level-2 costs 120; four level-1s cost 188
where a level-3 costs 253. Building wide stays the cheaper move, which is what
[village-tiers.md](village-tiers.md) asks for.

**Space is a fit check.** How much room a building needs comes from its own dimensions, not from
any number in this file. Whether a site can take those dimensions, and what it would cost to clear
one that nearly can, is [site-selection.md](site-selection.md).

### Variants are a look, not a recipe

**A variant is a family's shape of the same building** (decided 2026-09-01, superseding
[#50](https://github.com/Quzzar/kithkyn/issues/50), which had made variants competing
recipes). Variants of a category and level use the shared construction recipe by default
in generic wood and stone, with deliberate per-building price exceptions allowed. Which variant a village raises is settled once, at founding, by the
biome it stands in ([buildings.md](buildings.md), "Regional variants and biomes"), and kept
for its life, so a village reads as one place. The planner never chooses between variants: it
sees one building per category, the village's own family only, with the shared default price
unless an explicit exception is authored.

Two things follow:

- **A wood-poor village is not rescued by a stone variant.** It is rescued by the market and
  by "wood is wood": the guard's woodland chop pays a log cost with any wood, and logs pay a
  plank cost without a saw. A desert camp mines sandstone, and sandstone is stone.
- **The named specials stay variants.** A watermill or an igloo is a family's shape for its
  category, priced like the category. If a site cannot host one, that is site selection's
  business, not the variant system's.

What does still change with the family is what the building is made of, because the
structure file does: sandstone in a Desert house, birch and cobblestone in a Birch Forest one.
That is the template's business, not the recipe's. Making the built blocks follow the wood
actually paid, so a Birch village given spruce raises spruce houses, is a separate piece of
work that this rule leaves room for.

The upgrade to level 2 is priced at 1.5x the level-1 recipe, and level 3 at 3x. That means
upgrading always costs more than putting up a second level-1 building of the same category, which
is worth knowing but is **not a rule anyone implements**. Nothing tells the brain to prefer
sprawl. A village with land finds the cheaper option in its list and takes it; a village hemmed in
by a ravine never sees that option, because site-finding found nowhere to put it.

### Upgrade prices are never derived from the structure

That is not a detail, and getting it wrong silently deletes the whole sprawl-versus-tall choice.
Measured on the real houses we picked:

| | Solid blocks | Beds | Blocks per bed |
| --- | --- | --- | --- |
| tier 1, 7x7 | 153 | 1 | 153 |
| tier 2, 7x13 | 270 | 2 | 135 |
| tier 3, 7x11 | 340 | 4 | **85** |

**Bigger buildings are dramatically cheaper per bed**, because one roof and four walls get reused.
Four small houses cost 612 blocks for four beds; one big house costs 340 for the same four, 44%
less. So a cost that tracks the block count makes upgrading strictly better, and a village would
never sprawl.

Pricing upgrades off the level-1 recipe instead:

| | Cost | Gain | Sprawl alternative |
| --- | --- | --- | --- |
| tier 1 build | 153 | 1 bed | |
| upgrade to 2 | 230 | +1 bed | 153 for a second small house |
| upgrade to 3 | 459 | +2 beds | 306 for two more small houses |

Four beds by sprawling costs 612. By upgrading, 842, **38% more**. The invariant holds against
real geometry rather than assumed geometry.

So: **a level-1 recipe may be derived from its structure's block count. An upgrade price may
not.**

## Capabilities

What a village can do is the union of what its finished buildings grant. Capability comes from
construction, never from population or village tier.

### A building grants permission, not product

**A blacksmith does not produce iron tools. It makes iron tools possible.** The village still
needs real iron, dug by a real miner, sitting in a real chest, before a single tool exists.
Nothing in this system spawns items.

Every capability below is gated twice: once by a building standing, and once by the materials
being present. That is what keeps the simulation legible in ordinary Minecraft terms. A village
with a foundry and no diamonds has exactly what a player with a crafting table and no diamonds
has.

**The village inventory is not a number.** It is literally every container inside the
village's claim, read together, and nothing is abstracted or tracked in parallel. A recipe
is satisfiable when those chests hold those items.

**One pool for planning; real walking for work** (decided on
[#49](https://github.com/Quzzar/kithkyn/issues/49)). The union above is what the brain
counts when it asks "can we afford this", so affordability is simple and cannot deadlock on
geography. Workers do not get that luxury: a villager who needs an input walks to the
nearest container that actually holds it and carries it back, and a villager with output in
hand walks it to the nearest container with room. Goods are always in some chest, visibly,
and moving them is a trip someone makes.

The consequences that follow, and which the implementation owes:

- **The storehouse is capacity, not a special inventory.** Every building's chest counts
  toward the same pool; a storehouse is simply the building whose job is holding a lot. It
  stays in the founding set because a camp needs somewhere to put things.
- **A home's own chest is not in the pool** (built 2026-09-01). A definition may list a
  container under `personal_containers` instead of `containers`: every house does, and so does
  the bedside chest of a workplace with a live-in bed and more than one chest (the church, the
  lumberjack hut, the level-1 watchtower). It belongs to whoever sleeps there, shared between
  them. It is never registered as village storage, so no fetch reads it, no deposit fills it,
  the planner does not count it as a store, and the quartermaster's sweep never sees it
  (`village/PersonalChest`). What goes in is whatever kinds of thing its residents choose to
  keep at bedtime, any number or none ([worker-loops.md](worker-loops.md), "A chest of their
  own"). The camp circle is a home for four, so its one chest is theirs, shared (Aaron,
  2026-09-01): a new village's storage is the storehouse's two barrels. A workplace with a
  single chest that is first a workplace, the level-1 blacksmith and the level-2 watchtower,
  keeps that chest shared, and the upper blacksmith's two chests both sit in the workshop, well
  away from the bed, so it has none either. When a home is rebuilt for an upgrade its chest is emptied into village storage with
  the rest, since the alternative is the residents' things on the floor of a building site; and
  a leaver's chest stays with the house, for whoever moves in next.
- **A fetch can fail even when the pool says yes** — the chest holding it is unloaded, or
  unreachable. That emits the ordinary shortage event and a personal-log entry, and the
  worker gives up rather than spinning.
- **A full chest is a storage shortage.** A worker with nowhere to deposit carries to the
  next container with room; when none has room, that is an event, and it is what should make
  a village decide to build another storehouse. The collective planning context states that
  shared storage is full and that more shared storage is urgent. The worker keeps any rejected
  bedtime deposit in their pack. Storehouse containers add capacity even when their attached
  quartermaster job is open, so duplicate-vacancy filtering never hides a storehouse or its
  upgrade during the shortage.

**Who may take from a chest.** Any villager, for a real need: there is no ownership between
residents over the village's stores. A home's own chest is the one exception: only the people
who sleep there put things in it, and no worker's fetch ever reads it. The player may freely
PUT items into any village container, which is a gift and nothing else. The player TAKING
items, or breaking a chest, is **theft, but only if a villager sees it happen**: awake, within
about sixteen blocks, with line of sight. A home's chest counts as a village container here:
robbing someone's house is theft the same as robbing the storehouse. An
unwitnessed theft genuinely costs nothing, because nobody knows. A witnessed one is
recorded by the witness as a fact, and how much they hold it against you is their own
judgement on reflection; it reaches the rest of the village only as gossip through the
relationship web, not as an instant village-wide verdict. The consequences are detailed in
[#64](https://github.com/Quzzar/kithkyn/issues/64).

### How upgrading works

Decided on [#56](https://github.com/Quzzar/kithkyn/issues/56), then extended on 2026-09-04.
`upgrades_from` defines a cheaper reuse path, not the only way a higher level can exist. When a
compatible predecessor fits, the planner prefers the on-site upgrade. When it does not fit or
does not exist, the village may build the target level on a separate site at the cost of the base
building plus every upgrade through that level.

**Rebuilt block by block around the old footprint.** An upgrade is ordinary construction with
the new template over the old, using the machinery that already builds everything else, so you
watch it happen. It keeps the standing building's orientation, but the new origin may slide in
either horizontal direction. Every translated position in which the larger footprint fully
contains the old footprint is checked. The least ground work wins, with the most centred
extension breaking a tie. This is a rebuild within the same parcel rather than preservation of
each old block: the containing rule ensures the new template replaces the whole old footprint
and leaves no fragment behind. It lets a house grow west when its east side is blocked. The same
one-block lane is still required around every other building.

A narrower authored upgrade is also valid when the only old cells left outside the target
are dirt, grass blocks, plants or air. Those remain natural landscaping, not demolition targets.
Every structural block must still be inside the replacement parcel. This handles the Birch
watchtower's one-column grass border without adding artificial padding to its second tier.

The mine is the exception. Its root and child shafts are runtime geometry below the saved template,
so moving the headframe would strand the network. A mine upgrade retains its exact origin and its
saved child plans, and uses the fresh higher-level path elsewhere when that authored expansion
direction is blocked.

If no containing position works, that reuse path is refused and the lower-level building simply
stands. The higher level may still be planned elsewhere at its full combined cost. Nothing is
reserved in advance.

**The chest is emptied before work starts.** Contents are carried into other village containers
first, which the one-pool decision on [#49](https://github.com/Quzzar/kithkyn/issues/49) already
makes the natural move. Anything they reject enters the village's persisted structural-overflow
queue with full item components. The old container positions are unregistered once construction
starts, so queued goods cannot be put back into blocks about to be overwritten. They are retried
against real storage until the rebuilt containers come online. Nothing is destroyed, and a full
storehouse can therefore upgrade into the capacity needed to receive its own displaced contents.

**The worker keeps their job and waits.** The assignment is held, the station is unusable
for the duration, and the worker idles at the campfire until it is ready. The alternative —
returning them to the pool — would let stat-based placement quietly reshuffle the village
on every upgrade, and a smith who was a smith yesterday should still be one tomorrow.

**The building is unusable while it is being rebuilt.** That is the honest cost of the
upgrade and the reason a village should think before starting one.

**Stations must be reconciled, not assumed.** Most upgrades ADD stations (a watchtower goes
from one guard to two), and `processNewBuilding` only ever runs at construction, so a
definition that gains stations registers nothing. Shrinking and reordering are already safe:
`releaseInvalidAssignments` releases held assignments whose station no longer matches and
drops stale openings. Growing needs the additive half — walk the current definition's
stations, count what is represented across booked and open assignments, and register the
missing ones. Run it after any upgrade and after any datapack reload, which fixes the same
bug in its other guise: an author editing a definition on a live world.

**Redevelopment can remove specific blocking buildings as part of a named construction project.**
The game calculates the placement, consequences and salvage, then the model chooses. Net
materials and a viable transition must be secured before removal. The saved demolition phase
feeds into ordinary preparation and construction. See [redevelopment.md](redevelopment.md)
for safeguards, accounting and benchmarks for useful adoption and excessive rebuilding.

### What a capability is at runtime

Decided on [#55](https://github.com/Quzzar/kithkyn/issues/55).

**Derived, never stored.** A village's capabilities are simply the set of strings granted by
the buildings currently standing, recomputed whenever a building is added or lost. Nothing
persists, so nothing can desync from reality, and a datapack can invent a capability name
without touching Java. This supersedes `Building.Benefit`, the closed enum that exists today
and is read by nothing: it should be deleted rather than extended.

`grants_if` resolves in the same derivation as a fixed point — grant everything
unconditional, then re-evaluate the conditional grants until nothing new appears. Two
buildings that each require the other's capability simply never grant, which is the correct
quiet failure rather than a crash.

**Capabilities gate what the village can MAKE, never what it can build.** No blacksmith
means no iron tools; no church means no healing. Construction is gated by materials and
space alone, so a village can always build its way toward a capability it lacks and can
never lock itself out.

**Production is opportunistic, decided by the worker.** Nobody schedules it. A blacksmith
standing at their station looks at what the village is short of and makes it from the shared
pool, using the same "what do we lack" reasoning the planner already applies to buildings.
There is deliberately no request queue: a miner with a worn pickaxe does not file a demand,
the smith simply notices the village is short of pickaxes. If that proves too vague in play,
a demand signal is a later addition, not a prerequisite.

**The brain is never told what it cannot do.** Capability filtering happens before the model
sees anything, exactly as building options are filtered by affordability, so it chooses among
legal moves only and cannot fixate on an unreachable ambition.

| Group | Capabilities |
| --- | --- |
| Tools | `TOOLS_STONE` (baseline), `TOOLS_IRON`, `TOOLS_DIAMOND` |
| Armor | `ARMOR_LEATHER`, `ARMOR_IRON`, `ARMOR_DIAMOND`, `SHIELDS` |
| Smithing | `REPAIR`, `SMELTING` |
| Food | `GRAIN`, `MEAT`, `FOOD_COOKED`, `FOOD_PRESERVED`, `FOOD_BAKED`, `ALE` |
| Materials | `LOGS`, `PLANKS`, `STONE`, `CUT_STONE`, `ORES`, `FUEL`, `BRICK`, `GLASS`, `STAINED_GLASS`, `CLOTH`, `DYED_CLOTH` |
| Military | `PROTECTION`, `SOLDIERS`, `VETERANS`, `ARROWS` |
| Services | `WATER`, `HEALING`, `ENCHANTING`, `LEARNING`, `POTIONS`, `TRADE` |

`ATTRACTIVENESS` is deliberately not in that list, because it is not a capability. It is the
village's existing 0-to-100 score from [population-and-labor.md](population-and-labor.md), the
thing that already governs whether anyone moves in. Buildings that "raise morale" raise *that*,
and the brain reads it directly: people are unhappy, can we do something about it. There is no
second happiness stat.

Beds and containers are not capabilities either. They are beds and containers, counted by looking
at them.

Three capabilities need more than one building:

| Capability | Requires |
| --- | --- |
| `ENCHANTING` | `church_2` and `library_1` |
| `TOOLS_DIAMOND`, `ARMOR_DIAMOND` | `blacksmith_3` and `mine_3` |
| An inn's lift to `ATTRACTIVENESS` | `inn_1` and a brewery actually supplying ale |

### How a conditional grant is declared

**Implemented** as of [#68](https://github.com/Quzzar/kithkyn/issues/68). `grants` is a
list of capability strings; `grants_if` is a list of objects naming a capability plus
`requires_capability` and/or `requires_supply`. `VillageCapabilities.resolve` walks the
standing buildings, takes everything unconditional, then re-evaluates the conditional ones
until a pass adds nothing. `/kkdev village capabilities` prints the resolved set and what
each building contributes, marking conditional grants as granted or withheld with the
requirement that decides it.

One rule worth stating because it caught the market: **currency is not a supply.** A
village's treasury is physical emeralds sitting in a village container, so a supply
requirement naming emeralds would be satisfied by the village's own money. Emeralds are
skipped when checking supply.


**Conditions name capabilities and supplies, never building ids.** A church should not care which
library variant the village built, or whether a future datapack adds a third way to get
`LEARNING`.

```json
{
  "grants": ["HEALING"],
  "grants_if": [
    { "capability": "ENCHANTING", "requires_capability": ["LEARNING"] }
  ]
}
```

| Kind | Checked | Behaviour |
| --- | --- | --- |
| `requires_capability` | When the village's capability set is recomputed, on a building finishing or being lost | Static. Either the village has `LEARNING` or it does not. |
| `requires_supply` | On the brain's slow tick, against real container contents | Dynamic. An inn with no ale grants nothing this tick and grants again when the brewery catches up. |

**A station declares its grant.** A definition that adds a work station also carries the
grant that post stands for: a cleric station comes with `HEALING`, a blacksmith station with
`REPAIR`, and a building with both carries both (Aaron, 2026-09-12: "the planner knows what
buildings will do what"). The table lives in `StationGrants`. The loader warns about every
station whose grant is missing rather than rejecting the building, so a private datapack still
loads and its author sees the gap in the log; a test holds the bundled catalog to zero
warnings. Guard, builder and leader stations are exempt: the watchtower's guards grant
`PROTECTION` while the centre's captain does not, and that is a planning choice. So is a
station with a `worksite_category`: the centre's miner post works at the mine, and the mine
carries `ORES`. The quartermaster is exempt outright: `STORAGE` means shelves, the storehouse
that holds them grants it, and the desert and floodplain centres keep their quartermaster at a
centre with no chest by design.

Capability resolution is a fixed point: grant everything unconditional, then re-evaluate
`grants_if` until nothing new appears. Two buildings that each require the other's capability
simply never grant, which is the correct and quiet failure.

---

## The cut

Decided on [#57](https://github.com/Quzzar/kithkyn/issues/57). The catalogue below is
**22 categories**, down from 36. The filters were the ticket's own: a category needs a work
loop describable in one sentence, must not be another category wearing a different hat, and
must not exist solely to feed something else. Stronghold, the stated model, shipped roughly
25 building types.

**Survivors (17).** Infrastructure: `village_center`, `house`, `well`, `storehouse`,
`watchtower`. Extraction: `farm`, `lumberjack`, `stoneworks`, `mine`. Food:
`hunting_lodge`, `fishery`, `bakery`, `butchery`. Craft: `blacksmith`. Civic: `market`,
`church`, `tavern`.

**Cut, and why.** These are gone rather than deferred, so nobody re-proposes them in six
months:

- `granary` — absorbed into `storehouse` — it was a storehouse that only held food
- `mill` — collapsed into `bakery` — the baker grinds their own grain; a step nobody watches is complexity with no audience
- `charcoal_burner` — collapsed into `blacksmith` — the smith burns their own charcoal
- `kiln` (with the `pottery` and `glassworks` it had absorbed) — cut — Aaron's call: the village needs no dedicated brick/glass producer. No building costs brick, and glass is bought through the market like any other traded good (`glass_pane` derives to an authored value, so it is always purchasable). This takes the survivor count from 22 to 21.
- `weaver` and `workshop` — folded into `butchery` — Aaron built the old `tannery` as a livestock butchery (a fenced pen of cows and sheep with a worker who makes meat, leather, and wool), which produces leather directly, so there is no separate leather-goods building. The `tannery` is accordingly renamed `butchery`.
- `pasture` — folded into `butchery` — the butchery already holds the cows and sheep; a separate livestock building is redundant.
- `brewery` — cut — Aaron's call: pure flavour, and there is no ale item for the brewer to make.
- `library` — cut — Aaron's call: knowledge grants nothing the design uses.
- `inn` — renamed `tavern` — it is a bar that draws wanderers (grant `WANDERERS`), not lodging: no beds and no residents. Together with the four cuts above, this takes the survivor count from 21 to 17.
- `mushroom_cellar` — a `farm` variant, not a category
- `apiary` — no worker and no capability it uniquely grants
- `graveyard` — its only purpose was MORALE, and there is no second happiness stat
- `fletcher` — needs an arrow economy that does not exist
- `armoury` — a `blacksmith` at level 3, not a separate trade
- `alchemist` — needs a potion system that does not exist
- `barracks` — needs a military system beyond guards; the `watchtower` carries defence for now
- `training_yard` — a stat on a building that no longer survives

Walls and gatehouses are not in the count either way: they are linear rather than footprints
and are probably not `Building`s at all.

**The structure manifest below predates these cuts** and still counts 173 files across 36
categories. It needs regenerating against the 21 (it still enumerates the cut `kiln`,
`pottery`, and `glassworks`, along with every other cut category).

## Beds belong to houses

Decided on [#61](https://github.com/Quzzar/kithkyn/issues/61). **A workplace never
contains a bed.** Blacksmiths smith; houses house. The one exception is `village_center`,
because a camp is people sleeping around a fire before there are any houses, so the centre
carries the starting beds and nobody minds that a station shares the building.

This settles an incoherence that had gone unnoticed: `population-and-labor.md` assigns beds
on arrival independently of employment, so a bed inside the blacksmith went to whichever
newcomer arrived next rather than to the blacksmith. With workplaces bedless, arrival-order
assignment is simply correct, and no employment-aware bed logic is needed. Villagers keep
the first free bed they are given and do not move house when their job changes, so a
villager may well walk across town to work.

It also makes housing the real growth lever: beds are the population cap, houses are the
only source of beds, so a village that wants to grow must build houses. And it makes both
halves of the vanilla template library usable — the 51 bed-only templates as houses, the 87
station-only ones as workplaces — where pairing them made most of vanilla useless to us.

**The catalog's bed columns below predate this rule** and still list beds on workplaces.
They are wrong wherever they do; the shipped definitions have already had those beds
removed.

## The catalog

The "Variants" line under each category names what the jar bundles today, which is the Birch
Forest catalog alone ([birch-village.md](birch-village.md)); the private Desert and Badlands
packs enumerate their own buildings in [desert-village.md](desert-village.md) and
[badlands-village.md](badlands-village.md). Recipes are per category and level, never per
variant, so each table's recipe column applies to every family.

### Core and civic

#### `village_center`  (founding building)

Worker: **BUILDER**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | camp circle | 7x7 | placed free at founding | 4 beds, a chest of the campers' own, campfire, bell, BUILDER station |
| 2 (upgrade) | village hall | 11x11 | 48 oak log, 76 oak planks, 80 cobblestone, 12 glass, 8 wool, 8 iron ingot | +2 beds, +2 containers, LEADER station: the brain's voice, shown in the UI |
| 3 (upgrade) | town hall | 21x21 | 100 oak log, 148 oak planks, 156 cobblestone, 28 glass, 16 wool, 16 iron ingot | +4 beds, +3 containers, festivals: a periodic lift to ATTRACTIVENESS |

Founding building, placed free. Four beds, one chest, a campfire and a bell outside, and a single BUILDER station. That is the entire camp. The four beds are the whole starting housing cap, so a camp supports four people until it builds a house. The chest is the campers' own (`personal_containers`), shared by the four who sleep there, not village storage: the camp keeps its goods in the storehouse.

**No level-2 centre exists yet: no definition and no structure.** That is the whole reason no
village on the shared server has ever upgraded its centre (checked against Wildflower Downs'
logs, 2026-09-02). The planner used to add a second reason, filtering the entire
`village_center` category as founding-only, which would have hidden a level-2 centre even
once one shipped; it now permits an on-site `village_center_<family>_2` upgrade while still
forbidding a second centre on a fresh site. When one is authored it should keep its
`gathering_point` at the level-1's local offset. The planner may translate the containing parcel
a few blocks to make the larger footprint fit, in which case the campfire moves with the hall
and the paths are laid again to its new world position.

#### `house`

Worker: **none**  ·  Phase 1  ·  Variants: `birch_forest` (levels 1 and 2); `igloo` and `stilt` are proposals

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | cottage | 7x7 | 12 oak log, 20 oak planks, 20 cobblestone, 4 glass, 2 wool | BEDS 2 |
| 2 (upgrade) | house | 11x11 | 20 oak log, 28 oak planks, 32 cobblestone, 6 glass, 4 wool | BEDS 4 |
| 3 (upgrade) | longhouse | 15x15 | 40 oak log, 60 oak planks, 64 cobblestone, 12 glass, 8 wool | BEDS 7 |

The housing cap, the most numerous building in any village, and where regional identity actually reads. Gets more variants than anything else for exactly that reason. `igloo` is a cheap snowy-only L1; `stilt` is the wetland answer.

Every house level also has one chest, listed as `personal_containers`: the residents' own, not
the village's (see "A home's own chest" under
[A building grants permission, not product](#a-building-grants-permission-not-product)). The
grants column names beds alone because beds are what a house gives the village; the chest is
what it gives the people who live in it.

#### `well`

Worker: **none**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | well | 5x5 | 6 oak log, 8 oak planks, 8 cobblestone | WATER |
| 2 (upgrade) | fountain | 7x7 | 8 oak log, 12 oak planks, 12 cobblestone, 2 glass | WATER, raises ATTRACTIVENESS slightly |

The desert variant is a covered cistern, because open water evaporates and a desert village that digs an open well is a village that has not lived in a desert.

The old families' wells stood ON the ground with the pool at rim level, as authored
(2026-09-01): their definitions declared `"sink": -1`, so the base course, a solid ring of the
variant's stone, sat on top of the ground's top block, with the water and its trapdoor-and-fence
rim one above that. The bundled Birch well seats at `"sink": 0`, the way its own capture was
authored. (`sink` seats a structure that many layers below the ground plane; a negative value
raises it. The well was tried at 1 and 0 first and Aaron judged both a block too low in the
world. The ground is still prepared and claimed at the surface whatever the sink.) The well
used to leak from its rim for two reasons, both fixed for every building: the builder's
block-by-block build set the water down before the rim, so the pool spread over the ground and
every trapdoor placed into that spill was waterlogged and became a source; and placement ran
with vanilla's keep-liquids rule, which let a pond beside a site soak into one rim block and
then round the whole ring. Now the builder places every liquid-bearing block of a structure
last, and both placement paths ignore waterlogging, so a block set into water replaces it. A
rim placed before its water stays dry: verified on a bare platform, rim first and water last,
not one trapdoor or fence waterlogged. The pool was briefly moved a layer down into the base
on a wrong reading of the flow rules; Aaron restored the authored layout.

#### `storehouse`  (founding building)

Worker: **none**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | storehouse | 7x7 | 8 oak log, 16 oak planks, 16 cobblestone, 2 glass | 2 barrels into the village inventory |
| 2 (upgrade) | great storehouse | 11x11 | 16 oak log, 24 oak planks, 24 cobblestone, 4 glass, 2 wool | 8 containers |
| 3 (upgrade) | warehouse | 15x15 | 28 oak log, 44 oak planks, 48 cobblestone, 8 glass, 6 wool | 20 containers |

Founding building, placed free. Two barrels: the whole of a new village's inventory, since the camp circle's chest belongs to the campers. Absorbed the granary: both were always the same chests read by the same code, so one category covers both.

**What ships is not the table above.** The bundled storehouse is the Birch 16x19 store with
six shared containers and a QUARTERMASTER station ([birch-village.md](birch-village.md)); no
level 2 or 3 is bundled, so no bundled storehouse upgrades. The old plains tent, and the 19x15
warehouse that never once fit around it on the shared server (46 refusals logged for Wildflower
Downs alone), went with the Village Life families on 2026-09-10; a smaller level 2 remains the
open design for any family that wants one.

#### `market`

Worker: **MERCHANT**  ·  Phase 2  ·  Variants: `birch_forest` (levels 1 to 3)

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | market stall | 7x7 | 16 oak log, 14 cobblestone | a trading screen: the player buys and sells with the village, in emeralds |
| 2 (upgrade) | market | 11x11 | 32 oak log, 28 cobblestone, 12 wool | the village spends its own emeralds on what the biome cannot make |
| 3 (upgrade) | trade hall | 15x15 | 48 oak log, 42 cobblestone, 18 wool, 4 iron ingot | better rates, wider stock |

Recipes as shipped (decided 2026-09-02): the stall costs wood and stone only, the market adds
wool of any colour, and the trade hall adds a little iron a forge can smelt. The red wool every
level used to ask for came off, since nothing in the village dyes anything, and so did the
stall's iron, which no village without a forge could ever have raised.

The village's trade organ, and the *legitimate* alternative to taking from its chests. Needs a staffed MERCHANT like any other workplace; without a market there is no trade at all, for the player or the village. Levels grant capabilities rather than numbers: L1 access, L2 initiative (trading unattended), L3 better rates. The treasury is physical emeralds in this building's chest, and a village founds broke. Full design in [economy.md](economy.md).

#### `inn`

Worker: **INNKEEPER**  ·  Phase 3  ·  Variants: `birch_forest`, shipped as `tavern_birch_forest_1`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | alehouse | 11x11 | 32 oak log, 48 oak planks, 48 cobblestone, 8 glass, 6 wool | raises ATTRACTIVENESS while ale sits in the village inventory |
| 2 (upgrade) | inn | 11x11 | 48 oak log, 68 oak planks, 72 cobblestone, 12 glass, 8 wool | raises ATTRACTIVENESS more, draws wanderers |
| 3 (upgrade) | tavern | 15x15 | 92 oak log, 140 oak planks, 148 cobblestone, 24 glass, 16 wool, 8 iron ingot | raises ATTRACTIVENESS most, recruits wanderers |

Consumes ale from the brewery. An inn with no ale is an empty room and grants nothing, which is the point: the attractiveness lift has a real supply chain behind it.

### Faith, learning, memory

#### `church`

Worker: **CLERIC**  ·  Phase 2  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | shrine | 7x7 | 16 oak log, 28 oak planks, 28 cobblestone, 4 wool | HEALING basic |
| 2 (upgrade) | church | 15x15 | 28 oak log, 40 oak planks, 40 cobblestone, 4 wool | HEALING, ENCHANTING (requires library_1), raises ATTRACTIVENESS |
| 3 (upgrade) | cathedral | 21x21 | 52 oak log, 80 oak planks, 84 cobblestone, 8 wool, 2 diamond | raises ATTRACTIVENESS strongly |

No glass at any level (decided 2026-09-02): nothing in the village makes it, and the shipped
`church_*_1` recipe lost its 66 panes the same day, and its 1800 stone bricks became 380 with
the 133 logs kept: a model shown eighteen hundred of anything sees no way to ever get there.

The clearest two-building capability in the catalog: ENCHANTING needs church L2 AND library L1, and neither grants it alone.

#### `library`

Worker: **LIBRARIAN**  ·  Phase 3  ·  Variants: none shipped

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | scriptorium | 7x7 | 20 oak log, 28 oak planks, 32 cobblestone, 6 glass, 4 wool, 12 book | LEARNING, book production |
| 2 (upgrade) | library | 11x11 | 28 oak log, 44 oak planks, 48 cobblestone, 8 glass, 6 wool, 24 bookshelf | LEARNING, ENCHANTING support |

### Food

#### `farm`

Worker: **FARMER**  ·  Phase 1  ·  Variants: `birch_forest` (levels 1 and 2)

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | croft | 11x11 | 16 oak log, 28 oak planks, 28 cobblestone, 4 glass, 4 wool | GRAIN |
| 2 (upgrade) | farm | 15x15 | 28 oak log, 40 oak planks, 40 cobblestone, 8 glass, 4 wool | GRAIN more, vegetables |
| 3 (upgrade) | estate farm | 21x21 | 52 oak log, 80 oak planks, 84 cobblestone, 16 glass, 8 wool, 8 iron ingot | GRAIN most, irrigation works poor soil |

The desert variant is terraced and irrigated: it costs more for the same yield, which is exactly what farming a desert should feel like.

#### `tannery`

Worker: **TANNER**  ·  Phase 1  ·  Variants: `birch_forest`, shipped as `butchery_birch_forest_1`

*Absorbs the old `pasture`.* Keeping cattle and taking beef and leather off them is
one job, not two buildings. **There is no separate "turn hides into worked leather"
step**, because a cow in Minecraft drops leather outright: a processing stage there
would be busywork with no material change behind it, unlike the mason, who turns
cobblestone into a block that genuinely does not otherwise exist.

The station is the water cauldron, vanilla's own leatherworker block, which reads as
the tanning vat; the smoker beside it cures the beef.

This is the settled half of the animal split. The other half is `hunting_lodge`,
which is deliberately the opposite shape: a hunter roams out after wild animals,
where the tanner cultivates a herd that stays put.

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | tannery | 15x7 | 16 oak log, 24 oak planks, 24 cobblestone, 4 glass, 2 wool | MEAT, LEATHER |
| 2 (upgrade) | stockyard | 15x15 | 24 oak log, 36 oak planks, 36 cobblestone, 6 glass, 4 wool | MEAT more, LEATHER more |
| 3 (upgrade) | ranch | 21x21 | 48 oak log, 68 oak planks, 72 cobblestone, 12 glass, 8 wool | breeding: the herd grows on its own |

Wool still matters as the bottleneck on beds, and the pen is what produces it: the shipped
butchery structures carry their starting cows and sheep in the template, and a HERDER
station beside the pen keeps the herd sheared, fed, and marked as farmed stock the hunter
will not touch (docs/worker-loops.md). The pen has a size: the BUTCHER keeps each kind at
six and slaughters whatever grows past it, while the herder breeds up to a ceiling of twelve
that only a pen without a butcher reaches, so the yard is never packed solid and everything
above six is meat and hide.

#### `hunting_lodge`

Worker: **HUNTER**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | hunter's camp | 7x7 | 12 oak log, 20 oak planks, 20 cobblestone, 4 glass, 2 wool | MEAT, LEATHER |
| 2 (upgrade) | hunting lodge | 11x11 | 20 oak log, 28 oak planks, 28 cobblestone, 4 glass, 4 wool | MEAT more, feathers, hides |

A camp, not a workplace: a shelter, a fire, a chest and an archery target on worn
ground. That is deliberate, and it is the half of the animal split that `tannery`
is not. A tanner cultivates a herd that stays put; a hunter roams after animals
that do not, so the hunter's building is somewhere to return to rather than
somewhere to stand all day.

The lit campfire is safe despite `KithkynPoiTypes` registering a POI over lit
campfire states: the village reads its gathering point from the town centre's own
`gathering_point` field, and nothing consumes that POI.

Feathers at L2 are what make the fletcher possible at all.

#### `fishery`

Worker: **FISHER**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | fishing hut | 9x7 | 12 oak log, 16 oak planks, 16 cobblestone, 4 glass, 2 wool | MEAT (fish), WATER |
| 2 (upgrade) | fishery | 11x11 | 16 oak log, 24 oak planks, 28 cobblestone, 4 glass, 4 wool | MEAT more, docks |

**It grants WATER, because it contains a 2x2 of source blocks**, which is an
infinite water source in Minecraft. That does not make the `well` redundant: a
well is cheap and lifts attractiveness, a fishery feeds people. Neither ranks
above the other, they differ.

Carrying its own water is also what makes this shippable at all. A fishery is the
one category whose SITE matters, and site-selection does not understand
shorelines; a self-contained source means it can be built anywhere. Requiring a
real shore belongs with the level-2 docks.

Keys off adjacent water, not off a biome, so it serves coast, river, lake, and swamp alike.

#### `bakery`

Worker: **BAKER**  ·  Phase 2  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | bake house | 7x7 | 16 oak log, 24 oak planks, 28 cobblestone, 4 glass, 4 wool | FOOD_BAKED: bread |
| 2 (upgrade) | bakery | 11x11 | 24 oak log, 36 oak planks, 40 cobblestone, 6 glass, 4 wool | FOOD_BAKED: pies and cake |
| 3 (upgrade) | guild bakery | 15x15 | 48 oak log, 76 oak planks, 80 cobblestone, 12 glass, 8 wool, 4 iron ingot | FOOD_BAKED at the best conversion ratio |

#### `butchery`

Worker: **BUTCHER**  ·  Phase 2  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | smokehouse | 7x7 | 12 oak log, 20 oak planks, 20 cobblestone, 4 glass, 2 wool | FOOD_COOKED |
| 2 (upgrade) | butchery | 11x11 | 20 oak log, 28 oak planks, 32 cobblestone, 6 glass, 4 wool | FOOD_PRESERVED: keeps through winter |

Consumes FUEL, which is what ties the food chain to the mine or the charcoal burner.

#### `brewery`

Worker: **BREWER**  ·  Phase 3  ·  Variants: none shipped

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | brewhouse | 11x11 | 24 oak log, 32 oak planks, 36 cobblestone, 6 glass, 4 wool, 4 iron ingot | ALE |
| 2 (upgrade) | brewery | 15x15 | 32 oak log, 48 oak planks, 52 cobblestone, 8 glass, 6 wool, 8 iron ingot | ALE enough to keep an inn supplied |

### Materials

#### `lumberjack`

Worker: **LUMBERJACK**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | woodcutter's hut | 7x7 | 8 oak log, 16 oak planks, 16 cobblestone, 2 glass | LOGS |
| 2 (upgrade) | sawmill | 11x11 | 16 oak log, 24 oak planks, 24 cobblestone, 4 glass, 2 wool, 4 iron ingot | PLANKS |
| 3 (upgrade) | timber yard | 15x15 | 28 oak log, 44 oak planks, 48 cobblestone, 8 glass, 6 wool, 8 iron ingot | PLANKS more, beams for large footprints |

PLANKS at L2 is a real gate, not throughput: without it a village builds in logs and stone only.

#### `stoneworks`

Worker: **MASON**  ·  Phase 1  ·  Variants: `birch_forest`

*Replaces the old `quarry`.* A quarry was a second hole competing with the mine for
the same cobblestone, which is not a building, it is a duplicate. **The mason does not
dig.** The mine brings raw stone up; the mason turns it into what a village actually
builds with. That chain is load-bearing rather than flavour: `stone_bricks` are the
single most-used crafted block across every structure shipped so far, and nothing
gathers them.

The conversion needs no new mechanic. `ProcessItemGoal` is general (input stack,
output stack, sound) and already does this work elsewhere: the lumberjack turns
stripped logs into planks, the farmer turns pumpkins into seeds. The mason is
registered the same way, which is also why this needs no separate "sawmill"-style
building of its own.

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | stone yard | 7x7 | 12 oak log, 16 oak planks, 16 cobblestone, 4 glass, 2 wool | STONE, CUT_STONE |
| 2 (upgrade) | stoneworks | 11x11 | 16 oak log, 24 oak planks, 28 cobblestone, 4 glass, 4 wool, 4 iron ingot | CUT_STONE more, sandstone |
| 3 (upgrade) | masonry | 15x15 | 32 oak log, 48 oak planks, 52 cobblestone, 8 glass, 6 wool, 8 iron ingot | pillars and decorative stone |

#### `mine`  (founding building)

Worker: **MINER**  ·  Phase 1  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | mine shaft | 7x7 | 16 oak log, 20 oak planks, 24 cobblestone, 4 glass, 2 wool | ORES: coal and iron. FUEL |
| 2 (upgrade) | twin headframe | 13x7 | 16 oak log, 20 cobblestone | a second MINER station and shaft, a second chest; ORES and FUEL as level 1 |
| 3 (upgrade) | deep mine | 15x15 | 44 oak log, 64 oak planks, 68 cobblestone, 12 glass, 8 wool, 12 iron ingot | ORES: diamond |

Founding building, placed free, and the only job a new camp has besides its builder. Two upgrades for one capability: DIAMOND at L3 is what makes blacksmith L3 mean anything, and the pairing is deliberate. The deepest mine and the greatest forge are a village's endgame together.

Level 2 shipped as scale, not capability, in the old families; no bundled family has one
today, and the derivation script went with those families. Nothing in the code gates which ore a pick brings
up (a miner pulls whatever `c:ores` block her tool can harvest), so an ORES grant naming gold
or lapis would have been a label with no reader. Because an upgrade is paid as the difference
between the two recipes (`BuildingUpgrade.effectiveCost`), it costs exactly what a second
level-1 mine costs, 8 logs and 10 cobblestone: a second shaft on the same ground rather than
on a second site. The level-3 row stays a sketch.

### Craft

#### `blacksmith`

Worker: **BLACKSMITH**  ·  Phase 2  ·  Variants: `birch_forest`

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | forge | 11x11 | 28 oak log, 44 oak planks, 44 cobblestone, 8 glass, 6 wool | TOOLS_IRON, REPAIR, SMELTING |
| 2 (upgrade) | smithy | 11x11 | 44 oak log, 64 oak planks, 68 cobblestone, 12 glass, 8 wool, 16 iron ingot | ARMOR_IRON |
| 3 (upgrade) | foundry | 15x15 | 84 oak log, 128 oak planks, 136 cobblestone, 24 glass, 16 wool, 32 iron ingot, 4 diamond | TOOLS_DIAMOND, ARMOR_DIAMOND (requires mine_3) |

The worked example for capability-by-level. Every level is a genuine unlock rather than throughput, and L3 additionally requires mine_3 for its diamond supply.
Level 1 costs no iron (decided 2026-09-02): the forge is what makes iron ingots possible, so a
recipe that needed them could never be started. Wildflower Downs was offered "save up for a
blacksmith, still needs 4 iron ingot" every round with no way to smelt one. The datapack
recipes (`buildings/blacksmith_*_1.json`) lost their iron the same day; the upper levels keep
theirs, since a standing forge smelts it.

#### `workshop`

Worker: **TANNER**  ·  Phase 3  ·  Variants: none shipped

*Merged category: absorbs the old `weaver`: turns hides and wool into goods.*

**The leather half of this is now redundant** and should probably be cut: `tannery`
yields leather directly, because cows drop it. What is left worth having here is the
wool half, which is the real bottleneck on beds.

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | tanning racks | 7x7 | 12 oak log, 20 oak planks, 20 cobblestone, 4 glass, 2 wool | worked LEATHER |
| 2 (upgrade) | tannery | 11x11 | 20 oak log, 28 oak planks, 32 cobblestone, 6 glass, 4 wool | ARMOR_LEATHER |

### Military

#### `watchtower`

Worker: **GUARD**  ·  Phase 1  ·  Variants: `birch_forest` (levels 1 and 2)

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | watchpost | 5x5 | 8 oak log, 12 oak planks, 16 cobblestone, 2 glass | PROTECTION, GUARD station x1 |
| 2 (upgrade) | guard tower | 7x7 | 12 oak log, 20 oak planks, 20 cobblestone, 4 glass, 2 wool | GUARD station x2, longer sight range |
| 3 (upgrade) | keep tower | 11x11 | 28 oak log, 40 oak planks, 40 cobblestone, 8 glass, 4 wool, 8 iron ingot | GUARD station x3, alarm bell raises the village |

The only phase 1 military building. A camp with no watchpost is a camp the wolves clear out.

#### `wall`

Built by **BUILDERS** as a perimeter project, with one wall stage for every village style.
There are no wall upgrades. Desert uses sandstone, Mesa/Pueblo red sandstone and Birch
cobblestone. Desert and Mesa keep oak trapdoors and normal ladders for access to guard posts.

Construction consumes one regional wall material per ten placed cells. The perimeter
includes gates, corners, and accessible guard posts. See [walls.md](walls.md) for
planning, construction, and placement rules.

#### `gatehouse`

Worker: **none**  ·  Phase 4  ·  Variants: none shipped

| Level | Name | Footprint | Recipe | Grants |
| --- | --- | --- | --- | --- |
| 1 | gate | 7x7 | 8 oak log, 12 oak planks, 16 cobblestone, 2 glass | controlled entry |
| 2 (upgrade) | gatehouse | 11x11 | 20 oak log, 28 oak planks, 32 cobblestone, 6 glass, 4 wool, 4 iron ingot | controlled entry, GUARD station x1 |
| 3 (upgrade) | barbican | 15x15 | 40 oak log, 60 oak planks, 64 cobblestone, 12 glass, 8 wool, 12 iron ingot | controlled entry, GUARD station x2, portcullis |

Pairs with `wall`. Same phase, same blocker.

---

## Structure manifest

> **Superseded.** This manifest predates the 36→21 category cut and still enumerates cut
> categories (`kiln`/`pottery`/`glassworks`, `graveyard`, `apiary`, `charcoal_burner`,
> `weaver`, and all of Phase 4), and it enumerates the old Village Life families removed on
> 2026-09-10. It needs regenerating against the 21 survivors and the bundled Birch catalog;
> until then the per-category tables above are authoritative, not this list.

Every `.nbt` this catalog needs, at `data/kithkyn/structure/<id>.nbt`.

| Set | What it buys | Structures |
| --- | --- | --- |
| **Minimum playable** | One level-1 building per phase 1 category, in a single family. A village that founds, feeds, houses, and defends itself. | **11** |
| Phase 1 | A village survives in any biome, at every level | 138 |
| Phase 2 | It thrives: processed food, iron, faith, trade | 32 |
| Phase 3 | It deepens: brewing, cloth, brick, glass, learning | 25 |
| Phase 4 | It fights: soldiers, walls, arrows, potions | 29 |
| | **Total** | **224** |

36 categories, 86 category-variant pairs, 224 structures. That total is the honest number and it
is large. Two things make it tractable:

- **The minimum playable set is 11 structures.** One level-1 building per phase 1 category, in one family.
  That alone gives a village that founds itself, feeds itself, houses its people, gathers wood and
  stone and ore, and posts a watch. Everything past it is variety and depth, not viability.
- **Vanilla cannot be copied, only referenced.** [Research on conversion](https://github.com/Quzzar/kithkyn/issues/53)
  found the EULA forbids redistributing Mojang `.nbt` files in our jar, modified or not. Loading
  them at runtime by `ResourceLocation` is legal and is the only route. Coverage is also thinner
  than this doc first claimed: 27 to 30 of the phase 1 files, all level 1, and 22 of our categories
  have no vanilla equivalent at all, including `storehouse`, `lumberjack`, `mine`, `hunting_lodge`,
  and `watchtower`.
- **Third-party sets** need the author's permission, asked for before anything is copied.

### Minimum playable (11 files)

```
  village_center_plains_1
  house_plains_1
  well_plains_1
  storehouse_plains_1
  farm_plains_1
  hunting_lodge_plains_1
  fishery_plains_1
  lumberjack_plains_1
  stoneworks_plains_1
  mine_plains_1
  watchtower_plains_1
```

### Phase 1 (81 files)

```
village_center_plains_1       village_center_plains_2       village_center_plains_3
village_center_taiga_1        village_center_taiga_2        village_center_taiga_3
village_center_snowy_1        village_center_snowy_2        village_center_snowy_3
village_center_desert_1       village_center_desert_2       village_center_desert_3
village_center_savanna_1      village_center_savanna_2      village_center_savanna_3
house_plains_1                house_plains_2                house_plains_3
house_taiga_1                 house_taiga_2                 house_taiga_3
house_snowy_1                 house_snowy_2                 house_snowy_3
house_desert_1                house_desert_2                house_desert_3
house_savanna_1               house_savanna_2               house_savanna_3
house_igloo_1                 house_igloo_2                 house_igloo_3
house_stilt_1                 house_stilt_2                 house_stilt_3
well_plains_1                 well_plains_2                 well_desert_1
well_desert_2                 storehouse_plains_1           storehouse_plains_2
storehouse_plains_3           farm_plains_1                 farm_plains_2
farm_plains_3                 farm_taiga_1                  farm_taiga_2
farm_taiga_3                  farm_snowy_1                  farm_snowy_2
farm_snowy_3                  farm_desert_1                 farm_desert_2
farm_desert_3                 farm_savanna_1                farm_savanna_2
farm_savanna_3                hunting_lodge_plains_1        hunting_lodge_plains_2
hunting_lodge_taiga_1         hunting_lodge_taiga_2
hunting_lodge_snowy_1         hunting_lodge_snowy_2
hunting_lodge_desert_1        hunting_lodge_desert_2
hunting_lodge_savanna_1       hunting_lodge_savanna_2         fishery_plains_1
fishery_plains_2              fishery_marsh_1               fishery_marsh_2
lumberjack_plains_1           lumberjack_plains_2           lumberjack_plains_3
lumberjack_taiga_1            lumberjack_taiga_2            lumberjack_taiga_3
stoneworks_plains_1           stoneworks_plains_2           stoneworks_plains_3
stoneworks_taiga_1            stoneworks_taiga_2            stoneworks_taiga_3
stoneworks_snowy_1            stoneworks_snowy_2            stoneworks_snowy_3
stoneworks_desert_1           stoneworks_desert_2           stoneworks_desert_3
stoneworks_savanna_1          stoneworks_savanna_2          stoneworks_savanna_3
mine_plains_1                 mine_plains_2                 mine_plains_3
mine_taiga_1                  mine_taiga_2                  mine_taiga_3
mine_snowy_1                  mine_snowy_2                  mine_snowy_3
mine_desert_1                 mine_desert_2                 mine_desert_3
mine_savanna_1                mine_savanna_2                mine_savanna_3
watchtower_plains_1           watchtower_plains_2           watchtower_plains_3
watchtower_taiga_1            watchtower_taiga_2            watchtower_taiga_3
watchtower_desert_1           watchtower_desert_2           watchtower_desert_3
```

### Phase 2 (38 files)

```
market_plains_1               market_plains_2               market_plains_3
market_desert_1               market_desert_2               market_desert_3
church_plains_1               church_plains_2               church_plains_3
church_taiga_1                church_taiga_2                church_taiga_3
church_desert_1               church_desert_2               church_desert_3
tannery_plains_1              tannery_plains_2              tannery_plains_3
tannery_taiga_1               tannery_taiga_2               tannery_taiga_3
tannery_snowy_1               tannery_snowy_2               tannery_snowy_3
tannery_desert_1              tannery_desert_2              tannery_desert_3
tannery_savanna_1             tannery_savanna_2             tannery_savanna_3
mushroom_cellar_plains_1      mushroom_cellar_plains_2      mill_windmill_1
mill_windmill_2               mill_watermill_1              mill_watermill_2
bakery_plains_1               bakery_plains_2               bakery_plains_3
butchery_plains_1             butchery_plains_2             blacksmith_plains_1
blacksmith_plains_2           blacksmith_plains_3           blacksmith_desert_1
blacksmith_desert_2           blacksmith_desert_3
```

### Phase 3 (25 files)

```
inn_plains_1                  inn_plains_2                  inn_plains_3
inn_taiga_1                   inn_taiga_2                   inn_taiga_3
library_plains_1              library_plains_2              graveyard_plains_1
graveyard_desert_1            apiary_plains_1               brewery_plains_1
brewery_plains_2              charcoal_burner_plains_1      charcoal_burner_plains_2
pottery_plains_1              pottery_plains_2              pottery_desert_1
pottery_desert_2              glassworks_desert_1           glassworks_desert_2
tannery_plains_1              tannery_plains_2              weaver_plains_1
weaver_plains_2
```

### Phase 4 (29 files)

```
fletcher_plains_1             fletcher_plains_2             armoury_plains_1
armoury_plains_2              alchemist_plains_1            alchemist_plains_2
barracks_plains_1             barracks_plains_2             barracks_plains_3
barracks_desert_1             barracks_desert_2             barracks_desert_3
training_yard_plains_1        training_yard_plains_2        wall_plains_1
wall_plains_2                 wall_plains_3                 wall_taiga_1
wall_taiga_2                  wall_taiga_3                  wall_desert_1
wall_desert_2                 wall_desert_3                 gatehouse_plains_1
gatehouse_plains_2            gatehouse_plains_3            gatehouse_desert_1
gatehouse_desert_2            gatehouse_desert_3
```

## Open questions

- **Does the level-2-only rule hold everywhere?** A village that loses its blacksmith to a raid
  must rebuild from `blacksmith_*_1`, which is a real setback. Probably correct, worth confirming.
- **Are the recipes at the right scale?** They were generated to sit in the same range as the
  existing datapack costs, which were themselves untuned. Nothing here has been played.
- **Do wall and gatehouse belong in this file at all**, or do they wait for site-selection and get
  their own treatment as linear structures.
