# Taiga village

The Taiga catalog is the cold conifer-forest camp. Stripped spruce halls with steep spruce roofs
stand around a paved meeting point with a little watch tower, a timber fort with its jail and
the ruler's hall guards the town, a firewatch tower and a spruce tavern stand among the works,
and a palisade of stripped spruce on stone footings rings it all. It is Aaron's selection from
the Taiga gallery he reviewed and edited live above the trio-20260912 test world on 2026-09-14,
built on the Towns & Towers Viking family; the Polish and Swedish families stood beside it and
were not chosen. The style token is `taiga`.

It founds on Taiga, Old Growth Pine Taiga and Old Growth Spruce Taiga, which carry the
`kithkyn:village_style/taiga` tag, and on modded biomes tagged as taiga or named for it that are
not snowy ([buildings.md](buildings.md), "Current runtime selection"). Snowy Taiga stays with the
Tundra. Before this catalog the conifer forests fell through to the temperate cluster and built
the bundled Birch catalog.

## Founding village

`village_center_taiga_1` is the Viking meeting point (TA01.1): a paved plaza with the town fire
and the bell, and a small log tower with a ladder and a railed top. It declares the five builder
duty anchors on the paving, the captain's post by the fire and the bell, and a crossbow watch post
on the tower, in its north railing gate, which stands open so the guard has a cell to stand on
above the top stairs; the hatch at the ladder's top is one more rung, so the ladder tops out at
the platform. The four white banners Aaron hung on the tower are village banners.

The meeting point has no beds, so the founding set brings homes, as the Polynesian hall's does:

- the mine (`mine_taiga_1`)
- the storehouse (`storehouse_taiga_1`)
- the one-bed house, the two-bed house and the second one-bed house, four beds in all

Those four beds sleep the founding builder, captain, miner and quartermaster; the crossbow post is
the fifth founding vacancy and waits for a fifth bed. The meeting point is beside the fire, the
village fire is that campfire, and the bell hangs on its post by the plaza. The founding
companions are declared on the centre (`starting_buildings`) so the site planner places them.

## Selected buildings

