package com.quzzar.kithkyn.awareness;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.awareness.PerceptionBudget.Work;

class PerceptionBudgetTest {
  @Test void aPopulationSharesTheTickCeiling() {
    var budget = new PerceptionBudget();
    int accepted = 0;
    for (int person = 0; person < 10000; person++) if (budget.take(Work.QUERY, 10)) accepted++;
    assertEquals(PerceptionBudget.QUERIES_PER_TICK, accepted);
    assertEquals(10000 - accepted, budget.counts().denied());
    assertTrue(budget.take(Work.QUERY, 11));
  }

  @Test void visibilityAndDeliveryHaveTheirOwnBounds() {
    var budget = new PerceptionBudget();
    for (int index = 0; index < PerceptionBudget.VISIBILITY_PER_TICK; index++) assertTrue(budget.take(Work.VISIBILITY, 0));
    assertFalse(budget.take(Work.VISIBILITY, 0));
    for (int index = 0; index < PerceptionBudget.DELIVERIES_PER_TICK; index++) assertTrue(budget.take(Work.DELIVERY, 0));
    assertFalse(budget.take(Work.DELIVERY, 0));
    assertEquals(PerceptionBudget.VISIBILITY_PER_TICK, budget.counts().visibility());
    assertEquals(PerceptionBudget.DELIVERIES_PER_TICK, budget.counts().deliveries());
  }
}
