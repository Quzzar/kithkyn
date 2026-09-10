package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Rotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JobClaimingTest {

  @AfterEach
  void clearBuildingDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void aptitudeRebalancingOnlyRunsAroundMidnight() {
    assertFalse(JobClaiming.isMidnightRebalanceTime(12_000L));
    assertFalse(JobClaiming.isMidnightRebalanceTime(16_999L));
    assertTrue(JobClaiming.isMidnightRebalanceTime(17_000L));
    assertTrue(JobClaiming.isMidnightRebalanceTime(18_000L));
    assertTrue(JobClaiming.isMidnightRebalanceTime(18_999L));
    assertFalse(JobClaiming.isMidnightRebalanceTime(19_000L));
    assertTrue(JobClaiming.isMidnightRebalanceTime(41_000L));
  }

  @Test
  void aReloadedSmallerDefinitionRehousesItsResidentWithoutReassigningRemovedBeds() throws Exception {
    BuildingInfo original = new BuildingInfo("house_birch_forest_2")
        .addBedLocation(2, 1, 2).addBedLocation(4, 1, 2);
    BuildingInfo spare = new BuildingInfo("house_birch_forest_1").addBedLocation(2, 1, 2);
    Buildings.reload(Map.of(original.getName(), original, spare.getName(), spare));
    Building house = new Building(original.getName(), Rotation.NONE);
    Building replacement = new Building(spare.getName(), Rotation.NONE);
    UUID retained = UUID.randomUUID();
    UUID displaced = UUID.randomUUID();

    JsonObject saved = Village.CODEC.encodeStart(JsonOps.INSTANCE, new Village("Bedhaven"))
        .getOrThrow().getAsJsonObject();
    for (Building building : new Building[]{house, replacement}) {
      saved.getAsJsonArray("buildings").add(Building.CODEC.encodeStart(JsonOps.INSTANCE, building).getOrThrow());
    }
    for (UUID resident : new UUID[]{retained, displaced}) {
      saved.getAsJsonArray("people").add(UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, resident).getOrThrow());
    }
    saved.getAsJsonObject("bed_assignments").add(retained.toString(), BedAssignment.CODEC.encodeStart(
        JsonOps.INSTANCE, new BedAssignment(retained, house.getUUID(), 0)).getOrThrow());
    saved.getAsJsonObject("bed_assignments").add(displaced.toString(), BedAssignment.CODEC.encodeStart(
        JsonOps.INSTANCE, new BedAssignment(displaced, house.getUUID(), 1)).getOrThrow());
    for (BedAssignment bed : new BedAssignment[]{
        new BedAssignment(null, house.getUUID(), 2),
        new BedAssignment(null, house.getUUID(), -1),
        new BedAssignment(null, UUID.randomUUID(), 0),
        new BedAssignment(null, replacement.getUUID(), 0)}) {
      saved.getAsJsonArray("unassigned_beds").add(BedAssignment.CODEC.encodeStart(JsonOps.INSTANCE, bed).getOrThrow());
    }
    Village restored = Village.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    BuildingInfo reduced = new BuildingInfo(original.getName()).addBedLocation(2, 1, 2);
    Buildings.reload(Map.of(reduced.getName(), reduced, spare.getName(), spare));

    Method reconcile = Village.class.getDeclaredMethod("reconcileBeds");
    reconcile.setAccessible(true);
    reconcile.invoke(restored);

    assertEquals(house.getUUID(), restored.getBedAssignment(retained).getBuildingUUID());
    assertEquals(0, restored.getBedAssignment(retained).getBedIndex());
    assertEquals(replacement.getUUID(), restored.getBedAssignment(displaced).getBuildingUUID());
    assertEquals(0, restored.getBedAssignment(displaced).getBedIndex());
    assertTrue(restored.getUnassignedBeds().isEmpty());
    assertEquals(2, restored.getTotalBeds());

    Map<UUID, BedAssignment> assignments = restored.getBedAssignmentsView();
    reconcile.invoke(restored);
    JobClaiming.registerMissingBeds(restored);
    assertEquals(assignments, restored.getBedAssignmentsView());
    assertTrue(restored.getUnassignedBeds().isEmpty());
    assertEquals(2, restored.getTotalBeds());
  }
}
