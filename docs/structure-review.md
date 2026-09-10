# Structure reference review

This file records decisions made while walking the local ValeCraft reference gallery. It is a
design trail for Kithkyn's own structure work: what earned a prototype, what was deferred,
and why. The local gallery and its exact reference templates remain under the gitignored `run/`
tree.

## Current Birch decision

The current `birch_polish` selection is **design-approved by Aaron as of 2026-09-07**.
Its 27 final exhibits are preserved in `run/valecraft-gallery/birch-approved-20260907/`,
with `approval.json` identifying the accepted captures, checks, and remaining integration.
Use that snapshot, including its authored tall grass, rather than regenerating an earlier
Romanian or Dungeons and Taverns proposal. The historical passes below explain how this
selection was reached; they do not override the approved current geometry.

Design approval is not production readiness. No production catalog replacement is made by
this lock-in. Metadata/export, real village navigation and occupancy, construction/upgrades,
and the remaining shared-workplace/guard integration still need implementation and verification.

## Review pass: 2026-09-04

### Defensive buildings

| Reference | Decision | What carries forward |
| --- | --- | --- |
| Towns and Towers old-growth pillager fort | Do not prototype in the current pass | The encampment is interesting, but it does not fit an immediate Kithkyn need. |
| Towns and Towers taiga tower | Do not prototype in the current pass | The silhouette is impressive, but the scale is too large for the ordinary watchtower ladder. |
| Dungeons and Taverns forest firewatch tower | Prototype as the level-1 watchtower reference | The compact footprint and clear tower shape are a strong starting point. Add village banners and audit villager navigation between every floor. |
| Towns and Towers flower-forest tower | Prototype as the level-2 watchtower reference | Keep the stronger tier-2 silhouette and banner placements. Rework the dark wood palette toward oak and stone, then audit stairs, ladders, doors, and floor access for villagers. |

The two retained towers should be evaluated as a progression pair. The level-2 result should feel
like a developed defensive building, not an unrelated monument.

### Mediterranean village

The white Mediterranean village has a strong church and several simple, convincing houses. Keep
the family in the reference pool, but defer prototypes until the first defensive and tavern pass
is complete. Its tight interiors and alleys need a deliberate villager-navigation review before
any individual building is selected.

### Taverns

| Reference | Decision | What carries forward |
| --- | --- | --- |
| Dungeons and Taverns desert tavern | Prototype against `tavern_desert_1` | Use it as the immediate desert tavern reference, then reshape it to Kithkyn's scale, palette, stations, and pathing requirements. |
| Oak, spruce, and cherry taverns | Defer | Compare each with the matching shipped Kithkyn tavern before replacing anything. The current oak tavern may already be the better base. |

### Ruins

The Terralith rubble structures are useful environmental references but do not currently map to a
selected Kithkyn building. Keep them in the gallery rather than starting a prototype.

### Second-wing selections

| Reference | Decision | What carries forward |
| --- | --- | --- |
| Towns and Towers Japanese family | Skip as a general Kithkyn direction | Keep the farm and stable as individual candidates. Both may be stronger than the corresponding current structure, but compare them before replacing anything. |
| Towns and Towers Swiss temple | Prototype as the level-1 church reference | Treat it as the first step in a church progression. The large cathedral reference becomes level 2. A distinct level 3 can come later. |
| Towns and Towers Pueblo family | Reserve the `badlands` village biome | Compare its material language and massing with CTOV Mesa and the Towns and Towers Wooded Badlands family without merging their catalogs. |
| Dungeons and Taverns birch fishery | Replace the current fishery direction with a Kithkyn adaptation | Preserve the appealing footprint and silhouette, then adapt its palette, work station, storage, water access, and villager pathing. |
| Dungeons and Taverns birch farm, armorer, and library | Strong candidates | Keep all three visible for the category-by-category variant review. No replacement is decided yet. |

### Dense-wing selections

| Reference | Decision | What carries forward |
| --- | --- | --- |
| Dungeons and Taverns birch animal pen | Strong candidate | Use its readable livestock layout as the leading animal-farm reference. |
| Dungeons and Taverns birch cleric | Do not prototype in the current pass | The building does not add enough beyond the stronger church references. |
| Towns and Towers classic family | Reserve `alpha_islands`, at low priority | Several buildings read close to generic Minecraft structures, but the family remains a distinct catalog rather than being pooled into Plains. |
| Towns and Towers Romanian center | Use as the level-2 village center reference | The selected civic-center schematic is `birch_forest_meeting_point_1`. It belongs in the center progression and must not be relabeled or duplicated as the Romanian church. |
| Towns and Towers Romanian large house | Strong high-capacity house candidate | Preserve the convincing multi-room massing and evaluate it as a four-bedroom house. |
| Towns and Towers Romanian farm and fishery | Strong candidates | Keep both for the food-building comparison pass. |
| Towns and Towers Romanian armor-and-tools and weaponsmith buildings | Strong blacksmith candidates | Compare both as blacksmith levels or regional variants rather than separate professions by default. |
| Towns and Towers Romanian meat-and-leather building | Strong butchery candidate | Compare it with the combined livestock and processing role already assigned to Kithkyn's butchery. |
| Towns and Towers Polish, Viking, swamp, Polynesian, jungle, mushroom, Iberian, Nilotic, and sunflower families | Give each its own village-biome slot | Review their architectural language across whole families rather than selecting isolated material swaps. |

The review now treats Towns and Towers as a major reference source rather than a source of a few
individual buildings. The useful unit is the full reference family: recurring roof language,
footprints, civic hierarchy, industry, housing, and public space. Any Kithkyn adaptation
still needs its own functional layout, navigation, identity anchors, and coherent group-wide
edits.

The next visual pass deliberately moves to non-Towns-and-Towers sources so the project does not
mistake one mod's aesthetic for the entire design space.

The church decision is a building-level progression, not a settlement-tier gate. A camp may still
build a church if resources, space, and the village's own priorities support it.

The next selection pass should begin only after the broader gallery walk. Inventory the current
Kithkyn categories and variants, then review one village biome at a time. Compare nearby
catalogs, such as Badlands, Wooded Badlands, and Mesa, to ensure each has a legible boundary
without pooling them. This prevents a good isolated building from leaving the full village
visually incoherent.

## Village identity must come first

Before the retained structures become production buildings, each village needs a persistent visual
identity:

- a primary color;
- a secondary color;
- a generated banner pattern representing the village;
- authored banner positions in buildings that are important enough to display it;
- semantic palette anchors that can use the two village colors without blindly replacing every
  block of the same material.

The village establishes this identity at founding, immediately after its permanent name is chosen.
It is stored with the village so every later structure uses the same identity.

The primary and secondary colors are ordered Minecraft dye colors, suitable for wool, banners,
and deliberately chosen accent blocks. The banner is a layered Minecraft banner design using
both colors. A dragon-like emblem is one possible outcome, not a required universal motif.

Village identity is independent of village biome. The village biome supplies the architecture; the
identity makes two villages using the same catalog visibly distinct.

Implemented foundation:

1. Generate the two colors and banner when the founding name lands.
2. Persist the complete identity in village save data, with deterministic migration for old saves.
3. Let building definitions declare banner sockets and semantic primary or secondary color anchors.

The remaining structure work is to author those slots in the selected buildings, prototype the
level-1 forest firewatch tower, level-2 flower tower, and desert tavern, then run villager navigation
through every occupied floor before accepting a structure.

## Gallery expansion

The second review wing adds 24 practical village-scale references rather than more oversized
monuments. Its rows cover Towns and Towers Japanese, Swiss, and Pueblo villages, plus the Dungeons
and Taverns birch village. Each row includes a mix of housing, civic, food, and production
buildings so the next review can compare roles as well as silhouettes.

The dense third wing adds another 106 exhibits in thirteen compact rows. It extends the Dungeons
and Taverns birch set, then covers Towns and Towers classic, Romanian, Polish, Viking, swamp,
Polynesian, jungle, mushroom, Iberian, Nilotic, and sunflower-farm families. A final row compares
six Dungeons and Taverns wells. Buildings sit four blocks apart, and the rows emphasize recurring
roles so the review can compare variants instead of merely collecting unusual landmarks.

The fourth wing adds 96 exhibits from sources other than Towns and Towers. Four ChoiceTheorem's
Overhauled Village rows compare beach, alpine, swamp, and mesa settlements across the same twelve
roles. Four Millenaire rows compare Norman, Byzantine, Seljuk, and Mayan architecture using each
building's complete first-stage template rather than isolated upgrade fragments. The rows remain
four blocks apart horizontally and build one at a time to keep world generation bounded.

The Millenaire gallery copies translate its runtime markers and small custom block palette to close
vanilla stand-ins. This keeps the architectural massing, floor plans, and role comparisons visible
without changing the active dev mod stack; the source templates in the ValeCraft instance remain
untouched.

### Fourth-wing selections

| Reference | Decision | What carries forward |
| --- | --- | --- |
| CTOV beach family | Keep for Coast and Jungle | The center reads as a market, the houses establish a convincing warm shoreline language, and the farm and fishery make the set useful beyond housing. |
| CTOV alpine family | Keep for Mountain and Tundra | The enclosed farm is the standout reference for agriculture in hostile weather. The houses and center support a complete highland settlement. |
| CTOV swamp family | Keep for Swamp | Its bushy, overgrown character is distinct from both ordinary Forest and the Towns and Towers boat-based swamp set. |
| CTOV mesa family | Keep for Badlands | The sparse, frontier-like character supplies a useful alternative to Pueblo massing while remaining coherent across a whole town. |
| Millenaire families | Remove from the influence pool | No further Millenaire review or adaptation is needed. The gallery copies may remain as historical exhibits, but they do not inform Kithkyn structures. |

There are still many structures not shown. The local pack contains about 3,300 Dungeons and
Taverns templates, 2,100 CTOV templates, and 840 Towns and Towers templates, although many are
dungeon pieces, repeated material variants, or connectors rather than village buildings. The
next useful gallery should target unresolved boundaries instead of adding volume: CTOV mountain
versus snowy, CTOV jungle-tree versus beach, desert oasis, taiga, mushroom, and the unreviewed
Towns and Towers forest, rustic, Swedish, Tudor, Mediterranean, and Iberian families.

## Recommended village-biome roster

The current three-state roster lives in [village-biomes.md](village-biomes.md). The discussion
below remains as gallery history and source-family evidence rather than the current status list.

The twelve-group proposal was too coarse. It pooled several complete Towns and Towers families
under Plains, Forest, Tundra, Badlands, and Jungle, which would discard exactly the settlement
variety the gallery exposed.

The revised rule is one distinct village-biome catalog for every Towns and Towers Overworld
settlement family. This establishes 26 slots: Alpha Islands, Subtropical Grassland, Hot Shrubland,
Floodplain, Autumn Forest, Shield, Highlands, Desert Oasis, Badlands, Beach, Birch Forest, Flower
Forest, Forest, Grove, Jungle, Meadow, Mushroom Fields, Deep Ocean, Old Growth Taiga, Savanna
Plateau, Snowy Slopes, Snowy Taiga, Sparse Jungle, Sunflower Plains, Swamp, and Wooded Badlands.
The Piglin family reserves Nether Wastes as a twenty-seventh slot when Nether villages enter
scope. The corresponding table and source-family mapping live in [buildings.md](buildings.md).

Large non-Towns-and-Towers families are also separate when they establish a coherent settlement
language. The current review already adds Plains, Old Growth Birch Forest, Tropical Coast, Alpine
Highlands, Mangrove Swamp, and Mesa. A partial reference family may reserve a slot before every
building role exists; missing roles receive original structures or carefully chosen secondary
references rather than forcing two families into one catalog.

This is not limited by biome supply. The local ValeCraft pack's Terralith data contains 95 custom
biome definitions plus overrides for 35 vanilla Overworld biomes. The actual constraint is
authoring and navigation quality. Every real biome still maps to one village biome at founding,
while shared biome traits carry climate, resource, and placement behavior across architecturally
different catalogs.

Fortified sets, seasonal sets, and settlement stages remain separate axes. They do not become
fake biomes just because they contain many templates. The category-by-category pass also remains
sparse: highly visible buildings need strong village-biome variants, while visually neutral
industry can use shared structures where the environment does not demand a different layout.

Farmer's Structures and Ribbits both contain promising references, but their required runtime
libraries need newer NeoForge builds than the current Kithkyn dev instance. Keep them in the
source inventory and revisit them with the planned runtime upgrade instead of showing partially
missing structures now.

## Birch Forest lock-in wing

The first category-by-category selection wing is the Birch Forest catalog, anchored by the Towns
and Towers Romanian family. It presents all 17 building categories retained by the final cut in
`building-spec.md`, every planned level for each category, the current Kithkyn structure when
one exists, a leading recommendation, and useful alternatives. The wing contains 89 candidates in
18 walkable rows; the final row treats walls and gatehouses as a perimeter system rather than as
ordinary building footprints.

The rows are ordered as follows: Village Center, Houses, Well, Storehouse, Watchtower, Farm,
Lumberjack, Stoneworks, Mine, Hunting Lodge, Fishery, Bakery, Butchery, Blacksmith, Market, Church,
Tavern, and Perimeter System. Removed concepts such as library, brewery, workshop, and specialty
shop remain visible in the broader reference field but are deliberately excluded from this
required-building review.

The role-first wing already uses every one of the 22 Romanian building and decoration templates
at least once. A parallel side library now repeats each of those templates exactly once and adds
the family's 20 street and terminator pieces, producing a complete 42-template Romanian inventory.
It is connected to the Birch entry by an eastward bridge and grouped into nine densely packed,
labeled rows:

- Core and decoration: meeting point 1; decorations 1, 2, and 3.
- Homes: small houses 1 through 6; medium houses 1 and 2; large house 1.
- Professions: armorer and toolsmith; butcher and leatherworker; cartographer and library; fisher;
  fletcher; mason; shepherd; small farm; weaponsmith.
