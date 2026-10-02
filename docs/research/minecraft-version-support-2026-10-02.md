# Research: Minecraft version support

Research checked on 2026-10-02 against first-party release metadata, NeoForge documentation
and this repository. This is a recommendation for release planning, not an announcement of
additional supported versions. No Minecraft, loader or build versions were changed by this
research.

## Recommendation

Ship the first release for **Minecraft 1.21.1, NeoForge and Java 21**. Keep one supported
Minecraft line while we establish real player and modpack demand. Before shipping, validate
**NeoForge 21.1.252** instead of the old 21.1.72 pin: the older loader predates a documented
server-crash mitigation. Minecraft 1.21.1 remains a current backport line, so staying on this
game version does not require staying on its old loader build.

After release, evaluate **26.1.2 and 26.2** for one additional supported line. Start with
26.1.2 as the port investigation baseline, then compare actual requested packs against 26.2.
Do not support every intervening 1.21.x release, chase snapshots or promise a Fabric/Forge
port without evidence of demand and a measured maintenance cost. The reasons and limits of
this recommendation are below.

## What the current sources establish

The official Maven metadata was read directly on 2026-10-02. Its `lastUpdated` value was
`20261002104605`. These are the highest published versions observed in the relevant lines,
not a formal long-term-support schedule. [NeoForge Maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)

| Minecraft target | Latest observed NeoForge | Status of this candidate |
| --- | --- | --- |
| 1.21.1 | 21.1.252 | Stable artifact; recommended first-release validation target |
| 1.21.11 | 21.11.45 | Stable artifact; no demonstrated Kithkyn demand collected |
| 26.1.2 | 26.1.2.112 | Stable artifact; first next-version candidate to investigate |
| 26.2 | 26.2.0.88 | Stable artifact; compare with 26.1.2 using pack demand |
| 26.3 | 26.3.0.41-beta | Latest vanilla release, but loader API is still marked beta |

Mojang's manifest identifies **26.3** as the latest release and **26.4-snapshot-2** as the
latest snapshot. The 26.3 release date is 2026-09-15. The new numbering scheme means newer
Minecraft releases are not simply a prospective 1.22. [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json),
[Minecraft 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)

