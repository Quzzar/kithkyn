# Website and brand status

On October 1, 2026 Aaron accepted Hewn Planks, then requested an in-game backdrop with an
logo and intro over it. The homepage at `/` now uses this honey-oak identity, cool slate
surfaces, ivory copy and Outfit typography. The wooden flavor belongs to the wordmark and K.
The full-width shoreline scene extends behind the navigation and intro, with layered slate
overlays and a fade into the following section. `/brand` provides the matching kit.

All 17 bundled Overworld land styles remain available, with Radix keyboard navigation and
nuqs shareable selection and browser history. Offline/cloud requirements, multiplayer guidance
and the expandable FAQ are retained. Modrinth and CurseForge remain coming soon; installation
and source links use GitHub. Ocean and Nether catalogs are planned.

## Chosen identity

| Asset | Master | Dimensions |
| --- | --- | --- |
| Hewn Planks wordmark | `Website/public/brand/wordmark.png` | 128 × 34 |
| Matching timber K | `Website/public/brand/icon.png` | 32 × 32 |

The accepted wordmark combines Hewn's broad letter shapes with the honey-oak planks from
Aaron's attached reference. Joined faces, pale cut ends, sparse grain and a slim dark outline
supply the material. The first K has a flat bottom. The rejected corner brackets and hanging
K tips remain absent. The matching K is a reference edit, not a pixel-identical extraction.

The built-in image editor produced wordmark source
`exec-59a21a0e-4ab7-4f47-a0d8-e2971099b9b1.png` (2170 × 725) and icon source
`exec-62248666-f059-4673-8261-828d4ea957ab.png` (1280 × 1280) in this chat's generated-images
directory. Exact edit prompts, input roles and the sampling recipe ship in
`Website/public/brand/generation-prompts.md`. The previous 116 × 48 identity and its original
provenance remain in Git at `0212509`.

The wordmark is center-sampled to 128 × 43. For output cell `(x, y)`, sample source coordinates
`floor((x + 0.5) * 2170 / 128)` and `floor((y + 0.5) * 725 / 43)`. Retain sampled RGB;
alpha below 128 becomes transparent with zero RGB, while alpha at or above 128 becomes fully
opaque. Trim empty top and bottom rows to one transparent row beyond the visible art, yielding
128 × 34. The K uses the same sampling and alpha rule from 1280 × 1280 to 32 × 32.

The kit includes wordmark exports at 512 × 136 and 1024 × 272, and square K exports at 64,
128, 256 and 512 pixels. Every larger PNG copies each native pixel into a whole-number square
block; no smoothing, intermediate colors or soft alpha are introduced. Pixel data is checked
against the native masters for every export.

The 128 × 128 K ships as `src/main/resources/kithkyn-logo.png`, referenced by NeoForge metadata.
The 32 × 32 SVG icon supplies the favicon. The header K is 1×, desktop hero wordmark 4×,
phone hero 2× and footer wordmark 2×. Brand specimens use 3×/2× for the wordmark and 5× for the K.
CSS uses `image-rendering: pixelated`. The README uses the 4× wordmark PNG at its native size.

The downloadable kit includes the native masters, integer PNG exports, SVG containers embedding
native pixels, a 1200 × 630 JPEG and SVG sharing card, palette, usage notes, generation prompts
and the Outfit license. The sharing SVG enlarges the wordmark exactly 8×. Its JPEG is recaptured
before packaging. `bun run brand:export` regenerates SVG containers and the ZIP from the
checked-in PNGs and integer exports. The SVGs preserve raster pixels instead of tracing paths.

The fixed dark system uses three slate levels: canvas `#252e35`, content surface `#303e46`
and artwork/hover stage `#40525a`. Text is ivory `#f2eee5`, muted copy `#c0cace`, and the oak
accent `#e3bd88`. Borders derive from ivory at 16% opacity. Text and oak accents exceed 4.5:1
on every surface, with a minimum ratio of 4.63:1. The backdrop's phone overlay has at least
70% opaque slate; ivory copy exceeds 4.55:1 even over pure white. The desktop oak eyebrow stays
in the horizontal overlay's 90% region. Browser controls and scrollbars use the dark scheme.

The comparison at `/brand/directions` and the complete `/brand/directions/hewn-planks` preview
remain available. Their wordmark and K PNGs are byte-identical to the canonical masters.
The earlier-directions gallery retains the original spruce Hewn, Cabin Joinery and Patchwork.
The rejected generated village diorama and literal wooden page controls remain deleted.
See [website-redesign.md](website-redesign.md) for references and design decisions.

## Real capture library

The scenic backdrop comes from
`run/floodplain-site-review/screenshots/client/screenshots/mavulena-overview.png`,
relative to the original project root. It shows real in-game village buildings and terrain.
Its visible caption is "Mangrove coast · In-game capture". This is a site-review capture,
not an autonomous-growth timeline.

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
