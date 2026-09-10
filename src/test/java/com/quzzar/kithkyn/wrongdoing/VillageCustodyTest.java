package com.quzzar.kithkyn.wrongdoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class VillageCustodyTest {
  @Test
  void absorptionMustBeExhaustedBeforeAHitCountsAsLethal() {
    assertFalse(VillageCustody.isLethal(5, 4, 2));
    assertTrue(VillageCustody.isLethal(6, 4, 2));
    assertFalse(VillageCustody.isLethal(2, 4, 0));
    assertFalse(VillageCustody.isLethal(0, 0, 0));
  }

  @Test
  void theCeasefireBelongsOnlyToTheArrestingVillageAndEndsAfterGrace() {
    var sentence = sentence();
    assertTrue(sentence.protects("village-a", 500));
    assertFalse(sentence.protects("village-b", 500));
    var released = sentence.released(7000);
    assertTrue(released.protects("village-a", 7599));
    assertFalse(released.protects("village-a", 7600));
    assertFalse(released.occupies("village-a", "castle", 7001));
  }

  @Test
  void anOfflineExpiredSentenceFreesTheCellButRetainsReleaseOnLogin() {
    var sentence = sentence();
    assertTrue(sentence.occupies("village-a", "castle", 6099));
    assertFalse(sentence.occupies("village-a", "castle", 6100));
    assertTrue(sentence.jailed());
    assertEquals(6000, sentence.releaseAt() - sentence.startedAt());
  }

  @Test
  void jumpingWithinTheCellIsAllowedButCrossingItsWallEndsCustody() {
    BlockPos cell = new BlockPos(6, 11, 18);
    assertFalse(VillageCustody.escaped(new Vec3(6.5, 12.2, 18.5), cell));
    assertTrue(VillageCustody.escaped(new Vec3(8.5, 11, 18.5), cell));
    assertTrue(VillageCustody.escaped(new Vec3(6.5, 9, 18.5), cell));
  }

  @Test
  void savedCustodyRetainsDeadlineCellAndAlreadyAnnouncedMinute() {
    UUID player = UUID.randomUUID();
    CompoundTag entry = new CompoundTag();
    entry.putUUID("player", player);
    entry.putString("village", "village-a");
    entry.putString("castle", "castle");
    entry.putString("dimension", "minecraft:overworld");
    entry.putLong("cell", new BlockPos(6, 11, 18).asLong());
    entry.putLong("release", new BlockPos(8, 11, 17).asLong());
    entry.putLong("started", 100);
    entry.putLong("until", 6100);
    entry.putLong("grace", 0);
    entry.putInt("announced_minutes", 3);
    ListTag list = new ListTag();
    list.add(entry);
    CompoundTag saved = new CompoundTag();
    saved.put("sentences", list);

    VillageCustody ledger = VillageCustody.load(saved, null);
    var reloaded = VillageCustody.load(ledger.save(new CompoundTag(), null), null).getSentence(player);
    assertEquals(6100, reloaded.releaseAt());
    assertEquals(new BlockPos(6, 11, 18), reloaded.cell());
    assertEquals(3, reloaded.announcedMinutes());
    assertTrue(reloaded.protects("village-a", 6200), "Release is processed when the offline player returns");
  }

  private static VillageCustody.Sentence sentence() {
    return new VillageCustody.Sentence(UUID.randomUUID(), "village-a", "castle", "minecraft:overworld",
        new BlockPos(6, 11, 18), new BlockPos(8, 11, 17), 100, 6100, 0, 5, null);
  }
}
