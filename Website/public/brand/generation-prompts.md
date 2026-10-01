# Selected brand generation prompts

The original Corner Frame source is preserved at Git commit fc64070. The native-grid preparation is preserved at 7725c93. The current refinement uses the built-in image editor to remove framing, clean the K and blend in the earlier study's ith. Shipping masters then use deterministic native-grid sampling and K extraction, documented in docs/website-brand.md.

## Pixel Joinery reference

Use case: logo-brand. Create one NEW original logo study for Kithkyn, an independent Minecraft mod about villagers building their own settlements. Input image 1 is a lettering/personality reference: use its confident soft rounded lettering as a starting point, not its flat doorway icon. Input image 2 is a material/reference for a K assembled from timber: replace its smooth illustrative wood treatment with crisp deliberate PIXEL ART. Exact text: "Kithkyn", K-i-t-h-k-y-n. One horizontal icon-plus-wordmark lockup only, compact square K icon on the left, beautifully kerned wordmark on the right. Genuine transparent background, tight but comfortable padding. The icon must look like a polished low-resolution game sprite, with a consistent coarse pixel grid, staircase diagonals and a 5-color wood palette: dark charcoal outline, brown shadow, mid oak, honey highlight, pale end grain. Clearly visible blocky pixels even when the logo is small. No smooth curves in the icon, no blur, no dithering, no realistic grain, no glossy gradients, no drop shadow around the whole canvas. Limited sparse rectangular grain marks; keep broad readable forms. The mood is lightly slapstick and handmade, but polished. The wordmark is simpler and smoother than the icon: original rounded bold lowercase letters with a capital K, pale warm ivory fill and a restrained dark slate outline, comparable in finish and readability to strong indie Minecraft mod branding. Do not copy Cobblemon, Pokemon, or the Minecraft title lettering. No scene, no plaque behind the name, no ropes, no slogan, no extra text or duplicate sample logos. This is logo exploration, not a mockup. Variation: a precise K made from three pixel-art oak planks. The vertical upright and the two diagonal arms meet at one tidy mortise-like center join. The diagonals use an obvious consistent stepped pixel contour. One arm projects a single pixel farther than expected for a quiet handmade joke. Flat front view, strong silhouette, no metal bolts. Keep the wordmark calm, rounded and tightly composed.

## corner-frame

```text
Use case: logo-brand.
Asset type: original transparent Kithkyn wordmark for a clean Minecraft mod website.
Input image: edit target. The existing pixel oak K is the approved icon. Preserve its three joined planks, silhouette, charcoal stepped outline, honey highlights and sparse wood grain.
Primary request: replace the smooth ivory lettering with the full name made from matching pixel wooden logs. The approved timber K should become the first letter of the name, followed directly by i t h k y n; do not show a separate K icon and another initial K.
Text (verbatim): "Kithkyn", exactly seven characters, spelled K-i-t-h-k-y-n. Capital K followed by lowercase ithkyn.
Style: deliberate pixel-game lettering with a coherent pixel grid. Each stroke is a joined oak timber with light end grain, warm golden oak faces, two restrained brown grain marks and a charcoal outline. Readable letter silhouettes, clean open counters, comfortable spacing and one baseline. The dot of the i is a small square oak block. The y has a clear descender.
Constraints: every letter is wooden, with the same material and light direction as the approved K. No ivory letter fills. No backdrop or white fringe. No scenery, mascots, slogans, Minecraft trademark font or extra text. Actual transparent alpha outside the logo and inside letter counters. Keep edges crisp and use few broad texture marks so it survives a header size.
Composition: one centered horizontal wordmark, generous transparent breathing room, no other studies or mockup labels.
Framing version: a quiet open corner frame. Add only four short, thin stepped charcoal corner brackets around the full timber wordmark. Leave the middle of every frame side open. Brackets use the same charcoal as the letter outline, are visually much lighter than the letters and have generous air between them and the name. No filled panel, complete border or extra wood rails.
```

