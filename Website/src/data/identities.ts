export type IdentityPresentation = "joinery" | "gather" | "offcut" | "neighbor";
export type IdentityId = IdentityPresentation | "pixel-joinery" | "crossgrain" | "woodcut" | "peek";
export type Identity = {
  readonly id: IdentityId;
  readonly presentation: IdentityPresentation;
  readonly number: number;
  readonly name: string;
  readonly character: string;
  readonly description: string;
  readonly width: number;
  readonly height: number;
  readonly headline: readonly [string, string];
};

/** The first round stays available as a reference while the timber direction is refined. */
export const FIRST_STUDIES: readonly Identity[] = [
  {
    id: "joinery",
    presentation: "joinery",
    number: 1,
    name: "Joinery",
    character: "Precise, with a little warmth.",
    description: "An interlocking K. Slate and oak. A quiet split layout.",
    width: 1933,
    height: 814,
    headline: ["A world with", "neighbors."],
  },
  {
    id: "gather",
    presentation: "gather",
    number: 2,
    name: "Gather",
    character: "A place to belong.",
    description: "Two neighboring homes. Soft lettering. An open, centered layout.",
    width: 2152,
    height: 731,
    headline: ["A little life", "of its own."],
  },
  {
    id: "offcut",
    presentation: "offcut",
    number: 3,
    name: "Offcut",
    character: "A little timber. A lot of character.",
    description: "A timber K. Hand-cut lettering. A clean, architectural layout.",
    width: 1942,
    height: 809,
    headline: ["Life finds a way", "to build."],
  },
  {
    id: "neighbor",
    presentation: "neighbor",
    number: 4,
    name: "Neighbor",
    character: "A quiet sense of humor.",
    description: "A peeking villager. Rounded lettering. A welcoming, playful layout.",
    width: 2095,
    height: 751,
    headline: ["Good neighbors.", "Great adventures."],
  },
];

/** A focused second round compares pixel timber and softer mod-style lettering. */
export const TIMBER_STUDIES: readonly Identity[] = [
  {
    id: "pixel-joinery",
    presentation: "joinery",
    number: 5,
    name: "Pixel Joinery",
    character: "A familiar blocky warmth.",
    description: "Three joined oak planks. Rounded ivory lettering.",
    width: 1942,
    height: 809,
    headline: ["A world with", "neighbors."],
  },
  {
    id: "crossgrain",
    presentation: "gather",
    number: 6,
    name: "Crossgrain",
    character: "A little crooked. Well made.",
    description: "An offset plank and a square peg. A looser K.",
    width: 1942,
    height: 809,
    headline: ["A little life", "of its own."],
  },
  {
    id: "woodcut",
    presentation: "offcut",
    number: 7,
    name: "Woodcut",
    character: "Small, simple, unmistakable.",
    description: "A K stamped into a pixel offcut. Quiet dark lettering.",
    width: 2172,
    height: 724,
    headline: ["Life finds a way", "to build."],
  },
  {
    id: "peek",
    presentation: "neighbor",
    number: 8,
    name: "Peek",
    character: "There’s somebody home.",
    description: "A tiny villager peeks around the timber K.",
    width: 2094,
    height: 751,
    headline: ["Good neighbors.", "Great adventures."],
  },
];

/** Both rounds use the same product previews and remain directly addressable. */
export const IDENTITIES: readonly Identity[] = [...TIMBER_STUDIES, ...FIRST_STUDIES];
