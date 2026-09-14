# Releasing Kithkyn

How a release is cut, and the plan for the first public one, **v1.0.0**. The first half is
the durable process; the second half is the v1.0.0 checklist, written so an agent can work
it top to bottom. Owners are marked **Codex** (agent work) or **Aaron** (needs an account,
hardware, art, or a judgement call).

## Decisions already made

These are settled. Do not reopen them in release work.

- **Free, forever.** GPL-3.0-only, never commercial. Recorded in
  [structure-sourcing.md](structure-sourcing.md).
- **The building catalogs ship in the jar.** Every catalog is Kithkyn's own build, inspired
  by the projects named in the README's "Credits and inspiration" section and altered from
  them, not copied. The credit, and the standing offer to alter a design further if a creator
  asks, is the whole stance (Aaron, 2026-09-14). Older text in the village docs and in the
  `source_policy` field of `tools/structure/*-catalog-*.json` that calls the catalogs
  "private" or "excluded from public distribution" predates this decision and gets updated
  to match it, not the other way round.
- **The LLM stays on by default, local provider.** The zero-setup offline model is the
  decided design in [llm-brain.md](llm-brain.md). The release job is to make the first boot
  legible, not to change the default.
- **Version 1.0.0**, tagged `v1.0.0` on `main`. `gradle.properties` already carries it.
- **Distribution:** GitHub Releases is canonical. Modrinth and CurseForge mirror it. Modrinth
  first, since its NeoForge support and publishing API are the cleaner of the two.
- **Saves:** a world created on 1.0.0 must load on every later 1.x. A save-breaking change
  is a 2.0.

## The release process

Semantic versioning in `gradle.properties` (`mod_version`): patch for fixes, minor for new
content or features, major for anything that breaks saves. Every release is a tag on `main`.

1. Land the release's last change on `main` through an ordinary PR.
2. Bump `mod_version`, move the `Unreleased` section of `CHANGELOG.md` under the new version
   with the date, and merge that as its own PR titled `Release vX.Y.Z`.
3. Tag the merge commit `vX.Y.Z` and push the tag. The release workflow builds the jar and
   drafts a GitHub Release with the changelog section as its body.
4. Publish the GitHub Release. Upload the same jar to Modrinth and CurseForge with the same
   changelog text (manual until `mc-publish` is wired; see Phase 4).
5. Update `updates.json` on kithkyn.com so the in-game update checker sees the new version.

The jar that ships is the one CI built from the tag, never a local build.

## v1.0.0 checklist

Phases run in order. Inside a phase the tasks are independent unless noted.

### Phase 1: bundle the catalogs (Codex)

Today the jar carries only the Birch Forest catalog (23 definitions, 71 templates). The other
thirteen catalogs (Desert, Badlands, Floodplain, Jungle, Swamp, Mediterranean, Tundra, Alpine
Highlands, Japanese Cherry Grove, Nautical Coast, Polynesian Coast, Romanian, and the market
review set) live as datapacks in the live server's world folder and in the per-catalog
records under `tools/structure/`.

- [ ] **Move every catalog into `src/main/resources`.** Definitions under
      `data/kithkyn/kithkyn/buildings/`, templates under `data/kithkyn/structure/`, at the ids
      the code already resolves (the private packs were built to those ids, so nothing in
      `village/buildings/` should change). Source each pack from the datapack its village doc
      names as the deployed one, and check its template hashes against the catalog record.
- [ ] **Retire the private-pack machinery.** `tools/structure/SetVillageDatapackPriority.java`
      exists so a world datapack overrides the jar; once nothing overrides, delete it and any
      doc that tells you to run it.
- [ ] **Update the docs and records.** `buildings.md`, `building-spec.md`, `birch-village.md`,
      every `*-village.md`, and `docs/README.md` say "private datapack" in places; change them
      to "bundled". Set `source_policy` in each `tools/structure/*-catalog-*.json` to state
      the decision above.
