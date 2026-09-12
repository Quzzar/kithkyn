package com.quzzar.kithkyn.village.buildings;

import java.util.List;
import java.util.Set;

/**
 * Compiles a flexible perimeter into the independent pieces builders raise.
 *
 * The built-in implementation makes the feature playable without external
 * assets. Authored NBT pieces can implement this same boundary later: builder
 * AI and save progress consume only the resulting {@link WallSection}s.
 */
public interface WallSegmentCatalog {

  List<WallSection> compile(List<Long> ring, Set<Long> gates, List<Integer> ground,
      List<Integer> deck, WallTier tier);

  default List<WallSection> compile(List<Long> ring, Set<Long> gates, List<Integer> ground,
      List<Integer> deck, WallTier tier, Set<Long> towerExclusions) {
    return compile(ring, gates, ground, deck, tier);
  }

  static WallSegmentCatalog builtIn() {
    return BuiltInWallSegmentCatalog.INSTANCE;
  }

  /** Regional art uses the same route compiler and persistent section model. */
  static WallSegmentCatalog forStyle(VillageStyle style) {
    return switch (style) {
      case BIRCH_FOREST -> BuiltInWallSegmentCatalog.BIRCH_FOREST;
      case DESERT, BADLANDS, FLOODPLAIN, JUNGLE, TUNDRA -> BuiltInWallSegmentCatalog.ARID;
      case SWAMP -> BuiltInWallSegmentCatalog.SWAMP;
      case MEDITERRANEAN -> BuiltInWallSegmentCatalog.MEDITERRANEAN;
    };
  }
}
