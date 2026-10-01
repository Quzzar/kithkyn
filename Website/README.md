# Kithkyn website

Four original icon and wordmark studies are compared at `/`. Each opens a complete, usable
homepage at `/directions/joinery`, `/directions/gather`, `/directions/offcut`, or
`/directions/neighbor`. No final identity has been selected.

Joinery uses an interlocking K and a split layout. Gather uses two homes and a centered
composition. Offcut uses a timber K with hand-cut lettering and an architectural composition.
Neighbor uses a peeking villager and a playful reversed layout. The page surfaces and controls
stay quiet; the wooden influence is concentrated in the logo.

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
`test-results/`. The suite covers all four previews, all 17 village styles, URL history,
keyboard focus, setup tabs, FAQ disclosure, reduced motion, loaded images, typography and
320-pixel layouts.

The old `/play` and `/brand` links redirect to the comparison. `/atlas`, `/stories` and
`/setup` redirect to the corresponding section in the first preview. Village selection
remains shareable through `?village=...`.

The old generated village illustration and wooden UI assets are removed. The scene is a real
site-review capture. Twelve catalog images are building-review captures; five styles use
material and biome notes. See `../docs/website-brand.md` for provenance and publishing status,
and `../docs/website-redesign.md` for the popular Minecraft mod logo reference pass.

The transparent PNG lockups are raster concepts. Exact generation prompts are saved in
`public/studies/prompts.md`. A final vector suite, icons and sharing graphics follow selection.

A static host must rewrite page routes to `index.html`. This version is a local preview
and draft PR, not a production deployment.
