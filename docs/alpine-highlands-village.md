# Alpine Highlands village

The Alpine Highlands catalog is the Iberian-inspired mountain village selected from the Alpine
and Iberian galleries. Brick walls and lower courses, steep spruce roofs, berry plots and large
shared houses give it a warmer identity than the frozen Tundra settlement. The style token is
`alpine_highlands`.

Meadow, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, Stony Peaks and the three vanilla
Windswept mountain biomes select it while its private founding catalog is loaded. The mountain
mapping runs before the broad snowy mapping, so Snowy Slopes and Frozen Peaks are Alpine while
Snowy Plains, Ice Spikes and frozen lowlands remain Tundra. Datapacks can extend the assignment
with the `kithkyn:village_style/alpine_highlands` biome tag.

## Founding village

`village_center_alpine_highlands_1` is AL09.1. Its bell and nearby paved court are the meeting
place, and its campfire remains the village fire. It has no beds. The center opens the builder,
guard captain, farmer, quartermaster and miner positions; the farmer tends the center's berry
plots and uses its barrel.

The normal site planner places the mine, founding storehouse, two-bed home and one-bed home as
four companion buildings. They use the same spacing, inward-facing preference and terrain rules
as later construction. The storehouse and two homes provide exactly five beds for the five
founding workers.

## Selected buildings

The private datapack contains 22 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_alpine_highlands_1` | AL09.1 | Bedless civic center, berry farmer, bell, campfire and village banner |
| `church_alpine_highlands_1` | AL09.2 | Cleric station, cleric bed, general bed and two shared chests |
| `farm_alpine_highlands_1` | AL09.3 | Berry farm and farmer station |
| `house_alpine_highlands_1__family_house` | AL10.1 | Six physical beds, including one couple room, across a dense shared home |
| `house_alpine_highlands_1__five_bed` | AL10.2 | Five-bed shared home |
| `house_alpine_highlands_1__three_bed` | AL10.3 | Three-bed home |
| `house_alpine_highlands_1__two_bed` | AL10.9 | Two-bed founding home |
| `house_alpine_highlands_1` | AL10.11 | One-bed founding home |
| `blacksmith_alpine_highlands_1` | AL10.4 | Three-bed mixed smithy and stoneworks with blacksmith and mason positions |
| `bakery_alpine_highlands_1` | AL10.5 | Bakery, baker bed and one general bed |
| `bakery_alpine_highlands_1__baker_home` | AL10.10 | Compact bakery with the baker's bed |
| `hunting_lodge_alpine_highlands_1` | AL10.7 | Hunter station, hunter bed and one general bed |
| `storehouse_alpine_highlands_1` | AL10.8 | Founding storehouse, quartermaster worksite and two beds |
| `storehouse_alpine_highlands_1__compact` | AL10.12 | Additional compact storage building |
| `lumberjack_alpine_highlands_1` | AD03.1 | Lumberjack yard with one spruce sapling and a registered work barrel |
| `mine_alpine_highlands_1` | AD02.6 | Compact approved mine with a north-facing public entrance and south-running three-wide shaft |
| `well_alpine_highlands_1` | AD03.8 | User-edited all-brick well with a stair-and-slab crown, seated seven blocks below grade |
| `butchery_alpine_highlands_1` | AD02.5 | User-edited brick-and-spruce butchery with brick upper supports, a lantern, three cows and three chickens |
| `fishery_alpine_highlands_1` | IF01.4 | Selected brick-and-spruce fishery with a pond and barrel |
| `market_alpine_highlands_1`, `_2`, `_3` | AD05.1 to AD05.3 | One, two and three merchant stalls with the fixed trade colors |

The catalog deliberately omits a tavern, watchtower and castle. Those categories stay absent
until a fitting structure is selected; the strict catalog does not borrow another village's art.

## Identity and wall

Every bed is exported as a neutral slot and resolves to either the village primary or secondary
color. The two halves of the AL10.1 couple room always share one color. Authored civic and church
banners resolve to the generated village banner. Market fabrics keep the established red, cyan,
orange and white trade colors.

The single wall level uses the approved AD06 straight, diagonal, corner, gatehouse and terrace
modules. Brick forms the wall body, cap and rail; spruce trapdoors retain access. The irregular
mangrove-leaf brush is authored into each module with persistent leaves. The wall footprint is
unchanged. The review platform extends two blocks past the footprint so this brush is visible and
does not look clipped in the showcase.

The church, both bakeries, blacksmith, hunting lodge, both storehouses and five house variants
place one block higher than the first draft so their authored entrance courses meet the terrain.
The butchery, farm, fishery, lumberjack yard, markets, mine and center retain their approved
height. The well's revised sink raises it by one block while preserving its deep shaft.

People use a one-block step height when following a route, so raised entrance courses do not
strand them outside an open door or fence gate. At an authored stair whose trim leaves less than
standing headroom, navigation briefly uses the existing crouching pose and stands again after
the body clears the trim. Crouching collision height is capped at 1.75 blocks across genetic
scales. These movement rules leave every approved structure block intact.

## Authoring and verification

`run/alpine-integration/prepare.py` promotes the 22 approved captures through
`tools/structure/VillageTemplateExport.java`. It preserves authored geometry, clears only
gallery structure voids, neutralizes identity slots, empties containers and removes the
butchery animals' gallery-only tag. The compact mine also preserves omitted source cells instead
of exporting them as explicit air, so placing it does not clear surrounding terrain. Third-party-derived
structures and definitions stay in the local private datapack. Their source and output hashes are recorded in
`tools/structure/alpine-highlands-catalog-20260913.json`.

The catalog audit checks all 22 structures for barriers, out-of-bounds cells, terrain-clearing
air, malformed market floors and entrances, and missing butchery livestock. Native placement,
founding and physical-access verifiers exercise every rotation, identity color pair, frame,
entity, bed, work station, storage target and mine route. The separate body-height verifier
checks one-block child and two-block working-age passages across the full genetic scale range.

Manual testing can create it with `/kithkyn create-village ~ ~ ~ alpine_highlands`; natural
founding in a mapped mountain biome uses the same selection and founding path.
