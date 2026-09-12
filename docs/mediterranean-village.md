# Mediterranean village

The Mediterranean catalog is the temperate Plains village: a white quartz-and-plaster town with
terracotta tile roofs, gathered around its church. It is Aaron's selection from the Towns & Towers
Mediterranean family (annex rows M06 to M08 of the live showcase world) plus two edited pieces
from the Mediterranean court, all arranged live on 2026-09-11 and 2026-09-12, with the
three-tier markets restyled from the Desert set, the hedged quartz wall from the workshop row, and
the M08.1 fort as its castle. Its style token is `mediterranean`. Vanilla Plains and Sunflower
Plains, the conventional Plains tag, and modded biome paths ending in `plains` select it whenever
its complete private founding set is loaded; a snowy or frozen plain is not Mediterranean country
and stays with the temperate Birch fallback.

## Selected buildings

The private datapack contains 24 definitions:

| Id | Use |
| --- | --- |
| `village_center_mediterranean_1` | M06.1 church; no beds; quartermaster, builder and guard captain vacancies plus a cleric at the altar's brewing stand; the meeting point and one campfire on the forecourt |
| `mine_mediterranean_1` | M07.8 library turned mine: it employs the miner at the station barrel under the bookshelf, digs a three-wide shaft south from the sunken room, and houses the miner in the bed with its barrel upstairs |
| `storehouse_mediterranean_1` | M07.5 fisher's house converted to four village containers with the quartermaster's physical worksite |
| `house_mediterranean_1` | M06.5 one-bed home with a barrel set into the floor |
| `house_mediterranean_1__two_bed` | M06.3 two single beds on two floors, a barrel and a chest |
| `house_mediterranean_1__two_room` | M06.2 two single rooms, each with its floor barrel |
| `house_mediterranean_1__couple_room` | M06.4 a single bed downstairs and a couple room upstairs |
| `house_mediterranean_1__small_4`, `__small_5` | M06.10 and M06.11 one-bed cottages |
| `couple_cottage_mediterranean_1` | M06.6 couple home; both bed halves share one village colour |
| `blacksmith_mediterranean_1` | M07.1 armourer's yard: blast furnace, grindstone, the smith's bed and floor barrel |
| `watchtower_mediterranean_1` | M07.3 tower with the guard's bed and post in the upper room; its ladder ends one rung below the bedroom floor so a climber steps off instead of hanging in the corner |
| `stoneworks_mediterranean_1` | M07.4 mason and farmer under one roof: stonecutter, composter, five berry bushes, two beds, two personal containers and two shared |
| `hunting_lodge_mediterranean_1` | M07.6 hunter and smith: fletching and smithing tables downstairs, two beds sharing one chest upstairs |
| `bakery_mediterranean_1` | M07.7 bakery with the baker's room and barrel, three village chests and a quartermaster's worksite: the second storehouse the village can build |
| `farm_mediterranean_1` | M05.3 CTOV small farm as Aaron edited it, with its field barrel |
| `farm_mediterranean_2` | M08.2 large sweet-berry field, the small farm's upgrade |
| `butchery_mediterranean_1` | M04.2 farm and pasture: a farmer picking glow berries from the trellised cave vines, and a butcher who works the pen whole (breeding, shearing and slaughter above six of a kind) with three cows and three sheep, one barrel each |
| `lumberjack_mediterranean_1` | M08.3 timber yard: one barrel is the lumberjack's station, the two hook-trimmed barrels are decoration, the other four are village storage with a quartermaster's worksite |
| `well_mediterranean_1` | M08.6 planter rebuilt by Aaron as a small well |
| `market_mediterranean_1`, `_2`, `_3` | The shared market geometry in quartz bricks and pillars, spruce rails and plain candles, keeping the stripe-matched awning stairs and the authored fabric colours |
| `castle_mediterranean_1` | M08.1 fort: ruler's double bed, baker and blacksmith with a shared chest, jail cell with two evidence chests upstairs, a jailer post, and four sword guards patrolling the gates and the yard; it has no stall, so the castle merchant systems do not apply |

The two farms and the stoneworks all grant grain: the Mediterranean village is mainly a berry
village, and its farmers pick sweet berries and glow berries alike (docs/worker-loops.md).

