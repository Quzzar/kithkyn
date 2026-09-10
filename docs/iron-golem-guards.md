# Iron and snow golem guards

A village can recruit an existing iron or snow golem as an auxiliary defender. It does not create a new
golem, spend resources, require a watchtower vacancy, or take a person's job away. The bond is to
the village, not the recruiting guard, so it survives that guard changing jobs or dying.

## Recruitment

Every active human guard checks a small encounter area once every five seconds, staggered by
entity. A friendly, visible golem within eight blocks can join when both are near the guard's own
settlement. Guards do not recruit during combat. Angry, fighting, leashed, mounted, passenger-carrying
and AI-disabled golems are left alone. The claim is committed synchronously before another guard
can attempt to recruit the same golem. Another village cannot take an already adopted golem.

There is no per-village or per-guard golem count cap. Each scan can adopt one eligible golem;
the same guard can keep adopting different golems on later scans. Membership is keyed by entity
UUID, so recording another golem adds a defender rather than replacing the previous one.

An unnamed golem receives a locally selected name, which the guard announces and remembers.
An existing custom name is preserved. Persona-driven model naming is not enabled in this pass.
Names appear only when the player aims at the golem. Adoption and reload clear the old
always-visible nameplate flag, including on existing recruited golems.

## Membership and behavior

- Golems remain their vanilla entity species. Iron golems retain their damage, combat, health,
  repair with iron ingots and player-created flag, including protection against attacking players.
  Snow golems retain ranged attacks, their four health points and their pumpkin/shearing state.
- They share the human guards' building-lap patrol goal, day and night. Their vanilla-village
  wandering and reputation-based defense goals are replaced with Kithkyn-village behavior.
- Patrol choices remain independently random, with no coverage reservations or coordination.
  Each species supplies its own strolling multiplier to the shared patrol: humans retain 0.45,
  while iron golems use vanilla's 0.60 and snow golems use 1.00. Navigation applies that multiplier to the individual
  entity's movement-speed attribute; adoption does not overwrite its attributes or match its
  recruiting guard's speed. Vanilla combat movement is unchanged.
- Adopted snow-golem snowballs deal at least two damage (one heart before armor) to hostiles
  or the golem's selected combat target. Blazes retain their vanilla three damage. Player-thrown
  snowballs, ordinary unadopted snow golems and accidental hits on neutral livestock do not gain
  damage. Same-village people, recruited golems and companion pets take neither damage nor the
  zero-damage retaliation/knockback effects from a recruited snow golem's snowballs.
- They assist nearby guards and intervene against attackers of nearby residents or village pets.
  Same-village residents, golems and companion pets cannot acquire one another as targets through
  the golem interaction. Vanilla hostile targeting continues to work.
- They never join the human population list, take a bed, consume food, fill a workplace, or count
  toward settlement population tiers. Human guard posts remain available to humans.
- A separate saved roster records each golem's UUID, name and last-known chunk. Its location gets
  the same chunk-loading bubble as residents when the village's existing loading mode permits it.
  Unloading does not mean death. Entity NBT stores the village and recruiting guard backlinks.
- Reloading reattaches goals idempotently. Death or explicit entity destruction removes the roster
  entry; the golem is not silently replaced or respawned.

These villages currently belong to the overworld. A golem taken to another dimension retains its
membership but does not patrol a same-coordinate imitation of its home there.

Recruitment explicitly supports `IronGolem` and `SnowGolem`, including subclasses, not arbitrary
`AbstractGolem` types or mobs whose names contain "golem". Allays follow the same roster and lifecycle
as keepers of the stores rather than defenders; see [allay-quartermasters.md](allay-quartermasters.md). Both use the same roster and lifecycle;
no save migration is needed to add snow members to an existing village.

Snow golems remain environmentally fragile: water and rain hurt them, hot biomes marked for snow
golem melting still melt them, and their ordinary snow trails follow the mob-griefing rules.
Adoption does not make them suitable for every climate or give them an iron golem's durability.

## Verification

`GolemRosterTest` covers independent legacy-save defaults, UUID deduplication, save/load and the
separation from human population, beds and jobs. `GolemVerification`, enabled only with
`-Dkithkyn.golems.verify=true` on a disposable local server, exercises real recruitment, eligibility,
duplicate claims, entity reload, vanilla protections, nighttime movement, defense and death.
It also checks the installed patrol through navigation and movement control at normal and altered
movement attributes, preserves human patrol pace, and verifies eight distinct recruits by one
guard, multi-golem save/load, and removal of only the dead member. Snow coverage includes natural
adoption, generated and preserved names, mixed-species roster persistence, pumpkin and health
preservation, night patrol at native speed, real vanilla snowball collision/damage, friendly-hit
protection, ordinary snowball isolation, and actual ranged defense of a resident.
Never enable that verification flag on a review or play world: it creates its own fixture and
stops the test server when finished.
