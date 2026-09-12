# Tundra village

The Tundra catalog is the exposed frozen-lowland village: compact snowbound buildings with
stone brick, packed ice, spruce details and enclosed food production. It is Aaron's selection
from the complete Tundra gallery built on 2026-09-12. The style token is `tundra`. Snowy and icy
biomes, including Snowy Plains, Ice Spikes, snowy beaches, frozen rivers and compatible modded
biome paths, select it whenever its complete private founding set is loaded.

The catalog deliberately has no castle. T17.3 and the other castle studies remain gallery
references for a future village variant.

## Founding village

`village_center_tundra_1` is T01.1. It contains four beds, four personal barrels and the four
core vacancies: quartermaster, builder, guard captain and miner. The quartermaster works in the
separate storehouse and the miner works at the separate mine. Those two buildings are placed by
the normal site planner as the centre's founding companions, so they use the same spacing,
orientation and terrain rules as later construction rather than forming a fixed compound.

The bell is the meeting point. The centre has no campfire. Four ordinary snow golems are captured
with the structure and start unclaimed, allowing nearby guards to adopt them through the normal
golem recruitment system.

## Selected buildings

The private datapack contains 23 definitions:

| Id | Use |
| --- | --- |
| `village_center_tundra_1` | T01.1; four core jobs, four worker beds, four personal barrels, bell and four snow golems |
| `mine_tundra_1` | T05.6; a three-block-wide shaft running south from its north-facing entrance |
| `storehouse_tundra_1` | T01.5; two reachable village storage barrels and the quartermaster worksite; the third buried barrel remains decoration |
| `house_tundra_1` | T01.4 one-bed home |
| `house_tundra_1__two_single` | T01.3 two single beds with separate personal storage |
| `house_tundra_1__three_single` | T01.6 three single beds with shared storage |
| `couple_cottage_tundra_1` | T01.2 couple home with shared personal storage |
| `butchery_tundra_1` | T02.1 butcher station and three persistent sheep in the enclosure |
| `fishery_tundra_1` | T02.3 fisher's hut with its worker bed and storage |
| `lumberjack_tundra_1` | T02.4 lumber yard with a worker bed, personal and shared storage, and a spruce planting site |
| `watchtower_tundra_1` | T03.1 sole tower level; crossbow post, guard bed and personal chest; the authored fence, lantern and six-rung ladder remain unchanged |
| `church_tundra_1` | T03.2 cleric station with a worker bed and personal chest |
| `stoneworks_tundra_1` | T03.4 mason station with a worker bed plus separate personal and workplace chests |
| `blacksmith_tundra_1` | T03.5 forge with a worker bed, personal barrel and workplace chest |
| `farm_tundra_1` | T05.5 compact wheat-and-carrot farm |
| `farm_tundra_2` | T06.2 berry and glow-berry greenhouse; an in-place upgrade from the compact farm |
| `hunting_lodge_tundra_1` | T04.8 hunter building with a worker bed, personal chest and workplace chest |
| `bakery_tundra_1` | T04.4 bakery with the baker's bed and snowy outdoor bake-fire roof |
| `tavern_tundra_1` | T11.2 tavern; two upstairs worker beds for two innkeepers and three downstairs general beds |
| `well_tundra_1` | T09.10 compact blue-ice and stone-brick well |
| `market_tundra_1`, `_2`, `_3` | One, two and three merchant stalls with fixed trade colours and one supported entrance carpet per stall |

## Identity and wall

Every logical bed is a neutral template slot resolved to the village primary or secondary colour.
Both halves of a couple bed always receive the same colour. Every authored banner slot receives
the generated village banner. Market fabric is the exception: all three tiers keep the established
red, cyan, orange and white trade colours.

The single wall level uses the approved arid wall geometry with snow-block courses, packed-ice
frames, stone-brick stairs and slabs, spruce rails and trapdoors, hanging lanterns, and single lit
brown candles. There is no second wall upgrade.

## Authoring and installation

`run/tundra-integration/prepare.py` promotes the selected live captures through
`tools/structure/VillageTemplateExport.java`. It strips every barrier and structure-void marker,
neutralizes beds and banners, records authored air above the ground layer and preserves the four
snow golems and three sheep. It also corrects the three approved market counters so their barrels
sit above a complete foundation like the other working regional markets. The private production
assets remain under `run/tundra-integration/datapack`; source and output hashes are recorded in
`tools/structure/tundra-catalog-20260912.json`.

T03.1 is exported without navigation-driven block overrides. Person navigation represents the
open cell above the top ladder rung as a two-way ladder transition and rejects decorative lantern,
candle, fence, gate and wall tops as false floors. Guards therefore descend through the authored
ladder opening instead of attempting an impossible lantern-to-railing shortcut.

Manual testing can create the style with `/kithkyn create-village ~ ~ ~ tundra`. Natural founding
in a snowy or icy lowland uses the same style selector and founding path.

## Verification

The final native verification covers all 23 strict templates, both construction paths, four
rotations, identity substitution, entity placement, upgrades and restart persistence. Full-size
adult villagers physically visit every station and declared container, transfer items, return to
personal storage and sleep in assigned beds. The founding simulation separately checks snowy and
icy biome selection, the four centre jobs, naturally planned mine and storehouse companions, the
three-block mine mouth and the four captured snow golems. Exact run totals and log paths are kept
in `tools/structure/tundra-catalog-20260912.json`.
