# Website and brand status

On October 1, 2026 Aaron selected Corner Frame (10), attaching the exact wordmark, then requested
the corners removed, a cleaner K foot and a blend with the preferred ith lettering.
The current unframed wordmark is paired with a square K extracted from the same native grid.
The wooden-board flavor is concentrated in the logo; the website stays sleek. Aaron requested
a lighter dark presentation, so the homepage, asset library and sharing card use slate surfaces,
ivory text and warm oak accents. The homepage at `/` now uses this chosen identity. `/brand`
provides downloads. Earlier comparisons and unused assets are removed, with exploration
preserved in Git at commit `e4c9fc4`.

All 17 bundled Overworld land styles remain available, with Radix keyboard navigation and
nuqs shareable selection and browser history. Offline/cloud requirements, multiplayer guidance
and the expandable FAQ are retained. Modrinth and CurseForge remain coming soon; installation
and source links use GitHub. Ocean and Nether catalogs are planned.

## Chosen identity

| Asset | Master | Dimensions |
| --- | --- | --- |
| Unframed timber wordmark | `Website/public/brand/wordmark.png` | 116 × 48 |
| Square timber K | `Website/public/brand/icon.png` | 32 × 32 |

Aaron's follow-up requested actual image pixels matching the artwork's visual granularity.
The selected 1945 × 809 source is preserved at Git commit `fc64070`, with SHA-256
`5b6d5491cf238665737ba9b4c726c1deffc4eecfcd5ca5e7d9c282946e603a8d`. Its roughly 17-pixel drawn blocks
were reduced to one native image pixel. The prior native version is preserved at `7725c93`.

The current refinement uses the built-in image editor. The "top one" reference was interpreted
as the earlier bare Log Lettering study. It guides the taller ith; the selected wordmark guides
the remaining letters. All corner brackets
are removed, and the capital K's lower side nub and hanging tip are cleaned up. The edited source
is 1950 × 807, SHA-256
`ca6f377ebe188a4cec37e661349e25ccc46c51bf3e4ffa2d6f42d61c34cb2fa8`.
Its original local file is `exec-f4a80f59-372f-4c7b-9ed6-ef5ccd0dd074.png` in this chat's
generated-images directory. Exact edit prompts ship beside the assets.

The wordmark is center-sampled to 116 × 48. For output cell `(x, y)`, sample source coordinates
`floor((x + 0.5) * 1950 / 116)` and `floor((y + 0.5) * 807 / 48)`. Retain the sampled RGB color;
alpha below 128 becomes transparent, and alpha at or above 128 becomes fully opaque. Transparent
RGB is zero. Extract the first K from a 26 × 30 region beginning at `(7, 8)` on this grid. Copy that
region to `(3, 1)` on a transparent 32 × 32 canvas for the square icon.

The kit includes wordmark exports at 464 × 192 and 928 × 384, and square K exports at 64, 128,
256 and 512 pixels. Every larger PNG copies each native pixel into a whole-number square block;
no smoothing, intermediate colors or soft alpha are introduced. Pixel data was checked against
the native masters for every export. The masters are 5505 and 1623 bytes respectively.

The 128 × 128 K ships as `src/main/resources/kithkyn-logo.png`, referenced by NeoForge metadata.
The 32 × 32 SVG icon supplies the favicon. The wordmark appears in the header and footer at 2×
on desktop and 1× in the phone header; brand specimens use 3×/2× for the wordmark and 5× for the K.
CSS uses `image-rendering: pixelated`. The README uses the 4× wordmark PNG at its native size.

The downloadable kit includes the native masters, larger PNGs, SVG containers embedding native
pixels with pixelated rendering, a 1200 × 630 sharing card in JPEG and SVG, palette, usage notes,
original generation prompts and the Outfit license. The sharing SVG enlarges the wordmark at
exactly 8×. Its JPEG is recaptured from that SVG before packaging. These remain raster pixel
masters inside SVG containers. Use a quiet slate background and integer scales.

The fixed dark system uses three slate levels. Canvas `#20272b` serves the page and footer;
surface `#293238` serves the village section and setup panels; raised `#333e44` serves logo
specimens, the sharing artwork and hover states. Text is ivory `#f0ece3`, muted text is `#b1bbbd`
and the oak accent is `#dcb075`. Borders derive from ivory at 16% opacity. The CSS contract uses
semantic roles rather than aliases from the previous forest and light palettes.

Browser controls and scrollbars use the dark color scheme. Filled actions use ivory or oak
with canvas-colored labels; selected tabs and keyboard focus use oak. Secondary text has at
least 5.60:1 contrast across all three surfaces. The PNG artwork and native pixel dimensions
remain unchanged.

Exact generation and refinement prompts live in `Website/public/brand/generation-prompts.md`.
The pixel reduction and K extraction used deterministic sampling. `bun run brand:export`
inside `Website/` regenerates the SVG containers and ZIP from the checked-in PNG masters and exports.

The rejected generated village diorama and literal wooden page controls remain deleted.
See [website-redesign.md](website-redesign.md) for the reference pass and decision history.

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

The local preview is `http://127.0.0.1:45173`. Reviewed desktop and mobile captures live in
`docs/website-preview/`. `/` is the landing page; `/brand` is the asset library. Old direction,
study and `/play` links redirect home. `/atlas`, `/stories` and `/setup` redirect to the relevant
section. Redirects preserve query parameters and, where no section is specified, existing hashes.

This version is not deployed. Earlier domain checks found that `kithkyn.com` served a hiring
product, while `kithkin.com` redirected to `/lander`. Confirm the intended domain and hosting
destination before production deployment. Static hosts must rewrite page routes to `index.html`.
The sharing-image metadata must use the final host's absolute URL when publishing.
