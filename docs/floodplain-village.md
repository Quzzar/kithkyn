# Floodplain village

The Floodplain catalog is the mangrove swamp village: the Nilotic houses from Towns and
Towers with roles assigned by Aaron, three editable drafts restyled toward mud and mangrove,
the two Polynesian farms, and the arid wall geometry painted in mud brick. It is the fourth
finished regional village after Birch Forest, Desert and Badlands
([buildings.md](buildings.md), "Current runtime selection"). The style token is
`floodplain`; vanilla `minecraft:mangrove_swamp` selects it through the
`kithkyn:village_style/floodplain` biome tag, an untagged modded biome whose registry path
contains `mangrove` selects it by name, and an unclassified hot, wet climate rolls it as its
cluster. Plain vanilla swamp is reserved for a separate Swamp village; being hot and wet under
NeoForge's conventional tags it builds Floodplain until that exists.

## Selected buildings

The public selection record is `tools/structure/nilotic-selections-20260910.json` (27
native captures: 20 buildings, 5 wall sections, 2 unassigned candidates) and the catalog
record is `tools/structure/floodplain-catalog-20260910.json`. The datapack holds twenty
definitions, all new ids, so the pack needs no filter:

| Id | Source exhibit | Beds | Stations |
| --- | --- | --- | --- |
| `village_center_floodplain_1` | Nilotic cartographer house with four white banners and the green carpet cross | 0 | quartermaster, builder, guard captain |
| `mine_floodplain_1` | Nilotic armorer house with the authored pit floor | 0 | miner in the 3x3 pit; the shaft leaves east |
| `storehouse_floodplain_1` | small house draft with barrels, a note block and two authored allays | 0 | none; the quartermaster keeps the centre post |
| `church_floodplain_1` | Nilotic temple | 0 | cleric |
| `lumberjack_floodplain_1` | Nilotic farmer house | 1 | lumberjack on the jungle sapling |
| `hunting_lodge_floodplain_1` | Nilotic fletcher house | 1 | hunter |
| `butchery_floodplain_1` | Nilotic leatherworker house with three cows and three pigs in the pen | 1 | butcher |
| `stoneworks_floodplain_1` | Nilotic mason house | 1 | mason |
| `blacksmith_floodplain_1` | Nilotic toolsmith | 0 | blacksmith |
| `fishery_floodplain_1` | desert oasis pool draft with a barrel and a lantern | 0 | fisher on the pool rim |
| `well_floodplain_1` | mangrove tavern well draft | 0 | none |
| `watchtower_floodplain_1` | firewatch tower draft with four white banners | 1 | crossbow post |
| `house_floodplain_1` | Nilotic small house 2 | 1 | none |
| `house_floodplain_2` | Nilotic large house, standalone | 2 | none |
| `couple_cottage_floodplain_1` | Nilotic small house 1 with the couple bed | 2 | none |
| `market_floodplain_1`, `_2`, `_3` | the Desert market tiers restyled | 0 | one to three merchants |
| `farm_floodplain_1`, `_2` | the Polynesian small and large farms | 0 | farmer |

There is no tavern and no bakery, and only one storehouse level: more storehouses are the
answer to storage strain, and the adopted allays help there
([allay-quartermasters.md](allay-quartermasters.md)). The market tiers and the farms are
the only upgrades.

The centre has no beds, so its founding set is the mine, the storehouse, two large houses
and a small house: five beds for the four starting jobs (quartermaster, builder, guard
captain, miner). One campfire sits on the earthen apron in front of the north opening,
because residents idle and cook at a fire; the meeting point is the bare plaza cell inside
that opening.

## Identity and environment

White banners are village banners: four on the centre and four on the watchtower. The
centre's six green carpets become the primary colour. Every bed is a colour slot: couple
beds take the primary colour and single beds alternate primary then secondary within a
building. The watchtower's brown banners and the market tent fabrics are decoration and
stay as authored. Barrels and chests are shared work storage; homes carry their own room
storage.

Every house ships with the patchy earthen ground course Aaron laid under it (packed mud,
mud, coarse dirt, rooted dirt) as its bottom layer, the way Desert ships its seating. The
lumberjack's dead bush marker is exported as a jungle sapling on dirt: the wood loop refuses
mangrove propagules because a grown mangrove leaves roots on the stand. The storehouse
doorway trapdoor and the watchtower's ladder hatch are exported open, since the runtime opens
doors and gates but never trapdoors; the guard climbs through the open hatch to the bunk.

Walls are not exported. The runtime paints the shared arid wall geometry
([walls.md](walls.md)) in the floodplain palette: mud bricks for posts, deck, body, stairs
and slabs, mud brick walls as railings, jungle trapdoors, muddy mangrove roots on the
gatehouse uprights and roof rim, brown candle clusters on every standing lamp, and the four
hanging gate lanterns kept. Wall bills are priced in mud bricks.

## Authoring and local installation

`run/floodplain-integration/prepare.py` reads the selection record and the selection
captures, writes the export plan, runs `tools/structure/VillageTemplateExport.java` (which
gained an optional `entities` plan key for the livestock and the allays) and assembles
`run/floodplain-integration/datapack/`. `check.py` validates every definition and template
statically. The private pack is installed in the local world's `datapacks` directory as
`kithkyn-floodplain` alongside the updated runtime and kept with world backups; the public
jar deliberately carries none of these third-party-derived structures
([structure-sourcing.md](structure-sourcing.md)). A manual sample can request
`/kithkyn create-village ~ ~ ~ floodplain`.

## Verification

Core tests cover mangrove selection, the hot and wet climate cluster, the mud brick wall
palette with its 160 roots frame cells, and the naming profile. The native checks reuse the
shared reviewed-village fixture with `-Dkithkyn.reviewedVillage.style=floodplain`, the real
placement, house-access and approved-centre fixtures, and natural founding in a mangrove
swamp world; `run/floodplain-integration/verify.py` runs each mode on a disposable server.
Opt-in verification flags belong only on disposable worlds. Results are recorded with the
release under `run/floodplain-integration/`.
