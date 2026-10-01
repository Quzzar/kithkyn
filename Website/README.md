# KithKyn website

Direction A is the chosen website: a wooden game title screen over a real Minecraft village
capture, with a plank menu, village-life introduction, all 17 regional style catalogs, and
installation guidance. The matching vector logo suite is shown at `/brand` and available
as `/brand/kithkyn-brand-kit.zip`.

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
`test-results/`. The suite covers menu navigation, all styles, URL history, keyboard
selection, setup tabs, FAQ disclosure, the brand download, reduced motion and narrow layouts.

The former `/play`, `/atlas`, `/stories` and `/setup` links redirect to the corresponding
homepage section. Village selection remains shareable through `?village=...`.

The old generated village illustration was removed. The title backdrop is a real site-review
capture. Twelve catalog images are building-review captures; five styles use material and
biome notes. See `../docs/website-brand.md` for provenance and publishing status.

A static host must rewrite page routes to `index.html`. This version is a local preview
and draft PR, not a production deployment.