- Streets: corners 1 through 3; turn 1; crossroads 1 through 6; straight streets 1 through 6.
- End pieces: terminators 1 through 4.

Every exhibit has its own `R01` through `R42` sign, so the in-world labels form the requested
complete list while keeping the underlying source identity visible. Run
`/function valecraft_gallery:tour_romanian_reference` to enter the side library. If it has not
been built in the current world, first run
`/function valecraft_gallery:build_romanian_reference` and allow its nine scheduled rows to
finish. The library is intentionally separate from the candidate table: it supports visual
discovery and repurposing without implying that every Romanian piece has already been selected
for a Kithkyn role.

Each candidate sign uses one of four decision labels:

- `Best`: the leading direction for that role and level;
- `Option`: a credible alternative worth comparing in person;
- `Adapt`: a useful shell, silhouette, or scale reference that still needs a Kithkyn role
  layout;
- `Baseline`: the corresponding structure currently shipped by Kithkyn.

Run `/function valecraft_gallery:tour_birch_selection` to enter the wing. If the gallery is rebuilt
from source, run `/function valecraft_gallery:build_birch_selection` once and allow the scheduled
rows to finish before touring it. The selection process should now proceed row by row: choose a
candidate, record the required functional and palette changes, prototype it, check villager access
to every occupied floor and workstation, and only then lock the level into the Birch Forest
catalog.

The local gallery generator also corrects absolute attachment coordinates found in a handful of
source paintings and item frames. For an already-built wing carried through a server restart, run
`/function valecraft_gallery:repair_birch_selection_decorations` once to restore those decorations
without rebuilding any structure.

## Birch Forest second-pass selection

The spoken review narrowed the Birch Forest catalog to 33 provisional exhibits in 11 rows. This is
a visual editing set, not a production lock-in: mechanically safe wood-profile-to-birch substitutions are
already applied to retained Kithkyn structures, while footprint, interior, navigation, banner, and
accent edits remain for the next in-world pass.

| Role | Provisional Birch Forest choice | Next-pass work |
| --- | --- | --- |
| Village center L1 | Current Kithkyn camp, birch profile | Restyle beyond the mechanical palette swap. |
| Village center L2 | Towns and Towers Romanian center | Use the actual `birch_forest_meeting_point_1` town-center schematic as the upgraded center reference. |
| House L1 | Romanian small house 6 | Keep its two-bed capacity. |
| House L2 | Romanian small house 1 | Keep its two-bed capacity. |
| House L3 | Romanian medium house 1 | Accept three beds; variants do not need identical capacities. |
| Well | Dungeons and Taverns birch well | Lower its height. |
| Storehouse L1 | Current Kithkyn tent, birch profile | Cobbled deepslate floor is applied; restyle and verify storage access. |
| Storehouse L2 | Romanian medium house 2 | Adapt the interior as a warehouse; omit L3 for now. |
| Watchtower L1 | Current Kithkyn watchpost, birch profile | Cobbled deepslate is applied; reshape toward the Romanian Birch Forest language. |
| Watchtower L2 | Towns and Towers flower tower, birch profile | Test villager access to every floor and add village-banner sockets. |
| Farm L1 | One Dungeons and Taverns birch farm 1 | Edit the retained base. |
| Farm L2 | Folded pair of birch farm 1 | Join and clean the seam. |
| Farm L3 | L-shaped group of three birch farm 1 footprints | Join and clean both seams. |
| Lumberjack | Current Kithkyn lumberjack, unified birch roof and deepslate base | Reshape toward the Romanian language. |
| Stoneworks | Romanian mason | Adapt its stations and functional interior. |
| Mine L1 | Current Kithkyn mine, birch profile | Retain as the small mine starting point. |
| Mine L2 | Duplicate of the current mine L1 | Redesign into a genuinely different upgrade. |
| Hunting lodge | Romanian fletcher | Adapt the shell to the hunter role. |
| Fishery | Romanian fisher | Adapt the chosen shell. |
| Bakery | Romanian medium house 2 | Convert the interior to the bakery role. |
| Butchery | Romanian butcher and leatherworker | Adapt the chosen shell. |
| Blacksmith | Romanian weaponsmith | Use one level only for now; the broader request to remove blacksmith L2 everywhere is deferred to the production pass. |
| Markets L1-L3 | Current Kithkyn markets, birch profile | Restyle each level; keep the existing progression. |
| Church L1 | Towns and Towers Swiss church | Adapt as the smaller church. |
| Church L2 | Current Kithkyn large church, pending replacement review | Keep the center schematic out of this row. The intended Romanian church reference still needs to be identified independently. |
| Tavern | Romanian large house 1 | Convert the ground floor to tavern use while retaining upper lodging. |
| Perimeter | Current straight, diagonal, terrace, corner-tower, and gatehouse pieces, birch profile | Restyle the full wall family consistently. |

There is no Church L3 in the current Birch Forest plan. A third tier is a possible future addition,
not a current gallery or implementation target.

Run `/function valecraft_gallery:tour_birch_final` to enter this compact selection wing. Rebuild it
with `/function valecraft_gallery:build_birch_final` after regenerating the review datapack. The
live review world currently places the wing east of the earlier galleries, beginning near
`609 171 907`.

### Production model decisions exposed by the review

Building level is a capability progression, not a universal amenity contract. A role and level
may have different bed counts, workstations, storage, or secondary amenities in different village
biomes. Each concrete variant must declare what it actually provides, and village planning must
read those declared amenities instead of assuming that every level-3 house, for example, contains
four beds.

Village visual identity is also a separate axis from village biome. A village owns an ordered
primary and secondary Minecraft dye color plus a generated banner pattern using those colors.
Structures may expose authored banner sockets and semantic accent anchors, allowing the same Birch
Forest catalog to retain a coherent architecture while different villages remain recognizable.
The implementation and authoring format are documented in [village-identity.md](village-identity.md).

## Complete Dungeons and Taverns Birch comparison

The later Birch Village I/II review is separate from the Romanian selection above. Its local
comparison catalog covers all 18 building roles, including the couples cottage, across 29
role-and-tier rows, followed by five wooden perimeter pieces and one explicitly unresolved
stone-perimeter row. There are 35 rows and 66 placed exhibits in total.

Run `/function birch_catalog:tour` to enter at `1004 241 155` in the overworld. Follow the aisle
south. Each row has an A/gray current Kithkyn baseline and a B/yellow Birch proposal. Empty pads
identify a missing baseline or unresolved design rather than pretending it already exists.
`ADAPT` and `RESTYLE` labels are unfinished conversions, not production-ready role assignments.
The catalog retains only church T1/T2, storehouse T1/T2, and blacksmith T1, as requested.

The source is `run/valecraft-gallery/build-birch-catalog.py`, the exact exhibit inventory is
`run/valecraft-gallery/birch-catalog.json`, and the generated private review pack is
`run/world/datapacks/birch-catalog`. On first `/reload`, the pack builds scheduled rows and saves
completion flags so subsequent reloads preserve in-world edits. It does not change production
building definitions. Saved-world checks verified all 66 exhibit signs and nonempty structure
footprints; a complete in-game visual and functional pass is still required.

## Preserved Birch editing pass: 2026-09-06

Aaron edited the complete Dungeons and Taverns comparison in-world. That original wing must
never be rebuilt, replaced, or removed. A flushed saved-world copy is kept under
`run/valecraft-gallery/backups/birch-edits-20260906-1109/`; all 66 original exhibits were
captured independently with native Minecraft NBT types, including chest contents and banner
data. `run/valecraft-gallery/revision-20260906/` holds the captures, checksums, exact selection
manifest, and village-center amenity markers.

The new review pack is `birch-revision`, with `/function birch_revision:tour` entering at
`1287 201 72` in the overworld. It has 28 exhibits in fourteen paired rows: follow the aisle
south, reviewing left then right. Only the new gallery volume is assigned the warm Plains
biome. Weather snow is omitted and frozen fishery water is thawed in the copies. Existing
galleries, their biomes, and global weather rules are untouched. Reload completion flags
protect this new editing copy from being rebuilt too.

| Birch role | Revised editing selection |
| --- | --- |
| Center T1 | Old B Center T2, including Aaron's four-block-deep basement; no center upgrade. |
| House T1 / T2 | Saved one-bed and edited two-bed houses; no House T3/four-bed house. |
| Couples cottage | Saved two-bed couple home. |
| Well | Earlier narrow 3-by-3, eleven-block-tall D&T Birch well. |
| Storehouse T1 | Old B Storehouse T2 becomes the sole storehouse; no tent or upgrade. |
| Watchtower T1 | Old B Market T1 (D&T cartographer), confirmed explicitly by Aaron. |
| Watchtower T2 | Current Kithkyn T1 watchtower, converted to Birch. |
| Farm T1 / T2 | Saved single farm, then a merged pair with one central composter; no Farm T3. |
| Lumberjack / Stoneworks | Saved toolsmith and mason proposals. |
| Mine T1 | Saved weaponsmith shell for Aaron to edit; no Mine T2. |
| Hunting lodge / Fishery | Saved fletcher and fishery proposals. |
| Bakery | Retained current B bakery proposal pending another choice. |
| Butchery | Old B Tavern (shepherd shell). |
| Blacksmith | Retained weaponsmith proposal. |
| Market T1 | Current Kithkyn baseline, converted to Birch. |
| Market T2 / T3 | Retained saved Birch baseline conversions. |
| Church T1 | Small saved cleric chapel only; no Church T2. |
| Tavern | Separate copy of old B Bakery (butcher shell) for interior editing. |
| Wooden perimeter | All five saved Birch wall/gatehouse pieces. |

Stone perimeter T2 remains unresolved, not silently considered complete or removed. The bakery
and tavern deliberately share a starting shell in this pass; only the tavern's replacement was
requested. Mine, tavern, and other role conversions remain editing shells, not finished workplaces.

The center's basement begins at old world Y237, with the beds and personal chest at Y238.
The old smooth-stone review platform at Y240 is excluded from the captured building, but
every other basement block is retained. Its one personal chest is intended to be shared by
the four residents, not counted as village storage. The four bed heads, chest, and four
placed banners are recorded as relative amenity/identity positions. Two northern beds use
primary and two southern beds use secondary. Blue/yellow and matching rhombus/border banners
are preview colors, not a universal Birch identity. A bed identity slot now recolors both
halves together; actual village identities continue to supply their own colors.

The new center includes one ordinary iron golem at the reference's authored location. It is
not pre-enrolled, pre-named, or made into an artificial guard. A real village guard can recruit
it through the normal encounter behavior. These are private review structures and staging
markers only: the production building catalog, costs, upgrade chains, and finished-building
golem spawning are not changed by this gallery pass.

Verification: all 28 new exhibits and signs were checked in the live saved world after the
dev server restarted. Offline comparison found exactly twelve changed cells in the center:
eight bed halves and four banners, with the remaining basement/building cells preserved.
The full Gradle check passed. A disposable real-server check verified that bed recoloring
keeps both halves and their block entities through repeated application, all four facings,
either authored half, and later neighbor updates. Its late golem query ran after the center
unloaded; the saved entity data independently confirms the ordinary golem. The new wing's
visual walkthrough remains pending: the Minecraft client was disconnected at screenshot time.

### Live review follow-up: center, homes, storage, towers, and farms

Aaron is now reviewing and editing the warm `birch_revision` wing. He approved the center's
overall layout, all three home designs, the narrow tall well, and the storehouse. The farm
pair with one central composter is the right direction; he is still making final block edits.
This records his visual review, not a completed agent navigation or occupancy test.

The following identity assignments supersede the initial gallery preview arrangement:

| Building | Required identity assignments |
| --- | --- |
| Center | Four beds in an alternating diagonal arrangement: primary/secondary on one side, secondary/primary on the opposite side. Keep two of each. All designated center banners use the village flag. |
| One-bed home | Its bed uses primary. |
| Two-bed home | One bed uses primary and the other uses secondary. |
| Couples cottage | Both adjacent beds use primary, visually forming one shared double bed. This does not reduce the two occupants to one bed entitlement. |
| Storehouse | Capture the banner Aaron is adding and mark it as a village-banner slot. |
| Watchtower T1 | The bed Aaron is adding uses primary. Preserve his moved chest, candles, and other interior edits. |
| Watchtower T2 | One bed uses primary and the other uses secondary. Preserve his interior edits. |

Watchtower T1 should have one stationed guard; T2 should have two. Each is a crossbow guard
with a sword for close combat. These are requested staffing/loadout changes, not claims that
the current gallery shells already grant those jobs or that the combat behavior is implemented.
The final layouts need reachable guard posts, beds, and storage before production acceptance.

The intended flags and bed colors are village-specific, never fixed blue and yellow. The
current identity applier supports these semantic slots on building completion and upgrade;
the private gallery uses preview colors, and arbitrary hand-placed banners do not currently
auto-convert. Final template capture must register the new and moved slots, and the placement
timing requirement must be checked when integrating these structures into the village catalog.
No village identity or new flag should be rerolled for an individual building or banner.

Do not overwrite, regenerate, or recolor this active editing wing while Aaron is still working.
These follow-up decisions are recorded only. After he finishes, obtain a fresh saved-world
flush and backup, then capture his actual final blocks before applying the requested identity
metadata and staffing changes. Coordinate main-server restarts before interrupting his review.

### Final-edit copy: farms, industry, shared hospitality, and stone perimeter

The next spoken review completed the warm-wing edits. A console `save-all flush` was issued
without restarting the server, and a separate world snapshot was taken at
`run/valecraft-gallery/backups/birch-final-edits-20260906-1242/`. The capture confirms the new
tower beds and moved chest, both farms' barrels, the lumberjack dead-bush marker, the mine
banner, the hunter and butcher beds, and the bakery's two upstairs beds and personal chest.
All 28 original edited exhibits, including the now-unselected separate tavern, were preserved
as native NBT captures under `run/valecraft-gallery/revision-20260906-polish/captures/`.

