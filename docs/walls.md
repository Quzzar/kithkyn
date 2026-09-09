# Walls

A wall is a route compiled into buildings-sized pieces. It is not one enormous
`Building`, because it has no fixed footprint, and it is no longer a stream of
single columns either. The village chooses and saves a perimeter once, compiles
that route into short sections, and lets builders claim those sections as
independent construction work.

Wooden walls combine authored and procedural geometry at the
`WallSegmentCatalog` boundary. Small NBT captures define the local silhouette
of straight runs, diagonal runs, terraces, watchtowers, and gatehouses. The
catalog rotates and joins those pieces along a procedural route, then emits the
same persistent `WallSection` and `WallBlockPlan` values used by builder AI and
saves. Stone currently keeps its fully procedural, walkable structure.

## What the village builds

There are two tiers on one permanent route:

- **Wood palisade:** a narrow three-course defensive shell decorated with
  authored uneven posts, beams, fences, lanterns, watchtowers, and gatehouses.
- **Stone wall:** a three-block-wide body with a walkable top course, parapets,
  stair transitions, corner towers, and gatehouse sections.

Stone is an in-place upgrade of wood. It reuses the saved ring and natural
ground profile. Village-owned palisade blocks may be replaced, while unrelated
solid construction is preserved.

## The route

The route wraps the saved claim bounds with eight blocks of breathing room.
Four cardinal runs are joined by broad 45-degree corners. The clipped corners
keep a wall from reading as one giant rectangle and provide real diagonal and
corner sockets for the segment catalog.

The ring is computed once when the first wall begins. It does not join
`claimGrid`, and it is not recomputed as the village grows. Newer neighborhoods
can therefore spill beyond an older defensive core.

Four gates prefer the cardinal midpoints. Before the plan is saved, each one
moves to the nearest dry socket on the same straight run when its full authored
footprint would touch water. A side with no dry socket gets no gate rather than
a gatehouse standing in a pond. Corner watchtowers use the same full-footprint
check and are omitted when their site is wet.

## Terrain: terraces, not terrain noise

The Great Wall is the visual reference for terrain behavior. The deck follows
the broad slope of the land, but it does not copy every grass-block bump.

At project creation, `WallTerraces` groups the captured natural ground into
four-block runs. Each run clears its highest ground and smooths toward its
neighbors while the local terrain allows it. A deck is never raised more than
five blocks above the ground in its own column. At a severe cliff the planner
therefore accepts a sharper terrace transition instead of turning the downhill
side into a giant palisade. The rule is circular, so the saved ring has no bad
seam where its last block meets its first.

Open water is a separate floor for the deck, not a replacement for terrain.
The deck keeps the wall's full normal height above the waterline, while the
saved natural-ground profile still extends its foundation down to the seabed.
This prevents a lake crossing from becoming a one-block barrier that can be
swum over.

The wall fills from the lowest neighboring ground sample to its deck. This
keeps the defensive shell closed at terrain steps. Trees and brush are not
sampled as terrain, and natural vegetation intersecting a planned wall cell is
replaced by that wall cell. Player blocks and earlier village construction are
still preserved.

Before the first construction cell, the project opens a three-block tree
line around the complete footprint through `SiteClearance`, also used by buildings,
using the same `TreeFelling` verdict as a
lumberjack. Only natural trees come down, never player-owned or village-owned
timber, and the logs drop into the world for villagers to collect. The pass is
route-shaped, so it does not clear the whole interior of the perimeter.

The saved `site_cleared` marker makes this an idempotent preparation phase. An older unfinished
wall defaults to uncleared and receives the same pass before workers resume its saved cursors.
Preparation waits for the route's clearance chunks to be resident, without loading them itself.
The final completion pass also checks for remaining or regrown foliage.

After the trunks are felled, the wall clears tagged vegetation through its own
columns and one horizontal block on both the village and wilderness sides. The
clearance follows gates and towers as well as ordinary runs, removing leaves
above the palisade along with adjacent saplings, brush, vines, and other
foliage down to real ground. Player-owned and village-owned construction stays
protected, as do authored plants in neighboring registered village buildings. Unloaded columns
are skipped rather than synchronously loaded. This leaves no canopy over the wall and no vegetation close enough
to give mobs a step onto it.

