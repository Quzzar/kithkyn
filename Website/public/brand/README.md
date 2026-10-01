# Kithkyn brand kit

Selected on October 1, 2026: Hewn Planks, joined honey oak lettering with pale cut faces.
The square K uses the same plank construction, grain and pixel scale.

- wordmark.png: transparent 128 × 34 native pixel master.
- icon.png: transparent 32 × 32 native square K.
- wordmark-512.png and wordmark-1024.png: exact 4× and 8× pixel copies.
- icon-64.png, icon-128.png, icon-256.png and icon-512.png: exact integer-scaled square exports.
- wordmark.svg and icon.svg: SVG containers embedding the native PNGs with pixelated rendering.
- social.jpg: 1200 × 630 sharing card, captured from social.svg.
- social.svg: sharing card with the native wordmark and embedded Outfit font.
- palette.json: three slate surface levels, ivory text, muted text and oak accents.
- generation-prompts.md: the exact original built-in image-generation prompts.
- outfit-LICENSE.txt: the sharing card font's license.

## Usage

Use the full wordmark when the name needs to be read. Use the K icon in compact spaces.
Keep the colors, proportions and transparent clear space. Use a quiet slate background
to bring out the warm oak lettering. Do not put the wordmark on a wood texture.
Use whole-number enlargement with nearest-neighbor sampling. In CSS, set image-rendering: pixelated.

The canvas is the page background, surface is for content panels, and raised is for logo stages
and hover states. Borders use the ivory ink at 16% opacity. Reserve oak for active controls and focus.

The edited 2170 × 725 wordmark was center-sampled to 128 × 43, with binary alpha.
Empty top and bottom rows were trimmed to a 128 × 34 master. The reference-edited K was
center-sampled from 1280 × 1280 to 32 × 32, using the same binary-alpha threshold.
The PNG masters have one actual image pixel per grid cell. The SVG containers embed these pixels.
The sharing card displays the wordmark at exactly 8×, with native SVG text and layout.
