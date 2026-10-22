package com.quzzar.kithkyn.chat;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.function.Predicate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.item.ItemStack;

/** The inventory-first half of handing an item to a player. */
final class DirectGiftDelivery {

  record Result(int inserted, ItemStack overflow) {
  }

  record Receipt(int inserted, int placed, int returned) {
    int handedOver() { return inserted + placed; }
  }

  private DirectGiftDelivery() {
  }

  /** Preview the same compatible variant and stack limit the native handoff can remove. */
  static Map<String, Integer> offers(List<ItemStack> slots, ToIntFunction<ItemStack> giveable) {
    Map<String, ItemStack> samples = new TreeMap<>();
    for (ItemStack stack : slots) {
      if (stack.isEmpty() || giveable.applyAsInt(stack) <= 0) continue;
      String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      ItemStack sample = samples.get(id);
      if (sample == null) samples.put(id, stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize())));
      else if (ItemStack.isSameItemSameComponents(sample, stack))
        sample.grow(Math.min(stack.getCount(), sample.getMaxStackSize() - sample.getCount()));
    }
    Map<String, Integer> offers = new TreeMap<>();
    samples.forEach((id, sample) -> {
      int count = Math.min(sample.getCount(), giveable.applyAsInt(sample));
      if (count > 0) offers.put(id, count);
    });
    return java.util.Collections.unmodifiableMap(offers);
  }

  /**
   * Offers the gift to an inventory operation that mutates the offered stack,
   * then reports exactly what landed and what is still physical overflow.
   */
  static Result insert(ItemStack gift, Consumer<ItemStack> inventoryInsert) {
    int offered = gift.getCount();
    inventoryInsert.accept(gift);
    int remaining = gift.getCount();
    return new Result(offered - remaining, gift.isEmpty() ? ItemStack.EMPTY : gift.copy());
  }

  /** An unsuccessful overflow spawn returns its physical items to the giver. */
  static Receipt deliver(ItemStack gift, Consumer<ItemStack> inventoryInsert,
      Predicate<ItemStack> placeOverflow, Consumer<ItemStack> returnOverflow) {
    Result result = insert(gift, inventoryInsert);
    ItemStack overflow = result.overflow();
    if (overflow.isEmpty()) return new Receipt(result.inserted(), 0, 0);
    int count = overflow.getCount();
    if (placeOverflow.test(overflow.copy())) return new Receipt(result.inserted(), count, 0);
    returnOverflow.accept(overflow);
    return new Receipt(result.inserted(), 0, count - overflow.getCount());
  }

  static String receipt(String recipient, String itemId, int requested, Receipt receipt) {
    return "Actual gift outcome: I handed " + receipt.handedOver() + " " + itemId + " to " + recipient
        + " (" + receipt.inserted() + " entered their inventory; " + receipt.placed()
        + " were placed at their feet). I chose " + requested + "; " + receipt.returned()
        + " were returned to me after delivery could not finish. "
        + (receipt.handedOver() == 0 ? "No handoff occurred." : "This records the actual handoff, not an offer.");
  }
}