- [ ] **`./gradlew check` passes on the bundled set** (`auditStructureTemplates` runs over the
      whole shipped template directory).
- [ ] **Acceptance:** with no datapack in the world folder, a fresh world founds a village
      from the jar alone in each catalog's biome. `/kithkyn create-village <pos> <style>` per
      style, or the headless verification launcher, is enough; look at each founding set
      once rendered.

### Phase 2: metadata and packaging (Codex, art from Aaron)

- [ ] **`neoforge.mods.toml`:** fill `credits` with the same creators the README names, set
      `logoFile` and add the PNG (256×256 works; Aaron supplies the mark, the website already
      has one), set `updateJSONURL` to `https://kithkyn.com/updates.json`, and delete the
      template's example comments. The file is generated from `src/main/templates/`.
- [ ] **`updates.json`:** write it in the NeoForge update-checker format with a `promos`
      block for `1.21.1-latest` and `1.21.1-recommended`, and host it at the URL above (Aaron
      deploys it with the website).
- [ ] **Both sides required.** The mod has custom entity renderers and container screens, so
      it must be on client and server. Confirm nothing in `mods.toml` claims otherwise and say
      so in the README's install section.
- [ ] **Model licences in the README.** The Llama 3.2 Community License asks products built
      on it to display "Built with Llama"; Gemma's terms ask that its use restrictions are
      passed on. Add a short "Models" paragraph to the credits section naming both, with links.
      The mod downloads the models rather than redistributing them, which is why this is a
      notice and not a licence file.
- [ ] **Jar contents review.** `unzip -l build/libs/kithkyn-1.0.0.jar` and check for
      anything that should not ship: stray `.DS_Store`, generated-source leftovers,
      `src/generated` duplicates. The `dev/` package (44 classes) does ship; it is gated
      behind the `Developer commands` config, which defaults to off, and that is acceptable
      for 1.0. Moving it to its own source set is post-1.0 work.

### Phase 3: make it behave on a stranger's machine (Codex, Windows test from Aaron)

Everything so far has run on one developer's Apple Silicon Mac against a dev server. The
release is the first time the mod meets other hardware.

- [ ] **First-boot download is legible.** `LlamaServerLauncher` fetches an ~18 MB
      `llama-server` and a ~2 GB GGUF model into `<game dir>/kithkyn/` on first server start.
      Before the download, log one INFO line naming the file, its size, and the directory.
      When the LLM status turns `FAILED`, tell online operators in chat once, with the detail
      and the config key that turns the LLM off or points it at a cloud provider.
- [ ] **Downloads are verified and resumable.** The launcher checks nothing today. Pin a
      SHA-256 per artifact, verify after download, delete and re-fetch on mismatch, and never
      launch a partial file (download to a temp name, rename on success).
- [ ] **Single-player is documented.** The integrated server does the same download into
      `.minecraft/kithkyn/`, so a single-player world pauses on first load until the model is
      present. The README says this plainly, with the disk and RAM numbers.
- [ ] **Platform matrix.** The launcher names binaries for macOS arm64/x64, Windows x64/arm64
      and Linux x64/arm64. Test a real first boot on Windows x64 and Linux x64 (Aaron for
      Windows; a Linux VM or the CI runner for Linux). On Windows, check the execute path and
      that the subprocess is killed on server stop.
- [ ] **Log volume.** Production runs at INFO. There are 316 `LOGGER.info` calls against 75
      `LOGGER.debug`; audit the ones that fire per tick, per villager, or per pathfinding
      attempt (the `[path]` lines and their relatives) and demote them to DEBUG. A public
      server's log must stay readable for a day.
- [ ] **Command permissions.** Walk the `/kithkyn` tree and make every world-changing branch
      (`create-village`, `delete`, `raid`, `start-project`, `treasury`, `emigrate`, and the
      like) require permission level 2. Conversation and lookup branches (`ask`, `chat`, `talk`,
      `profile`, `standing`) may stay open to players. Confirm `/kkdev` stays behind
      `Developer commands = false`.
