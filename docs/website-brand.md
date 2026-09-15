# Website and brand

The website lives in `Website/`, using Bun, React, Vite, TypeScript, vanilla-extract and
Playwright. This first branded pass is local only, not a production deployment.

## Selected identity

The approved primary logo is **Sturdy timber**, the first mixed-case alternative in v11.
Display the name as **KithKyn**, with two capital Ks. Repository/mod spelling remains
`kithkyn`. The exact tagline is **Bringing villages to life**.

The title consists of upright oak-board letters beneath three terracotta-roof cottages.
Keep straight wood grain, no knots, no repeated letter nailheads. The subtitle sits in a
fitted timber plaque, with no hanging ropes. The square companion shows only the three
cottages on forest green, making it usable without unreadable small text.

`Website/public/brand/` holds the active assets. The main logo uses
`kithkyn-logo-transparent.png`, a real RGBA cutout. Neutral checkerboard components were
removed directly, preserving the original colored pixels and enclosed plaster highlights.
The header uses `kithkyn-wordmark.png`, an extracted mixed-case timber wordmark with real
transparency. The square mark's forest fill is intentional and remains unchanged.

The wordmark was created with the built-in image-generation edit workflow using the approved
logo as its reference. The prompt asked for only the exact wooden **KithKyn** lettering, kept
both capital Ks, and removed the cottages, plaque, tagline and every background pixel. The
generator rendered a checkerboard instead of alpha twice, so a deterministic alpha cleanup
removed only the connected neutral background and preserved the generated colored lettering.

Previous explorations remain in `Website/public/brand-explorations/` as design references.
Do not reintroduce all-capitals, italic lettering, timber sign-only marks, knots, or repeated
letter fasteners: those directions were superseded by user feedback.

## Website direction

The full logo is always the focal point. The selected homepage is the **Title screen**
direction: a centered scenic introduction followed by three large village postcards. Its
composition takes cues from [Lightship's centered scenic panel](https://mobbin.com/sites/sections/a5fa6838-ff45-42cc-8e90-b513fd69a5e7).
The comparison bar and the unused Village atlas and World menu directions have been removed.
The top-left brand now pairs the square village emblem with the transparent timber wordmark
instead of typeset text.

The numbered three-step explanation was rejected because it read like generic AI marketing.
The technical section now compares the two real ways to run the village brain. Online models
use OpenAI, Claude or DeepSeek, while the offline path uses the bundled local model through
llama.cpp. Copy must explain that the selected LLM steers village priorities from a compact
simulation brief, the game validates available actions, and safe rules keep a village moving
when no model is available.

Village types appear in a horizontally scrollable carousel with explicit previous and next
controls. This should accommodate new variants without turning the section into a dense grid.
Directional, external-link and technical symbols use Lucide icons rather than Unicode glyphs.
The footer contains the compact brand, useful page navigation and the version-neutral
**Minecraft NeoForge** platform label. It does not repeat the tagline, pin compatibility to a
specific Minecraft release or describe itself as a KithKyn project.

The homepage palette is intentionally narrow: night and one raised forest surface for dark
areas, parchment for the download area, cream and muted sage for text, oak for emphasis, and
one timber brown for text on parchment. Filled dark buttons on the parchment section were
removed because their contrast competed with the primary identity.

The village carousel uses real **structure review screenshots**, not autonomous-village
progress or polished promotional imagery. Sources:

- Mediterranean: `run/mediterranean-full-profile/render/client/screenshots/mediterranean-full-center-homes.png`
- Jungle: `run/jungle-showcase/bamboo-adoption-20260910/render/client/screenshots/jungle-bamboo-homes.png`
- Mangrove: `run/next-village-preview/client/screenshots/mangrove_swamp.png`

The hero uses the existing 27-second Valecraft forest video at
`Website/public/media/valecraft-reference-hero.mp4`, paired with
`valecraft-reference-forest.jpg` as its static poster. The poster renders immediately and the
video fades in only after playback starts. Video is limited to wider screens and is disabled
for reduced-motion or data-saving preferences. It is muted, inline and looping.

Both hero assets come from the user's `valecraft-site/src/assets/` and remain composition
placeholders, not KithKyn simulation footage. The next media pass should replace them with a
real KithKyn village captured in its biome with no HUD. Keep the logo and scenery separate. Do
not pass off generated art, gallery previews, or illustrative timelines as live simulation
evidence.
Locally hosted Pixelify Sans and Outfit fonts include their OFL license files under
`Website/public/fonts/`. No live server-status, wake, or download behavior was copied.

There is no invented release/download link. No verified KithKyn project was found on Modrinth
or CurseForge as of September 14, 2026, so both download destinations are visible but marked
**Coming soon**. Development links point to the project on GitHub until actual release pages
exist.

## Verification

From `Website/`: `bun run build`, `bun run lint`, `bun run format:check`, and
`bun run test:e2e`. Browser tests run on 45175; the user preview uses 45173. Tests cover the
selected homepage on mobile and desktop, image loading, content and link accuracy, overflow,
keyboard focus, both transparent logo assets and reduced motion. Full-page captures are written
under `test-results/`.
