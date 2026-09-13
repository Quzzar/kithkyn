# Building-cost balance: locked first pass

**Locked and loaded by the game.** This inventory covers the 224 current authored
building definitions across Birch Forest and the eight installed private village-biome catalogs.
It is the balance sheet for prices and grants owned by individual building variants. Every listed
definition now carries the recipe and grants shown here, and the village brain sees both alongside
the building's exact capacity.

## Decision behind the balance

A construction recipe prices the complete authored building variant: its housing, jobs, grants,
storage, and place in progression. Category and level are comparison labels, not price keys. The
number of blocks in the structure is only a final sanity check and does not derive the price.
Two variants may deliberately land on the same price when they provide equivalent value, but that
is an authored decision on both definitions rather than inheritance from a universal recipe.

The catalog separates `Grants` from the other value a building contributes. A grant is
the building's qualitative outcome; `Other value` records the exact capacity supporting it. A
one-bed house therefore grants `HOUSING` and separately records one bed. Both describe the same
value at different resolutions and must not be charged twice.

Every building has at least one proposed grant. Broad and specific grants intentionally coexist:
a bakery grants `FOOD` and `BAKED_GOODS`, while a farm grants `FOOD` and `CROPS`. These are clearer
planning facts than one current item such as bread or wheat. A mine grants `STONE`, `ORES`, and
`MINERALS`; it does not grant `FUEL`, even if coal can happen to come out of a shaft.

The numbers below are explicit first-pass judgments, generated from a common rubric and then meant
to be recalibrated against the village audit harness. The rubric is an authoring aid, not a runtime
formula:

- A plain one-bed home lands around 16–20 effective items; additional beds and couple-ready rooms
  add housing value directly.
- A normal one-worker producer or service lands around 32–48. Live-in housing, extra jobs, extra
  grants, and usable storage move it upward.
- Mixed-use buildings are priced for the full combination. A house that also supplies blacksmith
  and cleric posts is not priced as a house.
- A numeric building level has no price by itself. More beds, posts, storage, capabilities, or
  throughput justify the increase. The current larger farm tiers receive an explicit throughput
  premium because field capacity is not represented elsewhere in their metadata.
- The first lumberjack remains a bootstrap exception: it is paid entirely in stone and held to
  24–40 items depending on included housing and storage, so the founding miner can unlock wood.
- Ordinary prices use generic logs and cobblestone. Their mix shifts with the village biome's
  plausible economy: dry catalogs lean toward stone; wet and jungle catalogs lean toward timber.
  Wool and iron are reserved for later markets and castles that can reasonably depend on those
  supply chains.
- Village centers are founded free. Their listed prices are nominal comparisons of what the
  compound provides, not a founding charge.

## Sanity checks against the old shared prices

| Building | Old shared price | First-pass authored price |
| --- | ---: | ---: |
| Birch church | 513 | 44 |
| Birch tavern | 319 | 52 |
| Birch bakery | 236 | 44 |
| Birch lumberjack | 104 | 28 |
| Birch one-bed house | 33 | 20 |
| Birch blacksmith | 68 | 64 |
| Birch markets 1 / 2 / 3 | 30 / 72 / 112 | 36 / 68 / 88 |
| Castles | 208 universally | Mediterranean 200 / Desert 232 / Swamp 312 |

The ordinary range is intentionally compact. Large prices now come from large bundles of useful
systems: the 312-item Swamp castle has eleven beds, twelve jobs, three capabilities, and storage;
it is no longer treated as economically interchangeable with the smaller Mediterranean fort.

## Recalibration targets

The audit harness should record time from selection to affordability, time from payment to
completion, and the resource that dominated the wait. The first pass is healthy when foundational
producers are affordable promptly, an ordinary single-role building does not hold the village for
many game days, mixed-use buildings feel materially more consequential, and castles remain long
projects without becoming permanent saving goals. Repeated shortages, abandoned goals, or a role
that is always selected because it is too cheap are balance failures and should move that specific
recipe.

## Grant vocabulary

These names are the current planning vocabulary. They describe why the village might want a
building, not a promise that every outcome is simulated by a separate production loop. Exact
capacities remain in `Other value` so the brain can distinguish, for example, one bed from five
beds even though both buildings grant `HOUSING`.

Personal containers remain visible as authored capacity in `Other value`, but they do not create
a separate grant. They are part of a building's housing or workplace arrangement rather than an
independent reason for the village to build it.

Overlapping grants are complementary descriptions, not additive price points. `FOOD` plus
`CROPS`, or `HOUSING` plus `FAMILY_HOUSING`, should be priced once as part of the building's total
package rather than receiving one surcharge per label.

A role contributes its production grant to the building where the work is performed. A local job
can therefore add a grant to a mixed-use building, while a job routed through `worksite_category`
does not duplicate the output already attributed to its destination building.