NeoForge's 1.21.1 branch received a backport on 2026-09-26 and still sets `java_version=21`,
`minecraft_version=1.21.1` and `fancy_mod_loader_version=4.0.44`. This is direct evidence of
recent maintenance, not a guarantee of a future support duration.
[Latest 1.21.1 backport](https://github.com/neoforged/NeoForge/commit/61045a61fca76876999281676a9e794688390a9e),
[1.21.1 build properties](https://github.com/neoforged/NeoForge/blob/1.21.1/gradle.properties)

NeoForge reported 1.21.1 as its most popular version in its **2025 retrospective, published
2026-01-01**, with more than 16,000 mods. That is historical ecosystem evidence. It is not
October 2026 player market share, nor does a mod count identify our audience. A NeoForge
maintainer predicted that 26.1 would become the next modding anchor, explicitly describing
this as his own educated guess rather than a team policy. No current player-share estimate
was established during this research. [2025 retrospective](https://neoforged.net/news/2025-retrospection/),
[26.1 release post](https://neoforged.net/news/26.1release/)

Curios, our optional integration, publishes stable artifacts for 26.1.2 and 26.2, while its
26.3 artifacts observed here are beta. Availability is a prerequisite; it does not prove
our integration works unchanged. [Curios Maven metadata](https://maven.theillusivec4.top/top/theillusivec4/curios/curios-neoforge/maven-metadata.xml)

## Immediate loader and metadata action

NeoForge's 2026-05-11 advisory identifies **21.1.229** as the first 1.21.1 build fixing
malicious network-packet collection allocation that can crash a server. This security floor
is separate from Kithkyn's minimum tested loader version. The official cumulative changelog
confirms that fix and subsequent loader, registry, save and slot corrections.
[Network advisory](https://neoforged.net/news/mitigating-vulnerabilities-network/),
[21.1.252 changelog](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.252/neoforge-21.1.252-changelog.txt)

At the start of this investigation, [gradle.properties](../../gradle.properties) pinned
21.1.72, admitted Minecraft `[1.21.1, 1.22)` and admitted NeoForge `[21.1.0,)`. Those broad
ranges are not evidence of compatibility with later Minecraft releases. NeoForge documents
these as compatibility ranges, with the build's Minecraft and NeoForge versions required to
agree. Recommend exact Minecraft `[1.21.1]`; use the validated patched loader as the
NeoForge minimum and restrict that range to the 21.1 line. If we want to advertise 21.1.229
as a lower compatibility floor, test that build separately. [NeoForge mod metadata](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/)

21.1.252 still uses Java 21. Its upstream build uses ModDevGradle 2.0.115, but this alone
does not establish that our ModDevGradle 1.0.21 must change; resolve and test the actual
artifact before changing the plugin or wrapper. Repeat the installable-jar checks, clean
client/server installation, Curios-present pass, save reload and AI startup/shutdown against
the new pin. Existing 21.1.72 receipts do not validate 21.1.252.
[Upstream build properties](https://github.com/neoforged/NeoForge/blob/1.21.1/gradle.properties),
[Repository release checks](../release.md)

## Port cost specific to Kithkyn

This is a substantive source port, not a wider version range around the existing jar.

- **Villager rendering and appearance:** 1.21.2 changed entity models/renderers to render
  states. Audit our custom villager models, skin compositor, equipment layers and sleeping
  presentation. [1.21.2 migration primer](https://docs.neoforged.net/primer/docs/1.21.2/#entity-render-states)
- **Persistence:** 1.21.6 moved entity save methods from `CompoundTag` to `ValueInput` and
  `ValueOutput`. Our `Person` entity and village state need migration and retained-data
  tests. [1.21.6 migration primer](https://docs.neoforged.net/primer/docs/1.21.6/),
  [Person source](../../src/main/java/com/quzzar/kithkyn/entities/Person.java)
- **Toolchain and UI:** 26.1 requires Java 25 and Gradle 9.1 or newer with an updated
  mod-development plugin. GUI rendering changed to extraction, and item-stack creation now
  depends on loaded registries. Audit chat/trade screens, model previews, datapack codecs,
  inventory initialization and test fixtures. [26.1 NeoForge migration guidance](https://neoforged.net/news/26.1release/)
- **World layout:** 26.1 changed dimension folders and namespaced saved data. Verify that
  the village registry, block ownership, resident references and chunk tickets survive an
  upgraded copy of a 1.21.1 world. Compilation cannot establish this.
  [Minecraft 26.1 world-storage changes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1),
  [VillageManagerSaveData](../../src/main/java/com/quzzar/kithkyn/savedata/VillageManagerSaveData.java)
- **New biome and behavior decisions:** 26.3 adds Dappled Forest and changes persistent
  mobs' wandering when players are absent. Our existing biome coverage and unattended
  simulation must be checked explicitly on a future port; current 17-catalog coverage does
  not mean every future biome has a dedicated catalog.
  [Minecraft 26.3 changes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)

These are risk areas inferred by comparing our source with the upstream changes. No
26.x Kithkyn build or runtime compatibility result was produced during this research.

## Process for selecting and maintaining another version

1. **Record real demand.** Use an issue for each candidate, listing requested Minecraft
   version, loader, actual modpacks, pack authors and the reasons players cannot use the
   released line. Compare download and support-request trends after launch. Do not confuse
   latest vanilla, loader availability or total mod counts with demand for Kithkyn.
2. **Check a small ecosystem list.** Record exact supported files for Curios and the mods
   in the requested packs. Include performance, world-generation and storage interactions.
   Use official project files/APIs. Reject unsupported or unstable dependencies unless we
   deliberately accept the maintenance cost. Curios remains optional.
3. **Run a bounded port investigation.** Keep the known-working 1.21.1 workspace available
   and make an isolated candidate port. Measure compile failures and real differences in
   rendering, persistence, networking, worldgen and AI goals. Estimate ongoing fixes from
   this diff, not from how small a version-number change looks. NeoForge itself recommends
   comparing old/new vanilla usages in parallel workspaces.
   [Port investigation guidance](https://neoforged.net/news/26.1release/#finding-out-what-needs-to-be-migrated)
4. **Require the release matrix.** Each supported Minecraft line must build and audit its
   own jar, pass Java tests and all 68 catalog/rotation founding cases, and pass real
   Windows/Linux server installation with and without Curios. Add normal-launcher client
   checks, screenshots of chat/trade and villager rendering, local model first boot and
   shutdown, and fresh-world plus saved-world reload. Carry these checks into CI rather
   than assuming a pass on one line covers another. [Existing validation contract](../release.md)
5. **Prove migration before claiming it.** Preserve a 1.0.0 fixture with buildings,
   inventories, families, relationships, ownership and village identities. Upgrade a copy,
   assert retained state, play and reload it. The existing 1.x save promise remains binding;
   a port must satisfy it or explicitly revise the release policy before publication.
   Do not infer downgrade support from forward migration.
6. **Choose one additional line.** Write the decision with measured demand, dependency
   readiness, port cost, save results and an owner for fixes. Begin with a separate port PR.
   Introduce a shared multi-version build only after the second port demonstrates useful
   common code. Multiple maintenance branches would also require an explicit update to
   our current policy that tags releases on `main`.
7. **Publish distinct, honest artifacts.** Give each Minecraft/loader build a distinct
   filename, explicit metadata and distribution tags, its own checksum and update-feed
   entry. Keep Kithkyn's content version separate from the Minecraft target. Patch common
   bugs in every supported line, and state the retirement policy when choosing a line,
   without promising support for every future game drop.

For now, the decision is one first-release Minecraft target and a patched loader. The
next-version process above is a proposal to evaluate after launch, not work that blocks
the initial release or a public roadmap promise.
