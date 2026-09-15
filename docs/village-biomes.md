# Village biome roster

A **village biome** is an architectural catalog selected from a settlement's founding
environment and retained for the life of that village. It is separate from biome traits,
which describe environmental facts such as wet, cold, wooded, or coastal.

This roster tracks only three useful states. Runtime fallback behavior does not change a
catalog's status here.

## Playable

These catalogs are integrated, selectable, and verified as complete founding villages.

| Village biome | Founding environments | Direction |
| --- | --- | --- |
| Birch Forest | Birch Forest, Old Growth Birch Forest, and compatible modded birch biomes | Pale birch timber, mossy stone, intimate woodland buildings |
| Desert Oasis | Desert and sandy desert families | Smooth sandstone, shaded courts, wells, oasis planting, candles, and the optional castle |
| Pueblo | Badlands, Eroded Badlands, Wooded Badlands, and compatible mesa families | Dense adobe and terracotta housing, roof terraces, courtyards, and red sandstone walls |
| Floodplain | Mangrove Swamp and compatible tropical floodplain biomes | Mud brick, mangrove details, raised earth, and water-oriented sites |
| Jungle Tribal | Jungle and Bamboo Jungle | Jungle timber, stripped bamboo roofs, compact huts, treehouses, three markets, the timber wall, and the Firewatch tower |
| Swamp | Swamp, Orchid Swamp, and compatible ordinary wetlands | Mossy ruins, oak-and-spruce wetland buildings, two campfires, candlelit timber walls, and the configured SC01.2 castle |
| Mediterranean | Plains and Sunflower Plains | White stone and plaster town core with tile roofs, courts, orchards, gardens, and open agriculture |
| Tundra | Snowy Plains, Ice Spikes, snowy beaches, frozen rivers, and compatible exposed frozen lowlands | Compact snowbound buildings, packed-ice accents, spruce details, enclosed cold-weather farming, and four founding snow golems |
| Polynesian Coast | Sparse Jungle, and beaches beside warm or lukewarm ocean | Stilted stripped-spruce huts under oak roofs, a king's hall that keeps the village jail, tiki torches, a pond fishery, an open-air shrine, and a stripped-log palisade on coral footings |
| Romanian | Dark Forest, forested highlands, and wooded valleys | Steep birch roofs, heavy dark-oak frames, enclosed yards, substantial woodland homes, and timber-and-deepslate walls |
| Alpine Highlands | Meadow, Grove, Snowy Slopes, mountain peaks, and Windswept mountain families | Iberian-inspired brick and spruce settlement with berry plots, dense shared homes, deep wells, and brushed brick walls |
| Japanese Cherry Grove | Cherry Grove, Flower Forest, and compatible Sakura biomes | Flowering garden settlement with spruce frames, ponds, compact farms and cherry-leaf walls |
| Nautical Coast | Beaches on temperate or cold water, Stony Shore, and compatible coasts | A fishing town around a lighthouse: thatched sandstone-and-jungle-timber beach cottages, a jetty fishery on its own water, a turnover-ship castle, and a sandstone seawall on stripped jungle wood |
| Savanna Tent | Savanna, Savanna Plateau, Windswept Savanna, and compatible dry grasslands | A tent camp on the dry grass: the original white-canvas savanna tent as its centre, blue and yellow tipis and camp tents, a trader-tent storehouse, an acacia mine and stoneworks, vanilla's savanna temple and farm, and an acacia palisade on cobblestone |
| Rustic Woodland | Forest and compatible ordinary oak woodland biomes | Familiar oak buildings, working barns, a merchant-and-well center, open agriculture, and a clean stripped-oak palisade |
| Taiga | Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga, and compatible cold forests that are not snowy | A Viking camp in the conifers: the meeting point with its crossbow watch tower, stripped spruce homes and workplaces, the old-growth taiga fort with its jail and ruler's room, the firewatch tower, the spruce tavern, and a stripped spruce palisade on cobblestone |
| Mushroom | Mushroom Fields and compatible fungal biomes | A fantasy village on the mushroom island: the Towns & Towers meeting point with a cleric's loft under its cap, houses under red and brown caps on pale stems, oak gazebos on orange paving for the trades, mooshrooms in the pen, an oak stand for the lumberjack, and a wall of mushroom caps on a stem footing |