| Area | Grant | Design meaning |
| --- | --- | --- |
| Settlement | `CIVIC_CENTER` | A meeting, arrival, and settlement-planning anchor. |
| Settlement | `GOVERNANCE` | Leadership and settlement-level decision capacity. |
| Settlement | `CONSTRUCTION` | Builder labor that can execute planned structures. |
| Settlement | `LOGISTICS` | Quartermaster coordination for village inventory and movement. |
| Accommodation | `HOUSING` | Valid resident accommodation; the bed count states its capacity. |
| Accommodation | `FAMILY_HOUSING` | Accommodation suitable for couples or families; couple-room count states its capacity. |
| Accommodation | `HOSPITALITY` | Inn or tavern service for residents and visitors. |
| Accommodation | `WANDERERS` | Capacity to receive and attract newcomers. |
| Accommodation | `STORAGE` | Shared village inventory space; shared-container count states its capacity. |
| Accommodation | `WATER` | Reliable access to fresh water. |
| Care | `HEALING` | Cleric or medical care for villagers. |
| Security | `PROTECTION` | Guard labor or defensive capacity. |
| Security | `RANGED_GUARD_POSTS` | Designated positions from which guards can defend at range. |
| Security | `CUSTODY` | Space and systems for prisoners, evidence, and release. |
| Food | `FOOD` | Broad contribution to the village's edible supply. |
| Food | `CROPS` | Farmed produce and harvest variety, not grain alone. |
| Food | `BAKED_GOODS` | Prepared baked food, not bread alone. |
| Food | `FISH` | Food supplied through fishing. |
| Food | `MEAT` | Food supplied through hunting or butchery. |
| Food | `LIVESTOCK` | A managed, renewable animal supply. |
| Food | `LEATHER` | Hides and leather supplied through animals or processing. |
| Food | `WOOL` | Fleece and other wool inputs supplied through livestock. |
| Materials | `LOGS` | Raw timber supplied by lumber work. |
| Materials | `PLANKS` | Processed lumber supplied by lumber work. |
| Materials | `STONE` | Raw construction stone supplied through mining. |
| Materials | `CUT_STONE` | Processed masonry supplied by stoneworking. |
| Materials | `ORES` | Metallic and valuable ore supplied through mining. |
| Materials | `MINERALS` | Broader mined resources; this does not promise fuel. |
| Craft | `REPAIR` | Maintenance of tools, weapons, armor, and equipment. |
| Craft | `SMELTING` | Processing capacity for ores and other smeltable materials. |
| Craft | `TOOLS_IRON` | Production of iron tools, swords, and buckets. |
| Craft | `ARMOR_IRON` | Production of iron armor. |
| Craft | `SHIELDS` | Production of shields. |
| Trade | `TRADE` | Player-facing or village-facing commerce. |
| Trade | `TRADE_INITIATIVE` | Autonomous trade outreach beyond passive commerce. |

## Authoring contract

When an agent adds a building, it should inspect the existing catalog and name the nearest
functional peers before choosing a price. The comparison should cover beds and room ownership,
every job post, routed worksites, grants, shared and personal storage, upgrade behavior, and which
materials the village can produce before the building exists. The new definition then carries a
complete explicit `cost`; omitting it is an authoring error rather than permission to inherit a
category price.

These two authored fields stay paired at the decision seam: every option presented to the village
brain includes its effective cost and its complete grant contract. A conditional grant is shown
with the capabilities or stored supplies it requires rather than being promised as immediate.

The authoring note should explain meaningful deviations from the peers in one sentence. Equivalent
buildings may intentionally receive equal recipes. Independence means each price is chosen and
stored on its building variant, not that all 224 totals must be numerically unique.

## Complete authored catalog

