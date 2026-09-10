package com.quzzar.kithkyn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quzzar.kithkyn.village.buildings.Materials;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class UtilsInventoryTest {
  @Test
  void actualMultiStackRecipeFetchDoesNotLoseItemsAtAnEmptyPackSlot() {
    SimpleContainer chest = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64));
    SimpleContainer pack = new SimpleContainer(4);
    List<ItemStack> dropped = new ArrayList<>();
    var taken = Materials.takeToward(chest, Items.COBBLESTONE, 128);
    assertEquals(128, taken.getFirst().getCount());
    Utils.insertItemsOrDrop(pack, taken, dropped::add);
    assertEquals(128, pack.countItem(Items.COBBLESTONE));
    assertTrue(chest.isEmpty());
    assertTrue(dropped.isEmpty());
    for (int slot = 0; slot < pack.getContainerSize(); slot++) assertTrue(pack.getItem(slot).getCount() <= 64);
  }

  @Test
  void partialRestrictedStoragePreservesNamedOverflowAndDestinationLimits() {
    SimpleContainer storage = new SimpleContainer(3) {
      @Override public int getMaxStackSize() { return 16; }
      @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot != 2; }
    };
    ItemStack goods = new ItemStack(Items.COBBLESTONE, 100);
    goods.set(DataComponents.CUSTOM_NAME, Component.literal("Castle stone"));
    storage.setItem(0, goods.copyWithCount(15));
    storage.setItem(2, new ItemStack(Items.DIAMOND, 10));
    List<ItemStack> dropped = new ArrayList<>();
    Utils.insertItemsOrDrop(storage, List.of(goods), dropped::add);
    assertEquals(16, storage.getItem(0).getCount());
    assertEquals(16, storage.getItem(1).getCount());
    assertEquals(83, dropped.stream().mapToInt(ItemStack::getCount).sum());
    assertEquals(115, storage.countItem(Items.COBBLESTONE) + dropped.stream().mapToInt(ItemStack::getCount).sum());
    assertEquals(10, storage.countItem(Items.DIAMOND));
    assertTrue(dropped.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()
        && stack.getHoverName().getString().equals("Castle stone")));
    assertEquals("Castle stone", storage.getItem(1).getHoverName().getString());
  }

  @Test
  void fullOrAbsentContainersDropAllGoodsWithoutMergingDistinctComponents() {
    for (SimpleContainer storage : new SimpleContainer[] {new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64)), null}) {
      ItemStack goods = new ItemStack(Items.COBBLESTONE, 128);
      goods.set(DataComponents.CUSTOM_NAME, Component.literal("Named stone"));
      List<ItemStack> dropped = new ArrayList<>();
      Utils.insertItemsOrDrop(storage, List.of(goods), dropped::add);
      assertEquals(128, dropped.stream().mapToInt(ItemStack::getCount).sum());
      assertEquals(2, dropped.size());
      assertTrue(dropped.stream().allMatch(stack -> stack.getHoverName().getString().equals("Named stone")));
      if (storage != null) assertEquals(64, storage.countItem(Items.COBBLESTONE));
    }
  }
}
