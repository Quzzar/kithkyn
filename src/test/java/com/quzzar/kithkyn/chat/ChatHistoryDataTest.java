package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class ChatHistoryDataTest {
  @Test void oldHistoryRemainsReadableWithoutInventingAMonotonicTime() {
    var old = JsonParser.parseString("{\"player\":\"Hello\",\"reply\":\"Hello\",\"day_time\":6000}");
    var decoded = ChatHistoryData.Exchange.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
    assertEquals(6000, decoded.dayTime()); assertEquals(-1, decoded.gameTime());
  }

  @Test void exchangesKeepTheirPhysicalClockAcrossSerialization() {
    var exchange = new ChatHistoryData.Exchange("Did it arrive?", "One item arrived.", 6000, 110);
    var saved = ChatHistoryData.Exchange.CODEC.encodeStart(JsonOps.INSTANCE, exchange).getOrThrow();
    assertEquals(exchange, ChatHistoryData.Exchange.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
  }
}
