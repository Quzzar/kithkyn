package com.quzzar.kithkyn.entities.ai.goals;

import java.util.Locale;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.OffHandUse;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * A villager drinks an ordinary potion that answers what is wrong with them
 * right now (Aaron, 2026-09-12: ordinary potions are drunk, splash and
 * lingering ones thrown). Out of air under water, water breathing; burning,
 * fire resistance; hurt below the drinking line, healing, regeneration,
 * absorption or health boost ({@link ClericPotions#need}). Only a potion that
 * helps the drinker as a whole is drunk, so nobody drinks harm, and a cleric
 * never drinks the last bottle of a brew, the recipe they brew more from.
 *
 * <p>The bottle is drunk from the off hand, the hand a cleric works from
 * ({@link OffHandUse}): it comes up from the pack, the hand's own item goes
 * down into its slot, and once the drink is done that item comes back. The
 * drink itself is the vanilla use, animation and sound included, and when it
 * completes {@code Person.completeUsingItem} applies the effects and empties
 * the bottle. Like eating, it holds no movement flag, so a villager drinks on
 * the move.
 */
public final class DrinkPotionGoal extends Goal {

  /** Needs are read once a second, not every tick. */
  private static final int CHECK_INTERVAL_TICKS = 20;

  private final RealPerson person;
  private final OffHandUse offHand = new OffHandUse();

  @Nullable
  private ItemStack potion;
  private int nextCheckTick;

  public DrinkPotionGoal(RealPerson person) {
    this.person = person;
  }

  @Override
  public boolean canUse() {
    if (this.person.tickCount < this.nextCheckTick || this.person.isSleeping() || this.person.isEating()
        || this.person.isUsingItem() || OffHandUse.inUse(this.person)) {
      return false;
    }
    this.nextCheckTick = this.person.tickCount + CHECK_INTERVAL_TICKS;
    this.potion = ClericPotions.drinkFor(this.person);
    return this.potion != null;
  }

  @Override
  public boolean canContinueToUse() {
    return this.potion != null && this.person.isUsingItem() && this.person.getUseItem() == this.potion;
  }

  @Override
  public void start() {
    if (this.potion == null || !this.offHand.ready(this.person, this.potion)) {
      this.potion = null;
      return;
    }
    Kithkyn.LOGGER.info("'{}' is drinking a {}", this.person.getFullName(),
        this.potion.getHoverName().getString().toLowerCase(Locale.ROOT));
    this.person.startUsingItem(InteractionHand.OFF_HAND);
  }

  @Override
  public void stop() {
    if (this.potion != null && this.person.isUsingItem() && this.person.getUseItem() == this.potion) {
      this.person.stopUsingItem();
    }
    this.offHand.restore(this.person);
    this.potion = null;
  }
}
