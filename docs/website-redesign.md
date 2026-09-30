# Hearth and horizon

The September 30, 2026 brief authorizes a completely new KithKyn website and identity.
This replaces the timber title-screen direction. The spelling stays KithKyn, while the
Minecraft mod ID and repository stay kithkyn.

## Visual plan

The page introduces autonomous village life to Minecraft players, then helps them explore
the 17 bundled regional catalogs and understand installation. The village diorama is the
single signature element.

Color: mist (#eef3ed) for the page, pine (#23483f) for type and strong surfaces, meadow
(#9cba9e) for quiet fields, honey (#f5c45b) for the primary action, sky (#cddfe7) for the
atlas, and white for reading surfaces. These are named tokens, not component values.

Type: Bricolage Grotesque for bold, tightly set display text; Outfit for body and controls.
Keep useful labels small, sentence case, and plainly legible.

Layout: an asymmetrical illustrated introduction, a short account of village life, one
interactive 17-style atlas, model/setup guidance, and a compact footer.

```
brand                 villages / life / get started
large promise         original village diorama
primary action        illustration caption
village life          actual catalog previews
style selector        selected village field notes
offline / cloud       installation and common questions
brand                 source / credits / brand kit
```

The emblem shows three homes gathered around one hearth. The suite includes the horizontal
lockup, wordmark, standalone emblem, reversed and monochrome lockups, icon sizes, social
artwork and a downloadable kit. The generated logo originals remain raster artwork.

## References and rejected directions

The Mobbin reference pass examined [basement.studio](https://mobbin.com/sites/sections/28a88d90-f813-478b-bb89-336dbe1e48a0),
[Shopify Editions](https://mobbin.com/sites/sections/47c5f830-b25e-4912-8484-47377cd8a238),
[Chronicle](https://mobbin.com/sites/sections/6985d6b3-5a76-490b-8082-b723bdb5d232) and
[Lightship](https://mobbin.com/sites/sections/a5fa6838-ff45-42cc-8e90-b513fd69a5e7).
Carry forward a single immersive subject, compact navigation, a clear primary action and
progressive disclosure of supporting information. Reject dark wireframes, purple lighting,
presentation-software language and the previous centered scenic title screen.

Also reject cream/serif/terracotta editorial styling, decorative numbered feature cards,
gradient type, and a generic technology dashboard. This is a village game.

## Media and release truth

The hero is original generated promotional illustration, explicitly captioned as such.
Catalog screenshots are actual review-world captures, identified as building previews.
They are not natural-terrain or autonomous-growth evidence. No borrowed Valecraft footage
remains. The 17-style roster follows docs/village-biomes.md. Ocean and Nether systems are
future work and are not advertised as included.

Public Modrinth and CurseForge projects remain unverified. Keep those destinations marked
coming soon. Source, installation documentation and issue links are real GitHub URLs.
Deployment awaits a resolved domain/hosting destination: the checked live kithkyn.com is
currently a hiring-product page, and kithkin.com redirects to /lander.

## Verification

Run the website build, lint, formatting and desktop/mobile Playwright checks. Inspect
fresh captures at both widths, including atlas selection, setup tabs, FAQ expansion and
the brand-kit route. Check keyboard focus, reduced motion, image loading, contrast,
console errors and horizontal overflow. Record the final results here before delivery.

### Final review

The production build, lint and formatting checks pass. All 22 desktop/mobile Playwright
checks pass, including the 17-catalog loop, URL history, keyboard focus, FAQ disclosure,
offline/cloud setup, ZIP download and section navigation from the brand page. Both page
routes load without JavaScript errors or horizontal overflow. Reduced motion is verified.

Fresh desktop and 390-pixel mobile captures were inspected. The review fixed the oversized
inline footer logo, an over-wide hero illustration, page scroll restoration and section
navigation. The atlas title was shortened to hold two clear lines, the redundant hero setup
action was removed, and the mobile selector was bounded so its chosen preview stays close.
The final primary, reversed, monochrome, emblem and wordmark gallery was inspected together.
Screenshot samples live in docs/website-preview/; complete automated captures are ignored
under Website/test-results/. No production domain was modified.
