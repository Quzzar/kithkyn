package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import com.quzzar.kithkyn.entities.UndertakingData.*;
import org.junit.jupiter.api.Test;

class UndertakingServiceTest {
  @Test void anEmptyOutcomeHistoryDoesNotImplyAnObservedActionWasPerformed() {
    String briefing = UndertakingService.briefing(UndertakingData.EMPTY, new UUID(0, 1), Optional.empty(), 8);
    assertTrue(briefing.contains("no recorded optional reaction outcomes"));
    assertTrue(briefing.contains("Observing an event does not mean you acted on it"));
  }
  private static final UUID RESIDENT = new UUID(0, 101), SUBJECT = new UUID(0, 202);
  private static final Duty DUTY = new Duty("WANDERER", "ADULT", "", Optional.empty(), -1, false, false);
  private static final Position POINT = new Position(8.5, 64, .5);
  private static Execution execution(String action, long revision) {
    return new Execution(RESIDENT, Lane.REACTION, action, Phase.REQUESTED, "minecraft:overworld",
        Optional.of(POINT), Optional.of(SUBJECT), revision, 0, 200, DUTY);
  }
  private static UndertakingData roundTrip(UndertakingData data) {
    return UndertakingData.CODEC.parse(JsonOps.INSTANCE, UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
  }
  private static UndertakingData begun() {
    var requested = UndertakingService.requested(UndertakingData.EMPTY, execution("investigate", 1), Optional.empty(), 1000);
    UUID id = requested.affected().orElseThrow().id();
    var accepted = UndertakingService.transition(requested.data(), RESIDENT, id, Phase.ACCEPTED, "Agreed", 1000);
    return UndertakingService.transition(accepted.data(), RESIDENT, id, Phase.UNDERWAY, "Native navigation started", 1000).data();
  }
  @Test void oneSavedStorePreservesTheActualReactionAndItsOwner() {
    var data = roundTrip(begun()); var matter = data.undertakings().getFirst();
    assertEquals(POINT, matter.execution().orElseThrow().destination().orElseThrow());
    assertEquals(RESIDENT, matter.execution().orElseThrow().resident());
    assertEquals(Phase.UNDERWAY, matter.execution().orElseThrow().phase());
    assertEquals(3, matter.steps().size());
    assertFalse(matter.steps().stream().anyMatch(step -> step.text().contains("COMPLETED")));
  }
  @Test void failedOrInterruptedReactionsCannotBeRestartedOrCompletedByDialogue() {
    for (Phase phase : List.of(Phase.FAILED, Phase.INTERRUPTED)) {
      var begun = begun(); UUID id = begun.undertakings().getFirst().id();
      var stopped = roundTrip(UndertakingService.transition(begun, RESIDENT, id, phase, "The route is blocked", 1001).data());
      assertFalse(UndertakingService.transition(stopped, RESIDENT, id, Phase.COMPLETED, "I said it was done", 1002).changed());
      assertFalse(UndertakingService.transition(stopped, RESIDENT, id, Phase.ACCEPTED, "Try again", 1002).changed());
      assertTrue(UndertakingService.briefing(stopped, RESIDENT, Optional.empty(), 16).contains(phase + ": investigate"));
      var expired = UndertakingService.transition(stopped, RESIDENT, id, Phase.EXPIRED, "Lifetime ended", 1003);
      assertEquals(State.ABANDONED, expired.affected().orElseThrow().state());
      assertTrue(expired.detail().contains(phase.name()));
    }
  }
  @Test void onlyVerifiedLiveExecutionCanRecordCompletion() {
    var data = begun(); UUID id = data.undertakings().getFirst().id();
    var completed = roundTrip(UndertakingService.transition(data, RESIDENT, id, Phase.COMPLETED, "Reached the injured resident; no treatment performed", 1001).data());
    assertEquals(State.RESOLVED, completed.byId(id).orElseThrow().state());
    assertTrue(UndertakingService.briefing(completed, RESIDENT, Optional.empty(), 16).contains("COMPLETED: investigate"));
    assertFalse(UndertakingService.transition(completed, RESIDENT, id, Phase.UNDERWAY, "Restart", 1002).changed());
  }
  @Test void anotherResidentCannotReadOrChangeAnOwnedReaction() {
    var data = begun(); var matter = data.undertakings().getFirst(); UUID other = new UUID(0, 999);
    assertFalse(UndertakingService.transition(data, other, matter.id(), Phase.COMPLETED, "No", 1001).changed());
    assertFalse(UndertakingService.briefing(data, other, Optional.empty(), 16).contains("investigate"));
  }
  @Test void oldNarrativeMattersRemainScopedUnverifiedHistory() {
    JsonObject root = new JsonObject(); var entries = new com.google.gson.JsonArray(); var old = new JsonObject();
    old.addProperty("id", new UUID(0, 3).toString()); old.addProperty("valence", "POSITIVE"); old.addProperty("state", "OPEN");
    old.addProperty("origin", "PLAYER"); old.addProperty("summary", "A previous conversation promise"); old.addProperty("with", SUBJECT.toString());
    old.addProperty("opened_day", 1); old.addProperty("updated_day", 1); entries.add(old); root.add("undertakings", entries);
    var data = UndertakingData.CODEC.parse(JsonOps.INSTANCE, root).getOrThrow();
    var result = UndertakingService.archiveNarrative(data, data.undertakings().getFirst().id(), 1000);
    assertEquals(State.ABANDONED, result.affected().orElseThrow().state());
    assertTrue(UndertakingService.briefing(roundTrip(result.data()), RESIDENT, Optional.of(SUBJECT), 16).contains("UNVERIFIED NARRATIVE"));
    assertFalse(UndertakingService.briefing(result.data(), RESIDENT, Optional.of(new UUID(0, 999)), 16).contains("previous conversation promise"));
  }
  @Test void capacityNeverSilentlyEvictsUnfinishedMatters() {
    UndertakingData data = UndertakingData.EMPTY;
    for (int index = 0; index < UndertakingData.MAX_OPEN; index++) {
      var requested = UndertakingService.requested(data, execution("warn", index + 1), Optional.empty(), 1000);
      assertTrue(requested.changed()); data = requested.data();
    }
    var rejected = UndertakingService.requested(data, execution("warn", 17), Optional.empty(), 1001);
    assertFalse(rejected.changed()); assertEquals(data, rejected.data()); assertEquals(16, data.allOpen().size());
  }
  @Test void malformedExecutionsAndContradictoryLifecycleStatesNeverDecode() {
    for (String state : List.of("OPEN", "RESOLVED", "ABANDONED")) {
      var json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
      json.getAsJsonArray("undertakings").get(0).getAsJsonObject().addProperty("state", state);
      assertTrue(UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), state);
    }
    for (String action : List.of("teleport", "continue", "follow")) {
      var json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
      json.getAsJsonArray("undertakings").get(0).getAsJsonObject().getAsJsonObject("execution").addProperty("action", action);
      assertTrue(UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), action);
    }
  }
  @Test void retiredCompanionRecordsCanOnlyBeClosedAndNeverIssuedAgain() {
    var json = UndertakingData.CODEC.encodeStart(JsonOps.INSTANCE, begun()).getOrThrow().getAsJsonObject();
    var matterJson = json.getAsJsonArray("undertakings").get(0).getAsJsonObject();
    matterJson.addProperty("with", SUBJECT.toString()); matterJson.addProperty("summary", "An earlier follow request");
    var execution = matterJson.getAsJsonObject("execution"); execution.addProperty("lane", "PLAYER_REQUEST"); execution.addProperty("action", "follow");
    var data = UndertakingData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(); var matter = data.undertakings().getFirst();
    assertFalse(UndertakingService.requested(UndertakingData.EMPTY, matter.execution().orElseThrow().phase(Phase.REQUESTED), Optional.of(SUBJECT), 1001).changed());
    assertFalse(UndertakingService.transition(data, RESIDENT, matter.id(), Phase.COMPLETED, "Done", 1001).changed());
    var closed = UndertakingService.transition(data, RESIDENT, matter.id(), Phase.CANCELLED, "Companion commands were removed", 1001);
    assertEquals(State.ABANDONED, roundTrip(closed.data()).byId(matter.id()).orElseThrow().state());
  }
}
