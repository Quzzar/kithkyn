package com.quzzar.kithkyn.appearance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class TatterTest {

  private static final int SIZE = 64;
  /** A lighter cut than the shipped one, for the comparisons below. */
  private static final Tatter.Strength LIGHT = new Tatter.Strength(
      new float[] {0.5F, 0.2F}, 1, 2, 3, 4, 0.0F, 0, 0.0F);
  private static final List<Tatter.Strength> STRENGTHS = List.of(LIGHT, Tatter.Strength.RUINED);

  @Test
  void theSameSeedCutsTheSameRags() {
    boolean[] garment = fullGarment();
    Tatter.Mask first = Tatter.of(4242, BodyModel.SLIM, garment, Tatter.Strength.RUINED);
    Tatter.Mask again = Tatter.of(4242, BodyModel.SLIM, garment, Tatter.Strength.RUINED);
    assertEquals(first, again);
    assertNotEquals(first.torn(), Tatter.of(4243, BodyModel.SLIM, garment, Tatter.Strength.RUINED).torn());
  }

  @Test
  void ragsNeverTouchTheHeadOrATopOrBottomFace() {
    boolean[] garment = fullGarment();
    for (Tatter.Strength strength : STRENGTHS) {
    for (BodyModel model : BodyModel.values()) {
      for (int seed = 1; seed < 64; seed++) {
        Tatter.Mask mask = Tatter.of(seed, model, garment, strength);
        assertFalse(mask.torn().isEmpty(), "seed " + seed + " tore nothing");
        for (int packed : mask.torn()) {
          int x = packed % SIZE;
          int y = packed / SIZE;
          assertTrue(y >= 16, "tore the head UV at " + x + "," + y);
          assertFalse(y >= 16 && y < 20, "tore a top or bottom face at " + x + "," + y);
          assertFalse(y >= 48 && y < 52, "tore a top or bottom face at " + x + "," + y);
        }
      }
    }
    }
  }

  @Test
  void ragsOnlyTearAndFrayWhatIsThere() {
    boolean[] garment = new boolean[SIZE * SIZE];
    // A vest: the torso's front face alone.
    for (int y = 20; y < 32; y++) {
      for (int x = 20; x < 28; x++) {
        garment[y * SIZE + x] = true;
      }
    }
    Tatter.Mask mask = Tatter.of(99, BodyModel.WIDE, garment, Tatter.Strength.RUINED);
    for (int packed : mask.torn()) {
      assertTrue(garment[packed], "tore a texel the garment never had");
    }
    for (int packed : mask.frayed()) {
      assertTrue(garment[packed], "frayed a texel the garment never had");
      assertFalse(mask.torn().contains(packed), "a texel is both torn and frayed");
    }
    assertFalse(mask.frayed().isEmpty());
  }

  @Test
  void aRealWardrobeIsRuinedButNotGone() throws IOException {
    boolean[] garment = shipped("fieldhand-tunic");
    int cloth = 0;
    for (boolean opaque : garment) {
      if (opaque) {
        cloth++;
      }
    }
    for (int seed = 1; seed < 32; seed++) {
      Tatter.Mask light = Tatter.of(seed, BodyModel.SLIM, garment, LIGHT);
      assertFalse(light.torn().isEmpty(), "seed " + seed + " tore nothing");
      assertTrue(light.torn().size() <= cloth / 4, "seed " + seed + " shredded the tunic past recognition");
      Tatter.Mask ruined = Tatter.of(seed, BodyModel.SLIM, garment, Tatter.Strength.RUINED);
      assertTrue(ruined.torn().size() > light.torn().size(), "seed " + seed + ": ruined tore less than a light cut");
      assertTrue(ruined.torn().size() >= cloth / 5, "seed " + seed + ": ruined is not ruined");
      assertTrue(ruined.torn().size() <= cloth * 3 / 5, "seed " + seed + ": ruined left no garment at all");
    }
  }

  @Test
  void everyFrayedTexelBordersATear() {
    Tatter.Mask mask = Tatter.of(7, BodyModel.SLIM, fullGarment(), Tatter.Strength.RUINED);
    for (int packed : mask.frayed()) {
      int x = packed % SIZE;
      int y = packed / SIZE;
      boolean borders = List.of(new int[] {1, 0}, new int[] {-1, 0}, new int[] {0, 1}, new int[] {0, -1}).stream()
          .anyMatch(step -> mask.isTorn(x + step[0], y + step[1]));
      assertTrue(borders, "frayed texel at " + x + "," + y + " borders no tear");
    }
  }

  @Test
  void theSeedIsNeverWholeCloth() {
    for (int seed = 0; seed < 1024; seed++) {
      assertNotEquals(Tatter.WHOLE, Tatter.seed(seed, "fieldhand-tunic"));
    }
    assertNotEquals(Tatter.seed(5, "fieldhand-tunic"), Tatter.seed(5, "shepherds-coat"));
  }

  @Test
  void grimeAndFrayOnlyEverDarken() {
    for (int rgb : List.of(0xFFFFFF, 0x7FB6B2, 0x3C6280, 0x000000)) {
      assertTrue(luminance(Tatter.grime(rgb)) <= luminance(rgb));
      assertTrue(luminance(Tatter.fray(rgb)) <= luminance(Tatter.grime(rgb)));
    }
    assertEquals(0, Tatter.grime(0));
  }

  /** Cloth over every base texel of every limb, plus the head, so the mask has everything to refuse. */
  private static boolean[] fullGarment() {
    boolean[] garment = new boolean[SIZE * SIZE];
    Arrays.fill(garment, true);
    return garment;
  }

  private static boolean[] shipped(String asset) throws IOException {
    String path = "assets/kithkyn/textures/entity/person/parts/" + asset + "/clothing.png";
    try (InputStream stream = TatterTest.class.getClassLoader().getResourceAsStream(path)) {
      assertNotNull(stream, path);
      BufferedImage image = ImageIO.read(stream);
      boolean[] garment = new boolean[SIZE * SIZE];
      for (int y = 0; y < SIZE; y++) {
        for (int x = 0; x < SIZE; x++) {
          garment[y * SIZE + x] = (image.getRGB(x, y) >>> 24) != 0;
        }
      }
      return garment;
    }
  }

  private static int luminance(int rgb) {
    return ((rgb >>> 16 & 0xFF) * 54 + (rgb >>> 8 & 0xFF) * 183 + (rgb & 0xFF) * 19) >> 8;
  }
}
