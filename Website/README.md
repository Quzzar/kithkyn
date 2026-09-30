# KithKyn website design study

Three low-fidelity directions are available from the comparison page at `/`:

- `/play`: a game title screen, with a future real gameplay loop.
- `/atlas`: an architecture browser, retaining all 17 catalogs and shareable selection.
- `/stories`: a comic-style introduction to individual villagers and village life.

The real installation requirements remain available at `/setup`. The previous generated
village illustration and its social-banner derivative were rejected and removed, including
copies inside the downloadable brand kit. The old logo exports are provisional assets;
the final logo suite follows the user's choice of direction. No concept is selected yet.

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
`test-results/`. A static host must rewrite page routes to `index.html`. This study is
not deployed to either live domain. See `../docs/website-redesign.md` for the directions
and the rejected-design record; `../docs/website-brand.md` records media provenance.
