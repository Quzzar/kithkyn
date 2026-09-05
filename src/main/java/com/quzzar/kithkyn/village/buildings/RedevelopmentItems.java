package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.world.item.ItemStack;

/** Full-fidelity overflow items, also accepting the material-only refund format from older saves. */
public final class RedevelopmentItems {
  public static final Codec<List<ItemStack>> CODEC = Codec.either(ItemStack.CODEC, MaterialAmount.CODEC)
      .listOf().xmap(entries -> normalize(entries.stream().map(entry -> entry.map(stack -> stack,
          amount -> new ItemStack(amount.item(), amount.count()))).toList()),
          stacks -> normalize(stacks).stream().map(stack -> Either.<ItemStack, MaterialAmount>left(stack)).toList());

  private RedevelopmentItems() {
  }

  /** Salvage totals can exceed a physical stack or the item codec's count limit of 99. */
  public static List<ItemStack> normalize(List<ItemStack> stacks) {
    List<ItemStack> result = new ArrayList<>();
    for (ItemStack stack : stacks) {
      int remaining = stack.getCount();
      if (stack.isEmpty()) {
        continue;
      }
      int limit = Math.min(99, stack.getMaxStackSize());
      while (remaining > 0) {
        int count = Math.min(limit, remaining);
        result.add(stack.copyWithCount(count));
        remaining -= count;
      }
    }
    return result;
  }
}
