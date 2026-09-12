package com.quzzar.kithkyn.village.buildings;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/** Shared physical footing checks; each construction job owns its own site and reach policy. */
public final class WorkerFooting {
  /**
   * The tallest thing in a cell that is still floor rather than obstacle: a carpet or a
   * pressure plate (a sixteenth), a single layer of snow (two). Anything taller is a step
   * the path planner reasons about separately.
   */
  private static final double THIN_COVERING = 0.125D;

  private WorkerFooting() { }

  /** Dry, supported ground with clearance for this worker's actual collision box. */
  public static boolean canStand(RealPerson person, BlockPos position) {
    return standingPosition(person, position) != null;
  }

  /** A resident-sized access check for surveys that have no particular worker yet. */
  public static boolean canStand(Level level, BlockPos position) {
    return standingPosition(level, position, null, 0.6D, 1.95D) != null;
  }

  /** A path node keeps integer coordinates; its actual floor can be a slab or a worn path below it. */
  @Nullable
  public static Vec3 standingPosition(Entity entity, BlockPos position) {
    return standingPosition(entity.level(), position, entity, entity.getBbWidth(), entity.getBbHeight());
  }

  @Nullable
  private static Vec3 standingPosition(Level level, BlockPos position,
      @Nullable Entity entity, double width, double height) {
    BlockPos below = position.below();
    if (!level.hasChunkAt(position) || !level.getFluidState(position).isEmpty()
        || !level.getFluidState(below).isEmpty()) return null;
    var support = level.getBlockState(below).getCollisionShape(level, below);
    if (support.isEmpty()) return null;
    double top = support.max(Direction.Axis.Y);
    if (top > 1.0D) return null;
    // A carpet in the cell itself lifts the feet onto it, the way a player stands on one;
    // measuring the body from the floor block below made every carpet a collision.
    var covering = level.getBlockState(position).getCollisionShape(level, position);
    double lift = 0.0D;
    if (!covering.isEmpty()) {
      lift = covering.max(Direction.Axis.Y);
      if (lift > THIN_COVERING) return null;
    }
    double half = width / 2.0D;
    // The whole footprint must rest on the surface; a fence tip or a stair edge is insufficient.
    var sole = Shapes.box(0.5D - half, top - 0.000001D, 0.5D - half,
        0.5D + half, top, 0.5D + half);
    if (Shapes.joinIsNotEmpty(sole, support, BooleanOp.ONLY_FIRST)) return null;
    double feetY = below.getY() + top + lift;
    AABB body = new AABB(position.getX() + 0.5D - half, feetY, position.getZ() + 0.5D - half,
        position.getX() + 0.5D + half, feetY + height, position.getZ() + 0.5D + half);
    return level.noCollision(entity, body) ? new Vec3(position.getX() + 0.5D, feetY, position.getZ() + 0.5D) : null;
  }
}
