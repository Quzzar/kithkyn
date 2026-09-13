# Polynesian Coast village

The Polynesian Coast catalog is the warm-coast island village. It has stilted huts of stripped
spruce under oak roofs, a king's hall with the village jail beside its fire, tiki torches, a pond
fishery and an open-air shrine. It is Aaron's selection from the Polynesian gallery he reviewed
and edited live above the trio-20260912 test world on 2026-09-12. The style token is
`polynesian_coast`.

It founds in Sparse Jungle and on beaches beside warm or lukewarm ocean. Sparse Jungle was taken
from Jungle, which keeps Jungle and Bamboo Jungle. Temperate and cold coasts stay reserved for
the future Nautical Coast village.

The catalog has no castle yet. None of the gallery's forts fit, so the castle slot stays open.
The centre itself is the ruler's seat and keeps the jail.

## Founding village

`village_center_polynesian_coast_1` is the Towns & Towers town centre (PC01.1), with the row
Aaron added at its north end. It has no beds. It opens six founding jobs:

- the quartermaster, who works in the separate storehouse
- the builder
- the guard captain
- the miner, who works at the separate mine
- the king, a LEADER station on the floor in front of the throne
- a jailer, a GUARD station with JAILER duty between the evidence barrels

The king's post is not on the throne itself. The throne is a north-facing stair: its seat is
lower at the front and full height only at the back, and a worker must stand fully on one flat
top. Standing in front of it leaves Aaron's throne as he built it.

Aaron chose the founding homes: the two-bed house (PC02.2), the three-bed house (PC02.6) and the
house with a single bed and a couple room (PC02.7). Together they hold eight beds for the six
starting workers. The normal site planner places them with the mine and storehouse as the
centre's companions.

The king sleeps in any bed, and nothing reserves one for him. A bed is still reserved for a worker
only through the building that employs them. Aaron decided to keep that rule rather than add a
royal bed.

The meeting point is beside the bell at the foot of the hall's steps. The village fire is the
campfire north of the jail, between the two evidence barrels set into the ground. The four tiki
torches are campfires on posts and stay decoration, not village fires. The hall carries the
village's three banners: two on its north wall, flanking the jail, and one inside above the
throne.

## The king's hall and its jail

This is the first village centre with castle amenities. The custody cell is the barred cell
against the hall's north wall. The release point is on the ground just outside it, and the two
evidence barrels sit in the ground by the fire. The code allows this in the following places:

- `CastleLayout.allowedIn` accepts castle amenities on a castle or a village centre. Definition
  validation and custody both use it, so a jail in any other building is still refused.
- `VillageRuler` finds the ruling incumbent at a castle's LEADER station or at the centre's.
- A centre that keeps the jail is not a castle. `GuardDuty.assignedCastle` returns only castles,
  so the captain still rounds the village and the jailer holds the post by the cell. A centre's
  layout also never counts against a castle in `CastleLayout.canStart`.

If a Polynesian castle is authored later, the ruling seat has to be decided with it. A settlement
has one ruling position, and a castle LEADER would be a second one.

## Selected buildings

The private datapack contains 24 definitions:

