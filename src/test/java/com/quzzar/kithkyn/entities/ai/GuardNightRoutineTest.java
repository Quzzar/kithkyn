package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GuardNightRoutineTest {

  private static final UUID GUARD = UUID.fromString("3d362f8e-96a3-4a43-9f34-0ffbd9f3b3b7");

  @Test
  void castleRoundsAndJailWatchKeepTheSameSleepingNights() {
    for (long day = 0; day < 1_000; day++) {
      long night = day * 24_000L + 18_000L;
      GuardNightRoutine ordinary = GuardNightRoutine.choose(GUARD, night, true, false, true);
      GuardNightRoutine sentry = GuardNightRoutine.choose(GUARD, night, true, false, true, true, false);
      GuardNightRoutine jailer = GuardNightRoutine.choose(GUARD, night, true, false, true, false, true);
      assertEquals(ordinary == GuardNightRoutine.SLEEP ? GuardNightRoutine.SLEEP : GuardNightRoutine.PATROL, sentry);
      assertEquals(ordinary == GuardNightRoutine.SLEEP ? GuardNightRoutine.SLEEP : GuardNightRoutine.POST, jailer);
      assertEquals(GuardNightRoutine.PATROL,
          GuardNightRoutine.choose(GUARD, day * 24_000L, false, false, true, true, false));
      assertEquals(GuardNightRoutine.POST,
          GuardNightRoutine.choose(GUARD, day * 24_000L, false, false, true, false, true));
    }
  }

  @Test
  void aWholeNightAndReloadedIdentityKeepTheSameChoice() {
    for (long day = 0; day < 40; day++) {
      GuardNightRoutine expected = GuardNightRoutine.choose(GUARD, day * 24_000L + 13_000L,
          true, false, true);
      UUID restored = UUID.fromString(GUARD.toString());
      for (long clock = 13_000L; clock < 24_000L; clock += 100) {
        assertEquals(expected, GuardNightRoutine.choose(restored, day * 24_000L + clock,
            true, false, true));
      }
    }
  }

  @Test
  void dawnRestoresPostsAndCaptainsKeepTheirPatrol() {
    for (long day = 0; day < 100; day++) {
      assertEquals(GuardNightRoutine.POST,
          GuardNightRoutine.choose(GUARD, day * 24_000L, false, false, true));
      assertEquals(GuardNightRoutine.PATROL,
          GuardNightRoutine.choose(GUARD, day * 24_000L, false, true, false));
      assertEquals(GuardNightRoutine.PATROL,
          GuardNightRoutine.choose(GUARD, day * 24_000L, false, false, false));
    }
  }

  @Test
  void sleepIsTwentyPercentAndPatrolIsThirtyPercentOfTheAwakeNights() {
    int nights = 100_000;
    int sleeping = 0;
    int patrolling = 0;
    for (long day = 0; day < nights; day++) {
      GuardNightRoutine routine = GuardNightRoutine.choose(GUARD, day * 24_000L + 18_000L,
          true, false, true);
      if (routine == GuardNightRoutine.SLEEP) sleeping++;
      if (routine == GuardNightRoutine.PATROL) patrolling++;
    }
    assertEquals(0.20D, sleeping / (double) nights, 0.005D);
    assertEquals(0.30D, patrolling / (double) (nights - sleeping), 0.005D);
  }

  @Test
  void captainSleepsOnTheSameNightsAndPatrolsEveryAwakeNight() {
    EnumSet<GuardNightRoutine> seen = EnumSet.noneOf(GuardNightRoutine.class);
    for (long day = 0; day < 100; day++) {
      long time = day * 24_000L + 18_000L;
      GuardNightRoutine captain = GuardNightRoutine.choose(GUARD, time, true, true, false);
      GuardNightRoutine posted = GuardNightRoutine.choose(GUARD, time, true, false, true);
      seen.add(captain);
      assertEquals(posted == GuardNightRoutine.SLEEP, captain == GuardNightRoutine.SLEEP);
      assertFalse(captain == GuardNightRoutine.POST);
    }
    assertEquals(EnumSet.of(GuardNightRoutine.SLEEP, GuardNightRoutine.PATROL), seen);
  }

  @Test
  void choicesVaryAcrossPeopleAsWellAsNights() {
    EnumSet<GuardNightRoutine> seen = EnumSet.noneOf(GuardNightRoutine.class);
    for (int index = 0; index < 100; index++) {
      UUID person = new UUID(GUARD.getMostSignificantBits(), GUARD.getLeastSignificantBits() + index);
      seen.add(GuardNightRoutine.choose(person, 18_000L, true, false, true));
    }
    assertTrue(seen.containsAll(EnumSet.allOf(GuardNightRoutine.class)));
  }
}
