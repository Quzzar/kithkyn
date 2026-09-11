package com.quzzar.kithkyn.appearance;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The rags the undead wear (docs/undead.md).
 *
 * An undead person takes the same occupation wardrobe as anyone else, and the
 * bake shreds it: the garment frays upward from its own hems, so trouser ends
 * show bone toes and cuffs show finger bones whatever the garment's cut, two
 * rips open the chest along the ribs, every remaining texel is grimed toward
 * old cloth, and the texels bordering a hole darken into a torn edge. No
 * wardrobe needs a second set of art, and the job stays readable, because the
 * mask only ever removes a bounded share of a garment.
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

  /** Chance a hem texel is torn away, then that the texel above goes too, then the one above that. */
  private static final float HEM_TORN = 0.65F;
  private static final float HEM_TORN_DEEPER = 0.29F;
  private static final float HEM_TORN_DEEPEST = 0.10F;
  /** A column keeps at least this much garment above its hem before it is torn at all. */
  private static final int MINIMUM_COLUMN = 3;
  /** Rips across the chest and back, each two rows tall along the ribs beneath. */
  private static final int RIPS = 2;
  private static final int RIP_HEIGHT = 2;
  private static final float RIP_JAG = 0.35F;
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
  public static Mask of(int seed, BodyModel model, boolean[] opaque) {
    if (opaque.length != TEXTURE_SIZE * TEXTURE_SIZE) {
      throw new IllegalArgumentException("A garment is 64x64");
    }
    Random random = new Random(seed);
    Set<Integer> torn = new HashSet<>();
    int armWidth = model == BodyModel.SLIM ? 3 : 4;
    List<List<Face>> limbs = List.of(
        sideFaces(16, 16, 8, 12, 4),
        sideFaces(40, 16, armWidth, 12, 4),
        sideFaces(32, 48, armWidth, 12, 4),
        sideFaces(0, 16, 4, 12, 4),
        sideFaces(16, 48, 4, 12, 4));
    for (List<Face> faces : limbs) {
      for (Face face : faces) {
        tearHem(random, opaque, face, torn);
      }
    }
    List<Face> torso = limbs.getFirst();
    for (int rip = 0; rip < RIPS; rip++) {
      // Front and back twice as often as a side: that is where the ribs are seen.
      Face face = torso.get(List.of(1, 3, 1, 0, 2).get(random.nextInt(5)));
      ripAcross(random, opaque, face, torn);
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

  /** Frays one face upward from the lowest garment texel in each column. */
  private static void tearHem(Random random, boolean[] opaque, Face face, Set<Integer> torn) {
    for (int x = face.x(); x < face.x() + face.width(); x++) {
      int hem = -1;
      for (int y = face.y() + face.height() - 1; y >= face.y(); y--) {
        if (opaque[index(x, y)]) {
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
      if (roll < HEM_TORN) {
        torn.add(index(x, hem));
      }
      if (roll < HEM_TORN_DEEPER && opaque[index(x, hem - 1)]) {
        torn.add(index(x, hem - 1));
      }
      if (roll < HEM_TORN_DEEPEST && opaque[index(x, hem - 2)]) {
        torn.add(index(x, hem - 2));
      }
    }
  }

  /** One rip, wider than tall, in the upper two thirds of a torso face, with jagged ends. */
  private static void ripAcross(Random random, boolean[] opaque, Face face, Set<Integer> torn) {
    int width = face.width() >= 8 ? 3 + random.nextInt(3) : 2;
    int x = face.x() + random.nextInt(face.width() - width + 1);
    int lowest = face.y() + face.height() - 2 - RIP_HEIGHT;
    int y = face.y() + 2 + random.nextInt(lowest - (face.y() + 2) + 1);
    for (int ry = y; ry < y + RIP_HEIGHT; ry++) {
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
