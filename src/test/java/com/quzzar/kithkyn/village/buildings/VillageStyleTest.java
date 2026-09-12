package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.Tags;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VillageStyleTest {
  private static final Predicate<VillageStyle> ALL_STYLES = ignored -> true;
  private static final Predicate<TagKey<Biome>> NO_TAGS = ignored -> false;

  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void bundledBirchLeadsTheEnumAndIsWhatUnknownSavedStylesReadAs() {
    assertEquals(List.of(VillageStyle.BIRCH_FOREST, VillageStyle.DESERT, VillageStyle.BADLANDS,
        VillageStyle.FLOODPLAIN, VillageStyle.JUNGLE, VillageStyle.SWAMP, VillageStyle.MEDITERRANEAN,
        VillageStyle.TUNDRA),
        List.of(VillageStyle.values()));
    assertEquals(VillageStyle.BIRCH_FOREST, VillageStyle.DEFAULT);
    assertEquals(VillageStyle.BIRCH_FOREST, VillageStyle.fromId(""));
    assertEquals(VillageStyle.BIRCH_FOREST, VillageStyle.fromId("taiga"));
    assertEquals(VillageStyle.BIRCH_FOREST, VillageStyle.fromId("removed_family"));
    assertEquals(VillageStyle.DESERT, VillageStyle.fromId("DESERT"));
    assertNull(VillageStyle.parse("taiga"));
  }

  @Test
  void explicitDatapackMappingWinsOverConventionalAndNamedFamilies() {
    Set<TagKey<Biome>> tags = Set.of(VillageStyle.DESERT.biomeTag(), Tags.Biomes.IS_BIRCH_FOREST);
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(tags::contains, "birch_hills", 0.6F, true, 0.6F, 7L, ALL_STYLES));
  }

  @Test
  void puebloCoversMesaAndSavannaWhileSandyDesertsStayDistinct() {
    Set<TagKey<Biome>> mapped = Set.of(VillageStyle.BADLANDS.biomeTag(), Tags.Biomes.IS_BADLANDS);
    assertEquals(VillageStyle.BADLANDS,
        VillageStyle.select(mapped::contains, "badlands", 2F, false, 0F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.BADLANDS,
        VillageStyle.select(Tags.Biomes.IS_BADLANDS::equals, "wooded_badlands", 2F, false, 0F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(Tags.Biomes.IS_DESERT::equals, "desert", 2F, false, 0F, 7L, ALL_STYLES));
    // A mapped style that is not loaded falls through to the hot, dry cluster's other member.
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(mapped::contains, "badlands", 2F, false, 0F, 7L,
            style -> style == VillageStyle.DESERT));
    for (long seed = 0; seed < 20; seed++) {
      assertFamily(Tags.Biomes.IS_BADLANDS, VillageStyle.BADLANDS, seed);
      assertFamily(Tags.Biomes.IS_SAVANNA, VillageStyle.BADLANDS, seed);
      assertFamily(Tags.Biomes.IS_SANDY, VillageStyle.DESERT, seed);
      assertFamily(Tags.Biomes.IS_DESERT, VillageStyle.DESERT, seed);
      Set<TagKey<Biome>> sandyMesa = Set.of(Tags.Biomes.IS_BADLANDS, Tags.Biomes.IS_SANDY);
      assertEquals(VillageStyle.BADLANDS,
          VillageStyle.select(sandyMesa::contains, "red_cliffs", 2F, false, 0F, seed, ALL_STYLES));
    }
  }

  @Test
  void untaggedMesaAndSavannaNamesHaveStableCoverageThatExplicitTagsCanNarrow() {
    for (String path : List.of("wooded_mesa", "red_badlands", "dry_savanna", "savannah_hills")) {
      assertEquals(VillageStyle.BADLANDS,
          VillageStyle.select(NO_TAGS, path, 1.2F, false, 0F, 12L, ALL_STYLES));
      assertEquals(VillageStyle.DESERT,
          VillageStyle.select(VillageStyle.DESERT.biomeTag()::equals, path, 1.2F, false, 0F, 12L, ALL_STYLES));
    }
    assertEquals(VillageStyle.BIRCH_FOREST,
        VillageStyle.select(Tags.Biomes.IS_SAVANNA::equals, "birch_savanna", 1F, false, 0F, 12L, ALL_STYLES));
  }

  @Test
  void birchConventionWinsOverBroadForestAndClimateTags() {
    Set<TagKey<Biome>> tags = Set.of(Tags.Biomes.IS_BIRCH_FOREST, Tags.Biomes.IS_FOREST,
        Tags.Biomes.IS_COLD);
    assertEquals(VillageStyle.BIRCH_FOREST,
        VillageStyle.select(tags::contains, "custom_woods", 0.2F, true, 0.5F, 8L, ALL_STYLES));
  }

  @Test
  void untaggedModdedBirchNameStillSelectsBirch() {
    assertEquals(VillageStyle.BIRCH_FOREST,
        VillageStyle.select(NO_TAGS, "old_birch_woodland", 0.7F, true, 0.6F, 1L, ALL_STYLES));
  }

  @Test
  void unfinishedConventionalFamiliesBuildBirchRatherThanARemovedCatalog() {
    List<TagKey<Biome>> unfinished = List.of(Tags.Biomes.IS_FOREST,
        Tags.Biomes.IS_DECIDUOUS_TREE, Tags.Biomes.IS_TAIGA,
        Tags.Biomes.IS_CONIFEROUS_TREE, Tags.Biomes.IS_MOUNTAIN);
    for (long seed = 0; seed < 20; seed++) {
      for (TagKey<Biome> tag : unfinished) {
        assertFamily(tag, VillageStyle.BIRCH_FOREST, seed);
      }
      assertEquals(VillageStyle.TUNDRA,
          VillageStyle.select(Tags.Biomes.IS_SNOWY::equals, "snowy_plains", 0.0F, true, 0.5F, seed, ALL_STYLES));
      assertEquals(VillageStyle.TUNDRA,
          VillageStyle.select(Tags.Biomes.IS_ICY::equals, "ice_spikes", 0.0F, true, 0.5F, seed, ALL_STYLES));
      assertEquals(VillageStyle.TUNDRA,
          VillageStyle.select(NO_TAGS, "frosted_lowlands", 0.0F, true, 0.5F, seed, ALL_STYLES));
      assertEquals(VillageStyle.BIRCH_FOREST,
          VillageStyle.select(Tags.Biomes.IS_SNOWY::equals, "snowy_plains", 0.0F, true, 0.5F, seed,
              style -> style == VillageStyle.BIRCH_FOREST),
          "without the Tundra pack, snowy biomes fall back to the bundled catalog");
    }
  }

  @Test
  void plainsFamiliesUseTheMediterraneanCatalogOnlyWhenItIsLoaded() {
    assertEquals(VillageStyle.MEDITERRANEAN,
        VillageStyle.select(Tags.Biomes.IS_PLAINS::equals, "plains", 0.8F, true, 0.4F, 5L, ALL_STYLES));
    assertEquals(VillageStyle.MEDITERRANEAN,
        VillageStyle.select(NO_TAGS, "sunflower_plains", 0.8F, true, 0.4F, 5L, ALL_STYLES),
        "the vanilla plains are recognizable by name without any tag");
    Set<TagKey<Biome>> snowyPlain = Set.of(Tags.Biomes.IS_PLAINS, Tags.Biomes.IS_SNOWY);
    assertEquals(VillageStyle.TUNDRA,
        VillageStyle.select(snowyPlain::contains, "snowy_plains", 0.0F, true, 0.5F, 5L, ALL_STYLES),
        "a snowy plain is Tundra country");
    for (long seed = 0; seed < 20; seed++) {
      assertEquals(VillageStyle.BIRCH_FOREST,
          VillageStyle.select(Tags.Biomes.IS_PLAINS::equals, "plains", 0.8F, true, 0.4F, seed,
              style -> style != VillageStyle.MEDITERRANEAN),
          "without the Mediterranean pack, plains stay a temperate Birch village");
    }
  }

  @Test
  void jungleFamiliesUseJungleOnlyWhenItsFoundingCatalogIsLoaded() {
    assertEquals(VillageStyle.JUNGLE,
        VillageStyle.select(Tags.Biomes.IS_JUNGLE::equals, "jungle", 0.95F, true, 0.9F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.JUNGLE,
        VillageStyle.select(NO_TAGS, "sparse_jungle_hills", 0.95F, true, 0.8F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.FLOODPLAIN,
        VillageStyle.select(Tags.Biomes.IS_JUNGLE::equals, "jungle", 0.95F, true, 0.9F, 7L,
            style -> style == VillageStyle.FLOODPLAIN));
  }

  @Test
  void onlyAHotDryClimateChoosesBetweenTheAridCatalogs() {
    List<VillageStyle> arid = List.of(VillageStyle.DESERT, VillageStyle.BADLANDS);
    List<VillageStyle> birch = List.of(VillageStyle.BIRCH_FOREST);
    assertEquals(arid, VillageStyle.climateStyles(NO_TAGS, 1.3F, false, 0.9F), "no precipitation is dry");
    assertEquals(arid, VillageStyle.climateStyles(NO_TAGS, 1.0F, true, 0.2F), "hot with little downfall");
    assertEquals(List.of(VillageStyle.FLOODPLAIN), VillageStyle.climateStyles(NO_TAGS, 1.3F, true, 0.9F),
        "hot and wet is floodplain country");
    assertEquals(birch, VillageStyle.climateStyles(NO_TAGS, 0.7F, true, 0.5F), "temperate");
    assertEquals(birch, VillageStyle.climateStyles(NO_TAGS, 0.7F, false, 0.5F), "temperate and dry");
    assertEquals(birch, VillageStyle.climateStyles(NO_TAGS, 0.0F, true, 0.5F), "freezing");
    assertEquals(birch, VillageStyle.climateStyles(NO_TAGS, 0.0F, false, 0.5F), "cold and dry");
    Set<TagKey<Biome>> hotWet = Set.of(Tags.Biomes.IS_HOT_OVERWORLD, Tags.Biomes.IS_WET_OVERWORLD);
    assertEquals(List.of(VillageStyle.FLOODPLAIN), VillageStyle.climateStyles(hotWet::contains, 0.7F, true, 0.1F),
        "explicit hot and wet tags protect a low-downfall biome from the arid pair");
    Set<TagKey<Biome>> hotDry = Set.of(Tags.Biomes.IS_HOT, Tags.Biomes.IS_DRY);
    assertEquals(arid, VillageStyle.climateStyles(hotDry::contains, 0.7F, true, 0.5F),
        "explicit hot and dry tags need no temperature threshold");
  }

  @Test
  void mangroveFamiliesAreFloodplainWhilePlainSwampUsesItsOwnCatalog() {
    assertEquals(VillageStyle.FLOODPLAIN,
        VillageStyle.select(VillageStyle.FLOODPLAIN.biomeTag()::equals, "mangrove_swamp", 0.8F, true, 0.9F, 3L, ALL_STYLES));
    assertEquals(VillageStyle.FLOODPLAIN,
        VillageStyle.select(Tags.Biomes.IS_SWAMP::equals, "mangrove_swamp", 0.8F, true, 0.9F, 3L, ALL_STYLES),
        "vanilla mangrove swamp is recognizable by name even without the style tag");
    assertEquals(VillageStyle.FLOODPLAIN,
        VillageStyle.select(NO_TAGS, "mangrove_bayou", 0.8F, true, 0.9F, 3L, ALL_STYLES));
    Set<TagKey<Biome>> swamp = Set.of(Tags.Biomes.IS_SWAMP, Tags.Biomes.IS_HOT_OVERWORLD, Tags.Biomes.IS_WET_OVERWORLD);
    assertEquals(VillageStyle.SWAMP,
        VillageStyle.select(swamp::contains, "swamp", 0.8F, true, 0.9F, 3L, ALL_STYLES),
        "plain swamp has its own conventional mapping");
    assertEquals(VillageStyle.SWAMP,
        VillageStyle.select(Tags.Biomes.IS_SWAMP::equals, "swamp", 0.8F, true, 0.9F, 3L, ALL_STYLES),
        "a modded swamp needs no climate tags when its family is known");
    assertEquals(VillageStyle.FLOODPLAIN,
        VillageStyle.select(Tags.Biomes.IS_SWAMP::equals, "swamp", 0.8F, true, 0.9F, 3L,
            style -> style == VillageStyle.FLOODPLAIN),
        "without the Swamp pack, its hot-wet climate fallback remains Floodplain");
    for (long seed = 0; seed < 20; seed++) {
      assertEquals(VillageStyle.FLOODPLAIN,
          VillageStyle.select(NO_TAGS, "steaming_marsh", 1.2F, true, 0.9F, seed, ALL_STYLES),
          "an unclassified hot, wet biome builds the floodplain catalog");
      assertEquals(VillageStyle.BIRCH_FOREST,
          VillageStyle.select(NO_TAGS, "steaming_marsh", 1.2F, true, 0.9F, seed,
              style -> style != VillageStyle.FLOODPLAIN),
          "without the floodplain pack the first loaded founding set stands in");
    }
  }

  @Test
  void unknownHotDrySiteChoiceIsStableAndVariesOnlyWithinTheAridPair() {
    Set<VillageStyle> seen = new HashSet<>();
    for (long seed = 0; seed < 100; seed++) {
      VillageStyle chosen = VillageStyle.select(NO_TAGS, "unclassified", 1.5F, false, 0F, seed, ALL_STYLES);
      assertEquals(chosen, VillageStyle.select(NO_TAGS, "unclassified", 1.5F, false, 0F, seed, ALL_STYLES));
      seen.add(chosen);
    }
    assertEquals(Set.of(VillageStyle.DESERT, VillageStyle.BADLANDS), seen);
    assertEquals(VillageStyle.BADLANDS,
        VillageStyle.select(NO_TAGS, "unclassified", 1.5F, false, 0F, 5L, style -> style == VillageStyle.BADLANDS));
    for (long seed = 0; seed < 100; seed++) {
      assertEquals(VillageStyle.BIRCH_FOREST,
          VillageStyle.select(NO_TAGS, "unclassified", 0.7F, true, 0.5F, seed, ALL_STYLES),
          "a temperate site never rolls an arid catalog");
    }
  }

  @Test
  void unavailableMappedStyleFallsBackToTheFirstLoadedFoundingSetInEnumOrder() {
    Set<TagKey<Biome>> tags = Set.of(VillageStyle.BIRCH_FOREST.biomeTag(), Tags.Biomes.IS_BIRCH_FOREST);
    EnumSet<VillageStyle> arid = EnumSet.of(VillageStyle.DESERT, VillageStyle.BADLANDS);
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(tags::contains, "birch", 0.7F, true, 0.5F, 5L, arid::contains));
    assertEquals(VillageStyle.BADLANDS,
        VillageStyle.select(tags::contains, "birch", 0.7F, true, 0.5F, 5L, style -> style == VillageStyle.BADLANDS));
    assertEquals(VillageStyle.BIRCH_FOREST,
        VillageStyle.select(NO_TAGS, "unknown", 2.0F, false, 0.0F, 5L, ignored -> false),
        "with nothing loaded the default lets founding report its missing centre");
  }

  @Test
  void savedStylesSurviveSavingAndRemovedFamiliesReadAsBirch() {
    for (VillageStyle style : VillageStyle.values()) {
      Village village = new Village("Oravel");
      village.setStyle(style);
      assertEquals(style, roundTrip(village).getStyle());
    }
    assertEquals(VillageStyle.BIRCH_FOREST, new Village("Oldstead").getStyle());
    // A village founded in one of the removed families keeps its saved token and reads as Birch.
    Village legacy = new Village("Wetherby");
    legacy.getBrain().getStrategy().putString("style", "plains");
    assertEquals(VillageStyle.BIRCH_FOREST, roundTrip(legacy).getStyle());
  }

  @Test
  void foundingApiReadsActualBiomeClimateAndOnlyLoadedCatalogs() {
    Map<String, BuildingInfo> definitions = new HashMap<>();
    for (VillageStyle style : List.of(VillageStyle.DESERT, VillageStyle.BADLANDS)) {
      for (String category : List.of("village_center", "mine", "storehouse")) {
        BuildingInfo info = new BuildingInfo(category + "_" + style.id() + "_1");
        definitions.put(info.getName(), info);
      }
    }
    Buildings.reload(definitions);
    Holder<Biome> scrub = biome(2.0F, false, 0.0F);
    Set<VillageStyle> seen = new HashSet<>();
    for (int x = 0; x < 100; x++) {
      BlockPos site = new BlockPos(x * 48, 64, 96);
      VillageStyle selected = VillageStyle.fromBiome(scrub, 42L, site);
      assertEquals(selected, VillageStyle.fromBiome(scrub, 42L, site));
      seen.add(selected);
    }
    assertEquals(Set.of(VillageStyle.DESERT, VillageStyle.BADLANDS), seen);
    // Birch is the temperate answer, but it is not loaded here, so the first loaded set wins.
    assertEquals(VillageStyle.DESERT, VillageStyle.fromBiome(biome(0.7F, true, 0.5F), 42L, BlockPos.ZERO));
  }

  private static void assertFamily(TagKey<Biome> tag, VillageStyle expected, long seed) {
    assertEquals(expected,
        VillageStyle.select(tag::equals, "familiar", 0.7F, true, 0.5F, seed, ALL_STYLES));
  }

  private static Village roundTrip(Village village) {
    return Village.CODEC.parse(NbtOps.INSTANCE,
        Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow()).getOrThrow();
  }

  private static Holder<Biome> biome(float temperature, boolean precipitation, float downfall) {
    return Holder.direct(new Biome.BiomeBuilder().temperature(temperature).downfall(downfall)
        .hasPrecipitation(precipitation).generationSettings(BiomeGenerationSettings.EMPTY)
        .mobSpawnSettings(MobSpawnSettings.EMPTY).specialEffects(new BiomeSpecialEffects.Builder()
            .fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build()).build());
  }
}
