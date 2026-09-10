# Kithkyn

Welcome! This mod introduces AI NPCs into your Minecraft world, adding a dynamic and autonomous village-building experience.

[Website](https://kithkyn.com) · [Source](https://github.com/Quzzar/kithkyn) · [Issues](https://github.com/Quzzar/kithkyn/issues)

Now running on **Minecraft 1.21.1 / NeoForge** (ported from the original [Forge 1.18.2 version](https://github.com/Quzzar/villagelife-legacy)).

Kithkyn replaces Village Life with a new mod ID. Start a new world; old mod saves are not
compatible. See [project identity](docs/project-identity.md) for the naming and compatibility decision.

## Features

- **Autonomous NPCs**: AI villagers ("people") with generated names, genders, personalities, and virtues that shape how they behave.
- **Dynamic Villages**: Villages plan and construct their own buildings, assign jobs and beds, and keep an internal event log.
- **Occupations**: Guards, farmers, lumberjacks, miners, builders, clerics, blacksmiths, and more — each with its own AI goals.
- **Quests and Interaction**: Engage with the villagers, form relationships, and manage guards (work in progress!).

## Development

Requires Java 21 (the Gradle toolchain provisions it automatically). Useful tasks:

```
./gradlew build       # build the mod jar into build/libs/
./gradlew runClient   # launch a dev client
./gradlew runServer   # launch a dev server
```

Debug triggers while testing: placing a **diamond block** founds a new village (instantly builds a town center and spawns its first villager); placing an **emerald block** instant-builds a well with marker blocks for beds/jobs/containers.

## Contributing

We welcome contributions! If you're interested in improving the Kithkyn mod, please feel free to fork the repository, make your changes, and submit a pull request.

---

With the recent developments in generative AI, I plan to revisit this mod.

Kithkyn is not affiliated with Mojang or Microsoft.

## Credits and inspiration

Kithkyn's village design work draws inspiration from the Minecraft building community.
Special thanks to the projects and creators whose structures, architectural styles, and
layouts have helped shape our designs and reference galleries:

- [Towns and Towers](https://www.curseforge.com/minecraft/mc-mods/towns-and-towers), by
  Biban_Auriu, original author Kubek, and maintainer Cristelknight, along with its contributing
  builders, including William Wythers.
- [Dungeons and Taverns](https://www.curseforge.com/minecraft/mc-mods/dungeon-and-taverns),
  by Nova_Wostra, with artist Konci and contributor WhityLee.
- [ChoiceTheorem's Overhauled Village](https://www.curseforge.com/minecraft/mc-mods/choicetheorems-overhauled-village),
  by ChoiceTheorem and contributors.
- [Unstructured](https://www.curseforge.com/minecraft/mc-mods/unstructured), by
  Cristelknight, Delta_Kaktus, and Biban_Auriu.

Thank you for the creativity and care you bring to Minecraft. If you're one of these creators
and would like us to alter our designs further or update how your work is credited, please
[reach out through our issue tracker](https://github.com/Quzzar/kithkyn/issues).
We're happy to listen and work with you on changes.
