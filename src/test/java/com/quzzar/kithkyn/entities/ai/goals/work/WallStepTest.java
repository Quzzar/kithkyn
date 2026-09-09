package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WallStepTest {

  @Test
  void choosesAnotherInteriorFootholdWhenATreeBlocksThePreferredOne() {
    WallWorkPlanner.Offset openSide = new WallWorkPlanner.Offset(0, 1);

    WallWorkPlanner.Offset chosen = WallWorkPlanner.choose(-1, 1, openSide::equals);

    assertEquals(openSide, chosen);
  }

  @Test
  void canBuildFromAReachableSideOfAGateInsteadOfOnlyTowardTheCenter() {
    WallWorkPlanner.Offset side = new WallWorkPlanner.Offset(2, 0);
    assertEquals(side, WallWorkPlanner.choose(-1, -1, side::equals));
  }

  @Test
  void canStandBackFromABlockedOrFloodedWallFoundation() {
    WallWorkPlanner.Offset bank = new WallWorkPlanner.Offset(0, 6);
    assertEquals(bank, WallWorkPlanner.choose(-1, -1, bank::equals));
  }

  @Test
  void canReachMallowensWaterSectionsFromTheirDryBanks() {
    for (WallWorkPlanner.Offset bank : java.util.List.of(new WallWorkPlanner.Offset(-8, -8),
        new WallWorkPlanner.Offset(-9, -7))) {
      assertEquals(bank, WallWorkPlanner.choose(-1, -1, bank::equals));
    }
  }

  @Test
  void neverInventsAnUnreachableFallbackOrUnboundedReach() {
    assertNull(WallWorkPlanner.choose(1, 1, offset -> false));
    assertNull(WallWorkPlanner.choose(1, 1, new WallWorkPlanner.Offset(13, 0)::equals));
  }
}