## Badlands (30)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_badlands_1` | jobs baker; 1 shared container | STORAGE / FOOD / BAKED_GOODS | 12 oak log, 20 cobblestone | 32 |
| `blacksmith_badlands_1` | jobs blacksmith; 2 shared containers | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 12 oak log, 52 cobblestone | 64 |
| `butchery_badlands_1` | 2 beds; 1 couple room; jobs butcher; 1 personal container | HOUSING / FAMILY_HOUSING / FOOD / MEAT / LEATHER | 16 oak log, 40 cobblestone | 56 |
| `church_badlands_1` | 1 bed; jobs cleric; 1 shared container | HOUSING / STORAGE / HEALING | 12 oak log, 40 cobblestone | 52 |
| `farm_badlands_1` | 2 beds; 1 couple room; jobs farmer; 1 personal container | HOUSING / FAMILY_HOUSING / FOOD / CROPS | 16 oak log, 40 cobblestone | 56 |
| `fishery_badlands_1` | 1 bed; jobs fisher; 1 personal container | HOUSING / FOOD / FISH | 16 oak log, 32 cobblestone | 48 |
| `house_badlands_1` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_badlands_1__small_house_2` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_badlands_1__small_house_3` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 12 oak log, 20 cobblestone | 32 |
| `house_badlands_1__small_house_4` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_badlands_1__small_house_5` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_badlands_1__small_house_6` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_badlands_2` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_badlands_2__large_house_2` | 2 beds; 1 personal container | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_badlands_2__large_house_4` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_badlands_2__medium_house_2` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_badlands_3` | 4 beds; 1 couple room; 2 personal containers | HOUSING / FAMILY_HOUSING | 20 oak log, 28 cobblestone | 48 |
| `house_badlands_3__large_house_3` | 6 beds; 2 couple rooms; 2 personal containers | HOUSING / FAMILY_HOUSING | 24 oak log, 44 cobblestone | 68 |
| `hunting_lodge_badlands_1` | jobs hunter; 1 shared container | STORAGE / FOOD / MEAT / LEATHER | 12 oak log, 28 cobblestone | 40 |
| `lumberjack_badlands_1` | 1 bed; jobs lumberjack; 1 personal container | HOUSING / LOGS / PLANKS | 36 cobblestone | 36 |
| `market_badlands_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 12 oak log, 24 cobblestone | 36 |
| `market_badlands_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 16 oak log, 44 cobblestone, 8 white wool | 68 |
| `market_badlands_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 20 oak log, 52 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_badlands_1` | jobs miner; 1 shared container | STORAGE / STONE / ORES / MINERALS | 12 oak log, 32 cobblestone | 44 |
| `stoneworks_badlands_1` | jobs mason; 1 shared container | STORAGE / CUT_STONE | 8 oak log, 28 cobblestone | 36 |
| `storehouse_badlands_1` | 5 shared containers | STORAGE | 8 oak log, 16 cobblestone | 24 |
| `storehouse_badlands_2` | 1 bed; jobs quartermaster; 9 shared containers; 1 personal container | LOGISTICS / HOUSING / STORAGE | 16 oak log, 44 cobblestone | 60 |
| `tavern_badlands_1` | 2 beds; jobs innkeeper; 5 shared containers | HOUSING / HOSPITALITY / WANDERERS / STORAGE | 24 oak log, 40 cobblestone | 64 |
| `village_center_badlands_1` | 10 beds; jobs quartermaster/builder/guard/guard/guard/guard/guard; 3 shared containers; 7 personal containers | CIVIC_CENTER / CONSTRUCTION / LOGISTICS / HOUSING / PROTECTION / STORAGE / WATER | 64 oak log, 148 cobblestone | 212 |
| `watchtower_badlands_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 8 oak log, 32 cobblestone | 40 |

## Birch Forest (23)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_birch_forest_1` | 1 bed; jobs baker; 1 shared container; 1 personal container | HOUSING / STORAGE / FOOD / BAKED_GOODS | 24 oak log, 20 cobblestone | 44 |
| `blacksmith_birch_forest_1` | jobs blacksmith; 1 shared container | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 24 oak log, 40 cobblestone | 64 |
| `butchery_birch_forest_1` | 1 bed; jobs butcher/herder; 1 shared container; 1 personal container | HOUSING / STORAGE / FOOD / MEAT / LIVESTOCK / LEATHER / WOOL | 32 oak log, 32 cobblestone | 64 |
| `church_birch_forest_1` | jobs cleric; 1 shared container | STORAGE / HEALING | 16 oak log, 28 cobblestone | 44 |
| `couple_cottage_birch_forest_1` | 2 beds; 1 personal container | HOUSING / FAMILY_HOUSING | 16 oak log, 12 cobblestone | 28 |
| `farm_birch_forest_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 16 oak log, 20 cobblestone | 36 |
| `farm_birch_forest_2` | jobs farmer; 2 shared containers | STORAGE / FOOD / CROPS | 24 oak log, 28 cobblestone | 52 |
| `fishery_birch_forest_1` | jobs fisher; 1 shared container | STORAGE / FOOD / FISH | 20 oak log, 20 cobblestone | 40 |
| `house_birch_forest_1` | 1 bed; 1 personal container | HOUSING | 12 oak log, 8 cobblestone | 20 |
| `house_birch_forest_2` | 2 beds; 1 personal container | HOUSING | 16 oak log, 12 cobblestone | 28 |
| `hunting_lodge_birch_forest_1` | 1 bed; jobs hunter; 1 personal container | HOUSING / FOOD / MEAT / LEATHER | 24 oak log, 24 cobblestone | 48 |
| `lumberjack_birch_forest_1` | jobs lumberjack; 1 shared container | STORAGE / LOGS / PLANKS | 28 cobblestone | 28 |
| `market_birch_forest_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 16 oak log, 20 cobblestone | 36 |
| `market_birch_forest_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 28 oak log, 32 cobblestone, 8 white wool | 68 |
| `market_birch_forest_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 36 oak log, 36 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_birch_forest_1` | jobs miner; 1 shared container | STORAGE / STONE / ORES / MINERALS | 20 oak log, 24 cobblestone | 44 |
| `stoneworks_birch_forest_1` | jobs mason; 1 shared container | STORAGE / CUT_STONE | 12 oak log, 24 cobblestone | 36 |
| `storehouse_birch_forest_1` | jobs quartermaster; 6 shared containers | LOGISTICS / STORAGE | 20 oak log, 24 cobblestone | 44 |
| `tavern_birch_forest_1` | 1 bed; jobs innkeeper; 1 shared container; 1 personal container | HOUSING / HOSPITALITY / WANDERERS / STORAGE | 28 oak log, 24 cobblestone | 52 |
| `village_center_birch_forest_1` | 4 beds; jobs builder/builder/builder/guard; 1 personal container | CIVIC_CENTER / CONSTRUCTION / HOUSING / PROTECTION | 52 oak log, 56 cobblestone | 108 |
| `watchtower_birch_forest_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 16 oak log, 24 cobblestone | 40 |
| `watchtower_birch_forest_2` | 2 beds; jobs guard/guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 24 oak log, 36 cobblestone | 60 |
| `well_birch_forest_1` | none | WATER | 16 cobblestone | 16 |

