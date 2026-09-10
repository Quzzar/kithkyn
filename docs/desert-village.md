# Desert village

The September 9 selection uses the edited Desert Oasis buildings from the dry-biome
gallery. Sandy desert biomes select this catalog. Mesa, badlands, and the savanna
family select [Pueblo](badlands-village.md); Birch biomes retain the Birch catalog.
Desert has a strict catalog and does not borrow missing buildings from plains.

The private catalog contains 28 selected building definitions:

| Selection | Buildings |
| --- | --- |
| Founding center | D05.1, four beds, quartermaster, builder, and guard captain |
| Homes | Nine alternatives with fifteen beds; D05.5 contains the two-bed couple room |
| Storage | D01.7 Storehouse I, upgrading to the D03.8 vault with a quartermaster room |
| Food and materials | Farm, butchery, bakery, fishery, hunting lodge, lumberjack, stoneworks, blacksmith, and mine |
| Services | Tavern, church, watchtower, and well |
| Trade | Three market tiers with one, two, and three merchant stations |

The mine supplies the fourth founding job and fifth founding bed. Storehouse I
does not duplicate the center's quartermaster job. The bell plaza is the meeting
point; the campfire remains a separate cooking and gathering location.

All declared beds use a primary or secondary village color. Couple beds share a
color, and personal containers are assigned to their rooms separately from communal
storage. The center's six banner placeholders receive the village's actual banner.

Desert walls use sandstone throughout their masonry, with oak trapdoors and normal
ladders. Every village has one wall stage; there is no wall upgrade. The editable
Desert and Mesa wall workshop is at **3680, 230, 873**, north of the dry-biome gallery.
See [walls.md](walls.md) for construction and guard access.

Selections, immutable capture hashes, export corrections, and physical access checks
are recorded in `tools/structure/desert-*-20260909.json`. The reviewed private assets
are assembled in `run/desert-integration/datapack` and installed as `kithkyn-desert`
after `mod_data`. Its filter removes superseded Desert definitions from lower packs;
no old-building aliases or save migrations are provided.

The native regional fixture checks all 28 templates, the nine housing alternatives,
twelve rotated storehouse/market upgrade fits, four founding rotations and save
reloads, natural biome selection, bed colors, and banner patterns. Separate physical
tests walk workers to their beds, private and communal containers, service stations,
mine stairs, and elevated guard posts. Market counter entrances remain two blocks
high and wide enough for adult workers to move between stalls.
