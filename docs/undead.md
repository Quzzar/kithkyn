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

## Where undead villages come from

- **Natural founding** rolls the kind once, from the world seed and the site, with
  `UndeadVillageChance` (default 0.2). The roll is deterministic, so a founding probe and
  the founding it leads to agree, and the site search cannot reroll its way to a preference.
  0 founds only living villages.
- **Manual founding** is living unless asked: `/kithkyn create-village <pos> <style> undead`.
- The kind lives on the village (in the brain's strategy tag beside the style) and on each
  person (synced, saved as `Kind`). Campfire arrivals take the village's kind, children
  take their parents', a wandering merchant takes its home village's, and a village never
  recruits a village-less wanderer of the other kind. A village is one kind for its life.

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
  anyone else and the bake shreds it (`Tatter`): the cloth frays upward from its own hems, so
  trouser ends show bone toes and cuffs show finger bones whatever the garment's cut, two rips
  open the chest along the ribs beneath, every remaining texel is grimed toward old cloth, and
  the texels bordering a hole darken into a torn edge. No wardrobe needs a second set of art,
  and the job stays readable, because the mask removes a bounded share of a garment. The
  rags are cut from the person's appearance seed and the garment, so every client bakes the
  same tears and they never move. Hoods and the head UV are never torn.
- Hurt, death and step sounds are the skeleton's. Speech is the same, because they are
  people.
- The persona sheet and the chat prompt state the kind beside the gender, or the model
  writes them as the living.

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
```

Both mirror the living age lineups ([ui-preview.md](ui-preview.md)) on the other kind; the
screen version adds an armed guard in iron at the end, the skeleton a player actually meets
first. Pass `-Pjoinport=<port>` when a deployed server holds 25565. `/kkdev appearance
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
- `configuration/KithkynConfig`: `UndeadVillageChance`, `UndeadStrangerBaseline`.
