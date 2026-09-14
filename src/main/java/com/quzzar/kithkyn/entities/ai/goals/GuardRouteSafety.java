package com.quzzar.kithkyn.entities.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

/** Rejects surface guard routes that use a deep cave as a shortcut. */
final class GuardRouteSafety {

  private static final int MAX_DETOUR_BELOW_ENDPOINTS = 2;

  private GuardRouteSafety() {}

  static boolean staysNearSurface(BlockPos start, BlockPos target, Iterable<BlockPos> route) {
    int floor = Math.min(start.getY(), target.getY()) - MAX_DETOUR_BELOW_ENDPOINTS;
    for (BlockPos step : route) {
      if (step.getY() < floor) return false;
    }
    return true;
  }

  static boolean staysNearSurface(BlockPos start, BlockPos target, Path path) {
    int floor = Math.min(start.getY(), target.getY()) - MAX_DETOUR_BELOW_ENDPOINTS;
    for (int index = 0; index < path.getNodeCount(); index++) {
      if (path.getNode(index).y < floor) return false;
    }
    return true;
  }
}
