import type { ReactElement } from "react";
import { BRAND_WORDMARK } from "../data/brand";

/** Home links name the chosen wordmark, so its image is decorative. */
export function BrandLogo(): ReactElement {
  return (
    <img
      src={BRAND_WORDMARK.source}
      alt=""
      width={BRAND_WORDMARK.width}
      height={BRAND_WORDMARK.height}
    />
  );
}
