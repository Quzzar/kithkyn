package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import org.junit.jupiter.api.Test;

class BirchWallTest {
  @Test
  void gatehousesPreserveTheFourAuthoredFlagsInEveryDirectionAndOnSlopes() {
    for (boolean sloped : List.of(false, true)) {
      List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
      List<Integer> ground = new ArrayList<>();
      for (int i = 0; i < ring.size(); i++) ground.add(64 + (sloped ? (i / 10) % 3 : 0));
      Set<Long> gates = WallPreview.cardinalGates(ring, 0, 64, 0, 64);
      WallProject project = new WallProject(ring, gates, ground, WallTier.WOOD, VillageStyle.BIRCH_FOREST);
      List<WallBlockPlan> cells = project.plannedBlocks();
      assertEquals(16, cells.stream().filter(WallBlockPlan::isBanner).count());
      Map<Long, Integer> order = new HashMap<>();
      for (int i = 0; i < cells.size(); i++) order.put(cells.get(i).position(), i);
      for (long gate : gates) {
        BlockPos anchor = BlockPos.of(gate);
        int index = ring.indexOf(gate);
        int base = project.getDeck().get(index) - (WallTier.WOOD.height() - 1);
        BlockPos next = BlockPos.of(ring.get((index + 1) % ring.size()));
        int dx = Integer.signum(next.getX() - anchor.getX());
        int dz = Integer.signum(next.getZ() - anchor.getZ());
        for (int along : List.of(-2, 2)) for (int across : List.of(-3, 3)) {
          BlockPos expected = anchor.offset(along * dx - across * dz, base + 4, along * dz + across * dx);
          WallBlockPlan banner = cells.stream().filter(cell -> cell.pos().equals(expected)).findFirst().orElseThrow();
          assertTrue(banner.isBanner(), "Missing authored banner " + expected);
          Direction facing = Direction.fromDelta(-Integer.signum(across) * dz, 0, Integer.signum(across) * dx);
          var state = banner.desiredState(WallTier.WOOD, VillageStyle.BIRCH_FOREST);
          assertTrue(state.is(Blocks.WHITE_WALL_BANNER));
          assertEquals(facing, state.getValue(WallBannerBlock.FACING));
          BlockPos support = expected.relative(facing.getOpposite());
          assertTrue(order.containsKey(support.asLong()), "Missing support " + support);
          assertTrue(order.get(support.asLong()) < order.get(expected.asLong()), "Flag built before support");
        }
      }
      var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, project).getOrThrow();
      assertEquals(cells, WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().plannedBlocks());
    }
  }

  @Test
  void approvedStoneArtRetainsClimbingAndSupportedTorchesAcrossTerrain() {
    for (boolean sloped : List.of(false,true)) {
      List<Long> ring=WallRoute.aroundBox(0,64,0,64);
      List<Integer> ground=new ArrayList<>();
      for (int i=0;i<ring.size();i++) ground.add(64+(sloped ? (i/10)%3 : 0));
      Set<Long> gates=WallPreview.cardinalGates(ring,0,64,0,64);
      WallProject project=new WallProject(ring,gates,ground,WallTier.WOOD,VillageStyle.BIRCH_FOREST);
      var saved=WallProject.CODEC.encodeStart(JsonOps.INSTANCE,project).getOrThrow();
      assertEquals(VillageStyle.BIRCH_FOREST,WallProject.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow().getStyle());
      List<WallBlockPlan> cells=WallSegmentCatalog.forStyle(VillageStyle.BIRCH_FOREST)
          .compile(ring,gates,ground,WallTerraces.deckProfile(ground,3),WallTier.WOOD).stream().flatMap(s->s.blocks().stream()).toList();
      Set<Long> positions=new HashSet<>();
      cells.forEach(c->assertTrue(positions.add(c.position()),"Overlapping wall work"));
      assertTrue(cells.stream().anyMatch(c->c.piece().name().startsWith("LADDER_")));
      assertTrue(cells.stream().anyMatch(c->c.piece()==WallBlockPlan.Piece.MOSSY_POST));
      for (WallBlockPlan cell:cells) {
        var state=cell.desiredState(WallTier.WOOD,VillageStyle.BIRCH_FOREST);
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        assertFalse(id.contains("birch") || id.endsWith("_fence") || id.endsWith("_log") || id.equals("lantern"), id);
        if (state.is(Blocks.TORCH)) assertTrue(positions.contains(cell.pos().below().asLong()),"Torch unsupported "+cell.pos());
        if (state.is(Blocks.WALL_TORCH)) assertTrue(positions.contains(cell.pos().relative(state.getValue(WallTorchBlock.FACING).getOpposite()).asLong()),"Wall torch unsupported "+cell.pos());
      }
    }
  }
}