The follow-up copy has 27 exhibits. Its pack is `birch-polish`, namespace `birch_polish`,
with `/function birch_polish:tour` entering at `1447 201 72`. The convenience functions
`birch_polish:basement`, `birch_polish:bakery`, `birch_polish:butcher`, and
`birch_polish:walls` lead to the key changes. `birch_polish:previous` returns to the
unedited source wing. The new destination volume was verified empty in already-generated
saved chunks, and only its own biome becomes Plains. Existing galleries are never rebuilt.

| Area | Applied in the new templates |
| --- | --- |
| Center, homes, and towers | All bed-color assignments from the preceding review, including the diagonal primary/secondary center arrangement and both primary-colored halves of the couples' double bed. The center basement and ordinary golem remain. |
| Village banners | The four center banners and the new storehouse and mine banners are authored village-identity slots, previewed in blue/yellow. Existing decorative market banners stay unchanged. |
| Farm T1 / T2 | Preserve both final layouts and their new barrels; T2 still has one central composter. |
| Lumberjack | Replace the one authored dead bush at relative `[2,1,3]` with a birch sapling on existing dirt, and record it as the chopping station. No grown tree was present in the capture. |
| Stoneworks, mine, blacksmith, markets, and chapel | Preserve Aaron's final blocks and choices, with inventory cleanup only where applicable. |
| Hunting lodge | Preserve the added upstairs bed and color it secondary. |
| Butchery | Secondary-colored resident bed, inside personal chest, outside communal barrel, and three cows plus three chickens inside the pen. Sheep were superseded by the final chicken choice. |
| Bakery + tavern | One shared building using the edited bakery, two upstairs beds colored primary/secondary, one shared upstairs personal chest, separate communal storage, and staged BAKER and INNKEEPER stations. Omit the separate tavern only from this new copy. |
| Perimeter | Replace birch log columns across all five wall/gatehouse pieces with cobblestone and clustered mossy cobblestone. Replace their lanterns with supported standing/wall torches. Keep the remaining wooden rails, platforms, and functional details. This is a Birch style choice, not a global stone-wall-tier change. |
| Containers | Clear inventories and remove unopened loot tables/seeds in every copied exhibit, so reference loot cannot generate when a chest is opened. Original containers and backup contents remain intact. |

The generator is `run/valecraft-gallery/build-birch-polish.py`. The `selection.json`,
`amenities.json`, `changes.json`, and `template-verification.json` beside its captures retain
the exact mapping and verification evidence. All 27 templates passed exact requested-edit
and native block-entity preservation checks, inventory/loot cleanup, bed/color assignments,
and animal counts. There are no new structural support warnings. The four inherited
corner-tower ladder/trapdoor support warnings remain recorded for in-game review.

These are still private review templates, not replacements for the production catalog.
The existing building model supports two distinct professions and a shared personal chest,
so the bakery/tavern does not require a new multi-workplace system. INNKEEPER currently lacks
a work routine. Standalone watchtower GUARD stations also need the requested fixed-post,
crossbow-and-backup-sword behavior; ordinary guard jobs currently use the patrol/axe path.
The desired one/two tower guards are recorded, not falsely reported as implemented combat.
Village identity slots are staged for integration, while blue/yellow remain gallery previews.

The server subsequently shut down cleanly when the other task's console session ended.
It was started again for this review, and startup loaded the new pack. All 27 exhibits and
their signs are now confirmed in the saved world by
`revision-20260906-polish/live-verification/placement-verification.json`. Fresh native
captures also confirm the bed colors, designated banners, empty containers, lumberjack
sapling, center golem, and exactly three cows plus three chickens at the butchery. No
`/reload` is needed to see the gallery.

A placement-time item-frame warning prompted a narrowly scoped missing-frame fallback.
Inspection of the saved bakery confirms the original pie frame is already at the correct
position with the correct attachment coordinates, so no live repair is necessary. The
fallback, when loaded in a future reload, skips an existing frame and cannot rebuild the
gallery. Source captures and the original editing wing were not changed.

The rendered in-game visual walkthrough and worker navigation/occupancy checks remain
pending. Saved-world checks establish placement and contents, not those runtime behaviors.

### Polished-gallery review and wall finish: 2026-09-07

Aaron reviewed `birch_polish` in-game and approved the overall catalog. His further edits
include stone/moss accents on Watchtower T1, a mine chest, and another decorative item frame
in the shared bakery/tavern. All are present in a fresh saved-world capture. The bakery/tavern
remains one building for two residents and two professions. Nothing in this follow-up changes
the production catalog or the remaining runtime integration work.

The remaining requested edits are confined to the five perimeter exhibits:

- 61 birch fences become 47 cobblestone walls and 14 mossy cobblestone walls. Moss follows
  the supporting mossy blocks, keeping the already-approved mixed-stone palette.
- All ten birch slabs become cobblestone slabs, retaining their top/bottom/double state.
- The four birch ladder-backing trapdoors become oak, retaining their orientation, half,
  and open state. Ladders and lighting are not replaced.

The mine already has the requested single chest at world `1405 202 264` (relative `[5,1,5]`).
Keep it instead of adding a duplicate or a barrel. All 22 non-perimeter exhibits are outside
the patch. In particular, do not rebuild the user's towers or bakery/tavern from older templates.

The relevant saved region and entity region were cloned into
`run/valecraft-gallery/backups/birch-polish-wall-finish-20260907-2130/`. Their clone hashes
matched the live source hashes immediately after the copy. This was a saved-world snapshot,
not an acknowledged player-issued flush. All 27 exhibits were then captured from that backup
under `revision-20260906-polish/wall-finish-20260907/captures/`, including the newer watchtower
blocks, mine chest, and bakery potion frame. Original captures remain immutable.

`build-birch-polish.py finish-walls BACKUP` reuses the native capture/patch tools and prepares
75 individual block substitutions, not structure placement or a broad fill. Before changing
anything, the in-game function checks every target against the captured state or its intended
replacement. An unexpected edit stops the pass. A completion flag prevents later reloads from
reapplying it. Temporary chunk tickets are removed only when this patch added them.

At the end of that patch-preparation turn, the patch awaited `/reload`. The existing `/function birch_polish:walls`
still leads to the same exhibits. Template checks confirm only the 75 intended substitutions,
unchanged block-entity/entity data, and preserved slab/trapdoor geometry. The four old
corner-ladder support warnings are identical except for the requested trapdoor species;
their real Minecraft support depends on the preserved open, north-facing trapdoor state.
Live application and ladder survival were subsequently confirmed in the approval capture below.
The independent agent-rendered visual check remains pending. The available
computer-use surfaces do not expose the Minecraft window, so no agent visual verification is claimed.

For subsequent reference examples, plain white beds and blank white banners are acceptable
placeholders for dynamic village colors/flags. Keep the primary/secondary/banner role metadata
even when those placeholders look identical. This permission does not require recoloring the
current approved examples. Aaron also observed the ordinary center golem walking/falling off
the raised gallery platform; do not duplicate it or freeze its normal AI just for the display.

### Birch design approval: 2026-09-07

After updating the exhibits and landscaping, Aaron approved the set for Birch. Preserve the
latest in-world geometry as the chosen design, including intentional short and tall grass.
This concludes the selection/design pass, not the gameplay integration pass.

`run/valecraft-gallery/birch-approved-20260907/` contains an independent saved-region snapshot,
all 27 native captures, their actual world origins and dimensions, and `approval.json`.
The snapshot's region/entity hashes matched the live files after copying. It is explicitly a
saved-world snapshot, not a claim that a newly requested player flush was acknowledged.
Capture bounds include a one-block horizontal margin so new edge landscaping is retained;
the smooth-stone review platforms at Y196/Y200 are excluded. Final production metadata must
therefore be rebased against `captures/inventory.json`, not copied blindly from the earlier
amenity file retained as a reference.

The saved completion flag confirms the wall patch ran. Comparison against the actual captured
blocks confirms all 75 substitutions, all five corner-tower ladders and all six gatehouse
ladders, and the new mine chest. The captures also retain 30 tall-grass halves across six
buildings, the lumberjack sapling, both bakery item frames, three cows and three chickens,
and one ordinary center golem. No live-world blocks were changed during this approval capture.

The final design is frozen unless Aaron requests another revision. Before production use,
export and validate the concrete templates, empty copied inventories/loot as already requested,
recheck every bed/container/station/identity slot, verify the shared bakery/tavern and intended
tower staffing/loadouts, and test actual rotated placement, navigation, occupancy, construction,
and upgrade behavior. The user's visual approval is recorded separately from those checks.

### Birch tavern approval: 2026-09-09

Aaron selected the edited Birch tavern F05.1 from the complete style showcase. Its immutable
capture and flushed world backup are in
`run/valecraft-gallery/birch-tavern-approved-20260909-135429/`. A second capture after the
room clarification exactly matches the first, including the added grass, wall torches and
ceiling barrel. This is the selected source, not the unedited mod template.

The new tavern takes the INNKEEPER role from the bakery. Its back-room bed and ceiling
barrel are personal; its front chest is communal. The bakery retains one BAKER bed and its
existing storage. The other three authored tavern beds remain furnishings. The previously
approved center remains; F01.1 is a separate future reference Aaron liked.

## Full-showcase biome discussion: September 9

During the 300-exhibit showcase tour, Aaron identified Swamp as a strong possible next village,
with Viking and Desert also of particular interest. Pueblo is an especially exciting direction.
Romanian and Japanese should each receive a suitable environmental home; the renewed interest
in Japanese reopens the earlier general-family deferral. Swiss, Swedish, Tudor, Mediterranean,
Rustic, jungle, Polynesian and mushroom families remain strong candidates. These comments are
preferences and exploration, not approval of a new production catalog.

Dungeons & Taverns Birch Center 1, F01.1, is a definite future reference to retain. It does not
replace the current approved Birch center. Possible later concepts include village ships,
a distinct design adapted from Piglin architecture, and ruined settlements with skeleton
residents. Skeleton residents are an exploratory population/settlement idea, not an implemented
feature or a decided replacement for ordinary villagers.

### Proposed environmental homes

The following are assistant recommendations for the next selection pass, not final user-approved
biome mappings. The earlier minimum roster still reserves distinct architecture families. These
are priority coverage targets, not a reduction of the roster or permission to blend whole families.
Existing Birch gameplay selects the approved Birch catalog for both ordinary and old-growth Birch;
Romanian's historical Birch assignment should not overwrite that catalog.

| Environmental target | Reference direction | Showcase |
| --- | --- | --- |
| Swamp and orchid wetland | Compare the boat settlement, D&T swamp and CTOV wetland families independently | C07, F03, E07 |
| Mangrove swamp | Packed-mud/mangrove direction; compare CTOV with D&T mangrove details | E07, E08, G06 |
| Jungle | Tribal settlement; evaluate canopy settlements as another distinct direction | B06, D06, D07, F02 |
| Sparse jungle and tropical coast | Polynesian settlement | C05 |
| Snowy taiga | Viking settlement | C04 |
| Taiga and Shield | Swedish settlement | A06 |
| Old-growth taiga | Polish settlement | C01 |
| Meadow and alpine valleys | Swiss settlement; retain CTOV Alpine as its own comparison family | B07, D11 |
| Flower forest and cherry/Sakura landscapes | Japanese garden-settlement direction; compare CTOV Mountain separately for upland architecture | B03, D10 |
| Forested highlands and wooded valleys | Romanian settlement | B02 |
| Desert and oasis | Compare full desert and oasis towns, retaining the tent camp as a separate family | D03, D04, A08, G01 |
| Badlands | Pueblo; frontier Mesa remains a distinct alternative for another badlands setting | A09, D08 |
| Warm coasts and dry shrubland | Mediterranean; retain Iberian as a separate grassland direction | A03, A02 |
| Mushroom fields | Compare the fantasy and CTOV mushroom families independently | B08, E01 |
| Rolling highlands | Tudor | A07 |
| Beaches and coast | Nautical settlements and lighthouse landmarks | B01, D01 |

Suggested near-term build order: Swamp, Viking, Desert, then Pueblo, bringing each to the same
functional and authoring standard as Birch. Swamp selection should first compare C07, F03 and
E07, then choose a coherent base family for the next working village. Other Swamp families
remain available for later distinct catalogs.

Terralith 2.6.2 in the local Valecraft source pack contains 95 custom biome definitions, verified
from its jar during this discussion. Useful named future targets include Forested Highlands,
Temperate Highlands, Shield, Sakura Grove, Sakura Valley, Desert Oasis and Orchid Swamp.
This source-pack inventory does not assert that Terralith is installed on the current live server.
Seasonal Christmas/Halloween collections and fortified editions retain their existing treatment
as event/defense variants, separate from the biome roster. Ruined and inhabited-skeleton
settlements need their own later design decision.

### Pueblo selection showcase: September 9

Aaron chose Pueblo as the next architecture selection pass, superseding the assistant's earlier
Swamp-first recommendation. Beach is explicitly deferred. He requested a separate comparison
showcase collecting Pueblo-like, Badlands, Mesa, Desert, Oasis and Savanna architecture while
preserving the complete seven-wing gallery. Combining suitable buildings from these sources is
part of this exploratory pass; no new production catalog has been selected yet. Romanian in a
dark-oak forest is another tentative environmental idea from this tour.

The separate collection is cataloged under `run/pueblo-showcase/`, with an entrance at
`3585.5 230 986.5`. Its six wings contain 454 native-compatible buildings, centers and details:
Pueblo/Mesa, Desert/Oasis, Savanna/Tents, Taverns/Mines, Forts/Ruins, and vanilla references.
The original gallery ends near X3373 and the new structures start near X3591. Source IDs, exact
origins, exclusions, and a browsable exhibit list are in its catalog and guide. Private copies
use the `kithkyn_pueblo_showcase` namespace.

