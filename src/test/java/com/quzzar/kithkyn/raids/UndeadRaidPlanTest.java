package com.quzzar.kithkyn.raids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.Difficulty;

class UndeadRaidPlanTest {

  @Test
  void peaceSendsNobodyAndDifficultyClimbs() {
    assertEquals(0, UndeadRaidPlan.waves(Difficulty.PEACEFUL));
    assertEquals(2, UndeadRaidPlan.waves(Difficulty.EASY));
    assertEquals(3, UndeadRaidPlan.waves(Difficulty.NORMAL));
    assertEquals(4, UndeadRaidPlan.waves(Difficulty.HARD));
  }

  @Test
  void aHamletMeetsAHandfulAndATownABand() {
    assertEquals(2, UndeadRaidPlan.waveSize(0, 1));
    assertEquals(2, UndeadRaidPlan.waveSize(2, 1));
    assertEquals(5, UndeadRaidPlan.waveSize(9, 1));
    assertEquals(8, UndeadRaidPlan.waveSize(40, 1));
    assertEquals(10, UndeadRaidPlan.waveSize(40, 4));
    for (int population = 0; population < 60; population++) {
      for (int wave = 1; wave < 4; wave++) {
        assertTrue(UndeadRaidPlan.waveSize(population, wave + 1) >= UndeadRaidPlan.waveSize(population, wave));
        assertTrue(UndeadRaidPlan.waveSize(population, wave) <= 10);
      }
    }
  }

  @Test
  void aKitIsFixedByItsSeedAndSharpensWithTheWave() {
    assertEquals(UndeadRaidPlan.kit(91L, 1), UndeadRaidPlan.kit(91L, 1));
    int bows = 0;
    for (long seed = 0; seed < 300; seed++) {
      UndeadRaidPlan.Kit early = UndeadRaidPlan.kit(seed, 1);
      UndeadRaidPlan.Kit late = UndeadRaidPlan.kit(seed, 3);
      assertNotEquals(UndeadRaidPlan.Weapon.IRON_SWORD, early.weapon(), "iron comes with the third wave");
      assertNotEquals(UndeadRaidPlan.Weapon.STONE_SWORD, late.weapon(), "the third wave has moved past stone");
      assertNotEquals(UndeadRaidPlan.Armour.CHAIN, early.chest(), "chain comes with the third wave");
      assertNotEquals(UndeadRaidPlan.Armour.LEATHER, late.chest(), "the third wave has moved past leather");
      if (early.weapon() == UndeadRaidPlan.Weapon.BOW) {
        bows++;
        assertFalse(early.shield(), "an archer carries no shield");
      }
    }
    assertTrue(bows > 60 && bows < 140, "about a third of the dead shoot, saw " + bows + " of 300");
  }
}
