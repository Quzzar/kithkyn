# Approved Birch Forest village

The 2026-09-07 approved set, with the revisions below, is the playable `birch_forest` catalog. It uses the existing
village, building, labor, construction, and identity systems. It is not a separate simulation.
These decisions supersede the older candidate-gallery choices and generic counts in
`building-spec.md` for this family.

## Catalog

| Building | Available tiers | Birch-specific contents |
| --- | --- | --- |
| Village center | 1 | Approved center with the custom basement, four beds, shared personal chest, three builder stations, guard station, and one initially unadopted golem |
| House | 1, 2 | One bed; two beds. No four-bed or tier-3 home |
| Couple's cottage | 1 | Adjacent double-bed arrangement, both primary color |
| Well | 1 | Narrow tall well |
| Storehouse | 1 | Approved former tier-2 storehouse, quartermaster, communal storage |
| Watchtower | 1, 2 | One and two fixed crossbow/sword sentries, with one and two beds |
| Farm | 1, 2 | Single field and merged double field, one composter each, barrels |
| Lumberjack | 1 | Birch sapling chopping station and communal chest |
| Stoneworks | 1 | Stonecutter station and communal chest |
| Mine | 1 | Edited weaponsmith shell, mine mouth, communal chest and village banner |
| Hunting lodge | 1 | Hunter station, secondary-color bed and personal chest |
| Fishery | 1 | Fisher station and communal storage |
| Bakery | 1 | BAKER station, one primary-color bed, personal chest and separate communal storage |
| Tavern | 1 | Edited Dungeons & Taverns Birch tavern, INNKEEPER station, back-room bed and private ceiling barrel, separate communal chest |
| Blacksmith | 1 | Crafting station, furnaces and communal chest |
| Butchery | 1 | Butcher/herder stations, secondary-color bed, personal chest, communal barrel, three cows and three chickens |
| Market | 1, 2, 3 | Approved stalls and merchant stations |
| Church | 1 | Approved cleric building |

This is 23 concrete building templates plus five authored wall pieces. Missing tiers and
categories are intentional: no catalog borrows another family's building, and Birch is the
only catalog bundled in the jar.
Costs retain the existing per-category/per-tier recipes. Higher tiers elsewhere are unaffected.
Amenities come from the actual selected definition, not a global bed or chest count for its tier.

The bakery grants BREAD and employs the baker. The separate tavern grants WANDERERS and
employs the innkeeper. Its back-room bed is the only assigned housing slot; the other three
authored beds remain furnishings.
This integration does not add a new physical innkeeper food-service loop or new hospitality bonuses.

## Biome selection

Both `/kithkyn create-village ~ ~ ~` and natural founding use the same selector, then save
the chosen style for the life of the village. Existing villages do not change architecture
when the jar is updated or when residents walk into another biome.

Priority is:

1. An explicit datapack biome tag, `kithkyn:village_style/<style>`.
2. Conventional biome-family tags. Birch forests, including birch-named modded biomes that
   omitted the conventional tag, select `birch_forest`.
3. A deterministic selection among loaded climate-compatible founding catalogs. Temperature,
   precipitation, downfall and conventional hot/cold/wet/dry tags inform the cluster. World
   seed, biome ID and founding location seed the choice.

The styles are Birch Forest (bundled), Desert and Badlands (private datapacks). Only birch,
desert/sandy and mesa/badlands/savanna families map conventionally; every other family goes by
climate, where a hot, dry biome chooses between Desert and Badlands and everything else builds
Birch Forest. This is a practical fallback, not a claim that temperature identifies a biome's
trees or culture. A modpack tag can explicitly correct any ambiguous match. Future styles enter
these clusters only once their own founding set is loaded.

An explicit dev override remains available:

```mcfunction
/kithkyn create-village ~ ~ ~ birch_forest
```

## Identity, entities and preservation

The center's four beds alternate primary/secondary on opposing sides. One-bed homes and
the couple's joined beds use primary; two-bed homes use one of each. The bakery's remaining
bed and the tavern keeper's back-room bed use primary.
Tower beds follow the same one/two rule. The hunter and butcher use secondary. Selected
center, storehouse and mine banners become the village's saved flag. Decorative market
banners are not identity slots. Neutral white in the exported source marks dynamic slots;
both instant and incremental placement resolve the actual colors while placing the blocks.

Containers start empty. Personal chests are excluded from communal storage. The butcher's
six initial animals are marked as village stock; the center's golem remains ordinary until
a guard encounters and adopts it. Template entities are placed once, with persistent receipts
that prevent reloads or same-building upgrades from spawning duplicates. Decorative bakery
item frames keep their contents and attachment orientation.

