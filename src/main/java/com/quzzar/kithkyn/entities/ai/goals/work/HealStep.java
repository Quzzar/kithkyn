package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.OffHandUse;
import com.quzzar.kithkyn.entities.Person;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.RangedShotSafety;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The cleric's round: an APPLY step whose target is a person rather than a
 * place, and whose act is thrown rather than touched.
 *
 * The first step that is not {@link BlockWorkStep}, and the reason
 * {@link WorkStep} asks where a target IS rather than being handed a position.
 * A hurt villager walks about while being treated, so the cleric follows them.
 *
 * Reach here is throwing distance, not arm's length: the cleric closes to
 * within three quarters of their range, crouches to steady the throw, waits out
 * a charge that is longer the further away the patient is, and lobs a splash
 * potion from their own stock ({@link ClericPotions}): healing if the patient
 * is nearly dead, regeneration otherwise, then anything else that mends, and
 * only a brew that helps that patient as a whole. Each throw consumes a bottle,
 * and the last of a brew is never thrown, so a cleric with only their seed
 * potions left has nobody to tend until they brew more. A helpful throw is never
 * made while an enemy stands inside the burst or in the way (Aaron,
 * 2026-09-12): the cleric holds it until the enemy is clear.
 *
 * The bottle comes up into the off hand as the cleric crouches, and the hand's
 * resting bottle comes back once it is thrown or the throw is held
 * ({@link OffHandUse}).
 */
public final class HealStep implements WorkStep<LivingEntity> {

  private static final double SEARCH_RANGE = 10.0D;

  private final int minChargeTicks;
  private final int maxChargeTicks;
  private final float throwRange;
  private final OffHandUse offHand = new OffHandUse();

  private boolean charging;
  private int chargeLeft;

  public HealStep(int minChargeTicks, int maxChargeTicks, float throwRange) {
    this.minChargeTicks = minChargeTicks;
    this.maxChargeTicks = maxChargeTicks;
    this.throwRange = throwRange;
  }

  @Override
  @Nullable
  public LivingEntity select(RealPerson person) {
    if (person.isSleeping()) {
      return null;
    }
    List<LivingEntity> nearby = person.level().getEntitiesOfClass(LivingEntity.class,
        person.getBoundingBox().inflate(SEARCH_RANGE, 3.0D, SEARCH_RANGE));
    for (LivingEntity candidate : nearby) {
      if (candidate != null && !candidate.hasEffect(MobEffects.REGENERATION) && needsTending(person, candidate)
          && brewFor(person, candidate) != null) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public BlockPos positionOf(LivingEntity target) {
    return target.blockPosition();
  }

  @Override
  public boolean act(RealPerson person, LivingEntity target) {
    if (!needsTending(person, target)) {
      return false; // mended, or dead, or somebody else got there
    }
    ItemStack brew = brewFor(person, target);
    if (brew == null) {
      return false; // the stock ran out while walking over
    }
    // A potion thrown at a wall helps nobody. Hold the charge and close up.
    if (!person.getSensing().hasLineOfSight(target)) {
      holdThrow(person);
      person.getNavigation().moveTo(target, 0.5D);
      return true;
    }
    // A helpful splash that would also reach an enemy, or break on one on the
    // way, is held until the enemy is clear.
    if (ClericPotions.helpfulSplashWouldCatchEnemy(person, target) || RangedShotSafety.blockedByEnemy(person, target)) {
      holdThrow(person);
      return true;
    }
    if (!this.charging) {
      if (!this.offHand.ready(person, brew)) {
        return true;
      }
      this.charging = true;
      person.setPose(Pose.CROUCHING);
      this.chargeLeft = (int) Mth.lerp(person.distanceTo(target) / this.throwRange,
          this.minChargeTicks, this.maxChargeTicks);
      return true;
    }
    if (this.chargeLeft-- > 0) {
      return true;
    }
    ItemStack held = person.getOffhandItem();
    if (ClericPotions.isThrowable(held) && ClericPotions.outcomeOn(held, target) == ClericPotions.Outcome.HELPS) {
      person.swing(InteractionHand.OFF_HAND);
      Kithkyn.LOGGER.debug("'{}' threw a {} to {}", person.getFullName(), held.getHoverName().getString(),
          target.getName().getString());
      ClericPotions.throwOne(person, target, held);
    }
    this.offHand.restore(person);
    this.charging = false;
    person.setPose(Pose.STANDING);
    return false;
  }

  @Override
  public void released(RealPerson person, LivingEntity target) {
    holdThrow(person);
    this.chargeLeft = 0;
  }

  @Override
  public String describe() {
    return "anyone who needs tending";
  }

  @Override
  public String activity() {
    return "tending the hurt";
  }

  /** Close enough to throw, not close enough to touch. */
  @Override
  public double reachSqr(RealPerson person) {
    double range = this.throwRange * 0.75D;
    return range * range;
  }

  /** The charge is counted in seconds, as it was. */
  @Override
  public int actEveryTicks() {
    return 20;
  }

  /** Stand down from a throw: the resting bottle back in hand, the charge undone. */
  private void holdThrow(RealPerson person) {
    this.offHand.restore(person);
    this.charging = false;
    person.setPose(Pose.STANDING);
  }

  private boolean needsTending(RealPerson person, LivingEntity candidate) {
    if (candidate == person || !candidate.isAlive()
        || candidate.getHealth() >= candidate.getMaxHealth()) {
      return false;
    }
    if (candidate instanceof Person) {
      return true;
    }
    return candidate instanceof Player player && !player.getAbilities().instabuild;
  }

  /** The bottle this cleric would throw over this patient, or null when they can spare none that helps. */
  @Nullable
  private static ItemStack brewFor(RealPerson person, LivingEntity patient) {
    return ClericPotions.throwable(person, ClericPotions.healingPreference(patient));
  }

}
