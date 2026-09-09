package com.quzzar.kithkyn.entities;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Exchanges real equipment stacks without needing spare pack space or dropping displaced items. */
public final class EquipmentSwap {
  private EquipmentSwap() { }

  /** The selected stack leaves its exact slot available for the previous hand, even in a full pack. */
  public static ItemStack exchange(Container pack, int slot, ItemStack hand) {
    ItemStack next = pack.getItem(slot);
    pack.setItem(slot, hand);
    return next;
  }
}
