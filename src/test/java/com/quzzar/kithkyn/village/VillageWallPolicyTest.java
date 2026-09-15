package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillageWallPolicyTest {

  @Test
  void foundingCampDoesNotWallItselfAfterItsFirstWorkshop() {
    assertFalse(Village.canAutomaticallyStartWall(0, 8, 6, 0, 0.0F));
    assertFalse(Village.canAutomaticallyStartWall(0, 8, 6, 6, 1.0F));
  }

  @Test
  void hungryHamletBuildsFoodBeforeDefenses() {
    assertFalse(Village.canAutomaticallyStartWall(1, 10, 8, 7, 1.0F));
  }

  @Test
  void establishedHamletWallsForScaleOrDanger() {
    assertTrue(Village.canAutomaticallyStartWall(1, 8, 8, 8, 0.0F));
    assertTrue(Village.canAutomaticallyStartWall(1, 5, 8, 8, 0.26F));
    assertFalse(Village.canAutomaticallyStartWall(1, 5, 8, 8, 0.25F));
  }
}
