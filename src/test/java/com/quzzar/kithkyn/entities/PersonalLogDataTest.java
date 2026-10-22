package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PersonalLogDataTest {

  @Test
  void changingJobsClearsOperationalBlockersButPreservesMemories() {
    PersonalLogData log = PersonalLogData.EMPTY
        .withEntry(PersonalLogData.memory("I finished the old work.", 100L, 100L, Optional.empty()))
        .withBlocker("I cannot find suitable trees to fell.", 120L, 120L)
        .withBlocker("I cannot get to the brush that wants clearing.", 140L, 140L);

    PersonalLogData reassigned = log.withoutBlockers();

    assertTrue(reassigned.blockersNewestFirst().isEmpty());
    assertEquals(java.util.List.of("I finished the old work."),
        reassigned.memoriesNewestFirst().stream().map(PersonalLogData.Entry::text).toList());
  }

  @Test
  void requesterScopedLifecycleFactsShareMemoryBoundsAndReflectionWithoutLeakingToOtherSpeakers() {
    var alice = new java.util.UUID(0, 1); var bob = new java.util.UUID(0, 2);
    PersonalLogData log = PersonalLogData.EMPTY
        .withEntry(PersonalLogData.memory("Bob attacked the village.", 1, 1, Optional.of(bob)))
        .withEntry(PersonalLogData.undertaking("Bob's private request failed.", 2, 2, Optional.of(bob)))
        .withEntry(PersonalLogData.undertaking("Alice's private request is underway.", 3, 3, Optional.of(alice)))
        .withEntry(PersonalLogData.undertaking("I witnessed a threat die.", 4, 4, Optional.empty()));
    var roundTrip = PersonalLogData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
        PersonalLogData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, log).getOrThrow()).getOrThrow();
    assertEquals(java.util.List.of("I witnessed a threat die.", "Alice's private request is underway.", "Bob attacked the village."),
        roundTrip.memoriesFor(alice).stream().map(PersonalLogData.Entry::text).toList());
    assertTrue(roundTrip.unreflected().stream().anyMatch(entry -> entry.text().equals("Bob's private request failed.")));
    for (int index = 0; index < 20; index++) log = log.withEntry(PersonalLogData.undertaking("Phase " + index, 5, 5, Optional.of(alice)));
    assertEquals(PersonalLogData.MAX_MEMORIES, log.memoriesNewestFirst().size());
  }
}
