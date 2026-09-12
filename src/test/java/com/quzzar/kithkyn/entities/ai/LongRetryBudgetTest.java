package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LongRetryBudgetTest {

  private static final long MILLISECOND = 1_000_000L;

  @Test
  void aFreshBudgetAdmitsARetry() {
    assertTrue(new LongRetryBudget().admits(1_000L));
  }

  @Test
  void theRetryThatEmptiesTheBucketHoldsBackTheRestOfTheTick() {
    LongRetryBudget budget = new LongRetryBudget();
    assertTrue(budget.admits(1_000L));
    budget.charge(1_000L, 50 * MILLISECOND); // one long search two minutes after a restart
    assertFalse(budget.admits(1_000L));
  }

  @Test
  void aBurstOfExactRequestsInOneTickRunsOnlyWhatTheBucketHolds() {
    // Twelve cells round a chest, all asked in the same tick, each long search taking 4 ms.
    long search = 4 * MILLISECOND;
    LongRetryBudget budget = new LongRetryBudget();
    int ran = 0;
    for (int cell = 0; cell < 12; cell++) {
      if (budget.admits(1_000L)) {
        budget.charge(1_000L, search);
        ran++;
      }
    }
    assertEquals(Math.ceilDiv(LongRetryBudget.CAPACITY_NANOS, search), ran);
  }

  @Test
  void anOverdraftIsPaidBackAtTheRefillRateBeforeTheNextRetry() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(1_000L, LongRetryBudget.CAPACITY_NANOS + 3 * LongRetryBudget.REFILL_NANOS_PER_TICK);
    assertFalse(budget.admits(1_003L)); // three ticks of refill only clear the debt
    assertTrue(budget.admits(1_004L));
  }

  @Test
  void idleTicksNeverSaveMoreThanTheCapacity() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(1_000L, LongRetryBudget.CAPACITY_NANOS);
    assertTrue(budget.admits(100_000L));
    budget.charge(100_000L, LongRetryBudget.CAPACITY_NANOS);
    assertFalse(budget.admits(100_000L));
  }

  @Test
  void overAnyStretchOfTicksLongRetriesTakeTheRefillAndNeverMore() {
    // Five people wanting a 12 ms long retry every tick, for twenty seconds.
    long search = 12 * MILLISECOND;
    int ticks = 400;
    LongRetryBudget budget = new LongRetryBudget();
    long spent = 0L;
    for (long tick = 1_000L; tick < 1_000L + ticks; tick++) {
      for (int asker = 0; asker < 5; asker++) {
        if (budget.admits(tick)) {
          budget.charge(tick, search);
          spent += search;
        }
      }
    }
    long refill = ticks * LongRetryBudget.REFILL_NANOS_PER_TICK;
    assertTrue(spent <= LongRetryBudget.CAPACITY_NANOS + refill + search, "over budget: " + spent);
    assertTrue(spent >= refill - search, "the refill went unused: " + spent);
  }

  @Test
  void anotherWorldStartsWithAFullBucket() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(50_000L, 10 * LongRetryBudget.CAPACITY_NANOS);
    assertFalse(budget.admits(50_001L));
    assertTrue(budget.admits(20L)); // game time ran backwards: a different world was loaded
  }
}
