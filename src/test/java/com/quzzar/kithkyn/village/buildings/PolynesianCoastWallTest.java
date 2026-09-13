package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Study A, the Polynesian Coast palisade Aaron locked on 2026-09-12: the Birch
 * geometry in stripped spruce with spruce fence tips and oak slab walks, on a
 * dead coral footing the catalog seats on the ground.
 */
class PolynesianCoastWallTest {

  private static final int SPAN = 64;
  private static final VillageStyle STYLE = VillageStyle.POLYNESIAN_COAST;
  /** The route ordinal of a gatehouse template that sits on its gate anchor. */
  private static final int GATEHOUSE_ANCHOR_X = 8;

  /** Flat ground, the gentle roll of the Birch tests, and a steep sawtooth that lifts terraces off it. */
  private static final List<IntUnaryOperator> TERRAIN = List.of(
      i -> 0, i -> (i / 10) % 3, i -> Math.abs(i % 24 - 12));

  /** One block of a bundled study A template, with the facing and slab half it was captured with. */
  private record StudyCell(int x, int y, int z, String name, Direction facing, String slabType) {
  }

  /** A bundled study A template: its depth across the wall and its blocks. */
  private record StudyTemplate(int sizeZ, List<StudyCell> cells) {
  }

  private static List<Long> ring() {
    return WallRoute.aroundBox(0, SPAN, 0, SPAN);
  }

  private static List<Integer> ground(List<Long> ring, IntUnaryOperator height) {
    List<Integer> ground = new ArrayList<>(ring.size());
    for (int i = 0; i < ring.size(); i++) ground.add(64 + height.applyAsInt(i));
    return ground;
  }

  private static WallProject project(List<Long> ring, List<Integer> ground) {
    return new WallProject(ring, WallPreview.cardinalGates(ring, 0, SPAN, 0, SPAN), ground,
        WallTier.WOOD, STYLE);
  }

  private static Map<Long, WallBlockPlan> byPosition(WallProject project) {
    return project.plannedBlocks().stream()
        .collect(Collectors.toMap(WallBlockPlan::position, Function.identity()));
  }

  private static boolean isBodyCourse(WallBlockPlan.Piece piece) {
    return piece == WallBlockPlan.Piece.BODY || piece == WallBlockPlan.Piece.POST
        || piece == WallBlockPlan.Piece.CORAL_FOOTING;
  }

  private static long column(BlockPos pos) {
    return BlockPos.asLong(pos.getX(), 0, pos.getZ());
  }

  /** Columns a gatehouse or watchtower owns; everything else on the route is a run. */
  private static Set<Long> featureColumns(WallProject project) {
    List<Long> ring = project.getRing();
    Set<Long> columns = new HashSet<>();
    for (long gate : project.getGates()) {
      columns.addAll(AuthoredWoodWallSegments.POLYNESIAN_COAST.footprintAt(
          ring, ring.indexOf(gate), WallSectionKind.GATEHOUSE));
    }
    for (long tower : WallFeaturePlacement.towerAnchors(ring, project.getTowerExclusions())) {
      columns.addAll(AuthoredWoodWallSegments.POLYNESIAN_COAST.footprintAt(
          ring, ring.indexOf(tower), WallSectionKind.CORNER_TOWER));
    }
    return columns;
  }

  private static StudyTemplate studyA(String piece) throws IOException {
    String path = "data/kithkyn/structure/wall/polynesian_coast/" + piece + ".nbt";
    try (InputStream input = PolynesianCoastWallTest.class.getClassLoader().getResourceAsStream(path)) {
      assertNotNull(input, "missing " + path);
      CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
      ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
      List<StudyCell> cells = new ArrayList<>();
      for (Tag item : root.getList("blocks", Tag.TAG_COMPOUND)) {
        CompoundTag block = (CompoundTag) item;
        CompoundTag state = palette.getCompound(block.getInt("state"));
        if ("minecraft:air".equals(state.getString("Name"))) continue;
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        CompoundTag properties = state.getCompound("Properties");
        cells.add(new StudyCell(pos.getInt(0), pos.getInt(1), pos.getInt(2), state.getString("Name"),
            properties.contains("facing") ? Direction.byName(properties.getString("facing")) : null,
            properties.contains("type") ? properties.getString("type") : null));
      }
      return new StudyTemplate(root.getList("size", Tag.TAG_INT).getInt(2), List.copyOf(cells));
    }
  }

