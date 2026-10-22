package com.quzzar.kithkyn.entities.ai;

import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Bounded native navigation for autonomous resident reactions. */
public final class ResidentWalk {
  public enum State { WALKING, ARRIVED, FAILED }
  public record Step(State state, String detail) { }
  private int nextPathTick;
  private int failedPaths;
  private int stalledTicks;
  private double closest = Double.MAX_VALUE;

  public static boolean usableGround(RealPerson person, Vec3 target) {
    if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) return false;
    BlockPos feet = BlockPos.containing(target);
    return person.distanceToSqr(target) <= 48 * 48
        && person.level().hasChunkAt(feet) && person.level().getWorldBorder().isWithinBounds(feet)
        && person.level().getFluidState(feet).isEmpty() && person.level().getFluidState(feet.below()).isEmpty()
        && person.level().getBlockState(feet.below()).isFaceSturdy(person.level(), feet.below(), Direction.UP)
        && person.level().noCollision(person, person.getBoundingBox().move(target.subtract(person.position())));
  }

  /** A reachable flag alone may describe an adjusted or cached target rather than the captured fixed point. */
  public static net.minecraft.world.level.pathfinder.Path path(RealPerson person, Vec3 target) {
    BlockPos feet = BlockPos.containing(target);
    var path = person.getNavigation().createPath(feet, 0);
    if (path == null || !path.canReach() || path.getEndNode() == null) return null;
    var end = path.getEndNode();
    return end.x == feet.getX() && end.y == feet.getY() && end.z == feet.getZ() ? path : null;
  }

  public Step tick(RealPerson person, Vec3 target, double arrivalSquared, double speed) {
    double distance = person.distanceToSqr(target);
    if (distance <= arrivalSquared) {
      person.getNavigation().stop();
      closest = Double.MAX_VALUE;
      stalledTicks = failedPaths = 0;
      return new Step(State.ARRIVED, "Reached the destination.");
    }
    if (distance < closest - 0.25) {
      closest = distance;
      stalledTicks = 0;
    } else if (++stalledTicks >= 200) return new Step(State.FAILED, "Could not make progress toward the destination.");
    if (person.tickCount >= nextPathTick) {
      nextPathTick = person.tickCount + 10;
      var path = path(person, target);
      if (path != null && path.canReach() && person.getNavigation().moveTo(path, speed)) failedPaths = 0;
      else if (++failedPaths >= 3) return new Step(State.FAILED, "Could not find a reachable route to the destination.");
    }
    return new Step(State.WALKING, "Walking toward the destination.");
  }
}
