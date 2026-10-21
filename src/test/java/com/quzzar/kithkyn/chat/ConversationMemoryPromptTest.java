package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConversationMemoryPromptTest {

  @Test
  void worldStateComesFromSimulationRatherThanTheVillagersPreviousAnswer() {
    ConversationMemoryPrompt.Prompt prompt = ConversationMemoryPrompt.build(
        "Mara", "Steve", "We still need to finish the windmill. Steve has been curious about me.",
        List.of("What is the village building right now?"), "No recorded actions.");

    assertTrue(prompt.system().contains("never turn a question into a fact"));
    assertTrue(prompt.system().contains("facts always come fresh from the simulation"));
    assertTrue(prompt.user().contains("What is the village building right now?"));
    assertFalse(prompt.user().contains("windmill"));
  }
  @Test
  void aCompletionClaimCannotOverrideAnAuthoritativeFailedAttempt() {
    var prompt = ConversationMemoryPrompt.build("Mara", "Steve", "Steve asked me to walk there.",
        List.of("You already finished it!"), "FAILED: go_to_point. The route was blocked. A fresh decision is required.");
    assertTrue(prompt.user().contains("FAILED: go_to_point"));
    assertTrue(prompt.system().contains("Never upgrade a promise or a claim of completion into a verified outcome"));
  }
}
