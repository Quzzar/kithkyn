package com.quzzar.kithkyn.entities.ai;

/**
 * How much server-thread time the long path retries of every person may take
 * between them, so a burst of them cannot hold up a tick.
 *
 * <p>{@link PersonPathNavigation} retries an exact destination that the
 * ordinary 48-block search cannot reach with a 128-block horizon and double the
 * node budget, for posts such as a watch platform whose way in is a long
 * detour. One such search costs a few milliseconds on a warm server and up to
 * ten times that in the first minutes after a start or on a busy machine, and
 * most of them fail. {@link LongRetryMemo} stops a person asking again for a
 * target that just failed, but not the first asking: a villager trying the
 * twelve cells round a chest ran twelve long searches in one tick, a miner's
 * shaft hop is an exact target too, and workers re-planning together ran theirs
 * in the same tick. On 2026-09-12 the live server, two minutes after a restart,
 * spent about half its time in path searches and was killed by the watchdog.
 *
 * <p>The budget is a bucket of time. It refills {@link #REFILL_NANOS_PER_TICK}
 * a tick, up to {@link #CAPACITY_NANOS}. A long retry may start while anything
 * is left and is charged what it actually took, so the one that empties the
 * bucket can overdraw it, and the refill pays that back before the next one may
 * start. Over any stretch of ticks, long retries take at most the capacity, plus
 * the refill for each tick, plus one search. Time rather than nodes is counted
 * because the cost of a node is what varies: the same search took 3 microseconds
 * a node on the warm server and over 20 two minutes after a restart.
 *
 * <p>A retry the bucket cannot pay for does not run. The caller keeps the
 * ordinary search's answer, exactly as if the retry had failed, and nothing is
 * remembered as failed, so the person asks again at their next re-plan.
 */
public final class LongRetryBudget {

  /** Time added each tick: five milliseconds, a tenth of a tick. */
  public static final long REFILL_NANOS_PER_TICK = 5_000_000L;

  /** Most time the bucket holds: ten milliseconds, a fifth of a tick. */
  public static final long CAPACITY_NANOS = 10_000_000L;

  private long balance = CAPACITY_NANOS;
  private long refilledAt;

  /** Whether a long retry may start at this game time: while any time is left. */
  public boolean admits(long gameTime) {
    refill(gameTime);
    return this.balance > 0L;
  }

  /** A long retry at this game time took {@code nanos} of server-thread time. */
  public void charge(long gameTime, long nanos) {
    refill(gameTime);
    this.balance -= Math.max(0L, nanos);
  }

  private void refill(long gameTime) {
    if (gameTime < this.refilledAt) {
      // Game time only runs backwards when another world is loaded: it starts full.
      this.balance = CAPACITY_NANOS;
    } else {
      long elapsed = gameTime - this.refilledAt;
      long ticksToFull = Math.ceilDiv(CAPACITY_NANOS - this.balance, REFILL_NANOS_PER_TICK);
      this.balance = elapsed >= ticksToFull
          ? CAPACITY_NANOS : this.balance + elapsed * REFILL_NANOS_PER_TICK;
    }
    this.refilledAt = gameTime;
  }
}
