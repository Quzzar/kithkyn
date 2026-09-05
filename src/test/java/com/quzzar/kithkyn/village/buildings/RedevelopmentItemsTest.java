package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class RedevelopmentItemsTest {
  private static final net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> OPS =
      RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(NbtOps.INSTANCE);

  @Test
  void displacedItemsRetainNamesDamageAndCountsAcrossSaveReload() {
    ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
    tool.set(DataComponents.CUSTOM_NAME, Component.literal("The old quarry pick"));
    tool.setDamageValue(73);
    var encoded = RedevelopmentItems.CODEC.encodeStart(OPS, List.of(tool, new ItemStack(Items.DIAMOND, 17))).getOrThrow();
    List<ItemStack> restored = RedevelopmentItems.CODEC.parse(OPS, encoded).getOrThrow();
    assertTrue(ItemStack.isSameItemSameComponents(tool, restored.getFirst()));
    assertEquals(73, restored.getFirst().getDamageValue());
    assertEquals(17, restored.get(1).getCount());
    assertEquals(encoded, RedevelopmentItems.CODEC.encodeStart(OPS, restored).getOrThrow());
  }

  @Test
  void oldRefundTotalsMigrateAboveTheItemCodecLimitWithoutLoss() {
    var old = MaterialAmount.CODEC.listOf().encodeStart(NbtOps.INSTANCE,
        List.of(new MaterialAmount(Items.COBBLESTONE, 200))).getOrThrow();
    List<ItemStack> restored = RedevelopmentItems.CODEC.parse(OPS, old).getOrThrow();
    assertEquals(200, restored.stream().mapToInt(ItemStack::getCount).sum());
    assertEquals(List.of(64, 64, 64, 8), restored.stream().map(ItemStack::getCount).toList());
    var migrated = RedevelopmentItems.CODEC.encodeStart(OPS, restored).getOrThrow();
    assertEquals(200, RedevelopmentItems.CODEC.parse(OPS, migrated).getOrThrow().stream().mapToInt(ItemStack::getCount).sum());
  }

  @Test
  void malformedInventoryDoesNotDecodeAsAnEmptyQueue() {
    var broken = new net.minecraft.nbt.ListTag();
    var item = new net.minecraft.nbt.CompoundTag();
    item.putString("id", "kithkyn:missing_inventory_item");
    item.putInt("count", 1);
    broken.add(item);
    assertTrue(RedevelopmentItems.CODEC.parse(OPS, broken).result().isEmpty());
  }
}
