package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void badlandsMappingIsExplicitAndDoesNotConsumeOtherAridFamilies() {
    Set<TagKey<Biome>> mapped = Set.of(VillageStyle.BADLANDS.biomeTag(), Tags.Biomes.IS_BADLANDS);
    assertEquals(VillageStyle.BADLANDS,
        VillageStyle.select(mapped::contains, "badlands", 2F, false, 0F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(Tags.Biomes.IS_BADLANDS::equals, "wooded_badlands", 2F, false, 0F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(Tags.Biomes.IS_DESERT::equals, "desert", 2F, false, 0F, 7L, ALL_STYLES));
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(mapped::contains, "badlands", 2F, false, 0F, 7L,
            style -> style == VillageStyle.DESERT));
    assertFalse(VillageStyle.BADLANDS.usesPlainsFallback());
  }

  @Test
  void badlandsStyleSurvivesSavingWithoutRestylingExistingDesertVillages() {
    for (VillageStyle style : List.of(VillageStyle.BADLANDS, VillageStyle.DESERT)) {
      Village village = new Village("Oravel");
      village.setStyle(style);
      Village restored = Village.CODEC.parse(NbtOps.INSTANCE,
          Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow()).getOrThrow();
      assertEquals(style, restored.getStyle());
    }
  }

  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void explicitDatapackMappingWinsOverConventionalAndNamedFamilies() {
    Set<TagKey<Biome>> tags = Set.of(VillageStyle.DESERT.biomeTag(), Tags.Biomes.IS_BIRCH_FOREST);
    assertEquals(VillageStyle.DESERT,
        VillageStyle.select(tags::contains, "birch_hills", 0.6F, true, 0.6F, 7L, ALL_STYLES));
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
        VillageStyle.select(ignored -> false, "old_birch_woodland", 0.7F, true, 0.6F, 1L, ALL_STYLES));
  }

  @Test
  void familiarBiomeFamiliesDoNotRollDifferentArchitectures() {
    for (long seed = 0; seed < 20; seed++) {
      assertFamily(Tags.Biomes.IS_DESERT, VillageStyle.DESERT, seed);
      assertFamily(Tags.Biomes.IS_SNOWY, VillageStyle.SNOWY, seed);
      assertFamily(Tags.Biomes.IS_JUNGLE, VillageStyle.SAVANNA, seed);
      assertFamily(Tags.Biomes.IS_TAIGA, VillageStyle.TAIGA, seed);
      assertFamily(Tags.Biomes.IS_FOREST, VillageStyle.PLAINS, seed);
      assertFamily(Tags.Biomes.IS_PLAINS, VillageStyle.PLAINS, seed);
    }
  }

  @Test
  void unknownWetAndDryTropicalBiomesUseDifferentClusters() {
    Predicate<TagKey<Biome>> noTags = ignored -> false;
    assertEquals(List.of(VillageStyle.DESERT, VillageStyle.SAVANNA),
        VillageStyle.climateStyles(noTags, 1.3F, false, 0.9F));
    assertEquals(List.of(VillageStyle.SAVANNA, VillageStyle.PLAINS),
        VillageStyle.climateStyles(noTags, 1.3F, true, 0.9F));
  }

  @Test
  void explicitHotWetClimateTagsProtectAgainstDesertWithoutFamilyTags() {
    Set<TagKey<Biome>> tags = Set.of(Tags.Biomes.IS_HOT_OVERWORLD, Tags.Biomes.IS_WET_OVERWORLD);
    assertEquals(List.of(VillageStyle.SAVANNA, VillageStyle.PLAINS),
        VillageStyle.climateStyles(tags::contains, 0.7F, true, 0.1F));
  }

  @Test
  void precipitationAlsoDistinguishesFreezingAndDryColdClusters() {
    Predicate<TagKey<Biome>> noTags = ignored -> false;
    assertEquals(List.of(VillageStyle.SNOWY, VillageStyle.TAIGA),
        VillageStyle.climateStyles(noTags, 0.0F, true, 0.5F));
    assertEquals(List.of(VillageStyle.TAIGA, VillageStyle.PLAINS),
        VillageStyle.climateStyles(noTags, 0.0F, false, 0.5F));
    Set<TagKey<Biome>> cold = Set.of(Tags.Biomes.IS_COLD_OVERWORLD);
    assertEquals(List.of(VillageStyle.TAIGA, VillageStyle.PLAINS),
        VillageStyle.climateStyles(cold::contains, 0.7F, true, 0.5F));
  }

  @Test
  void unknownSiteChoiceIsStableAndVariesOnlyWithinItsAvailableCluster() {
    Set<VillageStyle> seen = new HashSet<>();
    for (long seed = 0; seed < 100; seed++) {
      VillageStyle chosen = VillageStyle.select(ignored -> false, "unclassified", 0.7F, true, 0.5F,
          seed, ALL_STYLES);
      assertEquals(chosen, VillageStyle.select(ignored -> false, "unclassified", 0.7F, true, 0.5F,
          seed, ALL_STYLES));
      seen.add(chosen);
    }
    assertEquals(Set.of(VillageStyle.PLAINS, VillageStyle.BIRCH_FOREST), seen);
    assertEquals(VillageStyle.BIRCH_FOREST,
        VillageStyle.select(ignored -> false, "unclassified", 0.7F, true, 0.5F,
            5L, style -> style == VillageStyle.BIRCH_FOREST));
  }

  @Test
  void unavailableMappedStyleCannotBeAutomaticallyFounded() {
    Set<TagKey<Biome>> tags = Set.of(VillageStyle.BIRCH_FOREST.biomeTag(), Tags.Biomes.IS_BIRCH_FOREST);
    EnumSet<VillageStyle> loaded = EnumSet.of(VillageStyle.PLAINS, VillageStyle.TAIGA);
    assertEquals(VillageStyle.PLAINS,
        VillageStyle.select(tags::contains, "birch", 0.7F, true, 0.5F, 5L, loaded::contains));
    assertEquals(VillageStyle.TAIGA,
        VillageStyle.select(ignored -> false, "unknown", 2.0F, false, 0.0F, 5L,
            style -> style == VillageStyle.TAIGA));
  }

  @Test
  void savedBirchStyleDoesNotGetReclassifiedOnReload() {
    Village village = new Village("Birchstead");
    village.setStyle(VillageStyle.BIRCH_FOREST);
    Village restored = Village.CODEC.parse(NbtOps.INSTANCE,
        Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow()).getOrThrow();
    assertEquals(VillageStyle.BIRCH_FOREST, restored.getStyle());
    assertEquals(VillageStyle.PLAINS, new Village("Oldstead").getStyle());
    assertEquals(VillageStyle.PLAINS, VillageStyle.fromId("removed_family"));
    assertFalse(VillageStyle.BIRCH_FOREST.usesPlainsFallback());
    assertTrue(VillageStyle.TAIGA.usesPlainsFallback());
  }

  @Test
  void foundingApiReadsActualBiomeClimateAndOnlyLoadedCatalogs() {
    Map<String, BuildingInfo> definitions = new HashMap<>();
    for (VillageStyle style : List.of(VillageStyle.PLAINS, VillageStyle.BIRCH_FOREST)) {
      for (String category : List.of("village_center", "mine", "storehouse")) {
        BuildingInfo info = new BuildingInfo(category + "_" + style.id() + "_1");
        definitions.put(info.getName(), info);
      }
    }
    Buildings.reload(definitions);
    Holder<Biome> biome = Holder.direct(new Biome.BiomeBuilder().temperature(0.7F).downfall(0.5F)
        .hasPrecipitation(true).generationSettings(BiomeGenerationSettings.EMPTY)
        .mobSpawnSettings(MobSpawnSettings.EMPTY).specialEffects(new BiomeSpecialEffects.Builder()
            .fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build()).build());
    Set<VillageStyle> seen = new HashSet<>();
    for (int x = 0; x < 100; x++) {
      BlockPos site = new BlockPos(x * 48, 64, 96);
      VillageStyle selected = VillageStyle.fromBiome(biome, 42L, site);
      assertEquals(selected, VillageStyle.fromBiome(biome, 42L, site));
      seen.add(selected);
    }
    assertEquals(Set.of(VillageStyle.PLAINS, VillageStyle.BIRCH_FOREST), seen);
  }

  private static void assertFamily(TagKey<Biome> tag, VillageStyle expected, long seed) {
    assertEquals(expected,
        VillageStyle.select(tag::equals, "familiar", 0.7F, true, 0.5F, seed, ALL_STYLES));
  }
}
