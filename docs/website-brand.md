# Website and brand

The active website lives in `Website/`. The September 30, 2026 redesign replaces the previous
timber title-screen identity with **Hearth and horizon**, a new direction requested by Aaron.
Display the brand as **KithKyn**. The repository and Minecraft mod ID remain `kithkyn`.

## Identity and assets

Three homes gather around a gold hearth flame. The horizontal wordmark uses rounded, compact
lettering. Pine, mist, meadow, honey and sky carry the identity. Bricolage Grotesque is the
display face; Outfit is the body face. Both fonts are locally hosted with their OFL licenses.

The suite in `Website/public/brand/` includes primary, reversed, monochrome, standalone
wordmark and emblem PNG originals, optimized lossless WebP counterparts, icons at 32, 64,
180 and 512 pixels, a 1200×630 social banner, usage notes and a downloadable ZIP. The `/brand`
route is the visual brand guide and the gallery for every `LogoMark` variant. Assets are
raster originals, not vector exports. Preserve their alpha and aspect ratio.

The artwork was created with the built-in image-generation tool. Exact prompts and provenance
are in `Website/public/brand/generation-prompts.md`; usage instructions are in that folder's
README. The new village illustration is in `Website/public/art/village-diorama.webp`. The
same hearth emblem is used as the mod-list logo in `src/main/resources/kithkyn-logo.png`.
No borrowed Valecraft video, poster or superseded timber-brand explorations are distributed.

## Website behavior

The homepage introduces village life, then offers a 17-style atlas, offline/cloud requirements,
real installation guidance and expandable common questions. The atlas uses Radix tabs with
keyboard navigation. Its selected catalog is shareable through the `village` URL parameter,
managed by nuqs, and restored by browser back/forward. Unknown values show Mediterranean.
The model comparison and FAQs use Radix tabs and accordion primitives. All styles use the
shared vanilla-extract tokens; reduced motion disables smooth scrolling and transitions.

The roster follows [village-biomes.md](village-biomes.md). Swamp and Floodplain are separate;
the latter belongs to mangrove biomes. Ocean and Nether settlement systems remain future work.
Modrinth and CurseForge remain **Coming soon** until actual project pages exist. Development,
installation, source, issue and credit destinations point to the real GitHub repository.

## Media provenance

The generated hero and social banner are promotional illustration, not gameplay captures.
The homepage says so beneath its artwork. Building previews are actual review-world images,
captioned as in-game catalog previews rather than naturally developed villages.

Thirteen catalog images are included. Four catalogs (Japanese Cherry Grove, Nautical Coast,
Savanna Tent and Mushroom) have material/biome field notes instead of an invented screenshot.
The capture sources, relative to the original project root, are:

| Catalog | Source |
| --- | --- |
| Mediterranean | `docs/release-gallery/mediterranean-homes.png` |
| Jungle Tribal | `docs/release-gallery/jungle-homes.png` |
| Swamp | `docs/release-gallery/swamp-homes.png` |
| Pueblo | `docs/release-gallery/badlands-center.png` |
| Tundra | `docs/release-gallery/tundra-walls.png` |
| Birch Forest | `run/full-style-showcase/client/screenshots/approved-birch-reference.png` |
| Rustic Woodland | `run/full-style-showcase/client/screenshots/rustic-complete-barns.png` |
| Taiga | `run/viking-full-profile/render/client/screenshots/viking-full-homes.png` |
| Romanian | `run/romanian-full-profile/render/client/screenshots/romanian-center-and-small-homes.png` |
| Desert Oasis | `run/pueblo-showcase/desert-center-approved-20260909-224500/screenshots/client/screenshots/desert-center-overview.png` |
| Floodplain | `run/nilotic-showcase/screenshots/client/screenshots/nilotic-houses.png` |
| Alpine Highlands | `run/next-village-preview/client/screenshots/alpine_highlands.png` |
| Polynesian Coast | `run/jungle-showcase/screenshots/client/screenshots/polynesian-center.png` |

These are composition/catalog previews; some review images contain donor rows in the background.
Use their captions, and do not present them as a production village's growth timeline.

## Deployment and verification

This redesign is a local working website, not a production deployment. At the September 30
check, `kithkyn.com` served a hiring product and `kithkin.com` redirected to `/lander`.
Resolve the intended domain and hosting account before publishing. Update the social-image
absolute URL in `index.html` if the domain changes. Static hosts must rewrite `/brand` to
`index.html`, while serving assets normally.

From `Website/`, run `bun run build`, `bun run lint`, `bun run format:check` and
`bun run test:e2e`. The preview runs on 45173; tests run on 45175. Tests cover desktop and
mobile layouts, all 17 selections, deep links and browser history, keyboard focus, setup
choices, FAQ disclosure, real destination URLs, brand-kit downloads, image loading, reduced
motion, console errors and overflow. Full-page captures are written under `test-results/`.

See [website-redesign.md](website-redesign.md) for the visual plan and final visual review.
