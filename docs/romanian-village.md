# Romanian village

The Romanian catalog is the Dark Forest village: steep pale birch roofs over dark-oak frames,
white walls, cobblestone details, enclosed yards and substantial woodland homes. The style token
is `romanian`. Dark Forest and compatible modded biome paths named `dark_forest`,
`darkforest`, `forested_highland` or `wooded_valley` select it whenever its private founding set
is loaded. A datapack may map any other biome explicitly with the
`kithkyn:village_style/romanian` biome tag.

The catalog deliberately has no castle. The stone keep in the source gallery remains a reference
for a later decision.

## Founding village

`village_center_romanian_1` is R01.1 and combines the town center with the church. Its bell is the
meeting point and its side campfire is the gathering fire. It opens five population-scaled builder
duties, one guard captain position and one cleric position, and carries the village's four authored banners.

The center has no beds. The normal site planner places `mine_romanian_1` and
`storehouse_romanian_1` as founding companions with the same spacing, orientation and terrain
rules used by later construction. Together they supply four beds for the four founding residents.
The mine owns the miner vacancy and reserves its bed for that worker. The storehouse owns the
quartermaster vacancy; its three beds are general housing. The cleric position remains open until
the village adds housing.

## Selected buildings

The private datapack contains 24 definitions:

| Id | Use |
| --- | --- |
| `village_center_romanian_1` | R01.1 combined center and church; five builder duties, captain and cleric, bell, side campfire and four banners |
| `mine_romanian_1` | R05.3; miner vacancy, live-in bed and east-running three-block shaft |
| `storehouse_romanian_1` | R03.1 founding storehouse and three-bed home; four shared storage containers and two personal containers |
| `storehouse_romanian_1__compact` | R02.6 extra compact storehouse with five shared containers |
| `house_romanian_1`, `__small_three` | R02.2 and R02.3 one-bed homes |
| `house_romanian_1__two_single` | R02.1 two-bed home |
| `house_romanian_1__four_single` | R03.3 four-bed home with shared personal storage on both floors |
| `couple_cottage_romanian_1` | R02.4 couple home; both bed halves share one village color |
| `butchery_romanian_1__herder_home` | R03.2 butcher and two-bed home with three cows and three pigs |
| `butchery_romanian_1` | R04.2 butcher and live-in bed with three sheep and three chickens |
| `blacksmith_romanian_1` | R04.1 forge |
| `bakery_romanian_1` | R04.3 bakery |
| `fishery_romanian_1` | R04.4 fishery with the approved birch-slab roof |
| `hunting_lodge_romanian_1` | R05.1 hunter station |
| `stoneworks_romanian_1` | R05.2 mason station |
| `farm_romanian_1` | R05.4 small farm |
| `tavern_romanian_1` | R06.1 four-bed tavern; one upstairs innkeeper bed and three general beds |
| `watchtower_romanian_1` | R06.2 crossbow post with a guard bed and personal chest |
| `well_romanian_1` | R06.3 woodland well |
| `lumberjack_romanian_1` | User-edited R05.3 copy; jungle-tree-shaped dark woodland yard with one live-in bed, personal barrel, work barrel and dark-oak planting site |
| `market_romanian_1`, `_2`, `_3` | One, two and three merchant stalls using the approved Romanian frames and fixed trade colors |

## Identity and wall

Every logical bed is exported as a neutral white slot and resolves to the village primary or
secondary color. Every authored banner resolves to the generated village banner. Market fabric
keeps the established red, cyan, orange and white trade colors.

The single wall level uses the five approved authored modules: straight, diagonal, corner tower,
gatehouse and terrace. Vertical stripped dark-oak timbers form the body, cobbled deepslate forms
the foundation and rails, birch caps brighten the top, dark-oak trapdoors provide access, and
torches light the wall. There is no second wall upgrade.

## Authoring and verification

`run/romanian-integration/prepare.py` promotes the selected live captures through
`tools/structure/VillageTemplateExport.java`. It removes gallery-only barriers and structure
voids, neutralizes identity slots, empties containers, records authored air above the ground and
places livestock only on clear pen floors. Third-party-derived building templates and definitions
remain in the local private datapack; source and output hashes are recorded in
`tools/structure/romanian-catalog-20260912.json`.

The native verification covers all 24 templates through instant and villager-paced construction,
all four rotations, identity replacement, entity placement, save/reload receipts, founding
placement, locally owned mine and storehouse jobs, and full-size villager access. Manual testing can use
`/kithkyn create-village ~ ~ ~ romanian`; natural founding in Dark Forest uses the same selector.
