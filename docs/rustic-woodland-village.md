# Rustic Woodland village

The Rustic Woodland catalog is the ordinary oak-forest village. It keeps the familiar shape of a traditional Minecraft woodland settlement while giving every building a concrete village role. Forest selects this catalog when its private datapack is installed. Compatible modded oak woodland biomes can opt in through `kithkyn:village_style/rustic_woodland`.

## Founding village

RW01.1 is the village center. Its bell is the meeting point and the guard captain's post; its lit campfire is a separate gathering fire. It contains five builder duty anchors, unlocked by the shared population thresholds, and one merchant post at the small stall. The built-in well grants water. The stall barrel is shared storage; the eight barrels around the water tower are scenery and are not village containers. The center has no beds.

A founding village places the center, RW07.3 mine, RW03.4 storehouse, RW02.2 two-bed house, and RW03.2 one-bed house twice through the ordinary building placement planner. The four beds house the founding population. The mine owns one miner vacancy and the storehouse owns one quartermaster vacancy. The center never owns either vacancy.

## Approved catalog

| Gallery | Building | Amenities |
| --- | --- | --- |
| RW01.1 | Village center | Bell meeting point, campfire, well, captain, merchant, five builder anchors |
| RW01.2 | Church | Cleric and shared chest |
| RW01.3 | Well | Deep oak well, sunk seven blocks |
| RW02.2 | Two-bed house | Two single beds and one shared personal chest |
| RW02.4 | Couple cottage | One couple room and shared personal chest |
| RW03.2 | One-bed house | One bed and personal chest |
| RW03.4 | Storehouse | Quartermaster and four shared barrels |
| RW04.1 | Blacksmith | Smith and shared chest |
| RW04.5 | Fishery | Live-in fisher, personal barrel, and shared fish barrel |
| RW04.6 | Hunting lodge | Hunter and shared chest |
| RW05.3 | Stoneworks | Mason and shared chest |
| RW06.1 | Watchtower | Crossbow guard, guard bed, and personal chest |
| RW06.2 | Tavern | One innkeeper bed and four general beds, each with personal storage; the existing upper trapdoor is saved open above two ladder rungs in its clear shaft |
| RW06.5 | Bakery | Baker and shared chest |
| RW07.1 | Lumberjack | Lumberjack, shared chest, and an oak sapling at the authored planting point |
| RW07.2 | Butchery | Butcher, shared barrel, three sheep, and three cows |
| RW07.3 | Mine | One miner and the user-carved three-wide north-facing shaft entrance |
| RW10.2 | Farm | Large oak-framed field and one farmer post |
| RW08.1–3 | Markets | Three upgrade tiers with the fixed red, cyan, and orange trade fabrics |

There is one approved wall tier: RW09.1–5 form a clean stripped-oak palisade with oak stairs, walks, fences, hatches, torches, a gatehouse, and corner towers. The foliage shown during early wall studies was removed. Rustic Woodland has no castle.

## Identity and structure rules

Every registered bed is a village identity slot and resolves to the primary or secondary village color. The two halves of a couple bed always use one color. The two center banners resolve to the village banner. Market fabrics stay in their fixed trade colors.

The production templates preserve the user-edited live gallery. Barrier and structure-void blocks are removed at export. Missing cells above the ground layer are authored as air, including the user's carved mine interior. The tavern's existing upper trapdoor is saved open and two ladder rungs occupy the clear shaft beneath it so the upstairs bed remains usable. Buildings keep their blocks; navigation failures are reported through the structure-access checks rather than repaired by deleting architecture.

The private datapack lives at `run/rustic-integration/datapack`. The tracked catalog receipt is `tools/structure/rustic-woodland-catalog-20260914.json`.
