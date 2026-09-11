package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

class MineSupportMaterialsTest {

  @Test
  void placesTheSameBlockTheMinerSpent() {
    assertEquals(Blocks.DIRT.defaultBlockState(),
        MineSupportMaterials.blockState(new ItemStack(Items.DIRT)));
    assertEquals(Blocks.DEEPSLATE.defaultBlockState(),
        MineSupportMaterials.blockState(new ItemStack(Items.DEEPSLATE)));
  }

  /** Item tags are not bound in a unit test, so dirt and stone are exercised natively; sand needs no tag. */
  @Test
  void sandPressesIntoSandstoneFourToTheBlock() {
    SimpleContainer pack = new SimpleContainer(9);
    pack.setItem(0, new ItemStack(Items.SAND, 9));
    assertEquals(2, MineSupportMaterials.held(pack), "two blocks' worth of sand");
    assertEquals(Items.SANDSTONE, MineSupportMaterials.takeOne(pack).getItem());
    assertEquals(5, pack.countItem(Items.SAND));
    assertEquals(Items.SANDSTONE, MineSupportMaterials.takeOne(pack).getItem());
    assertEquals(1, pack.countItem(Items.SAND));
    assertTrue(MineSupportMaterials.takeOne(pack).isEmpty(), "three sand are no block yet");
    assertEquals(0, MineSupportMaterials.held(pack));
  }

  @Test
  void redSandPressesIntoRedSandstone() {
    SimpleContainer pack = new SimpleContainer(3);
    pack.setItem(0, new ItemStack(Items.RED_SAND, 2));
    pack.setItem(2, new ItemStack(Items.RED_SAND, 2));
    ItemStack pressed = MineSupportMaterials.takeOne(pack);
    assertEquals(Items.RED_SANDSTONE, pressed.getItem());
    assertEquals(Blocks.RED_SANDSTONE.defaultBlockState(), MineSupportMaterials.blockState(pressed));
    assertTrue(pack.isEmpty(), "both part stacks were spent");
  }

  @Test
  void supportTagIncludesDirtAndTheStoneFamilies() throws Exception {
    try (var stream = getClass().getResourceAsStream(
        "/data/kithkyn/tags/item/mine_support_materials.json")) {
      assertTrue(stream != null, "mine support item tag should be packaged");
      var values = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonArray("values").toString();
      assertTrue(values.contains("#minecraft:dirt"), values);
      assertTrue(values.contains("#c:stones"), values);
      assertTrue(values.contains("#c:cobblestones"), values);
      assertTrue(values.contains("#c:sandstone/blocks"), values);
    }
  }
}