Pueblo includes 33 complete buildings plus three lamps. Its six profession inserts are assembled
inside their authored shared shell with native block-state rotations. The Ramshackle example
reuses the previously verified complete tower assembly. The shared typed-NBT display exporter
and placement helpers are reused; the original gallery's catalog and templates are preserved.
Empty anchors, exact duplicates and compatibility templates that require absent mod blocks are
excluded explicitly. Ruins, tavern grounds and mine parts remain labeled reference components.
These are editable review copies, separate from gameplay integration and production approval.

All 57 rows and their connecting navigation are placed and saved. Native saved-world captures
verify all 454 exhibits. Visual review caught three fluid spills, now contained; the final check
of all 53 water-bearing exhibits found no remaining spills around their platforms. Unsupported
ground-level grass and saplings were restored over soil in the review platforms. The source
farm P09.7 has one wheat plant suspended over air that cannot survive native block updates;
other block-name differences are Minecraft's grass rename and covered-path conversion to dirt.
Private backups, captures, provenance and verification results remain under the showcase folder.

### Pueblo center bed colors: September 9

Aaron edited center P01.1 and requested a mix of village primary and secondary colors for its
11 beds. Six bed slots use primary and five use secondary, alternating across the floors:
three primary/two secondary downstairs, three primary/three secondary upstairs. The live gallery
shows blue/yellow preview colors, following the earlier review convention. The role assignments
are independent of those colors and use the existing `village_identity` definition format.

`tools/structure/pueblo-center-20260909.json` records the exact captured origin, all bed head
and foot positions, and the semantic slots. The referenced private capture preserves Aaron's
edits; a derived neutral template has white beds for eventual integration. This remains an
in-review center, without a new production catalog entry. The live recolor changed only bed
colors, preserved all 11 pairs and their facing, and left the surrounding building intact.

### Pueblo center rooms and staffing: September 9

The next P01.1 capture supersedes the eleven-bed review above. Aaron removed the upper
southeast room's primary bed and placed a chest in its position. The center now has ten beds:
five primary and five secondary. Its bell plaza is the central meeting and arrival place;
the two rooftop campfires are independent cooking, recovery and fireside gathering amenities.
The meeting point is an authored safe standing position near the bell, not a rule that every
village must gather on its bell block.

The room inventory is explicit, relative to captured origin `(3598,230,1008)`:

| Room | Bed head | Personal chest |
| --- | --- | --- |
| Northwest ground | `(7,1,11)` | `(9,1,8)` |
| Northeast ground | `(18,1,12)` | `(15,1,11)` |
| Southwest ground | `(6,1,18)` | `(8,1,18)` |
| Southeast ground | `(17,1,21)` | `(21,2,25)` |
| South central ground | `(13,1,23)` | None |
| Northwest upper | `(9,4,10)` | None |
| Southwest upper | `(8,4,18)` | `(8,4,20)` |
| Northeast upper | `(19,5,12)` | None |
| Southeast upper | `(21,5,21)` | `(19,5,21)` |
| South central upper | `(13,5,23)` | `(15,5,25)` |

The courtyard barrel `(15,1,14)`, the southwest room without a bed's chest `(6,2,26)`,
and the northeast rooftop chest `(16,8,10)` are communal storage. A resident in a room
without a chest must not claim another room's chest merely because it is nearby.
The six authored red banners are village-banner sockets. The live gallery uses a blue
banner with yellow center stripe and border as a preview; the saved template is neutral
and receives the actual village's generated colors and pattern when placed.

The starting staffing target is eight: quartermaster, builder, guard captain, two rooftop
crossbow posts (one near each fire), two additional stone-sword patrol guards, and miner.
The default is to house the miner here and give them a separate mine entrance, as in Birch;
the Pueblo mine itself remains to be selected. Consequently the center defines seven
workstations and the founding mine contributes the eighth. These are job openings filled
through normal arrivals and claiming, not eight villagers spawned instantly. Two beds remain
beyond the eight intended workers. The captain patrols whenever awake; the existing nightly
sleep and occasional sentry-patrol rules apply to the appropriate guards.

`tools/structure/pueblo-center-20260909.json` records the capture hash, full runtime definition,
room ownership, identity slots, meeting point and both fires. The immutable edited structure
and prepared review datapack remain under `run/pueblo-showcase/center-approved-20260909-161322/`.
This is one configured center in the ongoing Pueblo selection pass, not a complete Pueblo
building catalog or a live simulation deployment.

Verification passed on the prepared private center: 88 bed, container and post access routes
across all four rotations, eight actual villager climbs from the plaza to the rooftop posts,
and eight fire-approach routes. The worker-loop fixture fetched four raw cod from a chest,
cooked them and physically returned all four cooked cod to storage. All 386 Gradle tests pass.
The crossbow stations use clear, supported diagonal floor beside each fire at `(8,8,18)`
and `(13,10,23)`, rather than the upright trapdoor rims. No structure edits were needed.
The live gallery's six banners are updated; these AI changes are verified locally and await
a subsequent server deployment.

### Pueblo shared houses: September 9

Aaron edited P01.4 through P01.7 as four housing alternatives. The source exhibit names
are design labels, not an upgrade ladder:

| Exhibit | Design | Single beds | Couple rooms | Total people |
| --- | --- | --- | --- | --- |
| P01.4 | Large House 1 | 2 | 1 | 4 |
| P01.5 | Large House 2 | 2 | 0 | 2 |
| P01.6 | Large House 3 | 2 | 2 | 6 |
| P01.7 | Large House 4 | 2 | 0 | 2 |

The fourteen beds provide eight single spaces and three reserved couple rooms. House 1's
two ground-floor singles share their floor's chest, and its upstairs couple shares another.
House 2's ground-floor single owns the sole chest; its upper single has no personal storage.
House 3 assigns one chest to each couple room, leaving both singles without a personal chest.
House 4 has a separate chest for each single. None of these seven containers is communal.

The live gallery uses eight primary blue beds and six secondary yellow beds. Both halves
of each couple's double bed share a color. House 1's two banners and House 4's banner use
the same village-banner preview as the center. The neutral exports use white bed and banner
sockets for the actual village identity. Capture verification confirms all other blocks,
orientations and block-entity contents remain as Aaron authored them.

`tools/structure/pueblo-houses-20260909.json` records the immutable source hashes, exact
coordinates, room ownership, couple pairs and identity slots. The captures, neutral templates,
and inspected screenshots are private review files under
`run/pueblo-showcase/houses-approved-20260909-165758/`. Production categories, tiers, prices
and upgrade relationships remain part of the complete Pueblo catalog selection. The gallery
colors are live; the supporting shared-house AI changes await a later server deployment.

The native private review test placed all four designs in all four rotations, verified
84 bed/container routes, assigned every single and couple to the intended room, and completed
56 actual walks and sleeps through the ordinary nighttime goal. All 399 Gradle tests pass,
including save/reload of mixed households, single-bed reservations, occupied-room refusal,
job changes retaining a couple's room, and returning spouses waiting until both arrive.
The three couple rooms also passed another 24 ordinary sleep-goal trips across all rotations
with maximum-size adults measured at exactly two blocks of collision height. Their existing
geometry and the production sleeping behavior needed no changes.

### Pueblo medium and small houses: September 9

Aaron selected Medium Houses 1–2 and Small Houses 1–6 as eight additional home designs.
Use the first, left-hand gallery copy of each design. The alternate right-hand copies stay
in the gallery as references and are excluded from the selected set. He edited only the
selected Small House 3 into a compact couple home, replacing its single bed with two adjacent
beds and moving its personal chest up to the ceiling/upper-floor level.

| Exhibit | Design | Single beds | Couple rooms | Personal containers |
| --- | --- | --- | --- | --- |
| P02.1 | Medium House 1 | 2 | 0 | One chest per floor |
| P02.3 | Medium House 2 | 2 | 0 | One chest per floor |
| P02.5 | Small House 1 | 1 | 0 | Chest |
| P02.7 | Small House 2 | 1 | 0 | Chest |
| P02.9 | Small House 3 | 0 | 1 | Shared raised chest |
| P02.11 | Small House 4 | 1 | 0 | Barrel |
| P03.1 | Small House 5 | 1 | 0 | Barrel |
| P03.3 | Small House 6 | 1 | 0 | Chest |

These eleven beds add nine single spaces and one couple room. Together with the four large
houses, Pueblo now has twelve selected housing designs. These are alternative home designs,
not twelve sequential upgrade tiers. Their final gameplay catalog mapping remains part of
the ongoing Pueblo selection pass.

`tools/structure/pueblo-houses-extra-20260909.json` holds the immutable capture hashes and
exact amenities. The source, neutral templates, and before/after saved-world captures are
under `run/pueblo-showcase/houses-extra-approved-20260909-173019/`. Live preview beds use six
primary blue slots and five secondary yellow slots. Both medium homes have one of each;
Small House 3's couple beds share primary. The other small homes alternate colors.
All ten containers are personal, and every bed is explicitly bound to its room's storage.
The selected copies contain no banners. The recolor preserves all other blocks and native
block-entity data, and all eight excluded alternate copies remain unchanged.

All eight designs pass the native check in all four rotations at maximum adult size:
84 planned access routes, 44 physical personal-container deposits and 44 assigned-bed sleeps.
The raised chest in Small House 3 is accessed from the roof; both spouses then walk down
into the bedroom. This review exposed two shared movement defects: wide bodies caught the
edge of an open door, and a mattress target could strand a resident on the roof above a
tight bedroom. Doorway clearance and supported bedroom approaches now address those cases
without editing the authored geometry or allowing sleep through walls. All 399 Gradle tests
pass. Nine inspected screenshots and the native evidence are archived with the captures.
The four previously selected large houses also pass the updated movement checks in all
orientations: 56 assigned-bed sleeps and 44 personal-container deposits at maximum adult size.
The gallery colors are live; these runtime fixes await deployment with the ongoing catalog work.

### Pueblo workplaces: September 9

Aaron repurposed P03.5 Temple 1 as the watchtower and P11.5 Well as the mine entrance.
P03.6 Toolsmith And Weaponsmith 1 is the selected blacksmith. P03.7 Butcher Workshop
and P09.6 Coop were combined in a separate draft preview, P12.1, beyond the Pueblo rows
at `3600 230 1385`. Aaron subsequently selected P10.4 Sheep Farm for the butcher instead.
The superseded draft and both source exhibits remain available for comparison.

| Exhibit | Selected use | Staffing and storage |
| --- | --- | --- |
| P03.5 | Watchtower | One fixed crossbow guard, one primary-color bed, one bed-bound personal chest |
| P03.6 | Blacksmith | One blacksmith at the upper workbench area, two communal chests, no bed |
| P11.5 | Mine entrance | One miner on the supported rim, one communal chest, no bed |
| P12.1 | Superseded butcher and coop draft | Reference only; P10.4 is the selected butcher |

The tower retains the edited shooting windows, ladders, bedroom and personal chest.
Its live bed is blue for the primary-color preview; the neutral export uses a white bed
socket. The smith's station is an open floor tile beside the smithing table, and the
butcher's is beside the smoker. Containers are storage destinations rather than crafting
station targets. These bedless workplaces use residents housed in the village's homes.

The mine retains the authored dry three-by-three opening and canopy. Its shaft mouth is
one block above the template ground, allowing the first ramp step to return to the rim.
During ordinary five-wide mine excavation, sixteen ground-layer blocks around and beyond
the opening fall inside the planned shaft; all authored blocks above that layer remain.
This excavation is future miner work, not an alteration to the gallery capture.

The butcher and coop use their original orientations, with a one-block path between them.
The workshop faces west and the pen gate faces north. The pen interior remains within the
butcher's livestock search radius. Starter livestock and the final production catalog are
still part of the selection pass; this draft does not promise leather or wool production.

`tools/structure/pueblo-workplaces-20260909.json` records source hashes, component placement,
job stations, storage ownership, mine orientation and village identity. Immutable captures,
neutral exports, native readback evidence, before/after snapshots and inspected screenshots
are under `run/pueblo-showcase/workplaces-approved-20260909-181024/`. The directory label is
an archive identifier; the source snapshot was taken at 18:05:37 local time. Native block
comparison confirms that the tower's two bed halves are the only changes to the five source
exhibits. The separate composite matches all 312 expected non-air block states.

The access walkthrough exposed the same wide-body clearance issue on an open trapdoor
that had affected open doors: a centered route brushed the vertical wooden panel. The
shared movement correction now covers both door types while retaining their collisions.
This runtime change awaits deployment with the ongoing Pueblo work.

All four workplaces pass the native review in all four rotations with maximum-size adults:
40 planned access routes, 16 station walks, 16 shared-container deposits, four personal
storage visits and guard sleeps, eight mine descent/return trips, and eight coop gate/interior
visits. The guard keeps its real post assignment while returning to its assigned bed on a
sleeping night. Both fixture reports are archived as `verification-posts-and-forge` and
`verification-mine-and-coop`. All 399 project tests pass.

Small House 3 and Large House 1 also pass the final shared-movement regression in all
four rotations: 24 personal-container deposits and 24 assigned-bed sleeps across the
existing raised-storage, ladder, doorway and couple-room layouts.

### Pueblo services and worker households: September 9

The next selection comprises ten edited buildings. `tools/structure/pueblo-services-20260909.json`
records their immutable capture hashes, local stations, room ownership and village-color slots.
Source captures, neutral exports and verification evidence are archived under
`run/pueblo-showcase/services-approved-20260909-183601/`.