  @Test
  void onlyThePolynesianFamilyAuthorsAFooting() {
    assertTrue(AuthoredWoodWallSegments.POLYNESIAN_COAST.hasFooting());
    for (AuthoredWoodWallSegments family : List.of(AuthoredWoodWallSegments.INSTANCE,
        AuthoredWoodWallSegments.BIRCH_FOREST, AuthoredWoodWallSegments.ARID,
        AuthoredWoodWallSegments.SWAMP, AuthoredWoodWallSegments.MEDITERRANEAN)) {
      assertFalse(family.hasFooting(), "an existing family's compile must not change");
    }
  }

  @Test
  void studyAGatehousesAreReproducedCellForCellOnFlatGround() throws IOException {
    List<Long> ring = ring();
    WallProject project = project(ring, ground(ring, TERRAIN.get(0)));
    Map<Long, WallBlockPlan> planned = byPosition(project);
    List<WallBlockPlan> sequence = project.plannedBlocks();
    Map<Long, Integer> order = new HashMap<>();
    for (int i = 0; i < sequence.size(); i++) order.put(sequence.get(i).position(), i);
    StudyTemplate gatehouse = studyA("gatehouse");
    assertEquals(4, project.getGates().size());
    int checked = 0;
    for (long gate : project.getGates()) {
      int index = ring.indexOf(gate);
      BlockPos anchor = BlockPos.of(gate);
      BlockPos next = BlockPos.of(ring.get((index + 1) % ring.size()));
      int tx = Integer.signum(next.getX() - anchor.getX());
      int tz = Integer.signum(next.getZ() - anchor.getZ());
      int base = project.getDeck().get(index) - (WallTier.WOOD.height() - 1);
      for (StudyCell cell : gatehouse.cells()) {
        int along = cell.x() - GATEHOUSE_ANCHOR_X;
        int across = cell.z() - gatehouse.sizeZ() / 2;
        BlockPos world = new BlockPos(anchor.getX() + tx * along - tz * across, base + cell.y(),
            anchor.getZ() + tz * along + tx * across);
        WallBlockPlan compiled = planned.get(world.asLong());
        assertNotNull(compiled, () -> "study A's " + cell + " is missing at " + world);
        BlockState state = compiled.desiredState(WallTier.WOOD, STYLE);
        assertEquals(cell.name(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
            () -> "study A's " + cell + " resolves differently at " + world);
        if (cell.slabType() != null) {
          assertEquals(cell.slabType(), state.getValue(SlabBlock.TYPE).getSerializedName(),
              () -> "slab half of " + cell + " at " + world);
        }
        if (cell.facing() != null) {
          Direction turned = Direction.fromDelta(
              tx * cell.facing().getStepX() - tz * cell.facing().getStepZ(), 0,
              tz * cell.facing().getStepX() + tx * cell.facing().getStepZ());
          assertEquals(turned, state.getValue(BlockStateProperties.HORIZONTAL_FACING),
              () -> "facing of " + cell + " at " + world);
          if (state.is(Blocks.WHITE_WALL_BANNER) || state.is(Blocks.WALL_TORCH)) {
            long support = world.relative(turned.getOpposite()).asLong();
            assertTrue(order.containsKey(support) && order.get(support) < order.get(world.asLong()),
                () -> "the flag or torch at " + world + " is built before its support");
          }
        }
        checked++;
      }
    }
    assertEquals(4 * gatehouse.cells().size(), checked);
  }

  @Test
  void theFootingIsOneCoralCourseSeatedOnEveryColumnsGround() {
    boolean lifted = false;
    for (IntUnaryOperator terrain : TERRAIN) {
      List<Long> ring = ring();
      List<Integer> ground = ground(ring, terrain);
      WallProject project = project(ring, ground);
      Map<Long, WallBlockPlan> planned = byPosition(project);
      int coral = 0;
      for (WallBlockPlan cell : project.plannedBlocks()) {
        if (!isBodyCourse(cell.piece())) continue;
        BlockPos pos = cell.pos();
        int groundY = AuthoredWoodWallSegments.nearestGround(ring, ground, pos.getX(), pos.getZ());
        BlockState state = cell.desiredState(WallTier.WOOD, STYLE);
        if (pos.getY() <= groundY) {
          assertTrue(state.is(Blocks.DEAD_BUBBLE_CORAL_BLOCK), () -> "spruce on or in the ground at " + pos);
          coral++;
        } else {
          assertTrue(state.is(Blocks.STRIPPED_SPRUCE_WOOD), () -> "coral above the ground at " + pos);
        }
      }
      assertTrue(coral > 0);
      Set<Long> features = featureColumns(project);
      int seated = 0;
      for (int i = 0; i < ring.size(); i++) {
        BlockPos route = BlockPos.of(ring.get(i));
        lifted |= project.getDeck().get(i) > ground.get(i) + WallTier.WOOD.height() - 1;
        if (features.contains(column(route))) continue;
        BlockPos foot = new BlockPos(route.getX(), ground.get(i), route.getZ());
        WallBlockPlan cell = planned.get(foot.asLong());
        assertNotNull(cell, () -> "the run has no course on the ground at " + foot);
        assertEquals(WallBlockPlan.Piece.CORAL_FOOTING, cell.piece(),
            () -> "the run's ground course at " + foot);
        seated++;
      }
      // Gatehouses and watchtowers own about half of this ring; the rest is run.
      assertTrue(seated > ring.size() / 4, seated + " run columns checked of " + ring.size());
    }
    assertTrue(lifted, "the sawtooth lifts decks, where a coral course pinned to y 0 would float");
  }

  @Test
  void guardPostsStandOnTheOakSlabWalksWhereBirchPostsStand() {
    List<Long> ring = ring();
    Set<Long> gates = WallPreview.cardinalGates(ring, 0, SPAN, 0, SPAN);
    List<Integer> ground = Collections.nCopies(ring.size(), 64);
    WallProject wall = WallProject.completed(ring, gates, ground, WallTier.WOOD, STYLE);
    List<WallPost> posts = WallPosts.plan(wall);
    assertEquals(WallPosts.plan(WallProject.completed(
        ring, gates, ground, WallTier.WOOD, VillageStyle.BIRCH_FOREST)), posts,
        "the same geometry posts its guards in the same places");
    Map<Long, WallBlockPlan> planned = byPosition(wall);
    int raised = 0;
    for (WallPost post : posts) {
      if (!post.duty().usesCrossbow()) continue;
      WallBlockPlan floor = planned.get(post.position().below().asLong());
      assertNotNull(floor, () -> "crossbow post in the air at " + post.position());
      assertTrue(floor.desiredState(WallTier.WOOD, STYLE).is(Blocks.OAK_SLAB),
          () -> "crossbow post off the oak slab walks at " + post.position());
      raised++;
    }
    assertTrue(raised >= 8, "a crossbow above every gate and on every watchtower");
  }

  @Test
  void theFamilySurvivesTheProjectCodec() {
    List<Long> ring = ring();
    WallProject project = project(ring, ground(ring, TERRAIN.get(2)));
    var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, project).getOrThrow();
    assertEquals(project.plannedBlocks(),
        WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().plannedBlocks());
  }
}
