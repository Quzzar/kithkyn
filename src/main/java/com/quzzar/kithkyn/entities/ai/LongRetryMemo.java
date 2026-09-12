package com.quzzar.kithkyn.entities.ai;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Remembers the last long path retry that failed, so a person who cannot reach
 * a place does not pay for the same failure twice a second.
 *
 * <p>{@link PersonPathNavigation} retries an exact destination with a 128-block
 * horizon and double the node budget when the ordinary search fails, for posts
 * such as a watch platform whose way in is a long detour. A worker re-plans
 * every ten ticks, so one who is stuck, or whose target is walled off, ran
 * that retry on every re-plan: on 2026-09-12 long retries were 38% of all path
 * search time on the live server and reached their target 1.2% of the time,
 * and one sat under a watchdog kill (#138). A failed retry is now not repeated
 * for the same targets for {@link #COOLDOWN_TICKS}, unless the person has
 * moved {@link #MOVED_BLOCKS} since, which gives the search a new start.
 * A retry that reaches its target clears the memory.
 */
public final class LongRetryMemo {

  /** How long a failed long retry to the same targets is not asked again: five seconds. */
  public static final int COOLDOWN_TICKS = 100;

  /** Moving this far since the failure earns a fresh retry: the search starts somewhere new. */
  public static final int MOVED_BLOCKS = 4;

  @Nullable
  private Set<BlockPos> targets;
  @Nullable
  private BlockPos from;
  private long failedAt;

  /** Whether the long retry is worth asking for: false only for a recent failure to these targets from about here. */
  public boolean worthRetrying(Set<BlockPos> wanted, BlockPos standing, long gameTime) {
    if (this.targets == null || this.from == null || !this.targets.equals(wanted)) {
      return true;
    }
    if (gameTime - this.failedAt >= COOLDOWN_TICKS) {
      return true;
    }
    return this.from.distSqr(standing) >= (long) MOVED_BLOCKS * MOVED_BLOCKS;
  }

  /** The long retry to these targets from here came back without reaching them. */
  public void failed(Set<BlockPos> wanted, BlockPos standing, long gameTime) {
    this.targets = Set.copyOf(wanted);
    this.from = standing.immutable();
    this.failedAt = gameTime;
  }

  /** A long retry reached its target: nothing to hold back. */
  public void reached() {
    this.targets = null;
    this.from = null;
  }
}