| Exhibit | Selected use | Beds and storage |
| --- | --- | --- |
| P08.7 Small 1 | Storehouse tier 1 | No beds; two chests and three barrels, all communal |
| P10.6 Vault | Storehouse tier 2 | Quartermaster bed, personal floor barrel upstairs; nine communal basement barrels |
| P10.3 Orchard | Acacia lumberjack lodge | One bed and personal chest; dead bush marks the acacia stand |
| P01.9 Mason 1 | Stoneworks | No beds; one communal chest |
| P09.7 Farm | Grain farm | Farmer household couple room with shared personal chest |
| P10.4 Sheep Farm | Butchery | Butcher household couple room with shared personal chest; three sheep |
| P03.10 Fletcher Workshop | Hunting lodge | No beds; one communal chest |
| P09.8 Fishing Oasis | Fishery | One fisher bed and personal barrel |
| P09.5 Bakery | Bakery | No beds; one communal chest |
| P08.2 Big | Tavern | One innkeeper bed and one general resident bed; five communal containers |

P10.4 replaces the P12.1 butcher-and-coop draft. Three sheep are retained; the three nearby
unnamed, untamed llamas spawned during editing were removed by their exact entity IDs.
The neutral sheep-farm template preserves three adult sheep and the authored painting.
Existing green wool stripes on the sheep farm and tavern are primary-color sockets, while
white wool remains white. Every bed is a primary or secondary socket; both sides of each
couple room share a color. The live review uses blue and yellow, not fixed production colors.

The vault is sunk five blocks and the oasis four blocks. Enclosed basement air is retained
without excavating surrounding terrain. Native sheep navigation exposed an escape route via
the raised trough onto the pen perimeter. Its eleven blocks were lowered one block into the
ground in both the live exhibit and a fresh native capture. Other authored blocks retain
their world positions, and the original capture remains archived. Four gallery floor lights
included by the expanded capture are excluded from the neutral export. The vault export
adds eight matching foundation blocks five blocks
below ground, making its footprint eight by eight so it can contain the first storehouse's
eight-by-six footprint during upgrading. Its visible authored geometry remains unchanged.
Gallery-only water containment barriers and escaped farm water are excluded from the neutral
farm asset. The edited gallery itself retains its water containment and dead-bush marker.

A worker couple room is reserved for a household where at least one spouse works in that
building. Claiming the job can allocate both beds atomically, even when no single bed is free.
The other spouse can hold a different job or remain idle. An unmarried worker may take the
job while housed elsewhere, but cannot occupy half the couple room. The pair keeps its room
while either spouse works there, and both beds reopen when neither does. The tavern declares
only its innkeeper bed as `worker_beds`, leaving the second bed available to a general resident.

The sheep farm's public entrance is on its north face; its raised west wall is not an entry.
The tavern keeps its authored spiral stair and half-slab loft. Standing positions now resolve
the actual supporting surface and test the full body at that height. Pathfinding can accept
a two-block gap between a lower slab floor and upper slab ceiling. A half-step beneath the
spiral stair's slab bridge uses its actual rise rather than requiring a full-block jump;
the existing collision sweep still rejects a bridge that is too low. Solid obstructions,
unsupported surfaces and special gate or hazard rules retain their normal restrictions.

The tavern's one-cell storage closet is approached through its wooden doorway. Planning
keeps that lower doorway as the destination instead of lifting the target onto the roof;
the normal door goal opens it during the walk. Storage tasks then choose a fresh hand
stance around the open door panel. Container transfers still require an unobstructed ray
from the villager's actual eyes. Iron doors and obstructed closet interiors do not qualify
for this approach. An active visit to the adjacent container holds its door open through
the quartermaster's delayed transfers; closing the visit restores normal door closing.
Route edges also respect the open panel's physical shape, so a transverse approach goes
around the leaf. Final path nodes center the villager on the accepted standing position;
vanilla's earlier stopping tolerance could leave a vault shelf outside actual hand reach.

All ten services pass native review in four rotations: 40 placements, 184 access routes,
40 station walks, 88 communal visits with two real timed transfers each, 28 personal-container
deposits and 36 assigned-bed sleeps. The storehouse upgrade fits in all four rotations.
All 48 closed-gate sheep route probes remain inside the repaired pen; three sheep and the
painting survive each placement and its resident activity. Previously approved Small House 3,
Large House 1, mine and coop also pass their access regressions. `./gradlew check` passes all
408 tests, and seventeen live screenshots have been inspected. `final-verification.json`
records the accepted evidence: the repaired pen report supersedes the original food report's
failed pen result, while the other four unchanged food-service buildings retain their passing checks.

These assets remain a reviewed selection, pending the complete Pueblo production catalog.
The gallery colors and trough repair are live. The household and access runtime changes are
local and await deployment.

### Pueblo cleric and remaining catalog choices: September 9

The edited P09.4 Alchemist is the selected church/temple for a cleric. Its immutable capture
and amenity definition are recorded in `tools/structure/pueblo-cleric-20260909.json`, with
private assets under `run/pueblo-showcase/cleric-approved-20260909-204200/`. The one bed is
reserved for the cleric and uses the village primary color. The back chest sits in the
work area, away from the bed, and remains communal under the workplace storage convention.
The authored bookshelves, nether-wart planter, roof and decorative chimney fire are preserved.
The selected capability is `HEALING`; this does not add an enchanting or brewing capability.

Native review passes all four rotations with maximum-height adults: twelve access routes,
four station walks, four assigned-bed sleeps, and four communal chest visits with two timed
quartermaster transfers each. The live preview changes only the two bed halves; every other
captured block and every block-entity tag is unchanged.
Three live screenshots verify the exterior, workroom and primary-colored bed.

The three existing Birch market tiers are adapted with acacia framing, orange terracotta,
red-sandstone counters, arid potted plants and primary/secondary village-colored canvas.
Existing white canvas remains white. `tools/structure/pueblo-markets-20260909.json` records
the native source hashes, palette conversion, local access repairs and review metadata.
The P13 row sits south of P12 and is separate from the Desert Oasis market previews in D12.

| Exhibit | Template origin | Capacity |
| --- | --- | --- |
| P13.1 | `3597 230 1432` | One merchant station and one communal barrel |
| P13.2 | `3625 230 1432` | Two merchant stations and two communal barrels |
| P13.3 | `3660 230 1432` | Three merchant stations and three communal barrels |

Native access checks found that the original floor barrels were obscured by trim and that
countertops made approaching merchants climb into the canopy. Each adapted stall raises
its existing barrel one block, fills the old floor cell with matching terracotta and opens
a two-cell-wide passage through the adjacent counter and trim. Station positions, storage capacity, authored
block-entity contents and the overall stall layout remain. Original Birch assets and Desert
Oasis previews are preserved.

Five entry corners also lose their obstructing vertical trim and low shelf. The final repair
touches 46 cells across the three adapted templates. Native review passes twelve rotated
placements, 48 access routes, 24 station walks, 24 communal visits with 48 real timed
transfers, and eight upgrade-containment checks. A fresh saved-world capture matches all
final declared preview blocks after the guarded live repair. The canopy and eaves remain intact.
Four final live screenshots cover all three tiers and the accessible barrel closeup.

A separate well is intentionally omitted because the selected town center already contains one.
The center manifest now grants `WATER`, backed by its six existing waterlogged stone-stair
basin blocks; no geometry changes or extra well structure are required. No large
watchtower was selected; the previously approved small watchtower remains in the selection.
These choices complete this selection pass, without activating the unfinished Pueblo catalog.

### Pueblo readiness and identity audit: September 9

The selected set now contains thirty reviewed templates: one center, twelve alternative homes,
ten service buildings, three workplaces, one cleric building and three market tiers. P10.4 Sheep
Farm is the selected butchery with three sheep; the P12.1 butcher-and-coop draft is superseded and
must be excluded from production export.

A read-only comparison of every selected neutral NBT against its manifest found all 46 bed
heads represented in both housing metadata and village-color slots: 26 primary and 20 secondary.
Every declared couple pair uses the same color. These are counts across the available designs,
not the starting population of one village. All nine actual village flags are marked for the
generated village banner: six at the center, two at Large House 1 and one at Large House 4.
The per-template evidence is in
`run/pueblo-showcase/catalog-audit-20260909/identity-audit.json`.

There is one remaining identity gap. The three markets use another 78 plain wall-banner blocks
as hanging awning fabric, with 13, 26 and 39 strips respectively. These are decorative cloth,
not village flags, and their current colored strips retain the blue/yellow preview palette.
`VillageIdentityApplier` does not yet support banner blocks as plain primary/secondary accents.
During integration, preserve the approved plain cloth appearance and white strips, and make the
colored strips follow the corresponding village color without applying the village flag pattern.

The remaining production work is:

- Register the environmental `badlands` catalog, export the reviewed assets and definitions,
  and require its own founding center, mine and storehouse without borrowing Plains designs.
- Expose all twelve house designs as alternatives through the shared catalog and planner.
  The current canonical category/level lookup does not make every alternative selectable;
  these homes must not become a twelve-step upgrade ladder.
- Assign final categories, levels and recipes, and connect the storehouse 1-to-2 and market
  1-to-2-to-3 upgrades using production identifiers.
- Add a naming profile through the existing village naming system. A proposed description is
  "A close community of orange-clay and red-sandstone courtyards, stepped roof terraces, shared
  households, acacia workshops and carefully protected water." Proposed fantasy tone examples
  are `Kestara`, `Oravel`, `Tavren` and `Sorela`; they are not fixed village names or implemented
  naming data. NPC awareness of an architectural description would require shared chat context;
  a naming prompt alone does not supply that awareness.
- Select the perimeter palette. Acacia is a compatible starting proposal; a newly authored
  wall and gatehouse family is optional rather than a prerequisite for playability.
- Verify a complete founded village through construction, housing and couple allocation, food,
  storage, recoloring, upgrades and save/reload. Individual building access checks do not replace
  this integration pass.

The initial biome recommendation is ordinary and eroded badlands, with deliberate mappings for
compatible modded biomes. Wooded badlands, savanna plateau and desert/oasis remain separate
reserved looks in the architecture roster. This is a proposal, not an activated biome mapping.
The center's existing well supplies water; no standalone well or large watchtower is needed for
this selected first version. This audit changes neither the live game nor the production catalog.

Aaron subsequently approved finishing this integration. The final catalog is documented in
[badlands-village.md](badlands-village.md), with reproducible production bindings in
`tools/structure/badlands-catalog-20260909.json`. All thirty structures remain in the approved
local datapack, outside public distribution. The shared runtime now supports house alternatives
and explicit standalone tiers, the Badlands naming profile and biome mapping, and plain banner
accents. P08.7 adds communal storage without a duplicate quartermaster job, keeping the center
and founding mine at the requested eight starting positions. Native verification and the
subsequent local release are recorded under `run/badlands-integration/`.

The completed integration passed 424 unit tests, 240 actual placements across both construction
paths and all rotations, a real restart preserving 240 buildings and 48 original entities,
twelve upgrade fits, and four complete founding rotations. The founding checks include the
eight starting positions, room ownership and storage, separate bell and campfire locations,
village persistence and natural biome selection. Rendered market awnings were also checked
with deliberately different primary and secondary colors.

### Desert and oasis selection pass: September 9

Aaron selected the CTOV Desert Oasis town center and nine homes spanning D05.3 through
D06.3. D05.2 was initially selected as a home, then reassigned as the tavern. The remaining
homes are alternative designs at three source scales, not a nine-step upgrade ladder.
Their private interior-authoring pass is complete: the set contains fifteen beds, thirteen as
singles and two as the one couple room in D05.5. Eight bed entitlements use the village primary
color and seven use the secondary color. All fifteen personal containers are registered with
their beds, including intentional shared-room storage. White beds remain the neutral-template
placeholders; the live gallery previews primary as blue and secondary as yellow. D05.3 preserves
Aaron's removal of the source building's middle bed and the other live interior edits are also
preserved. Their final level and cost bindings are recorded in the Desert catalog manifest.

| Exhibit | Source design | Selected use |
| --- | --- | --- |
| D05.1 | Town Center | Village center |
| D05.2 | Big 1 | Tavern |
| D05.3 | Big 2 | Home |
| D05.4 | Med 1 | Home |
| D05.5 | Med 2 | Home |
| D05.6 | Small 1 | Home |
| D05.7 | Small 2 | Home |
| D05.8 | Small 3 | Home |
| D06.1 | Small 4 | Home |
| D06.2 | Small 5 | Home |
| D06.3 | Small 6 | Home |

The same pass selected the following service and workplace shells. D01.7 is storehouse level 1
and D03.8 is its level-2 vault upgrade. D06.5 is the selected butchery. The larger fenced D07.1
loomhouse remains a reference only.

| Exhibit | Source design | Selected use |
| --- | --- | --- |
| D01.7 | Small 1, converted in the gallery | Storehouse level 1 |
| D03.8 | Vault | Storehouse level 2 |
| D03.3 | Observatory | Watchtower |
| D03.4 | Orchard | Lumberjack lodge |
| D02.8 | Fishing Oasis | Fishery |
| D02.9 | Fletcher | Hunting lodge |
| D04.12 | Well | Well |
| D06.4 | Bakery | Bakery |
| D06.5 | Butcher | Butchery |
| D06.6 | Farm | Farm |
| D07.5 | Pen, edited in the gallery | Mine |
| D07.2 | Mason | Stoneworks |
| D07.6 | Smith | Blacksmith |
| D07.7 | Temple | Church |
| Existing Desert market levels 1 through 3 | Existing Kithkyn structures | Approved Desert Oasis restyle; production market progression |

A reversible Desert Oasis restyle of the three existing markets is live in gallery row
D12 at `(3793,230,1365)`, `(3815,230,1365)` and `(3844,230,1365)`. It preserves the one-,
two- and three-stall geometry, work blocks and storage while unifying the set around sandstone
counters, acacia and spruce framing, white canvas, orange, cactus green and ochre awnings, and
arid potted plants. Five rendered views passed the visual review: the upgrade progression is
clear, the tier-three ochre stall makes a useful focal point, and the repeated white canvas keeps
the three colors coherent. Aaron approved all three on September 9. The reviewed binaries are now
the production `market_desert_1`, `market_desert_2` and `market_desert_3` structures, with the
superseded production files retained in the private review archive. The live world region was
backed up before placement.

