package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

class CampfireRoastTest {

  @Test
  void potatoesAreDiscoveredAsCampfireFood() {
    Set<Item> raws = new LinkedHashSet<>();
    Set<Item> results = new LinkedHashSet<>();

    CampfireRoast.addFoodRecipe(Ingredient.of(Items.POTATO), new ItemStack(Items.BAKED_POTATO),
        raws, results);

    assertEquals(Set.of(Items.POTATO), raws);
    assertEquals(Set.of(Items.BAKED_POTATO), results);
  }

  @Test
  void campfireRecipesThatDoNotPrepareFoodAreIgnored() {
    Set<Item> raws = new LinkedHashSet<>();
    Set<Item> results = new LinkedHashSet<>();

    CampfireRoast.addFoodRecipe(Ingredient.of(Items.IRON_ORE), new ItemStack(Items.IRON_INGOT),
        raws, results);

    assertFalse(raws.contains(Items.IRON_ORE));
    assertFalse(results.contains(Items.IRON_INGOT));
  }
}
