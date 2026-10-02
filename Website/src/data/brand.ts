export type BrandArtwork = {
  readonly source: string;
  readonly width: number;
  readonly height: number;
};

/** Native grids retain the source outline before any integer-sized enlargement. */
export const BRAND_WORDMARK: BrandArtwork = {
  source: "/brand/wordmark.png",
  width: 128,
  height: 37,
};
export const BRAND_ICON: BrandArtwork = {
  source: "/brand/icon.png",
  width: 32,
  height: 32,
};

/** Warm light surfaces support the timber artwork; saddle brown carries actions and focus. */
export const BRAND_PALETTE = {
  canvas: "#faf8f2",
  surface: "#f0ece3",
  raised: "#e7dfd1",
  ink: "#302a24",
  oak: "#805437",
  muted: "#665c50",
} as const;
