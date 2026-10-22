package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class PersonChatReplySchemaTest {
  @Test void localItemArgumentsAreStringsFromActualCarriedItems() {
    var properties = JsonParser.parseString(PersonChatReplySchema.build(List.of("minecraft:bread"), false))
        .getAsJsonObject().getAsJsonObject("properties");
    assertEquals("string", properties.getAsJsonObject("give").get("type").getAsString());
    assertEquals("[\"minecraft:bread\"]", properties.getAsJsonObject("give").get("enum").toString());
    assertFalse(properties.has("request"));
    assertNotNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":\"minecraft:bread\",\"give_count\":1}"));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":1}"));
  }
  @Test void emptyPocketsDoNotOfferAnItemToolAndPlanningRemainsExplicit() {
    var properties = JsonParser.parseString(PersonChatReplySchema.build(List.of(), true))
        .getAsJsonObject().getAsJsonObject("properties");
    assertFalse(properties.has("give"));
    assertFalse(properties.has("give_count"));
    assertTrue(properties.has("request"));
  }
}
