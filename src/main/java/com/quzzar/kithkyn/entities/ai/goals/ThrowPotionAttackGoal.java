package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.entities.ClericPotions;
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
 * <p>Only a cleric carrying a harmful splash potion with one to spare ever has
 * a target here (the threat goal in {@code RealPerson.registerGoals} checks the
 * same stock), and each throw consumes one bottle. The two safety rules are
 * absolute: a harmful potion is never thrown while an ally stands inside the
 * burst around the target, and never through a friend in the way. When either
 * holds, the cleric closes in and waits for a clear throw rather than throwing
 * badly. Out of harm, the goal ends and the target is dropped; the cleric's
 * healing round takes over if anyone is hurt.
 */
public final class ThrowPotionAttackGoal extends Goal {

  private static final double SPEED = 0.6D;
  private static final float THROW_RANGE = 7.0F;
  private static final int THROW_INTERVAL_TICKS = 40;
  private static final int CHARGE_TICKS = 20;

  private final RealPerson person;
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
        && !this.person.isSleeping() && !this.person.isEating()
        && ClericPotions.hasThrowable(this.person, ClericPotions::isHarmful);
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
    if (!ClericPotions.hasThrowable(this.person, ClericPotions::isHarmful)) {
      this.person.setTarget(null); // nothing left to fight with
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
      this.charge = 0;
      this.person.setPose(Pose.STANDING);
      this.person.getNavigation().moveTo(target, SPEED);
      return;
    }
    this.person.getNavigation().stop();
    if (this.cooldown > 0) {
      this.cooldown--;
      return;
    }
    // Crouch to steady the throw, as the healing round does.
    this.person.setPose(Pose.CROUCHING);
    if (++this.charge < CHARGE_TICKS) {
      return;
    }
    ItemStack harm = ClericPotions.throwable(this.person, ClericPotions::isHarmful);
    if (harm != null) {
      this.person.swing(InteractionHand.MAIN_HAND);
      ClericPotions.throwOne(this.person, target, harm);
    }
    this.person.setPose(Pose.STANDING);
    this.charge = 0;
    this.cooldown = THROW_INTERVAL_TICKS;
  }

}
