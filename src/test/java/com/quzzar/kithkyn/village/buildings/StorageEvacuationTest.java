package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class StorageEvacuationTest {
  @Test
  void aPartialTransferCannotDuplicateWhatReachedStorage() {
    SimpleContainer source = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 20));
    SimpleContainer destination = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 60));

    assertFalse(StorageEvacuation.transfer(List.of(source), List.of(destination)));
    assertEquals(16, source.getItem(0).getCount());
    assertEquals(64, destination.getItem(0).getCount());
    assertEquals(80, source.getItem(0).getCount() + destination.getItem(0).getCount());
  }

  @Test
  void multipleFullSourcesEvacuateToPersistentOverflowWithoutLosingOrDuplicatingItems() {
    SimpleContainer first = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 40));
    SimpleContainer second = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 40));
    SimpleContainer destination = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 60));
    List<ItemStack> overflow = new ArrayList<>();

    assertTrue(StorageEvacuation.transfer(List.of(first, second), List.of(destination), overflow::add));
    assertTrue(first.isEmpty());
    assertTrue(second.isEmpty());
    assertEquals(64, destination.getItem(0).getCount());
    assertEquals(76, overflow.stream().mapToInt(ItemStack::getCount).sum());
    assertTrue(StorageEvacuation.transfer(List.of(first, second), List.of(destination), overflow::add));
    assertEquals(76, overflow.stream().mapToInt(ItemStack::getCount).sum());

    SimpleContainer retained = new SimpleContainer(overflow.toArray(ItemStack[]::new));
    SimpleContainer newStorage = new SimpleContainer(2);
    assertTrue(StorageEvacuation.transfer(List.of(retained), List.of(newStorage)));
    assertTrue(retained.isEmpty());
    assertEquals(76, newStorage.getItem(0).getCount() + newStorage.getItem(1).getCount());
  }

  @Test
  void aFailedOverflowSaveLeavesItsItemsInTheSource() {
    SimpleContainer source = new SimpleContainer(new ItemStack(Items.DIAMOND, 17));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
        () -> StorageEvacuation.transfer(List.of(source), List.of(), stack -> {
          throw new IllegalStateException("saved queue unreadable");
        }));
    assertEquals(17, source.getItem(0).getCount());
  }

  @Test
  void successfulEvacuationPreservesFullContentsWithoutApplyingTheSalvageRate() {
    SimpleContainer source = new SimpleContainer(new ItemStack(Items.DIAMOND, 17));
    SimpleContainer destination = new SimpleContainer(2);

    assertTrue(StorageEvacuation.transfer(List.of(source), List.of(destination)));
    assertTrue(source.isEmpty());
    assertEquals(17, destination.getItem(0).getCount());
  }
}
