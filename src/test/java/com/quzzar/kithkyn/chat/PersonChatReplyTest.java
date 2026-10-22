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
  @Test void residentsCanJudgeBargainingOrPromisesWithoutASocialCategoryOrPrescribedSign() {
    String bargain = "{\"say\":\"A fair offer.\",\"opinion\":{\"reason\":\"I respect a shrewd bargain.\",\"quote\":\"Two emeralds for the bread?\",\"delta\":3}}";
    assertEquals(3, PersonChatDispatcher.parseReply(bargain, "Two emeralds for the bread?").opinionDelta());
    assertEquals(-3, PersonChatDispatcher.parseReply(bargain.replace("\"delta\":3", "\"delta\":-3"),
        "Two emeralds for the bread?").opinionDelta(), "The parser does not decide what feelings a reason permits");
    String promise = "{\"say\":\"I'll remember that.\",\"opinion\":{\"reason\":\"I trust their promise after the help I witnessed.\",\"quote\":\"I'll return tomorrow.\",\"delta\":2}}";
    assertEquals(2, PersonChatDispatcher.parseReply(promise, "I'll return tomorrow.").opinionDelta());
    assertNull(PersonChatDispatcher.parseReply(bargain, "Did you help?"), "Attribution must still match the actual speaker");
    assertNull(PersonChatDispatcher.parseReply(bargain.replace("I respect a shrewd bargain.", ""), "Two emeralds for the bread?"));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"No.\",\"opinion\":-3}", "Did you help?"));
  }

}
