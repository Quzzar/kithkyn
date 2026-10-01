export type BrandAsset = {
  readonly name: string;
  readonly description: string;
  readonly source: string;
  readonly width: number;
  readonly height: number;
  readonly svgSource: string;
};

/** The owner's chosen PNG masters are used without repainting or tracing. */
export const BRAND_WORDMARK: BrandAsset = {
  name: "Wordmark",
  description: "Pixel oak lettering with an open corner frame.",
  source: "/brand/wordmark.png",
  width: 1945,
  height: 809,
  svgSource: "/brand/wordmark.svg",
};
export const BRAND_ICON: BrandAsset = {
  name: "K icon",
  description: "Three joined oak planks, for compact spaces.",
  source: "/brand/icon.png",
  width: 1254,
  height: 1254,
  svgSource: "/brand/icon.svg",
};

/** Shared colors keep the site and exported sharing card consistent. */
export const BRAND_PALETTE = {
  canvas: "#fafbfc",
  surface: "#eff3f6",
  ink: "#22384a",
  oak: "#9d6c36",
  muted: "#657584",
  line: "#d8dfe5",
} as const;
