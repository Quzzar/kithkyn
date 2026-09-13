# Nautical Coast village

The Nautical Coast catalog is the temperate fishing town. A lighthouse stands over the town fire,
thatched cottages of sandstone and jungle timber sit on the sand, a fishing hut stands on its own
water at the end of a jetty, and a turnover ship drawn up on the beach is the ruler's castle. It is
Aaron's selection from the Nautical gallery he reviewed and edited live above the trio-20260912
test world on 2026-09-13. The style token is `nautical_coast`.

It founds on beaches beside temperate or cold water and on stony shores. A beach beside warm or
lukewarm ocean stays the Polynesian Coast, and a snowy beach stays Tundra
([buildings.md](buildings.md), "Current runtime selection").

## Founding village

`village_center_nautical_coast_1` is the Towns & Towers beach lighthouse (NC05.1). It opens the
four founding jobs, and its four beds sleep the workers who take them:

- the quartermaster, who works the lighthouse's ten outside barrels, the town's storage
- the builder
- the guard captain
- the miner, who works at the separate mine

Each of the four beds has its own barrel or chest. A new town starts with the lighthouse and the
mine, which the normal site planner places as the centre's companion. The storehouse (NC05.3) is
not part of the founding set: it is extra storage a town builds when it needs more room.

The meeting point is beside the bell at the lighthouse's foot. The village fire is the campfire
among the outside barrels, and the lighthouse's four banners are village banners.

## The castle

`castle_nautical_coast_1` is the Towns & Towers beach camp's turnover ship (NC05.5), the piece the
Polynesian gallery showed as PC07.4. It is the settlement's ruling seat ([castles.md](castles.md)):

- The king's LEADER post is on the carpet of his quarters. The double bed there is reserved for
  him, and the deck chest is his.
- A quartermaster works the seven barrels beside the water cauldrons, the castle's storage.
- The baker works the campfire and smoker, and the blacksmith the smithing table and grindstone.
- The jail is the fenced pen by the hull, with the jailer's post beside it and two evidence
  barrels set into the ground.
- Four crew beds sleep the jailer, the baker, the blacksmith and the quartermaster.
- Its seven banners are village banners.

## Selected buildings

