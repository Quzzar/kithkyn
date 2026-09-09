package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;

import org.junit.jupiter.api.Test;

class GolemRosterTest {
  @Test
  void auxiliaryGuardSurvivesVillageSaveWithoutTakingPopulationHousingOrJobs() {
    Village village = new Village("Birchhaven");
    UUID golem = UUID.randomUUID();
    village.getGolems().remember(golem, "Bastion", ChunkPos.asLong(12, -4));

    CompoundTag saved = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    Village restored = Village.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();

    assertEquals(village.getGolems().members(), restored.getGolems().members());
    assertEquals(village.getIdentity(), restored.getIdentity());
    assertTrue(restored.getPopulation().isEmpty());
    assertTrue(restored.getJobAssignmentsView().isEmpty());
    assertEquals(0, restored.getTotalBeds());
  }

  @Test
  void oldSavesLoadWithIndependentEmptyRosters() {
    CompoundTag saved = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, new Village("Oldhaven"))
        .getOrThrow();
    saved.remove("golems");
    Village first = Village.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    Village second = Village.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    first.getGolems().remember(UUID.randomUUID(), "Flint", 0L);

    assertTrue(second.getGolems().members().isEmpty());
  }

  @Test
  void repeatedObservationDoesNotDuplicateAndDeathRemovesOnlyThatGolem() {
    GolemRoster roster = new GolemRoster();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    roster.remember(first, "Flint", 0L);
    roster.remember(first, "Flint", 12L);
    roster.remember(second, "Anvil", 13L);
    assertEquals(2, roster.members().size());
    assertEquals(12L, roster.members().get(first).chunk());
    roster.remove(first);
    assertEquals(1, roster.members().size());
    assertTrue(roster.members().containsKey(second));
  }
}
