# Kithkyn

Autonomous villagers who build and run their own villages. Kithkyn replaces Minecraft's
villagers with people: each one has a name, a personality, a family, a job, and a small
language model behind their conversation and their village's decisions. Villages plan their
own buildings, raise walls, fill jobs, trade, marry, have children, and sometimes fall.

[Website](https://kithkyn.com) · [Source](https://github.com/Quzzar/kithkyn) · [Issues](https://github.com/Quzzar/kithkyn/issues)

A NeoForge mod for **Minecraft 1.21.1**. Kithkyn is the successor to Village Life
([Forge 1.18.2](https://github.com/Quzzar/villagelife-legacy)); it is a new mod ID and does
not load old Village Life worlds.

## What it does

- **Villages that grow.** A village founds itself with a centre and a campfire, then plans,
  saves for, and constructs its own buildings from a catalog of thirteen biome styles:
  birch forest, desert, badlands, floodplain, jungle, swamp, Mediterranean plains, tundra,
  alpine highlands, Japanese cherry grove, nautical coast, Polynesian coast, and Romanian,
  each with its own houses, farms, workshops, market, and wall.
- **People, not villagers.** Every person carries genetics, a stat block, an appearance
  composed from inherited features, a persona, and opinions of everyone they know. They
  take jobs from the campfire pool: builder, farmer, lumberjack, miner, guard, cleric,
  blacksmith, quartermaster, baker, butcher, fisher, hunter, innkeeper, and more.
- **Lives.** Villagers talk to each other, grow close, marry, move into a cottage the
  village saves for, raise children through four life stages, keep a dog or a cat, and
  hold grudges.
- **Undead villages.** A few villages are raised by the dead. They are people in every
  respect but one, hostile to strangers on sight, and can be befriended one person at a
  time. Pillager outposts are replaced by the undead too.
- **An economy.** A market with real prices, emeralds that mean something, a bank, and a
  wandering merchant in place of the wandering trader.
- **Talk to them.** Walk up to anyone and have a conversation. What they say comes from a
  language model that knows who they are, what their village needs, and what you have
  done for or to them.

## Requirements

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.0 or newer |
| Java | 21 (the Minecraft launcher and the NeoForge installer provide it) |
| Sides | Both. Install on the server and on every client. |
| Disk | About 2 GB for the offline language model, downloaded once |
| Memory | Roughly 3 GB on top of the server's usual heap for the offline model, or nothing extra with a cloud provider |

## Install

1. Install NeoForge for 1.21.1.
2. Put `kithkyn-<version>.jar` in the `mods` folder, on the server and on each client.
3. Start the game. On the first start of any world or server, Kithkyn downloads its
   language model (about 2 GB) into `kithkyn/` inside the game directory. This happens once
   and needs an internet connection; later starts are offline. Villagers fall back to
   rule-based behaviour until the model is ready, so the world is playable meanwhile.

Kithkyn's villages generate in place of Minecraft's in chunks created after the mod is
installed. Villages already generated in an existing world are left as they are.

## The language model

Villagers converse and villages make their big decisions through a small language model.
The rules decide what the legal options are; the model picks among them and says why, in
character. If the model is slow, absent, or gives a bad answer, the rules' own best option
stands in, so a village never stalls waiting for it.

By default the model runs **offline on your machine** through llama.cpp, in a separate
process outside the game's memory. Nothing to install and no account. Two models are
supported, chosen in the config: Llama 3.2 3B (default) and Gemma 2 2B.

If you would rather not spend the RAM, set `LLM provider` in `config/kithkyn-common.toml`
to `claude`, `openai`, or `deepseek` and paste your API key. Keep that file to yourself.
Set `Enable LLM?` to `false` to turn the model off entirely; villagers then run on rules
alone and stop talking in prose.

## Configuration

Settings live in `config/kithkyn-common.toml` and `config/kithkyn-advanced.toml`, each
key with a comment. The ones most people change:

| Key | Default | What it does |
| --- | --- | --- |
| `Generate villages` | `true` | Kithkyn villages replace Minecraft's |
| `Undead villages` / `Undead village chance` | `true` / `0.07` | Whether some villages are undead, and how often |
| `Replace pillagers` | `true` | The undead replace pillager outposts |
| `Wandering merchant` | `true` | A wandering merchant replaces the wandering trader |
| `Village loading` | `HYBRID` | Whether villages keep running when no player is near |
| `Enable LLM?` / `LLM provider` / `LLM local model` | `true` / `local` / `llama-3b` | The language model, as above |
| `Developer commands` | `false` | Registers the `/kkdev` developer tree |

## Commands

`/kithkyn` is the player-facing command: found a village at a position in a chosen style,
inspect a village's plan, treasury, and residents, and trigger raids. The world-changing
branches need operator permission. `/kkdev` holds developer tooling and is
off unless `Developer commands` is enabled.

## Compatibility

- Works alongside other structure and world-generation mods; Kithkyn only replaces the
  vanilla village and pillager outpost placements.
- [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) is optional. When a
  Curios-backed mod is installed, villagers notice and use accessory slots.
- Modpacks: item values for the market are data-driven; see [docs/economy.md](docs/economy.md).

## Development

Java 21 and Gradle, with the toolchain provisioned automatically.

```
./gradlew build       # build the mod jar into build/libs/
./gradlew check       # build, unit tests, and the structure template audit
./gradlew runClient   # launch a dev client
./gradlew runServer   # launch a dev server
```

The design docs under [docs/](docs/README.md) describe the intended behaviour of every
system; read the relevant one before changing code in its area. The release process and the
current release checklist are in [docs/release.md](docs/release.md).

## Contributing

Issues and pull requests are welcome at the
[issue tracker](https://github.com/Quzzar/kithkyn/issues). Fork, branch, and open a PR; CI
builds every push.

## License

Kithkyn is free software under the [GNU GPL v3](LICENSE). It is free to play and free to
include in modpacks, and it will never be sold.

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

**Models.** Built with Llama. The default offline model is
[Llama 3.2 3B Instruct](https://www.llama.com/llama3_2/license/), used under the Llama 3.2
Community License. The alternative is [Gemma 2 2B](https://ai.google.dev/gemma/terms),
used under the Gemma Terms of Use. The offline runtime is
[llama.cpp](https://github.com/ggml-org/llama.cpp) (MIT). Kithkyn downloads these on first
start rather than shipping them.