## Desert (31)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_desert_1` | 1 bed; jobs baker; 1 shared container; 1 personal container | HOUSING / STORAGE / FOOD / BAKED_GOODS | 16 oak log, 28 cobblestone | 44 |
| `blacksmith_desert_1` | 1 bed; jobs blacksmith; 1 shared container; 1 personal container | HOUSING / STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 12 oak log, 60 cobblestone | 72 |
| `butchery_desert_1` | 1 bed; jobs butcher; 1 personal container | HOUSING / FOOD / MEAT / LEATHER / WOOL | 16 oak log, 36 cobblestone | 52 |
| `castle_desert_1` | 8 beds; 1 couple room; jobs blacksmith/guard/guard/guard/guard/leader/guard/merchant; 3 shared containers; 4 personal containers | GOVERNANCE / HOUSING / FAMILY_HOUSING / PROTECTION / CUSTODY / STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS / TRADE | 40 oak log, 176 cobblestone, 16 iron ingot | 232 |
| `church_desert_1` | jobs cleric; 1 shared container | STORAGE / HEALING | 8 oak log, 36 cobblestone | 44 |
| `couple_cottage_desert_1` | 2 beds; 1 couple room; 3 shared containers; 2 personal containers | HOUSING / FAMILY_HOUSING / STORAGE | 12 oak log, 24 cobblestone | 36 |
| `farm_desert_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 12 oak log, 24 cobblestone | 36 |
| `fishery_desert_1` | 1 bed; jobs fisher; 1 personal container | HOUSING / FOOD / FISH | 12 oak log, 36 cobblestone | 48 |
| `house_desert_1` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_desert_1__small_house_2` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_desert_1__small_house_3` | 2 beds; 1 personal container | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_desert_1__small_house_4` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_desert_1__small_house_5` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_desert_1__small_house_6` | 2 beds; 3 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_desert_2` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_desert_2__medium_house_2` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 12 oak log, 20 cobblestone | 32 |
| `house_desert_3` | 2 beds; 3 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `hunting_lodge_desert_1` | 1 bed; jobs hunter; 1 personal container | HOUSING / FOOD / MEAT / LEATHER | 12 oak log, 36 cobblestone | 48 |
| `lumberjack_desert_1` | 1 bed; jobs lumberjack; 1 personal container | HOUSING / LOGS / PLANKS | 36 cobblestone | 36 |
| `market_desert_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 12 oak log, 24 cobblestone | 36 |
| `market_desert_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 16 oak log, 44 cobblestone, 8 white wool | 68 |
| `market_desert_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 20 oak log, 52 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_desert_1` | 1 bed; jobs miner; 1 shared container; 1 personal container | HOUSING / STORAGE / STONE / ORES / MINERALS | 16 oak log, 40 cobblestone | 56 |
| `stoneworks_desert_1` | 1 bed; jobs mason; 1 personal container | HOUSING / CUT_STONE | 8 oak log, 36 cobblestone | 44 |
| `storehouse_desert_1` | 3 shared containers | STORAGE | 4 oak log, 16 cobblestone | 20 |
| `storehouse_desert_2` | 1 bed; jobs quartermaster; 8 shared containers; 1 personal container | LOGISTICS / HOUSING / STORAGE | 16 oak log, 40 cobblestone | 56 |
| `tavern_desert_1` | 5 beds; jobs innkeeper; 1 shared container; 1 personal container | HOUSING / HOSPITALITY / WANDERERS / STORAGE | 32 oak log, 52 cobblestone | 84 |
| `village_center_desert_1` | 4 beds; jobs quartermaster/builder/guard; 4 personal containers | CIVIC_CENTER / CONSTRUCTION / LOGISTICS / HOUSING / PROTECTION | 24 oak log, 68 cobblestone | 92 |
| `watchtower_desert_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 8 oak log, 32 cobblestone | 40 |
| `watchtower_desert_2` | 1 bed; jobs guard; 1 shared container; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS / STORAGE | 8 oak log, 32 cobblestone | 40 |
| `well_desert_1` | none | WATER | 16 cobblestone | 16 |

## Floodplain (20)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `blacksmith_floodplain_1` | jobs blacksmith; 1 shared container | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 32 oak log, 32 cobblestone | 64 |
| `butchery_floodplain_1` | 1 bed; jobs butcher; 1 shared container | HOUSING / STORAGE / FOOD / MEAT / LEATHER | 28 oak log, 20 cobblestone | 48 |
| `church_floodplain_1` | jobs cleric; 1 shared container | STORAGE / HEALING | 20 oak log, 20 cobblestone | 40 |
| `couple_cottage_floodplain_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 20 oak log, 12 cobblestone | 32 |
| `farm_floodplain_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 20 oak log, 16 cobblestone | 36 |
| `farm_floodplain_2` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 32 oak log, 20 cobblestone | 52 |
| `fishery_floodplain_1` | jobs fisher; 1 shared container | STORAGE / FOOD / FISH | 24 oak log, 16 cobblestone | 40 |
| `house_floodplain_1` | 1 bed | HOUSING | 12 oak log, 4 cobblestone | 16 |
| `house_floodplain_2` | 2 beds; 1 personal container | HOUSING | 20 oak log, 8 cobblestone | 28 |
| `hunting_lodge_floodplain_1` | 1 bed; jobs hunter; 1 shared container | HOUSING / STORAGE / FOOD / MEAT / LEATHER | 28 oak log, 20 cobblestone | 48 |
| `lumberjack_floodplain_1` | 1 bed; jobs lumberjack | HOUSING / LOGS / PLANKS | 32 cobblestone | 32 |
| `market_floodplain_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 20 oak log, 16 cobblestone | 36 |
| `market_floodplain_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 36 oak log, 24 cobblestone, 8 white wool | 68 |
| `market_floodplain_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 40 oak log, 32 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_floodplain_1` | jobs miner; 1 shared container | STORAGE / STONE / ORES / MINERALS | 24 oak log, 20 cobblestone | 44 |
| `stoneworks_floodplain_1` | 1 bed; jobs mason; 1 shared container | HOUSING / STORAGE / CUT_STONE | 20 oak log, 24 cobblestone | 44 |
| `storehouse_floodplain_1` | 3 shared containers | STORAGE | 12 oak log, 8 cobblestone | 20 |
| `village_center_floodplain_1` | jobs quartermaster/builder/guard | CIVIC_CENTER / CONSTRUCTION / LOGISTICS / PROTECTION | 32 oak log, 24 cobblestone | 56 |
| `watchtower_floodplain_1` | 1 bed; jobs guard; 1 shared container | HOUSING / PROTECTION / RANGED_GUARD_POSTS / STORAGE | 20 oak log, 20 cobblestone | 40 |
| `well_floodplain_1` | none | WATER | 16 cobblestone | 16 |

## Jungle (22)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_jungle_1` | jobs baker; 1 shared container | STORAGE / FOOD / BAKED_GOODS | 24 oak log, 8 cobblestone | 32 |
| `blacksmith_jungle_1` | jobs blacksmith; 1 shared container | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 32 oak log, 32 cobblestone | 64 |
| `butchery_jungle_1` | 1 bed; jobs butcher; 1 personal container | HOUSING / FOOD / MEAT / LEATHER | 28 oak log, 20 cobblestone | 48 |
| `church_jungle_1` | jobs cleric; 1 shared container | STORAGE / HEALING | 24 oak log, 20 cobblestone | 44 |
| `couple_cottage_jungle_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 24 oak log, 8 cobblestone | 32 |
| `farm_jungle_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 24 oak log, 12 cobblestone | 36 |
| `fishery_jungle_1` | 1 bed; jobs fisher; 1 shared container | HOUSING / STORAGE / FOOD / FISH | 28 oak log, 20 cobblestone | 48 |
| `house_jungle_1` | 1 bed; 1 personal container | HOUSING | 16 oak log, 4 cobblestone | 20 |
| `house_jungle_1__small_house_4` | 1 bed | HOUSING | 12 oak log, 4 cobblestone | 16 |
| `house_jungle_2` | 2 beds; 2 personal containers | HOUSING | 20 oak log, 8 cobblestone | 28 |
| `house_jungle_2__large_house_1` | 2 beds; 2 personal containers | HOUSING | 20 oak log, 8 cobblestone | 28 |
| `hunting_lodge_jungle_1` | jobs hunter; 1 shared container | STORAGE / FOOD / MEAT / LEATHER | 24 oak log, 16 cobblestone | 40 |
| `lumberjack_jungle_1` | jobs lumberjack; 1 shared container | STORAGE / LOGS / PLANKS | 28 cobblestone | 28 |
| `market_jungle_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 24 oak log, 12 cobblestone | 36 |
| `market_jungle_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 36 oak log, 24 cobblestone, 8 white wool | 68 |
| `market_jungle_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 44 oak log, 28 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_jungle_1` | worksites miner; 2 shared containers | STORAGE / STONE / ORES / MINERALS | 20 oak log, 12 cobblestone | 32 |
| `stoneworks_jungle_1` | jobs mason; 1 shared container | STORAGE / CUT_STONE | 20 oak log, 16 cobblestone | 36 |
| `storehouse_jungle_1` | worksites quartermaster; 3 shared containers | STORAGE | 16 oak log, 8 cobblestone | 24 |
| `tavern_jungle_1` | jobs innkeeper; 1 shared container | HOSPITALITY / WANDERERS / STORAGE | 28 oak log, 12 cobblestone | 40 |
| `village_center_jungle_1` | jobs quartermaster/builder/guard/miner | CIVIC_CENTER / CONSTRUCTION / PROTECTION | 48 oak log, 32 cobblestone | 80 |
| `watchtower_jungle_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 20 oak log, 20 cobblestone | 40 |

## Mediterranean (24)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_mediterranean_1` | 1 bed; jobs baker; worksites quartermaster; 3 shared containers; 1 personal container | HOUSING / STORAGE / FOOD / BAKED_GOODS | 24 oak log, 32 cobblestone | 56 |
| `blacksmith_mediterranean_1` | 1 bed; jobs blacksmith; 1 shared container; 1 personal container | HOUSING / STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 20 oak log, 52 cobblestone | 72 |
| `butchery_mediterranean_1` | jobs farmer/butcher; 2 shared containers | STORAGE / FOOD / CROPS / MEAT / LEATHER | 24 oak log, 40 cobblestone | 64 |
| `castle_mediterranean_1` | 4 beds; 1 couple room; jobs baker/blacksmith/leader/guard/guard/guard/guard/guard; 6 shared containers; 2 personal containers | GOVERNANCE / HOUSING / FAMILY_HOUSING / PROTECTION / CUSTODY / STORAGE / FOOD / BAKED_GOODS / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 48 oak log, 136 cobblestone, 16 iron ingot | 200 |
| `couple_cottage_mediterranean_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 16 oak log, 16 cobblestone | 32 |
| `farm_mediterranean_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 12 oak log, 24 cobblestone | 36 |
| `farm_mediterranean_2` | jobs farmer | FOOD / CROPS | 16 oak log, 32 cobblestone | 48 |
| `house_mediterranean_1` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_mediterranean_1__couple_room` | 3 beds; 1 couple room; 2 personal containers | HOUSING / FAMILY_HOUSING | 16 oak log, 24 cobblestone | 40 |
| `house_mediterranean_1__small_4` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_mediterranean_1__small_5` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_mediterranean_1__two_bed` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `house_mediterranean_1__two_room` | 2 beds; 2 personal containers | HOUSING | 12 oak log, 16 cobblestone | 28 |
| `hunting_lodge_mediterranean_1` | 2 beds; jobs hunter/blacksmith; 2 shared containers; 1 personal container | HOUSING / STORAGE / FOOD / MEAT / LEATHER / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 36 oak log, 60 cobblestone | 96 |
| `lumberjack_mediterranean_1` | worksites lumberjack/quartermaster; 3 shared containers | STORAGE / LOGS / PLANKS | 40 cobblestone | 40 |
| `market_mediterranean_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 12 oak log, 24 cobblestone | 36 |
| `market_mediterranean_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 20 oak log, 40 cobblestone, 8 white wool | 68 |
| `market_mediterranean_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 24 oak log, 48 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_mediterranean_1` | 1 bed; jobs miner; 1 personal container | HOUSING / STONE / ORES / MINERALS | 20 oak log, 32 cobblestone | 52 |
| `stoneworks_mediterranean_1` | 2 beds; jobs mason/farmer; 2 shared containers; 2 personal containers | HOUSING / STORAGE / FOOD / CROPS / CUT_STONE | 20 oak log, 60 cobblestone | 80 |
| `storehouse_mediterranean_1` | worksites quartermaster; 4 shared containers | STORAGE | 12 oak log, 16 cobblestone | 28 |
| `village_center_mediterranean_1` | jobs quartermaster/builder/guard/cleric; 1 shared container | CIVIC_CENTER / CONSTRUCTION / PROTECTION / STORAGE / HEALING | 32 oak log, 60 cobblestone | 92 |
| `watchtower_mediterranean_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 12 oak log, 28 cobblestone | 40 |
| `well_mediterranean_1` | none | WATER | 16 cobblestone | 16 |