The home and storehouse review captures, neutral exports and exact amenity mappings are recorded
in `tools/structure/desert-houses-20260909.json` and
`tools/structure/desert-storehouses-20260909.json`. D01.7 keeps Aaron's conversion into a compact
three-container storehouse and intentionally has no resident bed. D03.8 has eight communal
barrels, one private staff barrel and one primary-color quartermaster bed. Its neutral export
replaces twenty-eight ground-layer sand blocks with stable sandstone and adds a nine-block
foundation strip so the level-2 solid footprint contains D01.7 in every rotation. D05.5's neutral
export lowers two staircase cells just enough for the maximum-size villager while retaining the
upper landing. These functional export repairs do not replace the preserved live source captures.

All nine home interiors and both storehouses passed rendered review. The complete native home
fixture passed thirty-six rotated structures, 120 planned access routes, sixty physical room
walks and assigned-bed sleeps, sixty personal-container deposits and zero failed placements with
the maximum-size probe. The storehouse fixture passed eight rotated structures, all four upgrade
fits, sixty access routes, eight quartermaster-station walks, forty-four communal-container
deposits and transfers, four staff-bed sleeps, four private-container deposits and zero failed
placements. The audit reports are under `run/desert-house-verification-20260909-221500/` and
`run/desert-storehouse-verification-20260909-221300/`.

The completed private catalog contains exactly 28 buildings: one center, nine house alternatives,
two storehouses, three market tiers, and thirteen service buildings. The home set is grouped as
six level-1 choices, two standalone level-2 choices, and one standalone level-3 choice; those are
alternatives rather than a nine-step ladder. The only Desert upgrade chains are D01.7 to the D03.8
storehouse vault and the three existing market tiers. Desert no longer borrows missing Plains
buildings.

All thirteen services now have durable neutral captures, exact amenities, costs, grants and
identity slots. D07.5's miner station is local `[3,1,3]`; its authored north-facing shaft begins
two cells toward the front. A narrow fence-and-trapdoor throat was cleared in the exported copy,
and maximum-size villagers completed descent and return in every rotation. D03.3 retains its
spiral stair for the climb to the guard post at `[3,13,8]` and adds a visually inspected internal
ladder for a reliable return to the lower bed and personal chest. D04.12 remains a sealed
decorative basin: it has no villager endpoint, and construction grants WATER directly.

The final service fixture passed 48 rotations, 168 planned access routes, 44 physical bed walks,
44 personal-container deposits, 48 work-station walks, 32 communal-container transfers and eight
mine descent/return walks with zero failures. The strict regional fixture passed all 28 native
templates, all nine housing choices, twelve rotated upgrade fits, four founding rotations and
save reloads, natural biome selection, five founding beds, four founding jobs, room colors and
actual banner patterns. The watchtower, mine and sealed well were rendered from the final generated
assets after their navigation checks.

The runtime naming path needs no Desert-specific follow-up. Its profile describes sheltered
courtyards, pale masonry, shade and hospitable traders; generated names may use that tone without
copying the examples. The deterministic fallback uses starts such as `Dara`, `Sere`, `Amar`,
`Tala`, `Sola` and `Nara`. Sandy Desert biomes select the persisted `desert` style, and
`/kithkyn create-village <pos> desert` can force a sample settlement. Mesa and badlands select the
separate Pueblo catalog.

Desert walls use one sandstone stage with oak trapdoors and ordinary ladders; there is no second
wall upgrade. Exact selected-asset hashes and verification evidence are recorded in
`tools/structure/desert-catalog-20260909.json` and [desert-village.md](desert-village.md). CTOV
derived NBT remains in the private local datapack and is not copied into shipped resources without
written redistribution permission. The three Kithkyn-authored Desert market assets do not have
that dependency.

On September 10, Aaron changed the wall material choice to smooth sandstone for Desert and
smooth red sandstone for Mesa. The source palette now uses smooth full blocks, stairs and
slabs; narrow railings keep the matching sandstone wall block because Minecraft has no smooth
wall-block variant. The editable workshop rings were changed in place, preserving the current
geometry and all other blocks. The comparison checked 1,238 blocks per ring with zero unexpected
differences. The live runtime palette awaits the next mod deployment while the user continues
editing these wall samples.

### Jungle building showcase: September 9

Aaron broadened the current Pueblo coverage to mesa/badlands and savanna families while
keeping sandy deserts with Desert and birch families with Birch. These recognizable biome
families use deterministic assignments; explicit style tags can narrow coverage as future
catalogs become playable. Existing villages retain their saved styles.

The new jungle review collection extends the dry gallery eastward, with an entrance at
`4881.5, 230, 986.5` and a walkable connection beginning at x=4718. It contains 189 exhibits
in 24 rows and eight wings: T&T Tribal and Polynesian; CTOV ground and tree villages;
Dungeons & Taverns jungle villages, taverns and ruins; and jungle forts and lookouts.
Each exhibit has a stable JA–JH identifier, a label and a separate editing footprint.
The large ruined temple combines its original foundation and upper temple pieces.

All source captures and display NBT remain private. The public provenance and exact positions
are recorded in `tools/structure/jungle-showcase-20260909.json`. Optional integration buildings
whose blocks are unavailable were omitted, rather than displayed with holes. Source markers,
loot and executable commands were removed through the existing native display exporter.
Selection of a playable Jungle catalog remains a later authoring step; the showcase itself
does not activate Jungle as a village style.

All 189 placed exhibits passed the final block comparison, including restored hanging vines.
The only block-name differences are Minecraft’s grass-to-short-grass data migration. All
25 floating headings passed native codecs, and eight rendered views were inspected.

### Jungle Tribal selections: September 10

Aaron selected the JA Tribal family for the first Jungle village. The spoken “GA” identifiers
match the JA exhibit numbers and building names. Native captures preserve nineteen selected buildings and two unassigned candidates,
including the later service-building edits. Exact capture hashes, bounds,
bed heads, household containers, and intended village identity slots are recorded in
`tools/structure/jungle-selections-20260910.json`. Captures remain private under
`run/jungle-showcase/selection-20260910-tribal/` and
`run/jungle-showcase/selection-20260910-services/`; their padded bounds are not final building
envelopes. No production Jungle catalog is activated by this selection record.

| Exhibit | Selected use | Current beds and authoring decisions |
| --- | --- | --- |
| JA01.1 Meeting Point 1 | Village center | No beds. Village banners and four jobs: guard captain, builder, quartermaster, miner. Bell meeting area and campfire cooking area remain distinct amenities. |
| JA01.4 Animal Pen 1 | Lumberjack station | No bed. Shared chest; the dead bush at local [4,1,7] marks a jungle sapling planting site. |
| JA01.5 Armorer 1 | Mine entrance | Edited mine with two shared material barrels. Preserve the extension beyond the original source width. |
| JA01.6 Butcher 1 | Tavern | No bed currently; innkeeper can use general housing. |
| JA01.7 Cartographer 1 | Two-person treehouse | Two separate bedrooms on different floors, each with a personal chest. Lower bed is primary; upper bed is secondary. |
| NH06.5 Firewatch Jungle | Watchtower | One upstairs guard bed and personal chest. Four corner lanterns and the removed upper trapdoor are preserved. Guard access and firing position still need verification. |
| JA01.8 Fisher 1 | Fishery | One secondary-color bed. Barrel is workplace storage. No separate chest was found in the current capture. |
| JA02.1 Fletcher 1 | Hunting lodge | No bed. Two village banners. This fills the missing hunting role; Shepherd remains the butcher. |
| JA02.2 Large House 1 | Two-person house | Two separate single beds with their own nearby chests. |
| JA02.3 Leatherworker 1 | Couple home | Two adjacent upstairs beds share the primary color and the upstairs personal chest. Three village banners. |
| JA02.5 Mason 1 | Stoneworks | No bed. Existing stonecutter and shared chest. |
| JA02.6 Shepherd 1 | Butcher | One primary-color bed, nearby private chest, two village banners. |
| JA02.7 Small Farm 1 | Farm | No bed. Existing composter and shared barrel. |
| JA02.8 Small House 1 | Bakery | No beds. Floor smoker and shared barrel. |
| JA02.9 Small House 2 | Single-person house | One bed and private chest. |
| JA03.1 Small House 3 | Storehouse I | No beds. Two ordinary chests and one trapped chest are communal storage; both banners receive village heraldry. No second tier. |
| JA03.2 Small House 4 | Single-person house | One bed. |
| JA03.3 Temple 1 | Church / cleric | No bed. Existing brewing stand and shared chest. |
| JA03.5 Weaponsmith 1 | Blacksmith | No bed. Existing furnaces and shared chest. |

Every selected bed must use a primary or secondary village color, including both halves of
each bed. Both beds in the couple hut use the same color. All selected banners receive village
heraldry. These assignments are recorded intent; the captured NBT and live gallery retain their
current colors until the reviewed production/preview export. Household container associations
follow the explicit floor-barrel instruction and nearby bedroom chests; physical access still
requires verification. Six rendered views were inspected, including the center, couple hut,
fishery room and unused candidate exteriors.

The five housing designs provide eight beds: six singles and one two-person couple room.
The butcher, fishery and Firewatch tower add three staff beds. The center contributes none.
Aaron confirmed **four separate starting homes**. The proposed composition is two copies of
Small House 2 and two copies of Small House 4, each with one ordinary bed. These house the
four core workers without consuming the couple home. Exact placement still needs the shared
founding-layout extension described below.

The existing founding layout currently places only the center, mine and storehouse. Supporting
starting homes requires extending that shared layout and its placement validation. Also,
the current miner derives its shaft from its job building: adding a miner station directly
to the center would make it dig there. The requested design needs a single miner vacancy owned
by the center and a separate physical worksite at JA01.5, without a duplicate mine vacancy or
a fallback shaft beneath the plaza. Exact job anchors, mine orientation, terrain placement,
four-rotation navigation, worker housing and founding behavior remain implementation work.

The service selections are now complete: JA03.1 is the only storehouse tier, JA01.4 is the
bedless lumberjack station, and JA02.8 is the bedless bakery. JA02.4 Library 1 and JA03.4
Toolsmith 1 remain unused candidates. Three Jungle market tiers and one Jungle wall ring
are placed as editable previews of the reviewed shared layouts. The current fishery has the workplace
barrel but no personal chest; do not silently relabel that barrel as private storage.

Aaron approved stripped bamboo with each original hay block's orientation preserved. All 831
hay blocks across the 16 affected selected exhibits have been replaced and captured; the other
two selections contained no hay. No custom thatch block or new dependency is needed. Comparison
controls and unused candidates remain reference exhibits. Polynesian remains a separate future style.


The Jungle wall-and-market editing court is at **4970.5, 230, 977.5**, north of the
Jungle concourse. Its entry sign is at **4895, 230, 981**. The wall ring is west of the
path; the three market tiers are east. Jungle timber provides the wall body and gate frames;
bamboo mosaic forms deck accents and market panels. Oak access trapdoors and the ladder
routes remain. The markets retain the existing raised barrels, open entrances, station counts,
and primary/secondary cloth slots. These are vanilla-block adaptations and need no extra mods.

Native readback matched all **2,194** preview blocks. The wall overview, gate detail, and
three market tiers were rendered and visually reviewed. This stage verified placement, before
Jungle founding and worker navigation were implemented. The editable assets,
source hashes, definitions and verification are recorded in
`tools/structure/jungle-workshop-20260910.json`.

### Desert tavern/well comparison and castle: September 10

Aaron prefers **T02.9 Tavern Building** and **T02.8 Well Desert** from Dungeons & Taverns,
and requested a comparison with the current Desert choices before replacement. The current
tavern is **D05.2 Oasis Big House**, with two beds; T02.9 has five. The current well is
**D04.12 Desert Well**, eight by seven blocks; T02.8 is five by five and contains five
waterlogged blocks. The proposed structures' current live states were captured before
preparing any review copies.

A separate comparison court is placed at **4240.5, 230, 980.5**, just north of the dry-biome
concourse. Facing north from its entrance, current buildings are on the left and proposed
buildings are on the right; wells are nearest the entrance and taverns are behind them.
The native comparison checked all 2,044 template blocks with no differences. Four rendered
views were inspected. Exact sources, hashes and locations are recorded in
`tools/structure/desert-comparison-20260910.json`.

The subsequent decision retains D04.12 as the current well, converts D05.2 to the
couple home `couple_cottage_desert_1`, and adopts T02.9 as `tavern_desert_1`. D05.2 has
one shared-color pair and shared upstairs personal storage, with no job. T02.9 has one
innkeeper bed with its nearby ground-floor chest, four general beds, and one communal
chest. The Desert catalog now contains 29 definitions. The two changed assets passed
native placement in all four rotations through instant and incremental construction;
all beds, personal storage, communal storage and the innkeeper station passed physical
access checks. Their definitions are live after datapack reload. The records are in
`tools/structure/desert-tavern-adoption-20260910.json`.

Aaron also edited **R07.2 Desert Fort** into a proposed castle and explicitly chose to
configure its authoring draft while designing the ruler and jail systems first. Its eight
beds, ten village banners, two rooftop tent palettes, shared and personal storage, proposed
guard positions, smithy and jail area are recorded in `tools/structure/desert-castle-20260910.json`.
Blue represents primary and yellow secondary in the live preview. The complete color pass
preserves the structure's 3,246 blocks exactly apart from declared identity changes. All eight
complete beds were checked after restoring the halves affected by Minecraft's neighbor updates;
temporary bed item drops created by recoloring were identified against the earlier entity
snapshot and removed by their exact UUIDs.

