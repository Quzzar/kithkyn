import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { BRAND_ICON, BRAND_PALETTE, BRAND_WORDMARK, type BrandArtwork } from "../src/data/brand";

/** Resolve website artwork without depending on the shell’s working directory. */
function brandPath(name: string): string {
  return fileURLToPath(new URL(`../public/brand/${name}`, import.meta.url));
}

/** The favicon embeds the already-corrected native pixels without resampling. */
function artworkSvg(asset: BrandArtwork, label: string): string {
  const encoded: string = readFileSync(
    fileURLToPath(new URL(`../public${asset.source}`, import.meta.url)),
  ).toString("base64");
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${String(asset.width)}" height="${String(asset.height)}" viewBox="0 0 ${String(asset.width)} ${String(asset.height)}" role="img" aria-label="${label}">
  <image width="${String(asset.width)}" height="${String(asset.height)}" style="image-rendering:pixelated" href="data:image/png;base64,${encoded}"/>
</svg>\n`;
}

const wordmark: string = readFileSync(brandPath("wordmark.png")).toString("base64");
const font: string = readFileSync(
  new URL("../public/fonts/outfit.woff2", import.meta.url),
).toString("base64");
const width: number = BRAND_WORDMARK.width * 8;
const height: number = BRAND_WORDMARK.height * 8;
writeFileSync(brandPath("icon.svg"), artworkSvg(BRAND_ICON, "Kithkyn K icon"));
writeFileSync(
  brandPath("social.svg"),
  `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630" role="img" aria-label="Kithkyn: Autonomous Villages for Minecraft">
  <style>@font-face{font-family:Outfit;src:url(data:font/woff2;base64,${font}) format('woff2');font-weight:100 900}text{font-family:Outfit,sans-serif}</style>
  <rect width="1200" height="630" fill="${BRAND_PALETTE.raised}"/>
  <image x="${String((1200 - width) / 2)}" y="${String(315 - height / 2)}" width="${String(width)}" height="${String(height)}" style="image-rendering:pixelated" href="data:image/png;base64,${wordmark}"/>
  <text x="600" y="570" text-anchor="middle" font-size="46" font-weight="500" fill="${BRAND_PALETTE.ink}">Autonomous Villages for Minecraft</text>
</svg>\n`,
);
console.log("Exported the website favicon and sharing artwork.");
