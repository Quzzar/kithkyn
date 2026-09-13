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
      case SWAMP -> new WallPalette(
          Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_STAIRS,
          Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
      case MEDITERRANEAN -> new WallPalette(
          Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_STAIRS,
          Blocks.QUARTZ_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
      case TUNDRA -> new WallPalette(
          Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.STONE_BRICK_STAIRS,
          Blocks.STONE_BRICK_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
      // Study A (2026-09-12): stripped spruce body, oak slab walks, spruce fence
      // tips and hatches. Its dead coral footing is a literal piece, not a slot.
      case POLYNESIAN_COAST -> new WallPalette(
          Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.OAK_STAIRS,
          Blocks.OAK_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
      case ROMANIAN -> new WallPalette(
          Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.BIRCH_STAIRS,
          Blocks.BIRCH_SLAB, Blocks.COBBLED_DEEPSLATE_WALL, Blocks.DARK_OAK_TRAPDOOR);
      case ALPINE_HIGHLANDS -> new WallPalette(
          Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICK_STAIRS,
          Blocks.BRICK_SLAB, Blocks.BRICK_WALL, Blocks.SPRUCE_TRAPDOOR);
      case JAPANESE_CHERRY_GROVE -> new WallPalette(
          Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_STAIRS,
          Blocks.DEEPSLATE_TILE_SLAB, Blocks.SPRUCE_FENCE, Blocks.CHERRY_TRAPDOOR);
    };
  }

  /**
   * The Mediterranean wall wears a low hedge along both faces (Aaron's
   * 2026-09-12 workshop edit): two leaf blocks mixed by position. Other
   * families author no foliage, so their pair is never placed.
   */
  Block leaves() {
    if (this.post == Blocks.STRIPPED_SPRUCE_LOG) return Blocks.CHERRY_LEAVES;
    if (this.post == Blocks.QUARTZ_BRICKS) return Blocks.JUNGLE_LEAVES;
    if (this.post == Blocks.BRICKS) return Blocks.MANGROVE_LEAVES;
    return Blocks.OAK_LEAVES;
  }

  Block leavesDark() {
    if (this.post == Blocks.STRIPPED_SPRUCE_LOG) return Blocks.FLOWERING_AZALEA_LEAVES;
    return Blocks.DARK_OAK_LEAVES;
  }

  /** Whether the route grows the procedural hedge beside its linear runs. */
  boolean hedged() {
    return this.post == Blocks.QUARTZ_BRICKS;
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
    if (this.post == Blocks.SNOW_BLOCK) return Blocks.PACKED_ICE;
    if (this.post == Blocks.STRIPPED_DARK_OAK_WOOD) return Blocks.COBBLED_DEEPSLATE;
    return this.post;
  }

  /** Stable position variation survives reloads and repeated construction checks. */
  BlockState standingLight(BlockPos position) {
    Block candle = this.post == Blocks.SMOOTH_SANDSTONE ? Blocks.CANDLE
        : this.post == Blocks.SMOOTH_RED_SANDSTONE ? Blocks.ORANGE_CANDLE
        : this.post == Blocks.MUD_BRICKS ? Blocks.BROWN_CANDLE
        : this.post == Blocks.OAK_LOG ? Blocks.CANDLE
        : this.post == Blocks.SNOW_BLOCK ? Blocks.BROWN_CANDLE : null;
    if (this.post == Blocks.JUNGLE_PLANKS || this.post == Blocks.STRIPPED_DARK_OAK_WOOD) {
      return Blocks.TORCH.defaultBlockState();
    }
    if (candle == null) return Blocks.LANTERN.defaultBlockState();
    if (this.post == Blocks.OAK_LOG || this.post == Blocks.SNOW_BLOCK) {
      return candle.defaultBlockState().setValue(CandleBlock.CANDLES, 1)
          .setValue(CandleBlock.LIT, true);
    }
    int count = 2 + Math.floorMod(31 * position.getX() + 17 * position.getZ()
        + position.getY(), 3);
    return candle.defaultBlockState().setValue(CandleBlock.CANDLES, count)
        .setValue(CandleBlock.LIT, true);
  }

}
