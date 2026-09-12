package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class ChopScanBoundsTest {

  @Test
  void aVillageWideBoxIsCutDownToWhatOnePathSearchCanReach() {
    // Claim centre at the origin, 64 each way; a lumberjack at the lodge 70 blocks east.
    ChopStep.ScanBounds bounds = ChopStep.ScanBounds.within(new BlockPos(0, 64, 0), 64,
        new BlockPos(70, 64, 5), 44);
    assertNotNull(bounds);
    assertEquals(26, bounds.minX());
    assertEquals(64, bounds.maxX());
    assertEquals(-39, bounds.minZ());
    assertEquals(49, bounds.maxZ());
  }

  @Test
  void aWorkerOutOfReachOfTheWholeBoxScansNothing() {
    assertNull(ChopStep.ScanBounds.within(new BlockPos(0, 64, 0), 16, new BlockPos(200, 64, 0), 44));
  }

  @Test
  void reachIsMeasuredAcrossTheGroundNotUpTheHill() {
    BlockPos worker = new BlockPos(0, 64, 0);
    assertTrue(ChopStep.ScanBounds.withinReach(new BlockPos(30, 90, 30), worker, 44));
    assertFalse(ChopStep.ScanBounds.withinReach(new BlockPos(32, 64, 32), worker, 44));
    assertEquals(25L, ChopStep.ScanBounds.horizontalDistSqr(new BlockPos(3, 0, 4), new BlockPos(0, 99, 0)));
  }
}
