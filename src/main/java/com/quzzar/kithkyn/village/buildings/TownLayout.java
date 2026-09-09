package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;

/**
 * Block-space geometry for compact village growth.
 *
 * <p>A planned slot is not a lot on an abstract grid. It is a footprint placed
 * one lane away from a footprint already standing, with one of their edges
 * aligned. Repeating that small relationship produces streets, rows and
 * courtyards without imposing a rigid town plan on uneven terrain.</p>
 */
final class TownLayout {

  static final int MIN_GAP = 1;
  static final int PREFERRED_GAP = 2;

  /** Legal placement preferences, best first; tight and turned sites remain usable. */
  record Preference(int gap, int inwardFronts) { }

  static final Comparator<Preference> PREFERRED_FIRST = Comparator.comparingInt(Preference::gap).reversed()
      .thenComparing(Comparator.comparingInt(Preference::inwardFronts).reversed());

  static final List<Preference> PREFERENCES = List.of(
      new Preference(PREFERRED_GAP, 1), new Preference(PREFERRED_GAP, 0),
      new Preference(MIN_GAP, 1), new Preference(MIN_GAP, 0));

  @FunctionalInterface
  interface ClaimedGround {
    boolean contains(int x, int z);
  }

  record Footprint(int minX, int minZ, int maxX, int maxZ) {

    Footprint {
      if (minX > maxX || minZ > maxZ) {
        throw new IllegalArgumentException("A footprint must have positive spans");
      }
    }

    Footprint moved(Origin origin) {
      return new Footprint(minX + origin.x(), minZ + origin.z(),
          maxX + origin.x(), maxZ + origin.z());
    }
  }

  record Origin(int x, int z) {
  }

  /** How strongly a legal site continues the fabric already standing. */
  record Relationship(int adjacentSides, int frontage) {
  }

  private TownLayout() {
  }

