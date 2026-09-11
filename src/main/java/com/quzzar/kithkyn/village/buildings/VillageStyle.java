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
 * Every style is a strict catalog: a village raises only what its own family
 * authored and never borrows another family's building to fill a gap. Birch
 * Forest is the one bundled catalog and so the default; Desert, Badlands,
 * Floodplain, Jungle and Swamp arrive through private datapacks (docs/desert-village.md,
 * docs/badlands-village.md, docs/floodplain-village.md, docs/jungle-village.md,
 * docs/swamp-village.md), so they are only
 * automatic candidates while their founding sets are loaded.
 *
 * Explicit datapack style tags take precedence over conventional biome families.
 * An unfamiliar family chooses among climate-compatible loaded catalogs using
 * the world seed and founding site, not the world's mutable random stream.
 */
public enum VillageStyle {
  BIRCH_FOREST, DESERT, BADLANDS, FLOODPLAIN, JUNGLE, SWAMP;

  /**
   * What a blank or unknown saved style reads as, the answer for every climate
   * without a catalog of its own, and the last resort. Birch holds this seat
   * only because it is the one bundled catalog; the intended default is the
   * Plains village once its catalog exists (docs/buildings.md, "The default is
   * a placeholder"). Moving the seat is a change here, in the temperate branch
   * of {@link #climateStyles} and in the enum order, nowhere else.
   */
  public static final VillageStyle DEFAULT = BIRCH_FOREST;

  /** The token this style takes in a building id, {@code house_<style>_1}. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
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

  /**
   * The style for an id token, or {@link #DEFAULT} for anything unknown or
   * blank: a village saved in one of the removed Village Life families keeps
   * its name and people and reads as the bundled catalog from then on.
   */
  public static VillageStyle fromId(String id) {
    VillageStyle style = parse(id);
    return style != null ? style : DEFAULT;
  }

  /** Biome-only selection for previews that have no founding seed or position. */
  public static VillageStyle fromBiome(Holder<Biome> biome) {
    return fromBiome(biome, 0L, BlockPos.ZERO);
  }

  /** The one selector used by both manual and naturally generated village founding. */
  public static VillageStyle fromBiome(Holder<Biome> biome, long worldSeed, BlockPos site) {
    return fromBiome(biome, worldSeed, site, Buildings::hasFoundingSet);
  }

  /**
   * The same selection with an explicit notion of which catalogs are loaded,
   * so a check can ask what a biome maps to regardless of what is installed.
   */
  public static VillageStyle fromBiome(Holder<Biome> biome, long worldSeed, BlockPos site,
      Predicate<VillageStyle> available) {
    long biomeSeed = biome.unwrapKey().map(key -> (long) key.location().toString().hashCode()).orElse(0L);
    Biome climate = biome.value();
    String biomePath = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
    return select(biome::is, biomePath, climate.getBaseTemperature(), climate.hasPrecipitation(),
        climate.getModifiedClimateSettings().downfall(), worldSeed ^ site.asLong() ^ biomeSeed, available);
  }

  /**
   * Pure selector seam: explicit style tags first, then the conventional
   * families that have a finished catalog, then a climate cluster, then the
   * first loaded founding set in enum order. Only styles whose founding set is
   * loaded are ever chosen automatically.
   */
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
    // A partial datapack can leave a climate with no loaded catalog. Prefer an
    // actual founding set in the stable style order, never a missing style.
    for (VillageStyle style : values()) {
      if (available.test(style)) {
        return style;
      }
    }
    // With no founding content at all, initNew reports its existing missing-center error.
    return DEFAULT;
  }

  /**
   * The conventional families that map to a finished catalog. Every other
   * family (plains, forest, taiga, snowy, swamp and the rest) has no
   * catalog of its own and falls through to the climate clusters.
   */
  @Nullable
  private static VillageStyle conventionalStyle(Predicate<TagKey<Biome>> tagged, String biomePath) {
    String path = biomePath.toLowerCase(Locale.ROOT);
    // Some biome mods omit conventional tags. A birch-named family is still
    // recognizable, while an explicit style tag above can correct an exception.
    if (tagged.test(Tags.Biomes.IS_BIRCH_FOREST) || path.contains("birch")) {
      return BIRCH_FOREST;
    }
    // Pueblo covers the mesa and savanna families until more specific catalogs
    // are authored. Explicit style tags can narrow that coverage later.
    if (tagged.test(Tags.Biomes.IS_BADLANDS) || tagged.test(Tags.Biomes.IS_SAVANNA)
        || path.contains("badlands") || path.contains("mesa") || path.contains("savanna")) {
      return BADLANDS;
    }
    if (tagged.test(Tags.Biomes.IS_DESERT) || tagged.test(Tags.Biomes.IS_SANDY)) {
      return DESERT;
    }
    // Mangrove is checked before the broad swamp tag so its established
    // Floodplain architecture stays distinct from ordinary Swamp.
    if (path.contains("mangrove")) {
      return FLOODPLAIN;
    }
    if (tagged.test(Tags.Biomes.IS_JUNGLE) || path.contains("jungle")) {
      return JUNGLE;
    }
    if (tagged.test(Tags.Biomes.IS_SWAMP) || path.contains("swamp")) {
      return SWAMP;
    }
    return null;
  }

  /**
   * Architecture candidates, not survival rules. A hot, dry climate has the two
   * arid catalogs to choose between and a hot, wet one builds the floodplain
   * catalog; every other climate builds the bundled Birch Forest catalog until
   * its own family is finished. Precipitation and wet/dry tags keep an
   * unclassified rainy tropical biome out of the arid pair.
   */
  static List<VillageStyle> climateStyles(Predicate<TagKey<Biome>> tagged, float temperature,
      boolean precipitation, float downfall) {
    boolean hot = tagged.test(Tags.Biomes.IS_HOT) || tagged.test(Tags.Biomes.IS_HOT_OVERWORLD)
        || temperature >= 1.0F;
    boolean wet = precipitation && (tagged.test(Tags.Biomes.IS_WET)
        || tagged.test(Tags.Biomes.IS_WET_OVERWORLD) || downfall >= 0.7F);
    boolean dry = !precipitation || (!wet && (tagged.test(Tags.Biomes.IS_DRY)
        || tagged.test(Tags.Biomes.IS_DRY_OVERWORLD) || downfall <= 0.3F));
    if (hot && dry) {
      return List.of(DESERT, BADLANDS);
    }
    return hot && wet ? List.of(FLOODPLAIN) : List.of(BIRCH_FOREST);
  }
}
