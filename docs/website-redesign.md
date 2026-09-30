# Website direction exploration

The September 30, 2026 **Hearth and horizon** proposal was rejected. Aaron disliked the
overall visual direction and explicitly asked to delete the generated village diorama.
Do not reuse that artwork or its social-banner derivative. The pale green rounded landing
page is not an approved direction. The previous logo assets are provisional, not selected.

The next deliverable is three deliberately low-fidelity, structurally different concepts.
Aaron chooses a direction before finished artwork, a new logo suite or production polish.
All three explain the same Minecraft mod: autonomous villagers building their communities.
Gray boxes stand for future real game captures or commissioned artwork; they are not footage.

## Reference observations

- [Shopify Editions](https://mobbin.com/sites/sections/47c5f830-b25e-4912-8484-47377cd8a238):
  a single scene, title and central action can make an entry screen feel like a game.
  Its glowing car and purple palette do not belong to this mod.
- [GetYourGuide map browser](https://mobbin.com/screens/7d8762de-2deb-4393-8667-dd8751d3b206):
  a persistent index beside a large detail surface lets exploration lead the experience.
  Real-world maps, price labels and crowded pins would misrepresent a Minecraft catalog.
- [Duolingo story section](https://mobbin.com/sites/sections/c7d4c0e0-4225-454a-85f0-e0386c7ff191):
  characters and a small story scene establish personality before explaining mechanics.
  Its finished mascot art is not a substitute for KithKyn's actual villagers.

## A — Title screen (`/play`)

Color: charcoal scene, white title, gray menu and quiet blue-gray lines.
Type: large, widely spaced Outfit title; restrained body copy; monospace capture annotations.
Layout: one full-width gameplay area, centered name and vertical menu, then a short scene strip.
Signature: the website opens like the entrance to a game.

```text
review navigation
+----------------------------------------------------+
|           [actual gameplay loop, later]             |
|                    KITHKYN                          |
|            A village with its own life              |
|                [Explore villages]                   |
|                [Installation guide]                 |
+----------------------------------------------------+
  first fire           first home          community
```

## B — Village atlas (`/atlas`)

Color: white canvas, pale gray drafting surface, charcoal text, muted blue selection.
Type: Outfit for catalog names and description; monospace for index and materials.
Layout: compact masthead, all 17 styles in a persistent index, generous selected-style preview.
Signature: browse the worlds immediately, with no conventional marketing hero.

```text
review navigation
KithKyn / Village atlas                     Setup
Where will they make a home?
+-----------------+----------------------------------+
| 17-style index  | [large architecture placeholder]  |
| selected style  |                                  |
|                 | Selected village / biome          |
|                 | Materials and regional character |
+-----------------+----------------------------------+
```

## C — Village stories (`/stories`)

Color: white page, ink-gray frames, light gray illustration boxes, one muted blue accent.
Type: Georgia headline gives the page a storybook voice; Outfit captions and controls.
Layout: broad masthead followed by an asymmetric comic strip and a full-width final scene.
Signature: framed moments and sample dialogue introduce individual villagers before buildings.

```text
review navigation
KithKyn                              Life / Places / Setup
          Meet your new neighbors.
+-----------------------+----------------------------+
| neighbor portrait     | building scene              |
| [sample dialogue]     | [sample dialogue]            |
+-----------------------+----------------------------+
|                a community grows                    |
+----------------------------------------------------+
             [Explore all 17 styles]
```

These are wireframes, not three color variations of one layout. No concept is chosen yet.
Final palette, lettering, artwork, motion and brand exports wait for that choice.

## Verification

Render each direction at desktop and mobile widths. Inspect hierarchy, navigation, caption
placement, keyboard selection, image loading and horizontal overflow. Save current captures
in `docs/website-preview/`; remove the rejected homepage captures. The local preview stays
available for side-by-side review. Do not publish or merge the exploratory designs.

## Render review — September 30, 2026

All three concepts were inspected at desktop and mobile widths. The centered title/menu,
persistent regional index, and asymmetric story frames remain distinct on the rendered pages.
The comparison page uses actual rendered concept captures, with equal thumbnail sizes.

A 320-pixel phone check exposed the inherited body minimum width and an oversized title.
Removing that minimum and scaling the mobile title fixed the measured horizontal overflow.
The corrected title screen was re-rendered at narrow and regular phone widths; the atlas
was recaptured after its final token change. Build, lint, formatting and all 16 desktop/mobile
Playwright scenarios pass, including every catalog selection and a 320-pixel overflow check
for each route. No direction has been selected.
