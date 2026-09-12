package com.quzzar.kithkyn.entities.ai.goals.work;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.LocationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * The cleric's other round: brewing more of what they carry.
 *
 * <p>A CONVERT act at the cleric's own station (Aaron, 2026-09-12). The stand
 * needs nothing put in it: one potion the cleric already carries stands for the
 * recipe, and a session as long as a brewing stand's yields three more of that
 * brew into the pack. What the cleric can make is therefore exactly what they
 * were handed: a healer given a splash of harming starts brewing harm. The
 * brew furthest below {@link ClericPotions#STOCK_TARGET} is made first, and a
 * pack with no room for a bottle waits.
 */
public final class BrewStep implements BlockWorkStep {

  private int ticksBrewing;

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    if (!ClericPotions.isCleric(person) || person.isSleeping() || person.getVillage() == null) {
      return null;
    }
    BlockPos station = LocationManager.getJobLocation(person);
    if (station.equals(BlockPos.ZERO) || ClericPotions.lowestBelowTarget(person) == null
        || emptySlots(person) == 0) {
      return null;
    }
    return station;
  }

  @Override
  public void acquired(RealPerson person, BlockPos target) {
    this.ticksBrewing = 0;
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    ClericPotions.Stock brew = ClericPotions.lowestBelowTarget(person);
    if (brew == null) {
      return false; // stocked meanwhile, or the seed was handed away
    }
    this.ticksBrewing += actEveryTicks();
    if (this.ticksBrewing < ClericPotions.BREW_TICKS) {
      return true;
    }
    int made = Math.min(ClericPotions.BREW_YIELD, emptySlots(person));
    for (int index = 0; index < made; index++) {
      person.personMainInv.addItem(brew.sample().copyWithCount(1));
    }
    Kithkyn.LOGGER.info("'{}' brewed {} more {} at their station", person.getFullName(), made,
        brew.sample().getHoverName().getString().toLowerCase(java.util.Locale.ROOT));
    this.ticksBrewing = 0;
    return false;
  }

  @Override
  public void released(RealPerson person, BlockPos target) {
    this.ticksBrewing = 0;
  }

  @Override
  public String describe() {
    return "the brewing station";
  }

  @Override
  public String activity() {
    return "brewing potions";
  }

  /** Potions do not stack, so each bottle brewed needs a slot of its own. */
  private static int emptySlots(RealPerson person) {
    int empty = 0;
    for (int slot = 0; slot < person.personMainInv.getContainerSize(); slot++) {
      if (person.personMainInv.getItem(slot).isEmpty()) {
        empty++;
      }
    }
    return empty;
  }

}
