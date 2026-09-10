# Desert village

The September 9 selection uses the edited Desert Oasis buildings from the dry-biome
gallery. Sandy desert biomes select this catalog. Mesa, badlands, and the savanna
family select [Pueblo](badlands-village.md); Birch biomes retain the Birch catalog.
Desert has a strict catalog and does not borrow missing buildings from plains.

The private catalog contains 30 selected building definitions:

| Selection | Buildings |
| --- | --- |
| Founding center | D05.1, four beds, quartermaster, builder, and guard captain |
| Homes | Nine alternatives with fifteen beds, plus the two-bed D05.2 couple home; D05.5 also contains a couple room |
| Storage | D01.7 Storehouse I, upgrading to the D03.8 vault with a quartermaster room |
| Food and materials | Farm, butchery, bakery, fishery, hunting lodge, lumberjack, stoneworks, blacksmith, and mine |
| Services | T02.9 tavern (one innkeeper bed and four general beds), R01.8 temple, two watchtower tiers, and retained D04.12 well |
| Trade | Three market tiers with one, two, and three merchant stations |

The mine supplies the fourth founding job and fifth founding bed. Storehouse I
does not duplicate the center's quartermaster job. The bell plaza is the meeting
point; the campfire remains a separate cooking and gathering location.

All declared beds use a primary or secondary village color. Couple beds share a
color, and personal containers are assigned to their rooms separately from communal
storage. The center's six banner placeholders receive the village's actual banner.

Desert walls use smooth sandstone blocks, stairs and slabs, with matching sandstone
wall railings, chiseled sandstone gate frames, lit clusters of two to four regular
candles, four hanging lanterns per gate, oak trapdoors and normal ladders. Every village has one wall stage; there is no wall upgrade. The editable
Desert and Mesa wall workshop is at **3680, 230, 873**, north of the dry-biome gallery.
See [walls.md](walls.md) for construction and guard access.

Selections, immutable capture hashes, export corrections, and physical access checks
are recorded in `tools/structure/desert-*-20260909.json` and
`tools/structure/desert-tavern-adoption-20260910.json`. The reviewed private assets
are assembled in `run/desert-integration/datapack` and installed as `kithkyn-desert`
after `mod_data`. Its filter removes superseded Desert definitions from lower packs;
no old-building aliases or save migrations are provided.

The September 9 native regional fixture checked the original 28 templates and nine housing alternatives,
twelve rotated storehouse/market upgrade fits, four founding rotations and save
reloads, natural biome selection, bed colors, and banner patterns. Separate physical
tests walk workers to their beds, private and communal containers, service stations,
mine stairs, and elevated guard posts. Market counter entrances remain two blocks
high and wide enough for adult workers to move between stalls.

The September 10 placement repair raises the mine, compact storehouse, and both medium-house
alternatives one block (`sink: -1`) so their lowest steps, fence bases, and trim remain exposed.
The mine's worker stands at local `[4,0,3]`; the shaft mouth is `[4,1,5]`, facing south down the
three-block covered lane between the fence and bedroom wall. The market entrance retains one
orange carpet. These corrections are recorded in `tools/structure/desert-placement-repair-20260910.json`.

The September 10 comparison decision keeps D04.12 as the well, adopts the Dungeons &
Taverns T02.9 building as the official tavern, and retains the former D05.2 tavern as
`couple_cottage_desert_1`. Its two adjacent beds form one general couple room, both in
the primary village color. Both spouses share the upstairs personal storage; the
existing three lower communal containers remain communal. It carries no innkeeper
station. T02.9 reserves its ground-floor bed and nearby chest for the innkeeper, with
four general beds on the upper floors. Its remaining chest is communal.

Both changed buildings passed sixteen instant/incremental placements and all four
rotations. Adult residents passed sixty access routes, twenty-eight assigned-bed sleeps,
twelve personal deposits, four innkeeper station walks, and sixteen communal deposits.
The two definitions and their templates are installed in the live private datapack;
a reload confirmed 199 total building definitions. The comparison court at
`4240.5, 230, 980.5` now labels the selected couple home, tavern, and retained well.


The subsequent temple and tower decision selects the edited **R01.8 Temple Plaza 16**
as `church_desert_1`. Its brewing stand remains on the central pedestal, with a
reachable cleric standing position beside it and the authored chest as communal storage.
The building has no bed. The previous D07.7 temple remains a gallery reference.

**V03.7** is `watchtower_desert_1`; **D03.3** is `watchtower_desert_2`, with an explicit
upgrade from the smaller tower. Each has one resident crossbow guard. Both towers use
ladders. The smaller tower's spiral was removed, with two-cell ladder openings at its
landings so an adult guard can climb without catching its head on the floor edge.
Its upper chest is personal storage for its guard. The larger tower retains its
existing personal chest beside the bed and communal chest near the lookout.

The three templates passed 24 instant/incremental placements and twelve rotated
physical-access checks: 36 routes, twelve workstation walks, eight assigned-bed
sleeps, eight personal deposits and eight communal deposits. The upgrade fits in all
four rotations. The selection and immutable capture hashes are in
`tools/structure/desert-temple-tower-adoption-20260910.json`.

Construction prices default to shared recipes by building category and level across styles.
The current regional definitions inherit those defaults; deliberate per-building `cost` overrides
are supported when needed. The temple, bakery and tavern use basic
masonry instead of finished stone bricks; quantities remain unchanged. A small tower
costs 24 timber and 40 stone; upgrading it costs 20 additional stone. Building the
larger tower fresh costs 24 timber and 60 stone. See [building-spec.md](building-spec.md).

The edited R07.2 fort is available as one optional `castle_desert_1` per village. Its
eight beds include a royal couple suite, accommodation for the existing center captain,
and five general beds. It adds the ruler, rooftop blacksmith, a merchant at the planted
wooden stall, two sword posts, two crossbow posts and a jailer. The merchant uses the
village's existing staffed market and treasury. Two stacked evidence barrels beside the
rooftop cell are excluded from village storage and theft rules. See [castles.md](castles.md) for custody, inventory handling
and ruler decisions. All four rotations passed physical access and role-allocation
checks; the live private catalog now loads 201 total building definitions.
