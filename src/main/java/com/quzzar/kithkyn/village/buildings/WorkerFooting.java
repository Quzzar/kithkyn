package com.quzzar.kithkyn.village.buildings;

import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Shared physical footing checks; each construction job owns its own site and reach policy. */
public final class WorkerFooting {
  private WorkerFooting() { }

  /** Dry, supported ground with clearance for this worker, including a passage they can duck through. */
  public static boolean canStand(RealPerson person, BlockPos position) {
    return canStand(person.level(), position, person, person.getBbWidth(), person.getBbHeight())
        || canStand(person.level(), position, person, person.getBbWidth(),
            person.getDimensions(Pose.CROUCHING).height());
  }

  /** A resident-sized access check for surveys that have no particular worker yet. */
  public static boolean canStand(Level level, BlockPos position) {
    return canStand(level, position, null, 0.6D, 1.95D);
  }

  private static boolean canStand(Level level, BlockPos position,
      @javax.annotation.Nullable Entity entity, double width, double height) {
    BlockPos below = position.below();
    if (!level.hasChunkAt(position) || !level.getFluidState(position).isEmpty()
        || !level.getFluidState(below).isEmpty()
        || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
    double half = width / 2.0D;
    AABB body = new AABB(position.getX() + 0.5D - half, position.getY(), position.getZ() + 0.5D - half,
        position.getX() + 0.5D + half, position.getY() + height, position.getZ() + 0.5D + half);
    return level.noCollision(entity, body);
  }
}
