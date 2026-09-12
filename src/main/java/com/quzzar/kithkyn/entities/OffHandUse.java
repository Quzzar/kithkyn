package com.quzzar.kithkyn.entities;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The off hand is the cleric's one working slot (Aaron, 2026-09-12). A bottle
 * to throw or drink is brought into it for the use, and whatever the hand held
 * goes down into the bottle's pack slot until the use is over, then comes back.
 * Potions do not stack, so a finished throw or drink leaves the hand empty and
 * the swap back always has room. Bottles are MOVED, never copied, like any held
 * item: a copy would duplicate on death.
 *
 * <p>One use holds a villager's off hand at a time. A throw being aimed and a
 * potion being drunk would otherwise each put the other's bottle away, so a
 * second use waits until the first has restored the hand ({@link #inUse}).
 */
public final class OffHandUse {

  private static final Map<RealPerson, OffHandUse> HOLDING = new WeakHashMap<>();

  /** The pack slot now holding what the hand held, or -1 when nothing was moved. */
  private int slot = -1;
  private ItemStack displaced = ItemStack.EMPTY;

  /** Whether some use is holding this villager's off hand. */
  public static boolean inUse(RealPerson person) {
    return HOLDING.containsKey(person);
  }

  /**
   * Bring one of the villager's own stacks into the off hand, found by
   * identity. True when it is there. False, and nothing moved, when another use
   * holds the hand, the villager is eating or using an item, or no longer
   * carries the stack. A bottle in the main hand, left there by the old
   * two-handed kit, moves across and the main hand is left empty.
   */
  public boolean ready(RealPerson person, ItemStack stack) {
    restore(person);
    OffHandUse holder = HOLDING.get(person);
    if (stack.isEmpty() || (holder != null && holder != this) || person.isEating() || person.isUsingItem()) {
      return false;
    }
    if (person.getOffhandItem() == stack) {
      HOLDING.put(person, this);
      return true;
    }
    if (person.getMainHandItem() == stack) {
      int empty = emptySlot(person);
      if (empty < 0) {
        return false;
      }
      this.displaced = person.getOffhandItem();
      person.personMainInv.setItem(empty, this.displaced);
      person.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      person.setItemSlot(EquipmentSlot.OFFHAND, stack);
      this.slot = empty;
      HOLDING.put(person, this);
      return true;
    }
    for (int index = 0; index < person.personMainInv.getContainerSize(); index++) {
      if (person.personMainInv.getItem(index) == stack) {
        this.displaced = person.getOffhandItem();
        person.setItemSlot(EquipmentSlot.OFFHAND,
            EquipmentSwap.exchange(person.personMainInv, index, this.displaced));
        this.slot = index;
        HOLDING.put(person, this);
        return true;
      }
    }
    return false;
  }

  /**
   * End the use: what the hand held comes back from its pack slot, and the
   * bottle, if anything is left of it, takes that slot. Left alone when the
   * slot was emptied or refilled meanwhile; the daily tending of the hand
   * ({@link RealPerson#tendSignatureGear}) settles it then.
   */
  public void restore(RealPerson person) {
    if (HOLDING.get(person) == this) {
      HOLDING.remove(person);
    }
    if (this.slot < 0) {
      return;
    }
    ItemStack inSlot = person.personMainInv.getItem(this.slot);
    if (inSlot == this.displaced || (this.displaced.isEmpty() && inSlot.isEmpty())) {
      person.setItemSlot(EquipmentSlot.OFFHAND,
          EquipmentSwap.exchange(person.personMainInv, this.slot, person.getOffhandItem()));
    }
    this.slot = -1;
    this.displaced = ItemStack.EMPTY;
  }

  private static int emptySlot(RealPerson person) {
    for (int index = 0; index < person.personMainInv.getContainerSize(); index++) {
      if (person.personMainInv.getItem(index).isEmpty()) {
        return index;
      }
    }
    return -1;
  }
}
