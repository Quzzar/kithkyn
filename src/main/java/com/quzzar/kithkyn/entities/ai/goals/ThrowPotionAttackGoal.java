package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.OffHandUse;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * A cleric with harm to throw throws it at the village's enemies (Aaron,
 * 2026-09-12: the swamp's witches are clerics, and a cleric's loadout decides
 * whether they heal, hex, or both).
 *
 * <p>Only a cleric carrying a harmful splash or lingering potion that would
 * hurt this target, with one to spare, ever has a target here (the threat goal
 * in {@code RealPerson.registerGoals} checks the same stock). Harm is judged on
 * the body: a splash of harming heals the undead, so it is never thrown at a
 * zombie. Each throw consumes one bottle. The two safety rules are absolute: a
 * harmful potion is never thrown while an ally stands inside the burst around
 * the target, and never through a friend in the way. When either holds, the
 * cleric closes in and waits for a clear throw rather than throwing badly. Out
 * of harm, the goal ends and the target is dropped; the cleric's healing round
 * takes over if anyone is hurt.
 *
 * <p>The throw is made from the off hand ({@link OffHandUse}): the bottle comes
 * up into it as the cleric crouches to aim, and the hand's resting bottle comes
 * back once it is thrown or the aim is broken off.
 */
public final class ThrowPotionAttackGoal extends Goal {

  private static final double SPEED = 0.6D;
  private static final float THROW_RANGE = 7.0F;
  private static final int THROW_INTERVAL_TICKS = 40;
  private static final int CHARGE_TICKS = 20;

  private final RealPerson person;
  private final OffHandUse offHand = new OffHandUse();
  private int cooldown;
  private int charge;

  public ThrowPotionAttackGoal(RealPerson person) {
    this.person = person;
    this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override
  public boolean canUse() {
    LivingEntity target = this.person.getTarget();
    return target != null && target.isAlive() && ClericPotions.isCleric(this.person)
        && !this.person.isSleeping() && !this.person.isEating() && harmFor(target) != null;
  }

  @Override
  public boolean canContinueToUse() {
    return canUse();
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return true;
  }

  @Override
  public void start() {
    this.cooldown = 0;
    this.charge = 0;
  }

  @Override
  public void stop() {
    this.person.getNavigation().stop();
    this.person.setPose(Pose.STANDING);
    this.offHand.restore(this.person);
    this.charge = 0;
    LivingEntity target = this.person.getTarget();
    if (target == null || harmFor(target) == null) {
      this.person.setTarget(null); // nothing left to fight this one with
    }
  }

  @Override
  public void tick() {
    LivingEntity target = this.person.getTarget();
    if (target == null) {
      return;
    }
    this.person.getLookControl().setLookAt(target, 30.0F, 30.0F);
    double reach = THROW_RANGE * 0.75D;
    boolean inReach = this.person.distanceToSqr(target) <= reach * reach;
    boolean clear = this.person.getSensing().hasLineOfSight(target)
        && !RangedShotSafety.blockedByFriendly(this.person, target)
        && !ClericPotions.harmfulSplashWouldCatchAlly(this.person, target);
    if (!inReach || !clear) {
      breakOffAim();
      this.person.getNavigation().moveTo(target, SPEED);
      return;
    }
    this.person.getNavigation().stop();
    if (this.cooldown > 0) {
      this.cooldown--;
      return;
    }
    if (this.charge == 0) {
      ItemStack harm = harmFor(target);
      if (harm == null || !this.offHand.ready(this.person, harm)) {
        return;
      }
    }
    // Crouch to steady the throw, as the healing round does.
    this.person.setPose(Pose.CROUCHING);
    if (++this.charge < CHARGE_TICKS) {
      return;
    }
    ItemStack held = this.person.getOffhandItem();
    if (ClericPotions.isThrowable(held) && ClericPotions.outcomeOn(held, target) == ClericPotions.Outcome.HURTS) {
      this.person.swing(InteractionHand.OFF_HAND);
      Kithkyn.LOGGER.debug("'{}' threw a {} at {}", this.person.getFullName(),
          held.getHoverName().getString(), target.getName().getString());
      ClericPotions.throwOne(this.person, target, held);
    }
    this.offHand.restore(this.person);
    this.person.setPose(Pose.STANDING);
    this.charge = 0;
    this.cooldown = THROW_INTERVAL_TICKS;
  }

  private void breakOffAim() {
    if (this.charge > 0) {
      this.offHand.restore(this.person);
    }
    this.charge = 0;
    this.person.setPose(Pose.STANDING);
  }

  /** A harmful bottle to spare that would hurt this target, or null. */
  @Nullable
  private ItemStack harmFor(LivingEntity target) {
    return ClericPotions.throwable(this.person, stack -> ClericPotions.isHarmful(stack)
        && ClericPotions.outcomeOn(stack, target) == ClericPotions.Outcome.HURTS);
  }

}
