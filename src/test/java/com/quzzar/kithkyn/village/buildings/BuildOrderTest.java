package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

/** The block-by-block build order: dry blocks, then every liquid, then the plants that need the water. */
class BuildOrderTest {
  @Test
  void waterPlantsWaitForTheirWaterWhileEverythingElseKeepsItsPlace() {
    var stone = info(0, Blocks.STONE.defaultBlockState());
    var lily = info(1, Blocks.LILY_PAD.defaultBlockState());
    var water = info(2, Blocks.WATER.defaultBlockState());
    var cane = info(3, Blocks.SUGAR_CANE.defaultBlockState());
    var fence = info(4, Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.WATERLOGGED, true));
    var planks = info(5, Blocks.OAK_PLANKS.defaultBlockState());
    assertEquals(List.of(stone, planks, water, fence, lily, cane),
        StructureInProgress.liquidsLast(List.of(stone, lily, water, cane, fence, planks)));
  }

  @Test
  void aTemplateWithoutWaterPlantsKeepsItsSavedOrder() {
    var stone = info(0, Blocks.STONE.defaultBlockState());
    var water = info(1, Blocks.WATER.defaultBlockState());
    var planks = info(2, Blocks.OAK_PLANKS.defaultBlockState());
    assertEquals(List.of(stone, planks, water), StructureInProgress.liquidsLast(List.of(stone, water, planks)));
  }

  @Test
  void onlyPlantsThatStandOnOrBesideWaterWait() {
    assertTrue(StructureInProgress.needsWater(Blocks.LILY_PAD.defaultBlockState()));
    assertTrue(StructureInProgress.needsWater(Blocks.SUGAR_CANE.defaultBlockState()));
    assertTrue(StructureInProgress.needsWater(Blocks.FROGSPAWN.defaultBlockState()));
    assertFalse(StructureInProgress.needsWater(Blocks.WHEAT.defaultBlockState()));
    assertFalse(StructureInProgress.needsWater(Blocks.JUNGLE_SAPLING.defaultBlockState()));
  }

  private static StructureTemplate.StructureBlockInfo info(int x, BlockState state) {
    return new StructureTemplate.StructureBlockInfo(new BlockPos(x, 0, 0), state, null);
  }
}
