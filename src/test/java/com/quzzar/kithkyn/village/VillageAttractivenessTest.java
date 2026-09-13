package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillageAttractivenessTest {

  @Test
  void repeatedShortageReportsWaitForTheVillageCooldown() {
    assertFalse(Village.shortageCooldownElapsed(11_999L, 0L, 12_000L));
    assertTrue(Village.shortageCooldownElapsed(12_000L, 0L, 12_000L));
  }

  @Test
  void theftPenaltyContributesToTheReportedTotal() {
    VillageAttractiveness report = new VillageAttractiveness(
        4, 0, 4, 0, 0,
        0.0F, 0.0F, 0.0F, 2.0F,
        50.0D, 0.0D, 0.0D, 0.0D,
        0.0D, 0.0D, 0.0D, -2.0D);

    assertEquals(48.0D, report.total());
  }
}
