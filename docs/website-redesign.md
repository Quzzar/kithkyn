# Website design decisions

## Current identity: refined timber lettering

On October 1, 2026 Aaron selected Corner Frame (10), attaching the exact generated wordmark.
His follow-up requested native pixels matching the visual block granularity. The chosen source
is reduced to a 116 × 48 master, with a 32 × 32 K extracted from its first letter.
A later refinement removes the corner marks and the K's small lower protrusions. The taller
ith is guided by the earlier unframed study, with the remaining letters based on Corner Frame.
The root route becomes the finished landing page, and the unused comparison pages, alternate
identities and their assets are removed. Git history preserves the exploration.

The signature remains the pixel oak lettering, now without framing. The website uses
slate surfaces, warm ivory text, oak accents and Outfit typography. Its split hero, real
capture, all 17 catalogs and setup content remain in place.

The brand page serves creators downloading the exact chosen artwork. The reference pass used
[Whereby](https://mobbin.com/sites/sections/cf76cdcc-c266-4216-b88a-5daa902bb9f1),
[1Password](https://mobbin.com/sites/sections/403eb185-13d1-4e62-a44c-8182fb79d068) and
[Fiverr](https://mobbin.com/sites/sections/1ffa8f98-2e40-4b5f-837a-049dbca8870e).
Their useful patterns are a clear specimen beside its downloads, a complete kit action above
individual assets, and format labels close to the files. Dense legal copy, monochrome alternatives
and corporate contact panels do not serve this small mod's kit.

```text
home / selected wordmark + navigation / split village hero
     / village life / 17 catalogs / setup + FAQs
brand / complete kit download
      / wordmark specimen + files / standalone icon + files
      / sharing card / short usage notes
```

The PNG masters are native raster pixel artwork. SVG containers embed those pixels with
nearest-neighbor rendering. Integer-sized exports preserve the same grid. A sharing card uses the
chosen mark, plain slate stage and one short product line. No new generated scene or wooden page controls.

## Dark presentation

Aaron requested a dark site to suit the timber lettering. This is a fixed brand presentation
across the homepage, asset library and sharing card. Slate canvas and raised surfaces provide
depth; ivory headings and body text carry the content; muted gray supports captions; oak marks
active tabs and keyboard focus. The logo remains the signature. Keep the established layout,
Outfit hierarchy and real imagery. Avoid neon accents, decorative glow, wood-texture panels and
dim body text.

The dark reference pass inspected
[Linear](https://mobbin.com/sites/sections/66f59864-ae93-419b-89c9-e53fbb46a3f3),
[Framer](https://mobbin.com/sites/sections/5831b9f2-ea5e-4e01-b34e-bb68e74ebef3) and
[Retool](https://mobbin.com/sites/sections/e086c44d-6365-469c-860a-0450b25241df).
Carry forward strong light headings over dark surfaces, restrained navigation, colorful artwork
that supplies the personality, and clearly contrasted filled actions. Linear's small secondary
copy is too subdued for the setup instructions. Framer's animated artwork strip and Retool's
multicolored glow do not fit the quiet timber direction.

The first near-black canvas was too dark for the preferred logo presentation. Aaron requested
a slightly lighter dark mode, so the system now has three deliberate levels: the slate page,
lighter content panels, and a brighter stage for the timber art. Logo specimens and sharing
artwork use that stage. The dark outline stays visible without adding glows or artificial shadows.
Color roles replace the old forest/light aliases; hover states use the raised level and borders
derive from ivory at 16% opacity.

The canvas, village panels, setup section, footer and logo specimens all use the same palette.
Ivory actions use canvas-colored labels; oak hovers retain those labels. Active setup tabs and all
focus rings use oak. Browser controls and scrollbars use the dark color scheme. The sharing
JPEG is recaptured from its dark SVG before packaging. The logo pixels and geometry remain unchanged.

## Popular Minecraft mod references


The project owner asked for a reference pass through popular Minecraft mod logos. These
observations come from viewing their official sites and author-maintained listings, not
from generic logo inspiration. Download counts below are Modrinth snapshots from
September 30, 2026, not totals across platforms.

| Mod | Observed identity | Useful lesson for Kithkyn |
| --- | --- | --- |
| [Create](https://modrinth.com/mod/create) (26.8M downloads) | A recognizable mechanical crank on a blue circular ground, with strong material color and a simple silhouette. | An object from the gameplay can carry the icon without a whole illustrated landscape. |
| [Farmer's Delight](https://modrinth.com/mod/farmers-delight) (24.3M downloads) | A pixel-art wooden cutting board and knife, with a framed title strip in the full lockup. | Timber can live in the identity. This is the closest reference for the requested wooden-board flavor. |
| [Cobblemon](https://cobblemon.com/en) ([35.5M Modrinth downloads](https://modrinth.com/mod/cobblemon)) | A compact hexagonal ball icon paired with a rounded lowercase wordmark and a dark outline. Its site uses clean navigation and real game imagery. | Game personality and a sleek website can coexist. The logo does not need the Minecraft title font. |
| [MineColonies](https://minecolonies.com/) | Chunky uppercase settlement lettering, copper tones, a skyline and ribbon. | Settlement imagery makes the subject apparent. Its dense decorative page framing does not fit this brief. |

Our design conclusion is an interpretation: use one memorable construction or villager symbol
with custom readable lettering, then let spacing, typography and real captures carry the
page. Do not copy any of these mods' assets or letterforms. Their artwork is reference only.

The earlier layout pass also used sparse compositions from
[KOBU](https://mobbin.com/sites/sections/92720ac4-a34f-4460-854c-ba4c969c697e),
[General Intelligence Company](https://mobbin.com/sites/sections/fbf517c5-c4b4-4246-818b-818bd1f88032)
and [Koto](https://mobbin.com/sites/sections/062eb6b8-b141-4fe5-8c11-9edc778e2468).
Minecraft mod identities are the more relevant brand precedents.

## Exploration and rejection record

The first broad round compared Joinery, Gather, Offcut and Neighbor. A focused pixel-timber
round then compared Pixel Joinery, Crossgrain, Woodcut and Peek. Aaron selected Pixel Joinery's
K and requested the entire name in matching logs. The final lettering round compared bare Log
Lettering, open Corner Frame and slim Oak Frame. Corner Frame was selected, then refined into
unframed lettering with the earlier study's taller ith. The corner brackets and small lower K
protrusions were specifically rejected; do not reintroduce them.

The comparison pages, alternate identities and historical captures are removed from the
shipping site. Git commit `e4c9fc4` preserves the exploration and original prompts. The selected
Pixel Joinery reference, Corner Frame and subsequent refinement prompts ship in
`Website/public/brand/generation-prompts.md`.

The generated village diorama and its banner derivative remain deleted. The later wooden sign
logo, plank controls, wooden page frames and Pixelify headings were rejected as tacky and
unfinished. They remain removed. The real in-game scene and catalog captures stay, with their
provenance recorded in [website-brand.md](website-brand.md). Five styles use field notes where
useful captures are unavailable. The scene is not presented as an autonomous-growth timeline.

## Verification and publishing

Current captures in `docs/website-preview/` show the chosen homepage and brand downloads at
desktop and phone sizes. Browser checks cover the chosen identity, download formats, old-link
redirects, all 17 styles, shared selection and browser history, keyboard focus, setup tabs,
FAQ disclosure, reduced motion, loaded assets, typography and 320-pixel overflow.

All 18 browser checks passed after the dark theme pass, along with the strict TypeScript
production build, ESLint and Prettier. Native dimensions, equal integer display scales,
pixelated rendering and exported downloads remain covered. The sampling and extraction recipe
is recorded in [website-brand.md](website-brand.md). The logo PNGs and integer exports remain
byte-identical. The earlier Gradle `processResources` check passed, and the K's packaged resource
matches its 128-pixel export.
The render review covered 1280 × 900, 390 × 844 and 320 × 720 viewports. A clean final browser
reload reported no new console warnings or errors. Keyboard navigation visibly renders the
amber focus ring. Ivory text, muted copy and oak accents all exceed 4.5:1 against all three dark
surfaces; muted text has a minimum 5.60:1 ratio. The brand ZIP passed an archive integrity check,
and all 16 entries match their published files.

Production deployment waits for the intended domain and hosting destination. The sharing-image
metadata needs the final host's absolute URL at publication. See [website-brand.md](website-brand.md).
