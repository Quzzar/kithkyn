package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.HealthRecoveryPolicy;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A badly hurt villager goes to the cleric to be tended (Aaron, 2026-09-12: if
 * there is a cleric nearby, they are a better source of healing than the fire).
 *
 * <p>A personal need like eating, not a job: anyone hurt may do it. The patient
 * walks to within throwing reach of the nearest awake cleric of their village
 * and stands there; the cleric's own round ({@code HealStep}) sees a hurt
 * neighbour within ten blocks and lobs the potion. Eating is not displaced: it
 * holds no movement flag, so a villager carrying food eats on the walk and
 * while they wait. The visit ends when the patient is back over the line or
 * regenerating, when the cleric sleeps, leaves the village or dies, at night,
 * or after a minute of standing beside a cleric who has not thrown, so a
 * cleric out of potions cannot pin a patient for the day.
 */
public final class SeekClericGoal extends Goal {

  private static final double SPEED = 0.6D;
  private static final int SELECT_INTERVAL_TICKS = 20;
  private static final int NAVIGATION_REFRESH_TICKS = 10;

  private final RealPerson person;
  private final ApproachWatch approach;

  @Nullable
  private RealPerson cleric;
  private int nextSelectTick;
  private int nextNavigationTick;
  private int ticksWaiting;
  private boolean complete;

  public SeekClericGoal(RealPerson person) {
    this.person = person;
    this.approach = new ApproachWatch(person, "the cleric to be tended");
    this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    if (!maySeek() || this.approach.standingDown() || this.person.tickCount < this.nextSelectTick) {
      return false;
    }
    this.nextSelectTick = this.person.tickCount + SELECT_INTERVAL_TICKS;
    Village village = this.person.getVillage();
    if (village == null || !village.hasResident(this.person.getUUID())
        || !(this.person.level() instanceof ServerLevel level)) {
      return false;
    }
    this.cleric = nearestCleric(village, level);
    return this.cleric != null;
  }

  @Override
  public boolean canContinueToUse() {
    return !this.complete && this.cleric != null && maySeek() && isAvailable(this.cleric)
        && !HealthRecoveryPolicy.isBackOverTheLine(this.person.getHealth(), this.person.getMaxHealth())
        && this.ticksWaiting < HealthRecoveryPolicy.CLERIC_WAIT_TICKS;
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return true;
  }

  @Override
  public void start() {
    this.complete = false;
    this.ticksWaiting = 0;
    this.nextNavigationTick = this.person.tickCount;
    this.approach.begin();
    if (this.cleric != null) {
      Kithkyn.LOGGER.debug("{} is hurt and going to {} the cleric to be tended",
          this.person.getFullName(), this.cleric.getFullName());
    }
  }

  @Override
  public void stop() {
    this.person.getNavigation().stop();
    this.cleric = null;
    this.complete = false;
    this.ticksWaiting = 0;
  }

  @Override
  public void tick() {
    if (this.cleric == null) {
      this.complete = true;
      return;
    }
    double reach = HealthRecoveryPolicy.CLERIC_TENDING_REACH;
    if (this.person.distanceToSqr(this.cleric) <= reach * reach) {
      this.approach.arrived();
      this.person.getNavigation().stop();
      this.person.getLookControl().setLookAt(this.cleric, 30.0F, 30.0F);
      this.ticksWaiting++;
      return;
    }
    if (this.approach.giveUp(this.cleric.blockPosition())) {
      this.complete = true;
      return;
    }
    if (this.person.tickCount >= this.nextNavigationTick) {
      this.nextNavigationTick = this.person.tickCount + NAVIGATION_REFRESH_TICKS;
      this.person.getNavigation().moveTo(this.cleric, SPEED);
    }
  }

  /** Personal state that permits starting or carrying on with the visit. */
  private boolean maySeek() {
    return HealthRecoveryPolicy.shouldSeekCleric(this.person.getHealth(), this.person.getMaxHealth(),
            this.person.hasEffect(MobEffects.REGENERATION), this.person.level().isNight())
        && this.person.getTravelTarget() == null
        && this.person.getTarget() == null
        && !this.person.isAggressive()
        && !this.person.isSleeping()
        && !this.person.isImmobile()
        && !this.person.isInterrupted();
  }

  /** A cleric who could tend anyone right now: alive, awake, loaded, and not this villager. */
  private boolean isAvailable(RealPerson candidate) {
    return candidate != this.person && candidate.isAlive() && !candidate.isSleeping()
        && !candidate.isRemoved() && candidate.getOccupation() == Occupation.CLERIC;
  }

  @Nullable
  private RealPerson nearestCleric(Village village, ServerLevel level) {
    double range = HealthRecoveryPolicy.CLERIC_SEARCH_RANGE;
    RealPerson nearest = null;
    double nearestSqr = range * range;
    for (Map.Entry<UUID, JobAssignment> entry : village.getJobAssignmentsView().entrySet()) {
      if (entry.getValue().getOccupation() != Occupation.CLERIC) {
        continue;
      }
      RealPerson candidate = village.getPerson(level, entry.getKey());
      if (candidate == null || !isAvailable(candidate)) {
        continue;
      }
      double distanceSqr = this.person.distanceToSqr(candidate);
      if (distanceSqr <= nearestSqr) {
        nearest = candidate;
        nearestSqr = distanceSqr;
      }
    }
    return nearest;
  }

}