  /**
   * Origins that put {@code candidate} beside {@code anchor}, leaving exactly
   * {@code laneWidth} clear blocks between them. Each side is tried with the
   * beginnings, centres and ends of the two edges aligned.
   */
  static List<Origin> frontageOrigins(Footprint anchor, Footprint candidate, int laneWidth) {
    Set<Origin> origins = new LinkedHashSet<>();
    for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
      origins.addAll(frontageOrigins(anchor, candidate, laneWidth, side));
    }
    return List.copyOf(origins);
  }

  /** One edge of the same frontage search, for candidates whose entrance must face inward. */
  static List<Origin> frontageOrigins(Footprint anchor, Footprint candidate, int laneWidth,
      net.minecraft.core.Direction side) {
    int separation = laneWidth + 1;
    Set<Origin> origins = new LinkedHashSet<>();
    if (side.getAxis() == net.minecraft.core.Direction.Axis.X) {
      int x = side == net.minecraft.core.Direction.WEST
          ? anchor.minX() - separation - candidate.maxX() : anchor.maxX() + separation - candidate.minX();
      for (int z : alignedOrigins(anchor.minZ(), anchor.maxZ(), candidate.minZ(), candidate.maxZ())) {
        origins.add(new Origin(x, z));
      }
    } else if (side.getAxis() == net.minecraft.core.Direction.Axis.Z) {
      int z = side == net.minecraft.core.Direction.NORTH
          ? anchor.minZ() - separation - candidate.maxZ() : anchor.maxZ() + separation - candidate.minZ();
      for (int x : alignedOrigins(anchor.minX(), anchor.maxX(), candidate.minX(), candidate.maxX())) {
        origins.add(new Origin(x, z));
      }
    } else {
      throw new IllegalArgumentException("Frontage must use a horizontal side");
    }
    return List.copyOf(origins);
  }

  /** The centered slot on one side, rounded by at most half a block for mixed odd/even spans. */
  static Origin centeredFrontageOrigin(Footprint anchor, Footprint candidate, int laneWidth,
      net.minecraft.core.Direction side) {
    return frontageOrigins(anchor, candidate, laneWidth, side).stream()
        .min(Comparator.comparingInt(origin -> centreShiftSqr(anchor, candidate.moved(origin))))
        .orElseThrow();
  }

  /**
   * Every origin at which {@code candidate} fully contains {@code standing}.
   * Closest-centred placements come first, so an upgrade expands evenly unless
   * terrain or a neighbour makes a directional extension cheaper.
   */
  static List<Origin> containingOrigins(Footprint standing, Footprint candidate) {
    int minOriginX = standing.maxX() - candidate.maxX();
    int maxOriginX = standing.minX() - candidate.minX();
    int minOriginZ = standing.maxZ() - candidate.maxZ();
    int maxOriginZ = standing.minZ() - candidate.minZ();
    if (minOriginX > maxOriginX || minOriginZ > maxOriginZ) {
      return List.of();
    }

    return replacementOrigins(standing, candidate);
  }

  /** Alignments for growing or shrinking a parcel; callers validate any uncovered old cells. */
  static List<Origin> replacementOrigins(Footprint standing, Footprint candidate) {
    int edgeX = standing.maxX() - candidate.maxX();
    int otherX = standing.minX() - candidate.minX();
    int edgeZ = standing.maxZ() - candidate.maxZ();
    int otherZ = standing.minZ() - candidate.minZ();
    int minOriginX = Math.min(edgeX, otherX), maxOriginX = Math.max(edgeX, otherX);
    int minOriginZ = Math.min(edgeZ, otherZ), maxOriginZ = Math.max(edgeZ, otherZ);

    List<Origin> origins = new ArrayList<>();
    for (int x = minOriginX; x <= maxOriginX; x++) {
      for (int z = minOriginZ; z <= maxOriginZ; z++) {
        origins.add(new Origin(x, z));
      }
    }
    origins.sort(Comparator.comparingInt(origin -> centreShiftSqr(standing, candidate.moved(origin))));
    return List.copyOf(origins);
  }

  /** Counts each edge column once across either permitted lane width. */
  static Relationship relationship(Footprint candidate, ClaimedGround claimed) {
    int sides = 0;
    int frontage = 0;

    int west = claimedAlongZ(candidate.minX(), -1, candidate.minZ(), candidate.maxZ(), claimed);
    int east = claimedAlongZ(candidate.maxX(), 1, candidate.minZ(), candidate.maxZ(), claimed);
    int north = claimedAlongX(candidate.minZ(), -1, candidate.minX(), candidate.maxX(), claimed);
    int south = claimedAlongX(candidate.maxZ(), 1, candidate.minX(), candidate.maxX(), claimed);
    for (int edge : new int[] {west, east, north, south}) {
      if (edge > 0) {
        sides++;
        frontage += edge;
      }
    }
    return new Relationship(sides, frontage);
  }

  /** True when a footprint leaves the requested number of clear columns around every edge. */
  static boolean hasClearance(Footprint footprint, int gap, ClaimedGround claimed) {
    for (int x = footprint.minX() - gap; x <= footprint.maxX() + gap; x++) {
      for (int z = footprint.minZ() - gap; z <= footprint.maxZ() + gap; z++) {
        if (claimed.contains(x, z)) return false;
      }
    }
    return true;
  }

  /** Inclusive bounds leave this many walking blocks between the nearest edges. */
  static int clearGap(Footprint first, Footprint second) {
    return Math.max(Math.max(second.minX() - first.maxX() - 1, first.minX() - second.maxX() - 1),
        Math.max(second.minZ() - first.maxZ() - 1, first.minZ() - second.maxZ() - 1));
  }

  /** The front points along a closest cardinal direction to the center; diagonal ties allow both. */
  static boolean facesCenter(Footprint footprint, Direction front, Origin center) {
    int towardX = 2 * center.x() - footprint.minX() - footprint.maxX();
    int towardZ = 2 * center.z() - footprint.minZ() - footprint.maxZ();
    int forward = front.getStepX() * towardX + front.getStepZ() * towardZ;
    int sideways = front.getAxis() == Direction.Axis.X ? towardZ : towardX;
    return forward > 0 && forward >= Math.abs(sideways);
  }

  /** Preferences are measured from the placed footprint, independently of its authored origin. */
  static Preference preference(Footprint footprint, Direction front, Origin center, ClaimedGround claimed) {
    return new Preference(hasClearance(footprint, PREFERRED_GAP, claimed) ? PREFERRED_GAP : MIN_GAP,
        facesCenter(footprint, front, center) ? 1 : 0);
  }

  private static List<Integer> alignedOrigins(int anchorMin, int anchorMax, int candidateMin, int candidateMax) {
    List<Integer> alignments = new ArrayList<>(3);
    alignments.add(anchorMin - candidateMin);
    alignments.add(Math.floorDiv(anchorMin + anchorMax - candidateMin - candidateMax, 2));
    alignments.add(anchorMax - candidateMax);
    return alignments;
  }

  private static int centreShiftSqr(Footprint first, Footprint second) {
    int deltaX = first.minX() + first.maxX() - second.minX() - second.maxX();
    int deltaZ = first.minZ() + first.maxZ() - second.minZ() - second.maxZ();
    return deltaX * deltaX + deltaZ * deltaZ;
  }

  private static int claimedAlongZ(int x, int outward, int minZ, int maxZ, ClaimedGround claimed) {
    int count = 0;
    for (int z = minZ; z <= maxZ; z++) {
      for (int gap = MIN_GAP; gap <= PREFERRED_GAP; gap++) {
        if (claimed.contains(x + outward * (gap + 1), z)) {
          count++;
          break;
        }
      }
    }
    return count;
  }

  private static int claimedAlongX(int z, int outward, int minX, int maxX, ClaimedGround claimed) {
    int count = 0;
    for (int x = minX; x <= maxX; x++) {
      for (int gap = MIN_GAP; gap <= PREFERRED_GAP; gap++) {
        if (claimed.contains(x, z + outward * (gap + 1))) {
          count++;
          break;
        }
      }
    }
    return count;
  }
}
