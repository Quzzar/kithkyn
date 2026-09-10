package com.quzzar.kithkyn.village.buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
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
          Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS,
          Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SANDSTONE_WALL, Blocks.OAK_TRAPDOOR);
      case BADLANDS -> new WallPalette(
          Blocks.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
          Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_WALL, Blocks.OAK_TRAPDOOR);
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

  /** Chiseled trim belongs to the gate frame; wall bodies keep their smooth masonry. */
  Block frame() {
    if (this.post == Blocks.SMOOTH_SANDSTONE) return Blocks.CHISELED_SANDSTONE;
    if (this.post == Blocks.SMOOTH_RED_SANDSTONE) return Blocks.CHISELED_RED_SANDSTONE;
    return this.post;
  }

  /** Stable position variation survives reloads and repeated construction checks. */
  BlockState standingLight(BlockPos position) {
    Block candle = this.post == Blocks.SMOOTH_SANDSTONE ? Blocks.CANDLE
        : this.post == Blocks.SMOOTH_RED_SANDSTONE ? Blocks.ORANGE_CANDLE : null;
    if (candle == null) return Blocks.LANTERN.defaultBlockState();
    int count = 2 + Math.floorMod(31 * position.getX() + 17 * position.getZ()
        + position.getY(), 3);
    return candle.defaultBlockState().setValue(CandleBlock.CANDLES, count)
        .setValue(CandleBlock.LIT, true);
  }

}
