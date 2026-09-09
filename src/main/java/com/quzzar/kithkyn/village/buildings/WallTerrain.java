package com.quzzar.kithkyn.village.buildings;

import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;


/** Pure terrain-height policy shared by wall sampling and its regression tests. */
final class WallTerrain {

  private WallTerrain() {
  }

  /** Walks down from a heightmap result until a genuine supporting surface is found. */
  static int surfaceY(int top, int minimum, IntPredicate sturdyTerrainAt) {
    int surface = top;
    while (surface > minimum && !sturdyTerrainAt.test(surface - 1)) {
      surface--;
    }
    return surface;
  }

  /**
   * Uses the highest waterline touching a wall column. The neighborhood covers
   * water that flows back into a column immediately after an older wall is removed.
   */
  static int defensiveSurfaceY(int naturalGround, int x, int z,
      IntBinaryOperator waterSurfaceAt) {
    int defensiveSurface = naturalGround;
    for (int offsetX = -1; offsetX <= 1; offsetX++) {
      for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
        defensiveSurface = Math.max(defensiveSurface,
            waterSurfaceAt.applyAsInt(x + offsetX, z + offsetZ));
      }
    }
    return defensiveSurface;
  }

  /** Exposed natural soil is replaced once so the wall reads as embedded in the bank. */
  static boolean shouldEmbedSurface(boolean isNaturalSoil, boolean isOwned,
      boolean hasExposedSide) {
    return isNaturalSoil && !isOwned && hasExposedSide;
  }

}
