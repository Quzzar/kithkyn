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
   changelog text (manual until `mc-publish` is wired; see step 3).
5. Update `updates.json` on kithkyn.com so the in-game update checker sees the new version.

The jar that ships is the one CI built from the tag, never a local build.

## v1.0.0 preparation checklist

Updated 2026-09-30. These four steps describe preparation; public publication remains a
separate final action. The first release’s changelog is deliberately still undated.

### 1. Bundle every approved catalog

- [x] All **17 regional styles** are in `src/main/resources`: **394 building definitions**
  and **459 templates**, including wall families.
- [x] Source from the latest deployed, approved production sets, preserving their ids and
  later cost, workplace and storage corrections. Birch remains the saved-style fallback.
- [x] Record every shipped definition/template hash in
  [`bundled-catalogs.json`](../tools/release/bundled-catalogs.json).
- [x] Remove private-only installation claims and the obsolete datapack-priority helper.
- [x] Run the source and installable-jar catalog audits, the full template audit, and unit tests.
  The formerly disabled regional asset tests now run in normal `check`.
- [x] Verify actual founding from the jar alone in every style, in all four rotations,
  with physical beds, shared containers, and village save round trips.

### 2. Verify installation and runtime

- [x] Official NeoForge **21.1.72** installer, Java 21, empty mod directory, fresh disposable
  world, no building datapacks. macOS arm64 reaches `Done` and passes all 68 founding cases.
- [x] Real local runtime and default model provisioning, SHA-256 verification, resume of an
  interrupted transfer, AI ready, clean server exit and no remaining AI listener.
  Evidence: [`verification/macos-arm64.json`](../tools/release/verification/macos-arm64.json).
- [x] Pin all six platform runtime archive sizes/hashes and both model revisions/sizes/hashes.
  Windows ZIP extraction uses Java, so players do not need an external `unzip`.
- [x] Test corruption, wrong checksums, ignored ranges, wrong offsets, verified caches and
  safe ZIP paths. Startup/shutdown cannot spawn a process after server stop.
- [x] Warn operators once per failed AI attempt; keep routine worker and path logs at DEBUG.
- [x] Audit command permissions: `/kithkyn` requires level 2; `/kkdev` additionally requires
  `Developer commands = true`, which defaults to false.
- [x] Curios-present production boot and packaged-jar client connection; chat and trade
  both rendered. Evidence: [server](../tools/release/verification/macos-arm64-curios.json),
  [client](../tools/release/verification/macos-arm64-client.json).
- [ ] Confirm the Windows x64 and Linux x64 production CI matrix passes.
- [ ] Complete an ordinary Minecraft-launcher client installation on Windows before publication.

The disposable server fixture disables world generation, uses loopback networking and offline
test users, and disables NeoForge’s common-config file watcher. Those are fixture settings,
not changes to players’ defaults. With this pinned NeoForge version, the watcher can retain a
dedicated JVM after the worlds have saved; if that happens in a real installation, set
`disableConfigWatcher=true` in `config/fml.toml` and restart. The mod’s AI subprocess shutdown
is checked separately. The fixture’s native client uses the prepared development launcher
with the packaged jar; it is not a substitute for the ordinary-launcher check above.

Repeat the server test on Java 21, from the repository root:

```sh
./gradlew check build
python3 tools/release/audit-catalogs.py build/libs
python3 tools/release/smoke-server.py --jar build/libs --directory /tmp/kithkyn-clean-install --accept-eula
```

Use a new directory for every world. Add `--disable-llm --curios /path/to/curios.jar` for the
optional integration pass. That pass does not count as an AI first-boot check. The script
refuses existing worlds or populated mod folders and preserves a JSON report plus logs.
`--java` chooses the Java 21 executable; `--port` allows isolated parallel fixtures.

For the packaged-jar client capture:

```sh
./gradlew prepareClientJoinLocal
python3 tools/release/smoke-client.py --jar build/libs/kithkyn-1.0.0.jar --directory /tmp/kithkyn-client --mode chat
```

First start the server fixture with `--keep-alive --disable-llm --port 25680 --stop-file /tmp/kithkyn-client-stop`.
Create the stop file after the client check for normal server shutdown. Use `--mode trade`
to capture the other screen. If Curios is on the server, also pass `--curios /path/to/curios.jar`
to the client. [ui-preview.md](ui-preview.md) explains the sample payloads.

### 3. Prepare distribution

- [x] Fill mod metadata, approved logo, homepage, issue link, creator credits and update URL.
- [x] Include the GPL license and README/model notices in the jar.
- [x] Prepare NeoForge’s update feed at `Website/public/updates.json`. Deploy it with the
  website **after** the release is public; it currently advertises the prepared 1.0.0.
- [x] Java 21 CI runs `check build`, audits the installable jar, uploads an artifact, and
  tests real Windows/Linux dedicated installations both with and without Curios.
- [x] Tagged release workflow validates `mod_version` and a dated changelog, runs checks
  and a production smoke, and drafts a GitHub Release with the canonical jar and SHA-256 sums.
- [x] Draft common Modrinth/CurseForge copy in [mod-page.md](mod-page.md), including client
  and server requirements, downloads, RAM, configuration, credits and current limits.
- [x] Review seven [publication screenshots](release-gallery/README.md), with accurate
  captions for catalog showcases and sample UI payloads.
- [ ] Create or identify the Modrinth and CurseForge projects. Set GPL-3.0-only, NeoForge,
  Minecraft 1.21.1, client and server, source/issue/website links, and the prepared copy.
- [ ] Add platform project ids/tokens only if automatic mirroring is wanted. Manual upload
  is supported for 1.0.0; no placeholder publishing credentials are required.

### 4. Match public claims to released behavior

- [x] README, changelog, website and mod-page copy describe seventeen bundled regional
  catalogs and explain that local model weights are downloaded.
- [x] Remove undertakings from the released feature list. That system remains parked;
  the release work does not enable it. Ships and Nether settlement catalogs remain future work.
- [x] Document rules while AI starts or is unavailable, model memory/disk use, both-side
  installation and the new-chunks rule for world generation.
- [ ] Final review of the release candidate and CI evidence.

### Publish after the remaining checks

Merge the preparation PR through the normal review process. Date the 1.0.0 changelog in the
release PR, tag `v1.0.0` on main, and let CI build the canonical jar and draft release. Review
that draft, publish it, upload that same jar to Modrinth and CurseForge, and deploy the website
update feed. Fresh-install the **downloaded public jar** once as the final distribution check.
