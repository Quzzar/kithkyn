package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import com.quzzar.kithkyn.agency.ResidentActionState.Order;
import com.quzzar.kithkyn.agency.ResidentAgency;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.ResidentWalk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Executes an accepted intention below danger/recovery and above ordinary work, without inference. */
public final class ResidentActionGoal extends Goal {
  public static final int PRIORITY = 3;
  private final RealPerson person;
  private Order order;
  private ResidentWalk walk;

  public ResidentActionGoal(RealPerson person) {
    this.person = person;
    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    return person.agency().current().filter(person.agency()::valid).isPresent();
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
    walk = new ResidentWalk();
    person.agency().underway(order);
  }

  @Override
  public void stop() {
    if (canContinueToUse()) person.agency().finish(Phase.INTERRUPTED, "Another need interrupted the request.");
    order = null;
    person.getNavigation().stop();
  }

  @Override
  public void tick() {
    // GoalSelector can tick an old goal briefly before its next continuation check.
    // A completed walk must never finish a newer follow/wait request in that interval.
    if (!canContinueToUse() || !(person.level() instanceof ServerLevel level)) return;
    if (!person.agency().valid(order)) { person.agency().finish(Phase.INTERRUPTED, "The captured request or duties changed."); return; }
    ServerPlayer requester = person.agency().requester(order).orElse(null);
    if (requester == null || requester.level() != level) {
      person.agency().finish(Phase.FAILED, "The requester is no longer here.");
      return;
    }
    Vec3 target = order.action() == Action.FOLLOW ? requester.position() : ResidentAgency.vector(order.destination());
    double arrival = order.action() == Action.FOLLOW ? 9.0D : 1.0D;
    person.getLookControl().setLookAt(requester, 30.0F, 30.0F);
    if (order.action() != Action.FOLLOW && !person.agency().validGround(order.destination())) {
      person.agency().finish(Phase.FAILED, "The destination is no longer usable standing ground.");
      return;
    }
    var step = walk.tick(person, target, arrival, order.action() == Action.FOLLOW, 0.65D);
    if (step.state() == ResidentWalk.State.FAILED) person.agency().finish(Phase.FAILED, step.detail());
    else if (step.state() == ResidentWalk.State.ARRIVED && order.action() == Action.GO_TO_POINT) {
      person.agency().finish(Phase.COMPLETED, "Reached the pointed location.");
    }
  }
}
