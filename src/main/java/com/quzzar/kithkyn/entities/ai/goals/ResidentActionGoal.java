package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.agency.ResidentActionState.End;
import com.quzzar.kithkyn.agency.ResidentActionState.Order;
import com.quzzar.kithkyn.agency.ResidentAgency;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Executes an accepted intention below danger/recovery and above ordinary work, without inference. */
public final class ResidentActionGoal extends Goal {
  public static final int PRIORITY = 3;
  private final RealPerson person;
  private Order order;
  private int nextPathTick;
  private int failedPaths;
  private int stalledTicks;
  private double closest = Double.MAX_VALUE;
  private Vec3 progressPosition;

  public ResidentActionGoal(RealPerson person) {
    this.person = person;
    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    return person.agency().current().isPresent();
  }

  @Override
  public boolean canContinueToUse() {
    return order != null && person.agency().current().filter(current -> current.revision() == order.revision()).isPresent();
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return true;
  }

  @Override
  public void start() {
    order = person.agency().current().orElseThrow();
    nextPathTick = person.tickCount;
    failedPaths = 0;
    stalledTicks = 0;
    closest = Double.MAX_VALUE;
    progressPosition = person.position();
  }

  @Override
  public void stop() {
    if (canContinueToUse()) person.agency().finish(End.INTERRUPTED, "Another need interrupted the request.");
    order = null;
    person.getNavigation().stop();
  }

  @Override
  public void tick() {
    // GoalSelector can tick an old goal briefly before its next continuation check.
    // A completed walk must never finish a newer follow/wait request in that interval.
    if (!canContinueToUse() || !(person.level() instanceof ServerLevel level)) return;
    ServerPlayer requester = person.agency().requester(order).orElse(null);
    if (requester == null || requester.level() != level) {
      person.agency().finish(End.FAILED, "The requester is no longer here.");
      return;
    }
    Vec3 target = order.action() == Action.FOLLOW ? requester.position() : ResidentAgency.vector(order.destination());
    double distance = person.distanceToSqr(target);
    double arrival = order.action() == Action.FOLLOW ? 9.0D : 1.0D;
    person.getLookControl().setLookAt(requester, 30.0F, 30.0F);
    if (order.action() != Action.FOLLOW && !person.agency().validGround(order.destination())) {
      person.agency().finish(End.FAILED, "The destination is no longer usable standing ground.");
      return;
    }
    if (distance <= arrival) {
      person.getNavigation().stop();
      closest = Double.MAX_VALUE;
      progressPosition = person.position();
      stalledTicks = 0;
      failedPaths = 0;
      if (order.action() == Action.GO_TO_POINT) person.agency().finish(End.COMPLETED, "Reached the pointed location.");
      return;
    }
    // A moving speaker can keep the same lead while both make progress.
    if (distance < closest - 0.25D
        || order.action() == Action.FOLLOW && person.distanceToSqr(progressPosition) >= 0.25D) {
      closest = distance;
      progressPosition = person.position();
      stalledTicks = 0;
    } else if (++stalledTicks >= 200) {
      person.agency().finish(End.FAILED, "Could not make progress toward the requested destination.");
      return;
    }
    if (person.tickCount >= nextPathTick) {
      nextPathTick = person.tickCount + 10;
      Path path = person.getNavigation().createPath(BlockPos.containing(target), order.action() == Action.FOLLOW ? 2 : 0);
      if (path != null && path.canReach() && person.getNavigation().moveTo(path, 0.65D)) failedPaths = 0;
      else if (++failedPaths >= 3) person.agency().finish(End.FAILED, "Could not find a route to the requested destination.");
    }
  }
}
