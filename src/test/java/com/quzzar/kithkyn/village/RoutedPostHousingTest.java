package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Where a centre post routed to a separate worksite sleeps: at the worksite when it has beds of
 * its own (the Romanian mine), and otherwise in the centre's staff beds (the Tundra and Nautical
 * centres, whose four beds sleep the founding miner).
 */
class RoutedPostHousingTest {
  private static final UUID MINER = UUID.randomUUID();

  @AfterEach
  void clearDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void aRoutedMinerSleepsInTheCentreWhenTheMineHasNoBed() throws Exception {
    Building centre = centre();
    Building mine = mine("{\"structure\":\"mine_tundra_1\",\"worksites\":[{\"pos\":[3,1,1],\"occupation\":\"MINER\"}]}");
    Village village = savedVillage(List.of(centre, mine));
    village.assignJob(MINER, new JobAssignment(null, Occupation.MINER, centre.getUUID(), 0));
    assertEquals(centre.getUUID(), village.getBedAssignment(MINER).getBuildingUUID());
    reconcile(village);
    assertEquals(centre.getUUID(), village.getBedAssignment(MINER).getBuildingUUID(),
        "the routed post keeps its centre bed through reconciliation");
    assertEquals(0, village.getFreeReservedBedCountIn(centre.getUUID()));
  }

  @Test
  void aRoutedMinerSleepsAtTheMineWhenItHousesItsWorker() throws Exception {
    Building centre = centre();
    Building mine = mine("{\"structure\":\"mine_tundra_1\",\"beds\":[[2,1,2]],\"worker_beds\":[[2,1,2]],"
        + "\"worksites\":[{\"pos\":[3,1,1],\"occupation\":\"MINER\"}]}");
    Village village = savedVillage(List.of(centre, mine));
    village.assignJob(MINER, new JobAssignment(null, Occupation.MINER, centre.getUUID(), 0));
    assertEquals(mine.getUUID(), village.getBedAssignment(MINER).getBuildingUUID());
    reconcile(village);
    assertEquals(mine.getUUID(), village.getBedAssignment(MINER).getBuildingUUID());
    assertEquals(1, village.getFreeReservedBedCountIn(centre.getUUID()), "the centre's staff bed stays free");
  }

  @Test
  void aRoutedMinerWithNoMineYetTakesTheCentreBed() throws Exception {
    Building centre = centre();
    Village village = savedVillage(List.of(centre));
    village.assignJob(MINER, new JobAssignment(null, Occupation.MINER, centre.getUUID(), 0));
    assertEquals(centre.getUUID(), village.getBedAssignment(MINER).getBuildingUUID());
    assertNull(village.getJobAssignment(UUID.randomUUID()));
  }

  private static Building centre() {
    BuildingInfo info = decode("""
        {"structure":"village_center_tundra_1","beds":[[5,1,4]],"worker_beds":[[5,1,4]],
         "work_stations":[{"pos":[9,1,9],"occupation":"MINER","worksite_category":"mine"}]}
        """);
    assertNull(info.validate());
    return new Building(info.getName(), Rotation.NONE);
  }

  private static Building mine(String json) {
    BuildingInfo info = decode(json);
    assertNull(info.validate());
    return new Building(info.getName(), Rotation.NONE);
  }

  private static BuildingInfo decode(String json) {
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    Map<String, BuildingInfo> loaded = new java.util.HashMap<>(Buildings.allBuildings());
    loaded.put(info.getName(), info);
    Buildings.reload(loaded);
    return info;
  }

  private static Village savedVillage(List<Building> buildings) {
    JsonObject saved = Village.CODEC.encodeStart(JsonOps.INSTANCE, new Village("Routes")).getOrThrow().getAsJsonObject();
    for (Building building : buildings) {
      saved.getAsJsonArray("buildings").add(Building.CODEC.encodeStart(JsonOps.INSTANCE, building).getOrThrow());
      for (int index = 0; index < building.getInfo().getBedLocations().size(); index++) {
        saved.getAsJsonArray("unassigned_beds").add(BedAssignment.CODEC.encodeStart(JsonOps.INSTANCE,
            new BedAssignment(null, building.getUUID(), index)).getOrThrow());
      }
    }
    saved.getAsJsonArray("people").add(UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, MINER).getOrThrow());
    return Village.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
  }

  private static void reconcile(Village village) throws Exception {
    var method = Village.class.getDeclaredMethod("reconcileBeds");
    method.setAccessible(true);
    method.invoke(village);
  }
}
