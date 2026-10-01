import { BRAND_PALETTE, type BrandArtwork } from "./brand";

export type TimberPalette = {
  readonly canvas: string;
  readonly surface: string;
  readonly raised: string;
  readonly ink: string;
  readonly muted: string;
  readonly oak: string;
};

export type TimberIdentity = {
  readonly id: "cabin" | "patchwork" | "hewn" | "hewn-planks" | "woven" | "cabin-mark";
  readonly layout: "split" | "centered" | "backdrop";
  readonly name: string;
  readonly description: string;
  readonly wordmark: BrandArtwork;
  readonly icon: BrandArtwork;
  readonly palette: TimberPalette;
};

/** The plank refinement keeps Hewn's cool slate website presentation. */
const HEWN_PALETTE: TimberPalette = BRAND_PALETTE;

/** Each timber identity pairs original artwork with its own complete website palette. */
export const TIMBER_IDENTITIES: Readonly<Record<TimberIdentity["id"], TimberIdentity>> = {
  cabin: {
    id: "cabin",
    layout: "split",
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
    layout: "centered",
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
  hewn: {
    id: "hewn",
    layout: "split",
    name: "Hewn",
    description: "Thick spruce lettering. Friendly shapes. Cool slate.",
    wordmark: { source: "/brand/directions/hewn-wordmark.png", width: 128, height: 35 },
    icon: { source: "/brand/directions/hewn-icon.png", width: 32, height: 32 },
    palette: HEWN_PALETTE,
  },
  "hewn-planks": {
    id: "hewn-planks",
    layout: "backdrop",
    name: "Hewn Planks",
    description: "Chunky lettering, joined from honey oak planks.",
    wordmark: { source: "/brand/directions/hewn-planks-wordmark.png", width: 128, height: 34 },
    icon: { source: "/brand/directions/hewn-planks-icon.png", width: 32, height: 32 },
    palette: HEWN_PALETTE,
  },
  woven: {
    id: "woven",
    layout: "centered",
    name: "Woven",
    description: "One interlocking timber K. Calm ivory lettering.",
    wordmark: { source: "/brand/directions/woven-wordmark.png", width: 128, height: 31 },
    icon: { source: "/brand/directions/woven-icon.png", width: 32, height: 32 },
    palette: {
      canvas: "#292b25",
      surface: "#383b31",
      raised: "#484e40",
      ink: "#f2eee2",
      muted: "#c5caba",
      oak: "#d7bc89",
    },
  },
  "cabin-mark": {
    id: "cabin-mark",
    layout: "split",
    name: "Cabin Mark",
    description: "A little home built around the K. Birch and cedar.",
    wordmark: { source: "/brand/directions/cabin-mark-wordmark.png", width: 128, height: 29 },
    icon: { source: "/brand/directions/cabin-mark-icon.png", width: 32, height: 32 },
    palette: {
      canvas: "#2c2925",
      surface: "#3b352d",
      raised: "#4c4338",
      ink: "#f4ecdd",
      muted: "#cfc4b2",
      oak: "#e4be8e",
    },
  },
};

export const CURRENT_TIMBER_IDENTITIES: readonly TimberIdentity[] = [
  TIMBER_IDENTITIES["hewn-planks"],
  TIMBER_IDENTITIES.woven,
  TIMBER_IDENTITIES["cabin-mark"],
];

export const PREVIOUS_TIMBER_IDENTITIES: readonly TimberIdentity[] = [
  TIMBER_IDENTITIES.hewn,
  TIMBER_IDENTITIES.cabin,
  TIMBER_IDENTITIES.patchwork,
];
