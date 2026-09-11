# Castles

## Desert castle

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

The castle adds one LEADER, one blacksmith, one merchant, two crossbow guards,
two sword guards, and one jailer. Crossbow guards carry backup stone swords and patrol
the first-floor battlements. Sword guards receive a stone sword and shield and patrol the
ground level. Existing better equipment is retained. Each duty has its own `patrol_routes`
entry, transformed with the building; a nearby point on another floor does not count as arrival.
Both sentry groups patrol whenever awake, with their existing 20% chance to sleep each night.
The town captain continues patrolling the village. The jailer uses the GUARD occupation with
JAILER duty and remains at the cell whenever awake, with the same nightly sleep chance.
Combat still interrupts patrols, and rebuilding temporarily suspends access.

The merchant uses the planted wooden stall on the eastern rooftop. This is an additional
place to trade through the village's existing market and treasury, so trade requires a
staffed market. Its barrel is communal storage, separate from jail evidence.

Five general beds cannot house all seven non-ruler castle workers. A village needs at least
two additional general beds elsewhere to fill every castle position. The royal spouse's bed
remains reserved, and the captain moves from their existing town-center assignment.

### Identity and appearance

All ten banners receive village heraldry, including original ominous patterns. Every bed
uses a primary or secondary village color; the royal pair shares a color. The white/light-blue
rooftop tent keeps white and uses primary for light blue. The gray/light-gray tent uses
secondary for gray and primary for light gray. These rules apply to wool and carpet. Interior
carpets outside the declared tent areas retain their authored colors.

The merchant stands on clear floor inside the eastern rooftop stall. A small carpet patch
beside the smithy chest is cleared for hand access. Two evidence barrels are placed beside
the barred rooftop cell. One matching sandstone slab closes a low parapet gap where pathfinding tried an
unreachable shortcut onto target decorations. Native checks passed 92 access routes, 32 assigned-bed sleeps, 28 personal-container
deposits and twelve shared-container deposits across all four rotations. Rendered inspection
confirmed the two barrels and preserved castle appearance before deployment.

## Swamp castle

The Swamp castle is the user-edited SC01.2 Overgrown Stone Fort, derived from the
Towns and Towers Jungle Fort and restyled with spruce, dark oak, mossed stone and ordinary
oak foliage. It belongs to the planned ordinary Swamp village, separate from the playable
Floodplain village for mangrove biomes. Its final gallery capture stands at `10107, 230, 1308`;
the private staged assets live under `run/swamp-integration/castle/`, while
`tools/structure/swamp-castle-20260911.json` records the public metadata and provenance.

This building uses the same castle category, ruler, custody, evidence, housing and guard systems
as the Desert castle. It is a different physical structure: Desert uses R07.2, while Swamp uses
SC01.2. The Swamp definition remains staged until the complete Swamp catalog and style token land.

The two residential wings provide nine general single beds with shared personal barrels.
The upstairs ruler suite has a reserved double bed and its own chest. The castle adds a
leader, baker, blacksmith and jailer. Two sword-and-shield guards cover the entrance, two
crossbow guards cover the opposing balconies, and three crossbow guards cover the roof.
These are ordinary authored jobs and housing assignments, so the wider village still needs
enough residents to fill them.

Each of the seven sentry stations owns a short patrol route on its assigned level. Entrance
guards remain at the gate, balcony guards remain on their respective balconies, and roof guards
cover separate parts of the roof. This prevents the upper guard shifts from crowding the central
ladder while preserving all seven patrols through every building rotation.

The enclosed, multi-block jail room is on the ground floor. Its two evidence barrels sit above the cell door and
remain outside both village storage and personal storage. Custody uses the same five-minute
sentence, item rules, reminders, release behavior and theft exclusions described below.
The eight authored banner positions receive the village banner; all eleven beds receive the
primary or secondary village color, with both ruler beds sharing the primary color.

The roof is reached by the central ladder. Native testing of all rotations exposed two
general navigation problems and fixed them in the shared runtime: a body on a rung now
advances by occupying that exact rung rather than by an unstable fractional-height test,
and personal-container routing avoids stepping over beds when a floor route exists.
The export removed no authored block positions. It only closed the two halves of one spruce
door for the template's starting state and wrote explicit air into otherwise empty interior
cells so terrain cannot fill the rooms.
All 28 sentry runs reached every assigned waypoint across four rotations. The custody fixture
also passed melee and arrow arrest, all 41 ordinary inventory slots, evidence overflow,
five-minute reminders, persistence, escape, damaged-cell release and obstructed-exit fallback.

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

## Verification

The September 10 native playtest ran all four sentries through every waypoint in all four
rotations, including real movement and floor checks. Separate access checks walked between
stations, slept in all beds and deposited into personal and shared containers. The castle
merchant's actual menu completed bulk purchases and sales with conserved goods and emeralds.
The jail checks exercised damage-triggered arrest, evidence capacity, countdown and release.

A real builder collected the exact recipe from two storehouses and completed the castle through
ordinary gathering and construction goals. This exposed and fixed oversized stack truncation
in shared inventory insertion: the 128-cobblestone recipe now retains both stacks. Beds and jobs
were unavailable until the ordinary completion callback published the finished building.
