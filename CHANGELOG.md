# Changelog

All notable changes to Kithkyn. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project uses [semantic versioning](https://semver.org/): patch for fixes, minor for
new content or features, major for anything that breaks saves.

## [Unreleased]

## [1.0.0] - unreleased

First public release, for Minecraft 1.21.1 on NeoForge.

### Added

- Autonomous villages that found themselves, plan and construct their own buildings, raise
  walls, and fill jobs from a campfire pool.
- Seventeen bundled regional building catalogs: birch forest, desert, badlands, floodplain, jungle, swamp,
  Mediterranean plains, tundra, alpine highlands, Japanese cherry grove, nautical coast,
  Polynesian coast, Romanian, Savanna Tent, Rustic Woodland, Taiga, and Mushroom.
  Includes 394 building definitions and 459 templates, including shared wall families.
- Villagers with genetics, a stat block, a composed appearance, a persona, relationships,
  marriage, families and children, and companions.
- Undead villages and undead pillager outposts.
- A market economy with a bank, emerald pricing, and a wandering merchant.
- A language model behind villager conversation and village decisions, run offline through
  llama.cpp by default or against a cloud provider.
- Iron golem guards and allay quartermasters.

### Release preparation

- Pinned, SHA-256-verified and resumable local runtime and model downloads.
- Windows ZIP extraction through Java and provider shutdown cleanup.
- One operator warning per failed AI startup and quieter routine worker/path logs.
- Mod-list logo, credits, update feed source, install guidance and draft distribution copy.
- Java 21 CI artifacts, Windows/Linux packaged-server smoke checks and tagged draft releases.
- Undertakings and quests remain experimental; they are not part of the released feature list.

[Unreleased]: https://github.com/Quzzar/kithkyn/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Quzzar/kithkyn/releases/tag/v1.0.0
