package com.quzzar.kithkyn.village.buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** One persistent construction cell emitted by a wall-segment catalog. */
public record WallBlockPlan(long position, Piece piece, WallCellRole role) {

  public enum Piece {
    BODY,
    WALKWAY,
    STEP_NORTH,
    STEP_EAST,
    STEP_SOUTH,
    STEP_WEST,
    PARAPET,
    POST,
    BEAM_NORTH_SOUTH,
    BEAM_EAST_WEST,
    SLAB,
    TRAPDOOR_NORTH,
    TRAPDOOR_EAST,
    TRAPDOOR_SOUTH,
    TRAPDOOR_WEST,
    LADDER_NORTH,
    LADDER_EAST,
    LADDER_SOUTH,
    LADDER_WEST,
    LANTERN,
    LANTERN_HANGING,
    CAMPFIRE_NORTH,
    CAMPFIRE_EAST,
    CAMPFIRE_SOUTH,
    CAMPFIRE_WEST,
    COBBLE_POST, MOSSY_POST, COBBLE_WALL, MOSSY_WALL,
    COBBLE_SLAB_BOTTOM, COBBLE_SLAB_TOP,
    TORCH, TORCH_NORTH, TORCH_EAST, TORCH_SOUTH, TORCH_WEST,
    // Append only: saved section signatures include these ordinals.
    BANNER_NORTH, BANNER_EAST, BANNER_SOUTH, BANNER_WEST,
    GATE_FRAME_POST, GATE_FRAME_BEAM;
  }

  public BlockPos pos() {
    return BlockPos.of(this.position);
  }

  /** Resolves the catalog's semantic piece through the project's current palette. */
  public BlockState desiredState(WallTier tier) {
    return desiredState(tier, VillageStyle.PLAINS);
  }

  /** Resolves the catalog's semantic piece through the village's regional palette. */
  public BlockState desiredState(WallTier tier, VillageStyle style) {
    WallPalette palette = WallPalette.forStyle(style);
    return switch (this.piece) {
      case BODY -> post(palette, Direction.Axis.Y);
      case WALKWAY -> palette.deck().defaultBlockState();
      case PARAPET -> palette.railing().defaultBlockState();
      case STEP_NORTH -> stair(palette, Direction.NORTH);
      case STEP_EAST -> stair(palette, Direction.EAST);
      case STEP_SOUTH -> stair(palette, Direction.SOUTH);
      case STEP_WEST -> stair(palette, Direction.WEST);
      case POST -> post(palette, Direction.Axis.Y);
      case GATE_FRAME_POST, GATE_FRAME_BEAM -> palette.frame().defaultBlockState();
      case BEAM_NORTH_SOUTH -> post(palette, Direction.Axis.Z);
      case BEAM_EAST_WEST -> post(palette, Direction.Axis.X);
      case SLAB -> palette.slab().defaultBlockState()
          .setValue(SlabBlock.TYPE, SlabType.TOP);
      case TRAPDOOR_NORTH -> trapdoor(palette, Direction.NORTH);
      case TRAPDOOR_EAST -> trapdoor(palette, Direction.EAST);
      case TRAPDOOR_SOUTH -> trapdoor(palette, Direction.SOUTH);
      case TRAPDOOR_WEST -> trapdoor(palette, Direction.WEST);
      case LADDER_NORTH -> ladder(Direction.NORTH);
      case LADDER_EAST -> ladder(Direction.EAST);
      case LADDER_SOUTH -> ladder(Direction.SOUTH);
      case LADDER_WEST -> ladder(Direction.WEST);
      case LANTERN -> palette.standingLight(pos());
      case LANTERN_HANGING -> Blocks.LANTERN.defaultBlockState()
          .setValue(LanternBlock.HANGING, Boolean.TRUE);
      case CAMPFIRE_NORTH -> campfire(Direction.NORTH);
      case CAMPFIRE_EAST -> campfire(Direction.EAST);
      case CAMPFIRE_SOUTH -> campfire(Direction.SOUTH);
      case CAMPFIRE_WEST -> campfire(Direction.WEST);
      case COBBLE_POST -> Blocks.COBBLESTONE.defaultBlockState();
      case MOSSY_POST -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
      case COBBLE_WALL -> Blocks.COBBLESTONE_WALL.defaultBlockState();
      case MOSSY_WALL -> Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState();
      case COBBLE_SLAB_BOTTOM -> Blocks.COBBLESTONE_SLAB.defaultBlockState();
      case COBBLE_SLAB_TOP -> Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
      case TORCH -> Blocks.TORCH.defaultBlockState();
      case TORCH_NORTH -> wallTorch(Direction.NORTH);
      case TORCH_EAST -> wallTorch(Direction.EAST);
      case TORCH_SOUTH -> wallTorch(Direction.SOUTH);
      case TORCH_WEST -> wallTorch(Direction.WEST);
      case BANNER_NORTH -> wallBanner(Direction.NORTH);
      case BANNER_EAST -> wallBanner(Direction.EAST);
      case BANNER_SOUTH -> wallBanner(Direction.SOUTH);
      case BANNER_WEST -> wallBanner(Direction.WEST);
    };
  }

