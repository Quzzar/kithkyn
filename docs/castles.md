# Castles

## Building and roles

The first castle is the edited R07.2 Desert Fort from Towns and Towers. The editable gallery
copy stands at `4402, 230, 1208`; `tools/structure/desert-castle-20260910.json` records the
capture, room coordinates, village identity, access corrections and deployment evidence.
Source and derived NBT stay in the private local catalog under `run/`.

A Desert village may build one castle. Ordinary planning chooses it when resources and room
allow; it is not a starting building and there is no castle for Jungle yet. The initial recipe
is 64 logs, 128 cobblestone and 16 iron ingots. It uses the shared construction-recipe system,
so a later regional design inherits that price unless it supplies an explicit override.

The fort has eight beds:

| Room | Capacity and eligibility |
| --- | --- |
| Upper eastern suite | Ruler, with the adjacent bed reserved for their spouse; an unmarried ruler can live here |
| Lower bookshelf room | Existing village guard captain; their job stays at the town center |
| Lower shared accommodation | Four general single beds |
| Upper western room | One general single bed with its barrel |

The existing bed ledger owns every assignment. `room_reservations` adds role eligibility to
that ledger; it does not create another housing system. A ruler's station belongs to this
castle, while the captain is matched to the existing center role. A valid new room is checked
before releasing an old bed. A married captain stays with their spouse rather than being
moved alone into the castle's single room. Vacancies, role loss and households continue through
ordinary job and bed reconciliation.

The castle adds one LEADER, one blacksmith, one merchant, two posted crossbow guards,
two posted sword guards, and one jailer. One sword guard watches the royal-suite approach;
the former eastern rooftop guard position belongs to the merchant's planted wooden stall.
The merchant shares the village's existing market and treasury, so trade still requires
a staffed market. The stall barrel is communal storage, separate from jail evidence.
Guards acquire or craft shields through the ordinary equipment system when supplies allow.
The jailer is a GUARD with the JAILER duty and a fixed sword post beside the cell. Castle
sentries use their authored posts and follow castle waypoints when their night routine chooses
a patrol. The town captain continues patrolling the village. Sleep and shift choices reuse
the existing guard routines.

## Identity and appearance

All ten banners receive village heraldry, including original ominous patterns. Every bed
uses a primary or secondary village color; the royal pair shares a color. The white/light-blue
rooftop tent keeps white and uses primary for light blue. The gray/light-gray tent uses
secondary for gray and primary for light gray. These rules apply to wool and carpet. Interior
carpets outside the declared tent areas retain their authored colors.

The merchant stands on clear floor inside the eastern rooftop stall. A small carpet patch
beside the smithy chest is cleared for hand access. Two evidence barrels are placed beside
the barred rooftop cell. These are functional corrections to the edited fort, not a replacement
design. Native checks passed 92 access routes, 32 assigned-bed sleeps, 28 personal-container
deposits and twelve shared-container deposits across all four rotations. Rendered inspection
confirmed the two barrels and preserved castle appearance before deployment.

## Ruler decisions

The existing LEADER occupation supplies the ruler. Its title is King, Queen or Sovereign,
derived from the incumbent's gender. A spouse shares the suite without creating a second
ruling job. The ordinary job system fills a vacant role; routine aptitude swaps and urgent
labor reassignment leave an incumbent ruler in place. Death, departure or losing the job
returns succession to the normal vacancy flow.

The loaded, living castle incumbent provides their identity, persona, virtues and recent
personal memories to the existing decisions about construction, labor, contested jobs,
bank trade, marriage blessing and quartermaster planning. Structured choices and validation
still use their original application paths. Accepted decisions can become the speaker's
personal memories; a rejected proposal is not remembered as a completed action, and a later
successor is not credited with their predecessor's decision.

`VillageBrain` retains inventories, assignments and persistent village state. This feature
changes who deliberates and speaks, not the storage owner. If the ruler is vacant or unloaded,
planning falls back to the collective voice without forcing chunks to load. Player conversation
uses the same chat flow and identifies the actual ruler's role.

## Arrest and release

A village guard hit that would kill a hostile player instead arrests them when a usable,
unoccupied castle cell and safe release spot exist. Sword hits, attributed arrows and recruited
village golems share the same damage boundary. Arrest eligibility comes from the village's
HOSTILE standing; a guard's personal grudge is not enough. If no usable cell exists, ordinary
combat continues. The first implementation supports one prisoner per castle.

A sentence lasts **five minutes** (6,000 overworld game ticks). Server time continues while
a player is offline; a stopped server pauses the clock. The player receives a private arrest
message and notices at four, three, two and one minute remaining. Mining Fatigue III lasts
for the remaining sentence. Their sentence survives logout and world reload.

When the sentence ends, the player is teleported to the safe space immediately outside the
cell, beside their belongings. Escape is allowed: leaving the cell ends custody. The arresting
village's guards stop targeting or damaging the prisoner while held and for 30 seconds after
release or escape. This is not invulnerability to unrelated creatures, players or hazards.
An unavailable or damaged castle cannot silently strand an active prisoner.

## Confiscated belongings

The two evidence barrels are stacked at castle-local `[9,11,19]` and `[9,12,19]`. The release position
is `[8,11,17]`; the cell is `[6,11,18]`. All coordinates rotate and translate with the castle.

Confiscation transfers eligible death-drop items into available barrel space, including normal
inventory, armor, offhand and supported accessory slots. Existing barrel contents are never
erased. Only quantities successfully inserted are removed from the player; anything that does
not fit remains with them. Item components, names, enchantments and container contents remain
on the moved stacks. Items are not automatically returned at release; players retrieve them.

Evidence barrels are neither shared village storage nor personal villager containers. Workers
do not organize or consume their contents. Opening them, taking belongings, or breaking those
barrels is not recorded as theft. Theft detection identifies the menu's actual backing inventory
rather than guessing from nearby village containers.

Accessory handling must preserve inventory integrity when equipment grants additional slots.
Items retained by death rules stay on the player. Optional integrations must remain isolated
when their mod is absent; death events must not be simulated on a living prisoner merely to
collect an inventory list. Native tests with Curios 9.5.1 cover partial/full barrels, retained
overflow, functional and cosmetic slots, equipment callbacks and temporary crafting menus.

The Curios integration respects its configured retention policy, slot and item drop rules,
cosmetic slots and vanilla inventory retention. Equipment that grants accessory slots remains
equipped so removing it cannot eject other items on a later tick. Mods that change inventory
only through death events need separate integration; an arrest does not invoke those events.

An open temporary crafting menu must be able to return its inputs without dropping anything.
Arrest checks this before changing inventory or teleporting. If the remaining backpack cannot
hold those inputs, arrest is unavailable for that hit. Ordinary chest menus do not transfer
the chest's contents, and cursor overflow remains carried by the player.
