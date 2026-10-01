# Timber identity studies

Generated with the built-in imagegen tool on October 1, 2026. These are two new identities
for comparison; the existing selected brand kit remains at `/brand`.

## Cabin Joinery wordmark

Source: `exec-d6a5b2e1-53da-4649-81e0-8591c20a31d8.png` (2079 × 756).
Output: `cabin-wordmark.png` (128 × 35).

```text
Use case: logo-brand.
Asset type: a new transparent horizontal wordmark for Kithkyn, a cozy Minecraft mod about villagers building their own communities.
Primary request: Design original custom lettering made from interlocking timber beams, with a log-cabin patchwork construction. Text exactly "Kithkyn", seven letters K-i-t-h-k-y-n. One capital K and six lowercase letters. Make the lettering friendly, substantial, exceptionally readable, compact and carefully spaced.
Style: disciplined low-resolution pixel art with a single uniform square-pixel cadence, crisp stair-step diagonals, simple silhouettes. Suggest a logical 160 by 40 pixel drawing enlarged with nearest-neighbor squares. Each glyph is built from a few honey oak and walnut beams with visible squared joints and occasional pale end-grain faces. Short restrained grain marks only. Use mostly flat wood-color blocks, very shallow pixel shading and a thin dark pine outline. The initial K should be a memorable monogram with a vertical beam and two interlocking diagonal beams. Log-cabin craft, playful but sophisticated.
Composition: one horizontal wordmark, on a wide transparent canvas close to 4:1. Occupy most of the width with modest clear space. No background, no other object or text.
Avoid: corner brackets, plaque frames, dangling tabs or nubs under the K, nails, rope, leaves, tiny noisy wood texture, realistic rendering, smooth gradients, glossy 3D extrusion, strokes of inconsistent pixel size, Minecraft title-font imitation, watermarks. A clean flat base to the first K. Actual transparency.
```

## Patchwork wordmark

Source: `exec-326474b2-07b4-4b01-903d-05f77644b1c2.png` (1983 × 793).
Output: `patchwork-wordmark.png` (128 × 26).

```text
Use case: logo-brand.
Asset type: a new transparent horizontal wordmark for Kithkyn, a Minecraft mod about villagers making homes and communities.
Primary request: Create a distinct original patchwork timber identity. Text exactly "KITHKYN", seven uppercase letters K-I-T-H-K-Y-N. Build a broad, compact custom geometric wordmark from joined rectangular wood pieces like a beautifully assembled cabin wall. Bold, very readable, subtly irregular through alternating pieces rather than wobbly letter outlines.
Style: flat graphic pixel art, strong squared letter forms and crisp stair-step K diagonals on one uniform square-pixel grid. Suggest a logical 160 by 36 pixel drawing enlarged with nearest-neighbor squares. Use a restrained patchwork of pale birch, warm honey oak and muted reddish cedar. One to three deliberately arranged colored pieces per letter. Very few short blocky grain marks. Small dark charcoal join lines show how the wood fits together. A slim dark outline, no bevel or deep extrusion. The K is strong and square with a clean vertical base and a two-beam diagonal construction.
Composition: one horizontal wordmark on a wide transparent canvas around 4:1. Occupy most of the width with modest clear space. No background or other text.
Avoid: wooden signs, border frames, corner brackets, dangling feet or extra tabs, heavy black outlines, photo texture, gradient gloss, small mottled detail, curves, 3D perspective, scenery, decorative objects, Minecraft title-font imitation, watermarks. Actual transparency.
```

## Matching K icons

Each icon was generated as a reference edit of its corresponding wordmark using the prompt
below. These are matching interpretations, not claimed to be pixel-identical extractions.
Cabin source: `exec-f67a8367-4708-451e-837e-2fe92f8e0a1b.png`.
Patchwork source: `exec-bc7395ad-7fff-49e6-83af-7f7ea92fe9c0.png`.
Outputs: `cabin-icon.png` and `patchwork-icon.png`, both 32 × 32.

```text
Use case: precise-object-edit. Asset type: a square standalone K brand icon.
The supplied image is the edit target. Extract and isolate ONLY the first capital K from the wordmark, preserving its exact silhouette, joined wood pieces, colors, grain, outline and pixel-art style. Keep the clean flat bottom of the vertical stem and all the construction details of this K. Do not redesign it. Remove every other letter completely. Center this single K on a square canvas with balanced modest clear space all around, occupying approximately 78 percent of the height. The result must be a genuine transparent PNG cutout. No background, no plaque, no frame, no other letters or symbols, no shadow outside the original letter, no embellishments. The icon must clearly be the same K as the reference wordmark.
```

## Native grid preparation

The user explicitly requested real pixel dimensions that match the artwork's visible granularity.
The generated sources live under
`/Users/quzzar/.codex/generated_images/01a0f318-e21f-7af2-9514-a771d5c5e370/`.

Wordmarks were center-sampled onto 128 × 46 (Cabin) and 128 × 51 (Patchwork) grids; icons onto
32 × 32 grids. For each target cell (x, y), read the source pixel at
`floor((x + 0.5) * sourceWidth / targetWidth), floor((y + 0.5) * sourceHeight / targetHeight)`.
Alpha below 128 becomes transparent black; other samples keep their RGB and use alpha 255.
Wordmarks retain their 128-pixel width, with transparent rows trimmed to one row outside the
visible artwork at each end. Icons retain the square canvas.

The website renders these PNG masters with pixelated sampling at integer scales: header 2×
(desktop) or 1× (phone), Cabin hero 3×, Patchwork hero 6×, both phone heroes 2×, toolbar icons
1× and comparison icons 2×. No interpolated large raster is used as the downloadable master.
