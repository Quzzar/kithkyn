# Forest village world generation

Research checked 2026-09-08 against two upstream GitHub source snapshots targeting Minecraft
Java 1.21.1. This compares generation mechanisms, not the installed modpack. The final section
records the separate Kithkyn implementation check. No external village mod is required by
these findings.

## Finding

Forest village generation is a supported design: both projects explicitly allow forest and
birch biomes through their own structure biome tags. Biome eligibility, candidate spacing,
and terrain suitability are separate decisions. Towns and Towers demonstrates the first two
with ordinary Minecraft jigsaw definitions. Repurposed Structures additionally rejects
candidates using surrounding biome and terrain samples. See the pinned definitions below.

| Project and source snapshot | Forest eligibility | Candidate placement defaults | Terrain evidence |
| --- | --- | --- | --- |
| [Repurposed Structures 7.5.22, `1.21-Arch`](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/gradle.properties) | Separate birch and regular-forest collections | Custom random spread: spacing 50, separation 25, salt 608450500 | Birch rejects a sampled height spread above 22 blocks and checks neighboring biomes |
| [Towns and Towers 1.13.11, `1.21.1`](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/gradle.properties) | Dedicated birch and forest village tags | Minecraft random spread: spacing 51, separation 12, salt 213742069 | Reviewed villages use Minecraft jigsaw placement and `beard_thin`; no comparable slope filter is declared |

The placement numbers above are raw source resource defaults, in chunks for spacing and
separation. They describe candidate placement, not a guarantee of a successfully generated
village within that distance. Runtime configuration or datapacks can change them.

## Repurposed Structures

The [birch structure definition](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/worldgen/structure/village_birch.json)
uses `repurposed_structures:generic_jigsaw_structure`, its own town-center pool, size 6,
`WORLD_SURFACE_WG`, `beard_thin`, and
`#repurposed_structures:has_structure/villages/birch`.

That [structure biome tag](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/tags/worldgen/biome/has_structure/villages/birch.json)
references a [birch collection](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/tags/worldgen/biome/collections/birch_forests.json)
containing `minecraft:birch_forest`, `minecraft:old_growth_birch_forest`, optional
`#c:is_birch_forest`, and optional named biomes from Terralith, Regions Unexplored, Wythers,
and others. Its [oak biome tag](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/tags/worldgen/biome/has_structure/villages/oak.json)
uses a [regular-forest collection](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/tags/worldgen/biome/collections/regular_forests.json)
that explicitly includes `minecraft:forest` and optional named mod biomes. The reviewed oak
collection does not use a universal forest-family tag. Support is therefore a combination
of conventional tags where present and curated biome IDs, not an inference from every
biome's display name.

The [overworld village structure set](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/worldgen/structure_set/villages_overworld.json)
contains multiple biome variants, including birch at weight 4 and oak at weight 1. It uses
`repurposed_structures:advanced_random_spread` and a six-chunk exclusion against
[`#repurposed_structures:overworld_village_avoid`](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/resources/data/repurposed_structures/tags/worldgen/structure_set/overworld_village_avoid.json),
which contains vanilla villages and pillager outposts.
[The placement implementation](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/placements/AdvancedRandomSpread.java)
selects a seeded candidate within each spacing region and rejects candidates near the
configured other structure sets. This is placement avoidance, not collision inspection of
every generated building.