Large authored pieces remain coherent instead of shearing with every terrain
sample. Their vertical posts extend to the live natural ground in each post's
exact column. For rigid gatehouses and corner towers, only post columns touching
the template's local Y=0 are foundation legs. Suspended masonry roof beams stay
at their authored height; extending those down would seal the gate passage.
A watchtower's low fence, trapdoor, and ladder shaft uses the same
foundation rule, so a raised terrace or off-route downhill leg cannot leave the
tower or its access hanging above the ground. Ordinary authored runs keep no
more than two courses of decorative silhouette above their local deck, which
preserves uneven posts without allowing one terrain step to become a tall mast.

Structural foundations, including Birch cobblestone and mossy cobblestone posts,
embed exposed soil during placement. Built sections are checked when wall work resumes
and after each section finishes, so a distant unfinished water section cannot delay
the cleanup. When a natural dirt course has a side exposed to
air, the foundation replaces that one course. Buried dirt and player-owned or
village-owned ground remain untouched. This makes an edge wall read as sunk
into the bank instead of balanced on its visible dirt face.
Repeated checks never descend below the original foundation's single soil course.
Lower run caps and torches that directly meet the underside of an overlapping rigid
corner are solidified into the join; the corner's own decorations remain authored.

## Segment projects

`WallSegmentCatalog` compiles the ring into sections of at most seven route
blocks and classifies them as:

- straight,
- diagonal,
- terrace,
- corner tower,
- gatehouse.

Each section owns an ordered list of construction cells and a saved cursor.
`WallProject` leases different incomplete sections to different builders. A
lease is runtime-only and expires if its builder disappears, while the cursor
is persistent. An unreachable section waits briefly and releases its builder;
all other sections remain available.

Workers choose dry, supported positions using `WorkerFooting`, shared with redevelopment solely
for actual-body collision and footing safety. Wall access retains its own policy: up to twelve
blocks horizontally and twelve vertically from the construction cell, including side and outer
approaches to a gatehouse. Selection checks at most eight new paths per scan and continues its
candidate list on later scans. Routes must actually reach the selected endpoint. There is no
one-block navigation slack for this foothold: the shared work loop delivers an exact path too,
so the builder does not stop just short of the chosen construction radius. There is no
fallback to an unreachable riverbed; exhausted searches defer that section. Acting still requires
arrival at safe footing, and never places a cell through a living entity.

Structural cells are ordered before ladders, trapdoors, campfires, and
lanterns. In particular, the beam above a hanging lantern is placed first, so
the lantern cannot pop off during construction. Ordinary runs retain one
lantern on alternating sections. Feature-overlap cleanup discards any lantern
whose support was removed. Authored gatehouses and towers keep the outward roof
lights and omit the visually busy pair facing into the village.

This makes a wall behave like a continued structure. One swing places one
construction cell. A builder stays with a short section, several builders can
raise visibly different parts of the perimeter, and a restart resumes each
piece at its exact next cell.

## Occupancy and materials

Construction cells have semantic roles:

- **Barrier:** any collidable block satisfies the cell. Air, fluid, and other
  passable states are filled. Natural logs, leaves, and other clearable
  vegetation are deliberately cut through rather than accepted as part of the
  defensive shell.
- **Exact:** the catalog prefers its stair, parapet, or decorative state. An unrelated
  collidable block is still preserved, because closing the defensive shell is
  more important than forcing a palette over player or terrain construction.
- **Foundation:** an exact authored state that repeats down to the live terrain
  in its own column, used by off-route posts and watchtower access shafts.
- **Clearance:** an internal planning cell removes lower-priority generated
  geometry from an authored tower or gatehouse's empty volume. It is discarded
  before construction, so builders never place an "air block."

