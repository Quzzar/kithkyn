# Structure tooling

The pipeline that turns a raw schematic or Litematica capture into a
`kithkyn` structure `.nbt` the datapack can place. Dev tooling, not mod
code — it lives outside `src/` and is never shipped in the jar.

**Licensing:** the source→licence position for every structure set (which are
reference-only, which are used by permission, which are free) lives in
[`docs/structure-sourcing.md`](../../docs/structure-sourcing.md). Read it before
adopting anything from a third-party set. These tools carry no structure data
themselves — they only transform files you point them at.

## The pipeline

```
.schematic / .litematic  ──►  flatten  ──►  schem2nbt  ──►  validate + navcheck
   (raw capture)          (old ids →      (→ 1.21.1 .nbt)    (is it placeable
                          blockstates)                        and walkable?)
```

| Tool | Does |
| --- | --- |
| `nbt.py` / `nbtwrite.py` | Read and write raw NBT — the format every other tool speaks |
| `litematic.py` | Read a Litematica `.litematic` into a `{(x,y,z): (name, props)}` grid (handles the bit-packed palette that straddles longs) |
| `schem2nbt.py` | Convert an MCEdit `.schematic` to a 1.21.1 structure `.nbt` |
| `flatten.py` | Map pre-1.13 numeric block ids to modern blockstates |
| `split_scene.py` | Crop the separate buildings out of one multi-building `.schematic` (flood-fill footprints; rejects trees by composition) |
| `validate.py` | Flag blocks that would drop on placement — bed and door halves, wall torches, gravity-affected stacks, carpet on nothing |
| `audit-templates.py` | Reject production templates containing barrier states or coordinates outside their declared size |
| `navcheck.py` | Score how walkable a finished structure is for a villager |
| `roof.py` | Fix roof-stair facing |
| `seating-check.py` | Flag catalog buildings seated one block low: a bottom-half stair in the layer that meets the ground is a doorstep swallowed by the terrain |
| `export-birch.py` | Derive the approved Birch assets from the immutable capture manifest, rebase amenities, and print new definitions as an apply_patch patch |
| `RemoveTemplateBlocks.java` | Drop listed cells from structure templates with Minecraft's typed NBT reader and writer (plan: `[{"path", "remove": [[x,y,z], ...]}]`); the September 12 market floor-mat repair, `market-mat-repair-20260912.json`, is its record |
| `ReplaceTemplateBlocks.java` | Swap the block at listed cells of structure templates for another state, keeping facing/half/shape unless the plan gives properties, with the typed NBT reader and writer; the September 12 market stripe-stair restoration, `market-stripe-repair-20260912.json`, is its record |
| `VillageTemplateExport.java` | Native Minecraft NBT export preserving typed entity data, clearing gameplay inventories, neutralizing explicit identity slots, and carving declared air | An optional `entities` plan key adds authored livestock or allays to a template.

Each script's `__main__` is an example driver; point the glob at your own
structure directory. Run them from this directory so their imports resolve.

Run `python3 tools/structure/audit-templates.py <data/kithkyn/structure>` over every
public or private catalog before deployment. Gallery containment barriers, including unused
palette entries, are review fixtures, never building content. Blocks outside an explicit crop
are invalid for the same reason. The native exporter enforces both invariants while writing a
template. `./gradlew check` runs the audit over the public catalog automatically.

The Birch export expects the canonical project root, prepared Minecraft runtime dependencies,
an approved capture directory and a destination asset directory. It never reads a live world.
It refuses to overwrite existing JSON definitions; deliberate metadata revisions should be
reviewed as normal patches, not regenerated over user changes. The generated export plan can
be passed to the native writer separately to reproduce binary assets. Append `--assets-only`
to regenerate only the derived NBT files and export report, without replacing definitions.
Add `--only=bakery_birch_forest_1,tavern_birch_forest_1` to limit that export to named assets.
Exterior air is limited to the envelope of actual authored blocks, not the original capture
rectangle. Capture coordinates remain stable; runtime `BuildingFootprint` supplies tight planning
and claim bounds without moving saved amenity offsets. Mine entrance direction/offset and the
raised storehouse seating are explicit building metadata, not block-coordinate rewrites.
The one approved-set accessibility correction is a matching lead-in stair at the stoneworks;
the source capture is kept intact. Do not use the old generic
NBT writer on these entity-bearing captures: preserving numeric NBT types matters for entities.
The original approved captures remain unchanged. See [the Birch integration record](../../docs/birch-village.md).

