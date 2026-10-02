# Website and brand status

On October 2, 2026 Aaron requested a light presentation and different temporary Minecraft
images. The homepage at `/` uses the accepted Hewn Planks wordmark and matching K, warm-white
surfaces, dark brown copy, saddle-brown actions and Outfit typography. A softly washed Minecraft
village scene extends behind the desktop navigation and intro. On phones, the photograph sits
below the intro so it remains visible. `/brand` provides the matching light palette and kit.

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

The fixed light system uses canvas `#faf8f2`, content surface `#f0ece3` and artwork/hover
stage `#e7dfd1`. Text is dark brown `#302a24`, muted copy `#665c50`, and the saddle-brown
accent `#805437`. Borders derive from dark ink at 16% opacity. Text and accents exceed 4.5:1
on all three surfaces; the minimum ratio is 4.90:1. Desktop copy sits in the pale horizontal
wash. Phone copy sits on the opaque canvas, with a separate full-color photo below. Browser
controls use the light scheme. Earlier dark studies retain their scoped palettes.

The comparison at `/brand/directions` and the complete `/brand/directions/hewn-planks` preview
remain available. Their wordmark and K PNGs are byte-identical to the canonical masters.
The earlier-directions gallery retains the original spruce Hewn, Cabin Joinery and Patchwork.
The rejected generated village diorama and literal wooden page controls remain deleted.
See [website-redesign.md](website-redesign.md) for references and design decisions.

## Temporary scenery

The former shoreline and twelve catalog-review images have been removed from the website.
Three temporary Minecraft scenes now provide the backdrop and regional-browser imagery.
These are references from other Minecraft projects, not Kithkyn builds or evidence of
simulation behavior. Captions explicitly say "Placeholder" and link to each source gallery;
the footer also credits the two source projects. Five styles retain material/biome field notes.
The 17-style roster and descriptions still describe the bundled Kithkyn catalogs.

| Website file | Source gallery | Original image |
| --- | --- | --- |
| `Website/public/images/village-reference.jpg` | [RealisticWorld](https://modrinth.com/modpack/realisticworld/gallery), "Complimentary Reimagined Shaders" | [1920 × 1080 JPEG](https://cdn.modrinth.com/data/GO8ghx0J/images/9520376dca34bf77d222aba551e6f22599df8566.jpeg) |
| `Website/public/images/woodland-reference.jpg` | [Complementary Reimagined](https://modrinth.com/shader/complementary-reimagined/gallery), featured woodland scene | [1920 × 1080 JPEG](https://cdn.modrinth.com/data/HVnmMxH1/images/26327bef581206670288bf7e1b1b5f411291f793.jpeg) |
| `Website/public/images/desert-reference.jpg` | [Complementary Reimagined](https://modrinth.com/shader/complementary-reimagined/gallery), desert lake | [2560 × 1440 PNG](https://cdn.modrinth.com/data/HVnmMxH1/images/35b1b4eb6a186297fe039995449d17608f510560.png) |

The first two files are unchanged copies. The desert source is encoded as JPEG at quality 80
without resizing or repainting. The site crops with CSS and adds a CSS wash in the desktop hero.
The source projects retain their respective media rights; these are temporary design references.
Replace them with approved Kithkyn captures before launch. All sources and scene dimensions live
in `Website/src/data/imagery.ts`, so replacement does not require changing component layouts.

## Preview and publishing

The local preview is `http://127.0.0.1:45173`. Reviewed desktop and mobile captures live in
`docs/website-preview/`. `/` is the landing page; `/brand` is the asset library. Old direction,
study and `/play` links redirect home. `/atlas`, `/stories` and `/setup` redirect to the relevant
section. Redirects preserve query parameters and, where no section is specified, existing hashes.

This version is not deployed. Earlier domain checks found that `kithkyn.com` served a hiring
product, while `kithkin.com` redirected to `/lander`. Confirm the intended domain and hosting
destination before production deployment. Static hosts must rewrite page routes to `index.html`.
The sharing-image metadata must use the final host's absolute URL when publishing.
