# Undead people

**Decided 2026-09-11 ([#106](https://github.com/Quzzar/kithkyn/issues/106)).** Some villages
are undead. Their people are people in every respect the simulation cares about: they hold
jobs, marry, raise children, keep pets, petition the brain and gossip. What sets them apart
is one number, and the look that number comes with. Skeletons today; a zombie is a second
undead look waiting to be authored, and nothing below needs renaming for it.

## The one switch: where a stranger starts

Hostility toward a player was already derived entirely from opinion. Each fighter treats a
player as a personal enemy once their own opinion falls to the grudge line, and the whole
village climbs the standing ladder as the average of its residents' opinions moves
([economy.md](economy.md), [relationships.md](relationships.md)). The only hard-coded piece
was that every villager started every stranger at 0.

That 0 is now the person's **stranger baseline**, read from their `Kind`:

| Kind | Baseline | Default |
| --- | --- | --- |
| living | indifference | 0 |
| undead | a grudge on sight | `UndeadStrangerBaseline`, -40 |

The baseline is what an absent opinion entry means; it is never stored, which keeps the
standing rule of derived-not-stored intact and leaves every older save living. Two places
read it: `OpinionService`, wherever an opinion of someone outside the village is looked up
or first written, and the outsider fade in `RelationshipDrift`, which now creeps toward the
baseline rather than toward 0.

### What -40 does, with no new behaviour code

- It is past the grudge line (-30), so every undead fighter attacks a player on sight and
  every unarmed undead keeps their distance. That is personal, per villager.
- The village average sits at -40, which is **unwelcome** (market closed) but not shunned
  (chat still opens) and not hostile (no village-wide verdict, no castle custody).
- Improving standing works exactly as it does with the living, through the same opinion
  calls, just from a lower start. One good judgement, capped at +15, lifts one undead
  villager over the grudge line. That one stops attacking or fleeing. The rest of the
  village does not care what their neighbour thinks, so a village is befriended one person
  at a time, and the market opens when enough of them have come round.
- Goodwill fades toward the baseline at the same pace living goodwill fades toward
  neutral (one point per drift pass). A friendship among the undead has to be tended, or it
  sinks back to the grudge they hold every stranger in.

The baseline was chosen deliberately shallow. At -80 the village would sit on the hostile
rung and every villager would need four judgements to cross the grudge line, so nobody
would ever be the first friend. At -40 there is always a next skeleton to win.

## The three switches

Villages, the wandering merchant and pillagers each have one replace-or-vanilla switch in
the common config, and all three have the same shape: on means ours in place of Minecraft's,
off means Minecraft's as usual and none of ours.

| Switch | On | Off |
| --- | --- | --- |
| Generate villages | our villages are founded, no vanilla village generates | vanilla villages, none of ours |
| Wandering merchant | a merchant from one of our villages | the vanilla trader |
| Replace pillagers | no outposts, no patrols, raids are undead war parties | outposts, patrols and pillager raids as usual; the undead never raid |

The village and pillager switches work through two built-in data packs (`ReplacementPacks`),
each emptying one vanilla structure set, offered to the world only while its switch is on. A
file in the mod's own data folder would override vanilla unconditionally, which is exactly what
off must not do. Patrols are refused at spawn, and the vanilla Raid Omen is refused when it
would apply, so no pillager raid can start while the undead stand in for them.

## Where undead villages come from

- **Natural founding** rolls the kind once, from the world seed and the site, when
  `Undead villages` is on, with `UndeadVillageChance` (default 0.07). The roll is
  deterministic, so a founding probe and the founding it leads to agree, and the site search
  cannot reroll its way to a preference. Off founds only living villages.
- **Manual founding** is living unless asked: `/kithkyn create-village <pos> <style> undead`.
- The kind lives on the village (in the brain's strategy tag beside the style) and on each
  person (synced, saved as `Kind`). Campfire arrivals take the village's kind, children
  take their parents', a wandering merchant takes its home village's, and a village never
  recruits a village-less wanderer of the other kind, whether roaming nearby or on the road
  beyond the horizon. A village is one kind for its life, until it falls (below).

## The dead return

An undead village does not take strangers from nowhere while there are dead to raise. Every
living villager who dies is written whole into the **register of the dead** (`Graveyard`),
the way the road keeps a wanderer: their entire record, with when, where and how it ended.
Raiders are never buried, they were never alive, and the undead dying again are gone for good.
The register is one list for the whole server, bounded by `Register of the dead cap`.

When an undead village's campfire loop calls for an arrival and nobody of its kind is roaming
nearby or on the road, it raises someone from the register: **its own dead first, then the
nearest, then the most recent.** The risen come back at the village's edge and walk in like
anyone else. Only with the register empty is one of the long dead conjured, the way a living
village conjures a newcomer, so an undead village founded in a fresh world still fills.

What rises is the same person. `Raising` says exactly what of a record survives: name, gender,
genes, stats, virtues, personality, age, parents, and every attachment, which is where the
persona, the personal log, the chat history and the opinions of players live. So the dead
remember, and a villager who liked you in life likes you still, whatever the undead baseline
says about strangers. What stays in the grave is the old life, village, job, title, marriage,
camp, and the pack and clothes they dropped where they fell, and the body's moment, its wounds
and effects. The risen gain one memory, of how it ended and where they rose, and are undead from
then on.

## The look

The look goes through the skin compositor ([appearance.md](appearance.md)), not a new model
or renderer:

- `AppearanceInputs` carries the kind, and every catalog asset declares one (living unless
  it says otherwise). Skin, hair and both eyes come from assets of the person's kind.
  Clothing is shared: an undead smith wears the apron, an undead guard shows their iron.
- The `skeleton` pack is the provided transparency skeleton, taken into the Skin Splice Lab as a
  source-backed pack. Its limbs are two-texel bones with the rest of each face left clear, so the
  player model reads as thin bone, and the holes stay holes: an undead pack is exempt from the
  lab's base-completeness and underpaint checks. Its ribcage was drawn on the jacket overlay
  above an empty torso; the pack moves it onto the base torso, texel for texel, so a garment
  covers it the way it covers anyone's chest instead of the bones floating outside the shirt.
  The sockets become the eye pair. Nothing else is repainted, and it has no hair layer and no
  garment of its own.
- **Undead parts carry no pigment.** Bone is bone whatever the genes say, so an undead asset
  that declared pigment texels would be painted flesh-coloured; the catalog refuses it, and
  refuses a living asset without pigment, as before.
- Every undead body is slim, so an undead skin must be authored slim.
- Heterochromia is an iris condition. A skull has none, so for the undead it is carried in
  the genes, passed on, and expressed nowhere.
- A skull has no lids either. The sleeping face bake ([appearance.md](appearance.md)) is
  for the living; a sleeping undead keeps its sockets, and the recipe audit refuses an undead
  recipe with its eyes closed.
- **The wardrobe is worn in rags.** An undead person takes the same occupation garment as
  anyone else and the bake ruins it (`Tatter`, decided at the heaviest of three cuts rendered
  side by side): hems fray four rows deep, most sleeves and trouser legs are torn short so
  forearms and shins are bone, four wide rips open the chest and back onto the ribs, slashes
  cross the limbs, every remaining texel is grimed toward old cloth, and the texels bordering
  a hole darken into a torn edge. No wardrobe needs a second set of art. The garment's colour
  and collar survive, so a village still reads as dressed, but the job is told by held tools
  and guard armour more than by cloth. The rags are cut from the person's appearance seed
  and the garment, so every client bakes the same tears and they never move. Hoods and the
  head UV are never torn.
- Hurt, death and step sounds are the skeleton's. Speech is the same, because they are
  people.
- The persona sheet and the chat prompt state the kind beside the gender, or the model
  writes them as the living.

## Raids

Vanilla raids never could target our villages, because a pillager raid homes in on vanilla
villagers. What ships instead is the raid the world was missing: the dead of an undead village
marching on a living one, in waves, until they are beaten or give up.

**Two omens start one, and both are the player's doing.**

- **The grudge omen.** A player whose standing with an undead village has fallen to
  `UndeadRaidStandingBelow` (default -60, between shunned and hostile, so it takes real offences
  seen by its people) carries that village's grudge. The next living village they stand in is
  raided by its dead, and the undead village waits `UndeadRaidCooldownDays` before following the
  same player again. Creative and spectator players carry nothing.
- **The bottle.** An ominous bottle keeps its purpose. Vanilla turns Bad Omen into Raid Omen the
  moment the drinker stands in a village; the pillager switch refuses that Raid Omen and the
  undead come instead, named for the nearest undead village or, with none in the world, as the
  restless dead.

**What a raid is.** Half a minute of warning, with a boss bar and the raid horn, then waves rise
at the village's edge and walk in: two waves on easy, three on normal, four on hard, none in
peace, each wave sized to the village (a hamlet meets a handful, a town a band) and one larger
than the last. A wave that will not die is reinforced after three minutes; the dead give up
twelve minutes after the first wave and withdraw. Raiders are undead people in rags, named,
armed from a seeded kit (a third shoot, iron and chain from the third wave), with no village, no
bed and nothing to say. The raid lives in the village's strategy tag and survives a restart with
its raiders still in the world.

**Who fights.** Raiders attack players, the living, and golems. Guards and anyone who fights
treat a raider as they treat a monster, unarmed villagers keep their distance, and guard golems
count a raider a threat before it has picked anyone out. Killing a raider is defence: no murder
is reported, nobody mourns them, and a neighbour who cuts one down that was coming for you is
remembered for it the way a slain creeper is.

**Aftermath is memory, not mechanics.** Residents remember who brought the dead down on them and,
when the raid is beaten, every player who cut one of the dead down; reflection decides what
that was worth. Deaths among the residents go through the village's books like any other.

**The village can fall.** If every resident dies while the dead are at the gate, the raid ends
as the village falling: it turns undead, the raiders go back to the dark, and the ordinary
arrival loop, forced below the population floor, raises its own dead first to fill it again.
The name, the buildings, the stores and the golems stay. A village that had nobody when the
dead set out cannot fall; it is only ever lost, not emptied.

`/kkdev raid start [now]`, `stop` and `status` drive one by hand; `preview` is the harness hook.

## What stays the same, on purpose

- Vanilla monsters get a target-people goal at spawn and keep it; wild zombies and skeletons
  attack undead villagers like anyone else, and undead guards cut down wild skeletons.
- No sunlight burning, no inverted healing, no undead damage rules. They are not mobs.
- Marriage, births, families, pets, requests, and every worker loop.

## Seeing it

```bash
./gradlew runClientJoinLocal -Puipreview=undead-lineup
./gradlew runClientJoinLocal -Puipreview=undead-lineup-asleep
./gradlew runClientJoinLocal -Puipreview=undead-lineup-world
./gradlew runClientJoinLocal -Puipreview=undead-raid
```

Both mirror the living age lineups ([ui-preview.md](ui-preview.md)) on the other kind; the
screen version dresses the adult as a farmer, so the rags read against a dark tunic, and adds
an armed guard in leather at the end, the skeleton a player actually meets first. `undead-raid` stands the preview player over the nearest living village and photographs the first
wave walking in. Pass `-Pjoinport=<port>` when a deployed server holds 25565. `/kkdev appearance
show` prints the kind, and `/kkdev appearance audit` runs the recipe matrix for both kinds.

## Code map

- `entities/Kind`: the enum, the baseline, the founding roll.
- `entities/Person`: the synced and saved kind, sounds.
- `village/Village`, `village/VillageGeneration`, `savedata/VillageManagerSaveData`,
  `events/KithkynCommands`: the village's kind, its founding roll, the command.
- `village/Village.confirmArrival`, `relationships/ChildCreationService`,
  `village/WanderingMerchantSpawner`: the spawn hooks.
- `relationships/OpinionService`, `relationships/RelationshipDrift`: the baseline in use.
- `appearance/*`, `client/appearance/PersonAppearanceTextures`: the kind-filtered recipe.
- `client/gui/AgeLineupScreen`, `client/gui/UiPreview`: the lineups.
- `raids/UndeadRaid`, `raids/UndeadRaids`, `raids/UndeadRaidPlan`, `raids/RaidCommands`: the
  raid, its omens, its arithmetic, and the dev commands. `entities/ai/goals/RaidMarchGoal` walks
  a raider in.
- `village/Graveyard`, `village/Raising`: the register of the dead and what of a record rises.
  `village/WandererPool` keeps the road's kinds apart.
- `worldgen/ReplacementPacks`: the two data packs behind the village and pillager switches.
- `configuration/KithkynConfig`: `UndeadVillages`, `UndeadVillageChance`, `ReplacePillagers`,
  `UndeadStrangerBaseline`, `UndeadRaidStandingBelow`, `UndeadRaidCooldownDays`.
