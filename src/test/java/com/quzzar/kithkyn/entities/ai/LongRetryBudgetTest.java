package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class LongRetryBudgetTest {

  private static final long MILLISECOND = 1_000_000L;
  private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void aFreshBudgetAdmitsARetry() {
    assertTrue(new LongRetryBudget().admits(1_000L, FIRST));
  }

  @Test
  void theRetryThatEmptiesTheBucketHoldsBackTheRestOfTheTick() {
    LongRetryBudget budget = new LongRetryBudget();
    assertTrue(budget.admits(1_000L, FIRST));
    budget.charge(1_000L, 50 * MILLISECOND); // one long search two minutes after a restart
    assertFalse(budget.admits(1_000L, SECOND));
  }

  @Test
  void onePersonCannotRunAWholeBurstOfExactRequestsInOneTick() {
    // Twelve cells round a chest used to let its first walker spend the shared bucket alone.
    long search = 4 * MILLISECOND;
    LongRetryBudget budget = new LongRetryBudget();
    int ran = 0;
    for (int cell = 0; cell < 12; cell++) {
      if (budget.admits(1_000L, FIRST)) {
        budget.charge(1_000L, search);
        ran++;
      }
    }
    assertEquals(1, ran);
    assertTrue(budget.admits(1_000L, SECOND));
  }

  @Test
  void anOverdraftIsPaidBackAtTheRefillRateBeforeTheNextRetry() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(1_000L, LongRetryBudget.CAPACITY_NANOS + 3 * LongRetryBudget.REFILL_NANOS_PER_TICK);
    assertFalse(budget.admits(1_003L, FIRST)); // three ticks of refill only clear the debt
    assertTrue(budget.admits(1_004L, FIRST));
  }

  @Test
  void idleTicksNeverSaveMoreThanTheCapacity() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(1_000L, LongRetryBudget.CAPACITY_NANOS);
    assertTrue(budget.admits(100_000L, FIRST));
    budget.charge(100_000L, LongRetryBudget.CAPACITY_NANOS);
    assertFalse(budget.admits(100_000L, SECOND));
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
        UUID requester = new UUID(0L, asker + 1L);
        if (budget.admits(tick, requester)) {
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
  void stableTickOrderDoesNotStarveTheLaterRequester() {
    long search = 12 * MILLISECOND;
    LongRetryBudget budget = new LongRetryBudget();
    int firstRan = 0;
    int secondRan = 0;
    for (long tick = 1_000L; tick < 1_040L; tick++) {
      if (budget.admits(tick, FIRST)) {
        budget.charge(tick, search);
        firstRan++;
      }
      if (budget.admits(tick, SECOND)) {
        budget.charge(tick, search);
        secondRan++;
      }
    }
    assertTrue(firstRan > 0);
    assertTrue(secondRan > 0);
    assertTrue(Math.abs(firstRan - secondRan) <= 1,
        "stable caller order was unfair: first=" + firstRan + ", second=" + secondRan);
  }

  @Test
  void anAbandonedWaiterCannotHoldTheQueueForever() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(1_000L, LongRetryBudget.CAPACITY_NANOS);
    assertFalse(budget.admits(1_000L, FIRST));
    assertFalse(budget.admits(1_001L, SECOND));

    assertTrue(budget.admits(1_000L + LongRetryBudget.WAITER_TIMEOUT_TICKS + 1L, SECOND));
  }

  @Test
  void anotherWorldStartsWithAFullBucket() {
    LongRetryBudget budget = new LongRetryBudget();
    budget.charge(50_000L, 10 * LongRetryBudget.CAPACITY_NANOS);
    assertFalse(budget.admits(50_001L, FIRST));
    assertTrue(budget.admits(20L, SECOND)); // game time ran backwards: a different world was loaded
  }
}
