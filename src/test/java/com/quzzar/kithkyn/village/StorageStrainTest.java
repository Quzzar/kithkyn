package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class StorageStrainTest {

  @Test
  void storageIsSaturatedOnlyWhenEveryObservedSlotIsOccupied() {
    SimpleContainer storehouse = new SimpleContainer(3);
    storehouse.setItem(0, new ItemStack(Items.WHEAT, 64));
    storehouse.setItem(1, new ItemStack(Items.COBBLESTONE, 64));

    assertFalse(VillageBrain.allObservedStorageSlotsOccupied(List.of(storehouse)));
    VillageBrain.StorageOccupancy pressure = VillageBrain.StorageOccupancy.capture(List.of(storehouse));
    assertEquals(2, pressure.occupiedSlots());
    assertEquals(3, pressure.totalSlots());
    assertEquals(2.0D / 3.0D, pressure.fraction());
    storehouse.setItem(2, new ItemStack(Items.OAK_LOG, 64));
    assertTrue(VillageBrain.allObservedStorageSlotsOccupied(List.of(storehouse)));
    assertFalse(VillageBrain.allObservedStorageSlotsOccupied(List.of()));
  }

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
