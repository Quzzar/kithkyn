package com.quzzar.kithkyn.wrongdoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class EvidenceInventoryTest {
  @Test
  void fullEvidenceNeverRemovesThePrisonersItems() {
    SimpleContainer evidence = new SimpleContainer(new ItemStack(Items.COBBLESTONE, 64));
    AtomicReference<ItemStack> source = new AtomicReference<>(new ItemStack(Items.DIAMOND, 3));
    assertEquals(0, EvidenceInventory.transfer(source.get(), List.of(evidence), source::set));
    assertEquals(3, source.get().getCount());
    assertEquals(64, evidence.getItem(0).getCount());
  }

  @Test
  void partialCapacityLeavesExactOverflowAtTheOriginalSource() {
    SimpleContainer evidence = new SimpleContainer(new ItemStack(Items.DIAMOND, 62));
    AtomicReference<ItemStack> source = new AtomicReference<>(new ItemStack(Items.DIAMOND, 7));
    assertEquals(2, EvidenceInventory.transfer(source.get(), List.of(evidence), source::set));
    assertEquals(5, source.get().getCount());
    assertEquals(64, evidence.getItem(0).getCount());
  }

  @Test
  void componentsArePreservedAndDoNotMergeWithDifferentEvidence() {
    SimpleContainer first = new SimpleContainer(new ItemStack(Items.DIAMOND, 62));
    SimpleContainer second = new SimpleContainer(1);
    ItemStack named = new ItemStack(Items.DIAMOND, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Royal keepsake"));
    AtomicReference<ItemStack> source = new AtomicReference<>(named);
    assertEquals(7, EvidenceInventory.transfer(source.get(), List.of(first, second), source::set));
    assertTrue(source.get().isEmpty());
    assertEquals(62, first.getItem(0).getCount());
    assertTrue(ItemStack.matches(named, second.getItem(0)));
  }

  @Test
  void repeatedReferencesCannotInventExtraCapacity() {
    SimpleContainer evidence = new SimpleContainer(new ItemStack(Items.DIAMOND, 63));
    AtomicReference<ItemStack> source = new AtomicReference<>(new ItemStack(Items.DIAMOND, 7));
    assertEquals(1, EvidenceInventory.transfer(source.get(), List.of(evidence, evidence), source::set));
    assertEquals(6, source.get().getCount());
    assertEquals(64, evidence.getItem(0).getCount());
  }
}