The center sinks four blocks to preserve the authored basement. Its actual basement campfire
is the gathering anchor. Review-platform smooth stone is not exported. Enclosed underground
air is carved, while terrain outside the basement stays intact. Approved tall grass is retained.
A single matching birch stair at the stoneworks' raised entrance connects its existing walkway
to ground level. This access correction exists only in the derived asset, not the saved gallery.

Placement uses the tight envelope of authored blocks and decorations, with zero empty capture
padding. The four-block capture border is neither claimed nor prepared, and the exporter no
longer writes air through that border. Original local coordinates stay unchanged so beds,
containers, identity slots and basement details still refer to the approved edits. Ordinary
shared walking lanes remain separate from this removed padding.

The storehouse is raised one block (`sink: -1`) to expose its first entrance step. Both the
mine and storehouse declare their authored west-facing entrances; founding rotates each
entrance toward the center. The Birch mine's shaft runs east from local `[9,0,8]`, offset
`[1,0,1]` from its unchanged miner station. This leaves a flat landing after the authored
stairs before the first dug step; the former `[8,0,8]` mouth produced an unclimbable
one-and-a-half-block exit. Worker excavation and navigation use the same frame. Existing
mines retain their saved mouths so their already-dug tunnels and branches do not move.
The catalog also declares the other authored fronts: the homes, cottage, bakery, tavern, blacksmith,
butcher compound, church, fishery, hunter, stoneworks and first tower approach from the west;
the lumberjack and two-stall market approach from the north; the second tower and three-stall
market approach from the south. The single stall opens west. Fields, the well and center
retain the north convention for their multiple equivalent approaches. These directions describe
the public approach, not a door block's often inward-facing state. Ordinary growth now uses
these fronts when it prefers entrances toward the town center.

Each founding companion is centered along its selected side of the center's tight footprint,
within half a block for mixed odd/even widths, preferring two walking blocks between footprints
and accepting one when space is tight. Inward fronts are preferred; another rotation may fit
where the inward one cannot, as described in [site-selection.md](site-selection.md).
Terrain chooses the two sides, not an off-center alignment within a side. All completed buildings
use the same three-block natural tree line and one-block foliage clearance as walls, including
command/natural founding and upgrades. The approved planting, crops and sapling are protected;
the clearance does not add terrain padding or change existing galleries.

The second watchtower is narrower than the first. Its upgrade may leave the predecessor's
dirt/grass landscaping outside the new parcel, but cannot leave old structural blocks. No
dummy capture padding is added to make the upgrade appear larger.

Birch perimeter art uses the five approved cobblestone/mossy-cobblestone pieces, stone walls,
cobblestone slabs, oak access trapdoors, ladders and torches through the existing terrain-following
wall planner. No old gallery or reference build is replaced.

## Source and verification

The immutable local authoring record is `run/valecraft-gallery/birch-approved-20260907/`,
including its approval manifest, final captures and saved-world snapshot. Derived production
assets live under `src/main/resources/data/kithkyn/structure/`; definitions live under
`data/kithkyn/kithkyn/buildings/`. `run/birch-integration/export-report.json` records source
hashes. Provenance is retained separately from the derived gameplay metadata.

The 2026-09-08 center revision is recorded in
`run/valecraft-gallery/birch-center-approved-20260908-175033/`. The user replaced flowers
with grass, added a tall-grass plant, changed four lanterns to candle clusters, rebuilt the
bell cap, and added four birch-leaf tips above the prior capture height. The template is
now 29x11x29, with the same horizontal footprint and sink of four. The basement, beds,
personal chest, work/gathering positions, identity slots and original typed golem entry are
unchanged. The exporter applies `tools/structure/birch-center-20260908.json` over the immutable
September 7 source so future exports retain these 75 authored block changes and the bell's
native wall-attachment state. The smooth-stone review platform is never exported.

The editable sky copy remains at `[9700,190,10600]`, with an untouched reference at
`[9750,190,10600]`. Preserve both after integration. This approval updates the source asset;
existing founded centers must not be regenerated or overwritten automatically.
The revision passed 310 JUnit tests and the native 24-building placement/restart suite,
including rotated candles, grass and canopy tips, with all 72 fixture entities preserved.
At capture time the tested release was staged in the revision archive; the capture itself
did not restart or overwrite the live dev server's loaded template.

