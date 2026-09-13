package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class StorageStrainTest {

  @Test
  void anIdleKeeperCannotClearAnotherKeepersBlockedReport() {
    Village village = new Village("Storestead");
    UUID blocked = UUID.randomUUID();
    UUID idle = UUID.randomUUID();

    village.reportStorageStrain(blocked, true);
    village.reportStorageStrain(idle, false);

    assertTrue(village.isStorageStrained());
    village.reportStorageStrain(blocked, false);
    assertFalse(village.isStorageStrained());
  }

  @Test
  void eachBlockedKeeperOwnsOnlyItsOwnReport() {
    Village village = new Village("Storestead");
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    village.reportStorageStrain(first, true);
    village.reportStorageStrain(second, true);
    village.reportStorageStrain(first, false);

    assertTrue(village.isStorageStrained());
    village.reportStorageStrain(second, false);
    assertFalse(village.isStorageStrained());
  }
}
