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
| Jungle Tribal | Jungle, Bamboo Jungle, and Sparse Jungle | Jungle timber, stripped bamboo roofs, compact huts, treehouses, three markets, the timber wall, and the Firewatch tower |

## Locked directions

These village biomes have an agreed environmental home and architectural direction. A locked
direction may already have a complete gallery selection, but it remains in this section until
its production catalog and founding behavior are verified.

| Village biome | Founding environments | Direction |
| --- | --- | --- |
| Mediterranean | Plains and Sunflower Plains | White stone and plaster town core with tile roofs, courts, orchards, gardens, and open agriculture |
| Rustic Woodland | Forest and compatible ordinary oak woodland biomes | Familiar timber woodland settlement with a restrained rustic character |
| Romanian | Dark Forest, forested highlands, and wooded valleys | Heavy timber roofs, enclosed yards, and substantial woodland buildings |
| Japanese | Cherry Grove, Flower Forest, and compatible Sakura biomes | Garden settlement shaped around flowering woodland and deliberate landscape details |
| Taiga | Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga, and compatible cold forests | Cold forest settlement; its final visual language is deliberately deferred between the ornate Polish family and the grittier T&T Viking family |
| Tundra | Snowy Plains, Ice Spikes, and compatible exposed frozen lowlands | Compact igloos and snowbound buildings suited to treeless terrain |
| Alpine | Meadow, Grove, Snowy Slopes, and compatible mountain valleys and peaks | Swiss-inspired mountain settlement with steep roofs and slope-conscious buildings |
| Swamp | Swamp, Orchid Swamp, and compatible ordinary wetlands | Boat-based or overgrown wetland village, distinct from the mud-brick Floodplain catalog; SC01.2 is its configured castle |
| Polynesian Coast | Sparse Jungle, tropical beaches, warm-ocean islands, and compatible tropical coasts | Open, warm-climate coastal buildings based on the Polynesian reference family |
| Nautical Coast | Beach, Stony Shore, and compatible temperate or cold coasts | Fishing town, docks, shoreline buildings, and lighthouse landmarks |
| Savanna Tent | Savanna, Savanna Plateau, Windswept Savanna, and compatible dry grasslands | African-inspired tent and grassland settlement with portable-looking structures and a coherent warm-climate material palette |
| Mushroom | Mushroom Fields and compatible fungal biomes | Fantasy mushroom settlement drawn from the complete mushroom reference families |

Jungle is a complete private production catalog with nineteen selected buildings and three
market tiers. Its founding layout, housing, physical worksites, wall, and all access routes are
verified and locally deployed.

Swedish and Polish are source languages in the current review rather than settled biome
assignments. The Swedish temple and tower remain a useful candidate church progression for a
future Taiga catalog. The complete Polish family remains the ornate Taiga option, while the T&T
Viking family remains the grittier option with a stronger identity of its own. Swiss remains the
primary direction for Alpine because mountain terrain needs architecture distinct from forested
Taiga and exposed Tundra.

Rivers do not receive a separate village biome. An ordinary land village resolves from the
surrounding environment rather than a narrow river strip. Tropical riverbanks and deltas belong
to Floodplain when site selection can classify them reliably.

## Next authoring shortlist

The next scouting pass prioritizes three catalogs:

1. **Swamp** fills the clearest remaining wetland gap. Its court compares the Towns & Towers
   boat village, the Dungeons & Taverns swamp family, open and fortified CTOV swamp buildings,
   and wetland service and defense pieces.
2. **Taiga** will give taiga and old-growth spruce forests a coherent cold-timber identity. Its
   court compares complete Viking and Polish families with Swedish, CTOV taiga, spruce service,
   and tower donors. Final family selection is deferred while other village catalogs are built.
3. **Mediterranean** gives Plains and Sunflower Plains a distinct rural culture. Its court
   compares the Mediterranean and Iberian families with CTOV Plains civic buildings, fields,
   gardens, tavern, well, and fortified donors.

The three walk-through courts begin at **9913.5, 230, 986.5** in the live showcase world. The
individual entrances are Swamp at **9929.5, 230, 1004.5**, Viking at
**10201.5, 230, 1004.5**, and Mediterranean at **10473.5, 230, 1004.5**. The complete source
map and placement hashes are recorded in
`tools/structure/next-village-galleries-20260911.json`.

The current review status is:

- **Swamp:** S01.1 is the town center. The authoring pool combines the complete 23-piece
  Towns & Towers Boat Village family with the complete 34-piece Dungeons & Taverns Swamp
  family. Signs at the Swamp court link to both full source wings.
- **Taiga:** source selection is deferred. The ornate Polish family and grittier T&T Viking
  family remain the two leading alternatives; neither is the production direction yet. V01.1,
  V03.5, and V05.2 remain candidates rather than locked selections. The complete standalone T&T
  Viking and Polish families remain displayed in a six-row annex at
  **10201.5, 230, 1224.5** for the later decision.
- **Mediterranean:** the M01 white-city family supplies the core. M04.1 through M04.4 supply
  fields, orchards, and garden language around it.

The Viking annex contains all 43 standalone structures: 22 Viking and 21 Polish. Road pieces
and terminators are omitted because they are layout internals rather than building candidates.
The complete source paths, hashes, placement coordinates, and review evidence are recorded in
`tools/structure/viking-full-profile-20260911.json`.

Unstructured was included in the source audit. Its strongest coherent settlement is the Ocean
Village family, which belongs in the later Nautical Coast pass rather than one of these three
courts.

## Future systems

These ideas require placement or simulation work beyond an ordinary land village catalog.

| Settlement | Environment | Required system work |
| --- | --- | --- |
| Ship or floating village | Deep Ocean and other suitable ocean families | Water-aware founding, access, expansion, farms, walls, and construction |
| Piglin-influenced village | Nether Wastes, with later Nether families considered independently | Nether-specific resources, survival, threats, food, terrain, and resident behavior |

Seasonal, ruined, fortified, and inhabited-skeleton settlements are presentation, defense,
condition, or population concepts that may later modify one of the village biomes above.