The intended room split is a royal couple's suite, one separate lower bookshelf room for the
existing captain, and five general single beds. These are intended role reservations, not
enforced reservations in the running village. The royal beds sit under stairs, so both sides
need physical access testing before production use. Castle eligibility, ruler decisions,
succession, captain accommodation transfer and possible player custody remain in the
[castle design](castles.md); no active ruler or jail behavior was added in this pass.


### Arid candle walls: September 10

Aaron's Mesa gate edits define the shared arid trim: forty chiseled frame cells per
gate, removal of the last two standing gate-roof lamps, and lit candle clusters of two
to four everywhere other standing lanterns were placed. Four hanging lanterns remain
at each gate. Mesa uses orange candles and chiseled red sandstone; Desert uses regular
candles and chiseled sandstone. Wall bodies, stairs and slabs remain smooth masonry,
and oak trapdoors and ladders remain unchanged. The extra four corner ladder rungs
Aaron added were saved and mirrored into the Desert editing copy.

Both editable rings have 160 chiseled blocks, 38 candle clusters and 16 hanging lanterns.
Native before/after captures matched every expected block, and both rings and gate details
were photographed and viewed. The shared generator source uses the same frame coordinates;
all eight guard ascent/descent routes passed for each style. The matching generator was
deployed on September 10 with the shared-recipe update; the live samples are also updated.

### Desert temple and watchtower comparison: September 10

A separate court north of the tavern-and-well comparison contains four review copies.
Enter at **4240.5, 230, 917.5**, or use the new travel sign at **4234, 230, 980**.
Current selections are on the west/left; alternatives are on the east/right:

- Temple: **D07.7 Desert Oasis Temple** (CTOV, `church_desert_1`) beside
  **R01.8 Temple Plaza 16** (Dungeons and Taverns).
- Tier-one watchtower: **D03.3 Desert Observatory** (CTOV, `watchtower_desert_1`)
  beside **V03.7 Desert Small House 6** (vanilla).

Current copies use the production templates, including the watchtower access changes.
The alternatives were captured fresh from the live gallery; the original exhibits are
untouched. Comparison beds are blue, containers are empty, and template entities are
omitted. Native captures matched all 2,794 placed blocks after normalizing empty block
property maps. The live overview and detail screenshots were rendered and reviewed.
These are comparison copies only; neither production selection changed.
The placement record is `tools/structure/desert-temple-tower-comparison-20260910.json`.

### Nilotic and floodplain showcase: September 10

Aaron asked for a curated collection around the Towns & Towers Nilotic village, in the way
the dry-biome and jungle galleries gather every source mod's take on one biome family. The
Nilotic palette is packed mud, mangrove wood and thatch-like spruce slabs, so the collection
takes the floodplain, swamp and mangrove families from every installed structure jar and
orders them by material kinship with that palette. Towns & Towers Iberian and Mediterranean
were left out as a brick-and-quartz language, Millenaire for its non-vanilla marker blocks,
and every CTOV piece that needs compatibility-mod blocks (100 files) was excluded rather than
shown with holes.

The collection extends the jungle gallery eastward, with an entrance at `6561.5, 230, 986.5`
and a walkable concourse beginning at x=6398. It holds 196 exhibits in 26 rows and six wings
with stable NA–NF identifiers: the Nilotic houses and tower; CTOV's mud-brick swamp outpost
with the Dungeons & Taverns mangrove huts, firewatch tower and mud-brick remnant; the mangrove
tavern grounds; CTOV's swamp and fortified-swamp villages; the Dungeons & Taverns swamp village
and swamp tavern grounds; and the Towns & Towers boat village beside YUNG's witch huts. Exact
source duplicates are kept once. Selection of a playable floodplain catalog remains a later
authoring step; the showcase does not activate any village style.

All source captures and display NBT stay private under `run/nilotic-showcase/`. The public
provenance, positions and jar hashes are recorded in
`tools/structure/nilotic-showcase-20260910.json`. Placement used a per-gallery driver with a
private acknowledgment namespace, because the shared placer's marker key can be overwritten by
a second live session; the world was backed up first and the concourse joins the jungle
concourse at x=6397.

All 196 placed exhibits passed the native block comparison against their prepared templates
(47,214 blocks). The only differences are seven grass blocks in one exhibit that Minecraft's
data migration renamed to short grass; no vines were lost and nothing structural is missing.
All 27 floating headers exist exactly once at their intended scale, and ten rendered views were
inspected. The placement run was interrupted once by a second live session sharing the console
and once by a stray search process reading the console pipe; both are recorded in the private
verification summary, and no exhibit was placed twice.

### Unstructured showcase: September 10

The Unstructured collection extends the gallery eastward from the Nilotic concourse.
Use the **Unstructured** travel sign at the original gallery hub (`2027, 230, 976`),
or enter at **7857.5, 230, 986.5**. All 74 source templates appear in 15 rows across
six wings: ocean village, camps and piglins, towers and landmarks, desert graves,
temple and dungeon, and passages and piers. Modular sections are labeled as pieces;
this is a reference collection, not an assembled world-generation dungeon.

The source is Unstructured 0.5.8 for Forge/Minecraft 1.19.4, official CurseForge file
4443672. Its vanilla-block templates were migrated with Minecraft's native data fixer
for display in 1.21.1. The older mod was not installed. Original binaries and display
copies remain private under `run/unstructured-showcase/`; source licensing, hashes,
positions and stable exhibit identifiers are recorded in
`tools/structure/unstructured-showcase-20260910.json`.

Native readback checked 46,763 blocks after adding 109 invisible supports for otherwise
unsupported decorations and a collapsing trap floor. One stone brick moved one block
under its template's working piston; its destination was verified. Other differences
are normal block-state updates such as connected fences and flowing water. Spawners,
TNT, source mobs and container loot were removed for the display copies. Six exterior
views and the stabilized interior details were rendered for visual review. Existing
exhibits and edits remain in place. No production village catalog changed.


### Desert temple and watchtower selections: September 10

The edited alternative R01.8 Temple Plaza 16 replaces D07.7 as the Desert temple.
The brewing stand and communal chest were captured from the comparison court. V03.7
becomes watchtower I, while D03.3 becomes watchtower II with a compatible upgrade path.
Both use ladders; the smaller tower has widened landing openings after a real adult
guard caught its head beneath the initial one-cell opening. The corrected designs
passed construction, physical access, sleep and container-use checks in all rotations,
and the rendered temple, tower pair and ladder interior were inspected.

The court at **4240.5, 230, 917.5** now labels these selections. The temple on the right
is selected; watchtower II is on the left and watchtower I is on the right. D07.7 remains
as a comparison reference. The public provenance and exact private template hashes
are in `tools/structure/desert-temple-tower-adoption-20260910.json`.


### Jungle roof material comparison: September 10

Aaron compared stripped bamboo before deciding against introducing a custom thatch block. Six preview
copies stand east of the Jungle wall/market workshop, entered through the **Jungle Roofs**
sign at **5084, 230, 981** (arrival **5084.5, 230, 977.5**). Fresh captures of JA02.9 Small
House 2 and JA02.2 Large House 1 appear in three columns: original hay, stripped bamboo with
its ends facing up, and stripped bamboo running along the X axis. Only the hay-block material
changes between the comparison copies. Native readback matched all 1,029 placed structure blocks.
These comparison copies remain available after adoption in the selected exhibits.

The approved replacement is `minecraft:stripped_bamboo_block`, preserving the original hay axes:
714 vertical, 70 along Z and 47 along X. Fresh native captures verified all 831 replacement blocks
and compared 4,569 structure blocks. The only other changes were six naturally grown vines and
the expected loss of the center campfire's signal-fire state after removing the hay below it.
The latest captures are bound in the Jungle selection manifest; production integration remains
outstanding. Provenance is in `tools/structure/jungle-bamboo-adoption-20260910.json`; comparison
coordinates remain in `tools/structure/jungle-roof-comparison-20260910.json`.


### Jungle wall lighting approval: September 10

The Jungle wall preview is approved with torches replacing its candle clusters. All 38
candle blocks were replaced with standing torches at their existing positions, and the
reviewed neutral and preview templates were updated from a fresh live capture. Native
readback matched all 1,234 wall blocks. The editable ring remains at **4895, 230, 890**
in the Jungle wall/market workshop. Lighting uses ordinary torches; the wood, bamboo,
gates and access layout retain their reviewed design. The current wall asset hash and
verification are recorded in `tools/structure/jungle-workshop-20260910.json`. Production
Jungle village integration remains separate from this authoring approval.

### Current closeout status: September 10, after bamboo approval

The selected Jungle exhibits now use stripped bamboo in place of every hay block, preserving
the original axes. Native readback and three viewed live screenshots confirm the change.
Their latest immutable captures are referenced by the selection manifest. No custom thatch
block is planned for this pass. The wall torch replacement is also saved and visually checked.

Shared category/level construction recipes are defaults, not a restriction on future designs.
An explicitly authored building `cost` array replaces the full default and uses the same
validator. All current bundled definitions inherit the defaults. The optional-override runtime
passed `check build` (433 tests, zero failures, four skipped) and was deployed to the local
server and client, retaining the world and galleries. All 200 effective definitions loaded
without recipe rejection. This is a working-tree deployment, not a Git commit or push.

| Work | Current state |
| --- | --- |
| Pueblo/Mesa and Desert catalogs, selected Desert tavern/temple/towers and arid walls | Verified and locally deployed; earlier entries saying these await deployment are historical. |
| Unstructured structure gallery | Placed, verified and available for review. |
| Jungle buildings, three market tiers and one wall tier | Verified as a complete private catalog; bamboo, market repairs and torch-lit timber walls are applied. |
| Playable Jungle villages | Complete: four-home founding sprawl, center-owned miner and quartermaster routed to separate worksites, identity export, access, construction, restart and natural-founding checks all pass. |
| Castle authoring | Edited fort, room/bed/container intentions, banners and tent colors captured and visually reviewed. |
| Castle gameplay | Not buildable yet. Navigation and role reservations need implementation/verification; eligibility, pricing, ruler succession and jail/release rules remain design work. Ruler and jail implementation was explicitly deferred by Aaron. |

The castle's recorded royal/captain bed intentions and guard anchors are not enforced runtime
assignments. See [castles.md](castles.md) for the design questions and implementation boundary.

### Floodplain donor references: September 10

While Aaron assigned roles in the NA wing (temple, centre, mine, blacksmith, mason, hunting
lodge, lumberjack lodge, butcher with livestock, homes) and rebuilt NA01.2 as the mine, three
roles had no Nilotic source: farm fields, a watchtower decision, and water. An editable copy of
Small House 2 was placed as NA02.4 for his storehouse draft, so the Nilotic collection now shows
197 exhibits. The lumberjack's tree should be authored as a jungle sapling on dirt, not a
mangrove propagule: the wood loop refuses propagules because a grown mangrove leaves roots on
the stand.

A donor gallery of 247 exhibits in 38 rows and three wings (NG farms and pens, NH watchtowers,
NI wells, fountains and pools) gathers those roles from every culture in Towns & Towers, CTOV,
Dungeons & Taverns, vanilla, YUNG's Extras and Terralith. Its entrance is at `9153.5, 230, 986.5`
beyond the Unstructured showcase, which the Codex session placed east of the Nilotic gallery
in the meantime; the first donor base collided with it and the gallery was relaid after a sky
probe. All 247 exhibits passed the native block comparison (93,919 blocks) apart from the
grass rename, three namespace-less air entries in Terralith templates, and one unsupported
wheat block in the Mesa fortified farm. The public record is
`tools/structure/nilotic-donors-20260910.json`; captures and display NBT stay private under
`run/nilotic-donors/`.

### Market counter and color repair: September 10

Markets retain their original tent, decorative banner, rug and candle colors independently
of the village palette. Jungle and Mesa keep orange/cyan/red and white fabrics; Desert
keeps its original orange/green/yellow and white. Regional timber, masonry and bamboo remain.

The earlier access edits removed counter trapdoors and left carpet gaps. All three tiers
of all three styles now retain the complete authored pattern. Aaron’s tier-three Mesa
railing repair and added orange rug were captured before repair and preserved. Unsupported
Desert carpet pieces received matching sandstone support. The existing raised barrels stay.

The underlying navigation defect treated closed trapdoors as passable nodes, directing
villagers through counters beneath low roofs. Closed panels now obstruct those nodes;
villagers use the existing stall entrance. Native regression checks reject the closed panel
and retain the usable aperture of an open side panel. All nine markets passed four rotations
with the largest adult collision body: 144 access routes, 72 physical station walks,
72 shared-container visits and 144 real item transfers, with zero failures.

The live review copies match all 2,977 saved non-air blocks exactly. All three styles were
rendered and visually checked. The older three P13 Mesa exhibits were repaired as well.
The Desert/Mesa comparison court starts at **5203.5, 230, 976.5**; Desert is west and
Mesa east, with tiers 1, 2 and 3 from north to south. The Jungle workshop remains at
**4970.5, 230, 977.5** and links to this court.

Current assets, native results, immutable user-edit captures and viewed screenshots are
recorded in `tools/structure/market-review-20260910.json`. Earlier market entries describing
removed rails, slab counters or village-colored awnings are superseded by this review.
At this point in the review, Jungle remained an authoring collection pending its later
production/founding integration.

The market repair passed `check build` with 433 tests, zero failures and zero skips, then
was deployed to the local server and Prism client. Both use SHA-256
`a70324b51afc4f9f9ffdd393a17c9717b8e3cde3064b10b72de9932e51822015`; all 200 effective building
definitions loaded. The world and galleries were backed up and retained. This remains
a working-tree deployment, without a commit or push.

### Jungle watchtower candidate: September 10

