package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;
import com.quzzar.kithkyn.decision.ReactionState.Status;
import com.quzzar.kithkyn.decision.ResidentReactions.Movement;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.ResidentWalk;
import net.minecraft.world.entity.ai.goal.Goal;

/** Physical movement below native panic, combat, recovery and travel. Pending inference holds no goal flags. */
public final class ResidentReactionGoal extends Goal {
  private final RealPerson person;
  private Movement movement;
  private ResidentWalk walk;
  public ResidentReactionGoal(RealPerson person) { this.person = person; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
  @Override public boolean canUse() { return person.reactions().movement().isPresent(); }
  @Override public boolean canContinueToUse() { return person.reactions().movement().filter(value -> value == movement).isPresent(); }
  @Override public boolean requiresUpdateEveryTick() { return true; }
  @Override public void start() { movement = person.reactions().movement().orElseThrow(); walk = new ResidentWalk(); person.reactions().underway(); }
  @Override public void stop() {
    if (canContinueToUse()) person.reactions().finish(Status.INTERRUPTED, "Native activity interrupted the reaction walk.");
    person.getNavigation().stop(); movement = null;
  }
  @Override public void tick() {
    if (!canContinueToUse()) return;
    if (!person.reactions().validMovement(movement)) { person.reactions().finish(Status.INTERRUPTED, "The reaction walk became unavailable."); return; }
    var step = walk.tick(person, movement.destination(), 1, 0.65D);
    if (step.state() == ResidentWalk.State.FAILED) person.reactions().finish(Status.FAILED, step.detail());
    else if (step.state() == ResidentWalk.State.ARRIVED) person.reactions().arrived(movement);
  }
}
