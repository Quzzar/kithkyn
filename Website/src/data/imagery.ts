export type MinecraftScene = {
  readonly source: string;
  readonly alt: string;
  readonly width: number;
  readonly height: number;
  readonly creditUrl: string;
};

/** Temporary scenery is explicitly separate from Kithkyn gameplay and catalog artwork. */
export const MINECRAFT_SCENES: Readonly<Record<"village" | "woodland" | "desert", MinecraftScene>> =
  {
    village: {
      source: "/images/village-reference.jpg",
      alt: "Minecraft timber houses and gardens beside a lake at sunset",
      width: 1920,
      height: 1080,
      creditUrl: "https://modrinth.com/modpack/realisticworld/gallery",
    },
    woodland: {
      source: "/images/woodland-reference.jpg",
      alt: "Minecraft spruce woodland reflected in a lake at sunrise",
      width: 1920,
      height: 1080,
      creditUrl: "https://modrinth.com/shader/complementary-reimagined/gallery",
    },
    desert: {
      source: "/images/desert-reference.jpg",
      alt: "Minecraft sandstone cliffs and a desert lake in golden light",
      width: 2560,
      height: 1440,
      creditUrl: "https://modrinth.com/shader/complementary-reimagined/gallery",
    },
  };
