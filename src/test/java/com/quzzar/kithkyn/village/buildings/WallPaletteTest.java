package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class WallPaletteTest {
  @Test
  void aridWallsUseRegionalMasonryAndKeepOakLadderAccess() {
    var ring = WallRoute.aroundBox(0, 48, 0, 48);
    var gates = Set.of(BlockPos.asLong(24, 0, 0), BlockPos.asLong(48, 0, 24),
        BlockPos.asLong(24, 0, 48), BlockPos.asLong(0, 0, 24));
    for (VillageStyle style : Set.of(VillageStyle.DESERT, VillageStyle.BADLANDS)) {
      var wall = new WallProject(ring, gates, Collections.nCopies(ring.size(), 64), WallTier.WOOD, style);
      int ladders = 0, trapdoors = 0, posts = 0;
      for (var cell : wall.plannedBlocks()) {
        var state = cell.desiredState(wall.getTier(), style);
        assertFalse(state.is(Blocks.ACACIA_PLANKS) || state.is(Blocks.STRIPPED_ACACIA_LOG)
            || state.is(Blocks.ACACIA_FENCE) || state.is(Blocks.ACACIA_TRAPDOOR));
        if (cell.piece().name().startsWith("LADDER_")) {
          assertTrue(state.is(Blocks.LADDER));
          ladders++;
        } else if (cell.piece().name().startsWith("TRAPDOOR_")) {
          assertTrue(state.is(Blocks.OAK_TRAPDOOR));
          trapdoors++;
        } else if (cell.piece() == WallBlockPlan.Piece.BODY || cell.piece() == WallBlockPlan.Piece.POST) {
          assertTrue(state.is(style == VillageStyle.DESERT ? Blocks.SANDSTONE : Blocks.RED_SANDSTONE));
          posts++;
        }
      }
      assertTrue(ladders > 0 && trapdoors > 0 && posts > 0);
    }
    assertEquals(Items.SANDSTONE, WallTier.WOOD.material(VillageStyle.DESERT));
    assertEquals(Items.RED_SANDSTONE, WallTier.WOOD.material(VillageStyle.BADLANDS));
  }

  @Test
  void everyVillageHasOnlyOneWallStage() {
    assertArrayEquals(new WallTier[] {WallTier.WOOD}, WallTier.values());
  }
}
