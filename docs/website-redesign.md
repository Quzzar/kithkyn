# Website identity studies

Aaron clarified on September 30, 2026 that the slapstick wooden-board influence belongs
mainly in the logo and a little flavor. The website should look sleek. The literal plank
menu and pixel-lettered sign logo were rejected as tacky and unfinished. Four new identities
are compared before one is selected.

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

## Four directions

| Direction | Identity | Website treatment |
| --- | --- | --- |
| 1. Joinery | Interlocking slate and oak K, precise sans lettering. | Light slate surfaces, split text and scene. |
| 2. Gather | Two neighboring homes, pine and honey, serif logo lettering. | Soft green surfaces, centered copy and a wide scene. |
| 3. Offcut | Timber K, hand-cut dark lettering with a fine oak edge. | Ink and copper accents, two-column copy above a wide scene. |
| 4. Neighbor | A friendly character peeking from a doorway, rounded lettering. | Blue and honey, scene on the left and copy on the right. |

Offcut was refined after the mod reference pass to add a game artist's lettering and a
recognizable timber object. The other studies keep broader alternatives open. The body
and controls use locally hosted Outfit, with readable text and ordinary buttons.

```text
comparison / four icon + wordmark studies / links to full previews
preview / compact brand + navigation
        / distinct headline, real village still, restrained action
        / village life / 17 styles / actual setup and FAQs
```

The image-generation tool created original transparent raster lockups. Exact prompts are
saved in `Website/public/studies/prompts.md`. These are concepts, not vector exports or a
final brand kit. The existing mod-list icon remains in place until selection.

## Rejection record

The generated village diorama, its banner derivative and bundled copies remain deleted.
The later wooden sign logo, plank controls, wood frames, Pixelify font, obsolete brand-kit
downloads and obsolete preview images are removed. Git history preserves the prior attempts.

The real in-game scene and catalog captures remain. Their provenance is recorded in
[website-brand.md](website-brand.md). Five styles use field notes where a useful capture is
not available. Nothing in the preview claims the scene is an autonomous-growth timeline.

## Verification

Current desktop and mobile captures live in `docs/website-preview/`. They show the
comparison and the four homepage entrances. Browser checks cover all four directions,
all 17 styles, shared selection and browser history, keyboard focus, setup tabs, FAQ
disclosure, reduced motion, loaded assets, the page font and 320-pixel overflow.

All 18 desktop/mobile Playwright checks pass, along with the production build, ESLint and
Prettier. The render pass also checked 1280 × 900, 390 × 844 and 320 × 720 viewports and
found no console warnings or errors. A crowded narrow header was corrected before recapture.

The domain and hosting destination remain unresolved. This work stays a local preview and
draft PR. A final logo suite and publication follow the owner's identity selection.
