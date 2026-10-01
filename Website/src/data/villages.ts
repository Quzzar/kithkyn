export type Village = {
  readonly id: string;
  readonly name: string;
  readonly biome: string;
  readonly detail: string;
  readonly materials: readonly string[];
  readonly image?: string;
};

/** The public roster follows the 17 bundled land catalogs in docs/village-biomes.md. */
export const DEFAULT_VILLAGE: Village = {
  id: "mediterranean",
  name: "Mediterranean",
  biome: "Plains & sunflower plains",
  detail: "White stone homes, terracotta roofs, orchards, and open courtyards.",
  materials: ["White stone", "Terracotta", "Hedges"],
  image: "mediterranean-homes",
};

export const VILLAGES: readonly Village[] = [
  DEFAULT_VILLAGE,
  {
    id: "birch",
    name: "Birch Forest",
    biome: "Birch woodland",
    detail: "Pale timber and mossy stone make an intimate woodland home.",
    materials: ["Birch", "Mossy stone", "Oak"],
    image: "birch",
  },
  {
    id: "rustic",
    name: "Rustic Woodland",
    biome: "Oak forests",
    detail: "Working barns and oak homes gather around a merchant and well.",
    materials: ["Oak", "Stone", "Stripped logs"],
    image: "rustic",
  },
  {
    id: "taiga",
    name: "Taiga",
    biome: "Conifer forests",
    detail: "Spruce homes, a Viking meeting point, and a timber palisade.",
    materials: ["Spruce", "Cobblestone", "Stripped logs"],
    image: "taiga",
  },
  {
    id: "romanian",
    name: "Romanian",
    biome: "Dark forests & wooded valleys",
    detail: "Steep roofs, heavy timber frames, and enclosed woodland yards.",
    materials: ["Dark oak", "Birch", "Deepslate"],
  },
  {
    id: "jungle",
    name: "Jungle Tribal",
    biome: "Jungle & bamboo jungle",
    detail: "Bamboo roofs, treehouses, and compact homes under the canopy.",
    materials: ["Bamboo", "Jungle wood", "Timber"],
    image: "jungle-homes",
  },
  {
    id: "desert",
    name: "Desert Oasis",
    biome: "Sandy deserts",
    detail: "Sandstone courts, shaded homes, wells, and oasis planting.",
    materials: ["Sandstone", "Candles", "Oasis gardens"],
    image: "desert",
  },
  {
    id: "pueblo",
    name: "Pueblo",
    biome: "Badlands & mesas",
    detail: "Terracotta homes and roof terraces meet red sandstone walls.",
    materials: ["Terracotta", "Adobe", "Red sandstone"],
    image: "pueblo",
  },
  {
    id: "floodplain",
    name: "Floodplain",
    biome: "Mangrove swamps",
    detail: "Mud-brick homes and mangrove details follow the water.",
    materials: ["Mud brick", "Mangrove", "Raised earth"],
    image: "floodplain",
  },
  {
    id: "swamp",
    name: "Swamp",
    biome: "Ordinary wetlands",
    detail: "Mossy ruins and candlelit timber gather around two campfires.",
    materials: ["Oak", "Spruce", "Mossy stone"],
    image: "mangrove-homes",
  },
  {
    id: "tundra",
    name: "Tundra",
    biome: "Snowy plains & frozen lowlands",
    detail: "Snowbound homes, sheltered farms, and packed-ice defenses.",
    materials: ["Packed ice", "Snow", "Spruce"],
    image: "tundra",
  },
  {
    id: "alpine",
    name: "Alpine Highlands",
    biome: "Meadows & mountain slopes",
    detail: "Brick and spruce homes, berry plots, and deep wells.",
    materials: ["Brick", "Spruce", "Berries"],
    image: "alpine",
  },
  {
    id: "cherry",
    name: "Japanese Cherry Grove",
    biome: "Cherry groves & flower forests",
    detail: "Spruce frames, ponds, and gardens behind flowering walls.",
    materials: ["Spruce", "Cherry leaves", "Garden ponds"],
  },
  {
    id: "polynesian",
    name: "Polynesian Coast",
    biome: "Warm beaches & sparse jungle",
    detail: "Stilted homes, a king’s hall, and an open-air shrine.",
    materials: ["Stripped spruce", "Oak roofs", "Coral"],
    image: "polynesian",
  },
  {
    id: "nautical",
    name: "Nautical Coast",
    biome: "Temperate beaches & stony shores",
    detail: "Lighthouse life, fishing jetties, and thatched beach cottages.",
    materials: ["Sandstone", "Jungle timber", "Thatch"],
  },
  {
    id: "savanna",
    name: "Savanna Tent",
    biome: "Savannas & dry grasslands",
    detail: "Canvas tents, colorful tipis, and an acacia palisade.",
    materials: ["Canvas", "Acacia", "Cobblestone"],
  },
  {
    id: "mushroom",
    name: "Mushroom",
    biome: "Mushroom fields",
    detail: "Homes under mushroom caps, trade gazebos, and mooshroom pens.",
    materials: ["Mushroom caps", "Pale stems", "Oak"],
  },
];
