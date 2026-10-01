# Kithkyn brand kit

Selected on October 1, 2026: the Corner Frame timber wordmark, reduced to its native pixel grid.
The square icon is the first K extracted from that same master.

- wordmark.png: transparent 116 × 48 native pixel master.
- icon.png: transparent 32 × 32 native square K.
- wordmark-464.png and wordmark-928.png: exact 4× and 8× pixel copies.
- icon-64.png, icon-128.png, icon-256.png and icon-512.png: exact integer-scaled square exports.
- wordmark.svg and icon.svg: SVG containers embedding the native PNGs with pixelated rendering.
- social.jpg: 1200 × 630 sharing card, captured from social.svg.
- social.svg: sharing card with the native wordmark and embedded Outfit font.
- palette.json: the website's slate and oak colors.
- generation-prompts.md: the exact original built-in image-generation prompts.
- outfit-LICENSE.txt: the sharing card font's license.

## Usage

Use the full wordmark when the name needs to be read. Use the K icon in compact spaces.
Keep the colors, proportions and transparent clear space. Use a light, quiet background
so the charcoal outline remains readable. Do not put the wordmark on a wood texture.
Use whole-number enlargement with nearest-neighbor sampling. In CSS, set image-rendering: pixelated.

The generated 1945 × 809 source was center-sampled at the visual block cadence, with binary alpha.
The PNG masters have one actual image pixel per grid cell. The SVG containers embed these pixels.
The sharing card displays the wordmark at exactly 8×, with native SVG text and layout.
