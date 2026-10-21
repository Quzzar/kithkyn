package com.quzzar.kithkyn.agency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import com.quzzar.kithkyn.agency.ResidentActionState.Offer;
import com.quzzar.kithkyn.entities.UndertakingData.Position;

class ResidentActionStateTest {
  private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final Position HERE = new Position(2.5, 64, 2.5);
  private static final Position POINT = new Position(12.5, 64, 8.5);

  private static ResidentActionState state() {
    var data = new java.util.concurrent.atomic.AtomicReference<>(com.quzzar.kithkyn.entities.UndertakingData.EMPTY);
    return new ResidentActionState(() -> new UUID(0, 100), data::get, result -> data.set(result.data()), () -> 0,
        () -> new com.quzzar.kithkyn.entities.UndertakingData.Duty("WANDERER", "ADULT", "", java.util.Optional.empty(), -1, false, false));
  }

  private static Offer offer(ResidentActionState state, UUID requester, long tick) {
    return state.offer(requester, "Speaker", "minecraft:overworld", POINT, HERE, tick);
  }

  @Test
  void agreementDoesNotClaimCompletion() {
    ResidentActionState state = state();
    assertTrue(state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1).accepted());
    assertTrue(state.current().isPresent());
    assertTrue(state.latest().isEmpty());
  }

  @Test
  void anotherSpeakerCannotTakeOverAnActiveIntention() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1);
    var response = state.accept(offer(state, SECOND, 2), Action.WAIT, 3);
    assertFalse(response.accepted());
    assertEquals(FIRST, state.current().orElseThrow().requester());
    assertEquals(Action.FOLLOW, state.current().orElseThrow().action());
  }

  @Test
  void changedInstructionsRecordCancellationRatherThanCompletion() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1);
    var replacement = state.accept(offer(state, FIRST, 2), Action.WAIT, 3);
    assertTrue(replacement.accepted());
    assertEquals(Phase.CANCELLED, replacement.replaced().orElseThrow().end());
    assertEquals(Action.WAIT, state.current().orElseThrow().action());
  }

  @Test
  void aLateAnswerCannotExecuteAfterANewerQuestion() {
    ResidentActionState state = state();
    Offer old = offer(state, FIRST, 0);
    Offer latest = offer(state, FIRST, 1);
    assertFalse(state.accept(old, Action.FOLLOW, 2).accepted());
    assertTrue(state.accept(latest, Action.WAIT, 2).accepted());
  }

  @Test
  void anUnansweredOfferExpiresWithoutAnAction() {
    ResidentActionState state = state();
    Offer old = offer(state, FIRST, 0);
    assertFalse(state.accept(old, Action.FOLLOW, ResidentActionState.OFFER_LIFETIME).accepted());
    assertTrue(state.current().isEmpty());
  }

  @Test
  void pointedMovementRequiresAnOfferedFiniteTarget() {
    ResidentActionState state = state();
    Offer missing = state.offer(FIRST, "Speaker", "minecraft:overworld", null, HERE, 0);
    assertFalse(state.accept(missing, Action.GO_TO_POINT, 1).accepted());
    Offer invalid = state.offer(FIRST, "Speaker", "minecraft:overworld", new Position(Double.NaN, 0, 0), HERE, 2);
    assertFalse(state.accept(invalid, Action.GO_TO_POINT, 3).accepted());
  }

  @Test
  void theDestinationComesFromTheCapturedReference() {
    ResidentActionState state = state();
    Offer captured = offer(state, FIRST, 0);
    state.accept(captured, Action.GO_TO_POINT, 10);
    assertEquals(POINT, state.current().orElseThrow().destination());
  }

  @Test
  void waitingUsesTheCapturedStandingPosition() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.WAIT, 1);
    assertEquals(HERE, state.current().orElseThrow().destination());
  }

  @Test
  void expiryCannotBeConfusedWithSuccess() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1);
    assertTrue(state.expire(ResidentActionState.ORDER_LIFETIME).isEmpty());
    assertEquals(Phase.EXPIRED, state.expire(ResidentActionState.ORDER_LIFETIME + 1).orElseThrow().end());
    assertTrue(state.current().isEmpty());
  }

  @Test
  void anInterruptedRequestInvalidatesItsOldModelOffer() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1);
    Offer pending = offer(state, FIRST, 2);
    state.finish(Phase.INTERRUPTED, "Danger");
    assertFalse(state.accept(pending, Action.WAIT, 3).accepted());
  }

  @Test
  void resumeReleasesThePreviousRequestAndCompletesOnlyTheRelease() {
    ResidentActionState state = state();
    state.accept(offer(state, FIRST, 0), Action.FOLLOW, 1);
    var release = state.accept(offer(state, FIRST, 2), Action.RESUME, 3);
    assertTrue(release.accepted());
    assertEquals(Phase.CANCELLED, release.replaced().orElseThrow().end());
    assertTrue(state.current().isEmpty());
    assertEquals(Action.RESUME, state.latest().orElseThrow().order().action());
    assertEquals(Phase.COMPLETED, state.latest().orElseThrow().end());
  }
}
