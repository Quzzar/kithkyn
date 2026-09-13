package com.quzzar.kithkyn.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillageTimelapseTest {

  @Test
  void onlyUnfinishedTargetsMayPauseTheSharedSprint() {
    assertTrue(VillageTimelapse.needsMoreBuilds(0, 1));
    assertTrue(VillageTimelapse.needsMoreBuilds(1, 2));
    assertFalse(VillageTimelapse.needsMoreBuilds(1, 1));
    assertFalse(VillageTimelapse.needsMoreBuilds(2, 1));
  }
}
