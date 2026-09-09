package com.quzzar.kithkyn.entities.ai.goals.work;

/** Pure ordering rule for a flooded stretch of shaft. */
final class MineFluidPolicy {

  enum Action {
    SEAL,
    BULKHEAD,
    BAIL,
    BLOCKED
  }

  private MineFluidPolicy() {
  }

  static Action next(boolean boundaryOpen, boolean boundaryReachable,
      boolean bulkheadReachable, boolean waterReachable, boolean hasBucket) {
    if (boundaryOpen) {
      if (boundaryReachable) {
        return Action.SEAL;
      }
      if (bulkheadReachable) {
        return Action.BULKHEAD;
      }
      // Draining reachable water exposes the rest of the boundary. Filling the
      // mine interior instead eventually walls the miner off from that leak.
      return waterReachable && hasBucket ? Action.BAIL : Action.BLOCKED;
    }
    return hasBucket ? Action.BAIL : Action.BLOCKED;
  }
}