## Locked directions

These village biomes have an agreed environmental home and architectural direction. A locked
direction may already have a complete gallery selection, but it remains in this section until
its production catalog and founding behavior are verified.

| Village biome | Founding environments | Direction |
| --- | --- | --- |

No village biome is a locked direction today: every land variant on the roster has its catalog.

Jungle, ordinary Swamp, Mediterranean, Tundra, Polynesian Coast, Romanian, Alpine Highlands,
Japanese Cherry Grove, Nautical Coast, Savanna Tent, Rustic Woodland, Taiga and Mushroom are complete private production catalogs. Swamp remains
separate from the mud-brick Floodplain catalog used by mangroves. Tundra starts from a four-bed
centre with a naturally placed mine and storehouse and deliberately has no castle. The Polynesian
Coast centre is the king's hall, which seats the ruler and keeps the village jail; that catalog has
no castle yet. The Nautical Coast centre is a lighthouse whose four beds sleep the founding workers,
and its castle is the Towns & Towers beach camp's turnover ship. The Savanna Tent centre is the
original Village Life savanna tent, whose four beds sleep the founding workers; that catalog has no
castle and no watchtower. The Taiga centre is the Towns & Towers Viking meeting point, which has
no beds and carries a crossbow watch post on its tower; its founding set brings three homes, and
its castle is the old-growth taiga fort with the ruler's room and the jail.

Swedish and Polish are source languages in the current review rather than settled biome
assignments. The Swedish temple and tower remain a useful candidate church progression for a
Taiga catalog's church, should it grow one. Aaron chose the grittier T&T Viking family for the
Taiga on 2026-09-14; the ornate Polish family was not chosen. The Alpine review
moved away from the cyan-concrete Swiss set and locked the Iberian family in brick and spruce. It
remains distinct from forested Taiga and exposed Tundra.

Rivers do not receive a separate village biome. An ordinary land village resolves from the
surrounding environment rather than a narrow river strip. Tropical riverbanks and deltas belong
to Floodplain when site selection can classify them reliably.

## Next authoring shortlist

Tundra, Polynesian Coast, Romanian, Alpine Highlands, Japanese Cherry Grove, Nautical Coast and
Savanna Tent, Rustic Woodland, Taiga and Mushroom are complete. Taiga was parked until the ornate Polish and grittier
Viking directions could be judged; Aaron judged them on 2026-09-14 and the Taiga catalog is
complete on the Viking family. Mushroom, the last roster variant, was selected from its gallery
the same evening and closed out on 2026-09-15, so every land variant on the roster now has a
catalog; what remains are the ship and Piglin systems below.

The three walk-through courts begin at **9913.5, 230, 986.5** in the live showcase world. The
individual entrances are Swamp at **9929.5, 230, 1004.5**, Viking at
**10201.5, 230, 1004.5**, and Mediterranean at **10473.5, 230, 1004.5**. The complete source
map and placement hashes are recorded in
`tools/structure/next-village-galleries-20260911.json`.

The current review status is:

- **Swamp:** the source court remains as the provenance and comparison gallery for its completed
  production catalog. The selected village center is NF01.2 and SC01.2 is its castle.
