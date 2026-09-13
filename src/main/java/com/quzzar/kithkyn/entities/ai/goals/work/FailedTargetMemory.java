package com.quzzar.kithkyn.entities.ai.goals.work;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;

/** A small transient retry window for work positions that pathfinding could not reach. */
final class FailedTargetMemory {

  private final int retryTicks;
  private final Long2IntOpenHashMap failedUntil = new Long2IntOpenHashMap();

  FailedTargetMemory(int retryTicks) {
    this.retryTicks = retryTicks;
  }

  void reject(BlockPos target, int now) {
    failedUntil.put(target.asLong(), now + retryTicks);
  }

  boolean contains(BlockPos target, int now) {
    long key = target.asLong();
    int until = failedUntil.getOrDefault(key, Integer.MIN_VALUE);
    if (until == Integer.MIN_VALUE) {
      return false;
    }
    if (now >= until) {
      failedUntil.remove(key);
      return false;
    }
    return true;
  }
}
