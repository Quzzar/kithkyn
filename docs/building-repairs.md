# Building repairs

Builders use spare time to replace ordinary blocks damaged by explosions. All repairs are low
priority: construction and walls come first. The builder skips unavailable materials, unsupported
cells and failed approaches, tries other repairs, and returns to other work. A missing repair item
does not create a construction goal, shortage event or worker complaint.

Every replacement consumes its actual block item from the builder's pack. The builder visits a
village chest to fetch exact stock; construction's generic wood, wool and stone substitutions do
not apply. One cobblestone costs one cobblestone; a double slab costs two slabs. Full packs leave
goods in the chest. Terrain turf and worn paths are restored with paid dirt; grass can regrow and
the ordinary path worker can wear paths again.

## Evidence of damage

For new explosions, the village records the previous state of ordinary village-owned blocks and
nearby earth. It confirms that each recorded position actually became air after the explosion
tick, since an explosion's affected list can include blocks that survive or are protected by
another mod. Earth repair is local to buildings and destroyed nearby path tiles, not a general
terrain restoration system. It rebuilds known lost layers from supported ground upward.

Existing building damage has a conservative fallback: a single-palette template says a simple
structural block belongs here, the position is now air, and the ownership store still records a
village block. Explosion removals leave that ownership record behind; deliberate player breaks
and the miner's excavation clear it. Templates with ambiguous alternative palettes are skipped.
Old unrecorded terrain cannot be reconstructed. Surveying processes a small batch of template
blocks at a time, rather than rescanning every building each tick.

## Preservation

- Player placement and breaking cancel both pending explosion snapshots and queued repairs.
  Placement also removes previous village ownership, including when a player plants in the hole.
- A builder rechecks that the position is air and unowned by a player immediately before paying.
  It never removes or overwrites a replacement block.
- Planned mine excavation, ribs and entrance headroom are excluded in every rotation. Mine
  supports and escape work remain the miner's responsibility.
- Only ordinary full blocks, stairs, slabs, fences, walls and panes with exact block items are
  supported initially. Liquids, plants, leaves, block entities, doors, beds and other special
  fixtures are skipped. No stored goods, captured block-entity NBT or entities are restored.
- Repairs are attached to a building's identity and revision. Removing or replacing that
  building invalidates its old repairs.

Confirmed damage is stored per dimension in `RepairStore`, capped at 8,192 positions. An
unavailable item remains in that bounded queue for later; it never blocks an affordable item
behind it. Retry cooldowns are temporary, while confirmed damage survives save and reload.
Within maintenance, lower ground is considered before structural walls and roofs. The shared
worker loop still owns walking, interruption, night and failure recovery.

`RepairVerification` exercises real selection, chest fetching, paid placement, explosion
confirmation, save/load, player protection and rotated mine exclusion. Run only on a disposable
server with `-Dkithkyn.repairs.verify=true`; it exits with a `[repairs-verify] RESULT` log line.
