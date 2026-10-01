import { copyFileSync, existsSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { BRAND_ICON, BRAND_PALETTE, BRAND_WORDMARK, type BrandAsset } from "../src/data/brand";

/** Resolve checked-in brand files without depending on the shell's working directory. */
function brandPath(name: string): string {
  return fileURLToPath(new URL(`../public/brand/${name}`, import.meta.url));
}

/** SVG containers preserve the native PNG and its nearest-neighbor display rule. */
function artworkSvg(asset: BrandAsset): string {
  const encoded: string = readFileSync(
    fileURLToPath(new URL(`../public${asset.source}`, import.meta.url)),
  ).toString("base64");
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${String(asset.width)}" height="${String(asset.height)}" viewBox="0 0 ${String(asset.width)} ${String(asset.height)}" role="img" aria-label="Kithkyn ${asset.name.toLowerCase()}">
  <image width="${String(asset.width)}" height="${String(asset.height)}" style="image-rendering:pixelated" href="data:image/png;base64,${encoded}"/>
</svg>\n`;
}

const wordmark: string = readFileSync(brandPath("wordmark.png")).toString("base64");
const font: string = readFileSync(
  new URL("../public/fonts/outfit.woff2", import.meta.url),
).toString("base64");
writeFileSync(brandPath("wordmark.svg"), artworkSvg(BRAND_WORDMARK));
writeFileSync(brandPath("icon.svg"), artworkSvg(BRAND_ICON));
writeFileSync(
  brandPath("social.svg"),
  `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630" role="img" aria-label="Kithkyn: a world with neighbors">
  <style>@font-face{font-family:Outfit;src:url(data:font/woff2;base64,${font}) format('woff2');font-weight:100 900}text{font-family:Outfit,sans-serif}</style>
  <rect width="1200" height="630" fill="${BRAND_PALETTE.canvas}"/>
  <text x="600" y="66" text-anchor="middle" font-size="21" font-weight="500" fill="${BRAND_PALETTE.oak}">Autonomous villagers for Minecraft</text>
  <image x="136" y="112" width="928" height="384" style="image-rendering:pixelated" href="data:image/png;base64,${wordmark}"/>
  <text x="600" y="570" text-anchor="middle" font-size="46" font-weight="500" fill="${BRAND_PALETTE.ink}">A world with neighbors.</text>
</svg>\n`,
);
writeFileSync(brandPath("palette.json"), `${JSON.stringify(BRAND_PALETTE, null, 2)}\n`);
copyFileSync(
  new URL("../public/fonts/outfit-LICENSE.txt", import.meta.url),
  brandPath("outfit-LICENSE.txt"),
);
writeFileSync(
  brandPath("README.md"),
  `# Kithkyn brand kit

Refined on October 1, 2026: unframed timber lettering with a fuller ith and a cleaned capital K.
The square icon is the first K extracted from that same master.

- wordmark.png: transparent 116 × 48 native pixel master.
- icon.png: transparent 32 × 32 native square K.
- wordmark-464.png and wordmark-928.png: exact 4× and 8× pixel copies.
- icon-64.png, icon-128.png, icon-256.png and icon-512.png: exact integer-scaled square exports.
- wordmark.svg and icon.svg: SVG containers embedding the native PNGs with pixelated rendering.
- social.jpg: 1200 × 630 sharing card, captured from social.svg.
- social.svg: sharing card with the native wordmark and embedded Outfit font.
- palette.json: the website's slate and oak colors.
- generation-prompts.md: the exact original built-in image-generation prompts.
- outfit-LICENSE.txt: the sharing card font's license.

## Usage

Use the full wordmark when the name needs to be read. Use the K icon in compact spaces.
Keep the colors, proportions and transparent clear space. Use a light, quiet background
so the charcoal outline remains readable. Do not put the wordmark on a wood texture.
Use whole-number enlargement with nearest-neighbor sampling. In CSS, set image-rendering: pixelated.

The edited 1950 × 807 source was center-sampled at the visual block cadence, with binary alpha.
The PNG masters have one actual image pixel per grid cell. The SVG containers embed these pixels.
The sharing card displays the wordmark at exactly 8×, with native SVG text and layout.
`,
);

const names: readonly string[] = [
  "wordmark.png",
  "icon.png",
  "wordmark-464.png",
  "wordmark-928.png",
  "icon-64.png",
  "icon-128.png",
  "icon-256.png",
  "icon-512.png",
  "wordmark.svg",
  "icon.svg",
  "social.svg",
  "palette.json",
  "README.md",
  "generation-prompts.md",
  "outfit-LICENSE.txt",
];
const files: string[] = names.map((name: string): string => brandPath(name));
if (existsSync(brandPath("social.jpg"))) files.push(brandPath("social.jpg"));
const archive: string = brandPath("kithkyn-brand-kit.zip");
rmSync(archive, { force: true });
execFileSync("zip", ["-j", "-q", archive, ...files], { stdio: "inherit" });
console.log("Exported the chosen brand assets and kit.");
