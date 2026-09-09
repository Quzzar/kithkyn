# Authoring a structure

How a building's `.nbt` gets made, now that no external source is derivable
([structure-sourcing.md](structure-sourcing.md)): we author our own, headlessly, with
commands. This is the loop the current `village_center_plains_1` was built with.

## The loop

1. **Pick empty ground** far from anything the simulation touches, and force-load it:
   `forceload add <x1> <z1> <x2> <z2>`. A superflat dev world makes the build plane
   obvious.
2. **Build with fill and setblock**, driving RCON from a script rather than typing. Work
   from one origin corner and offset every coordinate from it, so the whole build is a
   list of relative positions that can be re-run after a mistake.

   **Never quote a block id.** `setblock X Y Z minecraft:red_bed[facing=north,part=head]`
   is correct; wrapping it in quotes makes the command parser reject it. Any shell
   escaping belongs outside the command text, not inside it. This exact mistake silently
   emptied the first village center: every stateful block (beds, chest, campfire, bell,
   door, torches) failed while the plain walls and floor succeeded, so the build looked
   finished and was a shell.

   **A build script's own success count proves nothing.** Failed setblocks come back as
   ordinary command output, not as errors a naive filter catches. Verify the world, then
   verify the capture (step 7).
3. **Capture it**: `/kkdev village save-structure <from> <to> <name>`, where the two
   positions are opposite corners INCLUSIVE. The file lands in
   `<world>/generated/kithkyn/structures/<name>.nbt`. This command captures blocks only.
   An approved capture/export may additionally retain deliberately authored initial entities,
   such as livestock, a center golem, and decorative item frames. Do not copy arbitrary
   nearby mobs or dropped items into a production template.
4. **Ship it**: copy that file to `src/main/resources/data/kithkyn/structure/<name>.nbt`.
   The id in the building JSON's `structure` field, the file name, and the `.nbt` name are
   all the same string, so a definition and its structure can never drift apart.
5. **Write the definition** JSON beside it, with positions RELATIVE to the structure
   origin (the corner you passed as `from`). Beds, work stations, containers, personal containers (a home's own chest, the rule in
   [building-spec.md](building-spec.md)), and the gathering point are all origin offsets.
6. **Look at it**: `/kkdev village gallery <pos>` places every loaded definition on labelled
   plinths. `/reload` picks up JSON edits without a restart; a new `.nbt` needs a restart.
7. **Verify the palette**, always, before shipping. The decompressed NBT contains every
   block id as plain text, so a raw string search answers "did this block actually make
   it in" without an NBT library:

   ```
   python3 -c "import gzip,sys;t=gzip.open(sys.argv[1],'rb').read().decode('latin-1');
   print([b for b in ['red_bed','chest','campfire','bell'] if 'minecraft:'+b not in t])" FILE.nbt
   ```

   It prints what is MISSING. A structure whose definition promises four beds and whose
   palette has no bed is the failure this catches: the simulation reads bed coordinates
   from the JSON, so attractiveness cheerfully reports four free beds while a villager
   walks to bare floor.

## What to check before shipping one

- **Rotation.** The gallery places structures as loaded, but villages place them rotated.
  Anything position-sensitive (a gathering point, a work station) must be verified in a
  real village, not only in the gallery. The campfire POI landing correctly after rotation
  is the check that catches this.
- **Nothing that damages a villager where they stand.** The gathering point is beside the
  campfire, never on it, because everything that gathers walks there and idles there.
- **No block entities in the footprint of anything the site scorer must accept**: a chest
  or sign in the way makes a site impossible rather than clearable
  ([site-selection.md](site-selection.md)).

## Initial entities and decorations

Both instant placement and incremental construction use `BuildingEntities` after the final
block-shape pass. It transforms the template's entity positions and rewrites a hanging entity's
`TileX/Y/Z` to its world attachment cell before loading its NBT. Local coordinates alone are not
enough for item frames: vanilla validates those tile coordinates against the world position.
Facing is rotated once, and the displayed item and item rotation remain authored data. Verify
each frame remains attached after at least 100 world ticks in all four building rotations.

The approved Birch center starts with one ordinary iron golem. It is not pre-adopted: a guard
can later adopt it through the normal mechanic. The butchery starts with three cows and three
chickens, marked as farmed before joining the world; nearby wildlife is not marked as a side
effect of construction. The bakery retains its two decorative item frames.

Each building saves which initial entity entries were placed, plus a completion flag. A normal
save/reload, repeat completion, or upgrade retains those receipts and does not replenish dead
stock, a lost golem, or a removed decoration. Legacy standing buildings are treated as already
initialized, while a legacy unfinished fresh project may run its initial spawn phase. An old
upgrade is already initialized because its prior building supplied its inhabitants. Future
upgrades that introduce or relocate decorations require an explicit authored transition; they
must not silently reseed the whole original template population.

Receipts and entity data live in Minecraft's separate saved-data and entity-region files. They
are tested for ordinary saves and reloads, not claimed to form a crash-atomic transaction.
Failed entity placement remains pending and is logged rather than silently considered complete.