The additional terrain work is concrete. In
[`GenericJigsawStructure.extraSpawningChecks`](https://github.com/TelepathicGrunt/RepurposedStructures/blob/d05c5e201881ef3c38cb2ff46a2b16590d7ab0de/common/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/GenericJigsawStructure.java),
the birch JSON's settings mean:

- `valid_biome_radius_check: 1`: test one biome sample in each of a 3 by 3 chunk neighborhood
  against that structure's valid-biome predicate. Checkerboard biome sources bypass this check.
- `terrain_height_radius_check: 2`: sample one height near the center of each chunk in a
  5 by 5 neighborhood.
- `allowed_terrain_height_range: 22`: reject when the highest sampled terrain exceeds the
  lowest by more than 22 blocks, before jigsaw assembly.

Inference: this can screen out broad cliffs and biome edges at modest sample cost. It cannot
prove that each building footprint is flat, accessible, clear of vegetation, or suitable for
future expansion. The code rejects the candidate; it does not search nearby offsets for a
better one. The value 22 is a setting for this particular village generator, not an established
threshold for other village layouts.

## Towns and Towers

The [birch village](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/towns_and_towers/worldgen/structure/village_birch_forest.json)
and [forest village](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/towns_and_towers/worldgen/structure/village_forest.json)
both use `minecraft:jigsaw`, `WORLD_SURFACE_WG`, `beard_thin`, and a maximum center distance
of 116. Their sizes are 6 and 7 respectively; both declare the `fluid_springs` generation step.
Their start pools select the Romanian birch and forest-ruins template families.

The [birch biome tag](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/towns_and_towers/tags/worldgen/biome/has_structure/village_birch_forest_romanian.json)
explicitly includes both vanilla birch biomes and optional named mod biomes. The
[forest biome tag](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/towns_and_towers/tags/worldgen/biome/has_structure/village_forest_ruins.json)
includes `minecraft:forest` and optional IDs from Biomes O' Plenty, Regions Unexplored,
Wythers, and others. Neither reviewed tag uses a conventional family-tag fallback. A new
mod biome needs membership supplied through these tags to qualify for these variants.

The [towns structure set](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/towns_and_towers/worldgen/structure_set/towns.json)
assigns birch and forest weight 5 each and declares `minecraft:random_spread` with a five-chunk
exclusion against `minecraft:villages`. The actual JSON values are 51/12, as recorded above.
The [Cristel Lib configuration descriptor](https://github.com/Cristelknight/Towns-and-Towers/blob/b7fd7edd64f4e39788e108e7ec5836e5fefa81fb/src/main/resources/data/cristellib/structure_config/t_and_t_P.json)
allows placement overrides, but its explanatory comments still say 48/24. Those comments
should not be mistaken for the current resource defaults.

Inference: these definitions demonstrate expanded biome coverage and distinct template
families using Minecraft's jigsaw machinery. They do not establish a general improvement in
terrain viability. The review did not audit Cristel Lib or run either mod across seeds, so
it makes no claim about every runtime hook or measured visual quality.

## Transferable conclusions

The upstream examples support exposing village eligibility as biome tags, keeping placement
settings separate from architecture selection, and checking terrain independently of biome
membership. A style selected after founding cannot by itself make an otherwise ineligible
biome receive a founding candidate. That last statement is a design inference from the
separate eligibility and template-selection mechanisms above; Kithkyn's actual generation
path must be traced separately.

For terrain improvements, Repurposed Structures is the more relevant code example because
its rejection checks are explicit. For adding forest-biome eligibility, Towns and Towers
shows that ordinary structure data is enough. Neither example demonstrates a guarantee of
safe starting footprints or room for a village that grows over time.

## Kithkyn implementation check and recommendation

Checked separately on 2026-09-08. Kithkyn already allows forest founding: its
[`VillageGeneration`](../../src/main/java/com/quzzar/kithkyn/village/VillageGeneration.java)
uses its own seeded 34-chunk spacing grid with separation 8, rather than consuming vanilla
village biome eligibility or exact vanilla structure locations. It runs during exploration
on the server tick, waits for a loaded 7 by 7 chunk neighborhood, and checks surface water
and separation from existing Kithkyn villages. The deployed and staged generation classes
were byte-identical during this check. The dev configuration enables Kithkyn generation;
the server's `generate-structures=false` setting is not a gate in this custom tick path.

The same [`VillageStyle.fromBiome`](../../src/main/java/com/quzzar/kithkyn/village/buildings/VillageStyle.java)
selection serves manual and natural founding. It recognizes explicit style tags, the
conventional birch-forest tag and birch-named biome IDs, then uses climate fallback for
unclassified biomes. Selecting Birch assets and allowing a forest founding are already
separate concerns in this implementation.

The limitation at investigation time was the single sampled location. Once handed to
[`registerNaturalVillage`](../../src/main/java/com/quzzar/kithkyn/savedata/VillageManagerSaveData.java),
that location receives only one attempt per server session, even if founding later fails.
There is no search for a better nearby center. The preliminary height check uses
`WORLD_SURFACE`, but actual [`Village.initNew`](../../src/main/java/com/quzzar/kithkyn/village/Village.java)
recomputes the founding ground using `MOTION_BLOCKING_NO_LEAVES` and asks
[`FoundingLayout`](../../src/main/java/com/quzzar/kithkyn/village/buildings/FoundingLayout.java)
for safe center, mine and storehouse sites. Do not infer that the preliminary canopy height
is used as the final building plane.

Recommendation from the investigation: retain that shared founding planner and biome-style
selection, add a bounded nearby-candidate search with inexpensive terrain screening, then
validate actual founding footprints using the existing protection and preparation rules.
Limit work per tick and never force-load unexplored land for the search. Repurposed
Structures is evidence for the screening idea; nearby retries are our proposed adaptation,
not behavior demonstrated by either upstream project. This source review establishes forest
eligibility, not a measured natural-founding success rate across forest terrain.

The subsequent implementation follows this recommendation through the shared prepared
founding plan and a finite 25-site search. Its current behavior and limits are described in
[site-selection.md](../site-selection.md#natural-founding-searches-nearby-land). The original
observations above describe the pre-change code, not the deployed state after that update.