  private static BlockState wallTorch(Direction direction) {
    return Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, direction);
  }

  /** A neutral authored flag; the owning village supplies its colors and layers. */
  private static BlockState wallBanner(Direction direction) {
    return Blocks.WHITE_WALL_BANNER.defaultBlockState()
        .setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, direction);
  }

  public boolean isBanner() {
    return switch (piece) {
      case BANNER_NORTH, BANNER_EAST, BANNER_SOUTH, BANNER_WEST -> true;
      default -> false;
    };
  }

  static Piece bannerPiece(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.BANNER_NORTH;
      case EAST -> Piece.BANNER_EAST;
      case SOUTH -> Piece.BANNER_SOUTH;
      case WEST -> Piece.BANNER_WEST;
      default -> throw new IllegalArgumentException("Wall banners must face horizontally");
    };
  }

  static Piece torchPiece(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.TORCH_NORTH;
      case EAST -> Piece.TORCH_EAST;
      case SOUTH -> Piece.TORCH_SOUTH;
      case WEST -> Piece.TORCH_WEST;
      default -> throw new IllegalArgumentException("Wall torches must face horizontally");
    };
  }

  private static BlockState post(WallPalette palette, Direction.Axis axis) {
    BlockState state = palette.post().defaultBlockState();
    return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
  }

  private static BlockState stair(WallPalette palette, Direction facing) {
    BlockState state = palette.stairs()
        .defaultBlockState();
    return state.setValue(StairBlock.FACING, facing);
  }

  private static BlockState trapdoor(WallPalette palette, Direction facing) {
    return palette.trapdoor().defaultBlockState()
        .setValue(TrapDoorBlock.FACING, facing)
        .setValue(TrapDoorBlock.HALF, Half.TOP)
        .setValue(TrapDoorBlock.OPEN, Boolean.TRUE)
        .setValue(TrapDoorBlock.POWERED, Boolean.FALSE);
  }

  private static BlockState ladder(Direction facing) {
    return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
  }

  private static BlockState campfire(Direction facing) {
    return Blocks.CAMPFIRE.defaultBlockState()
        .setValue(CampfireBlock.FACING, facing)
        .setValue(CampfireBlock.LIT, Boolean.TRUE);
  }

  static Piece step(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.STEP_NORTH;
      case EAST -> Piece.STEP_EAST;
      case SOUTH -> Piece.STEP_SOUTH;
      case WEST -> Piece.STEP_WEST;
      default -> Piece.WALKWAY;
    };
  }

  static Piece trapdoorPiece(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.TRAPDOOR_NORTH;
      case EAST -> Piece.TRAPDOOR_EAST;
      case SOUTH -> Piece.TRAPDOOR_SOUTH;
      case WEST -> Piece.TRAPDOOR_WEST;
      default -> throw new IllegalArgumentException("Wall trapdoors must face horizontally");
    };
  }

  static Piece ladderPiece(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.LADDER_NORTH;
      case EAST -> Piece.LADDER_EAST;
      case SOUTH -> Piece.LADDER_SOUTH;
      case WEST -> Piece.LADDER_WEST;
      default -> throw new IllegalArgumentException("Wall ladders must face horizontally");
    };
  }

  static Piece campfirePiece(Direction direction) {
    return switch (direction) {
      case NORTH -> Piece.CAMPFIRE_NORTH;
      case EAST -> Piece.CAMPFIRE_EAST;
      case SOUTH -> Piece.CAMPFIRE_SOUTH;
      case WEST -> Piece.CAMPFIRE_WEST;
      default -> throw new IllegalArgumentException("Wall campfires must face horizontally");
    };
  }
}
