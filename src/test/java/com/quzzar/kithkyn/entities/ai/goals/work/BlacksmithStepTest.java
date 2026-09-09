package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class BlacksmithStepTest {
  private Map<Item, Integer> stockedTools() {
    return new HashMap<>(Map.of(Items.BUCKET, 2, Items.IRON_PICKAXE, 1, Items.IRON_AXE, 1,
        Items.IRON_SHOVEL, 1, Items.IRON_HOE, 1, Items.IRON_SWORD, 1));
  }

  @Test
  void shieldRequiresIronAndWoodAndCountsFetchedMaterials() {
    Map<Item, Integer> stock = stockedTools();
    stock.put(Items.IRON_INGOT, 1);
    assertNull(BlacksmithStep.nextNeed(stock, new SimpleContainer(1)));
    assertNull(BlacksmithStep.nextNeed(stock, new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 5))));
    var need = BlacksmithStep.nextNeed(stock, new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 6)));
    assertNotNull(need);
    assertTrue(need.output().is(Items.SHIELD));
    assertEquals(2, need.cost().size());
    stock.remove(Items.IRON_INGOT);
    assertNull(BlacksmithStep.nextNeed(stock, new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 6))));
  }

  @Test
  void oneSpareShieldStopsProductionUntilSomeoneTakesIt() {
    Map<Item, Integer> stock = stockedTools();
    stock.put(Items.IRON_INGOT, 1);
    stock.put(Items.OAK_PLANKS, 6);
    stock.put(Items.SHIELD, 1);
    assertNull(BlacksmithStep.nextNeed(stock, new SimpleContainer(1)));
    stock.remove(Items.SHIELD);
    assertTrue(BlacksmithStep.nextNeed(stock, new SimpleContainer(1)).output().is(Items.SHIELD));
    assertNull(BlacksmithStep.nextNeed(stock, new SimpleContainer(new ItemStack(Items.SHIELD))));
  }
}
