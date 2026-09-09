package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class VillagePathsTest {
  @Test
  void allFourGatesHaveStableIdentitiesBeforeWallCompletion() {
    List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
    Set<Long> anchors = Set.of(new BlockPos(32, 0, 0).asLong(), new BlockPos(32, 0, 64).asLong(),
        new BlockPos(0, 0, 32).asLong(), new BlockPos(64, 0, 32).asLong());
    WallProject wall = new WallProject(ring, anchors, Collections.nCopies(ring.size(), 64),
        WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    var gates = VillagePaths.gates(wall);
    assertFalse(wall.isComplete());
    assertEquals(4, gates.size());
    assertEquals(4, gates.stream().map(VillagePaths.Gate::id).distinct().count());
    var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, wall).getOrThrow();
    assertEquals(gates, VillagePaths.gates(WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow()));
    UUID builder = UUID.randomUUID();
    int section = wall.claimSection(builder, BlockPos.ZERO, 100);
    wall.advance(builder, section);
    assertEquals(gates, VillagePaths.gates(wall), "Construction progress must not retrigger paths");
    WallProject raised = new WallProject(ring, anchors, Collections.nCopies(ring.size(), 65),
        WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    assertEquals(gates.stream().map(VillagePaths.Gate::id).toList(),
        VillagePaths.gates(raised).stream().map(VillagePaths.Gate::id).toList());
    assertNotEquals(gates.getFirst().revision(), VillagePaths.gates(raised).getFirst().revision());
  }

  @Test
  void gateApproachUsesSavedGroundAndNeverTheRoofOrAnUnsafeFallback() {
    BlockPos anchor = new BlockPos(10, 64, 20);
    assertEquals(anchor, VillagePaths.approach(anchor, pos -> pos.equals(anchor) || pos.equals(anchor.above(7))));
    assertEquals(anchor.below(), VillagePaths.approach(anchor, anchor.below()::equals));
    assertNull(VillagePaths.approach(anchor, anchor.above(7)::equals));
    assertNull(VillagePaths.approach(anchor, pos -> false));
  }
}
