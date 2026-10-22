package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class PersonChatReplySchemaTest {
  @Test void localItemArgumentsAreStringsFromActualCarriedItems() {
    var properties = JsonParser.parseString(PersonChatReplySchema.build(List.of("minecraft:bread"), false, "Could I have bread?"))
        .getAsJsonObject().getAsJsonArray("oneOf").get(3).getAsJsonObject().getAsJsonObject("properties");
    assertEquals("opinion", properties.keySet().iterator().next(), "Judge speaker evidence before generating the resident response");
    assertEquals("string", properties.getAsJsonObject("give").get("type").getAsString());
    assertEquals("[\"minecraft:bread\"]", properties.getAsJsonObject("give").get("enum").toString());
    assertFalse(properties.has("request"));
    var alternatives = JsonParser.parseString(PersonChatReplySchema.build(List.of("minecraft:bread"), false, "Could I have bread?"))
        .getAsJsonObject().getAsJsonArray("oneOf");
    assertFalse(alternatives.get(0).getAsJsonObject().getAsJsonObject("properties").has("give_count"));
    for (var element : alternatives) {
      var branch = element.getAsJsonObject();
      boolean opinionPresent = branch.getAsJsonObject("properties").has("opinion");
      if (opinionPresent) assertEquals("opinion", branch.getAsJsonArray("required").get(0).getAsString());
      if (branch.getAsJsonObject("properties").has("give_count"))
        assertTrue(branch.getAsJsonArray("required").toString().contains("give"));
    }
    assertNotNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":\"minecraft:bread\",\"give_count\":1}"));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":1}"));
  }
  @Test void emptyPocketsDoNotOfferAnItemToolAndPlanningRemainsExplicit() {
    var properties = JsonParser.parseString(PersonChatReplySchema.build(List.of(), true, "Could you build a farm?"))
        .getAsJsonObject().getAsJsonArray("oneOf").get(0).getAsJsonObject().getAsJsonObject("properties");
    assertFalse(properties.has("give"));
    assertFalse(properties.has("give_count"));
    assertTrue(properties.has("request"));
  }
}
