# Savanna Tent village

The Savanna Tent catalog is the dry-grassland camp. A white canvas tent over an acacia frame is
the town centre, blue and yellow tipis and camp tents stand around its fire, a trader's tent keeps
the stores, an acacia-framed mine and stoneworks work the ground, and a palisade of acacia posts on
stone footings rings the camp. It is Aaron's selection from the Savanna gallery he reviewed and
edited live above the trio-20260912 test world on 2026-09-14. The style token is `savanna_tent`.

It founds on Savanna, Savanna Plateau and Windswept Savanna, which carry the
`kithkyn:village_style/savanna_tent` tag, and on modded biomes tagged as savanna or named for it
([buildings.md](buildings.md), "Current runtime selection"). Before this catalog those three biomes
carried the Pueblo tag and founded that catalog as a stand-in; Pueblo keeps the mesa families.

## Founding village

`village_center_savanna_tent_1` is the original Village Life savanna tent (ST28.1), recovered from
git for the gallery at Aaron's request. It declares the five builder duty anchors and the captain's
post by the fire at its mouth, and its four beds sleep the founding workers:

- the builder
- the guard captain
- the miner, who works at the mine
- the quartermaster, who works at the storehouse

The four beds share the tent's one barrel. A new town starts with the tent, the mine and the
storehouse, which the site planner places as the centre's companions
([population-and-labor.md](population-and-labor.md)). The meeting point is beside the campfire at
the tent's mouth, the village fire is that campfire, and the bell hangs on the ridge.

The catalog has no castle and no watchtower: Aaron found no tent that reads as a tower, so the
captain's post is the village's only guard vacancy until a castle is added.

## Selected buildings

