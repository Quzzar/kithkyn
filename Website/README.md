# KithKyn website

The new Hearth and horizon website: original illustrated introduction, a shareable atlas of
all 17 bundled village styles, installation guidance and a full brand-kit page at `/brand`.
React, Vite, strict TypeScript, vanilla-extract, Radix, nuqs and Playwright, using Bun.

## Development

```sh
bun install
bun run dev
```

The preview runs at `http://127.0.0.1:45173`.

```sh
bun run build
bun run lint
bun run format:check
bun run test:e2e
```

Tests run at port 45175 on desktop and mobile Chromium. Full-page captures are saved in
`test-results/`. A static host must rewrite page routes such as `/brand` to `index.html`.
Domain/hosting destination is unresolved; this branch does not replace either live domain.

## Brand suite

`public/brand/` contains the primary, reversed, monochrome, wordmark and emblem assets,
favicon/touch/app icons, social banner, usage notes and generation prompts. These are
transparent PNG originals and optimized WebP exports, not vectors. Local font licenses
are in `public/fonts/`. The village diorama is original promotional artwork, and real
catalog screenshots are identified separately on the website.

To rebuild the downloadable kit after changing assets:

```sh
zip -j public/brand/kithkyn-brand-kit.zip public/brand/*.png public/brand/*.webp \
  public/brand/README.md public/brand/generation-prompts.md public/art/*.webp \
  public/fonts/*
```

The kit contains branding, not the mod. Public mod download destinations stay marked
coming soon until real project pages exist. See `../docs/website-brand.md` for details.
