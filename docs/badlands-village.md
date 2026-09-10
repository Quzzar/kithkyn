# Badlands village

The approved Pueblo/Mesa selection uses the environmental runtime style `badlands`.
It is a distinct catalog from sandy Desert/Oasis. For the current broad coverage pass,
Pueblo also serves wooded badlands, savanna and savanna plateau; those biomes can acquire
more specific looks when their own catalogs are ready.
The selected edited structures are installed as a private local datapack. They are not
included in the public source tree or default jar; the source boundary in
[structure-sourcing.md](structure-sourcing.md) still applies to publication.

## Selected buildings

Thirty templates cover the first playable version:

| Role | Selected exhibit | Housing and progression |
| --- | --- | --- |
| Center | P01.1 | Ten beds, bell meeting point, two separate campfires, integrated well |
| Small homes | P02.5, P02.7, P02.9, P02.11, P03.1, P03.3 | Six level-1 choices; P02.9 is a couple room, the others have one bed |
| Medium homes | P02.1, P02.3 | Two level-2 choices with two single beds each |
| Large homes | P01.4 through P01.7 | P01.4 and P01.6 are level 3 with four/six beds; P01.5 and P01.7 are level 2 with two singles |
| Storehouses | P08.7, P10.6 | Levels 1 and 2; vault has the quartermaster's room and personal barrel |
| Mine | P11.5 | Edited well with its approved shaft frame |
| Lumberjack | P10.3 | Acacia planting, one staff bed |
| Stoneworks | P01.9 | Selected smaller mason workshop |
| Blacksmith | P03.6 | Toolsmith/weaponsmith building |
| Farm | P09.7 | Farmer's staff couple room |
| Butchery | P10.4 | Sheep farm with three initial sheep and a staff couple room |
| Hunting lodge | P03.10 | Fletcher building |
| Fishery | P09.8 | Fishing oasis with one staff bed |
| Bakery | P09.5 | Separate baking workplace |
| Tavern | P08.2 | One innkeeper bed and one general bed |
| Church | P09.4 | Edited alchemist building, cleric and one staff bed |
| Watchtower | P03.5 | Edited temple with one guard bed |
| Markets | P13.1 through P13.3 | One, two and three merchant stalls |

P12.1, the earlier butcher-and-coop draft, is excluded. No separate well or large
watchtower is required. The center has seven posts: quartermaster, builder, captain,
two rooftop crossbow guards and two sword patrols. The founding mine adds the miner,
making eight starting positions. The starter storehouse adds storage without a duplicate
quartermaster post.

The twelve houses are alternatives, not a twelve-step upgrade sequence. All are available
as fresh construction. They use the existing house recipes at their assigned price tier;
there are no invented upgrade links between unrelated floor plans. Small House 3 and the
declared pairs in Large Houses 1 and 3 use the shared couple-room allocation. A spouse's
staff accommodation and the tavern's general bed follow the existing worker-bed rules.
Storehouse 1-to-2 and markets 1-to-2-to-3 are the selected actual upgrade chains.

## Identity and environment

All 46 beds across the catalog have explicit color roles: 26 primary and 20 secondary.
Both beds in every couple pair match. Nine actual flags use the saved village banner.
Market awnings have 48 colored hanging strips (32 primary and 16 secondary) and 30 white
strips. The colored strips follow village colors while retaining plain cloth; they do
not become repeated village flags. Architectural clay, masonry and white cloth retain
their authored colors. The standard terrain-following perimeter uses the smooth red
sandstone palette.

The naming profile describes a close community of clay courtyards, stepped roof terraces,
shared households, shaded markets, acacia workshops and protected water. Kestara, Oravel,
Tavren and Sorela are tone examples that generation may not copy. The bounded fallback
uses its own fantasy syllables and avoids duplicate names. This description is a naming
cue, not invented history or an additional saved NPC personality.

`kithkyn:village_style/badlands` maps all three vanilla badlands biomes and all three
vanilla savanna biomes. Conventional mesa/badlands and savanna families also use Pueblo,
including modded family tags and untagged registry paths containing `mesa`, `badlands` or
`savanna`. Birch keeps priority over broad conventional families. Sandy desert families
stay Desert; these recognized families do not randomly alternate between architectures.
An explicit style tag can narrow this temporary coverage when another catalog is ready.
Generic arid climate does not select this catalog. Automatic selection requires its center,
mine and storehouse to be loaded.
Badlands never substitutes another family's designs for missing roles or levels. Existing villages
retain their saved style and names; activation does not reconstruct their buildings.

## Authoring and local installation

The checked-in `tools/structure/pueblo-*-20260909.json` manifests retain immutable
capture hashes, reviewed geometry, rooms and final production bindings. The existing
`prepare-reviewed-houses.py` and native `VillageTemplateExport.java` produce the local
datapack under `run/badlands-integration/datapack/`. Source captures remain unchanged.
The private pack is installed in the local world's `datapacks` directory alongside the
updated runtime. Keep it with world backups: the default public jar intentionally does
not contain these third-party-derived structures.

After installation, ordinary and natural founding use the same selector. A manual sample
can request `/kithkyn create-village ~ ~ ~ badlands`. Authored gallery copies are not
regenerated by installation. New construction uses the selected templates; existing
projects retain their saved plans.

## Verification

Core tests cover explicit biome selection, saved styles, shared house alternatives and
standalone tiers, unique naming, color roles and unchanged neighboring catalogs. The
private asset contracts run with `KITHKYN_BADLANDS_DATAPACK` pointing to the exported pack.
Native verification reuses the approved center, house-access, real placement and natural
founding fixtures. Opt-in verification flags belong only on disposable worlds.
Final native results and local deployment details are recorded with the release under
`run/badlands-integration/`.

The September 9 release passed all 424 unit tests, including the private asset contracts.
All thirty templates passed 240 real placements across four rotations and both instant and
incremental construction, including saved construction progress. A real server restart retained
all 240 buildings and 48 original entities without duplication. Twelve upgrade fits passed.
Four complete founding rotations verified the eight starting jobs, ten center beds, room and
storage ownership, separated meeting and cooking locations, and village persistence. Natural
founding also passed biome selection, protected-anchor safety and delayed-plan checks.

The verified runtime and private datapack were installed locally on September 9. The server
loaded the pack automatically, registered 199 building definitions in total, and accepted the
returning player after the matching Prism client update. The current world and gallery were
fully backed up and preserved. Rendered views confirmed the colored market cloth, village
flags and the complete center with its two roof fires.

The subsequent broad biome coverage pass passed 425 tests and native registry checks for all
six vanilla Pueblo biomes, sandy Desert, and both Birch biomes. The expanded mapping is live
locally; the twelve upgrade fits, all four founding rotations, and natural founding passed
again with that mapping.
