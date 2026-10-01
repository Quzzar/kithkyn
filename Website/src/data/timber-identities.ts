import type { BrandArtwork } from "./brand";

export type TimberPalette = {
  readonly canvas: string;
  readonly surface: string;
  readonly raised: string;
  readonly ink: string;
  readonly muted: string;
  readonly oak: string;
};

export type TimberIdentity = {
  readonly id: "cabin" | "patchwork";
  readonly name: string;
  readonly description: string;
  readonly wordmark: BrandArtwork;
  readonly icon: BrandArtwork;
  readonly palette: TimberPalette;
};

/** Two complete directions pair original timber lettering with its own website palette. */
export const TIMBER_IDENTITIES: Readonly<Record<TimberIdentity["id"], TimberIdentity>> = {
  cabin: {
    id: "cabin",
    name: "Cabin Joinery",
    description: "Joined oak beams. Quiet moss. A place to settle.",
    wordmark: { source: "/brand/directions/cabin-wordmark.png", width: 128, height: 35 },
    icon: { source: "/brand/directions/cabin-icon.png", width: 32, height: 32 },
    palette: {
      canvas: "#202c27",
      surface: "#2b3a32",
      raised: "#384b40",
      ink: "#f2eee3",
      muted: "#bcc8be",
      oak: "#dab67f",
    },
  },
  patchwork: {
    id: "patchwork",
    name: "Patchwork",
    description: "Birch, oak and cedar. Bold pieces that fit together.",
    wordmark: { source: "/brand/directions/patchwork-wordmark.png", width: 128, height: 26 },
    icon: { source: "/brand/directions/patchwork-icon.png", width: 32, height: 32 },
    palette: {
      canvas: "#25272d",
      surface: "#30333a",
      raised: "#3d414a",
      ink: "#f0ebe1",
      muted: "#bdc1c9",
      oak: "#e0a887",
    },
  },
};
