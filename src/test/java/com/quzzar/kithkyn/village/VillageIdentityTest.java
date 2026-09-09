package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;

import org.junit.jupiter.api.Test;

class VillageIdentityTest {

  @Test
  void foundingChoosesTwoDifferentMinecraftColorsAndABanner() {
    VillageIdentity identity = VillageIdentity.generate("Willowfield", RandomSource.create(42));

    assertNotEquals(identity.primaryColor(), identity.secondaryColor());
    assertFalse(identity.bannerLayers().isEmpty());
    assertTrue(identity.bannerLayers().stream()
        .anyMatch(layer -> layer.color() == VillageIdentity.ColorRole.SECONDARY));
  }

  @Test
  void legacyNameGetsTheSameIdentityEveryTime() {
    assertEquals(VillageIdentity.legacy("Oldhaven"), VillageIdentity.legacy("Oldhaven"));
  }

  @Test
  void codecReadsOldNameStringsAndWritesTheCompleteIdentity() {
    VillageIdentity legacy = VillageIdentity.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("Oldhaven"))
        .getOrThrow();
    var encoded = VillageIdentity.CODEC.encodeStart(JsonOps.INSTANCE, legacy).getOrThrow();

    assertEquals("Oldhaven", legacy.name());
    assertTrue(encoded.isJsonObject());
    assertEquals(legacy, VillageIdentity.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
  }

  @Test
  void villageSaveMigratesTheOldNameSlotWithoutAddingASeventeenthField() {
    Village original = new Village(VillageIdentity.generate("Newcrest", RandomSource.create(9)));
    CompoundTag currentSave = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
    Village restored = Village.CODEC.parse(NbtOps.INSTANCE, currentSave).getOrThrow();

    assertEquals(original.getIdentity(), restored.getIdentity());
    assertTrue(currentSave.get("name") instanceof CompoundTag);

    currentSave.putString("name", "Oldhaven");
    Village migrated = Village.CODEC.parse(NbtOps.INSTANCE, currentSave).getOrThrow();
    assertEquals(VillageIdentity.legacy("Oldhaven"), migrated.getIdentity());
  }
}
