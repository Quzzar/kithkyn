# Kithkyn website

The landing page at `/` uses the accepted Hewn Planks wordmark and matching square K.
The artwork uses a real 128 × 34 grid, with a matching reference-edited 32 × 32 icon.
A temporary Minecraft village scene sits behind the desktop header and intro, under a warm-white
wash that fades into the page. On phones, the scene appears below the intro. The desktop
wordmark is 4×, the phone wordmark 2× and header K 1×. The wooden flavor belongs to the logo.
Warm-white, sand and soft tan surfaces pair with dark ink, saddle-brown actions and Outfit.
Semantic color tokens keep hover, focus, borders and captions consistent. Browser controls
use the light scheme. The sharing card uses the same light artwork stage.

`/brand` provides the complete kit, individual PNG and SVG logo downloads, and a sharing card.
The SVG logo containers embed the original pixels, not traced vector paths.

`/brand/directions` compares three new identities designed together with their websites:
Hewn Planks uses joined honey oak lettering and warm light surfaces; Woven uses one interlocking timber K and
ivory lettering over olive charcoal; Cabin Mark builds a K into a little cedar home, over warm
charcoal. Each has a native 128-pixel-wide wordmark and 32 × 32 K. All full previews keep the
17-style village browser, setup tabs and FAQ. `/brand/directions/previous` retains the original
spruce Hewn, Cabin Joinery and Patchwork. PNG downloads and exact generation prompts live in
`public/brand/directions/`.
Hewn Planks is also the main homepage and canonical downloadable brand kit.

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

Three temporary Minecraft scenes replace the old site-review and catalog images; five styles
use material and biome notes. Captions and footer links credit the original galleries. These
photos do not depict Kithkyn catalogs. Replace them with final Kithkyn captures before launch. See `../docs/website-brand.md` for provenance and
publishing status, and `../docs/website-redesign.md` for design decisions and references.

A static host must rewrite page routes to `index.html`. Production deployment waits for the
intended domain and hosting destination. Set an absolute sharing-image URL when that host is known.
