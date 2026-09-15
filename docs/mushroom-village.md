# Mushroom village

The Mushroom catalog is the fantasy village on the mushroom island. Houses stand under red and
brown mushroom caps on pale stems, oak gazebos on orange paving hold the trades around a meeting
point with its bell and a cleric's loft under the cap, mooshrooms graze in the pen, and a wall
of mushroom caps on a stem footing rings it all. It is Aaron's selection from the Mushroom
gallery he reviewed and edited live above the trio-20260912 test world on 2026-09-14, built on
the Towns & Towers mushroom-fields fantasy family; the CTOV mushroom village stood beside it and
was not chosen. The style token is `mushroom`.

It founds on Mushroom Fields, which carries the `kithkyn:village_style/mushroom` tag, and on
modded biomes tagged as mushroom or named for it ([buildings.md](buildings.md), "Current runtime
selection"). Before this catalog the island fell through to the climate clusters.

## Founding village

`village_center_mushroom_1` is the T&T mushroom meeting point (MU01.1): a mycelium and orange
concrete plaza around a bookshelf-walled trunk with a ladder up through the great cap. It
declares the five builder duty anchors on the plaza south of the trunk, the captain's post by
the campfire Aaron set at the plaza's south edge, and a cleric upstairs under the cap, before
the brewing stand. The one bed up there is meant for the cleric and open to any founder, as
every other centre's beds are (a room reserved for the cleric left a founder bedless while the
reservation waited, and the native centre check claims a building's first job for its worker
room, so the close-out dropped the reservation); the chest Aaron added is that bed's own and the
original chest is the station's store. His two
white banners on the trunk are village banners; the bell hangs on the trunk's west face.

With one bed at the centre, the founding set brings homes, as the Taiga's does:

- the mine (`mine_mushroom_1`)
- the storehouse (`storehouse_mushroom_1`)
- the two-bed house and the one-bed house, three beds

Those four beds sleep the founding builder, captain, cleric, miner and quartermaster, four of
the five; the fifth waits for the next bed. The meeting point is beside the fire, the village
fire is that campfire, and the bell is on the trunk. The founding companions are declared on the
centre (`starting_buildings`) so the site planner places them.

## Selected buildings

