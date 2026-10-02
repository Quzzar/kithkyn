export type BrandExport = {
  readonly label: string;
  readonly source: string;
};

export type BrandAsset = {
  readonly kind: "wordmark" | "icon";
  readonly name: string;
  readonly description: string;
  readonly source: string;
  readonly width: number;
  readonly height: number;
  readonly svgSource: string;
  readonly exports: readonly BrandExport[];
};

/** Artwork shared by the selected identity and complete website studies. */
export type BrandArtwork = Pick<BrandAsset, "source" | "width" | "height">;

/** Local destinations let a study retain its own identity while visitors browse the page. */
export type SiteBranding = {
  readonly wordmark?: BrandArtwork;
  readonly icon?: BrandArtwork;
  readonly homePath?: string;
  readonly brandPath?: string;
  readonly isStudy?: boolean;
};

/** The selected artwork is sampled onto a native pixel grid before any larger export. */
export const BRAND_WORDMARK: BrandAsset = {
  kind: "wordmark",
  name: "Wordmark",
  description: "128 × 34 native pixels. Joined honey oak planks.",
  source: "/brand/wordmark.png",
  width: 128,
  height: 34,
  svgSource: "/brand/wordmark.svg",
  exports: [
    { label: "4× PNG", source: "/brand/wordmark-512.png" },
    { label: "8× PNG", source: "/brand/wordmark-1024.png" },
  ],
};
export const BRAND_ICON: BrandAsset = {
  kind: "icon",
  name: "K icon",
  description: "32 × 32 native pixels.",
  source: "/brand/icon.png",
  width: 32,
  height: 32,
  svgSource: "/brand/icon.svg",
  exports: [
    { label: "64px", source: "/brand/icon-64.png" },
    { label: "128px", source: "/brand/icon-128.png" },
    { label: "256px", source: "/brand/icon-256.png" },
    { label: "512px", source: "/brand/icon-512.png" },
  ],
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
