package com.quzzar.kithkyn.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared timing and trajectory for a fisher's visible cast, wait, and retrieve. */
public final class FishingCast {
  public static final int CAST_TICKS = 12;
  public static final int CATCH_TICKS = 400;
  private static final int REEL_TICKS = 10;

  private FishingCast() { }

  /** The bobber flies in an arc, floats at the surface, then returns to the rod. */
  public static Vec3 bobber(Vec3 rod, BlockPos water, float age) {
    Vec3 surface = Vec3.atLowerCornerOf(water).add(0.5D, 0.9D, 0.5D);
    if (age < CAST_TICKS) {
      double fraction = Mth.clamp(age / CAST_TICKS, 0.0F, 1.0F);
      return rod.lerp(surface, fraction).add(0, 2.0D * fraction * (1.0D - fraction), 0);
    }
    if (age > CATCH_TICKS - REEL_TICKS) {
      double fraction = Mth.clamp((age - CATCH_TICKS + REEL_TICKS) / REEL_TICKS, 0.0F, 1.0F);
      return surface.lerp(rod, fraction);
    }
    return surface.add(0, Math.sin(age * 0.15D) * 0.025D, 0);
  }
}