## Romanian (24)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_romanian_1` | jobs baker; 1 shared container | STORAGE / FOOD / BAKED_GOODS | 16 oak log, 16 cobblestone | 32 |
| `blacksmith_romanian_1` | jobs blacksmith; 1 shared container | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 24 oak log, 40 cobblestone | 64 |
| `butchery_romanian_1` | 1 bed; jobs butcher; 1 personal container | HOUSING / FOOD / MEAT / LEATHER | 24 oak log, 24 cobblestone | 48 |
| `butchery_romanian_1__herder_home` | 2 beds; jobs butcher; 1 shared container; 1 personal container | HOUSING / STORAGE / FOOD / MEAT / LEATHER | 32 oak log, 28 cobblestone | 60 |
| `couple_cottage_romanian_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 20 oak log, 12 cobblestone | 32 |
| `farm_romanian_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 16 oak log, 20 cobblestone | 36 |
| `fishery_romanian_1` | jobs fisher; 1 shared container | STORAGE / FOOD / FISH | 20 oak log, 20 cobblestone | 40 |
| `house_romanian_1` | 1 bed; 1 personal container | HOUSING | 12 oak log, 8 cobblestone | 20 |
| `house_romanian_1__four_single` | 4 beds; 2 personal containers | HOUSING | 28 oak log, 16 cobblestone | 44 |
| `house_romanian_1__small_three` | 1 bed; 1 personal container | HOUSING | 12 oak log, 8 cobblestone | 20 |
| `house_romanian_1__two_single` | 2 beds; 1 personal container | HOUSING | 16 oak log, 12 cobblestone | 28 |
| `hunting_lodge_romanian_1` | jobs hunter; 1 shared container | STORAGE / FOOD / MEAT / LEATHER | 20 oak log, 20 cobblestone | 40 |
| `lumberjack_romanian_1` | 1 bed; jobs lumberjack; 1 shared container; 1 personal container | HOUSING / STORAGE / LOGS / PLANKS | 40 cobblestone | 40 |
| `market_romanian_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 16 oak log, 20 cobblestone | 36 |
| `market_romanian_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 28 oak log, 32 cobblestone, 8 white wool | 68 |
| `market_romanian_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 36 oak log, 36 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_romanian_1` | 1 bed; worksites miner; 1 shared container; 1 personal container | HOUSING / STORAGE / STONE / ORES / MINERALS | 24 oak log, 28 cobblestone | 52 |
| `stoneworks_romanian_1` | jobs mason; 1 shared container | STORAGE / CUT_STONE | 16 oak log, 20 cobblestone | 36 |
| `storehouse_romanian_1` | 3 beds; worksites quartermaster; 4 shared containers; 2 personal containers | HOUSING / STORAGE | 32 oak log, 28 cobblestone | 60 |
| `storehouse_romanian_1__compact` | worksites quartermaster; 5 shared containers | STORAGE | 16 oak log, 16 cobblestone | 32 |
| `tavern_romanian_1` | 3 beds; jobs innkeeper; 3 shared containers | HOUSING / HOSPITALITY / WANDERERS / STORAGE | 40 oak log, 32 cobblestone | 72 |
| `village_center_romanian_1` | jobs quartermaster/builder/guard/miner/cleric; 1 shared container | CIVIC_CENTER / CONSTRUCTION / LOGISTICS / PROTECTION / STORAGE / HEALING | 52 oak log, 52 cobblestone | 104 |
| `watchtower_romanian_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 16 oak log, 24 cobblestone | 40 |
| `well_romanian_1` | none | WATER | 16 cobblestone | 16 |

## Swamp (27)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_swamp_1` | jobs baker; 1 shared container | STORAGE / FOOD / BAKED_GOODS | 20 oak log, 12 cobblestone | 32 |
| `blacksmith_swamp_1` | jobs blacksmith; 1 shared container | STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 32 oak log, 32 cobblestone | 64 |
| `butchery_swamp_1` | 1 bed; jobs butcher; 1 personal container | HOUSING / FOOD / MEAT / LEATHER | 28 oak log, 20 cobblestone | 48 |
| `castle_swamp_1` | 11 beds; 1 couple room; jobs baker/merchant/blacksmith/leader/guard/guard/guard/guard/guard/guard/guard/guard; 2 shared containers; 5 personal containers | GOVERNANCE / HOUSING / FAMILY_HOUSING / PROTECTION / CUSTODY / STORAGE / FOOD / BAKED_GOODS / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS / TRADE | 144 oak log, 152 cobblestone, 16 iron ingot | 312 |
| `church_swamp_1` | jobs cleric; 1 shared container | STORAGE / HEALING | 20 oak log, 24 cobblestone | 44 |
| `church_swamp_1__cleric_couple_home` | 2 beds; 1 couple room; jobs cleric; 1 personal container | HOUSING / FAMILY_HOUSING / HEALING | 32 oak log, 32 cobblestone | 64 |
| `church_swamp_1__cleric_home` | 1 bed; jobs cleric; 1 shared container; 1 personal container | HOUSING / STORAGE / HEALING | 24 oak log, 28 cobblestone | 52 |
| `church_swamp_1__cleric_mixed_home` | 2 beds; jobs cleric; 1 shared container; 1 personal container | HOUSING / STORAGE / HEALING | 28 oak log, 32 cobblestone | 60 |
| `couple_cottage_swamp_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 20 oak log, 12 cobblestone | 32 |
| `farm_swamp_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 20 oak log, 16 cobblestone | 36 |
| `farm_swamp_2` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 32 oak log, 20 cobblestone | 52 |
| `fishery_swamp_1` | jobs fisher; 1 shared container | STORAGE / FOOD / FISH | 24 oak log, 16 cobblestone | 40 |
| `house_swamp_1` | 1 bed; 1 personal container | HOUSING | 12 oak log, 8 cobblestone | 20 |
| `house_swamp_1__two_bed` | 2 beds; 1 personal container | HOUSING | 20 oak log, 8 cobblestone | 28 |
| `hunting_lodge_swamp_1` | jobs hunter; 1 shared container | STORAGE / FOOD / MEAT / LEATHER | 24 oak log, 16 cobblestone | 40 |
| `lumberjack_swamp_1` | worksites lumberjack | LOGS / PLANKS | 24 cobblestone | 24 |
| `market_swamp_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 20 oak log, 16 cobblestone | 36 |
| `market_swamp_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 36 oak log, 24 cobblestone, 8 white wool | 68 |
| `market_swamp_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 40 oak log, 32 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_swamp_1` | worksites miner | STONE / ORES / MINERALS | 16 oak log, 12 cobblestone | 28 |
| `stoneworks_swamp_1` | jobs mason; 1 shared container | STORAGE / CUT_STONE | 16 oak log, 20 cobblestone | 36 |
| `storehouse_swamp_1` | worksites quartermaster; 3 shared containers | STORAGE | 12 oak log, 12 cobblestone | 24 |
| `tavern_swamp_1` | 5 beds; jobs innkeeper | HOUSING / HOSPITALITY / WANDERERS | 52 oak log, 28 cobblestone | 80 |
| `village_center_swamp_1` | jobs quartermaster/builder/guard/miner | CIVIC_CENTER / CONSTRUCTION / PROTECTION | 48 oak log, 32 cobblestone | 80 |
| `watchtower_swamp_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 20 oak log, 20 cobblestone | 40 |
| `watchtower_swamp_2` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 20 oak log, 20 cobblestone | 40 |
| `well_swamp_1` | none | WATER | 16 cobblestone | 16 |

## Tundra (23)
| Building | Other value | Grants | Recipe | Total |
| --- | --- | --- | --- | ---: |
| `bakery_tundra_1` | 1 bed; jobs baker | HOUSING / FOOD / BAKED_GOODS | 20 oak log, 20 cobblestone | 40 |
| `blacksmith_tundra_1` | 1 bed; jobs blacksmith; 1 shared container; 1 personal container | HOUSING / STORAGE / REPAIR / SMELTING / TOOLS_IRON / ARMOR_IRON / SHIELDS | 24 oak log, 48 cobblestone | 72 |
| `butchery_tundra_1` | jobs butcher; 1 shared container | STORAGE / FOOD / MEAT / LEATHER | 16 oak log, 24 cobblestone | 40 |
| `church_tundra_1` | 1 bed; jobs cleric; 1 personal container | HOUSING / HEALING | 16 oak log, 36 cobblestone | 52 |
| `couple_cottage_tundra_1` | 2 beds; 1 couple room; 1 personal container | HOUSING / FAMILY_HOUSING | 16 oak log, 16 cobblestone | 32 |
| `farm_tundra_1` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 16 oak log, 20 cobblestone | 36 |
| `farm_tundra_2` | jobs farmer; 1 shared container | STORAGE / FOOD / CROPS | 20 oak log, 32 cobblestone | 52 |
| `fishery_tundra_1` | 1 bed; jobs fisher; 1 shared container | HOUSING / STORAGE / FOOD / FISH | 20 oak log, 28 cobblestone | 48 |
| `house_tundra_1` | 1 bed; 1 personal container | HOUSING | 8 oak log, 12 cobblestone | 20 |
| `house_tundra_1__three_single` | 3 beds; 1 shared container | HOUSING / STORAGE | 16 oak log, 20 cobblestone | 36 |
| `house_tundra_1__two_single` | 2 beds; 2 personal containers | HOUSING | 16 oak log, 12 cobblestone | 28 |
| `hunting_lodge_tundra_1` | 1 bed; jobs hunter; 1 shared container; 1 personal container | HOUSING / STORAGE / FOOD / MEAT / LEATHER | 20 oak log, 28 cobblestone | 48 |
| `lumberjack_tundra_1` | 1 bed; jobs lumberjack; 1 shared container; 1 personal container | HOUSING / STORAGE / LOGS / PLANKS | 40 cobblestone | 40 |
| `market_tundra_1` | jobs merchant; 1 shared container | STORAGE / TRADE | 16 oak log, 20 cobblestone | 36 |
| `market_tundra_2` | jobs merchant/merchant; 2 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 24 oak log, 36 cobblestone, 8 white wool | 68 |
| `market_tundra_3` | jobs merchant/merchant/merchant; 3 shared containers | STORAGE / TRADE / TRADE_INITIATIVE | 32 oak log, 40 cobblestone, 12 white wool, 4 iron ingot | 88 |
| `mine_tundra_1` | worksites miner; 1 shared container | STORAGE / STONE / ORES / MINERALS | 12 oak log, 16 cobblestone | 28 |
| `stoneworks_tundra_1` | 1 bed; jobs mason; 1 shared container; 1 personal container | HOUSING / STORAGE / CUT_STONE | 16 oak log, 28 cobblestone | 44 |
| `storehouse_tundra_1` | worksites quartermaster; 2 shared containers | STORAGE | 12 oak log, 12 cobblestone | 24 |
| `tavern_tundra_1` | 5 beds; jobs innkeeper/innkeeper; 4 personal containers | HOUSING / HOSPITALITY / WANDERERS | 48 oak log, 52 cobblestone | 100 |
| `village_center_tundra_1` | 4 beds; jobs quartermaster/builder/guard/miner; 4 personal containers | CIVIC_CENTER / CONSTRUCTION / HOUSING / PROTECTION | 48 oak log, 68 cobblestone | 116 |
| `watchtower_tundra_1` | 1 bed; jobs guard; 1 personal container | HOUSING / PROTECTION / RANGED_GUARD_POSTS | 12 oak log, 28 cobblestone | 40 |
| `well_tundra_1` | none | WATER | 16 cobblestone | 16 |
