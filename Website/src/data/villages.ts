import { MINECRAFT_SCENES, type MinecraftScene } from "./imagery";

export type Village = {
  readonly id: string;
  readonly name: string;
  readonly biome: string;
  readonly detail: string;
  readonly materials: readonly string[];
  readonly image?: MinecraftScene;
};

/** The public roster follows the 17 bundled land catalogs in docs/village-biomes.md. */
export const DEFAULT_VILLAGE: Village = {
  id: "mediterranean",
  name: "Plains",
  biome: "Plains & sunflower plains",
  detail: "White stone homes, terracotta roofs, orchards, and open courtyards.",
  materials: ["White stone", "Terracotta", "Hedges"],
  image: MINECRAFT_SCENES.village,
};

export const VILLAGES: readonly Village[] = [
  DEFAULT_VILLAGE,
  {
    id: "birch",
    name: "Birch Forest",
    biome: "Birch woodland",
    detail: "Birch houses with mossy stone foundations.",
    materials: ["Birch", "Mossy stone", "Oak"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "rustic",
    name: "Forest",
    biome: "Oak forests",
    detail: "Oak homes, barns, and a central market and well.",
    materials: ["Oak", "Stone", "Stripped logs"],
    image: MINECRAFT_SCENES.village,
  },
  {
    id: "taiga",
    name: "Taiga",
    biome: "Conifer forests",
    detail: "Spruce homes, a meeting hall, and a timber palisade.",
    materials: ["Spruce", "Cobblestone", "Stripped logs"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "romanian",
    name: "Dark Forest",
    biome: "Dark forests & wooded valleys",
    detail: "Steep roofs, heavy timber frames, and enclosed woodland yards.",
    materials: ["Dark oak", "Birch", "Deepslate"],
  },
  {
    id: "jungle",
    name: "Jungle",
    biome: "Jungle & bamboo jungle",
    detail: "Bamboo roofs, treehouses, and compact homes under the canopy.",
    materials: ["Bamboo", "Jungle wood", "Timber"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "desert",
    name: "Desert",
    biome: "Sandy deserts",
    detail: "Sandstone courts, shaded homes, wells, and oasis planting.",
    materials: ["Sandstone", "Candles", "Oasis gardens"],
    image: MINECRAFT_SCENES.desert,
  },
  {
    id: "pueblo",
    name: "Badlands",
    biome: "Badlands & mesas",
    detail: "Terracotta homes, roof terraces, and red sandstone walls.",
    materials: ["Terracotta", "Adobe", "Red sandstone"],
    image: MINECRAFT_SCENES.desert,
  },
  {
    id: "floodplain",
    name: "Mangrove Swamp",
    biome: "Mangrove swamps",
    detail: "Mud-brick homes with mangrove timber on raised ground.",
    materials: ["Mud brick", "Mangrove", "Raised earth"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "swamp",
    name: "Swamp",
    biome: "Ordinary wetlands",
    detail: "Mossy stone buildings, oak homes, and candlelit timber walls.",
    materials: ["Oak", "Spruce", "Mossy stone"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "tundra",
    name: "Snowy Lowlands",
    biome: "Snowy plains & frozen lowlands",
    detail: "Snowbound homes, sheltered farms, and packed-ice defenses.",
    materials: ["Packed ice", "Snow", "Spruce"],
    image: MINECRAFT_SCENES.woodland,
  },
  {
    id: "alpine",
    name: "Mountains",
    biome: "Meadows & mountain slopes",
    detail: "Brick and spruce homes, berry plots, and deep wells.",
    materials: ["Brick", "Spruce", "Berries"],
    image: MINECRAFT_SCENES.village,
  },
  {
    id: "cherry",
    name: "Cherry Grove",
    biome: "Cherry groves & flower forests",
    detail: "Spruce frames, ponds, and gardens behind flowering walls.",
    materials: ["Spruce", "Cherry leaves", "Garden ponds"],
  },
  {
    id: "polynesian",
    name: "Warm Coast",
    biome: "Warm beaches & sparse jungle",
    detail: "Stilted homes, a king’s hall, and an open-air shrine.",
    materials: ["Stripped spruce", "Oak roofs", "Coral"],
    image: MINECRAFT_SCENES.village,
  },
  {
    id: "nautical",
    name: "Temperate Coast",
    biome: "Temperate beaches & stony shores",
    detail: "A lighthouse, fishing jetties, and thatched beach cottages.",
    materials: ["Sandstone", "Jungle timber", "Thatch"],
  },
  {
    id: "savanna",
    name: "Savanna",
    biome: "Savannas & dry grasslands",
    detail: "Canvas tents, colorful tipis, and an acacia palisade.",
    materials: ["Canvas", "Acacia", "Cobblestone"],
  },
  {
    id: "mushroom",
    name: "Mushroom Fields",
    biome: "Mushroom fields",
    detail: "Homes under mushroom caps, trade gazebos, and mooshroom pens.",
    materials: ["Mushroom caps", "Pale stems", "Oak"],
  },
];
