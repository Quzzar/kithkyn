package com.quzzar.kithkyn.village.buildings;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One shared bridge from construction materials to authored producer capabilities. */
public final class MaterialProduction {

  private record Source(Item example, String capability) {
  }

  private static final List<Source> SOURCES = List.of(
      new Source(Items.OAK_LOG, "LOGS"),
      new Source(Items.OAK_PLANKS, "PLANKS"),
      new Source(Items.STONE, "CUT_STONE"),
      new Source(Items.STONE_BRICKS, "CUT_STONE"),
      new Source(Items.SANDSTONE, "CUT_STONE"),
      new Source(Items.CUT_SANDSTONE, "CUT_STONE"),
      new Source(Items.WHITE_WOOL, "WOOL"),
      new Source(Items.IRON_INGOT, "SMELTING"));

  private MaterialProduction() {
  }

  /** Capability required to replenish this construction material, or null for mined materials. */
  @Nullable
  public static String capabilityFor(Item item) {
    if (Materials.isLog(item) || Materials.isPlank(item)) {
      return "LOGS";
    }
    if (Materials.isWool(item)) {
      return "WOOL";
    }
    return SOURCES.stream().filter(source -> source.example() == item)
        .map(Source::capability).findFirst().orElse(null);
  }

  /** A representative building material unlocked by this capability, for planner prose. */
  @Nullable
  public static Item representativeFor(String capability) {
    return SOURCES.stream().filter(source -> source.capability().equals(capability))
        .map(Source::example).findFirst().orElse(null);
  }
}