## Founding and village identity

The centre has no beds. Its founding companions are the mine, the storehouse, three one-bed
homes and the two-bed home, placed through the normal planner: five beds in the homes for the
four centre workers (quartermaster, builder, captain and cleric), and the miner's own bed
upstairs in the mine, which employs them. The quartermaster routes to the physical worksite in
the storehouse.

The church forecourt is the civic meeting point and carries the one authored campfire. Every
assigned bed is neutral in the template and becomes the village primary or secondary colour;
couple halves share the primary. Every orange wool awning cell is a primary-colour slot (the
family's orange is the village's colour, not a fixed orange) while white wool stays white.
Authored banner slots on the church and the fort become the village banner. Market carpets,
wool, candles and flags keep their authored red, orange, green, yellow and white palette.

## Wall and castle

The wall is the bundled `mediterranean` segment family (docs/walls.md): the wood geometry with
Aaron's workshop edits in quartz bricks, coping stairs along the parapets, wall torches on both
faces, spruce rails and trapdoors, and a persistent hedge of jungle and dark oak leaves at both
feet, authored inside the towers and gates and grown procedurally along every run. There is one
wall stage, paid for with cobblestone.

The castle uses the same ruler and custody systems as the Desert and Swamp castles with its own
M08.1 layout: four beds including the ruler's couple room, a baker's corner and a smithy sharing
one chest, the jail cell behind iron bars on the tower's upper floor with its two evidence
chests (the custody system takes any container the layout names, barrel or chest), a jailer
post beside it, and four sword-guard posts whose routes cover the north gate, the south gate
and both halves of the yard. Its nine authored pillager banners are village banner slots.
Unlike the Desert and Swamp castles it has no merchant stall.

## Authoring and installation

`run/mediterranean-integration/prepare.py` crops the 21 paused-save captures back to their
footprints with `tools/structure/EditTemplateBlocks.java` (dropping the row signs, the gallery's
barrier rings and two empty glow item frames), then exports them with
`tools/structure/VillageTemplateExport.java`: neutral beds, banners and awnings, the mine's
sunken pit carved as ground, the forecourt campfire, the six captured livestock, and the three
markets derived from the Desert set. Every station, worksite, meeting point, custody cell and
patrol point is checked to be a standable cell of its capture before export. Two runtime rules
were corrected for this catalog rather than editing the buildings around them: a worker's
footing now stands on a carpet instead of colliding with it, and a person can be sent to a cell
holding a sign or banner (vanilla's ground navigation counts those as solid although they have no
collision, and aimed one block above them, where nobody can stand). Containers no standing cell
can see, the fort's buried north-west chest and the barrels under other barrels in its nook and
the timber yard, are left as decoration rather than declared storage. The catalog hashes
and source record are in `tools/structure/mediterranean-catalog-20260912.json`. Third-party
building templates stay in the private local datapack; only the wall family is bundled.

Manual testing can create the style with `/kithkyn create-village ~ ~ ~ mediterranean`. Natural
founding on a temperate plain uses the same selector and founding path.

## Verification

The September 12 native runs, repeated on the final build after the follow-up fixes, loaded all
24 strict templates and passed natural Plains selection, all four founding rotations, twelve
supported in-place upgrades (the three market steps and the small farm to the large field) and
codec reloads, with six founding beds and five founding positions. Placement covered 192 instant
and incremental/reload builds; a real server restart retained all 192 saved buildings and 64
authored entities. Full-size adult villagers passed 396 access routes across 92 rotated
non-centre layouts, including 96 room walks and assigned-bed sleeps, 96 personal-container
deposits, 120 shared deposits, 100 station walks and both mine directions, with the carpets and
name-plate signs in place. The centre added twenty station walks.

The castle runs covered a paid incremental build with protected evidence in the two jail chests,
sixteen sentry patrols over every authored route level in four rotations, and the full custody
matrix: melee and arrow arrest, private minute notices, saved expiry, evidence capacity and
identity, escape, damaged-cell release, full-cell fallback and village-only grace. The castle
merchant check does not apply: the fort has no stall. The wall family's own checks are in
[walls.md](walls.md) and `tools/structure/mediterranean-walls-20260912.json`.
