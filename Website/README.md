# Kithkyn website

The landing page at `/` uses the refined unframed timber wordmark and its square K.
The artwork uses a real 116 × 48 grid, with a 32 × 32 icon extracted from the same master. The website keeps a clean slate canvas, Outfit
typography and real game captures; the wooden flavor belongs to the logo.

`/brand` provides the complete kit, individual PNG and SVG logo downloads, and a sharing card.
The SVG logo containers embed the original pixels, not traced vector paths.

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

Tests run at port 45175 on desktop and mobile Chromium. The suite covers the chosen identity,
brand downloads, all 17 village styles, URL history, keyboard focus, setup tabs, FAQ disclosure,
reduced motion, loaded images, typography and 320-pixel layouts. Captures are saved in
`test-results/`; reviewed captures live in `../docs/website-preview/`.

Old `/play`, `/directions/:identityId` and `/studies/:round` links return to the homepage.
`/atlas`, `/stories` and `/setup` open the corresponding home section. Redirects preserve query
parameters, and village selection remains shareable through `?village=...`.

## Brand exports

`public/brand/wordmark.png` and `icon.png` are the native pixel masters. Larger exports use
whole-number copies of each pixel. The 128-pixel K supplies the mod-list icon. The site uses
pixelated rendering and integer display scales. Sampling details and source provenance are
recorded in `../docs/website-brand.md`; original generation prompts are saved beside the assets.

```sh
bun run brand:export
```

The script embeds the original pixels in SVG containers, builds a sharing SVG with its font,
and packages the kit. When changing the sharing SVG, capture it at 1200 × 630 in the browser,
save the browser's JPEG as `public/brand/social.jpg`, inspect it, then rerun the export to include
the updated card. Keep the extension consistent with the actual browser output.

The scene is a real site-review capture. Twelve catalog images are building-review captures;
five styles use material and biome notes. See `../docs/website-brand.md` for provenance and
publishing status, and `../docs/website-redesign.md` for design decisions and references.

A static host must rewrite page routes to `index.html`. Production deployment waits for the
intended domain and hosting destination. Set an absolute sharing-image URL when that host is known.