The deliberate stone upgrade is the exception. Village-owned oak, spruce, or
acacia wall blocks do not satisfy a stone plan and can be replaced.

Walls remain priced at one material item per ten placed construction cells.
The affordability check uses the same compiled plan and occupancy policy as the
builder, so existing solid cells cost nothing and the estimate matches the work.
Credit left from an item carries across cells in that builder's pack.

## Gatehouses and defense

The catalog reserves seventeen route blocks for each gatehouse. A wooden
gatehouse is an open passage with no door; its authored deck, ladder, correctly
supported standing and hanging lanterns, beams, and flanking posts rotate onto
any cardinal run. Authored feature volumes replace the ordinary palisade rather
than being layered through it. One watchtower owns each clipped corner, rather
than placing overlapping towers at both ends of the same chamfer. Stone keeps a
three-high open center passage and compact procedural towers until it receives
its own art pass.

The normal barrier invariant remains simple: after a wall cell is decided,
that cell must be solid and collidable. Open gate passage cells and the authored
interiors of towers are intentional exceptions.

A completed wall also derives guard workstations from that exact compiled
geometry. These are not separate buildings and do not add wall beds. Each gate
has two ground-level sword posts and one elevated crossbow post; each non-gate
watchtower has one elevated crossbow post. Elevated stations are selected from
real walkway or roof support cells with two blocks of headroom, so wood, stone,
and terrain-following walls share the same job model.

Birch masonry and top slabs count as platform support, using the Birch feature's
own footprint. Ground sentries stand on the gate centerline, with the second
post farther inside. Their height is resolved within two blocks of the saved
surface, including constructed floors, never by searching down to unowned terrain.
Posts use live safe footing, exact reachable routes, and an eye-level outward
look. An unreachable station is retried at most once every two seconds.
Exact work destinations can retry a failed 48-block route with a 96-block
walking horizon, retaining the same bounded search budget. This accounts for
detours to the ladder and the climb without accepting an endpoint underneath
the intended platform.

Open wall posts are registered in four village-wide staffing tiers:

1. one stone-sword guard at every gate,
2. one crossbow guard at every non-gate watchtower,
3. one crossbow guard above every gate,
4. the second stone-sword guard at every gate.

All four are ordinary `GUARD` occupations with a wall-post specialization. This
keeps guard aptitude, housing, rations, shields, armor, threat selection, and
equipment upgrades in one system. Sword guards may leave the base to fight and
return afterward. Crossbow guards hold their elevated station instead of
pathing off the wall for a blocked shot. Better swords, crossbows, armor,
shields, and special arrows come from the same physical village inventory rules
as the existing guard and hunter equipment. A wall under construction or being
upgraded publishes no posts; the completed geometry registers the new set.

Guard duty always includes defending against hostiles, even for gentle residents.
Ranged guards search vertically as well as horizontally, with a nominal 48-block
radius adjusted by existing eyesight/wisdom variation. A visible ranged target
does not need a walking route from the platform. Sight obstruction and friendly
fire still prevent shots. Low-health recovery remains available.

Ranged aim accounts for the projectile's flight time, drag, gravity and the target's
current motion. The nominal watch range remains 48 blocks. Combat bows use full-draw
arrow speed (3 blocks per tick); hunting retains its slower 1.6 speed, and crossbows
retain their existing 6 speed. Combat bow base damage is reduced in proportion to
the speed increase to preserve its damage at close range. Both weapons aim at the
target's body with modest, difficulty-adjusted spread. This replaces the skeleton-style fixed upward correction,
which sent fast crossbow bolts over targets while slow bow arrows fell short. Special
bolts, multishot, loaded ammunition and weapon wear still use the actual held weapon.
Firework rockets use straight flight rather than arrow gravity compensation.

Ladder navigation is shared with all workers. Before descending, a resident's
whole body must clear the landing edge before horizontal motion stops. This
keeps workers from becoming stranded at the ladder top. Routes descend high
rungs vertically rather than taking a sideways drop that can catch on a rail.

