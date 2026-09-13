# Building seating audit: all-variant gallery

**Status: live gallery review in progress; two correction passes are deployed and verified.** The
active test world contains 224 loaded building definitions across nine variants. Every one was
placed successfully on one continuous grass datum through the same authored-sink path used by
village founding, growth, and exact developer placement.

## Gallery

- Original review entry: `11997.5, 222, 797.5`
- Original command origin and grass surface: `12000, 220, 800`
- Complete platform bounds: `11997, 214, 797` through `12143, 220, 1579`
- Corrected review entry: `13997.5, 222, 797.5`
- Corrected command origin and grass surface: `14000, 220, 800`
- Corrected platform bounds: `13997, 214, 797` through `14143, 220, 1579`
- Second-correction review entry: `15997.5, 222, 797.5`
- Second-correction command origin and grass surface: `16000, 220, 800`
- Second-correction platform bounds: `15997, 214, 797` through `16143, 220, 1579`
- Layout: 224 labelled plots in 38 rows
- Support: grass at Y 220, with dirt from Y 219 through Y 214
- Labels: category, variant, numeric level, authored sink, and design name
- Runtime cost: no gallery chunks remain force-loaded after placement

The gallery uses rotation `NONE` so authored structure coordinates are easy to compare. A village
may rotate the same structure, but rotation does not change its vertical seating. The gallery does
not register these displays as village buildings and does not create another simulated village.
The two earlier galleries remain untouched as records of their live editing passes. Each additive
corrected gallery was rebuilt from the promoted templates so unreported edits in an earlier
gallery could not be erased by an in-place refresh.

## Mechanical pre-pass

The seating checker looks for bottom-half entrance stairs occupying the same block layer as the
grass surface after the authored sink is applied. This is a useful sign that an entry step may be
swallowed by half a block. It is deliberately only a prioritization signal: farms and landscape
courses can use stairs as edging, buildings without stairs can still sit incorrectly, and an
intentional recessed entrance can be valid.

### Likely one block low in the mechanical pre-pass (12)

- `village_center_birch_forest_1`, sink 4
- `storehouse_badlands_2`, sink 5 before live review
- `well_floodplain_1`, sink 1
- `hunting_lodge_jungle_1`, sink 0
- `mine_jungle_1`, sink 0
- `mine_mediterranean_1`, sink 0
- `butchery_romanian_1__herder_home`, sink 0
- `farm_romanian_1`, sink 0
- `farm_swamp_1`, sink 0
- `farm_swamp_2`, sink 0
- `farm_tundra_1`, sink 0
- `well_tundra_1`, sink 0 before live review

### Ground-course or edging ambiguity, judge by eye (8)

- `farm_badlands_1`, sink 0
- `village_center_desert_1`, sink 0
- `farm_floodplain_2`, sink 0
- `mine_floodplain_1`, sink 0
- `farm_jungle_1`, sink 0
- `mine_swamp_1`, sink 0
- `fishery_tundra_1`, sink 1
- `mine_tundra_1`, sink 0

The visual pass used these cases as prioritization, then walked the remaining rows for
floating foundations, buried doors, disconnected paths, and water or farm surfaces that disagree
with the grass datum. Pay particular attention to definitions labelled `sink -1`, since
their authored layer zero begins one block above the grass and a missing foundation course would
appear as a visible gap.

## First live-review corrections

Aaron's first walk established five height changes. The Desert church and Swamp butchery rise one
block (`sink -1`). The Desert fishery lowers two blocks (`sink 2`), while the Floodplain and Tundra
fisheries each lower one (`sink 1`). These are visual decisions, not deductions from the stair
heuristic.

The same pass found several structure-content issues:

- the Swamp bakery keeps Aaron's two added lanterns;
- the Badlands butchery replaces its low water trough with Aaron's full water cauldron;
- all ordinary butchery doors now start closed, and the audit rejects a future open one;
- the Desert butchery starts with three cows and three sheep, while the Jungle butchery starts with
  three cows and three chickens;
