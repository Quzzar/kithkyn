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
  @Test void opinionsRequireActualCurrentSpeakerEvidenceAndConsistentPolarity() {
    String insult = "{\"say\":\"That was unkind.\",\"opinion\":{\"kind\":\"insult\",\"quote\":\"You're an idiot.\",\"delta\":-3}}";
    assertEquals(-3, PersonChatDispatcher.parseReply(insult, "You're an idiot.").opinionDelta());
    assertNull(PersonChatDispatcher.parseReply(insult, "Did you help?"));
    assertNull(PersonChatDispatcher.parseReply(insult.replace("-3", "3"), "You're an idiot."));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"No.\",\"opinion\":-3}", "Did you help?"));
    String apology = "{\"say\":\"Thank you.\",\"opinion\":{\"kind\":\"apology\",\"quote\":\"I'm sorry.\",\"delta\":2}}";
    assertEquals(2, PersonChatDispatcher.parseReply(apology, "I'm sorry.").opinionDelta());
  }

}
