package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import com.quzzar.kithkyn.agency.ResidentActionState;
import com.quzzar.kithkyn.entities.UndertakingData.*;
import org.junit.jupiter.api.Test;

class UndertakingServiceTest {
  private static final UUID RESIDENT = new UUID(0, 101), REQUESTER = new UUID(0, 202);
  private static final Duty DUTY = new Duty("WANDERER", "ADULT", "", Optional.empty(), -1, false, false);
  private static final Position POINT = new Position(8.5, 64, .5);
  private static ResidentActionState state(AtomicReference<UndertakingData> store, UUID resident) {
    return new ResidentActionState(() -> resident, store::get, result -> store.set(result.data()), () -> 1000, () -> DUTY);
  }
  private static UndertakingData roundTrip(UndertakingData data) {
    return UndertakingData.CODEC.parse(JsonOps.INSTANCE, UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
  }
  private static ResidentActionState.Offer offer(ResidentActionState state, UUID requester, long now) {
    return state.offer(requester, "Requester", "minecraft:overworld", POINT, new Position(.5, 64, .5), now);
  }
  private static UndertakingData begun() {
    var store = new AtomicReference<>(UndertakingData.EMPTY); var state = state(store, RESIDENT);
    assertTrue(state.accept(offer(state, REQUESTER, 0), ResidentActionState.Action.GO_TO_POINT, 1).accepted());
    state.underway(state.current().orElseThrow()); return store.get();
  }
  @Test void theOneSavedStoreRestoresOwnedAcceptedAndUnderwayPromises() {
    var store = new AtomicReference<>(roundTrip(begun())); var loaded = state(store, RESIDENT);
    var order = loaded.current().orElseThrow(); assertEquals(POINT, order.destination()); assertEquals(REQUESTER, order.requester());
    var matter = store.get().undertakings().getFirst(); assertEquals(Phase.UNDERWAY, matter.execution().orElseThrow().phase());
    assertEquals(3, matter.steps().size()); assertFalse(matter.steps().stream().anyMatch(step -> step.text().contains("COMPLETED")));
    assertTrue(state(store, new UUID(0, 999)).current().isEmpty());
  }
  @Test void aFailureSurvivesReloadAndOnlyAnActualOutcomeCanCompleteIt() {
    var store = new AtomicReference<>(begun()); var state = state(store, RESIDENT);
    state.finish(Phase.FAILED, "The route is blocked."); store.set(roundTrip(store.get()));
    assertTrue(state(store, RESIDENT).current().isEmpty());
    var matter = store.get().undertakings().getFirst();
    assertFalse(UndertakingService.transition(store.get(), RESIDENT, matter.id(), Phase.COMPLETED, "I said it was done", 2000).changed());
    String briefing = UndertakingService.briefing(store.get(), RESIDENT, Optional.of(REQUESTER), 16);
    assertTrue(briefing.contains("FAILED: go_to_point")); assertTrue(briefing.contains("route is blocked"));
    assertFalse(briefing.contains("COMPLETED: go_to_point"));
  }
  @Test void availabilityIsReconsideredOnceAcrossReloadWithoutRestartingOrCompleting() {
    var store = new AtomicReference<>(begun()); state(store, RESIDENT).finish(Phase.INTERRUPTED, "Owner unavailable.");
    UUID id = store.get().undertakings().getFirst().id();
    var result = UndertakingService.reconsider(store.get(), RESIDENT, id, "Owner returned", 1001, 20);
    assertTrue(result.changed()); store.set(roundTrip(result.data()));
    assertFalse(UndertakingService.reconsider(store.get(), RESIDENT, id, "Owner returned", 1002, 21).changed());
    assertTrue(state(store, RESIDENT).current().isEmpty()); assertEquals(Phase.INTERRUPTED, store.get().byId(id).orElseThrow().execution().orElseThrow().phase());
  }
  @Test void expiryAndCancellationNeverTurnInterruptedAttemptsIntoCompletions() {
    var store = new AtomicReference<>(begun()); var state = state(store, RESIDENT); state.finish(Phase.FAILED, "Blocked path");
    var matter = store.get().undertakings().getFirst();
    var expired = UndertakingService.transition(store.get(), RESIDENT, matter.id(), Phase.EXPIRED, "Lifetime ended", 9000);
    assertEquals(State.ABANDONED, expired.affected().orElseThrow().state()); assertTrue(expired.detail().contains("FAILED"));
    assertFalse(UndertakingService.reconsider(expired.data(), RESIDENT, matter.id(), "Available", 9001, 9001).changed());
  }
  @Test void aNewValidInstructionClosesEarlierUnfinishedPromisesWithoutMergingThem() {
    var store = new AtomicReference<>(begun()); var state = state(store, RESIDENT); state.finish(Phase.FAILED, "Blocked");
    assertTrue(state.accept(offer(state, REQUESTER, 20), ResidentActionState.Action.WAIT, 21).accepted());
    assertEquals(1, store.get().allOpen().size()); assertEquals(2, store.get().undertakings().size());
    assertEquals(Phase.CANCELLED, store.get().undertakings().getFirst().execution().orElseThrow().phase());
  }
  @Test void anotherResidentOrCounterpartCannotReadOrChangeAnOwnedPromise() {
    var data = begun(); var matter = data.undertakings().getFirst(); UUID other = new UUID(0, 999);
    assertFalse(UndertakingService.transition(data, other, matter.id(), Phase.COMPLETED, "No", 1001).changed());
    assertFalse(UndertakingService.briefing(data, other, Optional.empty(), 16).contains("go_to_point"));
    assertFalse(UndertakingService.briefing(data, RESIDENT, Optional.of(other), 16).contains("go_to_point"));
  }
  @Test void oldNarrativeMattersRemainExplicitlyUnverifiedHistory() {
    JsonObject root = new JsonObject(); var entries = new com.google.gson.JsonArray(); var old = new JsonObject();
    old.addProperty("id", new UUID(0, 3).toString()); old.addProperty("valence", "POSITIVE"); old.addProperty("state", "OPEN");
    old.addProperty("origin", "PLAYER"); old.addProperty("summary", "A previous conversation promise");
    old.addProperty("opened_day", 1); old.addProperty("updated_day", 1); entries.add(old); root.add("undertakings", entries);
    var data = UndertakingData.CODEC.parse(JsonOps.INSTANCE, root).getOrThrow();
    var result = UndertakingService.archiveNarrative(data, data.undertakings().getFirst().id(), 1000);
    assertTrue(result.changed()); assertEquals(State.ABANDONED, result.affected().orElseThrow().state());
    assertTrue(UndertakingService.briefing(roundTrip(result.data()), RESIDENT, Optional.empty(), 16).contains("UNVERIFIED NARRATIVE"));
  }
  @Test void capacityNeverSilentlyEvictsUnfinishedPromises() {
    var store = new AtomicReference<>(UndertakingData.EMPTY); var state = state(store, RESIDENT);
    for (int index = 0; index < UndertakingData.MAX_OPEN; index++) {
      assertTrue(state.accept(offer(state, new UUID(1, index), index * 10), ResidentActionState.Action.WAIT, index * 10 + 1).accepted());
      state.finish(Phase.INTERRUPTED, "Paused");
    }
    var before = store.get();
    assertFalse(state.accept(offer(state, new UUID(2, 1), 200), ResidentActionState.Action.WAIT, 201).accepted());
    assertEquals(before, store.get()); assertEquals(16, store.get().allOpen().size());
  }
  @Test void aRejectedOldReplyDoesNotInvalidateANewerOutstandingOffer() {
    var store = new AtomicReference<>(UndertakingData.EMPTY); var state = state(store, RESIDENT);
    var old = offer(state, REQUESTER, 0); var current = offer(state, REQUESTER, 1);
    state.rejected(old, ResidentActionState.Action.FOLLOW, "Stale", 2);
    assertTrue(state.accept(current, ResidentActionState.Action.WAIT, 3).accepted());
  }
  @Test void malformedSavedExecutionDoesNotAuthorizeAnInventedOrUnownedAction() {
    JsonObject json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
    JsonObject execution = json.getAsJsonArray("undertakings").get(0).getAsJsonObject().getAsJsonObject("execution");
    execution.addProperty("action", "teleport");
    assertTrue(UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
  }

  @Test void savedInstantActionsAndContradictoryLifecyclesCannotBecomeMovement() {
    for (String state : List.of("OPEN", "RESOLVED", "ABANDONED")) {
      var json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
      json.getAsJsonArray("undertakings").get(0).getAsJsonObject().addProperty("state", state);
      assertTrue(UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), state);
    }
    for (String phase : List.of("ACCEPTED", "UNDERWAY")) {
      var json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
      var execution = json.getAsJsonArray("undertakings").get(0).getAsJsonObject().getAsJsonObject("execution");
      execution.addProperty("action", "resume"); execution.addProperty("phase", phase); execution.remove("destination");
      assertTrue(UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), phase);
    }
  }

  @Test void anUnacceptedMissingPointCanBeRememberedButNeverStarted() {
    var store = new AtomicReference<>(UndertakingData.EMPTY); var state = state(store, RESIDENT);
    var missing = state.offer(REQUESTER, "Requester", "minecraft:overworld", null, POINT, 0);
    state.rejected(missing, ResidentActionState.Action.GO_TO_POINT, "No point supplied", 1);
    var matter = roundTrip(store.get()).undertakings().getFirst();
    assertEquals(Phase.FAILED, matter.execution().orElseThrow().phase()); assertEquals(State.ABANDONED, matter.state());
    assertTrue(state.current().isEmpty());
    var execution = matter.execution().orElseThrow().phase(Phase.REQUESTED);
    var requested = UndertakingService.requested(UndertakingData.EMPTY, execution, Optional.of(REQUESTER), 1);
    assertFalse(UndertakingService.transition(requested.data(), RESIDENT, requested.affected().orElseThrow().id(), Phase.ACCEPTED, "Agreed", 2).changed());
  }
}
