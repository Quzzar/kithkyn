# Wooden title-screen website

Aaron selected direction A on September 30, 2026: build the title-screen composition with
a slapstick, wooden-board feel. This is the active direction. The comparison page and B/C
prototypes are superseded; their source remains recoverable through Git history.

## Design plan

The visitor's job is to understand the autonomous-villager mod, explore its regional villages,
then find installation guidance. A centered wooden title sign and vertical plank menu are the
focal point. The signature is the slightly crooked, tactile plank menu, with a short press wobble.

Color: deep forest (#172d27) surrounds warm oak (#c58a4c), pale painted lettering (#fff3d8),
dark timber (#4f301d), muted leaf (#a9bb94), and paper (#f1e3c5) for setup information.
Type: original outlined pixel lettering for the logo, locally hosted Pixelify Sans for section
heads, and Outfit for readable controls and body copy. Keep the mixed-case name KithKyn.
Layout: scenic title/menu, concise village-life explanation, regional postcard carousel,
then installation and FAQs. Wood is concentrated in the identity, menu and photo frames;
plain forest and paper surfaces keep supporting information quiet.

```text
Minecraft / NeoForge                              Source
+-----------------------------------------------------+
|           [wooden KithKyn title sign]                 |
|             Bringing villages to life                |
|               [Explore villages]                     |
|               [Meet your neighbors]                  |
|               [Installation guide]                   |
|                real village capture                  |
+-----------------------------------------------------+
  village life / work, families, community
  17 styles / horizontal selector + framed postcard
  offline and cloud setup / installation / common questions
  footer / brand kit / source
```

## Reference observations

- [Shopify Editions](https://mobbin.com/sites/sections/47c5f830-b25e-4912-8484-47377cd8a238):
  the scene, title and action form a game entrance. Keep that hierarchy; glowing car effects
  and the purple palette do not fit KithKyn.
- [Lightship](https://mobbin.com/sites/sections/a5fa6838-ff45-42cc-8e90-b513fd69a5e7):
  a centered message can sit above scenery when contrast and foreground/background separation
  are deliberate. KithKyn keeps a compact vertical game menu rather than a standard hero CTA.
- [Going](https://mobbin.com/sites/sections/20111087-8150-4e7e-9d82-c50d0cc7684a):
  oversized identity can carry a scenic entrance. Its capsule-shaped photo and SaaS navigation
  are not the selected game-menu composition.

## Media and identity

Use a real KithKyn village capture from `run/floodplain-site-review/` for the backdrop,
captioned as an in-game site-review capture. It is a still, not an invented gameplay loop.
Inspect catalog captures before using them: the prior library included obstructed camera
views that should become field notes until a useful capture is available.

Create a self-contained vector identity with outlined lettering and a simple wooden sign
emblem, plus primary, reversed, monochrome, wordmark and icon exports. Use code-authored
plank/grain shapes for UI decoration. No generated scene illustration is needed.

## Rejected directions

Hearth and horizon was rejected: the pale green rounded landing page and generated village
diorama did not match Aaron's taste. The illustration, its social-banner derivative and
bundled copies were deleted. Do not restore them. B's dense atlas entrance and C's comic-strip
entrance were passed over when Aaron selected A.

## Verification

Desktop (1280 × 900), mobile (390 × 844) and narrow phone (320 × 720) renders were inspected.
The warm sign and plank controls remain the entrance's focal point, with readable supporting
copy and quiet forest/paper sections. The brand suite was inspected on desktop and mobile.

The first render exposed empty borders from inactive Radix panels. Their hidden state now
removes the inactive frames. The reduced-motion rule also disables each plank's resting tilt
and hover/press transform. Current renders were recaptured after these corrections.

All 18 desktop/mobile Playwright checks pass: all 17 styles, arrows, keyboard focus, shared
selection/history, setup tabs, FAQ disclosure, brand downloads, menu navigation, reduced motion,
loaded images and 320-pixel overflow. Production build, ESLint, Prettier and Gradle
`processResources` pass. Current captures are in `docs/website-preview/`.
The domain/hosting destination remains unresolved; this work is a local preview and draft PR.
