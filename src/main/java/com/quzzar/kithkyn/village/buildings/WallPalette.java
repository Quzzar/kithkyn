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
      case FLOODPLAIN -> new WallPalette(
          Blocks.MUD_BRICKS, Blocks.MUD_BRICKS, Blocks.MUD_BRICK_STAIRS,
          Blocks.MUD_BRICK_SLAB, Blocks.MUD_BRICK_WALL, Blocks.JUNGLE_TRAPDOOR);
      case JUNGLE -> new WallPalette(
          Blocks.JUNGLE_PLANKS, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_MOSAIC_STAIRS,
          Blocks.BAMBOO_MOSAIC_SLAB, Blocks.JUNGLE_FENCE, Blocks.JUNGLE_TRAPDOOR);
    };
  }

  /**
   * Trim belongs to the gate frame and wall bodies keep their plain masonry:
   * chiseled stone on the arid walls, muddy mangrove roots on the floodplain.
   */
  Block frame() {
    if (this.post == Blocks.SMOOTH_SANDSTONE) return Blocks.CHISELED_SANDSTONE;
    if (this.post == Blocks.SMOOTH_RED_SANDSTONE) return Blocks.CHISELED_RED_SANDSTONE;
    if (this.post == Blocks.MUD_BRICKS) return Blocks.MUDDY_MANGROVE_ROOTS;
    if (this.post == Blocks.JUNGLE_PLANKS) return Blocks.STRIPPED_JUNGLE_WOOD;
    return this.post;
  }

  /** Stable position variation survives reloads and repeated construction checks. */
  BlockState standingLight(BlockPos position) {
    Block candle = this.post == Blocks.SMOOTH_SANDSTONE ? Blocks.CANDLE
        : this.post == Blocks.SMOOTH_RED_SANDSTONE ? Blocks.ORANGE_CANDLE
        : this.post == Blocks.MUD_BRICKS ? Blocks.BROWN_CANDLE : null;
    if (this.post == Blocks.JUNGLE_PLANKS) return Blocks.TORCH.defaultBlockState();
    if (candle == null) return Blocks.LANTERN.defaultBlockState();
    int count = 2 + Math.floorMod(31 * position.getX() + 17 * position.getZ()
        + position.getY(), 3);
    return candle.defaultBlockState().setValue(CandleBlock.CANDLES, count)
        .setValue(CandleBlock.LIT, true);
  }

}
