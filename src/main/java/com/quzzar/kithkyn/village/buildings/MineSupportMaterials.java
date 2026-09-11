package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Items;
import java.util.Set;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dirt and stone blocks a miner can spend on floors, walls, and ceilings, and the
 * sand that presses into them. A desert shaft passes through nothing but sand for
 * its first columns, and sand falls, so it is no lining; four sand pack into one
 * sandstone block and red sand into red sandstone, and the miner packs them by
 * hand when the lining runs out, the same table-less press as the torch and bone
 * meal crafts. Sand therefore counts as support four to the block, real dirt or
 * stone is spent first, and the pack keeps its sand until a seal actually needs it.
 */
public final class MineSupportMaterials {

  /** Vanilla's press: four sand to a sandstone block. */
  public static final int SAND_PER_BLOCK = 4;

  private static final Map<Item, Item> PRESSES = Map.of(
      Items.SAND, Items.SANDSTONE,
      Items.RED_SAND, Items.RED_SANDSTONE);

  public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM,
      ResourceLocation.fromNamespaceAndPath("kithkyn", "mine_support_materials"));

  private MineSupportMaterials() {
  }

  /** Only a placeable item in the mine-support family can become lining. */
  public static boolean isSupport(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() instanceof BlockItem && stack.is(TAG);
  }

  /** Support blocks in hand, counting sand at four to the block. */
  public static int held(Container container) {
    int total = 0;
    for (int slot = 0; slot < container.getContainerSize(); slot++) {
      ItemStack stack = container.getItem(slot);
      if (isSupport(stack)) {
        total += stack.getCount();
      }
    }
    for (Item sand : PRESSES.keySet()) {
      total += container.countItem(sand) / SAND_PER_BLOCK;
    }
    return total;
  }

  /**
   * Takes mixed support blocks, preserving each actual block the miner carries,
   * then presses sand into sandstone for whatever is still short.
   */
  public static List<ItemStack> take(Container container, int amount) {
    List<ItemStack> taken = new ArrayList<>();
    int left = amount;
    for (int slot = container.getContainerSize() - 1; slot >= 0 && left > 0; slot--) {
      ItemStack stack = container.getItem(slot);
      if (!isSupport(stack)) {
        continue;
      }
      ItemStack piece = stack.split(left);
      left -= piece.getCount();
      taken.add(piece);
    }
    while (left > 0) {
      ItemStack pressed = pressOne(container);
      if (pressed.isEmpty()) {
        break;
      }
      taken.add(pressed);
      left--;
    }
    if (!taken.isEmpty()) {
      container.setChanged();
    }
    return taken;
  }

  /** One block pressed from four sand of one kind, or empty when no kind has four. */
  private static ItemStack pressOne(Container container) {
    for (Map.Entry<Item, Item> press : PRESSES.entrySet()) {
      if (container.countItem(press.getKey()) < SAND_PER_BLOCK) {
        continue;
      }
      int toSpend = SAND_PER_BLOCK;
      for (int slot = 0; slot < container.getContainerSize() && toSpend > 0; slot++) {
        ItemStack stack = container.getItem(slot);
        if (stack.is(press.getKey())) {
          toSpend -= stack.split(toSpend).getCount();
        }
      }
      return new ItemStack(press.getValue());
    }
    return ItemStack.EMPTY;
  }

  /** The sand kinds a miner presses into support, for restocking a short pack. */
  public static Set<Item> pressableSand() {
    return PRESSES.keySet();
  }

  public static ItemStack takeOne(Container container) {
    List<ItemStack> taken = take(container, 1);
    return taken.isEmpty() ? ItemStack.EMPTY : taken.getFirst();
  }

  /** The exact block state represented by a consumed support item. */
  public static BlockState blockState(ItemStack support) {
    return support.getItem() instanceof BlockItem blockItem
        ? blockItem.getBlock().defaultBlockState()
        : null;
  }
}
