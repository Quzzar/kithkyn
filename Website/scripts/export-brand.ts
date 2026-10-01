import { copyFileSync, existsSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { BRAND_ICON, BRAND_PALETTE, BRAND_WORDMARK, type BrandAsset } from "../src/data/brand";

/** Resolve checked-in brand files without depending on the shell's working directory. */
function brandPath(name: string): string {
  return fileURLToPath(new URL(`../public/brand/${name}`, import.meta.url));
}

/** SVG containers embed the original PNG; they do not repaint or trace the artwork. */
function artworkSvg(asset: BrandAsset): string {
  const encoded: string = readFileSync(
    fileURLToPath(new URL(`../public${asset.source}`, import.meta.url)),
  ).toString("base64");
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${String(asset.width)}" height="${String(asset.height)}" viewBox="0 0 ${String(asset.width)} ${String(asset.height)}" role="img" aria-label="Kithkyn ${asset.name.toLowerCase()}">
  <image width="${String(asset.width)}" height="${String(asset.height)}" href="data:image/png;base64,${encoded}"/>
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
  <image x="120" y="108" width="960" height="399" href="data:image/png;base64,${wordmark}"/>
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

Selected on October 1, 2026: the Corner Frame wordmark and the Pixel Joinery K icon.

- wordmark.png: original transparent 1945 × 809 PNG master.
- icon.png: original transparent 1254 × 1254 PNG master.
- wordmark.svg and icon.svg: self-contained SVG containers embedding the original PNGs.
- social.jpg: 1200 × 630 sharing card, captured from social.svg.
- social.svg: sharing card with the original wordmark and embedded Outfit font.
- palette.json: the website's slate and oak colors.
- generation-prompts.md: the exact original built-in image-generation prompts.
- outfit-LICENSE.txt: the sharing card font's license.

## Usage

Use the full wordmark when the name needs to be read. Use the K icon in compact spaces.
Keep the colors, proportions and transparent clear space. Use a light, quiet background
so the charcoal outline remains readable. Do not put the wordmark on a wood texture.

These are pixel-art raster masters. The SVG logo containers are not traced vector paths.
The sharing card uses the same artwork unchanged, with native SVG text and layout.
`,
);

const names: readonly string[] = [
  "wordmark.png",
  "icon.png",
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
