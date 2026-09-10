package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.entities.Gender;

class VillageRulerTest {
  @Test
  void rulerTitleUsesTheIncumbentsGender() {
    assertEquals("King", VillageRuler.title(Gender.MALE));
    assertEquals("Queen", VillageRuler.title(Gender.FEMALE));
    assertEquals("Sovereign", VillageRuler.title(Gender.NONBINARY));
  }

  @Test
  void aRulerUsesPersonalHistoryWithoutReplacingLegalChoicesOrCurrentFacts() {
    String context = VillageRuler.personalContext("Sunspire", "Elowen Farley", Gender.FEMALE,
        "A builder who values careful spending.", "Counts tools twice.", "curious; drive=0.35",
        List.of("Day 3, morning: Promised the farmers more storage."));
    assertTrue(context.contains("Queen Elowen Farley, ruler of Sunspire"));
    assertTrue(context.contains("values careful spending"));
    assertTrue(context.contains("Counts tools twice"));
    assertTrue(context.contains("drive=0.35"));
    assertTrue(context.contains("Promised the farmers more storage"));
    assertTrue(context.contains("only the offered legal options"));
    assertTrue(context.contains("do not override current facts"));
    assertFalse(context.contains("collective judgement"));
  }

  @Test
  void aVacantOrUnavailableRulerKeepsTheCollectiveFallback() {
    assertEquals("You are the collective judgement of Sunspire.\n", VillageRuler.collectiveContext("Sunspire"));
    assertTrue(VillageRuler.context(null).contains("collective judgement"));
  }

  @Test
  void ordinaryOptimizationCannotDeposeARuler() {
    assertTrue(VillageRuler.hasStableTenure(Occupation.LEADER));
    assertTrue(LaborPlanner.mustKeep(Occupation.LEADER, 1, true, true));
    assertTrue(LaborPlanner.mustKeep(Occupation.LEADER, 2, false, false));
    assertFalse(VillageRuler.hasStableTenure(Occupation.GUARD));
    assertFalse(VillageRuler.hasStableTenure(Occupation.BLACKSMITH));
  }
}
