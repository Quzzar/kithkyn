package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CoupleBedsDefinitionTest {
  @Test
  void workplacesCanMixGeneralBedsStaffBedsAndCoupleRooms() {
    BuildingInfo info = parse("""
        {"structure":"farm_birch_forest_1","beds":[[0,1,0],[1,1,0],[4,1,0],[6,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]]],"worker_beds":[[0,1,0],[1,1,0],[4,1,0]],
         "work_stations":[{"pos":[3,1,1],"occupation":"FARMER"}]}
        """);
    assertNull(info.validate());
    assertTrue(info.isWorkerBed(0));
    assertTrue(info.isWorkerBed(1));
    assertTrue(info.isWorkerBed(2));
    assertFalse(info.isWorkerBed(3));
    BuildingInfo decoded = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, info).getOrThrow()).getOrThrow();
    assertTrue(decoded.isWorkerBed(0));
    assertFalse(decoded.isWorkerBed(3));
    BuildingImpact.Capacity capacity = BuildingImpact.capacity(decoded, 0);
    assertEquals(1, capacity.generalBeds());
    assertEquals(1, capacity.workerBeds());
    assertEquals(1, capacity.coupleRooms());
    assertEquals(1, capacity.workerCoupleRooms());
    assertTrue(capacity.describe(false).contains("1 rooms for staff households"));
  }

  @Test
  void workerRoomDeclarationsRejectMissingBedsAndHalfReservedCoupleRooms() {
    assertEquals("worker_beds names an undeclared bed", parse("""
        {"structure":"farm_birch_forest_1","beds":[[0,1,0]],"worker_beds":[[1,1,0]],
         "work_stations":[{"pos":[3,1,1],"occupation":"FARMER"}]}
        """).validate());
    assertEquals("worker_beds must reserve both beds of a couple room or neither", parse("""
        {"structure":"farm_birch_forest_1","beds":[[0,1,0],[1,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]]],"worker_beds":[[0,1,0]],
         "work_stations":[{"pos":[3,1,1],"occupation":"FARMER"}]}
        """).validate());
    assertEquals("worker_beds requires a workplace", parse("""
        {"structure":"house_birch_forest_1","beds":[[0,1,0]],"worker_beds":[[0,1,0]]}
        """).validate());
  }

  @Test
  void mixedHousingDeclaresOnlyItsPairedRoomsAndRoundTripsThem() {
    BuildingInfo info = parse("""
        {"structure":"house_birch_forest_1", "beds":[[1,1,1],[4,1,1],[2,1,1]],
         "couple_beds":[[[1,1,1],[2,1,1]]]}
        """);
    assertNull(info.validate());
    assertEquals(1, info.getSingleBedCount());
    assertTrue(info.sharesCoupleBeds(0, 2));
    assertFalse(info.sharesCoupleBeds(0, 1));
    assertFalse(info.sharesCoupleBeds(0, 0));
    assertFalse(info.isCoupleBed(-1));
    assertFalse(info.isCoupleBed(3));
    BuildingInfo decoded = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, info).getOrThrow()).getOrThrow();
    assertEquals(info.getCoupleBeds(), decoded.getCoupleBeds());
    assertTrue(decoded.sharesCoupleBeds(2, 0));
    BuildingImpact.Capacity capacity = BuildingImpact.capacity(info, 0);
    assertEquals(1, capacity.generalBeds());
    assertEquals(0, capacity.workerBeds());
    assertEquals(1, capacity.coupleRooms());
    assertTrue(capacity.describe(false).contains("1 couple rooms (two reserved beds each; 0 rooms for staff households)"));
  }

  @Test
  void invalidPairsAreRejectedAtTheDefinitionBoundary() {
    assertTrue(BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"house_birch_forest_1","couple_beds":[[[1,1,1]]]}
        """)).error().isPresent());
    assertEquals("couple_beds names an undeclared bed", parse("""
        {"structure":"house_birch_forest_1","beds":[[1,1,1]],"couple_beds":[[[1,1,1],[2,1,1]]]}
        """).validate());
    assertEquals("couple_beds repeats a bed", parse("""
        {"structure":"house_birch_forest_1","beds":[[1,1,1],[2,1,1],[3,1,1]],
         "couple_beds":[[[1,1,1],[2,1,1]],[[2,1,1],[3,1,1]]]}
        """).validate());
    assertEquals("couple_beds must name neighboring beds on the same floor", parse("""
        {"structure":"house_birch_forest_1","beds":[[1,1,1],[8,1,1]],
         "couple_beds":[[[1,1,1],[8,1,1]]]}
        """).validate());
  }

  @Test
  void onlyLegacyCottagesInferAPairAndExplicitEmptyMetadataOverridesIt() {
    BuildingInfo cottage = parse("""
        {"structure":"couple_cottage_birch_forest_1","beds":[[1,1,1],[2,1,1]]}
        """);
    assertEquals(new BuildingInfo.CoupleBeds(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1)),
        cottage.getCoupleBeds().getFirst());
    assertTrue(parse("""
        {"structure":"house_birch_forest_1","beds":[[1,1,1],[2,1,1]]}
        """).getCoupleBeds().isEmpty());
    assertTrue(parse("""
        {"structure":"couple_cottage_birch_forest_1","beds":[[1,1,1],[2,1,1]],"couple_beds":[]}
        """).getCoupleBeds().isEmpty());
  }

  private static BuildingInfo parse(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
