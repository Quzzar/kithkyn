package com.quzzar.kithkyn.entities.ai;

import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Shared bounded native navigation for accepted player movement and resident reactions. */
public final class ResidentWalk {
  public enum State { WALKING, ARRIVED, FAILED }
  public record Step(State state, String detail) { }
  private int nextPathTick;
  private int failedPaths;
  private int stalledTicks;
  private double closest = Double.MAX_VALUE;
  private Vec3 progressPosition;

  /** A reachable flag alone may describe an adjusted or cached target rather than the captured fixed point. */
  public static net.minecraft.world.level.pathfinder.Path path(RealPerson person, Vec3 target, boolean movingTarget) {
    BlockPos feet = BlockPos.containing(target);
    var path = person.getNavigation().createPath(feet, movingTarget ? 2 : 0);
    if (path == null || !path.canReach() || path.getEndNode() == null) return null;
    var end = path.getEndNode();
    return movingTarget || end.x == feet.getX() && end.y == feet.getY() && end.z == feet.getZ() ? path : null;
  }

  public Step tick(RealPerson person, Vec3 target, double arrivalSquared, boolean movingTarget, double speed) {
    if (progressPosition == null) progressPosition = person.position();
    double distance = person.distanceToSqr(target);
    if (distance <= arrivalSquared) {
      person.getNavigation().stop();
      closest = Double.MAX_VALUE;
      progressPosition = person.position();
      stalledTicks = failedPaths = 0;
      return new Step(State.ARRIVED, "Reached the destination.");
    }
    if (distance < closest - 0.25 || movingTarget && person.distanceToSqr(progressPosition) >= 0.25) {
      closest = distance;
      progressPosition = person.position();
      stalledTicks = 0;
    } else if (++stalledTicks >= 200) return new Step(State.FAILED, "Could not make progress toward the destination.");
    if (person.tickCount >= nextPathTick) {
      nextPathTick = person.tickCount + 10;
      var path = path(person, target, movingTarget);
      if (path != null && path.canReach() && person.getNavigation().moveTo(path, speed)) failedPaths = 0;
      else if (++failedPaths >= 3) return new Step(State.FAILED, "Could not find a reachable route to the destination.");
    }
    return new Step(State.WALKING, "Walking toward the destination.");
  }
}
