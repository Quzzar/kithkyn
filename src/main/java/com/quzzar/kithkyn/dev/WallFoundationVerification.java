package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.buildings.*;
import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native foundation/ownership regression, only for a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class WallFoundationVerification {
  private static boolean ran;
  private WallFoundationVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.wallFoundation.verify") || ran) return;
    ran = true;
    try {
      verify(event.getServer().overworld());
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[wall-foundation-verify] RESULT FAIL", failure);
    }
    event.getServer().halt(false);
  }

  public static void verify(ServerLevel level) {
    for (int mode = 0; mode < 8; mode++) {
      int x = 200 + mode * 48;
      var ring = WallRoute.aroundBox(x, x + 32, 200, 232);
      WallProject wall = new WallProject(ring, Set.of(), Collections.nCopies(ring.size(), 151),
          WallTier.WOOD, VillageStyle.BIRCH_FOREST);
      var prototype = wall.plannedBlocks().stream().filter(cell ->
          cell.piece() == WallBlockPlan.Piece.BODY || cell.piece() == WallBlockPlan.Piece.COBBLE_POST
              || cell.piece() == WallBlockPlan.Piece.MOSSY_POST)
          .min(java.util.Comparator.comparingInt(cell -> cell.pos().getY())).orElseThrow();
      BlockPos soil = prototype.pos().below();
      level.getChunkAt(soil);
      var owned = PlacedBlockStore.get(level);
      for (BlockPos pos : BlockPos.betweenClosed(soil.offset(-1, -2, -1), soil.offset(1, 3, 1))) {
        owned.clearPlaced(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }
      level.setBlock(soil.below(), Blocks.DIRT.defaultBlockState(), 2);
      level.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
      if (mode == 1 || mode == 7) for (Direction direction : Direction.Plane.HORIZONTAL)
        level.setBlock(soil.relative(direction), Blocks.DIRT.defaultBlockState(), 2);
      if (mode == 2) owned.markPlayerPlaced(soil);
      if (mode == 3) owned.markVillagePlaced(soil);
      var piece = switch (mode) {
        case 4 -> WallBlockPlan.Piece.MOSSY_POST;
        case 5 -> WallBlockPlan.Piece.BODY;
        case 6 -> WallBlockPlan.Piece.POST;
        default -> WallBlockPlan.Piece.COBBLE_POST;
      };
      var cell = new WallBlockPlan(prototype.position(), piece, WallCellRole.EXACT);
      WallRaiser.place(level, cell, WallTier.WOOD, VillageStyle.BIRCH_FOREST);
      boolean preserved = mode == 1 || mode == 2 || mode == 3 || mode == 7;
      check(level.getBlockState(soil).is(preserved ? Blocks.GRASS_BLOCK
          : cell.desiredState(WallTier.WOOD, VillageStyle.BIRCH_FOREST).getBlock()), "Placement case " + mode);
      if (mode == 7) level.setBlock(soil.east(), Blocks.AIR.defaultBlockState(), 2);
      WallRaiser.settleFoundations(level, wall);
      WallRaiser.settleFoundations(level, wall);
      check(level.getBlockState(soil).is(mode >= 1 && mode <= 3 ? Blocks.GRASS_BLOCK
          : cell.desiredState(WallTier.WOOD, VillageStyle.BIRCH_FOREST).getBlock()), "Maintenance case " + mode);
      check(level.getBlockState(soil.below()).is(Blocks.DIRT), "Repeated cleanup dug deeper in case " + mode);
    }
    Kithkyn.LOGGER.info("[wall-foundation-verify] RESULT PASS: eight native placement, late-exposure, ownership and idempotence cases");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
