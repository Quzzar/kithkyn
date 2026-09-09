package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/** Bounded horizontal approaches to a wall segment, with an inward preference. */
final class WallWorkPlanner {

  /** Horizontal reach around one wall column. */
  static final int MAXIMUM_OFFSET = 12;

  /** One horizontal offset from the wall column. */
  record Offset(int x, int z) {

    private int distanceSqr() {
      return x * x + z * z;
    }

    private int inwardProgress(int inwardX, int inwardZ) {
      return x * inwardX + z * inwardZ;
    }
  }

  private WallWorkPlanner() {
  }

  /**
   * Returns an inward usable offset first, then side or outer approaches,
   * or null when nothing within construction reach can be used.
   */
  @Nullable
  static Offset choose(int inwardX, int inwardZ, Predicate<Offset> usable) {
    return offsets(inwardX, inwardZ).stream().filter(usable).findFirst().orElse(null);
  }

  /** Prefer the village side, but a safe side or outer approach is valid construction access too. */
  static List<Offset> offsets(int inwardX, int inwardZ) {
    List<Offset> candidates = new ArrayList<>();
    for (int x = -MAXIMUM_OFFSET; x <= MAXIMUM_OFFSET; x++) {
      for (int z = -MAXIMUM_OFFSET; z <= MAXIMUM_OFFSET; z++) {
        Offset offset = new Offset(x, z);
        int distanceSqr = offset.distanceSqr();
        if (distanceSqr == 0 || distanceSqr > MAXIMUM_OFFSET * MAXIMUM_OFFSET) {
          continue;
        }
        candidates.add(offset);
      }
    }
    candidates.sort(Comparator.comparingInt((Offset offset) -> offset.inwardProgress(inwardX, inwardZ) > 0 ? 0 : 1)
        .thenComparingInt(Offset::distanceSqr)
        .thenComparing(Comparator.comparingInt(
            (Offset offset) -> offset.inwardProgress(inwardX, inwardZ)).reversed())
        .thenComparingInt(Offset::x)
        .thenComparingInt(Offset::z));
    return candidates;
  }
}
