# Jungle village

The Jungle catalog is the first playable raised-timber village. It uses the selected Tribal
family, Aaron's edited tree homes and service buildings, the NH06.5 Firewatch tower, three
repaired market tiers, stripped-bamboo roofs and the timber-and-bamboo perimeter. It is the
fifth finished regional style after Birch Forest, Desert, Badlands and Floodplain
([buildings.md](buildings.md), "Current runtime selection"). The style token is `jungle`.
Vanilla jungle, bamboo jungle and sparse jungle select it; conventional jungle biome tags and
recognizable modded biome names use it whenever its complete private catalog is loaded.

## Selected buildings

The public selection record is `tools/structure/jungle-selections-20260910.json`; the final
catalog and immutable output hashes are recorded in
`tools/structure/jungle-catalog-20260910.json`. The private datapack contains 22 definitions:

| Id | Selected use | Beds and stations |
| --- | --- | --- |
| `village_center_jungle_1` | JA01.1 meeting point | No beds; quartermaster, builder, guard captain and miner vacancies |
| `mine_jungle_1` | JA01.5 edited armorer | No vacancy; one sunken physical miner worksite and two shared barrels at the stair rim |
| `storehouse_jungle_1` | JA03.1 edited small house | No vacancy; one physical quartermaster worksite and three shared containers |
| `lumberjack_jungle_1` | JA01.4 animal pen | Lumberjack and one shared chest |
| `tavern_jungle_1` | JA01.6 butcher | Innkeeper and one shared barrel; no bed |
| `fishery_jungle_1` | JA01.8 fisher | Fisher, one staff bed and one shared barrel |
| `hunting_lodge_jungle_1` | JA02.1 fletcher | Hunter and one shared chest |
| `stoneworks_jungle_1` | JA02.5 mason | Mason and one shared chest |
| `butchery_jungle_1` | JA02.6 shepherd | Butcher, one staff bed and its personal chest |
| `farm_jungle_1` | JA02.7 small farm | Farmer and one shared barrel |
| `bakery_jungle_1` | JA02.8 edited small house | Baker and one shared barrel |
| `church_jungle_1` | JA03.3 temple | Cleric and one shared chest |
| `blacksmith_jungle_1` | JA03.5 weaponsmith | Blacksmith and one shared chest |
| `watchtower_jungle_1` | NH06.5 Firewatch | Crossbow post, one guard bed and its personal chest |
| `house_jungle_1` | JA02.9 small house 2 | One bed and personal chest |
| `house_jungle_1__small_house_4` | JA03.2 small house 4 | One bed |
| `house_jungle_2` | JA01.7 treehouse, standalone | Two single rooms with personal chests |
| `house_jungle_2__large_house_1` | JA02.2 large house, standalone | Two single rooms with personal chests |
| `couple_cottage_jungle_1` | JA02.3 leatherworker | One two-person couple room and personal chest |
| `market_jungle_1`, `_2`, `_3` | repaired Jungle market tiers | One to three merchants and matching shared containers |

There is one storehouse level, one watchtower level and no well. The four ordinary houses are
alternatives rather than an upgrade chain; the two level-2 homes are standalone construction
choices. Markets are the only upgrade chain. The market tents retain the established orange,
cyan, red and white fabric instead of taking village colors.

## Founding and physical worksites

The center deliberately has no beds. Its founding set places the mine, the storehouse and four
one-person homes through ordinary construction site search, producing natural sprawl with the
same two-block spacing preference and inward-facing preference as later growth. Those four homes
house the center's four starting workers.

The quartermaster and miner vacancies remain owned by the center, while their physical work
happens in the separately placed storehouse and mine. A `worksite_category` on each center post
routes the worker to the nearest matching building; the target building declares a `worksites`
position without creating a duplicate vacancy. Removing or rebuilding that target makes the
workplace temporarily unavailable. The mine alone owns its three-block-wide shaft. Its four-by-three
pit is one block deep, with three stairs down its west side and both material barrels at that stair
rim. The miner stands in the pit's west column; the eastward ramp mouth is one column inward and one
block above that post, keeping the first descending cuts inside the pavilion.

The bell is the meeting and arrival point. The campfire is a separate cooking and idle amenity;
it does not redefine the village center.

## Identity and walls

Every authored white bed is a village-color slot. The two couple beds use the same primary
color; the remaining beds use their declared primary or secondary slots. White banners become
the village banner. Roof hay was replaced with stripped bamboo oriented along the original hay
axis, so the village keeps its layered thatch silhouette without giving players roof-sized wheat
stores.

Walls use the shared terrain-following arid geometry with Jungle materials: jungle planks for
the body and posts, bamboo mosaic for decks, stairs and slabs, jungle fences and trapdoors,
stripped jungle wood for the gate frame, and standing torches in place of candle clusters. The
four hanging gatehouse lanterns remain. There is one wall stage, priced in jungle logs.

The naming profile describes a shaded settlement of raised timber rooms, bamboo roofs, broad
leaves, open workshops and canopy watch platforms. Its fallback syllables are independent of
the other regional styles.

## Authoring and local installation

`run/jungle-integration/prepare.py` reads the immutable capture record, crops the approved
structures, neutralizes village identity slots, moves the butcher's inaccessible chest and applies
the approved sunken mine-floor revision, then assembles
`run/jungle-integration/datapack/`. Programmatically placed chests and barrels receive native
block-entity data from `tools/structure/VillageTemplateExport.java`. The private pack is installed
as `kithkyn-jungle` and kept with world backups; the public jar deliberately carries none of the
third-party-derived templates ([structure-sourcing.md](structure-sourcing.md)). A manual sample
can request `/kithkyn create-village ~ ~ ~ jungle`.

## Verification

Core tests cover biome selection, codec persistence, routed physical worksites, mine ownership,
the Jungle wall palette and unchanged fallback behavior. An opt-in private-asset test fixes the
mine's pit, stair, barrel, worksite and mouth geometry. Native checks run the private catalog
through the shared reviewed-village, access and real-placement fixtures.

The September 10 integration passed the complete access suite: 84 rotated structures, 224 real
navigation routes, 44 room walks and sleeps, 36 personal-container deposits, 68 station walks,
80 shared-container deposits and eight mine walks, with zero failed placements. The placement
fixture passed 176 real-template placements through instant and incremental construction in all
four rotations, including colors, frames, save receipts and upgrade preservation. A server
restart retained all 176 buildings. The founding fixture passed 22 strict templates, all four
center rotations, eight market upgrade fits, natural Jungle selection, four starting homes,
four assigned workers, distinct bell and campfire locations, routed mine/storehouse work and
codec reloads. After the mine-floor revision, a focused native pass added four real excavation
runs, eight routed-worksite access routes and eight instant/incremental construction placements
across all four rotations. The exported template has zero blockstate mismatches against Aaron's
flushed live capture.
