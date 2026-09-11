package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

class RaisingTest {

  private static CompoundTag dead() {
    CompoundTag tag = new CompoundTag();
    tag.putString("id", "kithkyn:person");
    tag.putUUID("UUID", UUID.fromString("7f1d5a4e-2b3c-4d5e-8f90-123456789abc"));
    tag.putString("FirstName", "Fern");
    tag.putString("LastName", "Hammond");
    tag.putString("Gender", "FEMALE");
    tag.putString("Personality", "CHEERFUL");
    tag.putString("AgeStage", "ADULT");
    tag.putString("Kind", "LIVING");
    tag.putString("FirstParentUUID", UUID.randomUUID().toString());
    tag.putString("VillageUUID", "village-1");
    tag.putString("VillageName", "Tallowford");
    tag.putString("Occupation", "FARMER");
    tag.putString("Title", "Elder");
    tag.putString("MarriageStatus", "MARRIED");
    tag.putString("SpouseUUID", UUID.randomUUID().toString());
    tag.putBoolean("Raider", true);
    tag.putFloat("Health", 0.0F);
    tag.putShort("DeathTime", (short) 19);
    tag.put("Pos", new ListTag());
    tag.put("MainInventory", new ListTag());
    tag.put("ArmorItems", new ListTag());
    CompoundTag attachments = new CompoundTag();
    attachments.putString("kithkyn:persona", "a persona");
    tag.put("neoforge:attachments", attachments);
    CompoundTag stats = new CompoundTag();
    stats.putInt("Strength", 12);
    tag.put("StatBlock", stats);
    return tag;
  }

  @Test
  void whoTheyWereSurvives() {
    CompoundTag risen = Raising.prepare(dead());
    assertEquals("kithkyn:person", risen.getString("id"));
    assertEquals(UUID.fromString("7f1d5a4e-2b3c-4d5e-8f90-123456789abc"), risen.getUUID("UUID"));
    assertEquals("Fern", risen.getString("FirstName"));
    assertEquals("Hammond", risen.getString("LastName"));
    assertEquals("FEMALE", risen.getString("Gender"));
    assertEquals("CHEERFUL", risen.getString("Personality"));
    assertEquals("ADULT", risen.getString("AgeStage"));
    assertTrue(risen.contains("FirstParentUUID"));
    assertEquals(12, risen.getCompound("StatBlock").getInt("Strength"));
    assertEquals("a persona", risen.getCompound("neoforge:attachments").getString("kithkyn:persona"));
  }

  @Test
  void theOldLifeAndTheBodyStayInTheGrave() {
    CompoundTag risen = Raising.prepare(dead());
    for (String gone : new String[] {"VillageUUID", "VillageName", "Title", "SpouseUUID", "Raider",
        "Health", "DeathTime", "Pos", "MainInventory", "ArmorItems"}) {
      assertFalse(risen.contains(gone), gone + " should stay in the grave");
    }
    assertEquals("UNDEAD", risen.getString("Kind"));
    assertEquals("SINGLE", risen.getString("MarriageStatus"));
    assertEquals("WANDERER", risen.getString("Occupation"));
  }

  @Test
  void preparingLeavesTheRecordItself() {
    CompoundTag record = dead();
    CompoundTag before = record.copy();
    Raising.prepare(record);
    assertEquals(before, record);
  }

  @Test
  void belongingsAreLeftWhereTheyFell() {
    CompoundTag kept = Raising.withoutBelongings(dead());
    assertFalse(kept.contains("MainInventory"));
    assertFalse(kept.contains("ArmorItems"));
    assertEquals("Tallowford", kept.getString("VillageName"), "everything else waits for the raising");
  }
}
