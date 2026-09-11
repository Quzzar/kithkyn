# Kithkyn

Kithkyn models people whose family, housing, and work status remain legible as a settlement changes around them.

## Language

**Age stage**:
One of the four social and physical phases of a person’s life: Toddler, Kid, Teenager, or Adult.
_Avoid_: Baby flag, child size

**Biome trait**:
An environmental property, such as Cold, Wet, Arid, Coastal, or Wooded, inferred from the founding site and used for survival, work, and placement rules independently of architecture.
_Avoid_: Village biome, culture

**Building category**:
The functional role shared by structures that answer the same village need, such as house, fishery, church, or watchtower.
_Avoid_: Building type

**Building level**:
The development stage of one building category within one village biome. Building levels are independent of the village’s camp-to-city progression.
_Avoid_: Building tier

**Building variant**:
The authored structure that realizes a building category for one village biome. It may have a different footprint and shape, not merely a substituted block palette.
_Avoid_: Material swap

**Dependent housing**:
A pre-adult person’s place in a resident parent’s household. It is valid nighttime accommodation but does not reserve or consume a bed.
_Avoid_: Bedless, free housing

**Kind**:
Whether a person, and the village they belong to, is living or undead. Fixed at founding and inherited; every arrival takes the village's kind.
_Avoid_: Race, species, mob type

**Independent housing**:
A bed assigned to one adult, either in general housing or at their own live-in workplace.
_Avoid_: Home ownership

**Raider**:
One of the dead sent against a living village: an undead person with no village, no bed and nothing to say, who exists only for the raid and is defence, not murder, to kill.
_Avoid_: Pillager, mob, monster

**Grudge omen**:
What a player carries once their standing with an undead village has fallen far enough: the next living village they stand in is raided by its dead. The ominous bottle is the other omen.
_Avoid_: Bad Omen, raid trigger

**Reference family**:
A coherent collection of gallery structures used as architectural evidence while authoring one village biome. Its source name never becomes a runtime village property.
_Avoid_: Culture, runtime style axis

**Stranger baseline**:
The opinion a person holds of anyone they have never met, read from their kind: indifference for the living, a grudge on sight for the undead. Never stored; it is what an absent opinion means and where an outsider's opinion fades back to.
_Avoid_: Default reputation, starting standing

**Teenager**:
The final pre-adult age stage. A teenager may hold a normal occupation while dependently housed, but needs independent housing upon becoming an Adult.
_Avoid_: Apprentice, young adult

**Wanderer**:
A work-eligible person who belongs to a village’s idle labor pool, or a village-less person traveling between settlements. Toddler and Kid are not Wanderers even when their stored occupation is idle.
_Avoid_: Nitwit, unemployed child

**Undead**:
The kind whose people start every stranger past the grudge line. People in every other respect: jobs, marriage, children, pets. Skeletons today; a zombie would be a second undead look, not a third kind.
_Avoid_: Skeleton villager, hostile mob, monster

**Village banner**:
The persistent banner design representing one village, composed from its village colors and Minecraft banner-pattern layers.
_Avoid_: Regional banner

**Village colors**:
The ordered primary and secondary Minecraft dye colors belonging to one village.
_Avoid_: Regional palette

**Village biome**:
A named architectural catalog, such as Birch Forest or Snowy Taiga, selected from a village’s founding environment and retained for its life. Multiple actual registry biomes may resolve to one village biome, but distinct complete reference families are not pooled into the same catalog.
_Avoid_: Biome group, culture, wood type

**Village identity**:
The persistent visual identity shared across one village’s structures, comprising its village colors and village banner independently of its village biome.
_Avoid_: Village style
