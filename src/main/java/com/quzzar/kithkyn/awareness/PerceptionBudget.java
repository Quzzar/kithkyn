package com.quzzar.kithkyn.awareness;

/** Shared per-server tick ceilings. Exhaustion skips evidence explicitly; it never invents a fact. */
public final class PerceptionBudget {
  public static final int QUERIES_PER_TICK = 32;
  public static final int VISIBILITY_PER_TICK = 256;
  public static final int DELIVERIES_PER_TICK = 128;
  public enum Work { QUERY, VISIBILITY, DELIVERY }
  public record Counts(long queries, long visibility, long deliveries, long denied) { }

  private long tick = Long.MIN_VALUE;
  private final int[] used = new int[Work.values().length];
  private final long[] totals = new long[Work.values().length];
  private long denied;

  public boolean take(Work work, long now) {
    if (tick != now) {
      java.util.Arrays.fill(used, 0);
      tick = now;
    }
    int limit = switch (work) {
      case QUERY -> QUERIES_PER_TICK;
      case VISIBILITY -> VISIBILITY_PER_TICK;
      case DELIVERY -> DELIVERIES_PER_TICK;
    };
    if (used[work.ordinal()] >= limit) {
      denied++;
      return false;
    }
    used[work.ordinal()]++;
    totals[work.ordinal()]++;
    return true;
  }

  public Counts counts() { return new Counts(totals[0], totals[1], totals[2], denied); }
}
