# Website and brand status

On October 1, 2026 Aaron selected Corner Frame (10), attaching the exact wordmark. It is paired
with the previously selected Pixel Joinery K. The wooden-board flavor is concentrated in the
logo; the website stays sleek. The homepage at `/` now uses this chosen identity. `/brand`
provides downloads. Earlier comparisons and unused assets are removed, with exploration
preserved in Git at commit `e4c9fc4`.

All 17 bundled Overworld land styles remain available, with Radix keyboard navigation and
nuqs shareable selection and browser history. Offline/cloud requirements, multiplayer guidance
and the expandable FAQ are retained. Modrinth and CurseForge remain coming soon; installation
and source links use GitHub. Ocean and Nether catalogs are planned.

## Chosen identity

| Asset | Master | Dimensions |
| --- | --- | --- |
| Corner Frame wordmark | `Website/public/brand/wordmark.png` | 1945 × 809 |
| Pixel Joinery K | `Website/public/brand/icon.png` | 1254 × 1254 |

Both transparent PNG masters ship unchanged. The wordmark's SHA-256 is
`5b6d5491cf238665737ba9b4c726c1deffc4eecfcd5ca5e7d9c282946e603a8d`, matching the owner's attachment.
The K also ships as `src/main/resources/kithkyn-logo.png`, referenced by NeoForge mod metadata.
The wordmark appears in the site header, footer and repository README; the K supplies the favicon.

The downloadable kit includes both PNG masters, SVG containers embedding the originals,
a 1200 × 630 sharing card in JPEG and SVG, the palette, usage notes, original generation prompts
and the embedded Outfit font's license. These are pixel-art raster masters, not traced vectors.
Use a quiet light background and preserve proportions, colors and clear space.

Exact prompts live in `Website/public/brand/generation-prompts.md`. `bun run brand:export`
inside `Website/` regenerates the SVG containers and ZIP. The sharing JPEG is captured from
the SVG in the browser at its native dimensions, then included by a subsequent export.

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
