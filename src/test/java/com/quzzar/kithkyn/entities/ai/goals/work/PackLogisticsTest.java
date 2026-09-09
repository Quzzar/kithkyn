package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

class PackLogisticsTest {

  @Test
  void bedtimeStowRetainsAnythingSharedStorageRejects() {
    SimpleContainer pack = new SimpleContainer(new ItemStack(Items.DIAMOND, 20));
    SimpleContainer storage = new SimpleContainer(new ItemStack(Items.DIAMOND, 60));

    boolean allStored = PackLogistics.stowUnkept(pack, Set.of(),
        stack -> HopperBlockEntity.addItem(null, storage, stack, null));

    assertFalse(allStored);
    assertEquals(16, pack.getItem(0).getCount());
    assertEquals(64, storage.getItem(0).getCount());
    assertEquals(80, pack.getItem(0).getCount() + storage.getItem(0).getCount());
  }

  @Test
  void bedtimeStowLeavesHomeItemsInPlaceWithoutOfferingThemToStorage() {
    SimpleContainer pack = new SimpleContainer(new ItemStack(Items.APPLE, 3));
    AtomicBoolean called = new AtomicBoolean();

    assertTrue(PackLogistics.stowUnkept(pack, Set.of(Items.APPLE), stack -> {
      called.set(true);
      return ItemStack.EMPTY;
    }));
    assertFalse(called.get());
    assertEquals(3, pack.getItem(0).getCount());
  }
}
