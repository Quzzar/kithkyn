package com.quzzar.kithkyn.entities;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared payment facts for forge stock and a guard's existing personal craft offer. */
public final class ShieldRecipe {
  public static final int IRON = 1;
  public static final int PLANKS = 6;

  private ShieldRecipe() { }

  /** Fresh stacks so no consumer can mutate another worker's recipe. */
  public static List<ItemStack> ingredients() {
    return List.of(new ItemStack(Items.IRON_INGOT, IRON), new ItemStack(Items.OAK_PLANKS, PLANKS));
  }
}