The disposable real-asset check is `BuildingPlacementVerification`, enabled only with the JVM
property `kithkyn.buildingPlacement.verify=true`. It builds the approved center, butchery and
bakery in all rotations through both placement paths, exercises mid-construction save/reload,
entity save/reload and idempotent upgrade receipts, then exits with a logged PASS or FAIL. Run
that same disposable world a second time to verify its saved manifest and actual entity-region
files survived a complete server shutdown/restart. Never enable it on the user's world; it
clears test plots near X/Z 2000 and stops its server afterward.

## Why headless

Building in a client with WorldEdit is faster for a human, but the command loop is
reproducible, reviewable in a script, and available to an agent session with no client
attached. A build that exists as a list of commands can be regenerated after a design
change; one that exists only as an `.nbt` cannot.

## The ground layer: leave what you are not building

A structure is seated with one layer on the ground's top block (layer 0, or layer `sink` when
the definition declares one), and every block recorded in that layer replaces the ground there,
air included. A template that records air around its floor at that layer therefore digs a
one-block pit ring into the grass wherever it is placed. Found 2026-09-01 in the bakeries,
taverns, fisheries and level-1 watchtowers, and taken out of all twenty files: the ground layer
should hold only what the building actually puts on or in the ground, and cells that are not the
building's are left absent so the world's own ground stays. The mine is the one deliberate
exception: its ground-layer air is the shaft mouth. Above the ground layer, air is wanted, it is
what clears grass, flowers and branches out of the footprint.

Air must stay inside the authored building envelope, never fill an empty capture border.
`BuildingFootprint` derives gameplay bounds from non-air/non-structure-void blocks across the
template palettes. Capture dimensions and local coordinates can remain unchanged: this preserves
amenity offsets while giving planning, ground preparation and claims zero capture padding.

A tree a worker is meant to cut is authored as a sapling, never as a grown tree (the lumberjack
lodge, 2026-09-02): the sapling sits on a block of dirt in the ground layer, since the world's
own top block may be sand or, on a slope, air, and the sapling is the work station. The first
tree then grows on Minecraft's schedule and its canopy is a natural one. A template canopy is
wrong either way: persistent leaves never decay, so the lodge's original tree left its whole
crown floating after the first felling (seen live); natural leaves placed one block per swing
by the builder can decay before their trunk lands. Decorative trees a structure carries, such
as the church's, stay persistent on purpose.

Seating is checked offline rather than by eye: a door's lower half should sit one layer above
the ground layer, beds and work stations likewise. Fisheries and level-1 watchtowers carried an
extra course below that and now declare `"sink": 1`; taverns and bakeries keep their floor a
step above the ground on purpose.

Negative `sink` raises the template: the approved Birch storehouse uses `-1` so its first step
is visible above ground. `entrance_facing` declares the horizontal direction an authored front
door faces; founding turns mine and storehouse doors inward using it. The default is north,
except storehouses default south to retain existing catalog behavior. A mine may also declare
`mine_entrance: {"facing": "east", "offset": [0, 0, 1]}`. The offset is from its unchanged
miner station, in template coordinates; facing is the direction the ramp descends. Both rotate
with the building, and excavation and navigation share that resulting frame. Without this field,
the shaft retains the original local-south, zero-offset convention.

## Deriving a level from a shipped structure

A level above 1 is rebuilt in the level-1's orientation. At runtime the new footprint may be
translated to any position that fully contains the old one
([building-spec.md](building-spec.md), "How upgrading works"), so an ordinary building can grow
in any horizontal world direction even though its authored local footprint still grows toward
+X or +Z. This re-seats the whole template within the old parcel; it does not preserve old cells
one for one. A structure with important runtime geometry outside its template needs an explicit
exception. A narrower upgrade may leave only dirt/grass landscaping and plants outside its
target; structural remnants are refused. Mines retain the exact origin because their shafts
are dug below the file. Where a
level is the level-1 developed rather than a different building, author it as
a script over the level-1 file instead of by hand. `tools/structure/mine-level-2.py` writes the
level-2 mine in all five families from the five level-1 files: the layout is written once, in the plains file's own
blockstates, and each family's blocks come from the block-for-block mapping between
`mine_plains_1` and `mine_<family>_1` at the same position, so no family is authored twice and a
change to a level-1 file is carried into its level 2 by re-running the script. It refuses a block
the level-1 palette lacks. Run it from `tools/structure/`:

```
python3 mine-level-2.py ../../src/main/resources/data/kithkyn/structure
```

then `validate.py` over the output, as for anything else. What no script checks is the shaft:
where the stations go is `MineStep`'s geometry (a five-wide ramp toward local +Z from each
mouth in these original families; `mine_entrance` can override that frame), and a second mouth
is placed so the two ramps never meet. The level-2 mine has been
validated and rendered offline only; the gallery and a real upgrade in a live village are the
checks still owed.
