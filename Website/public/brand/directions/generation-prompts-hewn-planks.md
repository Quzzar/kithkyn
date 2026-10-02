# Hewn with the original timber planks

Created with the built-in imagegen tool on October 1, 2026. The request was to combine
Hewn's broad letter shapes with the original honey-colored timber construction.

## Wordmark edit

Edit target: the original Hewn wordmark, enlarged 8× without interpolation.
Material reference: the user's `exec-dce95114-1647-4521-ab96-f89905b1020a.png`.
Generated source: `exec-59a21a0e-4ab7-4f47-a0d8-e2971099b9b1.png` (2170 × 725).
Output: `hewn-planks-wordmark.png`, 128 × 34 native pixels.
This is a reinterpretation of the broad Hewn forms in joined oak, not a pixel-identical recolor.

```text
Use case: style-transfer.
Asset type: transparent Kithkyn brand wordmark.
Input images: Image 1 is the edit target and controls the letter shapes. Image 2 is a wood-material reference only.
Primary request: rebuild Image 1's thick friendly "Kithkyn" lettering using the honey oak timber planks from Image 2.
Keep Image 1's seven-letter spelling K-i-t-h-k-y-n, capital K and lowercase ithkyn, squat substantial silhouettes, open counters, proportions, spacing and stepped pixel edges. Preserve the chunky weight. Change the construction and surface material: each letter is assembled from a few broad honey oak planks with clear dark seams and joined squared ends. Use Image 2's golden plank faces, pale cut end-grain caps, deep brown side faces and sparse dark rectangular grain marks. The wood feels assembled like cabin joinery rather than carved from a single pale spruce slab. Diagonal K arms are broad joined planks, still filling Image 1's thick K shape. Give the first K a clean flat bottom without protruding pegs or dangling tips.
Style: crisp disciplined low-resolution pixel art on one uniform square grid. Think of a logical 128 by 35 drawing enlarged with nearest-neighbor squares. Restrained shallow block shading, sparse detail, a slim dark outline. No smooth gradients.
Composition: one horizontal wordmark, around 4:1 transparent canvas, modest clear space, all seven letters readable.
Constraints: do not copy Image 2's narrow letter proportions, corner brackets, frames or hanging K foot. Preserve Image 1's broader letter shapes. No cabin emblem, plaque, nails, rope, leaves, scene, background, extra text, watermark, glossy bevel, noisy texture or deep 3D extrusion. Remove all stray pixels outside the letters. Genuine transparent PNG.
```

## Matching K

Edit target: the generated plank wordmark above.
Generated source: `exec-62248666-f059-4673-8261-828d4ea957ab.png`.
Output: `hewn-planks-icon.png`, 32 × 32 native pixels.
The icon is a matching reference edit, not claimed to be a pixel-identical crop.

```text
Use case: precise-object-edit.
Edit target: supplied Kithkyn timber-plank wordmark.
Create a matching standalone K icon by isolating only the first capital K. Keep this K's broad vertical honey oak plank, two thick joined diagonal plank arms, pale cut end-grain faces, dark side faces, grain marks and slim outline. Keep the flat bottom, no protruding pegs or dangling tips. Remove all other letters and all pixels outside the K. Center the K on a square transparent canvas with balanced clear space, occupying roughly 80 percent of its height. Preserve its crisp uniform pixel-art cadence. The silhouette must match the wordmark's K closely and remain readable at 32 pixels.
No plaque, cabin, frame, corner brackets, other letters, extra objects, background, stray pixels, glow or new ornament. Genuine transparent PNG.
```

## Native grid preparation

The sources remain in this chat's generated-images directory. For output cell `(x, y)`,
sample source coordinates `floor((x + 0.5) * sourceWidth / outputWidth)` and
`floor((y + 0.5) * sourceHeight / outputHeight)`. Retain sampled RGB. Alpha below 128
becomes transparent with zero RGB; alpha at or above 128 becomes fully opaque.
The wordmark's intermediate grid is 128 × 43; transparent rows are then trimmed to one
row beyond the visible artwork, yielding 128 × 34. The reference-edited K is sampled
from 1254 × 1254 to 32 × 32 and retains that square canvas. Larger exports copy each
native pixel into an exact integer square block.

Aaron accepted this direction, then requested a game backdrop with an overlay. On October 2,
the homepage and matching preview changed to warm light surfaces and saddle-brown accents.
Credited temporary Minecraft scenery supplies the setting, with Outfit text. The desktop
hero renders the wordmark at 4×; the phone hero is 2×. The header
K is 1× and footer wordmark 2×. All use pixelated rendering. The original spruce Hewn
remains available in the earlier-directions gallery for comparison. The same masters also
ship at the canonical `/brand/wordmark.png` and `/brand/icon.png` paths.

## October 2 K edge repair

The built-in image editor repaired the original K (`exec-62248666-f059-4673-8261-828d4ea957ab.png`).
The new source is `exec-a5a87a82-f882-415a-bdff-953a3dc2ae4e.png`, 1254 × 1254 pixels.
It uses the center-sampling and binary-alpha rule above. The 32 × 32 visible bounds are
`(7, 4)` through `(25, 27)`, with at least four transparent grid cells on every edge.
Canonical icon exports, favicon, selected preview and packaged mod icon all use this repair.

```text
Use case: precise-object-edit. Asset: the existing Kithkyn square timber K icon. Image 1 is the edit target. Make a minimal repair to its plank ends and outer outline so the K reads as complete at small size. Preserve the same single K silhouette, golden oak vertical plank, two joined diagonal timber arms, pale end-grain caps, dark brown side faces, sparse grain, and flat bottom. The pale cut ends and one-cell dark outer outline must be fully visible on EVERY tip and edge; no abruptly chopped ends, clipped strokes, missing outlines or pixels touching the canvas edge. Compose the entire K centered inside a square genuinely transparent canvas, with at least 4 logical grid cells of empty space around it. Use a uniform logical 32 by 32 pixel grid, each cell one flat color, enlarged as crisp squares. Sparse details only. Keep proportions closely matched to the supplied icon, no new design or decoration. No frame, corner marks, plaque, roof, extraneous letters, dangling pegs, gradients or smoothing.
```

### Final outline pass

The first repair still lost a thin cap outline at 32 pixels. Its native master was enlarged
exactly 16× as the edit target. The final built-in edit is
`exec-9e890def-5c77-401c-8e74-2fd076b5e979.png` (1254 × 1254). The final native
bounds remain `(7, 4)` through `(25, 27)`. This is the shipped icon source.

```text
Use case: precise-object-edit. Image 1 is the edit target: a 32 by 32 pixel K icon enlarged to 512 by 512 with each pixel a 16px square. Preserve its exact grid size, same honey oak timber K silhouette, proportions, plank arms, grain and colors. Fix only its incomplete dark outline. The lower diagonal arm's pale end-grain cap on the bottom right currently has no continuous dark border on its right edge; restore a complete dark border around this whole tip. Also preserve a continuous dark outer edge on the upper tip and the vertical stem. Every exposed edge needs an outline that is at least ONE FULL LOGICAL GRID CELL wide, so it survives center-sampling back to 32x32. Each cell must be a flat color with no gradients or finer texture. Do not produce finer-than-32x32 artwork. Use a square transparent canvas and keep at least four logical empty pixels of clear space around the whole K. Single K only. No redesign, new textures, decoration, bevel, floating pixels, plaque, frame or letters. The result should look identical except the cut plank tips have their complete dark borders.
```
