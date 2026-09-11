package com.quzzar.kithkyn.appearance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppearanceAssetTest {

  private static AppearanceCatalog catalog;

  @BeforeAll
  static void loadCatalog() throws IOException {
    try (InputStream stream = Objects.requireNonNull(
            AppearanceAssetTest.class.getClassLoader().getResourceAsStream("assets/kithkyn/appearance/catalog.json"),
            "The appearance catalogue is on the test classpath");
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      catalog = AppearanceCatalog.load(reader);
    }
  }

  @Test
  void everyAuthoredEyeClosesAlongTheBottomRowOfItsOwnMask() {
    int eyes = 0;
    for (AppearanceAsset asset : catalog.assets()) {
      for (AppearancePart eye : List.of(AppearancePart.EYE_LEFT, AppearancePart.EYE_RIGHT)) {
        if (!asset.has(eye)) {
          continue;
        }
        eyes++;
        List<Texel> mask = eye == AppearancePart.EYE_LEFT ? asset.leftEyeTexels() : asset.rightEyeTexels();
        List<Texel> lid = asset.lidTexels(eye);
        int bottom = mask.stream().mapToInt(Texel::y).max().orElseThrow();
        assertFalse(lid.isEmpty(), asset.id() + " " + eye + " has no lid row");
        assertTrue(mask.containsAll(lid), asset.id() + " " + eye + " lid leaves its mask");
        assertTrue(lid.stream().allMatch(texel -> texel.y() == bottom),
            asset.id() + " " + eye + " lid is not one bottom row");
        assertEquals(mask.stream().filter(texel -> texel.y() == bottom).count(), lid.size(),
            asset.id() + " " + eye + " lid misses part of its bottom row");
      }
    }
    assertTrue(eyes > 0, "The catalogue carries eye parts");
  }
}