- the Floodplain church's new barrel is shared storage, and the Swamp cleric home's new chest is
  shared cleric storage while its original chest remains personal storage for the bed;
- the Swamp fishery and canonical level-two Jungle house preserve Aaron's live vine trimming;
- four additional ground-level vines were removed from the Jungle couple cottage after the
  sink-aware full-catalog pass found that they would replace grass and leave holes;
- the Mediterranean lumberjack's dead bush marker is now an oak sapling; and
- all three Birch market tiers omit bottom-layer air so they leave surrounding terrain intact.

The retained live captures are under
`run/trio-20260912/generated/kithkyn/structures/audit20260912_*.nbt`. The source templates before
this repair are under `run/gallery-review-repairs-20260912/before/`.

## Second live-review corrections

The second walk established eight more direct seating changes. The Birch Forest mine lowers one
block (`sink 0`). The Badlands storehouse level two rises one block (`sink 4`), while the Desert
storehouse level two lowers four (`sink 4`). The Desert watchtower level two lowers one
(`sink 1`). The Birch Forest and Romanian wells each lower seven blocks (`sink 7`); the
Mediterranean and Tundra wells each rise one (`sink -1`).

The Desert mine keeps its visible building at the reviewed height while gaining a 3-by-5 shaft
mouth at the grass datum. Its structural frame and all absolute metadata moved up one local block,
and its authored sink changed from `-1` to `0`, so the visible frame does not move. The Romanian
and Tundra mines preserve the matching 3-by-5 shaft mouths carved during the review.

The Romanian level-one watchtower preserves four reviewed lanterns, including their exact hanging
states, and the Swamp level-one watchtower preserves its reviewed hanging lantern. The Floodplain
fishery now omits its four corner air cells on the terrain layer, so placement preserves the grass
instead of cutting four holes. Snow laid by gallery snow golems was not captured as authored
structure content.

The retained second-pass live captures are under
`run/trio-20260912/generated/kithkyn/structures/audit20260912_pass2_*.nbt`. The source templates
before this repair are under `run/gallery-review-pass2-repairs-20260912/before/`, and the repair
manifest is `tools/structure/gallery-review-repairs-pass2-20260912.json`.

## Verification receipt

- Server and Prism client loaded the same second-pass jar SHA-256:
  `e76d2fbc572b25a64fde04c468f043b3b8220c4486c2b1b8baac623885b57009`.
- The restarted server loaded 224 definitions, and the second-correction gallery placed 224 of 224
  while preserving both earlier galleries.
- The complete 244-template runtime asset set passed the barrier, bounds, terrain-surface,
  butchery-door, butchery-livestock, market-ground, and market-entrance audit. This count includes
  wall and other non-building templates in addition to the 224 gallery definitions.
- The sink-aware catalog check found no vine at or below its building's terrain layer.
- First-pass live commands verified all five corrected sink labels, representative closed
  entrances in all seven repaired butcheries, both missing livestock groups, both Swamp bakery
  lanterns, the Badlands water cauldron, both new church containers, the Mediterranean oak
  sapling, preserved terrain under the trimmed vines, and grass under each Birch market tier.
- Second-pass live verification checked all affected sink labels and 54 exact block states:
  45 mine-mouth air cells, four preserved Floodplain fishery grass cells, and five watchtower
  lanterns with their standing or hanging state.
- Four snow golems were removed from the new gallery bounds before the world was saved, and
  `mobGriefing` was restored to `true`.
- No gallery chunks remained force-loaded after verification.
- The far platform corner was verified as grass at Y 220 and dirt at Y 214, then saved.
- `./gradlew check jar` passed with 577 tests completed, eight skipped, and no failures or errors.
- The pre-gallery world is recoverable from
  `run/backups/before-seating-gallery-20260912-213106`.
- The world immediately before the live-review repair deployment is recoverable from
  `run/backups/before-gallery-review-repairs-20260912-2228`.
- The world immediately before the second correction deployment is recoverable from
  `run/backups/before-gallery-review-pass2-20260912-2302`.