| Id | Exhibit | Use |
| --- | --- | --- |
| `village_center_polynesian_coast_1` | PC01.1 | The king's hall. It has the six founding jobs, the jail, the meeting point by the bell, the village fire and three banners |
| `house_polynesian_coast_1` | PC02.1 | One single bed and a personal chest |
| `house_polynesian_coast_1__two_single` | PC02.2 | Two single beds sharing a chest. A founding home |
| `house_polynesian_coast_1__stilted` | PC02.4 | One single bed on two-block stilts, with Aaron's barrel as its storage |
| `house_polynesian_coast_1__three_single` | PC02.6 | Three single beds sharing a chest. A founding home |
| `house_polynesian_coast_1__couple_room` | PC02.7 | A couple room and a single bed sharing one chest. A founding home |
| `couple_cottage_polynesian_coast_1` | PC02.5 | The couple bed and its chest. Its two white banners are decoration |
| `storehouse_polynesian_coast_1` | PC02.3 | Aaron's three barrels and the quartermaster worksite |
| `bakery_polynesian_coast_1` | PC02.8 | Four beds, one of them the baker's. The baker works at the smoker, with the barrel beside it; the residents share a chest |
| `stoneworks_polynesian_coast_1` | PC03.1 | The mason at the stonecutter, with a workplace chest |
| `blacksmith_polynesian_coast_1` | PC03.2 | The smithy on the ground floor and two beds above, one of them the smith's |
| `well_polynesian_coast_1` | PC03.4 | The leatherworker hut as a water-cauldron well |
| `butchery_polynesian_coast_1` | PC03.5 | A pen with three pigs and three chickens and the butcher's post inside it. The barrel sits below the pen; the butcher's bed and chest are upstairs |
| `fishery_polynesian_coast_1` | PC03.6 | The fisher on the dry grass at the pond's edge, beside the dock and the barrel. The hunter stands at the top of the hut's stairs, by the fletching table and chest |
| `farm_polynesian_coast_1` | PC03.7 | The small farm |
| `farm_polynesian_coast_2` | PC03.8 | The large farm, an in-place upgrade from the small one |
| `mine_polynesian_coast_1` | PC04.4 | The restyled CTOV sawmill that Aaron converted. The miner's post is on the north rim, and a three-wide shaft runs south from the sunken yard, passing under the south rim below its course so the rim and its gates stay whole |
| `tavern_polynesian_coast_1` | PC05.3 | The restyled Dungeons & Taverns mangrove tavern. Three guest rooms have a barrel each, the keeper's bed and chest are up the stairs, and the innkeeper works behind the counter |
| `watchtower_polynesian_coast_1` | PC06.1 | The restyled CTOV priest tower that Aaron converted. The crossbow post is on top; the guard's bed and chests are in the tower |
| `church_polynesian_coast_1` | PC08.3 | The Towns & Towers open-air shrine: jungle logs around a lit fire, candles, a brewing stand and the cleric's post, with Aaron's sunken barrel. It has no bed, so the cleric sleeps in ordinary housing |
| `lumberjack_polynesian_coast_1` | LJ.1 | Aaron's copy of the PC03.4 well, converted to a fenced jungle-sapling plot. The lumberjack's post is the sapling, and the barrel sits under the gate |
| `market_polynesian_coast_1`, `_2`, `_3` | PC10.1 to PC10.3 | The Birch market geometry in spruce on a spruce-plank deck, with the fixed trade colours. Each stall barrel stands on the counter |

The PC07.4 beach camp is reserved for Nautical Coast. The PC03.3 cartographer and library is not
used yet.

A few definitions differ from the gallery copies:

- **Fishery.** The fisher does not stand on the dock stair, because it is waterlogged and a worker
  never stands on a wet block. The hunter's post is the north arm of the hut roof: the fletching
  table in the middle splits the roof into arms, and only that arm meets the stairs.
- **Markets.** The Birch counter sets each stall barrel into the deck under an open trapdoor,
  where no worker can reach it. Each barrel rises into the trapdoor's cell over a whole deck plank,
  the same correction the Tundra markets shipped.

## Identity and wall

Beds alternate between the village's primary and secondary colours, floor by floor. A couple room
counts as one unit, so both its halves share a colour. Only the centre's three banners are
village banners; every other banner stays white decoration. Candles are plain: the tavern's
magenta candles are replaced at export, while the gallery copy keeps them. Market fabric keeps
the fixed Birch trade colours.

The wall is Aaron's study A (PC11.1 to PC11.5), a stripped spruce palisade on a footing of dead
coral. Study B, in bamboo, was rejected.

The wall keeps the Birch wall's shape:

- fence-tipped runs with torches
- corner watchtowers with an oak slab platform
- gatehouses with an oak slab roof walk, a campfire, spruce rails and two banners on each face in
  the village's colours

The coral is a single course that follows the ground under every run, tower and gate, while the
palisade above it stays spruce. Where the wall crosses water, it rises from a coral course on the
seabed.

