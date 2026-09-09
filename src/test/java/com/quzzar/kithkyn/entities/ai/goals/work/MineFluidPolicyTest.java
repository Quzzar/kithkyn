package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MineFluidPolicyTest {

  @Test
  void sealsAnOpenBoundaryBeforeUsingTheBucket() {
    assertEquals(MineFluidPolicy.Action.SEAL, MineFluidPolicy.next(true, true, true, true, true));
    assertEquals(MineFluidPolicy.Action.SEAL, MineFluidPolicy.next(true, true, true, true, false));
  }

  @Test
  void anUnreachableBoundaryUsesAReachableBulkhead() {
    assertEquals(MineFluidPolicy.Action.BULKHEAD,
        MineFluidPolicy.next(true, false, true, true, true));
    assertEquals(MineFluidPolicy.Action.BULKHEAD,
        MineFluidPolicy.next(true, false, true, true, false));
  }

  @Test
  void anUnreachableBoundaryDrainsReachableWaterToExposeTheRemainingLeak() {
    assertEquals(MineFluidPolicy.Action.BAIL,
        MineFluidPolicy.next(true, false, false, true, true));
    assertEquals(MineFluidPolicy.Action.BLOCKED,
        MineFluidPolicy.next(true, false, false, true, false));
  }

  @Test
  void anUnreachableBoundaryWithoutAnyReachableSealWorkStillBlocks() {
    assertEquals(MineFluidPolicy.Action.BLOCKED,
        MineFluidPolicy.next(true, false, false, false, true));
    assertEquals(MineFluidPolicy.Action.BLOCKED,
        MineFluidPolicy.next(true, false, false, false, false));
  }

  @Test
  void bailsAClosedFloodWhenTheMinerHasABucket() {
    assertEquals(MineFluidPolicy.Action.BAIL,
        MineFluidPolicy.next(false, false, false, false, true));
  }

  @Test
  void aClosedFloodStillNeedsABucket() {
    assertEquals(MineFluidPolicy.Action.BLOCKED,
        MineFluidPolicy.next(false, false, false, false, false));
  }
}
