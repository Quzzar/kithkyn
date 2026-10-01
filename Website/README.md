# Kithkyn website

Four pixel-timber icon and wordmark studies are compared at `/`, numbered 5 through 8.
Each opens a complete homepage at `/directions/pixel-joinery`, `/directions/crossgrain`,
`/directions/woodcut`, or `/directions/peek`. No final identity has been selected.

Pixel Joinery joins three oak planks into a K. Crossgrain offsets the planks around a peg.
Woodcut stamps a K into a wooden tile. Peek adds a tiny villager behind the timber.
The page surfaces and controls stay quiet; the wooden influence is concentrated in the logo.

The first round remains available at `/studies/first`: Joinery, Gather, Offcut and Neighbor.
Their previews remain at `/directions/joinery`, `/directions/gather`, `/directions/offcut`,
and `/directions/neighbor`. Both rounds use the same four page compositions for context.

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
`test-results/`. The suite covers both comparisons, all eight previews, all 17 village styles, URL history,
keyboard focus, setup tabs, FAQ disclosure, reduced motion, loaded images, typography and
320-pixel layouts.

The old `/play` and `/brand` links redirect to the comparison. `/atlas`, `/stories` and
`/setup` redirect to the corresponding section in the Pixel Joinery preview. Village selection
remains shareable through `?village=...`.

The old generated village illustration and wooden UI assets are removed. The scene is a real
site-review capture. Twelve catalog images are building-review captures; five styles use
material and biome notes. See `../docs/website-brand.md` for provenance and publishing status,
and `../docs/website-redesign.md` for the popular Minecraft mod logo reference pass.

The transparent PNG lockups are raster concepts. Exact generation prompts are saved in
`public/studies/prompts.md` and `public/studies/pixel-timber-prompts.md`. A final vector suite,
icons and sharing graphics follow selection.

A static host must rewrite page routes to `index.html`. This version is a local preview
and draft PR, not a production deployment.
