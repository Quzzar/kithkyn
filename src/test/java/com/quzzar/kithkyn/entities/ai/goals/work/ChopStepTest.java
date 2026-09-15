package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

class ChopStepTest {

  @Test
  void standHarvestConvertsOneLogWhenTheCanopyDropsNoSapling() {
    List<ItemStack> haul = new ArrayList<>(List.of(new ItemStack(Items.SPRUCE_LOG, 7)));

    ChopStep.reserveForNextStand(Blocks.SPRUCE_LOG.defaultBlockState(), haul);

    assertEquals(6, count(haul, Items.SPRUCE_LOG));
    assertEquals(1, count(haul, Items.SPRUCE_SAPLING));
  }

  @Test
  void standHarvestKeepsNaturalSaplingDropsWithoutChargingAnotherLog() {
    List<ItemStack> haul = new ArrayList<>(List.of(
        new ItemStack(Items.OAK_LOG, 7), new ItemStack(Items.OAK_SAPLING, 2)));

    ChopStep.reserveForNextStand(Blocks.OAK_LOG.defaultBlockState(), haul);

    assertEquals(7, count(haul, Items.OAK_LOG));
    assertEquals(2, count(haul, Items.OAK_SAPLING));
  }

  @Test
  void darkOakUsesAOneBlockStandCompatibleCutting() {
    List<ItemStack> haul = new ArrayList<>(List.of(new ItemStack(Items.DARK_OAK_LOG, 3)));

    ChopStep.reserveForNextStand(Blocks.DARK_OAK_LOG.defaultBlockState(), haul);

    assertEquals(2, count(haul, Items.DARK_OAK_LOG));
    assertEquals(1, count(haul, Items.OAK_SAPLING));
  }

  private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
    return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
  }
}