- **Taiga:** the production catalog is selected and verified
  ([taiga-village.md](taiga-village.md)); the catalog is in
  `tools/structure/taiga-catalog-20260914.json`. Aaron chose the grittier T&T Viking family on
  2026-09-14; the ornate Polish family, once the other leading alternative, was not chosen. The
  complete standalone T&T Viking and Polish families were displayed in a six-row annex at
  **10201.5, 230, 1224.5** in the earlier showcase world. The full Taiga selection gallery stands
  in the sky above the **trio-20260912** world, beginning at **2393.5, 230, 10.5**, east of the
  Mushroom gallery. Its 38 rows hold 355 pieces:
  - TA01 to TA08: every standalone Towns & Towers Viking, Polish and Swedish piece, each family
    as centre and homes, workplaces, then food and details.
  - TA09: the T&T old-growth-taiga fort and the snowy taiga, taiga and Swedish outpost towers,
    with the Dungeons & Taverns taiga castle ruin and firewatch tower as castle and watchtower
    donors.
  - TA10 to TA25: both complete CTOV taiga villages, the ordinary one first, roads omitted.
  - TA26 and TA27: the CTOV taiga outpost, entities stripped.
  - TA28 and TA29: the Dungeons & Taverns spruce tavern modules and its spruce well.
  - TA30 and TA31: the vanilla taiga village: meeting points, houses and decorations, then
    workplaces and farms.
  - TA32 to TA34: Kithkyn's original Village Life taiga family, recovered from git: centre,
    storehouses and markets; mines, farms, well and works; church, homes, smiths and towers.
  - TA35 to TA38: the palette studies in the Viking materials: the three market tiers in spruce
    on a podzol floor, and three wall directions on the shared Birch geometry: a stripped spruce
    palisade, the fort's spruce and dark oak, and the Birch stone with spruce fittings.

  The rows' sky is filled with the void biome, which has no precipitation, so no snow settles on
  the roofs and lightning cannot strike. Blocks from mods the server lacks are replaced by vanilla
  stand-ins in the private display copies only. The sources, hashes, placements and the offline
  check are recorded in `tools/structure/taiga-full-profile-20260914.json`.
- **Tundra:** the complete source gallery begins at **11107.5, 230, 986.5**. Its 17 rows contain
  all 77 standalone CTOV Snowy Igloo templates, 23 Dungeons & Taverns snowy tavern modules,
  12 CTOV snowy outpost pieces and five Towns & Towers or Terralith landmarks. Fifty Snowy Igloo
  compatibility shells are kept visible with unavailable custom work blocks replaced only in
  their private display copies. The exact sources, hashes, positions and screenshots are recorded
  in `tools/structure/tundra-full-profile-20260912.json`. The selected center, homes, storehouse,
  food buildings, sole watchtower, church, workshops, two farm tiers, mine, T04.8 hunter, T04.4
  bakery, T11.2 tavern and T09.10 well form the verified 23-building private production catalog
  recorded in `tools/structure/tundra-catalog-20260912.json`. Four snow golems begin at the centre as
  unclaimed guard recruits. Three complete market tiers preserve the market's established trade
  colors; every stall has one supported entrance carpet. The single-level wall uses snow, packed
  ice, spruce and single lit brown candles. The design workshop begins at
  **11107.5, 230, 1810.5** and also displays three clean castle donors, the Ice Cathedral palette
  study, and an editable T17.3 Tundra Keep conversion at **11338, 230, 1935**. Those remain general
  references: the production Tundra catalog has no castle.
- **Mediterranean:** the production catalog is selected and verified
  ([mediterranean-village.md](mediterranean-village.md)): the M06.1 church is its centre, the
  M08.1 fort its castle, and the rest comes from annex rows M06 to M08 and the edited court
  copies of M04.2 and M05.3, recorded in `tools/structure/mediterranean-catalog-20260912.json`.
  The court, the seven-row annex at **10473.5, 230, 1224.5** and the workshop row of restyled
  markets and the hedged wall at **10473.5, 230, 1532.5** remain as provenance and comparison
  galleries.
- **Polynesian Coast:** the production catalog is selected and verified
  ([polynesian-coast-village.md](polynesian-coast-village.md)). The selection gallery stands in
  the sky above the **trio-20260912** test world, not the showcase world: Aaron chose it while the
  showcase world was not being served. It begins at **609.5, 230, 10.5**, about 600 blocks east of
  that world's spawn. The rows are:
  - PC01 to PC03: all 18 standalone Towns & Towers Polynesian structures.
  - PC04 to PC06: 16 tropical-coast donors for the roles the family lacks.
  - PC07 to PC09: the fort, stone-circle and ritual-site candidates he asked for.
  - PC10 and PC11: the market and wall workshop.

  Aaron restyled the CTOV sawmill, the priest tower and the mangrove tavern to oak roofs over
  stripped spruce, and converted the sawmill into the mine and the priest tower into the
  watchtower. He also edited his selection live, including the lumberjack plot he made from a copy
  of the well. CTOV Beach work blocks from mods the server lacks are replaced by vanilla stand-ins,
  in the private display copies only. The sources, hashes, restyles, placements and selection are
  in `tools/structure/polynesian-full-profile-20260912.json`, and the catalog is in
  `tools/structure/polynesian-coast-catalog-20260912.json`.
