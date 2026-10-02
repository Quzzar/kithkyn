# Website and artwork status

The player homepage uses the accepted Hewn Planks wordmark and matching K, warm-white
surfaces, dark brown copy, saddle-brown actions and Outfit typography. A softly washed
Minecraft village scene extends behind the desktop navigation and intro. On phones, the
photograph sits below the intro. The site has no public brand library, download kit or
identity-comparison pages. Artwork provenance stays in these internal project notes.

All 17 bundled land catalogs appear under biome-group names, with Radix keyboard navigation
and nuqs shareable selection and browser history. Existing selection IDs stay stable.
The main headline is "Autonomous Villages", with no duplicate eyebrow.
Copy describes concrete game actions. Local/cloud AI requirements and multiplayer installation
instructions remain; the FAQ, future-catalog note and coming-soon placeholders are removed.
Installation and source links use GitHub. The public page makes no roadmap promises.

Credits opens the existing [Credits and inspiration](https://github.com/Quzzar/kithkyn#credits-and-inspiration)
list in a new tab. That README section credits the building projects and their creators,
plus the language models and runtime. Keep that list as the source of truth rather than
maintaining a second contributor list in the website.

## Chosen identity and native pixels

| Asset | Master | Dimensions |
| --- | --- | --- |
| Hewn Planks wordmark | `Website/public/brand/wordmark.png` | 128 × 37 |
| Matching timber K | `Website/public/brand/icon.png` | 32 × 32 |

The wordmark combines Hewn's broad letter shapes with the honey-oak planks from Aaron's
attached reference. Joined faces, pale cut ends, sparse grain and a dark outline supply
the material. The first K has a flat bottom. Corner brackets and hanging K tips were rejected.
The matching K is a reference edit, not a pixel-identical extraction.

The built-in image editor produced wordmark source
`exec-59a21a0e-4ab7-4f47-a0d8-e2971099b9b1.png` (2170 × 725) and icon source
`exec-9e890def-5c77-401c-8e74-2fd076b5e979.png` (1254 × 1254) in this chat's generated-images
directory. Exact historical prompts and the corrected preparation recipe are in
[website-brand-prompts.md](website-brand-prompts.md). Earlier artwork and explorations remain
in Git history; they do not ship in the site's public directory.

The original single-center sampling discarded thin source-outline segments, making the
K's left edge and the wordmark's top look cropped despite transparent canvas margins.
The corrected conversion checks outline coverage in each native cell and selects existing
dark source colors for every exposed boundary cell. Alpha is binary. The wordmark keeps two
transparent rows above and below its silhouette; its visible bounds are `(3, 2)` through
`(124, 34)`. The K keeps its 32-pixel square, with bounds `(6, 4)` through `(26, 28)` and at
least three transparent cells on every side. No new generated artwork was needed for this repair.

The website retains only the assets it uses: native PNGs, a 512 × 148 wordmark for the README,
a 128 × 128 K for `src/main/resources/kithkyn-logo.png`, the SVG favicon and a 1200 × 630 sharing
card in JPEG and SVG. Larger PNGs copy each native pixel into an exact 4 × 4 square. Their
pixel data matches the native masters, and the mod logo is byte-identical to the 128-pixel K.
The header K is 1×, desktop hero wordmark 4×, phone hero 2× and footer wordmark 2×.
CSS uses `image-rendering: pixelated`. The README renders its 4× PNG at native size.

`bun run brand:export` embeds the corrected native PNGs into the favicon and sharing SVG
without resampling. The sharing wordmark uses an exact 8× enlargement. Its JPEG is recaptured
from the rendered SVG after artwork changes. The exporter creates no downloadable kit.

The fixed light system uses canvas `#faf8f2`, content surface `#f0ece3` and hover stage
`#e7dfd1`. Text is dark brown `#302a24`, muted copy `#665c50`, and the saddle-brown accent
`#805437`. Borders derive from dark ink at 16% opacity. Text and accents exceed 4.5:1 on all
three surfaces; the minimum ratio is 4.90:1. Desktop copy sits in the pale horizontal wash.
Phone copy sits on the opaque canvas, with a separate full-color photo below. Browser controls
use the light scheme. See [website-redesign.md](website-redesign.md) for the design references.

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
They remain temporary scenery for the approved Render deployment; replace them with
approved Kithkyn captures when available. All sources and scene dimensions live
in `Website/src/data/imagery.ts`, so replacement does not require changing component layouts.

## Preview and publishing

The local preview is `http://127.0.0.1:45173`. Reviewed desktop and mobile captures live in
`docs/website-preview/`. `/` is the player landing page. Old `/brand` and identity-preview links
redirect home. `/atlas`, `/stories` and `/setup` redirect to their relevant section. Redirects
preserve query parameters and, where no section is specified, existing hashes.

Browser checks cover intact native outlines, integer display scales, no public design tools,
the Credits destination, all 17 biome groups, shared selection/history, keyboard focus,
local/cloud setup, loaded assets, reduced motion and 320-pixel overflow. Inspect the rendered
homepage at desktop and phone sizes before reporting visual changes as complete.

October 2 verification: all 20 desktop/mobile checks passed with two workers. The 17-group
traversal has a longer test allowance after the original 30-second budget expired under
machine load; the final traversal took 2.7 seconds on desktop and 4.5 seconds on mobile.
The homepage was inspected at 1280 × 900, 390 × 844 and 320 × 720, and the live Credits
link opened the correct GitHub list. Strict TypeScript/build, lint and formatting passed.
Gradle `processResources` passed for the packaged K. Native alpha contains only 0 and 255;
both retained 4× PNG exports match their masters exactly. The updated sharing JPEG was
captured at 1200 × 630 from the SVG and visually inspected.

Production deployment uses the existing Render `kithkyn-website` service
(`srv-d89gkkjeo5us738rm5o0`) and its verified `kithkyn.com` domain. That service previously
tracked the retired `Quzzar/kithkyn-old-1` repository; it is being moved to `Quzzar/kithkyn`
with root directory `Website` and a frozen-lockfile Bun build. Keep its SPA rewrite to
`/index.html`. Canonical and sharing metadata now use absolute `https://kithkyn.com` URLs.
The update feed has no version promotions until the mod draft is publicly published.
Record the successful deployment commit and live desktop/mobile verification after deploying.