`BirchAssetsTest`, `BirchCatalogResolutionTest`, `VillageStyleTest`, `VillageFoundingFootprintTest`
and `BirchWallTest` cover the catalog, source contracts, selection, footprints and wall compilation.
The subsequent storehouse/fishery approval is archived at
`run/valecraft-gallery/birch-store-fish-approved-20260908-184200/`, including initial, approved
and margin captures, untouched references and the preceding assets/definitions. Storehouse
loses ten cobblestone forecourt blocks, shrinking its solid footprint by one column. Fishery
gains eight cobblestone supports and three entrance stairs; its former central barrel is
removed and the chest becomes a barrel. The fisher's workstation and sole communal container
now both use local `[10,2,10]`. No beds or personal storage were added. Seating is deliberately
unchanged: storehouse `sink: -1` (raised one block), fishery `sink: 0` (normal seating).
Their editable sky origins remain `[9700,190,10650]` and `[9700,190,10675]`, with untouched
references fifty blocks east. Display platforms are excluded. These edits update source
assets only; do not regenerate the workshop or already-built villages.
This revision passed 310 JUnit tests and native verification of 88 rotated templates,
300 planned access routes, 24 actual entity walks and biome-selected founding. The
relocated barrel also creates usable empty storage in all four orientations. Its tested
release was initially staged in that archive without updating the running server.

The September 8 deployment uses
`run/valecraft-gallery/founding-search-release-20260908-195700/`, combining these approved
assets, the shared iron/snow golem changes and the bounded natural-site search. It passed
314 unit tests and the native catalog, walking, delayed-plan protection and fallback-founding
checks. The old server was shut down cleanly and all dimensions saved. At the user's request,
its complete world was moved to
`run/backups/world-before-fresh-20260908-195900/world` before starting a fresh normal world.
The same backup holds the preceding server classes, resources and configuration. All old
workshop/gallery edits remain in that archived world; do not copy it over the fresh world
without a separate restoration request. Newly placed structures use the approved assets.

The later September 8 access approval is recorded in
`run/valecraft-gallery/birch-access-approved-20260908-205309/`, including the flushed live-world
snapshot, preceding templates, and the exact approved delta. The center changes twelve cells:
each of its four exit lintels becomes a banner one block inward from the old banner, and the
old banner cell and slab above it become air. Dynamic banner slots now use `[14,4,10]`,
`[10,4,14]`, `[18,4,14]`, and `[14,4,18]`. Width is not the problem: the old overhang blocked
adults slightly taller than two blocks while ascending the one-block-wide stairs. The revised
center retains all beds, personal storage, basement, planting and its original typed golem.
The mine changes six cells: three upside-down rear stairs move from X=10 to X=11 at Y=0,
Z=7..9, with the final row two cobblestone stairs and one mossy stair at Z=9. Its front stairs,
workstation, saved shaft frame and banner are unchanged. Runtime path wear, inventories,
excavation and incidental blocks are not imported. The other 25 Birch assets remain byte-identical.
These changes use the same exporter: the center revision is extended and the mine revision is
`tools/structure/birch-mine-20260908.json`. Existing built structures are never regenerated.

This access revision passed all 314 JUnit tests, 88 rotated-template checks, 396 planned
access routes and 32 actual walks. Real instant and incremental placement passed for 24
fixtures, including village-colored relocated banners, initial entities, mid-build reload and
upgrade preservation. A second server boot preserved all 24 fixture buildings and their 72
original entities without replenishing them. Verification logs and JUnit XML are in the
approval archive's `verification/` directory.

The dev deployment was cleanly stopped at 17:07 EDT, and `before-deployment/` in that archive
preserves the complete current world, prior classes, resources and jar. The tested release
changes only the two templates, the center's banner metadata and the disabled catalog-test
helper. No existing structure or world was regenerated. Release jar SHA-256:
`29e6110f739ba6c82b42a688f25b55ec6081c8f609ab32e1d66fe5eb6234078a`.

Disposable-server `BuildingPlacementVerification` checks real placement, rotations, colors,
entities and save/reload. `BirchVillageVerification` checks actual person navigation to declared
amenities in all rotations and the complete biome-selected founding set. These opt-in verification
checks now also cover all four center exits in both directions at Size 13, 15 and 18, in every
rotation, plus actual Size-18 walking out of and back into the basement. This is specific to the
approved center, not a claim that every building supports every genetic size. The verification
flags must never be enabled when launching the player's dev world.
`ConstructionClearanceVerification` separately exercises real add/replace completion hooks after
instant and incremental placement, including mid-build reload, in all rotations. It checks the
tree and foliage boundaries, overhead clearing, terrain, owned timber and authored plants.
The physical mine-entrance check gives its walker real village membership and an excavated
shaft. It must climb out and walk back down in all four rotations, including worn path footing;
merely reaching the surface work station does not catch an unclimbable exit. For a focused
disposable-server run use `-Dkithkyn.birch.mineEntryVerify=true`. Adding
`-Dkithkyn.birch.mineEntryLegacy=true` tests the surface-only repair for existing east-facing
`[0,0,1]` shafts: stairs move from local X=6 to X=5, the former stair cells are cleared, and
full cobblestone landing supports sit underneath at Y=-1. This is a tested manual repair, not
a migration that rewrites arbitrary old mines or user builds.

