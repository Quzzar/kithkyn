package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MineFluidPolicyTest {

  @Test
  void aLeakIsSealedAsSoonAsThereIsLiningWhateverElseIsAtHand() {
    assertEquals(MineFluidPolicy.Action.SEAL, MineFluidPolicy.next(true, true, true, true));
    assertEquals(MineFluidPolicy.Action.SEAL, MineFluidPolicy.next(true, false, false, true));
  }

  @Test
  void aLeakWithNoLiningIsDrainedWithABucketToExposeTheRest() {
    assertEquals(MineFluidPolicy.Action.BAIL, MineFluidPolicy.next(true, true, true, false));
    assertEquals(MineFluidPolicy.Action.BLOCKED, MineFluidPolicy.next(true, true, false, false));
    assertEquals(MineFluidPolicy.Action.BLOCKED, MineFluidPolicy.next(true, false, true, false));
  }

  @Test
  void aClosedPocketDrainsWithABucketAndIsPluggedWithoutOne() {
    assertEquals(MineFluidPolicy.Action.BAIL, MineFluidPolicy.next(false, true, true, true));
    assertEquals(MineFluidPolicy.Action.BAIL, MineFluidPolicy.next(false, true, true, false));
    assertEquals(MineFluidPolicy.Action.PLUG, MineFluidPolicy.next(false, true, false, true));
  }

  @Test
  void aClosedPocketWithNeitherBucketNorLiningStillBlocks() {
    assertEquals(MineFluidPolicy.Action.BLOCKED, MineFluidPolicy.next(false, true, false, false));
    assertEquals(MineFluidPolicy.Action.BLOCKED, MineFluidPolicy.next(false, false, false, true));
  }
}
