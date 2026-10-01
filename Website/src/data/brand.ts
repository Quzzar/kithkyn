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

/** The selected artwork is sampled onto a native pixel grid before any larger export. */
export const BRAND_WORDMARK: BrandAsset = {
  kind: "wordmark",
  name: "Wordmark",
  description: "116 × 48 native pixels.",
  source: "/brand/wordmark.png",
  width: 116,
  height: 48,
  svgSource: "/brand/wordmark.svg",
  exports: [
    { label: "4× PNG", source: "/brand/wordmark-464.png" },
    { label: "8× PNG", source: "/brand/wordmark-928.png" },
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

/** Three slate surface levels frame the timber art; ivory and oak carry content and actions. */
export const BRAND_PALETTE = {
  canvas: "#20272b",
  surface: "#293238",
  raised: "#333e44",
  ink: "#f0ece3",
  oak: "#dcb075",
  muted: "#b1bbbd",
} as const;
