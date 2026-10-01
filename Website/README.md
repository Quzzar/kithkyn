# Kithkyn website

The selected pixel-timber K and three full timber wordmarks are compared at `/`.
Log Lettering, Corner Frame and Oak Frame are numbered 9 through 11. Each opens the
same clean homepage at `/directions/log-lettering`, `/directions/corner-frame`, or
`/directions/oak-frame`. The icon is selected; lettering and framing remain under review.

Every letter uses oak logs matching the selected K. The three treatments compare bare
lettering, open charcoal corners and slim wooden rails around a transparent interior.
The page surfaces and controls stay quiet; the wooden influence is concentrated in the logo.

The pixel-timber icon round remains at `/studies/timber`: Pixel Joinery, Crossgrain, Woodcut
and Peek. The broader first round remains at `/studies/first`: Joinery, Gather, Offcut and
Neighbor. All earlier `/directions/:identityId` previews remain available.

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
`test-results/`. The suite covers all three comparisons, all eleven previews, all 17 village styles, URL history,
keyboard focus, setup tabs, FAQ disclosure, reduced motion, loaded images, typography and
320-pixel layouts.

The old `/play` and `/brand` links redirect to the comparison. `/atlas`, `/stories` and
`/setup` redirect to the corresponding section in the Log Lettering preview. Village selection
remains shareable through `?village=...`.

The old generated village illustration and wooden UI assets are removed. The scene is a real
site-review capture. Twelve catalog images are building-review captures; five styles use
material and biome notes. See `../docs/website-brand.md` for provenance and publishing status,
and `../docs/website-redesign.md` for the popular Minecraft mod logo reference pass.

The transparent PNG lockups are raster concepts. Exact generation prompts are saved in
`public/studies/prompts.md`, `public/studies/pixel-timber-prompts.md` and
`public/studies/log-lettering-prompts.md`. A final vector suite and sharing graphics follow
wordmark selection.

A static host must rewrite page routes to `index.html`. This version is a local preview
and draft PR, not a production deployment.
