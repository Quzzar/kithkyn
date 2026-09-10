# Allay quartermasters

A village can adopt a vanilla allay as an auxiliary keeper of its stores, the way it recruits
an iron or snow golem as an auxiliary defender ([iron-golem-guards.md](iron-golem-guards.md)).
It does not create the allay, spend resources, fill a quartermaster post, or take a person's
job away. The bond is to the village, not the adopting quartermaster, so it survives that
quartermaster changing jobs or dying.

## Recruitment

The village's quartermaster takes on the helpers, not its guards: every active human
quartermaster checks a small encounter area once every five seconds, half a period apart
from the guards' golem scan. A visible allay within eight blocks that is not leashed, riding or
already claimed joins the quartermaster's own settlement. The claim is committed
synchronously, so two quartermasters cannot adopt the same allay, and another village cannot
take an adopted one. The quartermaster keeps their post and work station in the storehouse and
remains the one who plans the shelves; the allays assist.

A storehouse can be authored with its own allays: any allay embedded in a building's structure
file is tagged with that building when it is spawned, and until the quartermaster takes it on it
waits by that storehouse's note block, held there by vanilla's own liking for a note block. With
nothing in hand it throws nothing there. The floodplain storehouse starts with two.

An unnamed allay receives a locally selected name, which the quartermaster announces and
remembers. Names appear only when the player aims at the allay.

## The keeper's loop

An adopted allay does what the human quartermaster does
([worker-loops.md](worker-loops.md), the storehouse loop), at the same pace, with one difference
of scale: its single inventory slot is its pack, so a trip moves one stack.

- **Collect.** It flies to the nearest shared workplace container that holds goods, skipping the
  storehouse shelves and the market's treasury, and takes one stack, thirty ticks a move.
- **Deliver.** It carries the stack to the shelf the village's shelving plan assigns to that item,
  and only when no owning shelf can take it, to an overflow shelf. A full storehouse raises the
  village's storage strain; an emptied pack clears it, the same signal the human loop uses.
- **Inspect.** With nothing to carry it visits the shelves in rotation, tidies the one in reach
  with the shared layout algorithm, and carries a misfiled stack to its own shelf when that shelf
  can take it.
- **Home.** Between rounds it waits above the storehouse's note block, or over the first shelf
  when there is none.

Allays have no goal selector, so the loop is a brain behaviour added beside vanilla's idle
behaviours whenever the entity loads. Vanilla's own courier tricks stay switched off for a
keeper: it forgets any player who hands it an item, so it never delivers the village's goods to
a passer-by, and it is never bound to a note block as a drop target. The keeper opens the
wooden doors it meets, since vanilla allays cannot.

The keeper follows the shelving plan; it never draws one up. Planning is a conversation
([worker-loops.md](worker-loops.md)) that only a person can hold, so a village with allays and
no human quartermaster shelves everything to overflow until a person plans the shelves.

## Membership and limits

- They remain vanilla allays: flying, one carried stack, the amethyst duplication and the player
  interaction are untouched. A duplicate is an ordinary unclaimed allay until a quartermaster adopts it.
- They never join the human population, take a bed, consume food, fill a quartermaster post, or
  count toward tiers. The labour planner's storage rules are unchanged; an allay does not stand
  in for a quartermaster when goods back up.
- Same-village people, golems and companion pets treat them as members and will not target them.
- A separate saved roster records each allay's UUID, name and last-known chunk, and gets the same
  chunk-loading bubble as residents. Unloading is not death; death or explicit destruction
  removes the roster entry, and nothing is respawned. A stack being carried at death drops like
  any allay's inventory.
- Adoption works in any village. Only the note block and any authored allays are per catalog.
- There is no second storehouse tier for the floodplain village. A village wanting more room
  builds more storehouses, and every keeper, human or allay, serves all of them: the shelves are
  the containers of every standing storage building, in building order.

## Authoring

Place a note block within four blocks of the storehouse's declared containers for the keeper's
perch. To start a storehouse with allays, embed `minecraft:allay` entities in its structure file;
they are placed once, on completion, like farm livestock.

## Verification

`AuxiliaryRosterTest` covers the allay roster's save, load and legacy defaults beside the golem
roster. `ShelvingTest` covers the shelf arithmetic both keepers share, with a one-slot pack.
`AllayVerification`, enabled only with `-Dkithkyn.allays.verify=true` on a disposable local
server, exercises the storehouse tether, quartermaster-only adoption, duplicate claims, a full
collect-and-deliver haul from a workplace chest to the shelves, reload persistence and death removal. Never enable that
flag on a review or play world: it creates its own fixture and stops the server when finished.

## Getting around

A keeper plans its flights with its own evaluator (`entities/ai/AllayPathNavigation`).
Vanilla flight admits every trapdoor cell whether the panel is open or closed, so a keeper
whose route crossed a closed hatch flew into it and pressed there; nobody in this mod opens
trapdoors, people included, so a closed trapdoor is a wall to a keeper and its route goes
round. Wooden doors are different: a keeper's route may pass through one, and the keeper
opens the door it is brushing past itself, since allays have no door behaviour of their own.
Buildings should therefore leave keepers an open panel or a door into every room they
serve; the floodplain storehouse exports its doorway trapdoor open for exactly this reason.

## Naming

An adopted keeper wears one of ten fallback names at once (Wisp, Fen, Reed, Sedge, Mote, Lumen,
Dewdrop, Rush, Tallow, Pip), the way a guard's golem does. The quartermaster who took it on
then names it themselves through the same single-voice talk an owner has when naming a pet
(`village/KeeperNaming`, on the shared `SingleVoiceNaming` plumbing with `PetNaming`): they
speak as themselves about the spirit at their counter, are shown the ten as the kind of name a
keeper carries, and settle a name of their own, which the keeper takes and the quartermaster
remembers. When the LLM is down or the talk lands no valid name, the fallback stays.
