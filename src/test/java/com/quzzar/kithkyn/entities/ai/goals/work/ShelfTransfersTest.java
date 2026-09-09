package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.ShelvingPlan;
import com.quzzar.kithkyn.village.Storehouse;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class ShelfTransfersTest {
  private static ShelvingPlan twoShelves() {
    return new ShelvingPlan(List.of(
        new ShelvingPlan.Category("Food", List.of("minecraft:apple"), 0, 1),
        new ShelvingPlan.Category("Stone", List.of("minecraft:cobblestone"), 1, 1)), 2);
  }

  @Test
  void fullMisfiledShelvesAreSortedByCarryingAndExchangingRealStacks() {
    var first = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64));
    var second = new SimpleContainer(new ItemStack(Items.APPLE, 64));
    var pack = new SimpleContainer(1);
    assertEquals(64, ShelfTransfers.collectOne(first, pack));
    assertTrue(first.isEmpty());
    assertTrue(second.getItem(0).is(Items.APPLE));
    assertEquals(64, ShelfTransfers.depositOne(pack, second, 1, twoShelves(), true));
    assertTrue(pack.getItem(0).is(Items.APPLE));
    assertTrue(second.getItem(0).is(Items.COBBLESTONE));
    assertEquals(64, ShelfTransfers.depositOne(pack, first, 0, twoShelves(), true));
    assertTrue(pack.isEmpty());
    assertEquals(64, first.countItem(Items.APPLE));
    assertEquals(64, second.countItem(Items.COBBLESTONE));
  }

  @Test
  void capacityProbeDoesNotTakeGoodsOrMutateShelves() {
    var pack = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 12));
    var shelf = new SimpleContainer(new ItemStack(Items.APPLE, 64));
    assertTrue(ShelfTransfers.canDeposit(pack, shelf, 1, twoShelves(), true));
    assertEquals(12, pack.countItem(Items.COBBLESTONE));
    assertEquals(64, shelf.countItem(Items.APPLE));
  }

  @Test
  void fullCorrectShelvesDoNotEvictGoodsIntoAnEndlessDeliveryLoop() {
    var pack = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64));
    var shelf = new SimpleContainer(new ItemStack(Items.APPLE, 64));
    assertFalse(ShelfTransfers.canDeposit(pack, shelf, 0, twoShelves(), true));
    assertFalse(ShelfTransfers.canDeposit(pack, shelf, 0, twoShelves(), false));
    assertEquals(0, ShelfTransfers.depositOne(pack, shelf, 0, twoShelves(), false));
    assertEquals(64, pack.countItem(Items.COBBLESTONE));
    assertEquals(64, shelf.countItem(Items.APPLE));
  }

  @Test
  void preferredDeliveryWaitsForTheContainerWithTheCorrectSlotRange() {
    var pack = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 10));
    var first = new SimpleContainer(1);
    var second = new SimpleContainer(1);
    assertFalse(ShelfTransfers.canDeposit(pack, first, 0, twoShelves(), true));
    assertTrue(ShelfTransfers.canDeposit(pack, second, 1, twoShelves(), true));
    assertEquals(10, ShelfTransfers.depositOne(pack, second, 1, twoShelves(), true));
    assertTrue(first.isEmpty());
    assertTrue(pack.isEmpty());
  }

  @Test
  void oneActMovesOneStackAndLeavesTheRestForLater() {
    var pack = new SimpleContainer(new ItemStack(Items.APPLE, 40), new ItemStack(Items.COBBLESTONE, 20));
    var shelf = new SimpleContainer(3);
    assertEquals(40, ShelfTransfers.depositOne(pack, shelf, 0, null, false));
    assertEquals(20, pack.countItem(Items.COBBLESTONE));
    assertEquals(0, shelf.countItem(Items.COBBLESTONE));
  }

  @Test
  void collectionRetainsThePartThatDoesNotFit() {
    var pack = new SimpleContainer(new ItemStack(Items.APPLE, 60));
    var source = new SimpleContainer(new ItemStack(Items.APPLE, 30));
    assertEquals(4, ShelfTransfers.collectOne(source, pack));
    assertEquals(64, pack.countItem(Items.APPLE));
    assertEquals(26, source.countItem(Items.APPLE));
    assertEquals(0, ShelfTransfers.collectOne(source, pack));
  }

  @Test
  void namedGoodsKeepTheirComponentsDuringTransfersAndLocalTidying() {
    ItemStack named = new ItemStack(Items.APPLE, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Harvest prize"));
    var pack = new SimpleContainer(named, new ItemStack(Items.APPLE, 9));
    var shelf = new SimpleContainer(3);
    assertEquals(7, ShelfTransfers.depositOne(pack, shelf, 0, null, false));
    assertEquals(9, ShelfTransfers.depositOne(pack, shelf, 0, null, false));
    Storehouse.arrange(List.of(shelf), null);
    assertEquals(16, shelf.countItem(Items.APPLE));
    assertEquals(7, shelf.getItem(0).getCount());
    assertEquals(Component.literal("Harvest prize"), shelf.getItem(0).get(DataComponents.CUSTOM_NAME));
    assertEquals(9, shelf.getItem(1).getCount());
    assertNull(shelf.getItem(1).get(DataComponents.CUSTOM_NAME));
  }

  @Test
  void aContainerRejectingTheItemRemainsUntouched() {
    var shelf = new SimpleContainer(1) {
      @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    };
    var pack = new SimpleContainer(new ItemStack(Items.APPLE, 8));
    assertFalse(ShelfTransfers.canDeposit(pack, shelf, 0, null, false));
    assertEquals(0, ShelfTransfers.depositOne(pack, shelf, 0, null, false));
    assertEquals(8, pack.countItem(Items.APPLE));
    assertTrue(shelf.isEmpty());
  }
}