The center's September 8 sky-workshop edits are a checked-in override plan,
`birch-center-20260908.json`, applied by the same exporter. Its 11-block height includes four
new treetop blocks above the original capture. The same revision path applies
`birch-storehouse-20260908.json` and `birch-fishery-20260908.json`. These preserve the user's
removed storehouse forecourt, new fishery steps and relocated fishery barrel. Amenities and
clearance are derived from the edited shape; removed outer blocks do not become a new
air-clearing apron. Storehouse sink stays -1 and fishery sink stays 0. Other buildings still
derive byte-for-byte from their earlier approved source. The later center access revision opens
the four low exit overhangs and moves each identity banner one block inward. Its `banners` field
updates the semantic slot positions as well as the block overrides. Beds, personal storage and
golem NBT are unchanged. `birch-mine-20260908.json` records the separate six-cell rear-stair move;
front steps, workstation and shaft frame remain unchanged. Review platforms, runtime path wear,
excavated tunnels and frozen preview entities are not imported.

The September 9 center approval supersedes the earlier entrance opening: the same center
revision now restores four chiseled-stone lintels and outer slabs and returns the dynamic
banners to their outward positions. Villager collision heights are capped separately from
their visible models. The immutable sky capture and initial comparison are recorded in the
revision's `collision_capture` fields; all twelve edits are reproduced by the existing exporter.

`birch-walls-20260908.json` records 44 masonry swaps from the wall showcase across
straight, diagonal, terrace, corner-tower and gatehouse pieces. These positions
are local to the cropped runtime pieces: the native writer crops the original
padded capture first, then applies the overrides. Source hashes and cropped sizes
must match. The showcase platform, snow and labels are not structure content.

## Preserving active construction during an asset update

`FreezeBuildingProjects.java` adds `template_snapshot` to pending building projects in a **new
copy** of a village save. It reads the currently deployed (old) assets, not the replacement
assets. It verifies that all preexisting NBT data remains exactly equal. Stop/save the server
and back up the world first; never replace a live saved-data file. Arguments are input `.dat`,
new output `.dat`, and the old `data/kithkyn/structure` directory. The runtime then resumes each
cursor against its frozen template. This does not move old buildings or regenerate terrain.

The September 9 bakery/tavern separation uses `birch-bakery-20260909.json` to remove the
second bakery bed and update identity slots. `birch-tavern-20260909.json` references its own
immutable approved capture, source hash and explicit amenity metadata. The same native
writer preserves the edited grass and lighting, empties storage, and neutralizes only the
keeper's assigned bed. Dense captures can declare `ground_layer` so exterior ground-level
air does not excavate the supporting terrain. `ground_layer` is the highest local template layer
whose empty cells must preserve existing terrain; account for `sink` when choosing it. A sink-one
pond building, for example, protects local layers 0 and 1. The source captures are never rewritten.

## Pueblo center review

`pueblo-center-20260909.json` is the current P01.1 room and staffing manifest. Its `info`
object is a runtime building definition, including `meeting_point`, `campfires`, per-bed
`bed_containers`, per-station `guard_duty`, and village identity slots. Its source capture,
neutral template and prepared datapack paths point into the private `run/` review workspace.
The capture remains immutable; the shared `VillageTemplateExport.java` produces the neutral
review template using the adjacent `export-plan.json`. The completed local Badlands catalog
now binds this center together with its founding mine and storehouse; never activate an isolated
center without that complete set. See [the Badlands record](../../docs/badlands-village.md).
Native room and rooftop access checks run only with
`-Dkithkyn.approvedCenter.verify=true` on the disposable world containing that review datapack.

## Pueblo housing review

