package com.quzzar.kithkyn.savedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

class VillageAuditPersistenceTest {

  @Test
  void monitoredAllowlistSurvivesWorldSaveAndReload() {
    VillageManagerSaveData data = new VillageManagerSaveData();
    assertTrue(data.setVillageAudited("village-b", true));
    assertTrue(data.setVillageAudited("village-a", true));
    assertFalse(data.setVillageAudited("village-a", true));

    CompoundTag saved = data.save(new CompoundTag(), null);
    VillageManagerSaveData restored = VillageManagerSaveData.load(saved, null);

    assertEquals(java.util.Set.of("village-a", "village-b"), restored.getAuditedVillageIds());
    assertTrue(restored.isVillageAudited("village-a"));
    assertEquals(2, restored.clearAuditedVillages());
    assertTrue(restored.getAuditedVillageIds().isEmpty());
  }

  @Test
  void oldWorldWithoutAuditStateStartsUnmonitored() {
    VillageManagerSaveData data = new VillageManagerSaveData();
    CompoundTag saved = data.save(new CompoundTag(), null);

    assertTrue(VillageManagerSaveData.load(saved, null).getAuditedVillageIds().isEmpty());
  }
}
