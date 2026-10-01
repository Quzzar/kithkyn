export type IdentityId = "joinery" | "gather" | "offcut" | "neighbor";
export type Identity = {
  readonly id: IdentityId;
  readonly number: number;
  readonly name: string;
  readonly character: string;
  readonly description: string;
  readonly width: number;
  readonly height: number;
  readonly headline: readonly [string, string];
};

/** Four independent logo ideas are judged with matching, usable homepage compositions. */
export const IDENTITIES: readonly Identity[] = [
  {
    id: "joinery",
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
    number: 4,
    name: "Neighbor",
    character: "A quiet sense of humor.",
    description: "A peeking villager. Rounded lettering. A welcoming, playful layout.",
    width: 2095,
    height: 751,
    headline: ["Good neighbors.", "Great adventures."],
  },
];
