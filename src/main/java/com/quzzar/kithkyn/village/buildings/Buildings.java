package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Registry of building definitions, populated from datapack JSON under
 * {@code data/<namespace>/kithkyn/buildings/*.json} by {@link BuildingDefinitionLoader}.
 *
 * Definitions come in regional variants, {@code <category>_<style>_<level>[__<design>]}, and
 * a village builds in one style for life ({@link VillageStyle}). Everything that
 * asks "what can this village build" goes through {@link #resolve} or
 * {@link #catalogue}, which hand back that style's variant and fall back to
 * plains only for the older fallback-enabled families. An explicitly complete
 * catalog can omit a role or tier without borrowing another architecture.
 */
public class Buildings {

  /** Founding always includes the center, mine and storehouse, plus authored starting homes. */
  public static final String VILLAGE_CENTER_CATEGORY = "village_center";
  public static final String FOUNDING_MINE_CATEGORY = "mine";
  public static final String FOUNDING_STOREHOUSE_CATEGORY = "storehouse";
  /**
   * The home a village raises for a married couple (docs/marriage.md). Unlike
   * every other category it is never chosen spontaneously by the brain: it is
   * offered only as the saved-for goal a marriage sets, so the planner filters
   * it out of the options it deliberates over.
   */
  public static final String COUPLE_COTTAGE_CATEGORY = "couple_cottage";

  private record Family(String category, String variant, int level) {}

  /** Publish definitions and their lookup index together on reload. */
  private record Registry(Map<String, BuildingInfo> byName, Map<Family, List<BuildingInfo>> families) {}

  private static volatile Registry registry = new Registry(Map.of(), Map.of());

  /** Replaces the whole registry; called on datapack (re)load. */
  public static void reload(Map<String, BuildingInfo> newRegistry) {
    BuildingFootprint.clearCache();
    Map<Family, List<BuildingInfo>> families = new HashMap<>();
    for (BuildingInfo info : newRegistry.values()) {
      if (info.hasWellFormedId()) {
        Family family = new Family(info.getCategory(), info.getVariant(), info.getLevel());
        families.computeIfAbsent(family, ignored -> new ArrayList<>()).add(info);
      }
    }
    Comparator<BuildingInfo> canonicalFirst = Comparator
        .comparingInt((BuildingInfo info) -> info.getDesign() == null ? 0 : 1)
        .thenComparing(BuildingInfo::getName);
    families.replaceAll((family, choices) -> choices.stream().sorted(canonicalFirst).toList());
    registry = new Registry(Map.copyOf(newRegistry), Map.copyOf(families));
  }

  public static Map<String, BuildingInfo> allBuildings() {
    return registry.byName();
  }

  @Nullable
  public static BuildingInfo getByName(String name) {
    return registry.byName().get(name);
  }

  /**
   * The canonical design, or the first named alternative when no canonical is
   * authored. All regional and fallback rules are shared with {@link #alternatives}.
   */
  @Nullable
  public static BuildingInfo resolve(String category, int level, VillageStyle style) {
    List<BuildingInfo> choices = alternatives(category, level, style);
    return choices.isEmpty() ? null : choices.getFirst();
  }

  /** Every layout in a regional family, canonical first, then stable by id. */
  public static List<BuildingInfo> alternatives(String category, int level, VillageStyle style) {
    return alternatives(registry, category, level, style);
  }

  private static List<BuildingInfo> alternatives(Registry snapshot, String category, int level, VillageStyle style) {
    List<BuildingInfo> own = snapshot.families().get(new Family(category, style.id(), level));
    if (own != null) return own;
    return style.usesPlainsFallback()
        ? snapshot.families().getOrDefault(new Family(category, VillageStyle.PLAINS.id(), level), List.of())
        : List.of();
  }

  /** Whether this exact loaded design is legal on a fresh site for the village. */
  public static boolean isRegionalChoice(BuildingInfo info, VillageStyle style) {
    return info.hasWellFormedId() && alternatives(info.getCategory(), info.getLevel(), style).contains(info);
  }

  /** Resolves the entire authored starting list; absence of any member refuses the whole settlement. */
  public static java.util.Optional<List<BuildingInfo>> foundingCompanions(BuildingInfo center, VillageStyle style) {
    List<String> names = center.getStartingBuildings();
    if (names.isEmpty()) {
      BuildingInfo mine = resolve(FOUNDING_MINE_CATEGORY, 1, style);
      BuildingInfo storehouse = resolve(FOUNDING_STOREHOUSE_CATEGORY, 1, style);
      return mine == null || storehouse == null ? java.util.Optional.empty()
          : java.util.Optional.of(List.of(mine, storehouse));
    }
    List<BuildingInfo> result = new ArrayList<>();
    for (String name : names) {
      BuildingInfo info = getByName(name);
      if (info == null || info.getCategory().equals(VILLAGE_CENTER_CATEGORY)
          || !isRegionalChoice(info, style)) return java.util.Optional.empty();
      result.add(info);
    }
    if (result.stream().filter(info -> info.getCategory().equals(FOUNDING_MINE_CATEGORY)).count() != 1
        || result.stream().filter(info -> info.getCategory().equals(FOUNDING_STOREHOUSE_CATEGORY)).count() != 1) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(List.copyOf(result));
  }

  /** Automatic founding requires the complete regional starting set, including authored homes. */
  public static boolean hasFoundingSet(VillageStyle style) {
    BuildingInfo center = registry.byName().get(VILLAGE_CENTER_CATEGORY + "_" + style.id() + "_1");
    return center != null && foundingCompanions(center, style).isPresent()
        && registry.byName().containsKey(FOUNDING_MINE_CATEGORY + "_" + style.id() + "_1")
        && registry.byName().containsKey(FOUNDING_STOREHOUSE_CATEGORY + "_" + style.id() + "_1");
  }

  /**
   * The catalogue as one style sees it: every level-1 design per category
   * (that style's family, or plains), plus every higher level for fallback-enabled
   * families. Those higher levels can upgrade a borrowed variant already standing;
   * the planner separately limits fresh builds to the village's regional variant.
   * A strict catalog exposes only its own authored definitions at every level.
   * Definitions whose id does not parse pass through untouched.
   */
  public static List<BuildingInfo> catalogue(VillageStyle style) {
    Registry snapshot = registry;
    List<BuildingInfo> out = new ArrayList<>();
    for (BuildingInfo info : snapshot.byName().values()) {
      if (!style.usesPlainsFallback() && info.hasWellFormedId()) {
        if (info.getVariant().equals(style.id())) {
          out.add(info);
        }
        continue;
      }
      if (!info.hasWellFormedId() || info.getLevel() > 1
          || alternatives(snapshot, info.getCategory(), 1, style).contains(info)) {
        out.add(info);
      }
    }
    return out.stream().sorted(Comparator.comparing(BuildingInfo::getName)).toList();
  }

}
