package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.Materials;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The miner's non-optional working supply of torches and torch fuel. */
public final class MineLightSupplies {
  public static final int PACK_TARGET = 16;
  public static final int TORCHES_PER_FUEL = 4;

  private MineLightSupplies() {
  }

  public static boolean isSupply(ItemStack stack) {
    return stack.is(Items.TORCH) || stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
  }

  public static int fuelNeeded(int torchesHeld) {
    return Math.max(0, Math.ceilDiv(PACK_TARGET - torchesHeld, TORCHES_PER_FUEL));
  }

  /** Pulls finished torches first, then presses enough local fuel to top up the pack. */
  static int restock(RealPerson person, Container chest) {
    int before = person.personMainInv.countItem(Items.TORCH);
    PackLogistics.pullWanted(person, chest, List.of(new ItemStack(Items.TORCH, PACK_TARGET)),
        person.getOccupation().name());
    int held = person.personMainInv.countItem(Items.TORCH);
    int wantedFuel = fuelNeeded(held);
    int spent = takeFuel(chest, wantedFuel);
    if (spent > 0) {
      person.addItems(List.of(new ItemStack(Items.TORCH, spent * TORCHES_PER_FUEL)));
      Kithkyn.LOGGER.debug("[resource-flow] {} (MINER) pressed {} fuel into {} torches at a chest",
          person.getName().getString(), spent, spent * TORCHES_PER_FUEL);
    }
    return person.personMainInv.countItem(Items.TORCH) - before;
  }

  /** Presses fuel already mined into light without making a pointless chest trip. */
  static int craftCarried(RealPerson person) {
    int before = person.personMainInv.countItem(Items.TORCH);
    int spent = takeFuel(person.personMainInv, fuelNeeded(before));
    if (spent > 0) {
      person.addItems(List.of(new ItemStack(Items.TORCH, spent * TORCHES_PER_FUEL)));
      Kithkyn.LOGGER.debug("[resource-flow] {} (MINER) pressed {} carried fuel into {} torches",
          person.getName().getString(), spent, spent * TORCHES_PER_FUEL);
    }
    return person.personMainInv.countItem(Items.TORCH) - before;
  }

  private static int takeFuel(Container chest, int wanted) {
    int spent = Materials.take(chest, Items.COAL, wanted).stream().mapToInt(ItemStack::getCount).sum();
    if (spent < wanted) {
      spent += Materials.take(chest, Items.CHARCOAL, wanted - spent).stream()
          .mapToInt(ItemStack::getCount).sum();
    }
    return spent;
  }
}
