package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;

/** The captured Mediterranean edits: face torches, coping stairs and the hedge. */
class MediterraneanWallTest {

  private static final int SPAN = 64;

  private static WallProject project(boolean sloped) {
    List<Long> ring = WallRoute.aroundBox(0, SPAN, 0, SPAN);
    List<Integer> ground = new ArrayList<>();
    for (int i = 0; i < ring.size(); i++) ground.add(64 + (sloped ? (i / 10) % 3 : 0));
    Set<Long> gates = WallPreview.cardinalGates(ring, 0, SPAN, 0, SPAN);
    return new WallProject(ring, gates, ground, WallTier.WOOD, VillageStyle.MEDITERRANEAN);
  }

  private static long column(BlockPos pos) {
    return BlockPos.asLong(pos.getX(), 0, pos.getZ());
  }

  private static Set<Long> routeColumns(WallProject project) {
    return project.getRing().stream().map(BlockPos::of).map(MediterraneanWallTest::column)
        .collect(Collectors.toSet());
  }

  private static String describe(WallProject project, long column) {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < project.sectionCount(); i++) {
      for (WallBlockPlan cell : project.section(i).blocks()) {
        if (column(cell.pos()) == column) {
          text.append(project.section(i).kind()).append('#').append(i).append(' ').append(cell.piece())
              .append('@').append(cell.pos().getY()).append(' ').append(cell.role()).append("; ");
        }
      }
    }
    int index = 0;
    for (long ringColumn : project.getRing()) {
      BlockPos r = BlockPos.of(ringColumn);
      if (Math.abs(r.getX() - BlockPos.getX(column)) <= 1 && Math.abs(r.getZ() - BlockPos.getZ(column)) <= 1) {
        text.append("route ").append(r.getX()).append(',').append(r.getZ()).append(" ground ")
            .append(project.getGround().get(index)).append(" deck ").append(project.getDeck().get(index)).append("; ");
      }
      index++;
    }
    return text.toString();
  }

  private static boolean outsideTheRing(BlockPos pos) {
    return pos.getX() < 0 || pos.getX() > SPAN || pos.getZ() < 0 || pos.getZ() > SPAN;
  }

  @Test
  void faceTorchesHangBesideTheRunOnBothFacesBehindAPlannedWallBlock() {
    WallProject project = project(false);
    Set<Long> route = routeColumns(project);
    Set<Long> planned = project.plannedBlocks().stream().map(WallBlockPlan::position)
        .collect(Collectors.toSet());
    int outward = 0;
    int inward = 0;
    for (WallBlockPlan cell : project.plannedBlocks()) {
      if (!cell.piece().name().startsWith("TORCH_")) continue;
      BlockPos pos = cell.pos();
      assertFalse(route.contains(column(pos)), "a face torch sits on the wall line at " + pos);
      Direction facing = cell.desiredState(WallTier.WOOD, VillageStyle.MEDITERRANEAN)
          .getValue(WallTorchBlock.FACING);
      BlockPos support = pos.relative(facing.getOpposite());
      assertTrue(planned.contains(support.asLong()), "no planned wall block behind the torch at " + pos);
      if (outsideTheRing(pos)) outward++; else inward++;
    }
    assertTrue(outward > 0 && inward > 0, "torches light both faces of the wall");
  }

  @Test
  void copingStairsCrossTheWallLineAndTurnWithIt() {
    WallProject project = project(false);
    int checked = 0;
    for (WallBlockPlan cell : project.plannedBlocks()) {
      if (!cell.piece().name().startsWith("STEP_")) continue;
      BlockPos pos = cell.pos();
      boolean nearCorner = Math.min(pos.getX(), SPAN - pos.getX()) < 10
          && Math.min(pos.getZ(), SPAN - pos.getZ()) < 10;
      if (nearCorner) continue;
      var state = cell.desiredState(WallTier.WOOD, VillageStyle.MEDITERRANEAN);
      assertTrue(state.is(Blocks.QUARTZ_STAIRS));
      Direction.Axis facing = state.getValue(StairBlock.FACING).getAxis();
      boolean onEastWestRun = Math.min(pos.getZ(), SPAN - pos.getZ()) <= 2;
      assertEquals(onEastWestRun ? Direction.Axis.Z : Direction.Axis.X, facing,
          "coping at " + pos + " should face across the wall, not along it");
      checked++;
    }
    assertTrue(checked > 20, "the coping stairs reach every run and gate");
  }

  @Test
  void theHedgeLinesBothFacesOffTheRouteAndClearOfTheGates() {
    for (boolean sloped : List.of(false, true)) {
      WallProject project = project(sloped);
      Set<Long> route = routeColumns(project);
      List<BlockPos> ring = project.getRing().stream().map(BlockPos::of).toList();
      Map<Long, List<WallBlockPlan>> byColumn = new HashMap<>();
      Set<Long> runColumns = new java.util.HashSet<>();
      for (int i = 0; i < project.sectionCount(); i++) {
        WallSection section = project.section(i);
        boolean linear = section.kind() == WallSectionKind.STRAIGHT
            || section.kind() == WallSectionKind.DIAGONAL || section.kind() == WallSectionKind.TERRACE;
        for (WallBlockPlan cell : section.blocks()) {
          if (!cell.isFoliage()) continue;
          byColumn.computeIfAbsent(column(cell.pos()), ignored -> new ArrayList<>()).add(cell);
          if (linear) runColumns.add(column(cell.pos()));
        }
      }
      assertTrue(runColumns.size() > 40, "the hedge runs along the ring");
      assertTrue(byColumn.size() > runColumns.size(), "the towers and gates keep their authored leaves");
      int outward = 0;
      int inward = 0;
      int jungle = 0;
      int darkOak = 0;
      for (Map.Entry<Long, List<WallBlockPlan>> entry : byColumn.entrySet()) {
        List<WallBlockPlan> stack = entry.getValue();
        BlockPos foot = stack.stream().map(WallBlockPlan::pos).min((a, b) -> Integer.compare(a.getY(), b.getY())).orElseThrow();
        assertFalse(route.contains(column(foot)), "hedge on the wall line at " + foot);
        Set<Long> positions = stack.stream().map(WallBlockPlan::position).collect(Collectors.toSet());
        for (WallBlockPlan cell : stack) {
          boolean isFoot = cell.pos().equals(foot);
          // The foot reaches the ground; a leaf above it is exact, unless a feature's own
          // authored foot landed on a run's hedge, in which case it rests on that leaf.
          assertTrue(isFoot ? cell.role() == WallCellRole.FOUNDATION
              : cell.role() == WallCellRole.EXACT || positions.contains(cell.pos().below().asLong()),
              () -> "hedge role at " + cell.pos() + " in " + describe(project, column(cell.pos())));
          var state = cell.desiredState(WallTier.WOOD, VillageStyle.MEDITERRANEAN);
          assertTrue(state.getValue(LeavesBlock.PERSISTENT));
          if (state.is(Blocks.JUNGLE_LEAVES)) jungle++; else if (state.is(Blocks.DARK_OAK_LEAVES)) darkOak++;
        }
        if (!runColumns.contains(entry.getKey())) continue;
        assertTrue(ring.stream().anyMatch(r -> Math.abs(r.getX() - foot.getX()) <= 1 && Math.abs(r.getZ() - foot.getZ()) <= 1),
            "hedge away from the wall at " + foot);
        assertTrue(stack.size() <= 3, "hedge taller than three at " + foot);
        if (outsideTheRing(foot)) outward++; else inward++;
      }
      assertTrue(outward > 0 && inward > 0 && jungle > 0 && darkOak > 0);
      for (long gate : WallPreview.cardinalGates(project.getRing(), 0, SPAN, 0, SPAN)) {
        BlockPos anchor = BlockPos.of(gate);
        int index = project.getRing().indexOf(gate);
        BlockPos next = ring.get((index + 1) % ring.size());
        int dx = Integer.signum(next.getX() - anchor.getX());
        int dz = Integer.signum(next.getZ() - anchor.getZ());
        for (int along = -1; along <= 1; along++) {
          for (int across : List.of(-2, -1, 1, 2)) {
            BlockPos approach = anchor.offset(along * dx - across * dz, 0, along * dz + across * dx);
            assertFalse(byColumn.containsKey(column(approach)), "hedge blocks the gate approach at " + approach);
          }
        }
      }
    }
  }

  @Test
  void theCapturedFamilySurvivesTheProjectCodec() {
    WallProject project = project(true);
    var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, project).getOrThrow();
    assertEquals(project.plannedBlocks(),
        WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().plannedBlocks());
  }
}