Aaron nominated his edited **NH06.5 Firewatch Jungle** from the Nilotic donor gallery
as a possible Jungle watchtower. The current selection remains **JA01.7 Cartographer 1**
from Towns and Towers, with one guard bed, a nearby private chest and an upper shared chest.
The Firewatch comes from Dungeons and Taverns and has one upstairs bed and chest, a ladder
and a raised lookout balcony under a bamboo roof. Its occupied envelope is 9 by 18 by 9,
compared with JA01.7’s 14 by 22 by 12.

Both live structures were captured natively and visually compared. NH06.5 retains the four
new corner lanterns and the removed upper trapdoor. The capture also preserves live vine
and cocoa growth; those differences are not all attributed to player edits. The candidate
remains at **9428, 230, 1215**; JA01.7 remains at **5004, 230, 1010**.

`tools/structure/jungle-tower-comparison-20260910.json` records immutable captures, hashes,
bed/container coordinates and comparison images. No tower selection was replaced in this
review. Replacement versus an additional design/tier is undecided; adoption still needs
guard/identity bindings and native ladder, landing, bed, storage and firing-position checks.

Later on September 10, Aaron assigned the remaining floodplain roles from the donor gallery:
the Polynesian large and small farms (NG04.1, NG04.2, with a barrel as the crop workstation),
the CTOV desert oasis pool as the fishery (NI09.3), the D&T mangrove tavern well as the well
(NI03.9) and the D&T savanna firewatch tower as the watchtower (NH06.7). No tavern and no
bakery for this village. Editable copies of the pool, well and tower stand on the NA03 row
beside the Nilotic tower, restyled toward the Nilotic palette by block-type substitution that
keeps every orientation (mud, mangrove, spruce, jungle trapdoors, andesite, mud bricks, green
beds), and the three approved Desert market tiers were copied and restyled the same way as the
NA04 row. Because the Nilotic houses seat their doors on the ground layer, a patchy earthen
ground course (packed mud, mud, coarse and rooted dirt) now lies under every house footprint in
the gallery; `run/nilotic-showcase/floors-20260910.json` records it for a sink-1 capture. All
spoken assignments live in `run/nilotic-showcase/selections-draft.json` until the edited
exhibits are captured with hashes.

Aaron requested an in-world side-by-side comparison. Editable copies now stand at
**5280.5, 230, 971.5** (viewing entry), with JA01.7 on the left and NH06.5 on the right.
The **COMPARE TOWERS** sign beside the original NH06.5 at **9432, 230, 1210** teleports
to the comparison; a **JUNGLE TOWERS** shortcut also stands by the Desert/Mesa markets at
**5237, 230, 976**. A return sign leads back to the Jungle workshop.

The originals were retained. Both copies use the saved edited captures at the same base
elevation, and native readback matches all 1,442 non-air blocks exactly. A rendered
side-by-side screenshot was inspected. The comparison is recorded in the tower review
manifest; no production tower selection or gameplay binding changed.

### Jungle treehouse and Firewatch selection: September 10

Aaron selected **NH06.5 Firewatch Jungle** as the watchtower and retained **JA01.7
Cartographer 1** as a **two-person treehouse**. He added the second bed upstairs in the
comparison copy. Fresh native captures preserve both selected buildings; the original
gallery copies remain reference exhibits. The comparison labels now identify the house
and watchtower, rather than a current/candidate tower choice.

The treehouse has two independent single bedrooms on separate floors, with one personal
chest per room. Its lower bed receives the primary village color, its upper bed the
secondary color; it has no guard station or worker-reserved bed. The Firewatch retains
one primary-color guard bed and its personal chest, plus the four corner lanterns and
removed upper trapdoor. These are authored role and identity bindings pending production
export; raw captures keep the player’s placed bed colors.

The selected Jungle collection now has **19 buildings, 11 beds and eight general-home
beds**. All 21 selected/candidate capture hashes and selected bed/container coordinates
were revalidated. Four single-bed founding huts remain the starting layout. At the time of
this capture, production integration and native resident/guard access verification remained.
See `tools/structure/jungle-selections-20260910.json` and the tower comparison manifest
for the authoritative selection and latest captures.

The selected pair and newly added upstairs bedroom were rendered and visually inspected.
The bed, adjacent personal chest and updated live labels match the captured selection.

### Floodplain selection captured: September 10

With the storehouse finished and the ground courses cornered by hand, the floodplain set was
captured from a flushed world snapshot: 22 native captures under
`run/nilotic-showcase/selection-20260910-floodplain/`, each hashed, with an amenity audit of
beds, containers, work blocks, banners and coloured blocks relative to every gallery origin.
The public record is `tools/structure/nilotic-selections-20260910.json`: 20 assigned buildings
(church, centre, mine, lumberjack lodge, hunting lodge, three homes, butcher, stoneworks,
blacksmith, storehouse, fishery, well, watchtower, three market tiers and the two Polynesian
farms) plus two unassigned candidates (the butcher-and-fisher house and the Nilotic tower),
ten beds in all. The centre carries four white banners and the green carpet that becomes the
primary colour; white beds and banners elsewhere are neutral identity slots. The lumberjack's
dead bush is recorded as the jungle-sapling planting marker. Aaron also proposed that villages
adopt allays as free quartermasters the way they recruit golems as guards, with this
storehouse starting with two; that is recorded as a design proposal with feasibility unchecked.
Production authoring (neutral exports, definitions, access fixtures) has not started.

Identity rules confirmed by Aaron after the capture: white banners are village banners (four on
the centre, four he added to the watchtower; its 23 brown banners are decoration), the centre's
green carpets become the primary colour, every bed is a colour slot, and two bed heads side by
side with the same facing are one couple unit in a single colour. The storehouse now holds a
note block as the delivery point for two allays that the village will adopt as auxiliary
quartermasters; that logic is being built on the `allay-quartermasters` branch. Apron sea
lanterns inside the padded captures are gallery furniture and must not be exported.

### Floodplain wall palette: September 10

The five wall drafts in the Nilotic showcase (straight, diagonal, terrace, corner tower,
gatehouse; teleport `/tp @s 6569.5 230 1146.5 -90 0`) now carry the decided floodplain
palette, applied by `run/nilotic-showcase/restyle-wall.py` and captured in the current
`tools/structure/nilotic-showcase-20260910.json` hashes. It mirrors the arid treatment
of the Desert and Mesa walls with wet-country materials:

| Arid element | Floodplain choice |
| --- | --- |
| Stone brick body, posts and deck | Mud bricks |
| Stone brick stairs and slabs | Mud brick stairs and slabs, same facing and half |
| Fence railings | Mud brick walls |
| Trapdoors | Jungle trapdoors, same facing and open state |
| Chiseled stone gate frame | Muddy mangrove roots on the gatehouse uprights and the beam over the arch |
| Candle clusters on the standing lantern cells | Brown candles, two to four per cell keyed to position the way `WallPalette.standingLight` keys them |
| Inward roof lanterns | Removed, as the arid palette removes them |
| Four lanterns hanging under the gate arch | Kept as lanterns |

The row overview and gatehouse close-up (`review-walls-final-na05`, `review-gate-final-na05`)
show the result: tan mud brick curtain with thin wall-block parapets, a dark roots frame around
the gate, candle clusters on every post top, and the two lantern pairs still lit under the arch.
This is the palette the runtime `FLOODPLAIN` case of `WallPalette.forStyle` takes once the
floodplain `VillageStyle` lands; the arid `isGateFrame` cell rule already describes the roots
cells, so only the material mapping is new.

### Floodplain wall approval: September 10

Aaron reviewed the restyled row and corrected one gatehouse cell by hand: the top of the
east end post (x 16, y 5, z 2) had come out as muddy mangrove roots and he set it back to
mud bricks. The cause was in the restyle pass, which guessed beam cells from the current
block instead of the authored piece; it now reads the wood template's log axes, so a log
standing on end at y 5 is a post top and stays mud bricks while the lying logs of the roof
rim remain roots. That is the classification `AuthoredWoodWallSegments.isGateFrame` uses,
so the west post top (x 5, y 5, z 2) took the same correction. A second defect surfaced on
the re-place: the server caches every placed template by id, so the third pass reused the
stale copy and put the roots back; `restyle.py` now places each pass under a fresh versioned
template id, as `replace-copy.py` already did. The row was diffed against its templates from
a flushed snapshot afterwards with no differing cells (`run/nilotic-showcase/diff-live.py NA05`),
and the corrected gatehouse was photographed (`review-gate-fixed-na05`).

The five sections are approved and captured into the floodplain selection record as `wall`
entries (`straight`, `diagonal`, `terrace`, `corner_tower`, `gatehouse`):
`tools/structure/nilotic-selections-20260910.json` now holds 27 native captures, 20 buildings,
five wall sections and two candidates, each hashed from the same flushed snapshot. The audit
lists the gatehouse campfire among its work stations; it is decoration.

### Old Village Life catalogs removed: September 10

Aaron decided to delete the whole old family: the bundled plains, taiga, snowy and savanna
catalogs and the old bundled desert set, 29 definitions and 29 templates each, plus the
developer placeholder market. The styles are now Birch Forest, Desert and Badlands, in that
order, with Birch the only bundled catalog and the default for any blank or unknown saved
style. Desert comes from the private `kithkyn-desert` datapack and Badlands from
`kithkyn-badlands`, both overriding the same ids the code resolves. Every style is strict: no
catalog borrows another family's building, the plains fallback is gone from the code, and
conventional biome mapping covers only the birch, desert/sandy and mesa/badlands/savanna
families; a hot, dry climate chooses between Desert and Badlands and every other climate
builds Birch. A village saved in a removed family keeps its name, people and identity, reads
as Birch, and treats its old buildings as absent: they stay in the save, provide nothing, and
one warning per load names them. The tools that derived the old families' level-2 mines and
cottage variants went with them.

### Floodplain catalog verified: September 10

The floodplain selection became a private catalog the same day: `run/floodplain-integration/prepare.py`
exported the twenty selected buildings from their captures as neutral templates with the earthen ground
course as layer 0, wrote their definitions, and baked the butcher's three cows and three pigs and the
storehouse's two allays into the templates through a new `entities` key on the native export tool. Three
cells changed in the export for the runtime's sake: the centre gained one campfire on its north apron,
and the storehouse doorway trapdoor and the watchtower's ladder hatch are open, because people open doors
and gates but never trapdoors. The runtime gained the `floodplain` style with the mangrove mapping, the
mud brick wall palette and a naming profile; the old Village Life catalogs were removed in the same branch.
The access run exposed two runtime defects the arid catalogs had hidden: climbers kept their approach
velocity while ascending a ladder and drifted off a free-standing rung, which `PersonPathNavigation` now
steers back onto, and idle allays aimed at the cell above their note block even when a candle sat there,
which the keeper behaviour now sidesteps. The access fixture also removes authored keepers for its walks,
since two allays in a small storehouse push the probe off a container mid-check.
Static checks, 470 unit tests and the five native modes (placement, restart, centre, access, founding with
natural founding in a mangrove swamp world) all passed; the public record is
`tools/structure/floodplain-catalog-20260910.json`.
The release went live the same afternoon: 104 building definitions, the seven old-family villages removed
after a flushed snapshot, and Ruwaleni founded at the nearest mangrove swamp (centre -3982 67 5344).
Aaron's first walk through Ruwaleni found four things, fixed and released the same evening: a floor candle
people pressed into (candles are obstacles now), keepers stuck at closed trapdoors (walls to them now), the
well one block high (it sinks one), and the mine ramp starting at the pit's edge (its middle column now). The
lumberjack also composts surplus saplings, and the quartermaster names the keeper. The site was founded again
as Mavulena.

### Storehouse re-edit: September 10

A copy of the production floodplain storehouse was placed on its own pad in the Nilotic gallery (row NA07)
for Aaron to edit. He removed the doorway trapdoor, so the doorway is open, and trimmed the earthen course
corners. The copy was captured from a flushed snapshot and became the storehouse's source capture; the
open-trapdoor export override is gone; the pack was rebuilt, checked and reinstalled live with a reload.

### Mine re-edits: September 10

Production copies of the floodplain and Desert mines were placed in the Nilotic gallery (rows NA08 and NA09)
for Aaron to edit. The floodplain pit grew one column west so the ramp has three columns of descent inside
the building, its door went, and a step leads down into the pit; the miner stands at the pit's west column
with the ramp mouth one column in. The Desert mine gained a shared chest at the pit floor plus two wall
posts, a fence, a trapdoor and a lantern, exported in its existing frame. Both were captured from a flushed
snapshot, re-exported, checked and installed in their packs.
### Jungle catalog verified: September 10

The approved Jungle selection is now a playable private catalog with 22 definitions: nineteen
selected buildings and the three repaired market tiers. Its bedless center owns the founding
quartermaster, builder, captain and miner vacancies. The starting planner places a mine, a
storehouse and four one-bed homes with ordinary growth placement, while the center's quartermaster
and miner route to non-vacancy physical worksites in those separate buildings. This also fixed a
real ownership error exposed by the center test: a routed miner post must not create a shaft under
the civic building; only the physical mine owns and excavates that shaft.

The final mine uses a three-block-wide eastward mouth, offset two blocks from its worksite so its
surface workstation, opening and final ramp headroom do not compete for one cell. Its two material
barrels and the butcher's personal chest moved to reachable positions. The native exporter now
writes block-entity data for chests and barrels created by export overrides, so strict template
loading sees the same containers as physical access does.

All five native checks pass. The access fixture walked 84 rotations and 224 routes, including every
bed, personal container, workstation, shared container and mine descent/return. The construction
fixture passed 176 instant and incremental placements across four rotations; a full server restart
retained all 176. The reviewed-village fixture passed 22 strict templates, all four founding
rotations, the market upgrades, natural Jungle selection, the four starting homes, routed worksites,
village identity and codec reloads. The public record is
`tools/structure/jungle-catalog-20260910.json`; implementation and operating details are in
[jungle-village.md](jungle-village.md).
