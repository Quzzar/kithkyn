package com.quzzar.kithkyn.village.buildings;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One regional wall stage; completed walls have no upgrade tier. */
public enum WallTier {

  WOOD;

  /**
   * Wall blocks one item of the material raises. A wall is village-scale work,
   * hundreds of segments round even a modest village, and at a block per log no
   * village ever afforded one: Wildflower Downs wanted 3700 logs and waited
   * forever. Aaron priced walls at a tenth instead (2026-09-02). The bill the
   * village checks and the draw the builder makes both use this rate.
   */
  public static final int BLOCKS_PER_ITEM = 10;

  /** Walls use local stone through the shared material-substitution rules. */
  public Item material(VillageStyle style) {
    return switch (style) {
      case DESERT -> Items.SANDSTONE;
      case BADLANDS -> Items.RED_SANDSTONE;
      case BIRCH_FOREST -> Items.COBBLESTONE;
    };
  }

  /** Solid courses above the ground; authored gates and towers add their own detail. */
  public int height() {
    return 3;
  }
}
