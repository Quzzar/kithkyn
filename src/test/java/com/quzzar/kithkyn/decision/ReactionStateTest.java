package com.quzzar.kithkyn.decision;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.quzzar.kithkyn.decision.ReactionState.*;
class ReactionStateTest {
  private static final DecisionProvider.Selected WARN = new DecisionProvider.Selected("warn", .8, Map.of("warn", .9, "continue", .1));
  @Test void rejectsLateReplacedReplayedAndCancelledOffers() {
    var state = new ReactionState(); var offer = state.offer(100, List.of(Action.CONTINUE, Action.WARN));
    assertInstanceOf(Accepted.class, state.resolve(offer, WARN, .8, 10));
    assertEquals(Status.STALE, ((Rejected) state.resolve(offer, WARN, .8, 11)).status());
    offer = state.offer(100, List.of(Action.CONTINUE, Action.WARN));
    assertEquals(Status.STALE, ((Rejected) state.resolve(offer, WARN, .8, 100)).status());
    offer = state.offer(200, List.of(Action.CONTINUE, Action.WARN)); state.invalidate();
    assertEquals(Status.STALE, ((Rejected) state.resolve(offer, WARN, .8, 100)).status());
  }
  @Test void thresholdsArePerPurposeAndInvalidChoicesNeverRun() {
    var state = new ReactionState();
    assertEquals(Status.UNCERTAIN, ((Rejected) state.resolve(state.offer(100, List.of(Action.WARN)), WARN, .9, 10)).status());
    assertInstanceOf(Accepted.class, state.resolve(state.offer(100, List.of(Action.WARN)), WARN, .7, 10));
    for (var result : List.of(new DecisionProvider.Selected("teleport", 1, Map.of()),
        new DecisionProvider.Selected("warn", Double.NaN, Map.of()),
        new DecisionProvider.Failure(DecisionProvider.FailureKind.TRANSPORT, "Unavailable"))) {
      assertEquals(Status.FAILED, ((Rejected) state.resolve(state.offer(100, List.of(Action.WARN)), result, .7, 10)).status());
    }
  }
}
