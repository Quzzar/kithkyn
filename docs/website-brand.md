# Website and brand status

Aaron selected direction A on September 30, 2026, with a slapstick wooden-board feel.
The active implementation is in `Website/`: a centered wooden title sign, crooked plank
menu, village-life explanation, regional postcard carousel, then installation and FAQs.
See [website-redesign.md](website-redesign.md) for the visual plan and rejection record.

All 17 bundled land styles remain available, with horizontal Radix keyboard navigation
and nuqs shareable selection and browser history. Offline/cloud requirements, multiplayer
guidance and the expandable FAQ are retained. Modrinth and CurseForge remain coming soon;
installation and source links use GitHub.

## Identity and artwork

The new identity is code-authored SVG geometry with outlined pixel lettering. The suite
includes primary, reversed, monochrome, wordmark and signpost-emblem variants, matching
transparent PNG and lossless WebP exports, 32/64/180/512-pixel icons, a logo-only sharing card,
and a downloadable ZIP with fonts, licenses and usage notes. `/brand` displays the suite.
The mod-list logo uses the new signpost emblem.

The rejected generated diorama, its banner derivative, old screenshots and bundled copies
remain deleted. The current sharing card is new vector logo artwork on forest green.

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
`docs/website-preview/`. The former comparison routes redirect to homepage sections.
`/atlas` preserves village query parameters.

This version is not published or merged. Earlier domain checks found that `kithkyn.com`
served a hiring product, while `kithkin.com` redirected to `/lander`; confirm the intended
domain and hosting destination before production deployment. Static hosts must rewrite
page routes to `index.html`.
