# Village identity

A village receives one visual identity when it is founded, at the same moment its permanent name
is chosen. The identity contains:

- a primary Minecraft dye color;
- a distinct, visually contrasting secondary dye color;
- a generated vanilla banner pattern made from those two colors.

The identity is saved for the village's life. It is separate from the village-biome architecture:
two Birch Forest villages can share the same building catalog while using different colors and
flags.

The current founding implementation selects colors and one of seven two-color banner designs
locally, immediately after the generated village name arrives. This is an automatic random
selection, not a choice made by the village language model. The complete ordered banner layers,
rather than just a design index, are persisted, so expanding the design pool does not reroll
existing flags.

Old saves stored only a string in the village's `name` slot. That slot now accepts either the old
string or the complete identity record. A legacy name deterministically generates the same colors
and banner every time it loads, while every new save writes the complete identity into the existing
slot. This preserves save compatibility without exceeding the village codec's sixteen-field limit.

## Founding names

Architecture is selected before naming, including explicit development overrides. Both manual
founding and natural generation give `VillageNamer` that same selected style. Each style supplies
a short community and architectural description and several varied examples of tone. The model
is asked to invent a name, not select an example or label nearby terrain. Birch describes grounded,
welcoming woodland folk, pale timber, mossy cobblestone, grass roofs, candles and shared hearths.

Replies are checked against the examples and current village names on the server thread, ignoring
case, spacing and punctuation. Invalid, copied or duplicate answers get one retry, then a unique
style-aware word-list fallback. Existing villages retain their permanent names. Colors and flags
still use the separate saved identity rules above.

## Structure authoring

Every concrete building definition already declares its own beds, workstations, village storage,
and personal storage. Level is a progression label, not an amenity contract. Two variants of
one tier may declare different bed counts, and village bookkeeping registers what each selected
definition actually declares. The approved Birch family currently stops at the two-bed tier-2
house; see [birch-village.md](birch-village.md) for its exact catalog.

A building may also declare semantic identity slots:

```json
"village_identity": {
  "primary_blocks": [[2, 4, 0], [3, 4, 0]],
  "secondary_blocks": [[4, 4, 0]],
  "banners": [[5, 6, 1]]
}
```

All positions are relative to the structure origin and rotate with the building. Primary and
secondary block slots recolor only an existing supported colored block at that position: wool,
carpet, concrete, concrete powder, terracotta, glazed terracotta, stained glass, stained glass
panes, candles, beds, or plain standing/wall banners used as decorative cloth. Decorative
banner accents change their base color without receiving the village flag's pattern or name;
white cloth without a slot stays white. A bed slot may name either half: both halves are recolored together,
preserving their facing and part. Do not assign opposite halves of one bed to different color
roles. This makes the author's intent explicit and avoids recoloring an entire regional
palette.

Markets are the deliberate exception to village-colored fabrics. Their tents, awnings,
decorative cloth banners, rugs and matching candles keep the original stall colors, suggesting
traders from beyond the village. Do not add those blocks to `primary_blocks`, `secondary_blocks`
or heraldic `banners`. Regional market variants may change timber, masonry and planting while
preserving the authored fabrics. The reviewed Birch/Mesa/Jungle stalls use orange, cyan and red
with white; the existing Desert stalls retain their own original palette.

A banner slot must point at a standing or wall banner authored in the structure. As it is
placed, its banner base becomes the village's primary color and its generated layers use
the saved primary and secondary roles. Rotation or wall facing is preserved.

Identity slots resolve during both instant and block-by-block placement. The builder's next-block
preview and already-matches check use the same transformed state as actual placement, so a building
does not spend construction wearing the reference gallery's placeholder colors. Either authored
bed half selects the color for both halves. Banner pattern data is applied after captured block
entity NBT, so a captured flag cannot overwrite the village's own flag. The owning village rebinds
its saved identity to an in-progress project after loading.

The completion and upgrade-registration passes apply the same identity again as an idempotent
final check. An unsupported block or a banner slot without a banner is left unchanged and logged
as an authoring error.

## Gatehouse flags

The Birch gatehouse carries four face-mounted flags copied from the September 9 live review:
two on each face, at local along offsets -2/+2, outward offsets -3/+3, and height 4 above
the gate base. The neutral asset contains white wall banners without a captured village pattern.
Its banner positions and facing rotate with the gate; their supporting masonry is built first.

`WallRaiser` uses `VillageIdentityApplier`'s same banner state and block-entity application as
buildings. Matching checks layers and name as well as base color, including white-primary flags.
`WallProject` binds the owning village's identity at creation and after village decoding; it
does not persist a second identity or generate another design. Village instant placement and
incremental builders share this path. Standalone wall previews without a village remain white.
Player-owned cells remain protected, and completed walls do not reopen merely because the new
catalog adds flags. This is future gatehouse artwork, not a blanket repaint of existing walls.