The private datapack contains 18 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_mushroom_1` | MU01.1 | The T&T meeting point: five builder anchors, the captain by the fire, the cleric's loft with its bed and chest, two village banners, the bell |
| `house_mushroom_1` | MU01.3 | T&T large house 1: two beds on the loft under the cap sharing the chest, reached by the spiral slab ramp round the stem |
| `house_mushroom_1__one_bed` | MU01.4 | T&T large house 2: one bed with its chest, the door on the east |
| `couple_cottage_mushroom_1` | MU01.6 | T&T small house 1: the paired beds on the upper floor, one unit; no chest |
| `storehouse_mushroom_1` | MU01.7 | T&T small house 2 with Aaron's four barrels, the town's storage, and the quartermaster among them |
| `mine_mushroom_1` | MU01.5 | The light professions gazebo with its interior cut one block into the ground inside the concrete ring: the miner in the hollow south of the anchor, the shaft descending north past the barrel he set in the floor, which is the store |
| `bakery_mushroom_1` | MU02.1 | The T&T butcher stall: the baker before the smoker, the chest at the north end |
| `butchery_mushroom_1` | MU02.2 | The T&T leatherworker's pen with four mooshrooms in it; the chest is the store, the gates on the east |
| `farm_mushroom_1` | MU02.3 | Aaron's rebuilt crop farm standing one block up on the ground: the farmer by the water, his barrel in its floor as the store |
| `well_mushroom_1` | MU12.5 | The Dungeons & Taverns jungle well, sunk seven layers so its rim stands one above the ground |
| `blacksmith_mushroom_1` | MU13.1 | The heavy gazebo with the armorer kit: the blast furnace, the chest |
| `stoneworks_mushroom_1` | MU13.2 | The heavy gazebo with the mason kit: the stonecutter, the chest |
| `fishery_mushroom_1` | MU13.6 | The light gazebo with the fisher kit over the pool Aaron dug into its floor; the barrel is the store |
| `hunting_lodge_mushroom_1` | MU13.7 | The light gazebo with the fletcher kit: the fletching table, the chest. No bed |
| `lumberjack_mushroom_1` | derived from MU01.5 | The light gazebo again, unedited, with an oak sapling stand three blocks west of it on its own mycelium and a barrel inside as the store |
| `market_mushroom_1`, `_2`, `_3` | MU14.1 to MU14.3 | The shared market geometry in oak with mushroom-stem posts on orange concrete paving, with the fixed trade colours |

There is no church (the cleric lives at the centre), no tavern, no watchtower and no castle:
Aaron left them out.

A few definitions differ from the gallery copies:

- **Lumberjack.** Not one of Aaron's picks; he left the lumberjack open. The island has no
  trees, and the planner's own rule is that a village with no way to make logs is stuck until
  it raises a lumberjack, which is paid for in stone alone, so the catalog carries one in the
  family's own gazebo with an oak sapling, the family's timber. The template box is three
  blocks wider than the gazebo, the stand's ground course is part of the template (mycelium),
  and the sapling is planted at stage 0. Swap or drop it at will.
- **Mine.** The gazebo's interior (five by three inside the ring) is explicit air on the ground
  course, which the export keeps for this piece alone, so the placement clears it and the miner
  stands one block down on the course below, as the gallery pad showed it and as Aaron cut it
  again on the height strip.
- **Farm and well.** One block higher than the first strip showed them (Aaron's height walk on
  2026-09-15): the farm's floor course sits on the ground (sink -1) and the well is sunk seven
  layers, not eight.
- **Fishery and farm.** Every water cell is a source.
- **Butchery.** The pen's gates start closed, as every door and gate where livestock lives does.
- **Markets.** The Birch market counters set each stall barrel into the deck with open air above
  it; the Mushroom markets keep the barrels where the study has them.

Everything else is Aaron's, block for block.

## Wall

The wall is study B of the gallery, the cap wall ([walls.md](walls.md)): the shared Birch
geometry with red mushroom blocks for its cobblestone, brown ones for its mossy cobblestone, oak
fence tips, oak slab walks and the oak hatches, on a course of mushroom stems that the catalog
seats on each column's own ground. Its five templates are bundled under
`data/kithkyn/structure/wall/mushroom/` and recorded in
`tools/structure/mushroom-walls-20260915.json`.

## Identity

Beds alternate between the village's primary and secondary colours by bed unit; a couple bed
is one unit. Every banner is the village banner: the two on the centre's trunk and the four on
each wall gatehouse. The markets keep the fixed Birch trade colours. The family's own accents
(orange concrete paving, cyan carpet at the centre, the red and brown caps) stay as authored.

## Costs and grants

Every definition carries its authored cost and the grants its category, rooms, storage and
local jobs imply, priced by analogy with the Taiga and Savanna catalogs in oak logs and
cobblestone: the centre 40 oak logs and 40 cobblestone, houses 16 to 20 logs, the gazebos 12
to 32 logs, the well 16 cobblestone, the lumberjack 28 cobblestone and no logs, the market
tiers 24, 36 and 44 logs with the shared wool and iron on the upper tiers.

## Verification

The close-out ran the native verifications on the worktree's jar with the private datapack:
placement and restart of every template, the founding checks in a mushroom-fields world, the
centre and access walks with the size-18 probe, and the gate-access walks on the Mushroom,
Polynesian and Nautical walls, with the static template audit and the unit tests. The verdicts
are in `tools/structure/mushroom-catalog-20260915.json`.

## Provenance

- Gallery: `tools/structure/mushroom-full-profile-20260914.json` (rows, sources, hashes,
  placements, the stand-in swap table, the studies and the selection).
- Catalog: `tools/structure/mushroom-catalog-20260915.json` (captures, definitions, costs,
  verdicts).
- Walls: `tools/structure/mushroom-walls-20260915.json`.
- Scripts: `run/mushroom-integration/` (capture, prepare, verify, finalize, records, the height
  strip); `run/` is gitignored, so the records above are what the repo keeps.
