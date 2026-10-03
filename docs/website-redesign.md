# Website design decisions

## Current direction: warm light, October 2, 2026

Aaron accepted the Hewn Planks identity and light layout, then requested a complete logo
outline, removal of the public brand kit and a working link to the existing credits.
Keep the honey-oak wordmark and matching K as the signature. Warm-white, sand and pale-tan
surfaces, brown ink, saddle-brown controls and Outfit typography carry the page. Wood belongs
in the logo rather than literal plank controls or decorative page frames.

The desktop hero uses a full-width Minecraft photograph with a pale horizontal wash and
soft bottom fade. Phone copy sits on a solid canvas with the photograph below. The headline
is "Autonomous Villages". A 17-biome-group browser and local/cloud setup follow the hero.
The Village life section and top header are removed. The hero supplies section links,
and the footer retains GitHub, issue and Credits links.

The site is for players. Brand-library and design-comparison pages, downloads, unused identity
assets and their code are removed. Old links redirect to the homepage while preserving village
selection. Credits opens the README's real "Credits and inspiration" list in a new tab.
The native reduction now preserves thin source-outline pixels; the wordmark is 128 × 37
and the K is 32 × 32. Source provenance, palette, media sources and the export recipe live in
[website-brand.md](website-brand.md) and [website-brand-prompts.md](website-brand-prompts.md).

Three credited Minecraft reference scenes supply the hero and photographed biome entries.
Their "Placeholder" captions link to the source galleries. The footer omits temporary
scenery credits. They do not represent Kithkyn builds. Five biome entries use material
field notes. Replace the references with approved Kithkyn captures before launch.

## Layout references

The light reference pass inspected [Monarch](https://mobbin.com/sites/sections/f64d7aff-77b4-4951-92df-2c9e4411ca67),
[In Common With](https://mobbin.com/sites/sections/a87f2a50-8a78-4193-b491-5589059041fa), and
[Aurora](https://mobbin.com/sites/sections/c7d426dd-5880-436c-85cb-ddf9fa2eddce).
Monarch shows compact light navigation and dark copy against photography. In Common With
uses warm architecture photography, simple navigation and little decorative framing. Carry
forward generous image space, restrained navigation and clear copy contrast. Aurora's blank
capture supplied no useful composition. Avoid promotional strips, boxed hero copy and wood
as the page's surface texture.

An earlier backdrop pass inspected
[Lightship](https://mobbin.com/sites/sections/a5fa6838-ff45-42cc-8e90-b513fd69a5e7) and
[Savor](https://mobbin.com/sites/sections/f6870d1a-3708-4347-92c6-048af2b0d73f).
Lightship carries a readable message directly over a landscape. Savor puts sparse navigation
above a full-width scene, but its blur would hide village detail. The owner cited Veilcraft;
no specific Veilcraft page was identified or claimed as inspected.

## Copy rules

Writing references: [Microsoft's voice guidance](https://learn.microsoft.com/en-us/style-guide/brand-voice-above-all-simple-human)
and [Nielsen Norman Group's web-writing research](https://www.nngroup.com/articles/concise-scannable-and-objective-how-to-write-for-the-web/).

- Say what the mod does in concrete terms: villagers mine, build, marry and raise children.
- Use everyday words and short sentences. Delete slogans that add no information.
- Use names players recognize, including biome-group labels instead of architectural labels.
- Keep each section to the information needed to understand it or take its next step.
- Describe current behavior only. Put plans and unreleased feature promises in project notes or a blog.

The FAQ, model-failure explanation, future-catalog note and coming-soon destinations stay removed.
The setup comparison says "AI cloud provider" and retains the actual local-model requirements.

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

## Exploration record

Earlier rounds explored timber lettering, open corner framing, dark presentations and
Cabin Joinery, Patchwork, Hewn, Woven and Cabin Mark identities. Aaron preferred Hewn's broader
letter shapes combined with the original honey-oak plank material. The current Hewn Planks
wordmark and K implement that choice. Dark variants and the comparison gallery are historical,
not additional shipping pages. Git history preserves their source, prompts and screenshots.

The generated village diorama, wooden sign logo, plank controls, wooden page frames and
Pixelify headings were rejected as tacky or unfinished. Corner brackets and the first K's
small lower protrusions were also rejected. Do not reintroduce these features.

## Verification

Inspect the actual homepage at 1280 × 900, 390 × 844 and 320 × 720, including the logo outlines,
biome navigation, setup, footer and visible keyboard focus. Keep current proof in
`docs/website-preview/`. Check native outline continuity and integer scaling rather than
assuming transparent canvas space proves the artwork is complete.

Required checks are strict TypeScript/production build, ESLint, Prettier and the desktop/mobile
Playwright suite. Run Gradle `processResources` when changing the packaged mod icon. The sharing
JPEG must be inspected after recapturing its SVG. Credits should open the actual contributor
list. Production publishing requirements are recorded in [website-brand.md](website-brand.md).
