package com.quzzar.kithkyn.raids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class UndeadRaidTest {

  @Test
  void theCountdownIsHalfAMinuteUnlessSkipped() {
    UndeadRaid raid = UndeadRaid.begin("the dead of Barrowdown", null, 3, 1000L, false);
    assertFalse(raid.countdownOver(1000L + UndeadRaid.COUNTDOWN_TICKS - 1));
    assertTrue(raid.countdownOver(1000L + UndeadRaid.COUNTDOWN_TICKS));
    assertTrue(UndeadRaid.begin("the restless dead", null, 3, 1000L, true).countdownOver(1000L));
  }

  @Test
  void aRaidSurvivesTheSaveWithItsDeadAndItsDefenders() {
    UUID culprit = UUID.randomUUID();
    UUID defender = UUID.randomUUID();
    UUID raider = UUID.randomUUID();
    UndeadRaid raid = UndeadRaid.begin("the dead of Barrowdown", culprit, 4, 500L, false);
    raid.raiderDied(raider, defender);
    raid.raiderDied(UUID.randomUUID(), defender);
    raid.raiderDied(UUID.randomUUID(), null);

    CompoundTag tag = raid.save();
    UndeadRaid loaded = UndeadRaid.load(tag);

    assertEquals("the dead of Barrowdown", loaded.sourceName());
    assertEquals(culprit, loaded.culprit());
    assertEquals(4, loaded.waves());
    assertEquals(UndeadRaid.Phase.COMING, loaded.phase());
    assertEquals(0, loaded.wave());
    assertEquals(500L, loaded.phaseAt());
    assertEquals(2, loaded.kills().get(defender));
    assertTrue(loaded.raiders().isEmpty());
    assertEquals(tag, loaded.save());
  }

  @Test
  void theNamelessDeadHaveNoCulprit() {
    UndeadRaid loaded = UndeadRaid.load(UndeadRaid.begin(UndeadRaids.NAMELESS_DEAD, null, 2, 0L, true).save());
    assertNull(loaded.culprit());
    assertEquals("The restless dead", UndeadRaid.capitalize(loaded.sourceName()));
  }
}