## Unframed lettering refinement

Built-in edit. Inputs: the selected Corner Frame source, the earlier bare Log Lettering study
and the 8× native-grid preview. Output: `exec-86a23e89-1af5-4e01-843b-7475134b974d.png`.

```text
Use case: precise-object-edit and compositing.
Asset: refined transparent pixel-oak Kithkyn wordmark.
Image 1 is the PRIMARY EDIT TARGET: the owner's selected Corner Frame wordmark.
Image 2 is a LETTERING INSERT REFERENCE: use its lowercase i, t, h shapes only.
Image 3 shows the required coarse native pixel granularity for the shipping mark.
Make ONE horizontal wordmark reading exactly "Kithkyn", K-i-t-h-k-y-n, seven letters. Do not add any other marks or lettering.
Edits requested:
1. Remove all four charcoal corner brackets entirely. There must be no frame, corner ticks, rails or backdrop.
2. On the first capital K, tidy the bottom tip of its lower diagonal plank. Remove the small hanging brown downward protrusion at the tip. Give the lower arm a single clean stepped end-grain cap perpendicular to the diagonal. Preserve the capital K's three-plank construction, size, angle and silhouette everywhere else. No little tail, tab, notch or dangling pixel below its lower end.
3. Replace ONLY the lowercase "ith" letter construction with the fuller taller "ith" from image 2: the larger block dot and taller upright i, the broad horizontal timber crossbar and hooked foot of t, and the taller upright and open counter of h. Match material colors and pixel cadence to image 1. The h must be readable as h.
4. Keep the lowercase k, y, n from image 1, especially its blocky y and broad n. Preserve their shapes, proportions, spacing and warm oak appearance. Retain the chosen capital K except for the cleaned tip.
Preserve original golden oak, pale end grain, dark charcoal stepped outline and sparse brown rectangular grain. Consistent coarse pixel-grid construction, staircase contours, broad rectangular color regions. No smooth illustrated redraw, blur, dithering, glossy shading or soft edges. Aim for roughly a 110-pixel-wide native sprite enlarged into the output, with each logical block clear. One coherent pixel granularity across the full name.
Actual transparent background outside the logo and inside counters. Comfortable even transparent padding. Do not stretch or clip the name. No scene, mockup, alternate options, border, badges, plaque, mascot, slogan or extra text.
Change only the described regions. The purpose is precise refinement of the supplied identity, not a new logo design.
```

## Capital K cleanup

Built-in edit of the preceding output. Final source:
`exec-f4a80f59-372f-4c7b-9ed6-ef5ccd0dd074.png`, 1950 × 807.

```text
Precise-object-edit of the supplied transparent pixel-oak wordmark. Keep all lettering "Kithkyn", all colors, positions, sizes, grain and pixel edges unchanged except the TWO small extra protrusions at the bottom of the FIRST capital K.
This is cleanup, not a redesign.
A. The vertical upright of the first capital K currently has a little brown block/peg projecting to the RIGHT near its foot. Remove that protruding right-side block. Make the upright a clean straight vertical plank down to a tidy flat bottom, with no side nub or foot.
B. The lower diagonal arm of that same first capital K has a brown vertical tab dropping below the diagonal near its pale end-grain tip. Trim that dangling brown tab completely. The end should read as the clean flat end of one diagonal wooden plank, with a compact pale end-grain face and regular stepped charcoal contour. No downward extension beyond the clean diagonal plank silhouette. Preserve the lower arm's angle and thickness.
Do not modify the six lowercase letters i t h k y n or the upper K arm. Do not change kerning or add framing. No brackets, border, plaque or corner marks.
Keep the same coarse pixel-art style and hard transparent alpha. Broad oak colors with dark charcoal outline; no blur or smooth repaint. Entire background and counters must remain truly transparent. Same canvas dimensions and same overall position as the supplied edit target.
```
