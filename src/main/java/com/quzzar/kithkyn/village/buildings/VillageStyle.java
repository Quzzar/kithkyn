package com.quzzar.kithkyn.village.buildings;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.neoforged.neoforge.common.Tags;

/**
 * The regional family a village builds in (docs/buildings.md, "Regional
 * variants and biomes"): chosen once, at founding, from the biome the camp
 * stands in, and kept for the village's life. Every later building takes this
 * family's variant, so a village reads as one place rather than a sampler.
 *
 * Explicit datapack style tags take precedence over conventional biome families.
 * An unfamiliar family chooses among climate-compatible loaded catalogs using
 * the world seed and founding site, not the world's mutable random stream.
 */
public enum VillageStyle {
  PLAINS, TAIGA, SNOWY, DESERT, SAVANNA, BIRCH_FOREST, BADLANDS;

  /** The token this style takes in a building id, {@code house_<style>_1}. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Approved catalogs deliberately omit roles and levels that have no selected design. */
  public boolean usesPlainsFallback() {
    return this != BIRCH_FOREST && this != BADLANDS;
  }

  /** Modpacks can assign a biome without introducing a separate mapping loader. */
  public TagKey<Biome> biomeTag() {
    return TagKey.create(Registries.BIOME,
        ResourceLocation.fromNamespaceAndPath("kithkyn", "village_style/" + id()));
  }

  /** The style for an id token, or null when no such style exists. */
  @Nullable
  public static VillageStyle parse(String id) {
    for (VillageStyle style : values()) {
      if (style.id().equalsIgnoreCase(id)) {
        return style;
      }
    }
    return null;
  }

  /** The style for an id token, plains for anything unknown or blank. */
  public static VillageStyle fromId(String id) {
    VillageStyle style = parse(id);
    return style != null ? style : PLAINS;
  }

  /** Biome-only selection for previews that have no founding seed or position. */
  public static VillageStyle fromBiome(Holder<Biome> biome) {
    return fromBiome(biome, 0L, BlockPos.ZERO);
  }

  /** The one selector used by both manual and naturally generated village founding. */
  public static VillageStyle fromBiome(Holder<Biome> biome, long worldSeed, BlockPos site) {
    long biomeSeed = biome.unwrapKey().map(key -> (long) key.location().toString().hashCode()).orElse(0L);
    Biome climate = biome.value();
    String biomePath = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
    return select(biome::is, biomePath, climate.getBaseTemperature(), climate.hasPrecipitation(),
        climate.getModifiedClimateSettings().downfall(), worldSeed ^ site.asLong() ^ biomeSeed,
        Buildings::hasFoundingSet);
  }

  /** Pure selector seam: tags carry architecture, climate only fills an unclassified family's gap. */
  static VillageStyle select(Predicate<TagKey<Biome>> tagged, String biomePath, float temperature,
      boolean precipitation, float downfall, long siteSeed, Predicate<VillageStyle> available) {
    for (VillageStyle style : values()) {
      if (tagged.test(style.biomeTag()) && available.test(style)) {
        return style;
      }
    }
    VillageStyle known = conventionalStyle(tagged, biomePath);
    if (known != null && available.test(known)) {
      return known;
    }

    List<VillageStyle> candidates = climateStyles(tagged, temperature, precipitation, downfall).stream()
        .filter(available).toList();
    if (!candidates.isEmpty()) {
      return candidates.get(RandomSource.create(RandomSupport.mixStafford13(siteSeed)).nextInt(candidates.size()));
    }
    // A partial datapack can omit an entire climate group. Prefer the existing
    // neutral fallback, then an actual founding set, never a random missing style.
    if (available.test(PLAINS)) {
      return PLAINS;
    }
    for (VillageStyle style : values()) {
      if (available.test(style)) {
        return style;
      }
    }
    // With no founding content at all, initNew reports its existing missing-center error.
    return PLAINS;
  }

  @Nullable
  private static VillageStyle conventionalStyle(Predicate<TagKey<Biome>> tagged, String biomePath) {
    // Some biome mods omit conventional tags. A birch-named family is still
    // recognizable, while an explicit style tag above can correct an exception.
    if (tagged.test(Tags.Biomes.IS_BIRCH_FOREST) || biomePath.toLowerCase(Locale.ROOT).contains("birch")) {
      return BIRCH_FOREST;
    }
    if (tagged.test(Tags.Biomes.IS_DESERT) || tagged.test(Tags.Biomes.IS_BADLANDS)
        || tagged.test(Tags.Biomes.IS_SANDY)) {
      return DESERT;
    }
    if (tagged.test(Tags.Biomes.IS_SNOWY) || tagged.test(Tags.Biomes.IS_ICY)) {
      return SNOWY;
    }
    if (tagged.test(Tags.Biomes.IS_SAVANNA) || tagged.test(Tags.Biomes.IS_JUNGLE)) {
      return SAVANNA;
    }
    if (tagged.test(Tags.Biomes.IS_TAIGA) || tagged.test(Tags.Biomes.IS_CONIFEROUS_TREE)
        || tagged.test(Tags.Biomes.IS_MOUNTAIN)) {
      return TAIGA;
    }
    if (tagged.test(Tags.Biomes.IS_PLAINS) || tagged.test(Tags.Biomes.IS_FOREST)
        || tagged.test(Tags.Biomes.IS_DECIDUOUS_TREE) || tagged.test(Tags.Biomes.IS_SWAMP)) {
      return PLAINS;
    }
    return null;
  }

  /**
   * Architecture candidates, not survival rules. Precipitation and wet/dry tags
   * keep an unclassified rainy tropical biome out of the desert cluster.
   */
  static List<VillageStyle> climateStyles(Predicate<TagKey<Biome>> tagged, float temperature,
      boolean precipitation, float downfall) {
    boolean cold = tagged.test(Tags.Biomes.IS_COLD) || tagged.test(Tags.Biomes.IS_COLD_OVERWORLD)
        || temperature < 0.4F;
    boolean hot = tagged.test(Tags.Biomes.IS_HOT) || tagged.test(Tags.Biomes.IS_HOT_OVERWORLD)
        || temperature >= 1.0F;
    boolean wet = precipitation && (tagged.test(Tags.Biomes.IS_WET)
        || tagged.test(Tags.Biomes.IS_WET_OVERWORLD) || downfall >= 0.7F);
    boolean dry = !precipitation || (!wet && (tagged.test(Tags.Biomes.IS_DRY)
        || tagged.test(Tags.Biomes.IS_DRY_OVERWORLD) || downfall <= 0.3F));
    if (cold) {
      return temperature < 0.15F && precipitation ? List.of(SNOWY, TAIGA) : List.of(TAIGA, PLAINS);
    }
    if (hot) {
      return dry ? List.of(DESERT, SAVANNA) : List.of(SAVANNA, PLAINS);
    }
    return dry ? List.of(PLAINS, SAVANNA) : List.of(PLAINS, BIRCH_FOREST);
  }
}