The private datapack contains 23 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_taiga_1` | TA01.1 | The Viking meeting point: five builder anchors, the captain by the fire and bell, the crossbow post on the tower, four village banners, no beds |
| `house_taiga_1` | TA01.6 | Viking small house 5: one bed, with Aaron's barrel set in the floor beside it |
| `house_taiga_1__two_bed` | TA01.5 | Viking small house 4: two beds sharing the chest |
| `house_taiga_1__small_house_2` | TA01.3 | Viking small house 2: one bed with its chest, added at the close-out so the four founders all have beds |
| `couple_cottage_taiga_1` | TA01.4 | Viking small house 3 with the beds Aaron moved into a pair, and its barrel |
| `church_taiga_1` | TA01.7 | The Viking temple: the cleric on the floor between the altar's brewing stand and the chest. No bed |
| `storehouse_taiga_1` | TA01.2 | Viking small house 1 with Aaron's four barrels, the town's storage, and the quartermaster among them |
| `butchery_taiga_1` | TA02.2 | The Viking butcher with three chickens in its fenced yard; the smoker and chest inside |
| `bakery_taiga_1` | TA02.3 | The Viking cartographer's house with Aaron's smoker in the table's place |
| `hunting_lodge_taiga_1` | TA02.4 | The Viking fletcher's yard: the hunter by the fletching table, the chest as the store. No bed |
| `stoneworks_taiga_1` | TA02.7 | The Viking mason: the stonecutter, the chest; its red wool is the village's primary colour |
| `lumberjack_taiga_1` | TA02.8 | Aaron's rebuilt shepherd hut: the spruce sapling where his dead bush stood, the lumberjack's bed and chest inside, the barrel in the ground outside as the store |
| `blacksmith_taiga_1` | TA02.9 | The Viking toolsmith: the smithing table, the lava cauldron, the chest |
| `mine_taiga_1` | TA02.10 | The Viking weaponsmith hut with its centre dug out: the miner east of the hollow, the shaft descending west into it, the chest as the store |
| `fishery_taiga_1` | TA03.1 | The Viking fisher: the barrel by the pool is the store, the bed and its barrel are the fisher's |
| `farm_taiga_1` | TA03.2 | The Viking large farm with Aaron's barrel, captured from the height strip after his second pass: potatoes on the west beds, carrots on the channel banks and three tilled path cells |
| `well_taiga_1` | TA17.20 | The CTOV taiga well, its deepslate bricks traded for cobblestone, sunk two layers, one higher than the gallery showed it |
| `castle_taiga_1` | TA09.1 | The Towns & Towers old-growth taiga fort: the ruler's paired bed upstairs is the leader's room, four open castle beds two per chest, the one-cell jail with the jailer's post and the prisoner's two barrels, and the mason, cleric, blacksmith, hunter and baker at the tables Aaron set. Its nine banners are village banners |
| `watchtower_taiga_1` | TA09.6 | The Dungeons & Taverns firewatch tower: the guard's crossbow post on the south balcony above the cabin door, the guard's bed and chest in the cabin |
| `tavern_taiga_1` | TA28.1 | The Dungeons & Taverns spruce tavern house: the keeper by the smoker, four guest beds each with its barrel in the floor beside it, the keeper's bed and chest upstairs |
| `market_taiga_1`, `_2`, `_3` | TA35.1 to TA35.3 | The shared market geometry in spruce on a podzol floor, with the fixed trade colours |

A few definitions differ from the gallery copies:

- **Centre.** The north railing gate on the tower top is placed open, so the crossbow guard can
  stand in it on the top stair below; the open hatch over the ladder is a ladder rung, since the
  native walker stopped on the top rung under the hatch; and the paving cell under the ladder is
  plain dirt, since a walker standing on the dirt path there, a fifteen-sixteenths block, had its
  feet below the ladder and never climbed. Every other block is Aaron's.
- **Lumberjack.** The template box grows one block north of the hut to hold the sapling cell.
  The dead bush marks it; the template plants a spruce sapling there at stage 0.
- **Mine.** The template box grows one block north and east for the ground Aaron laid there.
- **Fishery and farm.** Every water cell is a source. The farm's raised channel ended in crop cells
  to the north and east at the water's own level, so the water washed them and ran off the top;
  both ends are farmland now, like the channel's other banks, and Aaron planted carrots on them,
  swapped the west beds to potatoes and tilled three path cells on the height strip, from which
  the farm is captured. The strip's grass datum under the footprint's empty corners is left out.
- **Butchery.** The pen's doors start closed, as every door and gate where livestock lives does.
- **Well.** Deepslate bricks, stairs and walls are cobblestone, cobblestone stairs and cobblestone
  walls, and it sits one layer higher than the gallery showed it (Aaron's height walk).
- **Markets.** The Birch market counters set each stall barrel into the deck with open air above
  it; the Taiga markets keep the barrels where the study has them.

## Identity and wall

Beds alternate between the village's primary and secondary colours, floor by floor; a couple bed
counts as one unit. The stoneworks' red wool wears the primary colour
([village-identity.md](village-identity.md)). The centre's four tower banners and the fort's nine
are village banners. Market fabric keeps the fixed Birch trade colours.

The wall is Aaron's study A (TA36.1 to TA36.5), the spruce palisade: the Birch geometry in
stripped spruce wood with spruce fence tips, spruce slab walks and spruce hatches, on a course of
cobblestone the catalog seats on each column's own ground ([walls.md](walls.md)). Studies B (the
fort's spruce and dark oak) and C (the Birch stone with spruce fittings) stood beside it and were
not chosen. Because the Polynesian palisade shares the stripped spruce post, the wall palette now
names its footing course explicitly instead of reading it off the post.

## Verification

The catalog is verified the way the Savanna Tent was: every template placed natively and again
after a restart, the founding village in a taiga world, the physical access checks for the
meeting point and for every other building, arrest and release through the fort's jail, and the
gate-access walks on the Taiga palisade and again on the Polynesian and Nautical walls. The
records are `tools/structure/taiga-catalog-20260914.json` and
`tools/structure/taiga-walls-20260914.json`; the gallery is
`tools/structure/taiga-full-profile-20260914.json`.
