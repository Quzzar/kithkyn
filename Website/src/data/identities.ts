export type IdentityPresentation = "joinery" | "gather" | "offcut" | "neighbor";
export type IdentityId =
  | IdentityPresentation
  | "pixel-joinery"
  | "crossgrain"
  | "woodcut"
  | "peek"
  | "log-lettering"
  | "corner-frame"
  | "oak-frame";
export type IdentityAsset = {
  readonly source: string;
  readonly width: number;
  readonly height: number;
};
export type Identity = {
  readonly id: IdentityId;
  readonly presentation: IdentityPresentation;
  readonly number: number;
  readonly name: string;
  readonly character: string;
  readonly description: string;
  readonly width: number;
  readonly height: number;
  readonly icon?: IdentityAsset;
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

/** The owner selected this timber K as the icon for the next lettering iteration. */
export const SELECTED_IDENTITY: Identity = {
  id: "pixel-joinery",
  presentation: "joinery",
  number: 5,
  name: "Pixel Joinery",
  character: "A familiar blocky warmth.",
  description: "Three joined oak planks. Rounded ivory lettering.",
  width: 1942,
  height: 809,
  icon: { source: "/studies/selected-k.png", width: 1254, height: 1254 },
  headline: ["A world with", "neighbors."],
};

/** A focused second round compares pixel timber and softer mod-style lettering. */
export const TIMBER_STUDIES: readonly Identity[] = [
  SELECTED_IDENTITY,
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

/** Matching timber letters are compared with three levels of framing. */
export const LETTERING_STUDIES: readonly Identity[] = [
  SELECTED_IDENTITY,
  {
    id: "log-lettering",
    presentation: "joinery",
    number: 9,
    name: "Log Lettering",
    character: "The whole name, built from timber.",
    description: "Matching oak letters. Open and unframed.",
    width: 1944,
    height: 809,
    headline: ["A world with", "neighbors."],
  },
  {
    id: "corner-frame",
    presentation: "joinery",
    number: 10,
    name: "Corner Frame",
    character: "A little structure around the name.",
    description: "Timber letters with a light, open frame.",
    width: 1945,
    height: 809,
    headline: ["A world with", "neighbors."],
  },
  {
    id: "oak-frame",
    presentation: "joinery",
    number: 11,
    name: "Oak Frame",
    character: "Held together with a little oak.",
    description: "The full timber name inside slim wooden rails.",
    width: 1944,
    height: 809,
    headline: ["A world with", "neighbors."],
  },
];

export type IdentityStudyRound = "first" | "timber" | "lettering";
export type IdentityRoundContent = {
  readonly headline: readonly [string, string];
  readonly description: string;
  readonly studies: readonly Identity[];
  readonly previous: string;
  readonly footer: string;
};

/** One comparison surface serves every actively reviewed round. */
export const STUDY_ROUNDS: Record<IdentityStudyRound, IdentityRoundContent> = {
  first: {
    headline: ["Four ways to feel", "like Kithkyn."],
    description: "Small hints of timber. More room to breathe.",
    studies: FIRST_STUDIES,
    previous: "/",
    footer: "Four studies. One identity to choose.",
  },
  timber: {
    headline: ["Pixel timber.", "A little character."],
    description: "Four takes on a wooden K.",
    studies: TIMBER_STUDIES,
    previous: "/studies/first",
    footer: "The first K is selected. Next, the lettering.",
  },
  lettering: {
    headline: ["A timber name.", "A familiar K."],
    description: "The selected icon. Three ways to frame the name.",
    studies: LETTERING_STUDIES,
    previous: "/studies/timber",
    footer: "One selected K. Three lettering studies.",
  },
};

/** Earlier identities retain their direct links while the selected K is developed. */
export const IDENTITIES: readonly Identity[] = [
  ...LETTERING_STUDIES.filter(
    (identity: Identity): boolean => identity.id !== SELECTED_IDENTITY.id,
  ),
  ...TIMBER_STUDIES,
  ...FIRST_STUDIES,
];
