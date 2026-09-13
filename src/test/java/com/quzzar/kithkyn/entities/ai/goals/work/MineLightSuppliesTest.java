package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class MineLightSuppliesTest {

  @Test
  void recognisesFinishedTorchesAndEitherMineFuel() {
    assertTrue(MineLightSupplies.isSupply(new ItemStack(Items.TORCH)));
    assertTrue(MineLightSupplies.isSupply(new ItemStack(Items.COAL)));
    assertTrue(MineLightSupplies.isSupply(new ItemStack(Items.CHARCOAL)));
    assertFalse(MineLightSupplies.isSupply(new ItemStack(Items.COBBLESTONE)));
  }

  @Test
  void sizesFuelToTopUpTheWorkingPack() {
    assertEquals(4, MineLightSupplies.fuelNeeded(0));
    assertEquals(1, MineLightSupplies.fuelNeeded(15));
    assertEquals(0, MineLightSupplies.fuelNeeded(MineLightSupplies.PACK_TARGET));
  }
}
