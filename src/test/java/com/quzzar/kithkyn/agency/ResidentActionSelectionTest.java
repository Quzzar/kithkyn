package com.quzzar.kithkyn.agency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.agency.ResidentActionState.Action;

class ResidentActionSelectionTest {
  private static ResidentActionSelection parse(String json) {
    return ResidentActionSelection.parse(json);
  }

  @Test
  void ordinaryDialogueAndRefusalHaveNoAction() {
    var result = parse("{\"say\":\"I have work here.\"}");
    assertTrue(result.action().isEmpty());
    assertTrue(result.error().isEmpty());
  }

  @Test
  void anExplicitSupportedActionCanBeSelected() {
    assertEquals(Action.FOLLOW, parse("{\"say\":\"All right.\",\"action\":\"follow\"}").action().orElseThrow());
  }

  @Test
  void inventedVerbsDoNotBecomeMovementOrAttack() {
    var result = parse("{\"say\":\"Yes.\",\"action\":\"teleport\"}");
    assertTrue(result.action().isEmpty());
    assertTrue(result.error().isPresent());
  }

  @Test
  void coordinatesAndCodeCannotBePassedAsTheAction() {
    for (String value : new String[] {"null", "true", "3", "[]", "{\"x\":100,\"y\":64,\"z\":0}"}) {
      var result = parse("{\"say\":\"Yes.\",\"action\":" + value + "}");
      assertTrue(result.action().isEmpty());
      assertTrue(result.error().isPresent());
    }
  }

  @Test
  void malformedOrAmbiguousJsonNeverBecomesAnAction() {
    for (String raw : new String[] {
        "{say:'Yes',action:'follow'}", "{\"say\":\"Yes\",\"action\":\"follow\",}",
        "{\"say\":\"Yes\",\"action\":\"follow\"", "{\"action\":\"follow\",\"action\":\"wait\"}",
        "Here is my answer: {\"say\":\"Yes\",\"action\":\"follow\"}",
        "{\"action\":\"follow\"}{\"action\":\"wait\"}",
        "{\"action\":\"follow\",\"extra\":{\"unquoted\":value}}"}) {
      var result = parse(raw);
      assertTrue(result.action().isEmpty(), raw);
      assertTrue(result.error().isPresent(), raw);
    }
  }
}
