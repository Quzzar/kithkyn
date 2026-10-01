# Website and brand status

Aaron clarified on September 30, 2026 that the wooden-board flavor should be concentrated
in the logo, with a sleek website. He selected the Pixel Joinery K from the pixel-timber
round and asked for the full name written in matching logs with light framing.
`Website/` now compares the selected standalone K with Log Lettering, Corner Frame and
Oak Frame. The icon is selected; the wordmark and framing remain under review.
Earlier icon studies remain at `/studies/timber`, with the broader first round at `/studies/first`.
See [website-redesign.md](website-redesign.md) for the Minecraft mod
logo references, visual plan and rejection record.

All 17 bundled land styles remain available, with horizontal Radix keyboard navigation
and nuqs shareable selection and browser history. Offline/cloud requirements, multiplayer
guidance and the expandable FAQ are retained. Modrinth and CurseForge remain coming soon;
installation and source links use GitHub.

## Identity and artwork

Eleven original transparent PNG lockups were generated across the three rounds. A standalone
K was extracted through the image-generation tool from the selected Pixel Joinery reference.
Exact prompts
are saved in `Website/public/studies/prompts.md` and
`Website/public/studies/pixel-timber-prompts.md` and
`Website/public/studies/log-lettering-prompts.md`. Each wordmark is paired with a usable page preview
so the logo can be judged in context. These are raster studies, not a final vector suite.

The rejected wooden suite, plank menu, wooden page frames, sharing card and kit download are removed.
The earlier generated diorama and its derivatives remain deleted. The mod-list icon has
been restored to its existing version while the complete new identity is being refined.
A final suite will include vector artwork, small-size icon variants and sharing graphics
after wordmark selection. The latest framing experiments belong to the logo asset.

## Real capture library

The scenic backdrop comes from
`run/floodplain-site-review/screenshots/client/screenshots/mavulena-overview.png`,
relative to the original project root. It shows real in-game village buildings and terrain.
Its caption identifies it as a site-review capture, not an autonomous-growth timeline.

Twelve real catalog captures are displayed. Romanian, Japanese Cherry Grove, Nautical Coast,
Savanna Tent and Mushroom use material/biome field notes. The former Romanian capture was
removed because the camera was obstructed by a wall. Do not invent a screenshot for a style
without useful media.

Capture sources, relative to the original project root:

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
| Desert Oasis | `run/pueblo-showcase/desert-center-approved-20260909-224500/screenshots/client/screenshots/desert-center-overview.png` |
| Floodplain | `run/nilotic-showcase/screenshots/client/screenshots/nilotic-houses.png` |
| Alpine Highlands | `run/next-village-preview/client/screenshots/alpine_highlands.png` |
| Polynesian Coast | `run/jungle-showcase/screenshots/client/screenshots/polynesian-center.png` |

These are catalog review images; some contain donor rows in the background. Every displayed
image is captioned as an in-game building preview from a catalog review world.

## Preview and publishing

The local preview is `http://127.0.0.1:45173`. Current captures live in
`docs/website-preview/`. `/` compares the selected K and full timber lettering;
`/studies/timber` compares the earlier pixel-timber icons and `/studies/first` the first round.
`/directions/:identityId` opens each complete homepage.
`/play` and `/brand` return to the latest comparison. `/atlas`, `/stories` and `/setup`
open the corresponding section in Log Lettering; `/atlas` preserves query parameters.

This version is not published or merged. Earlier domain checks found that `kithkyn.com`
served a hiring product, while `kithkin.com` redirected to `/lander`; confirm the intended
domain and hosting destination before production deployment. Static hosts must rewrite
page routes to `index.html`.
