# Website design decisions

## Final selection: Corner Frame

On October 1, 2026 Aaron selected Corner Frame (10), attaching the exact generated wordmark.
Use that original PNG unchanged, paired with the previously selected Pixel Joinery K icon.
The root route becomes the finished landing page, and the unused comparison pages, alternate
identities and their assets are removed. Git history preserves the exploration.

The signature remains the pixel oak lettering with four charcoal corners. The website keeps
the slate canvas and surfaces, charcoal ink, oak accent and Outfit typography from the selected
preview. Its split hero, real capture, all 17 catalogs and setup content remain in place.

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

The PNG masters are raster artwork. SVG containers embed those same originals without tracing
or changing their appearance; describe them accurately. A sharing card uses the chosen mark,
plain slate canvas and one short product line. No new generated scene or wooden page controls.

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
Lettering, open Corner Frame and slim Oak Frame. Corner Frame is the chosen wordmark.

The comparison pages, alternate identities and historical captures are removed from the
shipping site. Git commit `e4c9fc4` preserves the exploration and original prompts. The selected
reference, K extraction and Corner Frame prompts ship in
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

All 18 desktop/mobile Playwright checks passed. The download checks passed again after verifying
the final PNG and JPEG signatures. The strict TypeScript production build, ESLint, Prettier and
Gradle `processResources` passed. The K's packaged resource matches its approved master.
The render review covered 1280 × 900, 390 × 844 and 320 × 720 viewports. A clean final browser
session reported no console warnings or errors. The brand ZIP passed an archive integrity check.

Production deployment waits for the intended domain and hosting destination. The sharing-image
metadata needs the final host's absolute URL at publication. See [website-brand.md](website-brand.md).
