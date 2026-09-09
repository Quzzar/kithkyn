package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.GuardNightRoutine;
import com.quzzar.kithkyn.village.GuardDuty;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** Keeps a wall or authored tower defender at their exact station between fights. */
public final class GuardPostGoal extends Goal {

  private static final double SPEED = 0.6D;
  private static final double ARRIVED_DISTANCE_SQR = 0.6D * 0.6D;

  private final RealPerson guard;
  private int nextPathAttempt;

  public GuardPostGoal(RealPerson guard) {
    this.guard = guard;
    this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    return guard.guardRoutine() == GuardNightRoutine.POST && GuardDuty.available(guard) != null;
  }

  @Override
  public boolean canContinueToUse() {
    return canUse();
  }

  @Override
  public void tick() {
    GuardDuty post = GuardDuty.available(guard);
    if (post == null) {
      return;
    }
    BlockPos station = post.position();
    guard.getLookControl().setLookAt(
        post.lookAt().getX() + 0.5D,
        guard.getEyeY(),
        post.lookAt().getZ() + 0.5D);
    if (guard.distanceToSqr(station.getX() + 0.5D, station.getY(), station.getZ() + 0.5D)
        <= ARRIVED_DISTANCE_SQR) {
      guard.getNavigation().stop();
      return;
    }
    if (guard.tickCount >= nextPathAttempt) {
      nextPathAttempt = guard.tickCount + 40;
      var path = guard.getNavigation().createPath(station, 0);
      // The exact endpoint is required. A bounded search may discover the final
      // node before exhausting its budget without setting canReach; that is
      // still a complete route, unlike a partial route into a basement below it.
      if (path != null && path.getEndNode() != null
          && path.getEndNode().asBlockPos().equals(station)) {
        guard.getNavigation().moveTo(path, SPEED);
      } else {
        guard.getNavigation().stop();
      }
    }
  }

  @Override
  public void stop() {
    guard.getNavigation().stop();
    nextPathAttempt = 0;
  }
}
