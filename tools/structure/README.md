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
| `navcheck.py` | Score how walkable a finished structure is for a villager |
| `roof.py` | Fix roof-stair facing |
| `mine-level-2.py` | Derive the level-2 mine, in every family, from the shipped level-1 files |
| `export-birch.py` | Derive the approved Birch assets from the immutable capture manifest, rebase amenities, and print new definitions as an apply_patch patch |
| `VillageTemplateExport.java` | Native Minecraft NBT export preserving typed entity data, clearing gameplay inventories, neutralizing explicit identity slots, and carving declared air |

Each script's `__main__` is an example driver; point the glob at your own
structure directory. Run them from this directory so their imports resolve.

The Birch export expects the canonical project root, prepared Minecraft runtime dependencies,
an approved capture directory and a destination asset directory. It never reads a live world.
It refuses to overwrite existing JSON definitions; deliberate metadata revisions should be
reviewed as normal patches, not regenerated over user changes. The generated export plan can
be passed to the native writer separately to reproduce binary assets. Append `--assets-only`
to regenerate only the derived NBT files and export report, without replacing definitions.
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
