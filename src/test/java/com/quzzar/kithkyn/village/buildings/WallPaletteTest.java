package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import org.junit.jupiter.api.Test;

class WallPaletteTest {
  @Test
  void aridWallsUseRegionalMasonryAndKeepOakLadderAccess() {
    var ring = WallRoute.aroundBox(0, 48, 0, 48);
    var gates = Set.of(BlockPos.asLong(24, 0, 0), BlockPos.asLong(48, 0, 24),
        BlockPos.asLong(24, 0, 48), BlockPos.asLong(0, 0, 24));
    for (VillageStyle style : Set.of(VillageStyle.DESERT, VillageStyle.BADLANDS)) {
      var wall = new WallProject(ring, gates, Collections.nCopies(ring.size(), 64), WallTier.WOOD, style);
      int ladders = 0, trapdoors = 0, posts = 0, frames = 0, hanging = 0, candles = 0;
      Set<Integer> candleCounts = new java.util.HashSet<>();
      for (var cell : wall.plannedBlocks()) {
        var state = cell.desiredState(wall.getTier(), style);
        assertFalse(state.is(Blocks.ACACIA_PLANKS) || state.is(Blocks.STRIPPED_ACACIA_LOG)
            || state.is(Blocks.ACACIA_FENCE) || state.is(Blocks.ACACIA_TRAPDOOR));
        if (state.is(Blocks.LANTERN)) {
          assertTrue(state.getValue(LanternBlock.HANGING));
          hanging++;
        } else if (state.is(style == VillageStyle.DESERT ? Blocks.CANDLE : Blocks.ORANGE_CANDLE)) {
          int count = state.getValue(CandleBlock.CANDLES);
          assertTrue(count >= 2 && count <= 4);
          assertTrue(state.getValue(CandleBlock.LIT));
          assertEquals(state, cell.desiredState(wall.getTier(), style));
          candleCounts.add(count);
          candles++;
        } else if (cell.piece() == WallBlockPlan.Piece.GATE_FRAME_POST
            || cell.piece() == WallBlockPlan.Piece.GATE_FRAME_BEAM) {
          assertTrue(state.is(style == VillageStyle.DESERT
              ? Blocks.CHISELED_SANDSTONE : Blocks.CHISELED_RED_SANDSTONE));
          frames++;
        }
        if (cell.piece().name().startsWith("LADDER_")) {
          assertTrue(state.is(Blocks.LADDER));
          ladders++;
        } else if (cell.piece().name().startsWith("TRAPDOOR_")) {
          assertTrue(state.is(Blocks.OAK_TRAPDOOR));
          trapdoors++;
        } else if (cell.piece() == WallBlockPlan.Piece.BODY || cell.piece() == WallBlockPlan.Piece.POST) {
          assertTrue(state.is(style == VillageStyle.DESERT
              ? Blocks.SMOOTH_SANDSTONE : Blocks.SMOOTH_RED_SANDSTONE));
          posts++;
        }
      }
      assertTrue(ladders > 0 && trapdoors > 0 && posts > 0);
      assertEquals(160, frames, "Forty edited trim blocks rotate onto each of four gates");
      assertEquals(16, hanging, "Four hanging lanterns remain at every gate");
      assertTrue(candles > 0);
      assertEquals(Set.of(2, 3, 4), candleCounts);
    }
    assertEquals(Items.SANDSTONE, WallTier.WOOD.material(VillageStyle.DESERT));
    assertEquals(Items.RED_SANDSTONE, WallTier.WOOD.material(VillageStyle.BADLANDS));
  }

  @Test
  void floodplainWallsUseMudBrickWithMangroveRootFramesAndBrownCandles() {
    var ring = WallRoute.aroundBox(0, 48, 0, 48);
    var gates = Set.of(BlockPos.asLong(24, 0, 0), BlockPos.asLong(48, 0, 24),
        BlockPos.asLong(24, 0, 48), BlockPos.asLong(0, 0, 24));
    var style = VillageStyle.FLOODPLAIN;
    var wall = new WallProject(ring, gates, Collections.nCopies(ring.size(), 64), WallTier.WOOD, style);
    int ladders = 0, trapdoors = 0, masonry = 0, frames = 0, hanging = 0, candles = 0, railings = 0;
    Set<Integer> candleCounts = new java.util.HashSet<>();
    for (var cell : wall.plannedBlocks()) {
      var state = cell.desiredState(wall.getTier(), style);
      assertFalse(state.is(Blocks.OAK_PLANKS) || state.is(Blocks.STRIPPED_OAK_LOG)
          || state.is(Blocks.OAK_FENCE) || state.is(Blocks.OAK_TRAPDOOR) || state.is(Blocks.MANGROVE_FENCE));
      if (state.is(Blocks.LANTERN)) {
        assertTrue(state.getValue(LanternBlock.HANGING));
        hanging++;
      } else if (state.is(Blocks.BROWN_CANDLE)) {
        int count = state.getValue(CandleBlock.CANDLES);
        assertTrue(count >= 2 && count <= 4);
        assertTrue(state.getValue(CandleBlock.LIT));
        candleCounts.add(count);
        candles++;
      } else if (cell.piece() == WallBlockPlan.Piece.GATE_FRAME_POST
          || cell.piece() == WallBlockPlan.Piece.GATE_FRAME_BEAM) {
        assertTrue(state.is(Blocks.MUDDY_MANGROVE_ROOTS));
        frames++;
      }
      if (cell.piece().name().startsWith("LADDER_")) {
        assertTrue(state.is(Blocks.LADDER));
        ladders++;
      } else if (cell.piece().name().startsWith("TRAPDOOR_")) {
        assertTrue(state.is(Blocks.JUNGLE_TRAPDOOR));
        trapdoors++;
      } else if (cell.piece() == WallBlockPlan.Piece.BODY || cell.piece() == WallBlockPlan.Piece.POST) {
        assertTrue(state.is(Blocks.MUD_BRICKS));
        masonry++;
      } else if (state.is(Blocks.MUD_BRICK_WALL)) {
        railings++;
      }
    }
    assertTrue(ladders > 0 && trapdoors > 0 && masonry > 0 && railings > 0);
    assertEquals(160, frames, "The roots trim takes the same forty gate cells as the arid chiseled stone");
    assertEquals(16, hanging, "Four hanging lanterns remain at every gate");
    assertTrue(candles > 0);
    assertEquals(Set.of(2, 3, 4), candleCounts);
    assertEquals(Items.MUD_BRICKS, WallTier.WOOD.material(style));
  }

  @Test
  void everyVillageHasOnlyOneWallStage() {
    assertArrayEquals(new WallTier[] {WallTier.WOOD}, WallTier.values());
  }
}
