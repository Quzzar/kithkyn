# Japanese Cherry Grove village

The Japanese Cherry Grove catalog is a flowering woodland settlement of spruce-framed houses,
dark tiled roofs, ponds, compact farms and deliberate garden planting. Its style token is
`japanese_cherry_grove`. Cherry Grove, Flower Forest and compatible modded biome paths containing
`cherry` or `sakura` select it whenever its private founding set is loaded. A datapack may map
another biome explicitly with the `kithkyn:village_style/japanese_cherry_grove` biome tag.

The catalog deliberately has no castle. JC07 remains in the gallery as a candidate for a later
pass and is not a production building. No standalone well was selected.

## Founding village

`village_center_japanese_cherry_grove_1` is JP01.2. Its ceiling bell anchors the civic meeting
point, while the side campfire is a separate gathering place. It has no beds. The center opens
the quartermaster, builder, guard captain and miner positions and carries six village banners.

The normal site planner places the mine, storehouse, four-bed home and two-bed home as four
companion buildings. They use the same one-or-two-block spacing, inward-facing preference and
terrain rules as later construction. The two houses provide six beds for the four founding
workers and the first two residents who join them. The center's quartermaster and miner positions
route to the physical storehouse and mine.

## Selected buildings

The private datapack contains 20 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_japanese_cherry_grove_1` | JP01.2 | Bedless center, four founding jobs, ceiling bell, side campfire and six village banners |
| `watchtower_japanese_cherry_grove_1` | JP01.3 | Crossbow post with a guard bed and personal chest |
| `house_japanese_cherry_grove_1__four_bed` | JP02.1 | Four-bed founding home with shared storage on both floors |
| `house_japanese_cherry_grove_1` | JP02.2 | Two-bed founding home with one personal chest per resident |
| `butchery_japanese_cherry_grove_1` | JP03.4 | Butcher station, work chest, three cows and three chickens |
| `fishery_japanese_cherry_grove_1` | JP03.5 | Pond fishery and work barrel |
| `farm_japanese_cherry_grove_1`, `_2` | JP03.8 and JP03.7 | Small and large farm progression, each with a work barrel |
| `mine_japanese_cherry_grove_1` | JP03.9 | Two-wide south-running mine ramp, miner worksite and storage chest |
| `blacksmith_japanese_cherry_grove_1` | JP04.1 | Forge and work chest |
| `church_japanese_cherry_grove_1` | JP04.7 | Cleric station and work chest |
| `tavern_japanese_cherry_grove_1` | JP05.7 | Five-bed tavern; two upstairs worker beds for its two innkeepers and three general beds |
| `bakery_japanese_cherry_grove_1` | JP06.2 | Bakery and work chest |
| `hunting_lodge_japanese_cherry_grove_1` | JP06.5 | Hunter station and work chest |
| `lumberjack_japanese_cherry_grove_1` | JP07.1 | Lumberjack yard, work barrel and cherry sapling site |
| `stoneworks_japanese_cherry_grove_1` | JP07.3 | Mason station and work chest |
| `storehouse_japanese_cherry_grove_1` | JP07.6 | Founding quartermaster worksite with four village chests |
| `market_japanese_cherry_grove_1`, `_2`, `_3` | JM01 to JM03 | One, two and three merchant stalls in the approved Japanese frames |

## Identity, markets and wall

Every logical bed is exported as a neutral white slot and resolves to the village primary or
secondary color. Every authored center banner resolves to the generated village banner. Market
fabric keeps the established red, cyan, orange and white trade colors.

The single wall level uses the approved JW01 through JW05 straight, diagonal, corner tower,
gatehouse and terrace modules. Stripped spruce forms the centered frame, spruce stairs form the
roof edge, deepslate tile slabs form the walk, and cherry trapdoors provide access. Persistent
cherry and flowering azalea leaves retain the full exterior foliage on the straight runs instead
of being clipped or covered by a displaced wall spine. Torches and the authored hanging gate
lanterns provide light. There is no second wall upgrade.

## Authoring and verification

`run/japanese-integration/prepare.py` promotes the 17 approved JP captures and three approved JM
market templates through `tools/structure/VillageTemplateExport.java`. It preserves authored
geometry, clears only gallery barriers and structure voids, neutralizes identity slots, empties
containers, converts the lumberjack marker to a cherry sapling and adds only the approved
butchery livestock. Third-party-derived buildings and definitions remain in the local private
datapack; source and output hashes are recorded in
`tools/structure/japanese-cherry-grove-catalog-20260913.json`.

The catalog audit checks all 20 structures for barriers, structure voids, out-of-bounds cells,
identity slots, containers, livestock and the exact mine frame. Native placement and founding
verification cover all four rotations, the farm and market upgrades, dynamic colors, job and bed
allocation, routed mine and storehouse work, and save/reload behavior. Manual testing can create
the village with `/kithkyn create-village ~ ~ ~ japanese_cherry_grove`; natural founding in a
mapped flowering woodland uses the same selection and founding path.
