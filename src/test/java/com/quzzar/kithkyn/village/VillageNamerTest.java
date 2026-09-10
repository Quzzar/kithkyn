package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import com.quzzar.kithkyn.village.buildings.VillageStyle;
import net.minecraft.util.RandomSource;

import org.junit.jupiter.api.Test;

class VillageNamerTest {

  @Test
  void badlandsNamesUseTheApprovedCommunityAndDoNotCopyItsExamples() {
    String prompt = VillageNamer.foundingPrompt(VillageStyle.BADLANDS);
    assertTrue(prompt.contains("roof terraces"));
    assertTrue(prompt.contains("protected water"));
    assertNotEquals(prompt, VillageNamer.foundingPrompt(VillageStyle.DESERT));
    for (String example : Set.of("Kestara", "Oravel", "Tavren", "Sorela")) {
      assertTrue(VillageNamer.acceptedName(example, VillageStyle.BADLANDS, Set.of()).isEmpty());
    }
    String fallback = VillageNamer.fallback(VillageStyle.BADLANDS, Set.of(), RandomSource.create(7));
    assertTrue(VillageNamer.acceptedName(fallback, VillageStyle.BADLANDS, Set.of()).isPresent());
    assertFalse(fallback.endsWith("field") || fallback.endsWith("bury") || fallback.endsWith("wick"));
  }

  @Test
  void foundingPromptUsesSelectedArchitecturalIdentityAndExamplesAsInspiration() {
    String prompt = VillageNamer.foundingPrompt(VillageStyle.BIRCH_FOREST);
    assertTrue(prompt.contains("grass roofs"));
    assertTrue(prompt.contains("candles"));
    assertTrue(prompt.contains("Brindle"));
    assertTrue(prompt.contains("do not copy"));
    assertFalse(prompt.contains("inspired only by the natural surroundings"));
    assertNotEquals(prompt, VillageNamer.foundingPrompt(VillageStyle.DESERT));
  }

  @Test
  void rejectsExamplesAndExistingNamesRegardlessOfPresentation() {
    assertTrue(VillageNamer.acceptedName("\"brindle\"", VillageStyle.BIRCH_FOREST, Set.of()).isEmpty());
    assertTrue(VillageNamer.acceptedName("Tallo Wick", VillageStyle.BIRCH_FOREST, Set.of()).isEmpty());
    assertTrue(VillageNamer.acceptedName("little haven", VillageStyle.BIRCH_FOREST, Set.of("Little-Haven")).isEmpty());
    assertEquals("Merriden", VillageNamer.acceptedName("merriden", VillageStyle.BIRCH_FOREST, Set.of()).orElseThrow());
    assertTrue(VillageNamer.acceptedName("", VillageStyle.BIRCH_FOREST, Set.of()).isEmpty());
  }

  @Test
  void fallbackDoesNotRepeatEvenAfterItsWordPoolIsExhausted() {
    for (VillageStyle style : VillageStyle.values()) {
      Set<String> existing = new HashSet<>();
      for (int index = 0; index < 110; index++) {
        String name = VillageNamer.fallback(style, existing, RandomSource.create(7));
        assertTrue(VillageNamer.acceptedName(name, style, existing).isPresent());
        assertTrue(existing.add(name));
      }
    }
  }
}
