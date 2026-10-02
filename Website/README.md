# Kithkyn website

The player landing page at `/` uses the accepted Hewn Planks wordmark and matching square K.
The native artwork is 128 × 37 and 32 × 32 pixels, with a complete dark outline. The desktop
wordmark is 4×, the phone wordmark 2× and header K 1×. A Minecraft village scene sits behind
the desktop header and intro under a warm-white wash. On phones, the photograph sits below
the intro. Warm-white, sand and pale-tan surfaces pair with dark ink, saddle-brown actions
and Outfit. Wood supplies the logo's character; the controls stay simple.

The public site contains village life, all 17 biome groups and local/cloud AI setup. Credits
opens the existing README list in a new tab. Brand-library, download-kit and design-comparison
pages are removed. Prompts and artwork provenance remain in internal project docs.

React, Vite, strict TypeScript, vanilla-extract, Radix, nuqs and Playwright, using Bun.

## Development

```sh
bun install
bun run dev --port 45173
```

The preview runs at `http://127.0.0.1:45173`.

```sh
bun run build
bun run lint
bun run format:check
bun run test:e2e
```

Tests run at port 45175 on desktop and mobile Chromium. The suite covers native outline
continuity, integer display scales, absence of public design tools, Credits, all 17 biome groups,
URL history, keyboard focus, setup tabs, reduced motion, loaded images, typography and 320-pixel
layouts. The 17-group traversal has a longer test budget because it visits every panel.
Captures are saved in `test-results/`; reviewed captures live in `../docs/website-preview/`.

Old `/brand`, `/play`, identity and study links return home. `/atlas`, `/stories` and `/setup`
open their corresponding home section. Redirects preserve query parameters, and village
selection remains shareable through `?village=...`.

## Artwork exports

`public/brand/wordmark.png` and `icon.png` are corrected native pixel masters. The retained
512-pixel README wordmark and 128-pixel mod-list K use exact 4× copies of each native pixel.
The site uses pixelated rendering and integer display scales. Source prompts and the corrected
conversion recipe are in `../docs/website-brand-prompts.md`.

```sh
bun run brand:export
```

The script embeds the corrected native pixels in the SVG favicon and sharing card without
resampling. It creates no kit. After changing the sharing SVG, capture it at 1200 × 630 in
the browser, save the JPEG as `public/brand/social.jpg` and inspect it. Keep the file extension
consistent with the actual browser output.

Three temporary Minecraft scenes provide the hero and biome-browser imagery; five groups
use material and biome notes. Captions and footer links credit their source galleries.
These photos do not depict Kithkyn builds. They remain clearly credited temporary scenery
for the approved deployment; replace them with final Kithkyn captures when available.
See `../docs/website-brand.md` for provenance and publishing status, and
`../docs/website-redesign.md` for design decisions and references.

## Production

Render's existing `kithkyn-website` static site (`srv-d89gkkjeo5us738rm5o0`) owns
`kithkyn.com` and redirects `www.kithkyn.com` to it. Deployment uses `Quzzar/kithkyn`, branch
`main`, root directory `Website`, build command `bun install --frozen-lockfile && bun run build`,
and publish directory `dist`. Route requests must rewrite to `/index.html` while existing
assets retain their normal URLs. Canonical, Open Graph URL and sharing image use the absolute
`https://kithkyn.com` host.

Keep `public/updates.json` promotions empty while the mod release is a draft. Populate the
released version and its promotions only after the canonical GitHub download is public.
Keep deployment verification and screenshots in `../docs/website-brand.md`.
