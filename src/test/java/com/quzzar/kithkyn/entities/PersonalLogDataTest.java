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
}
