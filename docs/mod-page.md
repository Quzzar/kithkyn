# Kithkyn

Villages that build a life of their own. Kithkyn replaces Minecraft’s villagers with people
who gather around campfires, take jobs, build homes and workshops, trade, form families,
and talk with you about their lives. Their village plans its next buildings from real
stockpiles, and its workers carry out the work in the world.

## Features

- **Seventeen regional building styles, all included.** Birch Forest, Desert, Badlands,
  Floodplain, Jungle, Swamp, Mediterranean, Tundra, Alpine Highlands, Japanese Cherry Grove,
  Nautical Coast, Polynesian Coast, Romanian, Savanna Tent, Rustic Woodland, Taiga and Mushroom.
  The jar carries 394 building definitions and 459 templates, including wall families.
- **Growing communities.** Villages build homes, farms, mines, markets and workplaces, with
  the variants and upgrades authored for their region. No extra building datapacks are needed.
- **People with lives.** Names, personalities, inherited appearances and attributes,
  relationships, marriage, children, companions and opinions of you.
- **Physical work and trade.** Builders, farmers, miners, smiths, bakers, guards and other
  workers use real workplaces and stockpiles. Markets price goods in emeralds.
- **Conversation.** Talk with villagers through a local language model, or connect your
  own OpenAI, Claude or DeepSeek account. Rules keep the village working when AI is unavailable.
- **Defence and the undead.** Villages can build walls. Some settlements are undead, and
  undead outposts can replace pillager outposts.

## Install

Minecraft **1.21.1**, **NeoForge 21.1.252 or later in the 21.1 line**, and **Java 21**.
Install the same Kithkyn jar on the server and on every client. Curios is optional.

On the first world or dedicated-server start, Kithkyn downloads the local runtime and its
default Llama 3.2 3B model into the game directory’s `kithkyn/` folder. Allow about **2 GB
of disk space** and **3 GB of extra RAM** beyond the server’s normal heap. An internet
connection is needed for the download; later starts work offline with the verified cache.
The world remains playable while it downloads. Interrupted transfers resume on the next start.
In single-player, the integrated server downloads into that Minecraft installation’s game directory.

New Kithkyn villages and replacement outposts appear in chunks generated after installation.
Previously generated vanilla settlements remain in place.

## Configuration

Edit `config/kithkyn-common.toml`, then restart the game or server:

| Setting | Default | Purpose |
| --- | --- | --- |
| `Generate villages` | `true` | Replace newly generated vanilla villages |
| `Undead villages` | `true` | Allow undead settlements |
| `Undead village chance` | `0.07` | Chance of an undead settlement |
| `Replace pillagers` | `true` | Replace newly generated pillager outposts |
| `Wandering merchant` | `true` | Replace the wandering trader |
| `Enable LLM?` | `true` | Enable AI conversation and decisions |
| `LLM provider` | `local` | Use `local`, `openai`, `claude` or `deepseek` |
| `LLM local model` | `llama-3b` | Choose Llama 3.2 3B or `gemma-2-2b` |

A cloud provider needs your API key and avoids the local model’s RAM use. Keep that config
file private. Set `Enable LLM?` to `false` for rules alone. `/kithkyn status` reports AI
startup, and operators can retry with `/kithkyn load`. Developer commands default to off.

## Current limits

Villager undertakings and quests remain experimental and are not a released gameplay feature.
The mod is a successor to Village Life, with a new mod ID; it does not load Village Life saves.
Keep backups when adding mods to an existing world.

## Credits

Free software under **GPL-3.0-only**. Architectural inspiration: Towns and Towers
(Biban_Auriu, Kubek, Cristelknight, William Wythers), Dungeons and Taverns (Nova_Wostra,
Konci, WhityLee), ChoiceTheorem’s Overhauled Village, and Unstructured (Cristelknight,
Delta_Kaktus, Biban_Auriu). See the [full credits and standing offer to revise designs](https://github.com/Quzzar/kithkyn#credits-and-inspiration).

Built with Llama. [Llama 3.2 Community License](https://www.llama.com/llama3_2/license/).
The alternative Gemma model is subject to the [Gemma Terms of Use](https://ai.google.dev/gemma/terms)
and [Gemma Prohibited Use Policy](https://ai.google.dev/gemma/prohibited_use_policy).
The downloaded runtime is [llama.cpp](https://github.com/ggml-org/llama.cpp), under MIT.

[Website](https://kithkyn.com) · [Source](https://github.com/Quzzar/kithkyn) · [Report an issue](https://github.com/Quzzar/kithkyn/issues)

Not affiliated with Mojang or Microsoft.
