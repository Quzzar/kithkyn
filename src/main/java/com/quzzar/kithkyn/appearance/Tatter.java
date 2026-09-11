package com.quzzar.kithkyn.appearance;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The rags the undead wear (docs/undead.md).
 *
 * An undead person takes the same occupation wardrobe as anyone else, and the
 * bake shreds it: the garment frays upward from its own hems, sleeves and
 * trouser legs are torn short so forearms and shins are bone, rips open the
 * chest and back along the ribs, every remaining texel is grimed toward old
 * cloth, and the texels bordering a hole darken into a torn edge. No wardrobe
 * needs a second set of art, and the mask only ever removes a bounded share
 * of a garment, so its colour still says something about the job.
 *
 * The mask is a pure function of a seed, the body geometry and the garment's
 * own opaque texels, so every client bakes the same rags for the same person
 * and a person's tears never move between logins. It never touches the head
 * UV, so hoods and their hair-occlusion metadata stay intact, and it never
 * touches a top or bottom face, where a hole would read as a missing lid.
 */
public final class Tatter {

  /** The garment of the living: whole cloth, no seed. */
  public static final int WHOLE = 0;

  private static final int TEXTURE_SIZE = 64;

  /** A column keeps at least this much garment above its hem before it is torn at all. */
  private static final int MINIMUM_COLUMN = 3;
  private static final float RIP_JAG = 0.35F;

  /**
   * How hard the rags are worn. {@code hem} is the chance a column's hem texel
   * goes, then the one above it, and so on up; rips cross the chest and back;
   * {@code stripChance} is the chance a limb column is torn short to
   * {@code stripKeep} rows, a sleeve ending at the elbow or a trouser leg at the
   * knee; {@code slashChance} is a short vertical slash on a limb face.
   */
  public record Strength(
      float[] hem,
      int rips,
      int ripHeight,
      int ripMinWidth,
      int ripMaxWidth,
      float stripChance,
      int stripKeep,
      float slashChance) {

    /**
     * What the undead wear (decided 2026-09-11 over two lighter cuts): barely a
     * garment. Four wide rips open the chest and back onto the ribs, most sleeves
     * and trouser legs are torn short so forearms and shins are bone, slashes
     * cross the limbs, and hems fray four rows deep. The garment's colour and
     * collar survive; its cut mostly does not.
     */
    public static final Strength RUINED = new Strength(
        new float[] {0.85F, 0.60F, 0.35F, 0.15F}, 4, 3, 4, 7, 0.60F, 5, 0.6F);
  }
  /** Grime: pull toward luminance, then darken; fray: darken a torn edge further. */
  private static final float GRIME_DESATURATION = 0.28F;
  private static final float GRIME_DARKENING = 0.85F;
  private static final float FRAY_DARKENING = 0.6F;

  private Tatter() {
  }

  /** Where the rags are torn, and which remaining texels border a tear, as packed {@code y * 64 + x} indices. */
  public record Mask(Set<Integer> torn, Set<Integer> frayed) {

    public Mask {
      torn = Set.copyOf(torn);
      frayed = Set.copyOf(frayed);
    }

    public boolean isTorn(int x, int y) {
      return torn.contains(y * TEXTURE_SIZE + x);
    }

    public boolean isFrayed(int x, int y) {
      return frayed.contains(y * TEXTURE_SIZE + x);
    }
  }

  private record Face(int x, int y, int width, int height) {
  }

  /** The seed an undead person's rags are cut from: their appearance seed and the garment they wear, never {@link #WHOLE}. */
  public static int seed(int appearanceSeed, String garmentId) {
    int mixed = mix32(appearanceSeed ^ garmentId.hashCode() ^ 0x7A77E2ED);
    return mixed == WHOLE ? 1 : mixed;
  }

  /**
   * Cuts the rags. {@code opaque} is the garment layer's opacity, indexed
   * {@code y * 64 + x}; only opaque texels are ever torn or frayed.
   */
  public static Mask of(int seed, BodyModel model, boolean[] opaque, Strength strength) {
    if (opaque.length != TEXTURE_SIZE * TEXTURE_SIZE) {
      throw new IllegalArgumentException("A garment is 64x64");
    }
    Random random = new Random(seed);
    Set<Integer> torn = new HashSet<>();
    int armWidth = model == BodyModel.SLIM ? 3 : 4;
    List<Face> torso = sideFaces(16, 16, 8, 12, 4);
    List<List<Face>> limbs = List.of(
        sideFaces(40, 16, armWidth, 12, 4),
        sideFaces(32, 48, armWidth, 12, 4),
        sideFaces(0, 16, 4, 12, 4),
        sideFaces(16, 48, 4, 12, 4));
    for (List<Face> faces : limbs) {
      for (Face face : faces) {
        stripShort(random, opaque, face, torn, strength);
      }
    }
    for (Face face : torso) {
      tearHem(random, opaque, face, torn, strength);
    }
    for (List<Face> faces : limbs) {
      for (Face face : faces) {
        tearHem(random, opaque, face, torn, strength);
        slash(random, opaque, face, torn, strength);
      }
    }
    for (int rip = 0; rip < strength.rips(); rip++) {
      // Front and back twice as often as a side: that is where the ribs are seen.
      Face face = torso.get(List.of(1, 3, 1, 0, 2).get(random.nextInt(5)));
      ripAcross(random, opaque, face, torn, strength);
    }
    return new Mask(torn, fray(opaque, torn));
  }

  /** Old cloth: the colour pulled toward its own luminance, then darkened. */
  public static int grime(int rgb) {
    int red = rgb >>> 16 & 0xFF;
    int green = rgb >>> 8 & 0xFF;
    int blue = rgb & 0xFF;
    int luminance = (red * 54 + green * 183 + blue * 19) >> 8;
    return pack(
        grimeChannel(red, luminance),
        grimeChannel(green, luminance),
        grimeChannel(blue, luminance));
  }

