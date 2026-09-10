package com.quzzar.kithkyn.village.buildings;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Regional materials applied to the canonical authored wall geometry. */
record WallPalette(Block post, Block deck, Block stairs, Block slab,
    Block railing, Block trapdoor) {

  static WallPalette forStyle(VillageStyle style) {
    return switch (style) {
      case BIRCH_FOREST -> new WallPalette(
          Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS,
          Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE_WALL, Blocks.OAK_TRAPDOOR);
      case DESERT -> new WallPalette(
          Blocks.SANDSTONE, Blocks.SANDSTONE, Blocks.SANDSTONE_STAIRS,
          Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE_WALL, Blocks.OAK_TRAPDOOR);
      case BADLANDS -> new WallPalette(
          Blocks.RED_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.RED_SANDSTONE_STAIRS,
          Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_WALL, Blocks.OAK_TRAPDOOR);
      case TAIGA, SNOWY -> new WallPalette(
          Blocks.STRIPPED_SPRUCE_LOG,
          Blocks.SPRUCE_PLANKS,
          Blocks.SPRUCE_STAIRS,
          Blocks.SPRUCE_SLAB,
          Blocks.SPRUCE_FENCE,
          Blocks.SPRUCE_TRAPDOOR);
      case SAVANNA -> new WallPalette(
          Blocks.STRIPPED_ACACIA_LOG,
          Blocks.ACACIA_PLANKS,
          Blocks.ACACIA_STAIRS,
          Blocks.ACACIA_SLAB,
          Blocks.ACACIA_FENCE,
          Blocks.ACACIA_TRAPDOOR);
      case PLAINS -> new WallPalette(
          Blocks.STRIPPED_OAK_LOG,
          Blocks.OAK_PLANKS,
          Blocks.OAK_STAIRS,
          Blocks.OAK_SLAB,
          Blocks.OAK_FENCE,
          Blocks.OAK_TRAPDOOR);
    };
  }

}
