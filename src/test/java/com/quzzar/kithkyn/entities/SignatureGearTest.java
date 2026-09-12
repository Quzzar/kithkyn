package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

class SignatureGearTest {

  private static ItemStack potion(Item item, Holder<Potion> potion) {
    ItemStack stack = new ItemStack(item);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  @Test
  void aClericHoldsOnlyASplashOfRegenerationAndOnlyInTheOffHand() {
    List<SignatureGear.Piece> pieces = SignatureGear.of(Occupation.CLERIC);
    assertEquals(1, pieces.size());
    assertEquals(EquipmentSlot.OFFHAND, pieces.get(0).slot());
    assertTrue(pieces.get(0).matches(potion(Items.SPLASH_POTION, Potions.REGENERATION)));
    assertFalse(pieces.get(0).matches(potion(Items.SPLASH_POTION, Potions.HEALING)));
    assertFalse(pieces.get(0).matches(potion(Items.POTION, Potions.REGENERATION)));
  }

  @Test
  void aClericsPackStartsWithASplashOfHealingAndAPotionOfRegeneration() {
    List<ItemStack> pack = SignatureGear.startingPack(Occupation.CLERIC);
    assertEquals(2, pack.size());
    assertTrue(ClericPotions.sameBrew(pack.get(0), potion(Items.SPLASH_POTION, Potions.HEALING)));
    assertTrue(ClericPotions.sameBrew(pack.get(1), potion(Items.POTION, Potions.REGENERATION)));
    assertTrue(ClericPotions.isDrinkable(pack.get(1)));
    assertTrue(SignatureGear.startingPack(Occupation.FARMER).isEmpty());
  }
}