  /** A torn edge: the grimed colour darkened further, so a hole reads as torn rather than missing. */
  public static int fray(int rgb) {
    return pack(
        Math.round((rgb >>> 16 & 0xFF) * FRAY_DARKENING),
        Math.round((rgb >>> 8 & 0xFF) * FRAY_DARKENING),
        Math.round((rgb & 0xFF) * FRAY_DARKENING));
  }

  /** The four side faces of one cuboid on the standard UV: right, front, left, back. */
  private static List<Face> sideFaces(int u, int v, int width, int height, int depth) {
    return List.of(
        new Face(u, v + depth, depth, height),
        new Face(u + depth, v + depth, width, height),
        new Face(u + depth + width, v + depth, depth, height),
        new Face(u + depth + width + depth, v + depth, width, height));
  }

  /** Frays one face upward from the lowest garment texel still standing in each column. */
  private static void tearHem(Random random, boolean[] opaque, Face face, Set<Integer> torn, Strength strength) {
    for (int x = face.x(); x < face.x() + face.width(); x++) {
      int hem = -1;
      for (int y = face.y() + face.height() - 1; y >= face.y(); y--) {
        if (opaque[index(x, y)] && !torn.contains(index(x, y))) {
          hem = y;
          break;
        }
      }
      // The roll is made for every column, so a column that is skipped does not
      // shift its neighbours' tears.
      float roll = random.nextFloat();
      if (hem < 0 || hem - face.y() < MINIMUM_COLUMN) {
        continue;
      }
      for (int depth = 0; depth < strength.hem().length; depth++) {
        int y = hem - depth;
        if (roll < strength.hem()[depth] && y >= face.y() && opaque[index(x, y)]) {
          torn.add(index(x, y));
        }
      }
    }
  }

  /** Tears a limb column short, to a sleeve at the elbow or a trouser leg at the knee. */
  private static void stripShort(Random random, boolean[] opaque, Face face, Set<Integer> torn, Strength strength) {
    for (int x = face.x(); x < face.x() + face.width(); x++) {
      // One roll per column, whatever it decides.
      boolean stripped = random.nextFloat() < strength.stripChance();
      if (!stripped) {
        continue;
      }
      for (int y = face.y() + strength.stripKeep(); y < face.y() + face.height(); y++) {
        if (opaque[index(x, y)]) {
          torn.add(index(x, y));
        }
      }
    }
  }

  /** A short vertical slash in the upper half of a limb face. */
  private static void slash(Random random, boolean[] opaque, Face face, Set<Integer> torn, Strength strength) {
    // Rolled for every face, so a face without a slash does not move the next one's.
    boolean slashed = random.nextFloat() < strength.slashChance();
    int x = face.x() + random.nextInt(face.width());
    int y = face.y() + 1 + random.nextInt(4);
    int length = 2 + random.nextInt(2);
    if (!slashed) {
      return;
    }
    for (int sy = y; sy < y + length && sy < face.y() + face.height(); sy++) {
      if (opaque[index(x, sy)]) {
        torn.add(index(x, sy));
      }
    }
  }

  /** One rip, wider than tall, in the upper two thirds of a torso face, with jagged ends. */
  private static void ripAcross(Random random, boolean[] opaque, Face face, Set<Integer> torn, Strength strength) {
    int width = face.width() >= 8
        ? strength.ripMinWidth() + random.nextInt(strength.ripMaxWidth() - strength.ripMinWidth() + 1)
        : 2;
    int x = face.x() + random.nextInt(face.width() - width + 1);
    int lowest = face.y() + face.height() - 2 - strength.ripHeight();
    int y = face.y() + 2 + random.nextInt(lowest - (face.y() + 2) + 1);
    for (int ry = y; ry < y + strength.ripHeight(); ry++) {
      for (int rx = x; rx < x + width; rx++) {
        boolean end = rx == x || rx == x + width - 1;
        // The roll is made for every texel, so a jag that is kept does not
        // shift the jags after it.
        boolean jagged = random.nextFloat() < RIP_JAG;
        if (end && jagged) {
          continue;
        }
        if (opaque[index(rx, ry)]) {
          torn.add(index(rx, ry));
        }
      }
    }
  }

  /** Every remaining garment texel that shares an edge with a tear. */
  private static Set<Integer> fray(boolean[] opaque, Set<Integer> torn) {
    Set<Integer> frayed = new HashSet<>();
    for (int packed : torn) {
      int x = packed % TEXTURE_SIZE;
      int y = packed / TEXTURE_SIZE;
      for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
        int nx = x + step[0];
        int ny = y + step[1];
        if (nx < 0 || ny < 0 || nx >= TEXTURE_SIZE || ny >= TEXTURE_SIZE) {
          continue;
        }
        int neighbour = index(nx, ny);
        if (opaque[neighbour] && !torn.contains(neighbour)) {
          frayed.add(neighbour);
        }
      }
    }
    return frayed;
  }

  private static int grimeChannel(int channel, int luminance) {
    return Math.round((channel * (1.0F - GRIME_DESATURATION) + luminance * GRIME_DESATURATION) * GRIME_DARKENING);
  }

  private static int pack(int red, int green, int blue) {
    return clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
  }

  private static int clamp(int channel) {
    return Math.max(0, Math.min(255, channel));
  }

  private static int index(int x, int y) {
    return y * TEXTURE_SIZE + x;
  }

  private static int mix32(int value) {
    value ^= value >>> 16;
    value *= 0x7FEB352D;
    value ^= value >>> 15;
    value *= 0x846CA68B;
    return value ^ value >>> 16;
  }
}
