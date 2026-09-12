package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

class ClericPotionsTest {

  private static ItemStack splash(Holder<Potion> potion) {
    ItemStack stack = new ItemStack(Items.SPLASH_POTION);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  @Test
  void onlySplashAndLingeringPotionsWithEffectsAreThrowable() {
    assertTrue(ClericPotions.isThrowable(splash(Potions.HEALING)));
    ItemStack lingering = new ItemStack(Items.LINGERING_POTION);
    lingering.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));
    assertTrue(ClericPotions.isThrowable(lingering));
    ItemStack drinkable = new ItemStack(Items.POTION);
    drinkable.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
    assertFalse(ClericPotions.isThrowable(drinkable));
    assertFalse(ClericPotions.isThrowable(splash(Potions.WATER)));
    assertFalse(ClericPotions.isThrowable(new ItemStack(Items.SPLASH_POTION)));
    assertFalse(ClericPotions.isThrowable(ItemStack.EMPTY));
  }

  @Test
  void harmIsNeverBeneficialAndHealingIsNeverHarmful() {
    assertTrue(ClericPotions.isHarmful(splash(Potions.HARMING)));
    assertFalse(ClericPotions.isBeneficial(splash(Potions.HARMING)));
    assertTrue(ClericPotions.isBeneficial(splash(Potions.HEALING)));
    assertFalse(ClericPotions.isHarmful(splash(Potions.HEALING)));
    assertTrue(ClericPotions.isHarmful(splash(Potions.POISON)));
    assertTrue(ClericPotions.isBeneficial(splash(Potions.REGENERATION)));
  }

  @Test
  void brewsAreTalliedByPotionNotByItem() {
    SimpleContainer pack = new SimpleContainer(9);
    pack.setItem(0, splash(Potions.HEALING));
    pack.setItem(3, splash(Potions.HARMING));
    pack.setItem(5, splash(Potions.HEALING));
    List<ClericPotions.Stock> stock = ClericPotions.stock(splash(Potions.REGENERATION), ItemStack.EMPTY, pack);

    assertEquals(3, stock.size());
    assertEquals(2, ClericPotions.carried(splash(Potions.REGENERATION), ItemStack.EMPTY, pack, splash(Potions.HEALING)));
    assertEquals(1, ClericPotions.carried(splash(Potions.REGENERATION), ItemStack.EMPTY, pack, splash(Potions.HARMING)));
    assertEquals(0, ClericPotions.carried(splash(Potions.REGENERATION), ItemStack.EMPTY, pack, splash(Potions.POISON)));
  }

  @Test
  void theLastPotionOfABrewIsNeverThrown() {
    SimpleContainer pack = new SimpleContainer(9);
    ItemStack hand = splash(Potions.REGENERATION);
    pack.setItem(0, splash(Potions.HARMING));

    // One of each: nothing to spare, of either kind.
    assertNull(ClericPotions.throwable(hand, ItemStack.EMPTY, pack, ClericPotions::isBeneficial));
    assertNull(ClericPotions.throwable(hand, ItemStack.EMPTY, pack, ClericPotions::isHarmful));

    // A second regeneration makes the hand's bottle throwable; harm still is not.
    pack.setItem(1, splash(Potions.REGENERATION));
    assertSame(hand, ClericPotions.throwable(hand, ItemStack.EMPTY, pack, ClericPotions::isBeneficial));
    assertNull(ClericPotions.throwable(hand, ItemStack.EMPTY, pack, ClericPotions::isHarmful));
  }

  @Test
  void aClericMayGiveEverythingButTheSeed() {
    SimpleContainer pack = new SimpleContainer(9);
    ItemStack hand = splash(Potions.REGENERATION);
    ItemStack spare = splash(Potions.REGENERATION);
    pack.setItem(0, spare);
    ItemStack bread = new ItemStack(Items.BREAD, 5);
    pack.setItem(1, bread);

    assertEquals(1, ClericPotions.giveable(hand, ItemStack.EMPTY, pack, spare));
    assertEquals(1, ClericPotions.giveable(hand, ItemStack.EMPTY, pack, hand));
    assertEquals(5, ClericPotions.giveable(hand, ItemStack.EMPTY, pack, bread));

    pack.setItem(0, ItemStack.EMPTY);
    assertEquals(0, ClericPotions.giveable(hand, ItemStack.EMPTY, pack, hand));
  }

  @Test
  void brewingRestocksTheEmptiestBrewFirstAndStopsAtTheTarget() {
    SimpleContainer pack = new SimpleContainer(9);
    ItemStack hand = splash(Potions.REGENERATION);
    pack.setItem(0, splash(Potions.HEALING));
    pack.setItem(1, splash(Potions.REGENERATION));

    ClericPotions.Stock lowest = ClericPotions.lowestBelowTarget(hand, ItemStack.EMPTY, pack);
    assertNotNull(lowest);
    assertTrue(ClericPotions.sameBrew(lowest.sample(), splash(Potions.HEALING)));
    assertEquals(1, lowest.count());

    for (int slot = 2; slot < 2 + ClericPotions.STOCK_TARGET; slot++) {
      pack.setItem(slot, splash(Potions.HEALING));
    }
    for (int slot = 6; slot < 6 + ClericPotions.STOCK_TARGET - 2; slot++) {
      pack.setItem(slot, splash(Potions.REGENERATION));
    }
    assertNull(ClericPotions.lowestBelowTarget(hand, ItemStack.EMPTY, pack));
  }
}
