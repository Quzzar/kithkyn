package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.relationships.RelationshipPair;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingImpact;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RoleHousingTest {
  private static final UUID RULER = UUID.randomUUID();
  private static final UUID SPOUSE = UUID.randomUUID();
  private static final UUID CAPTAIN = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  @AfterEach
  void clearDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void royalSuiteAdmitsSingleIncumbentButCannotSponsorAnotherCastleJob() {
    Building castle = castle(false);
    Village village = village(List.of(castle), Map.of(), List.of(RULER, OTHER), null);
    assertTrue(village.canHouseForJob(RULER, leader(castle)));
    assertFalse(village.canHouseForJob(OTHER, new JobAssignment(null, Occupation.GUARD, castle.getUUID(), 1)));
    assertFalse(village.canHouseForJob(OTHER, new JobAssignment(null, Occupation.BLACKSMITH, castle.getUUID(), 2)));
    assertEquals(0, village.getFreeGeneralBedCount());
    assertEquals(0, village.getFreeCoupleHomeCount());
    assertEquals(0, village.getFreeWorkerCoupleHomeCount());
    village.assignJob(RULER, leader(castle));
    assertEquals(0, village.getBedAssignment(RULER).getBedIndex());
    assertEquals(2, village.getUnassignedBeds().size());
    assertEquals(0, village.getFreeGeneralBedCount());
    assertFalse(village.canHouseForJob(OTHER, leader(castle)));
  }

  @Test
  void royalHouseholdMovesAtomicallyAndRemainsTogetherAcrossReloadAndSpouseJobs() throws Exception {
    Building castle = castle(false);
    Building cottage = cottage();
    Village village = village(List.of(castle, cottage), Map.of(), List.of(RULER, SPOUSE), null);
    marry(village, RULER, SPOUSE);
    assertTrue(village.houseCouple(RULER, SPOUSE, cottage.getUUID()));
    village.assignJob(RULER, leader(castle));
    assertTrue(village.sharesCoupleHome(RULER, SPOUSE));
    assertEquals(castle.getUUID(), village.getBedAssignment(SPOUSE).getBuildingUUID());
    assertEquals(1, village.getFreeCoupleHomeCount());
    village.assignJob(SPOUSE, new JobAssignment(null, Occupation.FARMER, cottage.getUUID(), 0));
    village = Village.CODEC.parse(JsonOps.INSTANCE,
        Village.CODEC.encodeStart(JsonOps.INSTANCE, village).getOrThrow()).getOrThrow();
    reconcile(village);
    assertTrue(village.sharesCoupleHome(RULER, SPOUSE));
    assertEquals(castle.getUUID(), village.getBedAssignment(RULER).getBuildingUUID());
    assertEquals(cottage.getUUID(), village.getJobAssignment(SPOUSE).getBuildingUUID());
  }

  @Test
  void singleRulerCanMarryLaterAndGiveTheirSpouseTheOtherSide() throws Exception {
    Building castle = castle(false);
    Village village = village(List.of(castle), Map.of(), List.of(RULER, SPOUSE), null);
    village.assignJob(RULER, leader(castle));
    marry(village, RULER, SPOUSE);
    reconcile(village);
    assertTrue(village.sharesCoupleHome(RULER, SPOUSE));
    assertEquals(1, village.getBedAssignment(SPOUSE).getBedIndex());
  }

  @Test
  void captainMovesFromCenterWithoutCreatingOrChangingAJob() throws Exception {
    Building castle = castle(false);
    Building center = center();
    Village village = village(List.of(castle, center),
        Map.of(CAPTAIN, new BedAssignment(CAPTAIN, center.getUUID(), 0)), List.of(CAPTAIN, OTHER), center);
    JobAssignment captain = new JobAssignment(null, Occupation.GUARD, center.getUUID(), 0);
    village.assignJob(CAPTAIN, captain);
    assertEquals(castle.getUUID(), village.getBedAssignment(CAPTAIN).getBuildingUUID());
    assertEquals(2, village.getBedAssignment(CAPTAIN).getBedIndex());
    assertEquals(center.getUUID(), village.getJobAssignment(CAPTAIN).getBuildingUUID());
    assertEquals(0, village.getJobAssignment(CAPTAIN).getStationIndex());
    assertEquals(1, village.getJobAssignmentsView().size());
    assertEquals(1, village.getFreeGeneralBedCount());
    reconcile(village);
    assertEquals(center.getUUID(), village.getBedAssignment(OTHER).getBuildingUUID());
    assertFalse(village.canHouseForJob(CAPTAIN, new JobAssignment(null, Occupation.GUARD, castle.getUUID(), 1)));
  }

  @Test
  void marriedCaptainKeepsTheirCoupleHomeInsteadOfSplittingIntoSingleCastleRoom() throws Exception {
    Building castle = castle(false);
    Building center = center();
    Building cottage = cottage();
    Village village = village(List.of(castle, center, cottage), Map.of(), List.of(CAPTAIN, SPOUSE), center);
    marry(village, CAPTAIN, SPOUSE);
    assertTrue(village.houseCouple(CAPTAIN, SPOUSE, cottage.getUUID()));
    village.assignJob(CAPTAIN, new JobAssignment(null, Occupation.GUARD, center.getUUID(), 0));
    reconcile(village);
    assertTrue(village.sharesCoupleHome(CAPTAIN, SPOUSE));
    assertEquals(cottage.getUUID(), village.getBedAssignment(CAPTAIN).getBuildingUUID());
    assertTrue(village.getUnassignedBeds().stream().anyMatch(bed -> bed.getBuildingUUID().equals(castle.getUUID())
        && bed.getBedIndex() == 2));
  }

  @Test
  void losingRulershipReleasesBothSidesAndDeathAllowsSuccession() throws Exception {
    Building castle = castle(false);
    Village village = village(List.of(castle), Map.of(), List.of(RULER, SPOUSE, OTHER), null);
    marry(village, RULER, SPOUSE);
    village.assignJob(RULER, leader(castle));
    village.releaseJob(RULER);
    assertNull(village.getBedAssignment(RULER));
    assertNull(village.getBedAssignment(SPOUSE));
    assertFalse(village.houseCouple(RULER, SPOUSE, castle.getUUID()));
    village.assignJob(RULER, leader(castle));
    village.removePerson(RULER);
    // The remaining spouse no longer holds a role; reconciliation releases their reserved side.
    reconcile(village);
    assertTrue(village.canHouseForJob(OTHER, leader(castle)));
    village.assignJob(OTHER, leader(castle));
    assertEquals(castle.getUUID(), village.getBedAssignment(OTHER).getBuildingUUID());
  }

  @Test
  void unavailableRoomDoesNotReleaseAnyExistingBedOrOccupant() {
    Building castle = castle(false);
    Building cottage = cottage();
    Map<UUID, BedAssignment> assigned = new java.util.HashMap<>(Map.of(
        RULER, new BedAssignment(RULER, cottage.getUUID(), 0),
        OTHER, new BedAssignment(OTHER, castle.getUUID(), 1)));
    List<BedAssignment> open = new ArrayList<>(List.of(new BedAssignment(null, castle.getUUID(), 0)));
    assertFalse(CoupleHousing.assignIncumbent(RULER, castle, castle.getInfo().getRoomReservations().getFirst(), assigned, open));
    assertEquals(cottage.getUUID(), assigned.get(RULER).getBuildingUUID());
    assertEquals(1, assigned.get(OTHER).getBedIndex());
    assertEquals(1, open.size());
  }

  @Test
  void fiveGeneralBedsRemainUsableInMixedCastleAndRolesCountAsReserved() {
    Building castle = castle(true);
    Village village = village(List.of(castle), Map.of(), List.of(OTHER), null);
    assertEquals(5, village.getFreeGeneralBedCount());
    assertEquals(5, BuildingImpact.generalBeds(castle.getInfo()));
    assertEquals(1, castle.getInfo().getWorkerSingleBedCount());
    assertEquals(1, castle.getInfo().getWorkerCoupleRoomCount());
  }

  @Test
  void roleRoomMetadataRejectsPartialPairsUnknownBedsAndAmbiguousRoles() {
    for (String reservation : List.of(
        "{\"beds\":[[0,1,0]],\"occupation\":\"LEADER\"}",
        "{\"beds\":[[9,1,9]],\"occupation\":\"LEADER\"}",
        "{\"beds\":[[0,1,0],[1,1,0]],\"occupation\":\"LEADER\",\"guard_role\":\"CAPTAIN\"}",
        "{\"beds\":[[0,1,0],[1,1,0]],\"guard_role\":\"PATROL\"}")) {
      BuildingInfo info = decode("""
          {"structure":"castle_desert_1","beds":[[0,1,0],[1,1,0]],
           "couple_beds":[[[0,1,0],[1,1,0]]],"worker_beds":[],
           "work_stations":[{"pos":[4,1,0],"occupation":"LEADER"}],"room_reservations":[%s]}
          """.formatted(reservation));
      assertNotNull(info.validate(), reservation);
    }
  }

  private static JobAssignment leader(Building castle) {
    return new JobAssignment(null, Occupation.LEADER, castle.getUUID(), 0);
  }

  private static Building castle(boolean generalBeds) {
    return register(decode("""
        {"structure":"castle_desert_1","beds":[[0,1,0],[1,1,0],[3,1,0]%s],
         "couple_beds":[[[0,1,0],[1,1,0]]],"worker_beds":[],
         "room_reservations":[{"beds":[[0,1,0],[1,1,0]],"occupation":"LEADER"},
          {"beds":[[3,1,0]],"guard_role":"CAPTAIN"}],
         "work_stations":[{"pos":[4,1,0],"occupation":"LEADER"},
          {"pos":[5,1,0],"occupation":"GUARD"},{"pos":[6,1,0],"occupation":"BLACKSMITH"}]}
        """.formatted(generalBeds ? ",[5,1,2],[7,1,2],[9,1,2],[11,1,2],[13,1,2]" : "")));
  }

  private static Building center() {
    return register(decode("""
        {"structure":"village_center_desert_1","beds":[[0,1,3]],
         "work_stations":[{"pos":[1,1,3],"occupation":"GUARD","guard_duty":"CAPTAIN"}]}
        """));
  }

  private static Building cottage() {
    return register(decode("""
        {"structure":"couple_cottage_desert_1","beds":[[0,1,0],[1,1,0]]}
        """));
  }

  private static Building register(BuildingInfo info) {
    assertNull(info.validate());
    Map<String, BuildingInfo> definitions = new java.util.HashMap<>();
    definitions.putAll(Buildings.allBuildings());
    definitions.put(info.getName(), info);
    Buildings.reload(definitions);
    return new Building(info.getName(), Rotation.NONE);
  }

  private static BuildingInfo decode(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }

  private static void marry(Village village, UUID first, UUID second) {
    village.putRelationship(RelationshipPair.create(first, second, 75, 0, 0, false, "", true));
  }

  private static Village village(List<Building> buildings, Map<UUID, BedAssignment> assignments,
      List<UUID> residents, Building center) {
    JsonObject saved = Village.CODEC.encodeStart(JsonOps.INSTANCE, new Village("Castle rooms")).getOrThrow().getAsJsonObject();
    if (center != null) saved.add("town_center", UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, center.getUUID()).getOrThrow());
    for (Building building : buildings) {
      saved.getAsJsonArray("buildings").add(Building.CODEC.encodeStart(JsonOps.INSTANCE, building).getOrThrow());
      for (int index = 0; index < building.getInfo().getBedLocations().size(); index++) {
        int bedIndex = index;
        if (assignments.values().stream().noneMatch(bed -> bed.getBuildingUUID().equals(building.getUUID())
            && bed.getBedIndex() == bedIndex)) {
          saved.getAsJsonArray("unassigned_beds").add(BedAssignment.CODEC.encodeStart(JsonOps.INSTANCE,
              new BedAssignment(null, building.getUUID(), index)).getOrThrow());
        }
      }
    }
    residents.forEach(resident -> saved.getAsJsonArray("people")
        .add(UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, resident).getOrThrow()));
    assignments.forEach((resident, bed) -> saved.getAsJsonObject("bed_assignments")
        .add(resident.toString(), BedAssignment.CODEC.encodeStart(JsonOps.INSTANCE, bed).getOrThrow()));
    return Village.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
  }

  private static void reconcile(Village village) throws Exception {
    Method method = Village.class.getDeclaredMethod("reconcileBeds");
    method.setAccessible(true);
    method.invoke(village);
  }
}