`pueblo-houses-20260909.json` records the four edited large houses.
`pueblo-houses-extra-20260909.json` adds the first copies of Medium Houses 1–2 and Small
Houses 1–6. Each entry retains its source hash, captured origin, capacity, explicit couple
pairs, room storage and village-color slots. Alternate copies remain separate gallery
references and are not additional selected designs. These named alternatives do not imply
an upgrade order or assign production categories and prices.

`prepare-reviewed-houses.py MANIFEST --java /path/to/java21` prepares a manifest with a
`work` directory by checking its immutable capture hashes, resolving both halves of each
declared bed, and producing neutral templates through `VillageTemplateExport.java`.
Its identity-only gallery preview patches reuse the private `run/valecraft-gallery/GallerySnapshot.java`
helper; the script itself never writes to the live world. It requires the existing Gradle
server classpath and review workspace. The native house fixture accepts a comma-separated
list of temporary definition IDs in `kithkyn.approvedHouses.ids`, so one fixture can verify
different review manifests without creating fake upgrade tiers.

The fixture walks each resident from the entrance to their own container using
`StashAtHomeGoal`, checks the physical deposit and item conservation, then continues into
their assigned bed using `SleepAtNightGoal`. Maximum-size adults exercise the two-block
collision cap and door clearance. `kithkyn.approvedHouses.rotation` can restrict a diagnostic
to one rotation; the default checks all four. The private launcher
`run/pueblo-house-verification/launch.py` selects a manifest and can narrow it with
`--exhibit` and `--rotation` while diagnosing an individual room.

## Pueblo workplace review

`pueblo-workplaces-20260909.json` records the edited watchtower and mine, selected
blacksmith, and separate butcher-and-coop draft. Its `sources` retain the five immutable
captures and original mod paths; `facilities` hold compact coordinates and neutral-template
paths. A private native composition plan reuses `RamshackleComposite.java`, followed by
`VillageTemplateExport.java`, preserving typed block entities. Explicit ground air retains
the mine opening. Existing source exhibits are not replaced by composite drafts.

The same `ApprovedHouseVerification` fixture accepts workplace definitions through the
private launcher's `facilities` and `review_id` fields. It physically walks to stations,
checks guard equipment, deposits in communal containers, and continues to the resident's
personal chest and bed when present. Bedless buildings use a temporary unassigned walker.
Optional local `review_targets` exercise an enclosure's gate and interior without adding
production metadata. The mine check replays the planned underground ramp and requires
both descent and a return outside; terrain seeding must preserve authored surface blocks
except those explicitly removed by the shaft's real corridor. No review manifest activates
a partial biome catalog or assigns final production tiers and prices.

`pueblo-services-20260909.json` adds ten service selections, including the two authored
storehouse tiers, acacia stand, married-worker farm and sheep farm, and a tavern with one
staff bed and one general bed. The preparation script also accepts `facilities`, optional
`prepared_source` native captures and `export_options_file` conversion plans. Wool identity
slots become neutral white wool while the independent live preview uses the village color.
Existing white awning stripes are outside the identity slots and remain white.

The same native fixture now claims staff couple rooms through production job assignment
before placing general residents. It checks both spouses' physical storage and sleep access,
exact spawned livestock counts and farmed marks, containment after gate visits, and upgrade
placement for selected `upgrades_from` links. The disposable terrain follows each template's
`sink`, including the underground vault and oasis. These checks do not restart the live world.

`pueblo-cleric-20260909.json` records the edited P09.4 Alchemist as the cleric temple,
including its single staff bed, shared workshop chest and healing capability. It uses the
same preparation and access fixtures. It also records the selected town center's built-in
well and the decision to omit a separate well and leave the large watchtower unselected.

`pueblo-markets-20260909.json` reuses the three approved market tiers for the separate
P13 Mesa review row. Native `GallerySnapshot.java` plans make material substitutions,
preserve typed block entities and apply the recorded barrel/passage access repairs.
The source market assets and the Desert Oasis previews remain unchanged. The same native
access fixture checks merchant stations, communal storage and upgrade containment.