The zero-padding regression also checks untouched terrain just outside each of the 88 rotated
placements, exact claim areas, inward-facing founding entrances, storehouse seating and all
four orientations of the narrower tower's upgrade fit. Terrain fixtures check adjacent-side
selection on flat ground and north/east placement when west/south are blocked. It does not
certify a full excavation work cycle or reconstruct existing terrain.

Deployment must preserve existing authoring worlds. In-progress construction saves block-list
indices and preparation queues, while old mines may already have tunnels dug in their previous
frame. Do not hot-reload these changed templates into active projects. Buildings now save their
mine entrance frame and placed sink: new mines use the authored entrance, while legacy mines
retain their old southward local frame and station. Legacy Birch stores retain sink zero;
new ones use minus one. No old tunnel, building or saved claim is physically moved. Construction
persists its exact template with the cursor. When updating an older save, either wait for its
projects to finish or use `FreezeBuildingProjects.java` on a stopped, backed-up save to add the
old templates. The helper writes a new copy and verifies that every preexisting NBT field is
unchanged. An old active project finishes its old plan; newly started projects use the new assets.
Saved claim grids also need deliberate rebuilding for an existing village.
A fresh disposable village verifies the corrected placement without repairing or overwriting
the player's old site.

The September 9 sky-workshop approval is archived at
`run/valecraft-gallery/birch-center-approved-20260909-120936/`. The user restored twelve cells:
four chiseled-stone exit lintels, the four outer cobblestone slabs, and the outward banners at
`[14,4,9]`, `[9,4,14]`, `[19,4,14]`, and `[14,4,19]`. The definition and checked-in center
revision use these banner positions as dynamic village identity slots. The 29x11x29 bounds,
basement, beds, storage, work stations, planting, bell, canopy and typed golem are unchanged.
Only this center asset changes. The edited sky copy remains at `[10640,190,9020]`, with its
smooth-stone platform below the capture at Y=189; preserve it for further user edits.

The catalog check pins ordinary adult genetics to average scores for reproducibility and
separately exercises larger residents through all four center exits. Collision height is now
capped at two blocks for teenagers/adults and one block for toddlers/kids, independently of
visual scale. This replaces the old tall-resident clearance failure and permits the restored
lintels. Existing village centers are not regenerated when the source asset is updated.
This approval passed 371 JUnit tests, 88 rotated-template checks, 396 planned access routes,
and 32 actual walks, including tall-resident exit and re-entry in all center rotations.
Native placement verified 24 fixtures through both construction paths; a complete server restart
retained all 72 original entities without replenishment. Exterior and basement previews were
rendered in a separate Minecraft client before deployment.

## Separate tavern approval: September 9

Aaron selected and edited sky-showcase F05.1, the Dungeons & Taverns 4.4.4 Birch tavern
(`nova_structures:tavern/tavern_house_birch`). The immutable approval is
`run/valecraft-gallery/birch-tavern-approved-20260909-135429/`: a flushed world backup,
exact and margin captures, and a fresh final recapture after the keeper-room clarification.
The two captures agree and retain the added grass, wall lighting and downward-facing ceiling
barrel at local `[13,4,16]`. The private bed is `[14,1,16]`; the innkeeper station is
`[10,1,14]`, beside the communal chest at `[10,2,15]`.

The same approval reduces the bakery to its baker bed at `[7,5,11]`, removing both halves
of the former innkeeper bed and its work station. The existing personal chest remains.
Household storage uses the shared supported-footing and eye-to-container reach checks, so the
keeper can use the ceiling barrel from a reachable spot without moving the authored block.
Entrance selection prefers the authored front among equally low doors, keeping the public
entrance distinct from the interior bedroom doors in every rotation.
The shared housing reconciliation releases bed indexes removed by a definition update before
assigning real available beds. Existing completed buildings are not automatically regenerated;
the new templates govern future construction.

The separately admired Birch center F01.1 is reserved as a future design reference. It does
not replace the currently approved village center.

Validation for the September 9 separation: the full build and all 377 JUnit tests pass.
The native Birch fixture passes 92 rotated placements, 404 planned access routes, 32
physical walks, and four real keeper stash-and-sleep trips. Each keeper carries seven
emeralds from outdoors into the private ceiling barrel and then uses the assigned back-room
bed, with no duplicated items. The quartermaster's existing visit, pacing and cleanup fixture
also passes. Live screenshots of the edited tavern and the repaired Rustic barns were inspected;
artifacts and logs are in `run/birch-tavern-verification/`. This records local validation, not
a deployment of the new gameplay code to the live server.
