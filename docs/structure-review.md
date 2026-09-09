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