The private datapack contains 24 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_nautical_coast_1` | NC05.1 | The lighthouse: four founding jobs and the four beds for them, the town's ten barrels, the meeting point by the bell, the village fire and four banners |
| `castle_nautical_coast_1` | NC05.5 | The turnover ship: the king, a quartermaster, a baker, a blacksmith and a jailer, the jail, four crew beds and seven banners |
| `couple_cottage_nautical_coast_1` | NC05.2 | The Towns & Towers main house on its gravel base: the couple bed, with the ceiling chest and eleven ground-floor barrels as the couple's storage |
| `storehouse_nautical_coast_1` | NC05.3 | The outdoor shack's six barrels, extra town storage |
| `church_nautical_coast_1` | NC05.4 | The giant cross, with Aaron's barrel and a brewing stand at the cleric's post. It has no bed, so the cleric sleeps in ordinary housing |
| `fishery_nautical_coast_1` | NC06.8 | The CTOV beach fishing hut on a two-block ring of water out to the end of its dock. The fisher stands at the dock's end, the barrel in the hut floor is the fishery's store, and the fisher's bed has a barrel of its own |
| `blacksmith_nautical_coast_1` | NC09.2 | The smith at the anvil and smithing table, with the workplace chest. The smith's bed has a barrel |
| `stoneworks_nautical_coast_1` | NC09.3 | The mason at the stonecutter, with the workplace barrel. The mason's bed has a chest |
| `butchery_nautical_coast_1` | NC09.4 | The butcher's hut with three pigs and three chickens in the pen behind it. It has no bed |
| `watchtower_nautical_coast_1` | NC09.8 | The CTOV archery range as hunter and watchtower in one: the crossbow post on the top deck and the hunter by the barrel in the yard. The hunter's bed is downstairs and the guard's upstairs, each with its own storage |
| `farm_nautical_coast_1` | NC09.9 | The beach farm and its barrel |
| `lumberjack_nautical_coast_1` | NC09.10 | The CTOV orchard. The lumberjack's post is the oak sapling where Aaron's dead bush stood, and the double chest is the workplace store |
| `mine_nautical_coast_1` | NC09.11 | The CTOV pen Aaron made into the mine: a log-curbed yard inside a fence, the miner's post at its north end, and a three-wide shaft that runs south and passes under the curb below its course |
| `house_nautical_coast_1` | NC10.6 | One bed and a chest |
| `house_nautical_coast_1__two_single` | NC10.7 | Two beds sharing a chest |
| `house_nautical_coast_1__two_loft` | NC10.8 | Two beds upstairs sharing the chest below |
| `house_nautical_coast_1__three_single` | NC10.4 | Three beds sharing a chest |
| `house_nautical_coast_1__four_single` | NC10.3 | Four beds, two to each chest |
| `tavern_nautical_coast_1` | NC11.2 | Two couple rooms upstairs: the keeper's household has one and the other is ordinary housing. The innkeeper works the ground floor, and the keg is a barrel, the tavern's store |
| `bakery_nautical_coast_1` | NC12.1 | The baker at the smoker, with the barrel beside it |
| `well_nautical_coast_1` | NC16.8 | The CTOV goddess statue, whose water cauldron is the well |
| `market_nautical_coast_1`, `_2`, `_3` | NC17.1 to NC17.3 | The shared market geometry in jungle wood on a jungle-plank deck, with the fixed trade colours |

The other exhibits are not used. The NC09.6 watchtower was dropped when the archery range became
the watchtower, and the NC09.12 exhibit is kept back for a later pass.

A few definitions differ from the gallery copies:

- **Fishery.** The gallery ringed the hut with barriers and its water spilled into them. The
  template keeps the pond as the two-block ring of source water Aaron asked for, out to the end of
  the dock.
- **Lumberjack.** Aaron's dead bush marked where the sapling belongs. The template plants an oak
  sapling there at stage 0, on the orchard's grass. The orchard's spruce fence had no gate, so one
  goes in beside the lodge, where the lumberjack comes out.
- **Tavern.** The gallery showed CTOV's keg, a block from a mod the server lacks, as a chiseled
  stone brick marker. The template has a barrel there, the tavern's store. The hatch over the
  first stair step is gone: closed, it met a climbing villager's head, and open, its leaf met the
  shoulder of the widest villager whichever edge it hung from. A stair needs its column clear two
  blocks and a body's width above every step.
- **Lighthouse.** The three green carpets in the corridor under the spiral stair are gone. A carpet
  lifts a villager a sixteenth of a block, into the stair's lowest steps overhead, on the way to
  the ground-floor bed's barrel.
- **Couple cottage.** Aaron's floor frame of bread is a fixed frame. The native placement check
  intermittently rejected it as unsupported although its stone floor is there, and a fixed frame
  always stands. Its bread cannot be taken.
- **Butchery and shack.** Every door and gate where livestock lives starts closed, and the
  storehouse shack's entrance is its open south side.
- **Markets.** Since the village audit, the Birch market counters on main set each stall barrel into
  the deck with open air above it, where a worker reaches it. The Nautical markets keep the barrels
  where the study has them, with the Birch container positions.

## Identity and wall

Beds alternate between the village's primary and secondary colours, floor by floor; a couple bed
counts as one unit. The lighthouse's four banners and the castle's seven are village banners, and
every other banner stays white decoration. Market fabric keeps the fixed Birch trade colours.

The wall is Aaron's study C (NC20.1 to NC20.5), his variant of the gallery's sandstone seawall: the
Birch geometry in sandstone, with smooth sandstone where Birch has mossy cobblestone, sandstone
wall tips and jungle slab walks, on a course of stripped jungle wood. Study A, a stripped jungle
palisade, and study B, the same seawall on terracotta, were not chosen. The wall keeps the Birch
wall's shape:

- wall-tipped runs with torches
- corner watchtowers with a jungle slab platform
- gatehouses with a jungle slab roof walk, a campfire and two banners on each face in the
  village's colours

The stripped jungle wood is a single course that follows the ground under every run, tower and
gate, while the masonry above it stays sandstone. The five pieces are bundled under
`data/kithkyn/structure/wall/nautical_coast/`, byte for byte the study C gallery files. The details
are in `tools/structure/nautical-coast-walls-20260913.json` and [walls.md](walls.md).

## Authoring and installation

`run/nautical-integration/capture.py` reads the selection from a flushed snapshot of the trio world.
Every piece is captured in its exact footprint, except the NC05.4 cross, which grows one block west
for the barrel Aaron set beside it.

`run/nautical-integration/prepare.py` writes the private datapack through
`tools/structure/VillageTemplateExport.java`:

- It removes gallery markers, neutralizes beds and village banners, and plants the lumberjack's
  sapling at stage 0.
- It makes the fishery's water sources, replaces the tavern's keg marker with a barrel and bakes
  the butchery's six animals into the template.
- It checks every station, worksite, meeting point, custody cell, release point and animal for dry,
  flat footing, and accounts for every container.
- It gives every definition the authored cost and grants that the live tree's building-cost
  contract requires, priced by analogy with the Polynesian and Jungle catalogs.
- The five CTOV homes stand their plank floor on the ground, with the front step reaching down to
  it (sink -1). Every other building seats its ground course (sand, gravel, paving or the pond)
  flush with the ground (sink 0).

`run/nautical-integration/height-gallery.py` places the catalog on the height gallery's Nautical
strip (x 15997 to 16097, z 520 to 623, grass at y 220), which joins the Alpine strip, so Aaron could
walk the seating of every building before the catalog was locked.

Source and output hashes and the verification verdicts are in
`tools/structure/nautical-coast-catalog-20260913.json`. The gallery itself is recorded in
`tools/structure/nautical-full-profile-20260913.json`.

Manual testing can create the style with `/kithkyn create-village ~ ~ ~ nautical_coast`. Natural
founding on a temperate beach or a stony shore uses the same selector and founding path.

## Engine changes

- **Beach routing.** After the explicit style tags and the warm-coast rule, any other open beach
  (`c:is_beach`, not snowy) and any stony shore (`c:is_stony_shores`) is the Nautical Coast, ahead
  of the conventional families that would read a beach as Desert (`VillageStyle.select`).
- **Wall footing.** The Polynesian `CORAL_FOOTING` piece became `FOOTING`, whose block comes from the
  palette's `footing()`: dead coral for the Polynesian wall and stripped jungle wood for the
  Nautical one. A new `BODY_ACCENT` piece carries the Nautical smooth sandstone as wall body through
  the palette's `accent()`. Both are appended pieces or keep their ordinal, so saved wall section
  signatures are unchanged.
- **Founding set.** A centre that keeps the town's storage in its own shared containers may found
  with its mine and no storehouse (`Buildings.foundingCompanions`). Every other catalog still
  founds with one of each, and the Nautical storehouse stays in the catalog for when the town needs
  more room.
- **Routed posts' beds.** A centre post routed to a separate worksite sleeps at that worksite only
  when the worksite has beds of its own, as the Romanian mine does. A bedless worksite leaves the
  bed at the post's own building, so the lighthouse's miner keeps a lighthouse bed
  (`Village.housingWorkplace`). The Tundra centre follows the same pattern and had been left
  without a miner's bed by the earlier rule.
- **A worker's couple room.** A building with a reserved couple room and a general one, like this
  tavern, housed the keeper's household in whichever pair came first in its definition. A couple
  entitled to a building's reserved pair now takes it before a general pair there
  (`Village.houseCouple`), so the guest room stays open for guests.
- **Placement order.** Vanilla's template loader lists full blocks first, then partial shapes,
  then blocks with block entities, so a sea pickle goes down before the barrel under it, and the
  flower pot set beside it next knocked the pickle off with a neighbour update. The lighthouse's
  pickle on its west barrels was the first to go. Both builders now lay every block with
  `UPDATE_KNOWN_SHAPE` and let their finishing pass settle every shape once the whole template
  stands, so no block is judged against its neighbours before its support exists. The export
  tool also writes templates floor by floor, as vanilla's own writer does.
- **Verification harness.** The reviewed-village harness knows the Nautical catalog: a founding
  set of the lighthouse and the mine, a quartermaster who works the lighthouse's barrels rather
  than a storehouse, and the town's storage in the lighthouse.

## Verification

The native verifications ran on one build in disposable beach worlds:

- **Placement:** 192 real-template placements, covering all 24 templates on both build paths in
  four rotations, with colours, frames, entity and save receipts, and upgrade preservation. A
  restart then kept all 192 buildings and their original animals.
- **Founding:** four rotations with codec reloads, the five housing choices, eight upgrade fits,
  Beach selecting the style naturally, four beds for the four founding jobs, and distinct meeting
  and fire locations.
- **Lighthouse:** 32 access routes across four rotations, with 16 room walks and assigned-bed
  sleeps, 16 personal-container deposits, 16 station walks and 40 shared deposits.
- **Access:** 92 rotated structures, with 204 access routes, 116 room walks and assigned-bed
  sleeps, 116 personal-container deposits, 88 station walks, 132 shared deposits and eight walks
  into the mine.
- **Custody:** arrest by melee and by arrow into the castle's cell, private minute notices, saved
  expiry, evidence capacity, escape, damaged-cell release and the full-cell fallback.
- **Walls:** all eight guards and workers climbed to and from every post, in the Nautical wall
  and in the Polynesian wall.

The static template audit passed for all 24 templates. The unit suite ran with no failures.