- **Nautical Coast:** the production catalog is selected and verified
  ([nautical-coast-village.md](nautical-coast-village.md)). Its selection gallery also stands in the
  sky above the **trio-20260912** world, beginning at **1463.5, 230, 10.5**. Its 20 rows hold 157
  pieces:
  - NC01 to NC04: the 30-piece Unstructured Ocean Village.
  - NC05 to NC08: the Towns & Towers beach lighthouse and camps, temperate and cold fisheries,
    taverns and wells, and moored boats.
  - NC09 to NC16: the CTOV beach village, which with three beach pieces in the rows before it shows
    all 81 of its templates.
  - NC17 to NC20: the market tiers and the three wall studies.

  Blocks from mods the server lacks are replaced by vanilla stand-ins in the private display copies
  only. The sources, hashes, placements and selection are in
  `tools/structure/nautical-full-profile-20260913.json`, and the catalog is in
  `tools/structure/nautical-coast-catalog-20260913.json`.

- **Savanna Tent:** the production catalog is selected and verified
  ([savanna-tent-village.md](savanna-tent-village.md)); the catalog is in
  `tools/structure/savanna-tent-catalog-20260914.json`. Its selection gallery stands in the sky
  above the **trio-20260912** world, beginning at **1723.5, 230, 10.5**, east of the Nautical
  gallery. Its 43 rows hold 410 pieces:
  - ST01 to ST06: every standalone Towns & Towers savanna and tent piece: the ramshackled savanna
    plateau village with the showcase's assembled tower village and the fourteen storeys its towers
    are stacked from, the savanna plateau camp, the wooded badlands tipi village and camp, the
    wandering trader camp and the savanna outpost tower.
  - ST07 to ST22: both complete CTOV savanna villages, the Native American one first, roads omitted.
  - ST23 and ST24: the CTOV savanna outpost, entities stripped.
  - ST25 and ST26: the Dungeons & Taverns acacia tavern modules and its savanna well.
  - ST27: the Towns & Towers Nilotic houses, for comparison only; that family is the Floodplain
    catalog.
  - ST28: Kithkyn's own original tents, recovered from git at Aaron's request: the five Village Life
    centre tents and five founding storehouse tents that the old catalogs removed on 2026-09-10
    founded villages with.
  - ST29 to ST31: the rest of the original Village Life savanna family, recovered the same way after
    Aaron chose its centre tent: mines, farms, well and works, then church, homes, smiths and towers,
    then markets and storehouses.
  - ST32 to ST39: donor rows for the roles the savanna families left thin: temples and churches,
    statues and obelisks, two rows of farms and pens, lumber yards, the Dungeons & Taverns badlands
    miner compound, and two rows of wells, drawn from vanilla, Towns & Towers, CTOV's dry villages,
    Terralith, YUNG's Extras and Dungeons & Taverns.
  - ST40 to ST43: the palette studies in Aaron's picks' materials: the three market tiers in acacia on
    a coarse-dirt camp floor, and three wall directions on the shared Birch geometry: an acacia
    palisade, a blue-and-yellow canvas wall on acacia, and dry stone with terracotta.

  The rows' sky is filled with the savanna biome, which has no precipitation, so lightning cannot
  reach the wool tents. Blocks from mods the server lacks are replaced by vanilla stand-ins in the
  private display copies only. The sources, hashes, placements and the offline check are recorded in
  `tools/structure/savanna-full-profile-20260914.json`.