- [ ] **Existing worlds.** The village and pillager replacement packs only affect chunks
      generated after install. Say so in the README, and confirm that installing into an
      existing vanilla world does nothing worse than leaving old villages alone.
- [ ] **Curios present and absent.** Boot once with the Curios mod installed and once without;
      both must load cleanly (`AccessoryCompat` is the seam).
- [ ] **Memory guidance.** State the numbers once, in the README and on the mod pages:
      the server's usual heap plus roughly 3 GB resident for the default model, or nothing
      extra with a cloud provider.

### Phase 4: CI and release automation (Codex, secrets from Aaron)

- [ ] **Fix `build.yml`.** It installs JDK 17; the toolchain resolver then downloads 21
      anyway. Install 21 directly, run `./gradlew check` instead of `build` so the template
      audit runs (`ubuntu-latest` has `python3`), cache Gradle, and upload `build/libs/*.jar`
      as a workflow artifact so every PR produces an installable jar.
- [ ] **Add `release.yml`.** Trigger on tags matching `v*`. Build with `check`, then create
      a GitHub Release with the jar attached and the matching `CHANGELOG.md` section as the
      body (`softprops/action-gh-release` or the `gh` CLI). Mark it a draft so Aaron
      publishes it by hand the first time.
- [ ] **Mod-platform upload (optional for 1.0).** `Kir-Antipov/mc-publish` can push the same
      jar to Modrinth and CurseForge from the release workflow. It needs `MODRINTH_TOKEN` and
      `CURSEFORGE_TOKEN` repository secrets and the two project ids, which exist only after
      Phase 5 creates the pages. Wire it after the first manual upload.
- [ ] **Production smoke test, scripted.** ModDevGradle runs differ from a real install in
      classloading and resource handling, so the built jar has to be booted the way a player
      boots it: run the NeoForge 21.1.72 installer into an empty directory, drop the jar into
      `mods/`, start the dedicated server, connect a client carrying the same jar. Pass means:
      the server reaches "Done", config files appear, the model downloads and the LLM status
      reaches ready, a village founds, and the log has no ERROR lines. Script the server half
      under `tools/release/` so it can be repeated for every release; record the client half
      as steps in this file if it cannot be scripted.

### Phase 5: publish (Aaron, copy from Codex)

- [ ] **Mod page copy.** Codex drafts `docs/mod-page.md`: a one-paragraph pitch, a feature
      list in player language, requirements (Minecraft 1.21.1, NeoForge 21.1+, both sides, the
      model download and RAM), the config toggles most people will want (`Generate villages`,
      `Undead villages`, `Replace pillagers`, `Wandering merchant`, `Enable LLM?`,
      `LLM provider`), and the credits paragraph. Modrinth and CurseForge take the same
      Markdown.
- [ ] **Screenshots.** Six to ten: a founded village in three or four biomes, the villager
      screen, a conversation, a wall. The gallery renders and the private preview server
      recipe in [ui-preview.md](ui-preview.md) produce these without playing.
- [ ] **Accounts and pages.** Create the Modrinth and CurseForge projects (agents cannot
      create accounts). Set licence GPL-3.0-only, loader NeoForge, game version 1.21.1,
      environment client and server, source and issues links to the repository, and the
      website link.
- [ ] **Cut the release.** Follow "The release process" above: `Release v1.0.0` PR, tag,
      publish the drafted GitHub Release, upload the jar to both pages, deploy `updates.json`.

## Out of scope for 1.0.0

Named so nobody spends release time on them.

- Moving the `dev/` package and the verification run configurations out of the shipped jar.
- The Taiga catalog (direction still undecided), and every "future settlement system" in
  [village-biomes.md](village-biomes.md).
- Save migration tooling. 1.0.0 is the baseline; there is nothing older to migrate from,
  since the Village Life break was deliberate ([project-identity.md](project-identity.md)).
- Quests. The README stops claiming them until they exist.
