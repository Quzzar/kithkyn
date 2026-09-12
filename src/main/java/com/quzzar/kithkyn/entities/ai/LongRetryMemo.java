package com.quzzar.kithkyn.entities.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Remembers the long path retries that failed lately, so a person who cannot
 * reach a place does not pay for the same failure twice a second.
 *
 * <p>{@link PersonPathNavigation} retries an exact destination with a 128-block
 * horizon and double the node budget when the ordinary search fails, for posts
 * such as a watch platform whose way in is a long detour. A worker re-plans
 * every ten ticks, so one who is stuck, or whose target is walled off, ran
 * that retry on every re-plan: on 2026-09-12 long retries were 38% of all path
 * search time on the live server and reached their target 1.2% of the time,
 * and one sat under a watchdog kill (#138). A failed retry is now not repeated
 * for the same targets for {@link #COOLDOWN_TICKS}, unless the person has
 * moved {@link #MOVED_BLOCKS} since, which gives the search a new start. A
 * retry that reaches its target forgets that target.
 *
 * <p>Several failures are kept, not one: a villager stuck beside a chest asks
 * for each of the nine cells round it in turn, and a single remembered failure
 * was forgotten before its cell came round again (the A/B run on 2026-09-12
 * that led to {@link #CAPACITY}).
 */
public final class LongRetryMemo {

  /** How long a failed long retry to the same targets is not asked again: five seconds. */
  public static final int COOLDOWN_TICKS = 100;

  /** Moving this far since the failure earns a fresh retry: the search starts somewhere new. */
  public static final int MOVED_BLOCKS = 4;

  /** Failed retries remembered at once: a ring of approach cells round a chest, with room to spare. */
  public static final int CAPACITY = 16;

  /** Where a long retry failed from, and when. */
  private record Failure(BlockPos from, long at) {
  }

  private final Map<Set<BlockPos>, Failure> failures = new LinkedHashMap<>(CAPACITY, 0.75F, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Set<BlockPos>, Failure> eldest) {
      return size() > CAPACITY;
    }
  };

  /** Whether the long retry is worth asking for: false only for a recent failure to these targets from about here. */
  public boolean worthRetrying(Set<BlockPos> wanted, BlockPos standing, long gameTime) {
    Failure failure = this.failures.get(wanted);
    if (failure == null || gameTime - failure.at() >= COOLDOWN_TICKS) {
      return true;
    }
    return failure.from().distSqr(standing) >= (long) MOVED_BLOCKS * MOVED_BLOCKS;
  }

  /** The long retry to these targets from here came back without reaching them. */
  public void failed(Set<BlockPos> wanted, BlockPos standing, long gameTime) {
    this.failures.put(Set.copyOf(wanted), new Failure(standing.immutable(), gameTime));
  }

  /** A long retry reached these targets: nothing to hold back for them. */
  public void reached(Set<BlockPos> wanted) {
    this.failures.remove(wanted);
  }
}
