package com.quzzar.kithkyn.chat;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
class PersonChatReplyTest {
  @Test void rejectsTheSupersededUndertakingToolAndMalformedPartialOutput() {
    for (String raw : List.of("{\"say\":\"Done\",\"undertaking\":{\"op\":\"resolve\"}}",
        "prefix {\"say\":\"Done\"}", "{\"say\":\"Done\"} trailing", "{\"say\":\"Done\",\"say\":\"No\"}",
        "{\"say\":\"Done\",\"fight\":\"true\"}", "{\"say\":\"Done\",\"give_count\":999}",
        "{\"say\":\"Done\",\"action\":\"teleport\"}", "{\"say\":\"I will follow.\",\"action\":\"follow\"}",
        "{\"say\":\"Done\",", "{}")) assertNull(PersonChatDispatcher.parseReply(raw), raw);
    assertNotNull(PersonChatDispatcher.parseReply("{\"say\":\"I could not reach it.\"}"));
  }
}
