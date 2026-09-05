package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MineShaftTest {

  @Test
  void rampRoutesAdvanceOneBuiltColumnAtATime() {
    assertEquals(23, MineShaft.nextHopColumn(22, 30));
    assertEquals(21, MineShaft.nextHopColumn(22, -1));
  }

  @Test
  void adjacentAndCurrentColumnsRemainTheDestination() {
    assertEquals(23, MineShaft.nextHopColumn(22, 23));
    assertEquals(21, MineShaft.nextHopColumn(22, 21));
    assertEquals(22, MineShaft.nextHopColumn(22, 22));
  }
}
