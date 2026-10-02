import type { ReactElement } from "react";
import { BRAND_WORDMARK, type BrandArtwork } from "../data/brand";

/** Home links name the chosen wordmark, so its image is decorative. */
export function BrandLogo({
  artwork = BRAND_WORDMARK,
}: { readonly artwork?: BrandArtwork } = {}): ReactElement {
  return <img src={artwork.source} alt="" width={artwork.width} height={artwork.height} />;
}