Wall placement computes connection arms against existing neighbors. Maintenance
also reconnects old village-owned masonry/fences, preserving player-owned edits
and the existing block material instead of repainting the gate.

Ground mobs are stopped by the continuous shell outside its open gates. The wooden
silhouette includes authored overhangs, while spider-proof behavior remains a
separate gameplay test.

## Authoring contract

The five canonical wooden templates live under
`data/kithkyn/structure/wall/wood/`. They were captured from the in-world
wall lab and are loaded as semantic cells instead of stamped directly into the
world. One oak-authored geometry therefore resolves through the village style
as oak for plains, spruce for taiga and snowy villages, or acacia for desert and
savanna villages.

Each authored template needs:

- an entry and exit socket on the route centerline,
- a declared section kind and supported tier,
- a deck socket elevation,
- barrier cells that may accept any existing collision,
- exact detail cells for stairs, rails, and decoration,
- foundation cells that can extend down to the captured terrain,
- clearance cells for the walkable deck and gate passage.

Straight, diagonal, and terrace captures contribute one vertical slice per route
column and therefore follow the terrain-owned sockets. Corner towers and
gatehouses are rigid authored interiors with foundation posts that grow down to
their exact live terrain columns. New palette blocks must map to a semantic
`WallBlockPlan.Piece`; unknown decorative blocks are ignored instead of leaking
a fixed biome palette into every village.

The existing structure capture loop in [structure-authoring.md](structure-authoring.md)
is used to revise the NBT files. The wall lab remains the visual authoring
gallery. Replacing a captured piece changes the catalog without another builder
or save-system rewrite. A finished project's persisted completion marker survives
changed section signatures, so an artwork update cannot restart its construction.
Incomplete projects still require matching signatures to restore their cursors.

Birch uses the separate approved masonry captures in
`data/kithkyn/structure/wall/birch_forest/`. The September 8 showcase revision adds
44 user-authored cobblestone/mossy-cobblestone swaps across all five pieces,
without changing their bounds or importing the display platform. These edits are
reproducible through `tools/structure/birch-walls-20260908.json` and the Birch exporter.
Changing the catalog does not automatically repaint or clear an existing world;
existing-wall repairs must compare old/new plans and protect player-owned blocks.

The September 9 gatehouse revision adds the user's four white banner placeholders, two on
each face. Its NBT crop grows one block on each face and the gate anchor stays centered;
structural clearance and all 159 preceding masonry/detail cells remain unchanged. Directional
banner pieces rotate with the gate and are placed after their supports. They use the shared
[village identity](village-identity.md#gatehouse-flags) application, not a separate flag design.
New piece values are appended because saved section signatures include their enum ordinals.
The opt-in `kithkyn.gateBanner.verify=true` disposable-server check exercises actual patterned
block entities, white-primary villages, instant and incremental construction, save/rebind,
attachment survival, and player-edit protection.

## Planning and developer preview

Walls are safety projects. An established village starts wood after sufficient
growth or danger, then later upgrades that same route to stone. While incomplete,
the wall holds normal village project selection just as a building project does.

`/kkdev village wall <wood|stone>` compiles the same project and places all of
its cells immediately. It is the fast geometry check for the route, terraces,
walkway, towers, and gatehouses. Ordinary builders use that identical plan one
cell at a time.

`/kkdev village wall-area <wood|stone> <from> <to> [style]` runs the planner
around any two opposite x/z corners without changing a village's saved wall.
Both spans must be 16 to 128 blocks. Omitting `style` derives the wood family
from the biome at the rectangle center. This is the live integration check for
flat ground, rolling terrain, slopes, and cliffs.

## Current limits

- Wooden wall art is authored. Stone wall art remains procedural.
- The stone walkway is structurally continuous but has no internal stairway
  from the village ground up to each tower yet.
- Spider-proof behavior still needs a focused gameplay test.
- Existing worlds with an in-progress legacy column wall should be reset. A
  completed legacy wall remains complete, but partially completed legacy
  cursor positions cannot map exactly onto the new multi-cell sections.
