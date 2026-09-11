package com.quzzar.kithkyn.entities.ai.goals.work;

/**
 * Pure ordering rule for a flooded stretch of shaft. A leak is sealed as soon
 * as the miner has lining to seal it with, wherever it is; water is drained
 * with a bucket, or plugged with lining and quarried back out without one.
 */
final class MineFluidPolicy {

  enum Action {
    SEAL,
    BAIL,
    PLUG,
    BLOCKED
  }

  private MineFluidPolicy() {
  }

  static Action next(boolean boundaryOpen, boolean waterFound, boolean hasBucket, boolean hasSupport) {
    if (boundaryOpen && hasSupport) {
      // Every leak first, or drained and plugged water refills from it.
      return Action.SEAL;
    }
    if (!waterFound) {
      return Action.BLOCKED;
    }
    if (hasBucket) {
      // With a leak still open and nothing to seal it, draining exposes the rest
      // of the boundary for a later restock; a closed pocket simply drains.
      return Action.BAIL;
    }
    return hasSupport ? Action.PLUG : Action.BLOCKED;
  }
}
