package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.Occupation;

class BuildingImpactTest {
  @Test
  void workplaceBedsCannotMasqueradeAsGeneralHousing() {
    BuildingInfo hut = new BuildingInfo("lumberjack_plains_1").addBedLocation(0, 1, 0)
        .addWorkLocation(1, 1, 0, Occupation.LUMBERJACK);
    BuildingImpact.Capacity capacity = BuildingImpact.capacity(hut, 0);
    assertEquals(0, capacity.generalBeds());
    assertEquals(1, capacity.workerBeds());
    assertTrue(capacity.describe(false).contains("0 general beds, 1 beds reserved for workers"));
  }

  @Test
  void centerBedsRemainGeneralEvenWhenItHasWorkstations() {
    BuildingInfo center = new BuildingInfo("village_center_plains_1").addBedLocation(0, 1, 0)
        .addWorkLocation(1, 1, 0, Occupation.BUILDER);
    assertEquals(1, BuildingImpact.capacity(center, 0).generalBeds());
    assertEquals(0, BuildingImpact.capacity(center, 0).workerBeds());
  }

  @Test
  void storageAndCropExpansionDoesNotClaimToHouseAnybody() {
    BuildingImpact.Capacity first = new BuildingImpact.Capacity(0, 0, 1, Map.of(Occupation.FARMER, 1), 26);
    BuildingImpact.Capacity larger = new BuildingImpact.Capacity(0, 0, 3, Map.of(Occupation.FARMER, 1), 52);
    BuildingImpact.Capacity net = BuildingImpact.net(larger, List.of(first));
    assertEquals(0, net.generalBeds());
    assertEquals(0, net.workplaces());
    assertEquals(2, net.containers());
    assertEquals(26, net.cropPlots());
    assertTrue(net.describe(true).contains("0 general beds"));
    assertTrue(net.describe(true).contains("+26 crop plots"));
  }

  @Test
  void removedHousingAndWorkerBedsAreSubtractedSeparately() {
    var net = BuildingImpact.net(new BuildingImpact.Capacity(5, 0, 1, Map.of(), 0), List.of(
        new BuildingImpact.Capacity(3, 0, 1, Map.of(), 0),
        new BuildingImpact.Capacity(0, 1, 1, Map.of(Occupation.BUILDER, 1), 0)));
    assertEquals(2, net.generalBeds());
    assertEquals(-1, net.workerBeds());
    assertEquals(Map.of(Occupation.BUILDER, -1), net.jobs());
  }

  @Test
  void losingBothWellsExplicitlyLosesLastWaterSourceButRemovingOneDoesNot() {
    BuildingInfo well = definition("well_plains_1", "\"grants\":[\"WATER\"]");
    BuildingInfo farm = definition("farm_plains_2", "\"grants\":[\"GRAIN\"]");
    var one = BuildingImpact.services(List.of(well, well, farm), List.of(well), farm, item -> true);
    var both = BuildingImpact.services(List.of(well, well, farm), List.of(), farm, item -> true);
    assertTrue(one.lostAfter().isEmpty());
    assertEquals(Set.of("WATER"), both.lostAfter());
    assertTrue(both.describe().contains("last fresh-water source"));
    assertFalse(one.describe().contains("last fresh-water source"));
  }

  @Test
  void replacingOnlyFoodBuildingReportsTemporaryRatherThanPermanentServiceLoss() {
    BuildingInfo farm = definition("farm_plains_1", "\"grants\":[\"GRAIN\"]");
    var effects = BuildingImpact.services(List.of(farm), List.of(), farm, item -> true);
    assertEquals(Set.of("GRAIN"), effects.lostDuring());
    assertTrue(effects.lostAfter().isEmpty());
    assertTrue(effects.describe().contains("unavailable only during work: grain production"));
  }

  @Test
  void removingAPrerequisiteAlsoReportsItsDependentService() {
    BuildingInfo well = definition("well_plains_1", "\"grants\":[\"WATER\"]");
    BuildingInfo producer = definition("farm_plains_1",
        "\"grants_if\":[{\"capability\":\"GRAIN\",\"requires_capability\":[\"WATER\"]}]");
    BuildingInfo house = definition("house_plains_1", "\"grants\":[]");
    var effects = BuildingImpact.services(List.of(well, producer), List.of(producer), house, item -> true);
    assertEquals(Set.of("WATER", "GRAIN"), effects.lostAfter());
  }

  @Test
  void supplyConditionsUseObservedStockInsteadOfPromisingUnavailableServices() {
    BuildingInfo workshop = definition("blacksmith_plains_1",
        "\"grants_if\":[{\"capability\":\"REPAIR\",\"requires_supply\":[\"minecraft:iron_ingot\"]}]");
    var unavailable = BuildingImpact.services(List.of(), List.of(), workshop, item -> false);
    var supplied = BuildingImpact.services(List.of(), List.of(), workshop, item -> true);
    assertTrue(unavailable.gained().isEmpty());
    assertEquals(Set.of("REPAIR"), supplied.gained());
  }

  private static BuildingInfo definition(String name, String fields) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        JsonParser.parseString("{\"structure\":\"" + name + "\"," + fields + "}")).getOrThrow();
  }
}
