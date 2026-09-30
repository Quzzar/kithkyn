# Website and brand status

The active website is a three-direction low-fidelity study in `Website/`. Aaron rejected
Hearth and horizon on September 30, 2026 and requested three substantially different options.
No direction is selected. See [website-redesign.md](website-redesign.md) for the concepts,
reference observations and rejection record.

The comparison page links to `/play` (game title screen), `/atlas` (architecture browser),
and `/stories` (comic-style village life). The atlas retains all 17 bundled styles,
Radix keyboard navigation and nuqs shareable selection with browser history. `/setup`
retains the actual offline/cloud requirements, multiplayer guidance and expandable FAQ.
Modrinth and CurseForge remain coming soon; installation and source links use GitHub.

## Artwork and logo status

The rejected generated village illustration, its social-banner derivative, the old homepage
screenshots and the ZIP containing that artwork have been removed. The concepts use labeled
media placeholders. No generated replacement illustration has been added.

Earlier raster logo variants remain in `Website/public/brand/` as provisional source exports.
They are not a chosen identity and are not displayed in the new concepts. Their provenance
and alpha transparency remain intact. The mod-list logo still uses the earlier hearth emblem.
Finish or replace the logo suite after Aaron chooses the website direction.

## Real capture library

The existing real catalog screenshots are retained for the next design phase. They are
building-review captures, not naturally developed villages or autonomous-growth timelines.
The current low-fidelity concepts deliberately use placeholders rather than finished media.

Thirteen catalog images are included. Four catalogs (Japanese Cherry Grove, Nautical Coast,
Savanna Tent and Mushroom) have material/biome field notes instead of an invented screenshot.
The capture sources, relative to the original project root, are:

| Catalog | Source |
| --- | --- |
| Mediterranean | `docs/release-gallery/mediterranean-homes.png` |
| Jungle Tribal | `docs/release-gallery/jungle-homes.png` |
| Swamp | `docs/release-gallery/swamp-homes.png` |
| Pueblo | `docs/release-gallery/badlands-center.png` |
| Tundra | `docs/release-gallery/tundra-walls.png` |
| Birch Forest | `run/full-style-showcase/client/screenshots/approved-birch-reference.png` |
| Rustic Woodland | `run/full-style-showcase/client/screenshots/rustic-complete-barns.png` |
| Taiga | `run/viking-full-profile/render/client/screenshots/viking-full-homes.png` |
| Romanian | `run/romanian-full-profile/render/client/screenshots/romanian-center-and-small-homes.png` |
| Desert Oasis | `run/pueblo-showcase/desert-center-approved-20260909-224500/screenshots/client/screenshots/desert-center-overview.png` |
| Floodplain | `run/nilotic-showcase/screenshots/client/screenshots/nilotic-houses.png` |
| Alpine Highlands | `run/next-village-preview/client/screenshots/alpine_highlands.png` |
| Polynesian Coast | `run/jungle-showcase/screenshots/client/screenshots/polynesian-center.png` |

These are composition/catalog previews; some review images contain donor rows in the background.
Use their captions, and do not present them as a production village's growth timeline.

## Preview and verification

The local preview is `http://127.0.0.1:45173`. Desktop concept captures also power the
comparison page from `Website/public/previews/`; mobile captures live in
`docs/website-preview/`. Tests run on port 45175, on desktop and mobile Chromium.
Use `bun run build`, `bun run lint`, `bun run format:check` and `bun run test:e2e`.

The study is not published or merged. Previously, `kithkyn.com` served a hiring product,
while `kithkin.com` redirected to `/lander`; confirm the intended domain and hosting before
any production deployment. Static hosts must rewrite page routes to `index.html`.
