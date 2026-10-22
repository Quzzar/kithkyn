package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ConversationMemoryPromptTest {
  @Test void questionsAndClaimsRemainExactQuotesRatherThanAutobiographicalFacts() {
    var prompt = ConversationMemoryPrompt.build("Mara", "Steve", List.of(),
        List.of("Did you kill that zombie?", "You already finished it!"));
    var selected = prompt.select("{\"keep\":[1,2]}").orElseThrow();
    var memory = ChatSummaryData.EMPTY.withQuotes(new UUID(0, 1), selected);
    assertTrue(memory.with(new UUID(0, 1)).contains("Did you kill that zombie?"));
    assertTrue(memory.with(new UUID(0, 1)).contains("The counterpart said:"));
    assertFalse(memory.with(new UUID(0, 1)).contains("I killed"));
    assertEquals(selected, ChatSummaryData.CODEC.parse(JsonOps.INSTANCE,
        ChatSummaryData.CODEC.encodeStart(JsonOps.INSTANCE, memory).getOrThrow()).getOrThrow().quotesWith(new UUID(0, 1)));
  }

  @Test void rejectsInventedReferencesProseDuplicatesAndMalformedSelections() {
    var prompt = ConversationMemoryPrompt.build("Mara", "Steve", List.of(), List.of("Did you help?"));
    for (String raw : List.of("I killed it.", "{\"keep\":[2]}", "{\"keep\":[1,1]}", "{\"keep\":[1.5]}",
        "{\"keep\":[\"1\"]}", "{\"keep\":[1],\"memory\":\"I killed it\"}", "{\"keep\":[0]}"))
      assertTrue(prompt.select(raw).isEmpty(), raw);
    assertEquals(List.of(), prompt.select("{\"keep\":[]}").orElseThrow());
  }

  @Test void oldModelProseIsReadOnlyUnverifiedHistoryAndNeverBecomesAQuotedSource() {
    UUID id = new UUID(0, 1);
    var old = ChatSummaryData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
        "{\"" + id + "\":\"I killed the zombie. We still need to build a windmill.\"}")).getOrThrow();
    assertTrue(old.with(id).startsWith("Unverified earlier"));
    assertFalse(old.with(id).contains("windmill"));
    assertTrue(old.quotesWith(id).isEmpty());
    var replacement = old.withQuotes(id, List.of("Did you kill that zombie?"));
    assertFalse(replacement.with(id).contains("I killed"));
  }

  @Test void consolidatingQuotesRetainsSourceOrderAndDeduplicatesRepeatedLines() {
    var prompt = ConversationMemoryPrompt.build("Mara", "Steve", List.of("My name is Steve."),
        List.of("My name is Steve.", "Did you help?"));
    assertEquals(List.of("My name is Steve.", "Did you help?"), prompt.select("{\"keep\":[2,1]}").orElseThrow());
  }

  @Test void quotedBuildingRequestsAreRetainedWithoutBecomingCurrentWorldState() {
    var data = ChatSummaryData.EMPTY.withQuotes(new UUID(0, 1), List.of("Could you build a windmill?"));
    assertTrue(data.with(new UUID(0, 1)).contains("Could you build a windmill?"));
    assertThrows(IllegalArgumentException.class, () -> data.withQuotes(new UUID(0, 1), List.of("x".repeat(2049))));
  }
}