The five pieces are bundled under `data/kithkyn/structure/wall/polynesian_coast/`, byte for byte
the study A gallery files. The loader reads the stripped spruce as wall body, and the coral as a
footing piece that the wall places at each column's ground. The details and the terrain checks are
in `tools/structure/polynesian-coast-walls-20260912.json` and [walls.md](walls.md).

## Authoring and installation

`run/polynesian-integration/capture.py` reads the selection from a flushed snapshot of the trio
world:

- Every piece is captured in its exact footprint.
- The town centre includes the row Aaron added at its north end.
- The shrine includes the course below it, which holds the barrel Aaron sank into the gallery pad.
- The lumberjack plot is his converted copy of the well.

`run/polynesian-integration/prepare.py` writes the private datapack through
`tools/structure/VillageTemplateExport.java`:

- It removes gallery markers, neutralizes beds and village banners, and plants the lumberjack's
  sapling at stage 0.
- It replaces the tavern's dyed candles, bakes the butchery's six animals into the template, and
  raises the market barrels.
- It checks every station, worksite, meeting point, custody cell and release point for dry, flat
  footing, and accounts for every container.
- It gives every definition the authored cost and grants that the live tree's building-cost
  contract requires (its `CONTRACT` table). Prices are set by analogy with the Jungle catalog:
  homes are 20 to 36 items, mixed-use buildings are priced whole, the lumberjack is the stone-only
  bootstrap and the well is 16 cobblestone.
- Where layer 0 is a ground course (paving, the pond, the pen floor, a plinth), the building
  seats it flush with the ground. Where layer 0 is stilts and fence bases, it stands them on the
  ground, as the gallery showed them.

Source and output hashes and the verification verdicts are in
`tools/structure/polynesian-coast-catalog-20260912.json`. The gallery itself is recorded in
`tools/structure/polynesian-full-profile-20260912.json`.

Manual testing can create the style with `/kithkyn create-village ~ ~ ~ polynesian_coast`.
Natural founding in a sparse jungle or on a warm-water beach uses the same selector and founding
path.

## Two engine fixes the catalog needed

The catalog surfaced two faults that affected every village, not just this one:

- **Water plants.** A block-by-block build sets liquids down last, so the fishery's lily pads and
  sugar cane broke at the first neighbour update and dropped as items. The plants that stand only
  on water or beside it now follow the liquids (`StructureInProgress.liquidsLast`).
- **Rows of gates.** A route through a row of gates, like the mine's three or a pen's double gate,
  can step from one gate cell into the next. The gate-opening goal took the first gate on the
  path, the open one the person stood in. So the closed gate beside it never opened, and the open
  one shut on them. It now opens the closed gate they are pressed against, including the next one
  in a row, and it never closes a gate while a body is inside it (`OpenFenceGateGoal`).

## Verification

The native verifications ran on one build in disposable sparse-jungle worlds:

- **Placement:** 192 real-template placements, covering all 24 templates on both build paths in
  four rotations, with colours, frames, entity and save receipts, and upgrade preservation. A
  restart then kept all 192 buildings and all 48 original animals.
- **Founding:** four rotations with codec reloads, the five housing choices, 12 upgrade fits,
  Sparse Jungle selecting the style naturally, eight beds for the six founding jobs, and distinct
  meeting and fire locations.
- **King's hall:** 24 station walks across four rotations.
- **Access:** 92 rotated structures, with 96 room walks and assigned-bed sleeps, 96
  personal-container deposits, 72 station walks, 80 shared deposits and eight walks into the mine.
- **Custody:** arrest by melee and by arrow into the hall's cell, private minute notices, saved
  expiry, evidence capacity, escape, damaged-cell release and the full-cell fallback.
- **Route regressions:** these ran after the gate-goal change. All eight guards and workers
  climbed to and from every wall post, in a Birch wall and in the Polynesian wall. The Birch
  mine-entry walk left and re-entered the shaft in all four rotations.

The static template audit passed for all 24 templates. All 580 unit tests ran and none failed;
8 are skipped.
