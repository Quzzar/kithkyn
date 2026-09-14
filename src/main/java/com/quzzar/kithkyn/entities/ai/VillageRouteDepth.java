package com.quzzar.kithkyn.entities.ai;

import net.minecraft.core.BlockPos;

/** Keeps ordinary village trips on the surface while leaving real underground destinations reachable. */
final class VillageRouteDepth {
  private static final int MAX_BELOW_SURFACE = 2;

  private VillageRouteDepth() {}

  static int minimumY(BlockPos villageCenter, BlockPos start, Iterable<BlockPos> targets) {
    int surface = villageCenter.getY();
    for (BlockPos target : targets) {
      surface = Math.min(surface, target.getY());
    }
    // Someone already underground must still be allowed to climb out. The
    // bound stops them descending farther without invalidating their start.
    return Math.min(start.getY(), surface - MAX_BELOW_SURFACE);
  }
}
