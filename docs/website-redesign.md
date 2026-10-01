# Website identity studies

## Selected icon, full timber lettering

Aaron selected the first icon in the pixel-timber round, Pixel Joinery (5). The next
iteration writes the full name, Kithkyn, in matching logs and tests restrained framing.
The approved K remains the reference: stepped oak planks, honey highlights, sparse grain
and a charcoal silhouette. Every letter should use the same material and pixel scale.

Three wordmarks compare bare log lettering, a thin open corner frame and a slim wooden
frame. The K serves as the first letter of the full name rather than being repeated beside
another K. The selected standalone icon is shown alongside the three wordmarks.

| Study | Treatment |
| --- | --- |
| Selected K | Standalone version of the approved Pixel Joinery icon. |
| 9. Log Lettering | All seven letters are matching oak timbers, with no frame. |
| 10. Corner Frame | Four short charcoal corner brackets leave the frame open. |
| 11. Oak Frame | Thin oak rails surround the name, with a transparent interior. |

Exact built-in generation prompts live in `Website/public/studies/log-lettering-prompts.md`.

The signature is the timber lettering. Existing slate surfaces, oak accents, charcoal text
and locally hosted Outfit remain fixed. Each new logo uses the quiet Pixel Joinery homepage
for context; the website gains no wooden controls or decorative texture. Earlier rounds
remain available for comparison. This changes the identity assets, not the page composition,
so the prior Mobbin layout pass remains applicable.

```text
comparison / selected K / three timber wordmarks
           / previous pixel-timber round
preview / full timber name in the header / existing product page
```

## Focused round: pixel timber

The next iteration follows Aaron's request for a Cobblemon-like pixel-game feel: a K made
from pixelated wooden planks, paired with softer readable lettering. Wood stays in the logo.
The signature is the stepped timber K; the existing quiet website layouts remain the context.
Oak and honey form the sprite, charcoal defines its silhouette, and ivory or slate fills
the rounded wordmark. Outfit remains the body and control face.

Four new studies, numbered 5 through 8, compare precise plank joinery, a looser pegged K,
a branded wooden tile and a little villager peeking around the planks. This round now lives
at `/studies/timber`; the first four remain at `/studies/first` for reference.
All rounds reuse the same comparison component and product content.

```text
pixel-timber round / four lockups / full page previews
             / link to first round for comparison
```

The prior Mobbin layout pass remains applicable because the page composition is unchanged.
The Minecraft mod reference pass supplies the identity precedent for this iteration.

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

## Pixel-timber directions

| Direction | Identity | Website treatment |
| --- | --- | --- |
| 5. Pixel Joinery | Three stepped oak planks form a K, with ivory rounded lettering and a charcoal outline. | Light slate surfaces, split text and scene. |
| 6. Crossgrain | A looser timber K with offset planks and a square peg, with ivory rounded lettering. | Soft green surfaces, centered copy and a wide scene. |
| 7. Woodcut | A dark K stamped into a pixel oak tile, with quiet dark lettering. | Ink and copper accents, two-column copy above a wide scene. |
| 8. Peek | A tiny original villager peeks around a timber K, with ivory rounded lettering. | Blue and honey, scene on the left and copy on the right. |

The same four page compositions make it possible to judge each identity without introducing
another website redesign. The first Woodcut output had noisy edges around the lettering;
a fresh generation replaced it with solid dark lettering and an opaque wooden tile.
Exact prompts, including that refinement, are saved in
`Website/public/studies/pixel-timber-prompts.md`.

## First-round directions

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
The later wooden sign logo, plank controls, wooden page frames, Pixelify font, obsolete brand-kit
downloads and obsolete preview images are removed. Git history preserves the prior attempts.

The real in-game scene and catalog captures remain. Their provenance is recorded in
[website-brand.md](website-brand.md). Five styles use field notes where a useful capture is
not available. Nothing in the preview claims the scene is an autonomous-growth timeline.

## Verification

Current desktop and mobile captures live in `docs/website-preview/`. They show the
latest lettering comparison and the three new homepage entrances, alongside the earlier captures.
Browser checks cover all three comparisons and all eleven directions,
all 17 styles, shared selection and browser history, keyboard focus, setup tabs, FAQ
disclosure, reduced motion, loaded assets, the page font and 320-pixel overflow.

All 18 desktop/mobile Playwright checks pass, along with the production build, ESLint and
Prettier. The render pass also checked 1280 × 900, 390 × 844 and 320 × 720 viewports and
found no console warnings or errors. All three full timber wordmarks were inspected in mobile headers.

The domain and hosting destination remain unresolved. This work stays a local preview and
draft PR. A final logo suite and publication follow the owner's wordmark selection.
