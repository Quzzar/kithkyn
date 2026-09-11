package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.entities.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

class GraveyardTest {

  private static Graveyard.Entry entry(String name, long diedAt, BlockPos where, String village) {
    CompoundTag person = new CompoundTag();
    person.putUUID("UUID", UUID.randomUUID());
    person.putString("FirstName", name);
    return new Graveyard.Entry(name, diedAt, where.asLong(), village, name + " was slain", person);
  }

  @Test
  void theVillagesOwnDeadRiseFirstThenTheNearestThenTheMostRecent() {
    BlockPos center = new BlockPos(0, 64, 0);
    Graveyard.Entry farRecent = entry("far recent", 900L, new BlockPos(400, 64, 0), "Elsewhere");
    Graveyard.Entry nearOld = entry("near old", 100L, new BlockPos(40, 64, 0), "Elsewhere");
    Graveyard.Entry nearRecent = entry("near recent", 800L, new BlockPos(40, 64, 0), "Elsewhere");
    Graveyard.Entry ownOld = entry("own old", 50L, new BlockPos(600, 64, 0), "Tallowford");
    List<Graveyard.Entry> ranked = Graveyard.ranked(List.of(farRecent, nearOld, nearRecent, ownOld), "Tallowford", center);
    assertEquals(List.of(ownOld, nearRecent, nearOld, farRecent), ranked);
  }

  @Test
  void anEntrySurvivesTheSave() {
    Graveyard.Entry entry = entry("Fern Hammond", 1234L, new BlockPos(3, 64, 40), "Tallowford");
    var encoded = Graveyard.Entry.CODEC.encodeStart(NbtOps.INSTANCE, entry).getOrThrow();
    Graveyard.Entry decoded = Graveyard.Entry.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    assertEquals(entry, decoded);
  }

  @Test
  void theRoadTellsKindsApart() {
    CompoundTag living = new CompoundTag();
    CompoundTag undead = new CompoundTag();
    undead.putString("Kind", "UNDEAD");
    assertEquals(Kind.LIVING, WandererPool.kindOf(living));
    assertEquals(Kind.UNDEAD, WandererPool.kindOf(undead));
  }
}
