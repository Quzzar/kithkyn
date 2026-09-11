# Swamp village

The Swamp catalog is the ordinary wetland village, separate from the mud-brick Floodplain
catalog used by mangrove swamps. It combines the approved Dungeons & Taverns Swamp and Towns
and Towers Boat Village buildings with the user-edited NF01.2 civic ruin, regional markets,
the approved one-stage wall, and the SC01.2 castle. Its style token is `swamp`. Vanilla Swamp,
the conventional Swamp biome tag, and recognizable modded biome paths containing `swamp`
select it whenever its complete private founding set is loaded. A path containing `mangrove`
is resolved to Floodplain first.

## Selected buildings

The private datapack contains 27 definitions:

| Id | Use |
| --- | --- |
| `village_center_swamp_1` | NF01.2 bell ruin; no beds; quartermaster, builder, guard captain, and miner vacancies; two separate campfires |
| `mine_swamp_1` | NE01.6 edited mine with a three-wide southward shaft frame |
| `storehouse_swamp_1` | NE03.2 storage house with three shared chests and the quartermaster's physical worksite |
| `house_swamp_1` | NE03.5 one-bed home |
| `house_swamp_1__two_bed` | NE03.8 two-bed home |
| `couple_cottage_swamp_1` | NE03.6 couple home; both bed slots share one village color |
| `church_swamp_1` | NF04.1 station-only cleric circle |
| `church_swamp_1__cleric_home` | NF04.4 one-bed cleric home |
| `church_swamp_1__cleric_couple_home` | NF04.3 cleric home with a couple room |
| `church_swamp_1__cleric_mixed_home` | NF04.2 two-room cleric home with one staff bed and one general bed |
| `blacksmith_swamp_1` | NE04.4 smithing yard |
| `well_swamp_1` | NE06.9 wetland well |
| `lumberjack_swamp_1` | NE01.5 timber yard; its marker becomes an oak sapling; the buried decorative barrel is not village storage |
| `bakery_swamp_1` | NE04.5 bakery |
| `butchery_swamp_1` | NE04.3 butcher home and pen with three black sheep and three pigs |
| `tavern_swamp_1` | NE06.10 five-bed tavern; the cartography-room bed belongs to the innkeeper; one obstructing upstairs trapdoor is omitted from the walking lane |
| `watchtower_swamp_1`, `_2` | NE01.8 and NE05.1 guard tower tiers |
| `stoneworks_swamp_1` | NF02.5 mason yard |
| `farm_swamp_1`, `_2` | NF02.7 and NF01.8 farm progression |
| `hunting_lodge_swamp_1` | NF01.7 hunter station |
| `fishery_swamp_1` | NE02.9 bank-seated fishery |
| `market_swamp_1`, `_2`, `_3` | Repaired shared market geometry in oak and spruce, retaining the established multicolor fabrics |
| `castle_swamp_1` | SC01.2 ruler, jail, baker, merchant, blacksmith, housing, and seven guard posts |

The two tower tiers use different footprints. A level-two tower is built on a new site rather
than replacing the smaller level-one tower in place.

## Founding and village identity

The center has no beds. Its founding companions are the mine, storehouse, and four one-bed
homes, all placed through the normal planner. They use the same terrain, spacing, and inward
facing rules as later construction, so the initial settlement has ordinary sprawl rather than
a fixed compound. The four homes house the four center workers. The center keeps those four
vacancies while the quartermaster and miner route to physical worksites in the separate
storehouse and mine.

The bell approach is the civic meeting point. Both authored campfires remain cooking and idle
amenities and neither replaces the bell as the center. Every assigned bed is neutral in the
template and becomes the village primary or secondary color. Couple halves share one color.
Authored village-banner slots become the village banner. Market carpets, wool, candles, and
decorative flags keep the shared red, cyan, orange, and white market palette.

## Wall and castle

Swamp uses the exact five approved wall captures as an authored regional segment family.
Mossy cobblestone forms the wet footing, oak logs and planks form the body, and spruce fences,
slabs, and trapdoors form the details. Every standing light is one lit candle. The gate keeps
four hanging lanterns. There is one wall stage, paid for with oak logs.

The castle uses the same ruler and custody systems as the Desert castle with its own SC01.2
layout. It has eleven resident beds, including the ruler's couple room; baker, merchant,
blacksmith, ruler, and jailer stations; two sword guards at the entrance; two crossbow guards on the
balconies; and three crossbow guards on the roof. Each guard follows its local authored route.

A guard's otherwise lethal response to a hostile player can begin a five-minute sentence.
Items that would normally drop on death move into the two evidence barrels above the cell door;
items that do not fit remain on the player. Evidence storage is excluded from village inventory
and taking it is not theft. The prisoner receives Mining Fatigue III and private minute-by-minute
messages, may escape physically, and is moved to the safe release point when the sentence ends.

## Authoring and installation

`run/swamp-integration/prepare.py` exports the 23 approved live captures, scrubs gallery barrier
and structure-void states, closes doors, clears one obstructing decorative trapdoor from each of
the NF04.2 cleric home and NE06.10 tavern, neutralizes identity slots, adds the approved livestock,
builds the three regional markets, and joins the completed castle into
`run/swamp-integration/datapack/`. The catalog hashes and source record are in
`tools/structure/swamp-catalog-20260911.json`. Third-party-derived building templates remain in
the private local datapack; only the authored wall segment family is bundled with the runtime.

Manual testing can create the style with `/kithkyn create-village ~ ~ ~ swamp`. Natural founding
in an ordinary swamp uses the same selector and founding path.

## Verification

The September 11 native run loaded all 27 strict templates and passed natural Swamp selection,
all four founding rotations, twelve supported in-place upgrades, and codec reloads. Placement
covered 216 instant and incremental/reload builds; a real server restart retained all 216 saved
buildings and 72 authored entities. Full-size adult villagers passed 384 access routes across 104
rotated non-center layouts, including 116 bed walks, 92 personal-container deposits, 80 shared
deposits, and both mine directions. The center added sixteen successful station walks.

The three market tiers were rendered after export. Their repaired counters, entrances and rails
were intact, their oak-and-spruce regional frame read coherently, and their red, cyan, orange and
white trade fabrics remained independent of village colors.

The castle-specific runs covered 28 sentry patrols, conserved merchant buys and sales, the full
custody and evidence matrix, and a paid incremental build. The planner offered the affordable
castle; an ordinary builder collected 64 oak logs, 128 cobblestone and 16 iron ingots from two
storehouses and completed it. Its eleven beds and twelve jobs were published only at completion.