- **Mushroom:** the production catalog is selected and verified
  ([mushroom-village.md](mushroom-village.md)); the catalog is in
  `tools/structure/mushroom-catalog-20260915.json`. Aaron chose the Towns & Towers
  mushroom-fields fantasy family on 2026-09-14; the CTOV mushroom village stood beside it and
  was not chosen. The selection gallery stands in the sky above the **trio-20260912** world,
  beginning at **2023.5, 230, 10.5**, east of the Savanna gallery. Its 17 rows hold 139 pieces:
  - MU01 and MU02: every standalone Towns & Towers mushroom-fields fantasy piece: the meeting
    point, houses and the two profession gazebos, then the trades and the eight profession
    inserts, with the mushroom-fields outpost tower. The two mooshroom templates are entity-only
    jigsaw pieces and are omitted.
  - MU03 to MU10: the complete CTOV mushroom village, roads omitted, with its lamps, wagon and
    well in the details row.
  - MU11 and MU12: donors for the roles neither family has: the ten original Village Life mines
    with the Dungeons & Taverns miner compound, and the six D&T village wells with Terralith's
    and the T&T classic well.
  - MU13: studies, each T&T gazebo composed with each of its four profession inserts on its
    jigsaw anchor, since the inserts alone (MU02.4 to MU02.11) are bare station kits.
  - MU14 to MU17: palette studies of the shared Birch market tiers and wall family in the
    family's materials: the markets in oak with mushroom-stem posts on orange concrete paving,
    then wall A (a stem palisade on cobbled deepslate), wall B (red and brown mushroom caps on a
    stem footing) and wall C (cobbled deepslate and smooth basalt, the outpost tower's stone).

  The rows' sky is filled with the void biome, which has no precipitation, so lightning cannot
  strike. Blocks from mods the server lacks are replaced in the private display copies only, each
  by a named vanilla block that keeps its facing and shape (Farmer's Delight crops as bushes on
  their farmland, Create's windmill in wool and oak, its factory in andesite, glass, copper and
  iron, the profession mods' job tables as the nearest vanilla job block); the table is the
  record's `standInSwaps`. The sources, hashes, placements and the offline check are recorded in
  `tools/structure/mushroom-full-profile-20260914.json`.

The Viking annex contains all 43 standalone structures: 22 Viking and 21 Polish. Road pieces
and terminators are omitted because they are layout internals rather than building candidates.
The complete source paths, hashes, placement coordinates, and review evidence are recorded in
`tools/structure/viking-full-profile-20260911.json`.

The Mediterranean annex contains all 60 standalone structures: 29 Mediterranean (the town
center, 23 houses and workplaces, the fort, two fields, the planter and the lamp) and 31 Iberian
(the town center, the temple, 14 houses, the fort, the garden, plants and lamp post, and the
twelve profession inserts T&T drops into Iberian houses). Streets, terminators and the bishop
villager template are omitted for the same reason. Pieces whose waterlogged blocks would flood the
platform stand inside barrier rings. The complete source paths, hashes, placement coordinates and
review evidence are recorded in `tools/structure/mediterranean-full-profile-20260911.json`.

The Tundra gallery contains all 117 useful snowy reference structures. Road pieces and entity-only
jigsaw templates are omitted because they do not present a building choice. Unavailable
compatibility blocks are normalized to vanilla markers only in the private gallery copy, leaving
the source hashes and original templates untouched. T17.3, the Snowy Plains tower, is retained as
a strong general castle donor for a future village variant; it is not assigned to Tundra merely
because it appears in the Tundra gallery.

Unstructured was included in the source audit. Its strongest coherent settlement, the Ocean
Village family, was shown in full in the Nautical Coast gallery (NC01 to NC04); Aaron chose the
CTOV beach village and the Towns & Towers lighthouse instead.

## Future systems

These ideas require placement or simulation work beyond an ordinary land village catalog.

| Settlement | Environment | Required system work |
| --- | --- | --- |
| Ship or floating village | Deep Ocean and other suitable ocean families | Water-aware founding, access, expansion, farms, walls, and construction |
| Piglin-influenced village | Nether Wastes, with later Nether families considered independently | Nether-specific resources, survival, threats, food, terrain, and resident behavior |

Seasonal, ruined, fortified, and inhabited-skeleton settlements are presentation, defense,
condition, or population concepts that may later modify one of the village biomes above.
