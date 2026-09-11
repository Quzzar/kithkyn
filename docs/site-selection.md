# Site selection and ground clearing

[village-tiers.md](village-tiers.md) names space as a first-class build constraint and
defers the design to its own doc. This is that doc. It covers how a site is found, how
local placement decisions form streets and blocks, whether villagers may reshape the
ground to make a site, and what that costs at runtime.

Implementation state: **the prepare phase is built** as of
[#69](https://github.com/Quzzar/kithkyn/issues/69). `SitePreparation.planWork` returns
the actual positions to break and to fill, a project carries that work in its own persisted
queues, and the BUILDER performs it as the first phase of construction: one block per swing,
cleared blocks going into village storage rather than onto the ground, fill paid for out of
the village's own dirt. A site that needs work enters `PREPARING` and places nothing until
the ground is ready. When fill runs out the village emits the ordinary shortage event and
the builder says so in their own log, rather than spinning.

**Candidates are snapped to the real surface before scoring**, which is mitigation 1 below
and was for a long time simply absent. The snap reads the footprint's most common ground
height, not one column's (2026-09-01): seated at its origin column, a house went into the
world a block low whenever that column was a dip in an otherwise level plain, and the prepare
phase would then have dug the whole footprint down to meet it; now the odd columns are the
ones levelled. Sites used to be scored at the village centre's
elevation plus a blind vertical offset, and the levelling budget then rejected anything that
was not within about a block of the actual terrain — so in a live world every candidate came
back impossible and no village could build anything at all. Snapping also removes the
nine-elevation loop, so a search reads roughly a ninth of the blocks it used to, and
candidates in unloaded chunks are skipped before any scan rather than being scored as
impossible one at a time.

The planner takes ground that needs work. It first tries edge-aligned frontage slots beside
completed buildings, preferring two clear blocks between footprints and allowing one block
where space is tight. Among legal sites it prefers the authored front toward the town center,
then a corner or row that continues existing frontage, frontage beside an already worn path,
preparation cost and distance. A frontage that cannot meet both preferences also lets the
nearest-first terrain sweep look for a better nearby fit. Heightmap-first screening is built, and a refused search leaves the
village knowing where its room ran out. Still unbuilt: the resumable budgeted search and the
site cache.

**Where a village looks.** `LocationValidator` starts from buildings, then falls back to a
nearest-first sweep. The village once threw a handful of random candidates at a square ring
and took the first free one, which put buildings a long way from the fire: open ground far
out is free, while near ground is often claimed or wants a little levelling.

The planner now enumerates the beginnings, centres and ends of each completed footprint's
four edges. It puts the candidate one or two clear blocks away in every offered rotation,
then ranks the legal fits by spacing, inward front and existing frontage. Repeating this local
relationship produces rows, narrow streets and small courtyards without forcing the village
onto a global grid. Later growth starts by asking how it relates to the town already standing,
not only how far it is from the fire.

When none of those exact slots fits, the sweep walks a grid outward from the fire, nearest
first, and takes the best ground in a short distance band. It steps over ground too steep to
level and slots too tight to hold the footprint, so the fallback molds itself to the terrain.
The sweep reaches 32 blocks past the ring (never beyond 96); the ring's outer edge grows with
the village, one search radius plus another for every four buildings, and never shrinks to
nothing. Candidates stand off the centre rather than starting on it.

Every candidate is tried in each rotation the caller offers (the planner offers all four),
and the facing that fits the slot is the one kept: a long building turns to fit a gap its
other facing could not. When that direction fits, the actual authored entrance side faces
the town center; both closest cardinal directions are equally inward on a diagonal.
Only ties after layout, preparation cost and distance are random. Two clear blocks are the
preferred spacing, with a hard minimum of one clear block between a new footprint and every
claim. These are preferences among safe sites, never permission to exceed the terrain budget,
erase a path or overlap a building. A clear gap of `MIN_GAP` blocks is held, so
lanes stay walkable and the cluster reads as planned rather than piled; a candidate whose
footprint, grown by that gap, touches a claim is passed over. A worn dirt path is public
space: a building may line it and gains a placement preference for doing so, but may not
cover it.

Before any candidate is scored, the heights of the whole search square are read once from the
chunk heightmaps (`MOTION_BLOCKING_NO_LEAVES`, the real ground under a canopy) into a grid,
so every candidate's flatness is arithmetic: its plane is the height most of its columns
share. A template raised with negative `sink` lifts that plane to the modal height immediately
outside its rotated public front when the approach stands higher; this keeps its exposed lowest
step above the bank on uneven ground. Ground with more than one column in eight past the per-column budget, or averaging
past the levelling budget across the rest, is refused without a block scan. A few tall columns
are let through because a tree reads as a tall column and is cleared, not levelled; one outlier
is always allowed through even on a small footprint. The exact scan also uses that allowance for
a compact low corner, but not for high ground or a broad depression. Only
survivors get the volume scan. Frontage scans retain their cap of 160 candidates, sharing
that budget across two-block/inward, two-block/turned, one-block/inward and one-block/turned
tiers. Empty tiers return their share to available candidates. This keeps terrain-rejected
preferred sites from consuming every opportunity to try a tight or turned fallback.
In the fallback, an existing usable frontage or the first free/preparable sweep candidate
settles a band and the sweep reads 8 blocks further out. Spacing and inward front come first,
then relationship to claimed edges and paths, preparation cost and distance. Taking the
cheapest ground in the whole reach (2026-09-02)
put Wildflower Downs' lumberjack 90 blocks from its fire, 78 blocks of work there against 211
within 50; a village that sprawls has a wall ring it cannot afford and ground it cannot finish
grading. The grid's stride is 2, fine enough to pack the ring in snugly, and a footprint is
wider than that, so a slot with room to spare cannot fall between grid points; a slot that
fits only exactly can, and a refusal means "no site with a little room to spare". Unloaded
chunks read as no ground and are never loaded by the search: a refusal records how far out
the village actually read ground, and with nobody near, that is roughly the founding
forceload rather than the full sweep.

A village only searches while its ground is loaded. Nothing can be sited in a chunk nobody
is near, and asking the brain to choose would spend a model call on an answer no site search
could act on, so an unwatched village waits rather than planning.

Every search leaves one debug line saying what it saw: how many candidates were scored, how
many sat on claimed ground, how many covered a path, and how many were in unloaded chunks.
The selected site's spacing, inward front, claimed frontage, adjacent sides, path frontage and preparation cost are
logged too. A search that skips every
candidate before scoring is otherwise indistinguishable from one where every site was
genuinely bad, and that ambiguity hid the zero-radius bug above for as long as it existed.

**What the village knows when it finds nothing.** A refused search is remembered in
`SiteMemory` for 900 village seconds, and dropped the moment anything is built: the
footprint that found no ground, which rules out every footprint at least as large in both
dimensions; how far out the search read ground; and the nearest thing to a site it saw,
meaning the refused ground with the least earth standing off level, with the fact that ruled
it out ("22 of its 99 columns stand more than 3 blocks off level"). Water, claimed ground and
someone's chest are never a near miss, since no levelling makes them a site.
`Village.describeRoom` turns that into one sentence of facts, and both briefings carry it:
the planner's situation, so the brain knows why a building is missing from its options
instead of choosing around a gap, and every villager's chat, so a builder asked why nothing
is going up can say that nothing 9 by 11 or larger has found ground within about 40 blocks
of the fire and that the nearest thing to a site was 18 blocks east. The sentence ends with
the rule, that villagers level ground only lightly and never reshape a hill, and says
nothing about what to do: building smaller, waiting, or a player levelling the slope by hand
are the reader's calls. `/kkdev village place` with no position records a refusal the same way
a real project does, which is the on-demand way to see the sentence; giving it a position
forces the building onto that exact spot instead, bypassing the search, so authoring a
schematic in-world is never blocked by the room the planner would refuse. That division of
labour is deliberate. The village holds the
surface-not-shape line; a player is free to break it, and a slope they level is found on the
next search once the refusal expires. The refusal is logged at INFO with the same sentence.

Water covering the proposed build plane is never a land-building site. The motion-blocking
heightmap includes a swamp's water surface, so the exact scan must reject fluid at that plane
instead of looking through it and pricing the mud below. Fluid strictly below a dry build plane
can still be filled as a local depression within the ordinary terrain budget. This keeps a
building beside or above a small filled hollow possible without letting its foundation float on
one block of water or silently accepting a deeper pool.

`SitePreparation.score` prices any candidate
footprint in blocks moved (clear, cut, fill, or impossible) using the
`kithkyn:clearable` whitelist tag, the per-column and average levelling budgets below,
the block-entity and claim protections, and the never-scan-unloaded rule. The planner's
site search accepts free and preparable sites within those budgets; rejected candidates log
their price at debug, and
`/kkdev village score-site <pos> <sizeX> <sizeZ>` prints any site's bill, and
`/kkdev village start-project <building> <pos>` begins a real project on ground you choose,
which is the direct way to watch preparation run. The resumable budgeted search and the site
cache are later slices.

## The problem this replaced

`LocationValidator.isValidLocation` answered a yes-or-no question: is this block position
buildable. `isValidPlacement` then sampled a footprint's perimeter plus five interior points
and rejected the site if any sample failed. **Both methods have since been deleted** in
favour of the cost model below; this section records why.

That model cannot express the most common real case. Two saplings and a dirt hummock in an
otherwise perfect meadow fail the check exactly as hard as a ravine does. The village then
reports "we don't have enough room" while standing in a field.

## Site validity is a cost, not a gate

**A site is not valid or invalid. It has a preparation cost, measured in blocks moved.**

That one change collapses space and resources into a single comparable number, which is what
the brain needs to choose between options:

| Site | Preparation | Reads as |
| --- | --- | --- |
| Flat meadow | 0 blocks | free |
| Meadow with two trees | ~40 blocks removed, yields ~30 logs | cheap, and it pays for itself |
| Gentle slope | ~120 blocks cut, ~80 filled | expensive, needs dirt from storage |
| Hillside, ravine edge, deep water | beyond budget | not a site |

Preparation cost is its own quantity, counted in blocks moved, and it is deliberately not
folded into the building's cost. A building's cost is a recipe of items
([building-spec.md](building-spec.md)); a site's preparation is the separate question of
whether the ground can take the building's dimensions and what it takes to make it. Fill
consumes real material from storage, so a site that needs levelling has a bill; clearing
mostly does not.

Clearing yields go into village containers. A forested site is not purely a cost: clearing it
is a lumber harvest that happens to also make room, which is exactly the kind of thing the
village should notice and the journal should mention.

## How far a village may reshape the ground

Three tiers of preparation, and the third one does not exist.

| Tier | What it does | Allowed |
| --- | --- | --- |
| **0. Clear** | Remove vegetation and non-solid cover: grass, flowers, snow layers, leaves, trees, loose surface litter | Always |
| **1. Level** | Cut solid terrain above the build plane and fill below it, within a budget | Within budget |
| **2. Excavate** | Remove a hill, fill a ravine, drain a lake | **Never** |

The budget for tier 1 is a per-column average, not a total: roughly 1.5 blocks off the build
plane averaged across the footprint, with an ordinary per-column maximum of 3. One compact
low depression may reach 6 blocks below the plane: at most one such column per eight footprint
columns (with one allowed even on a smaller footprint), and no connected patch more than three
blocks across either axis. That makes a dipped corner fillable without letting half a footprint,
a trench, high ground, or a ravine become a site. Fill is placed from the bottom up and consumes
dirt or the local ground material from village storage, so levelling a slope is a real expense
the brain can weigh against building somewhere else.

The rule this encodes:

> **A village changes the surface of the land, never its shape.**

**That rule governs the surface, and only the surface** (decided on
[#54](https://github.com/Quzzar/kithkyn/issues/54)). Underground is the miner's
business: a mine may sink shafts and drive tunnels as deep and as far as it likes, because
nobody is looking at the skyline from down there. One sentence covers both halves — do not
reshape what people see, dig what you like beneath it.

That line is the entire aesthetic difference between a settlement that grew into its terrain
and a player's flat dirt platform. It is also what keeps a village from eventually turning
its valley into a plateau over a hundred hours of unattended simulation.

### What may never be broken

Site preparation removes blocks. Getting this wrong once, on a player's house, poisons the
whole mod. The rule is a whitelist, not a blacklist:

- **Only blocks matching the `kithkyn:clearable` block tag may be removed.** Natural
  terrain and vegetation. Anything not in the tag makes the site invalid rather than
  becoming a target.
- Never break a block entity, ever. Not chests, not spawners, not signs.
- Never break inside another village's claim (`Village.hasClaimed` answers this). **Today
  the scorer consults only the building village's own claim**, so a neighbouring village's
  ground is not yet protected: a real gap, not a design choice.
- Never touch a vanilla or modded structure's bounds.
- A config switch turns terrain modification off entirely, leaving tier 0 clearing only.
  (Proposed; no such config key exists yet.)

The whitelist is datapack content, so a pack author decides what "natural" means in a
modded world without touching Java.

## Who does the clearing

**The BUILDER, as the first phase of construction. Not a new occupation.**

Clearing is bursty: it happens for a few minutes before a build and then not at all. Under
the campfire model ([population-and-labor.md](population-and-labor.md)) an occupation that
sits idle most of the time is a wasted job slot and a wasted bed, and the idle cap makes
worker slots genuinely scarce. Site prep is part of building, so it belongs to the builder.

Construction becomes three phases instead of one:

1. **Prepare**: walk the footprint, break tier 0 and tier 1 blocks, deposit yields in village
   storage, place fill.
2. **Build**: the existing `StructureInProgress` block-by-block placement.
3. **Finish**: register beds, work stations, and containers (already `processNewBuilding`).

A `NoResourceBookkeepingEvent` fires when fill is short, exactly as it does for build
materials, so a village that cannot afford to level a site complains in the way it already
complains about everything else.

## Natural founding searches nearby land

Natural villages do not depend on vanilla village-biome eligibility. During exploration,
each seeded 34-chunk region offers 25 candidate anchors, nearest-first on a 32-block grid
within 64 blocks per axis of its original point. The search shares its cursor across nearby
players and allows at most two exact layout probes per second across the level, inspecting
at most 32 nearby regions in that pass. Unloaded sites are deferred, never generated for a
probe. Tested unsuitable sites are skipped for the rest of the session; failed delayed
commits resume the remaining search. The grid and original seed salt remain unchanged.

Manual and natural founding share the same prepared plan: the exact center, its rotation,
mine, storehouse and ground plane. All three footprints receive the existing terrain-cost
and ownership checks, including the center. Only a viable natural plan requests a village
name. That request reserves a 272-block horizontal separation from other pending names and
standing villages. Immediately before placement, separation, loaded chunks, protected blocks
and the exact prepared footprints are checked again. The final village identity is applied
without rerolling the geometry. A failed preflight or stale plan creates no claims and
changes no blocks. Manual founding may load its requested camp area as before; natural
preflight and delayed commit do not.

The search deliberately reuses footprint scoring rather than adding a second slope policy.
The [upstream comparison](research/forest-village-worldgen.md) informed the separation of
biome eligibility, spacing and terrain checks; nearby retries are our own adaptation for a
living village's complete founding layout.

## What stands over the roof

Founding measures each building's ground footprint from its placed origin plus its rotated
`BuildingFootprint` bounds, not a rounded half-width around its center. These bounds enclose
authored non-air blocks across all palettes, including decorations, but exclude empty capture
borders. Placement, claims, site searches and upgrades use the same envelope with zero capture
padding; the one- or two-block walking lane is separate. Odd and even dimensions keep
their exact extent, including negative offsets after rotation. Sunken templates still prepare
the chosen surface plane; their underground blocks and explicit air are placed afterward.
`FoundingLayout` sends every starting companion through `LocationValidator`, the same
search used by normal construction. Its read-only placement context starts with the center
and adds each accepted footprint as both a reserved claim and a frontage anchor. Two-block
lanes and inward fronts remain preferred, with tight lanes and other rotations available.
There is no separate centered-pair solver, fixed side assignment, or shared ground height.
The center definition may request additional starting homes using `starting_buildings`.

Every starting building must fit before any terrain or ownership is changed. The final
commit surveys all sites again and applies the same `SitePreparation` queues as builders,
at each site's own unsunk ground elevation. Protected edits to a companion or home invalidate
the entire plan. Only the chosen footprints are prepared; no shared rectangle is flattened.
In-place upgrades retain the source building's rotation and mine-shaft alignment.

Ground preparation stops at its headroom, but completed buildings also use the wall's natural
vegetation clearance. `SiteClearance` expands the actual footprint by three horizontal blocks
for whole-tree felling, then by one block for a top-down foliage sweep. Overhead remnants and
branches are included; leaves above the roof and brush beside the building are cleared too.
These are vegetation radii, not terrain padding or claims: no soil is cut or filled by this pass.

`Village.addBuilding` and `replaceBuilding` are the common completion hooks for command/natural
founding, `/kkdev village place`, ordinary builder completion, redevelopment and upgrades.
`WallRaiser` uses the same clearance engine with its irregular route footprint, so it never
clears the whole perimeter interior. Both skip unloaded columns and retain the lumberjack's
natural-canopy and per-log ownership guards ([block-ownership.md](block-ownership.md)).
Player/village timber and block entities are protected. Connected branches may extend beyond
the three-block seed radius; disconnected trees beyond it remain.

The foliage sweep stops at terrain or protected construction. Authored plants in the new and
neighboring registered buildings are protected at their rotated template positions. In
particular, flowers, crops and the lumberjack sapling stay unowned for growth/harvesting, but
are not mistaken for brush during this one cleanup pass. Authored tall grass is already
ownership-protected. Village clearing yields go into storage, with overflow at the tree's
position; wall yields drop for villagers to collect. Raw authoring-gallery stamps do not
register as village buildings and are not retroactively swept.

## What it costs at runtime

The founding gap is also an access lane, not extra terrain-clearing padding. After
placement, `GradingSurvey` identifies narrow gaps (up to three columns) between overlapping
footprint edges and small doorway approaches. Those movable columns receive the gentler path
grade before a path exists. Saved sink offsets and exact rotated footprints supply floor anchors.
The ordinary builder performs the cut/fill physically in an early, filtered pass, then returns to
wall or building duties. Broader landscape grading remains lower priority; it does not flatten
the village's entire apron. Doorless structures use a supported opening on their authored front.
See [worker-loops.md](worker-loops.md) for scheduling and protection rules.

The expensive part is **finding** sites, not clearing them. Clearing is a villager breaking a
few hundred blocks over several minutes, which is nothing. Scoring candidate sites is a
volume scan, and that is where a naive implementation eats a tick.

Rough shape of the cost, to be measured rather than trusted:

- A 16x16 footprint scored across an 8-block height band is ~2000 block reads. On a loaded
  chunk a read is a few array lookups, so a single site is a fraction of a millisecond.
- The danger is candidate count. Scoring 200 candidates the naive way is ~400,000 reads,
  which is a whole tick, multiplied by every village in the world.

Four mitigations, in order of how much they buy:

1. **Heightmap first, blocks second.** A footprint's flatness comes from the chunk heightmap
   in ~256 reads with no volume scan. Reject most candidates on height variance alone, then
   volume-scan only the survivors. This is the difference between viable and not. Built:
   one grid read per search, then arithmetic per candidate.
2. **Budget reads per tick.** Site search becomes a resumable job with a read budget per
   village per tick, not a synchronous loop inside the planner. `Village` already
   phase-staggers attractiveness across villages, so the pattern exists to copy.
3. **Cache scored sites.** Terrain barely changes. Keep a small ring of the best known sites
   per village and let the planner pick from it, rescoring lazily.
4. **Never scan unloaded chunks.** Already the rule on the arrival path; it applies here for
   the same reason.

## How the brain sees a site

Sites reach the LLM as description, not coordinates, the same way every other option does:

- *"a clear meadow east of the well"*
- *"a wooded rise north, three trees to fell"*
- *"a slope by the river, would need levelling"*

The planner has already filtered to sites within budget, so the model is choosing among legal
moves and cannot ask for the ravine. When there is no site, the brain hears that too, as the
room sentence above rather than as a silent gap in its options.

## Open questions

- **Does the preparation yield count toward the building's cost**, or just land in storage as
  ordinary income? Counting it makes forested sites feel cleverer; not counting it is simpler.
- **How is player-placed detected**, if at all. The `kithkyn:clearable` tag is a good
  approximation and needs no bookkeeping, but a player's dirt hut is made of clearable blocks.
- **Should tier 1 levelling be visible over time**, with the builder actually digging, or
  applied in the same block-by-block pass the structure already uses.