The private datapack contains 18 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_savanna_tent_1` | ST28.1 | The original savanna tent: five builder anchors, the captain by the fire, four beds and their barrel, the meeting point by the campfire, the bell on the ridge, and the two village banners Aaron hung on its gables |
| `house_savanna_tent_1` | ST04.5 | The Towns & Towers large blue tipi on the ground: two beds sharing a chest. Its blue canvas and banner take the village's secondary colour; its timber is acacia |
| `couple_cottage_savanna_tent_1` | ST05.2 | The small blue tipi: the couple bed, with its chest. Its blue canvas and banner take the primary colour, its white canvas and its bed the secondary; its timber is acacia |
| `storehouse_savanna_tent_1` | ST06.5 | The trader's blue tent in acacia: Aaron's four barrels are the town's storage and the quartermaster stands among them. Its banner is the village banner; its blue canvas is primary, its yellow canvas and carpets secondary |
| `mine_savanna_tent_1` | ST29.1 | The original savanna mine hut: the miner inside, the shaft's mouth two cells south of him under the south steps, and the chest by the north steps as the store |
| `blacksmith_savanna_tent_1` | ST05.9 | The badlands camp workshop in acacia and white canvas, its banners taken down: the smith between the smithing table and the grindstone, the chest as the store. It has no bed |
| `hunting_lodge_savanna_tent_1` | ST05.10 | The camp tent in acacia, its banners taken down: the hunter by the fletching table, his bed with its chest, and the barrel as the store. Its dark gray canvas and carpet take the secondary colour, its light gray ones are white |
| `butchery_savanna_tent_1` | ST06.2 | The trader camp's llama pen in acacia with three cows in it; the butcher inside by the barrel |
| `bakery_savanna_tent_1` | ST07.6 | The CTOV Native Savanna cartographer's tower with a smoker where its table stood and a chest. Its brown terracotta is primary, its white canvas stays white, its timber is acacia |
| `fishery_savanna_tent_1` | ST09.4 | The CTOV priest tower over its pool, in acacia: the fisher on the rim, the barrel by the water as the store, and the fisher's bed with its own barrel. Its brown terracotta is primary and its brown canvas secondary |
| `farm_savanna_tent_1` | ST34.2 | Vanilla's savanna large farm, with Aaron's barrel, sunk one layer so its farmland is flush with the ground |
| `well_savanna_tent_1` | ST25.4 | The Dungeons & Taverns savanna well, sunk seven layers so its rim stands on the ground |
| `stoneworks_savanna_tent_1` | ST29.8 | The original savanna stoneworks: the mason at the stonecutter, the barrel as the store, entered by its open west side |
| `church_savanna_tent_1` | ST32.1 | Vanilla's savanna temple: the cleric on the floor before the altar step, Aaron's barrel as the store, its two wall banners village banners. It has no bed |
| `lumberjack_savanna_tent_1` | ST20.7 | The CTOV balloon stand: the lumberjack's post is the acacia sapling where Aaron's dead bush stood, the barrel is the store |
| `market_savanna_tent_1`, `_2`, `_3` | ST40.1 to ST40.3 | The shared market geometry in acacia on a coarse-dirt camp floor, with the fixed trade colours |

The other exhibits are not used. Aaron dropped the original farm (ST29.3) for the vanilla one, the
CTOV gunsmith (ST13.5) for the original stoneworks, and the original markets for the studies.

A few definitions differ from the gallery copies:

- **Fishery.** The fence over the pool barrel is gone, so a worker can reach the barrel from the
  rim. Every water cell of the pool is a source.
- **Farm.** Every water cell is a source.
- **Lumberjack.** Aaron's dead bush marked where the sapling belongs. The template plants an acacia
  sapling there at stage 0.
- **Butchery.** The pen's gate starts closed, as every door and gate where livestock lives does.
- **Mine.** The miner stands inside the hut two cells north of the shaft's mouth: the ramp's entry
  column, one behind the mouth, is dug one below its walk cell, so it is the hut's own floor.
- **Fishery frame.** The tower's item frame is exported fixed, as the Nautical couple cottage's was,
  so the native placement check never rejects it as unsupported.
- **Materials, after the height walk.** Every spruce, oak and jungle piece outside the markets is
  acacia: the camp workshop and tent, the tipis, the llama pen, the trader tent, the bakery's
  hatch and the fishery's fences. Light gray canvas and carpet are white. The blacksmith's and
  the hunting lodge's banners are gone, as Aaron took them down; the two he hung on the centre's
  gables and the temple's two are village banners.
- **Seating, after the height walk.** The large tipi stands with its floor flush with the ground
  (sink 0) and the farm one layer down (sink 1), so its farmland is flush and its edge water
  meets the ground beside it instead of spilling over the grass. The small tipi, the camp
  workshop and tent, the llama pen and the trader tent keep the gallery's look and stand on the
  ground (sink -1). The original tent's paving, the mine ring, the stoneworks floor, the tower
  bases and the balloon stand's base are ground courses and sit flush (sink 0). The well sits
  seven layers down.
- **Stoneworks and temple, after the access checks.** The stoneworks' south face is a porch post
  between two steps: a walker climbs the steps onto the wall top and cannot come down into the
  door, so the hut's open west side is its entrance. The temple's altar step is a full block up,
  which a walker cannot hop, so the cleric's post is on the floor before it rather than beside
  the brewing stand.
- **Markets.** The Birch market counters set each stall barrel into the deck with open air above it;
  the Savanna markets keep the barrels where the study has them.

## Identity and wall

Beds alternate between the village's primary and secondary colours, floor by floor; a couple bed
counts as one unit, and the couple cottage's unit is secondary at Aaron's word. Canvas, carpet and
terracotta wear the village's colours ([village-identity.md](village-identity.md)): primary on the
storehouse's blue canvas, the couple cottage's blue canvas and banner, and the brown terracotta of
the bakery and the fishery; secondary on the house's blue canvas and banner, the couple cottage's
white canvas, the storehouse's yellow canvas and carpets, the fishery's brown canvas, and the
hunting lodge's dark gray canvas and carpet. White canvas elsewhere stays white. The centre's two
gable banners, the storehouse's banner and the temple's two are village banners. Market fabric
keeps the fixed Birch trade colours.

The wall is Aaron's study A (ST41.1 to ST41.5), the acacia palisade: the Birch geometry in stripped
acacia wood with acacia fence tips, acacia slab walks and acacia hatches, on a course of
cobblestone the catalog seats on each column's own ground ([walls.md](walls.md)). Studies B (blue
and yellow canvas on acacia) and C (dry stone and terracotta) stood beside it and were not chosen.

## Verification

The catalog is verified the way the Nautical Coast was: every template placed natively and again
after a restart, the founding village in a savanna world, the physical access checks for the tent
and for every other building, and the gate-access walks on the Savanna palisade and again on the
Nautical seawall. The static template audit passes all 18 templates. The records are
`tools/structure/savanna-tent-catalog-20260914.json` and
`tools/structure/savanna-tent-walls-20260914.json`; the gallery is
`tools/structure/savanna-full-profile-20260914.json`.
