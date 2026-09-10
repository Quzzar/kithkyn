package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.relationships.RelationshipPair;
import com.quzzar.kithkyn.relationships.MarriageService;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.UrbanPlanner;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CoupleHousingTest {
  private static final UUID A = UUID.randomUUID();
  private static final UUID B = UUID.randomUUID();
  private static final UUID C = UUID.randomUUID();
  private static final UUID D = UUID.randomUUID();
  private static final UUID SINGLE = UUID.randomUUID();

  @AfterEach
  void clearDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void aMarriedFarmerOrButcherCanClaimTheJobAndMoveTheirNonworkingSpouseIntoItsPair() throws Exception {
    for (Occupation occupation : List.of(Occupation.FARMER, Occupation.BUTCHER)) {
      Building workplace = coupleWorkplace(occupation);
      Village village = savedVillage(List.of(workplace), Map.of(), List.of(A, B, SINGLE));
      assertEquals(0, village.getFreeGeneralBedCount());
      assertEquals(0, village.getFreeReservedBedCountIn(workplace.getUUID()));
      assertEquals(1, village.getFreeWorkerCoupleHomeCount());
      assertFalse(village.houseCouple(A, B, workplace.getUUID()));
      assertFalse(village.canHouseForJob(SINGLE, workplace.getUUID()));
      assertTrue(village.canHouseForJob(A, workplace.getUUID()));
      village.assignJob(A, new JobAssignment(null, occupation, workplace.getUUID(), 0));
      assertTrue(village.sharesCoupleHome(A, B));
      assertFalse(village.sleepsInReservedSingleBedAt(A, workplace.getUUID()));
      assertEquals(workplace.getUUID(), village.getBedAssignment(B).getBuildingUUID());
      assertEquals(null, village.getJobAssignment(B));
      reconcile(village);
      assertTrue(village.sharesCoupleHome(A, B));
      assertEquals(0, village.getFreeWorkerCoupleHomeCount());
    }
  }

  @Test
  void staffCouplesKeepOneHomeWhenTheirSpousesWorkAtDifferentLiveInWorkplaces() throws Exception {
    Building workplace = coupleWorkplace(Occupation.FARMER);
    BuildingInfo secondInfo = decode("""
        {"structure":"butchery_plains_1","beds":[[0,1,0],[1,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]]],
         "work_stations":[{"pos":[3,1,1],"occupation":"BUTCHER"}]}
        """);
    Buildings.reload(Map.of(workplace.getName(), workplace.getInfo(), secondInfo.getName(), secondInfo));
    Building second = new Building(secondInfo.getName(), Rotation.NONE);
    Village village = savedVillage(List.of(workplace, second), Map.of(), List.of(A, B));
    village.assignJob(A, new JobAssignment(null, Occupation.FARMER, workplace.getUUID(), 0));
    village.assignJob(B, new JobAssignment(null, Occupation.BUTCHER, second.getUUID(), 0));
    for (int pass = 0; pass < 3; pass++) reconcile(village);
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(workplace.getUUID(), village.getBedAssignment(A).getBuildingUUID());
    assertEquals(second.getUUID(), village.getJobAssignment(B).getBuildingUUID());
    assertEquals(1, village.getFreeWorkerCoupleHomeCount());
  }

  @Test
  void theWholeStaffRoomReopensOnlyWhenNeitherSpouseWorksThere() {
    Building workplace = coupleWorkplace(Occupation.FARMER);
    Village village = savedVillage(List.of(workplace), Map.of(), List.of(A, B));
    village.assignJob(A, new JobAssignment(null, Occupation.FARMER, workplace.getUUID(), 0));
    village.assignJob(B, new JobAssignment(null, Occupation.BUILDER, workplace.getUUID(), 1));
    village.releaseJob(A);
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(0, village.getFreeWorkerCoupleHomeCount());
    village.releaseJob(B);
    assertTrue(village.getBedAssignmentsView().isEmpty());
    assertEquals(1, village.getFreeWorkerCoupleHomeCount());
    assertEquals(0, village.getFreeGeneralBedCount());
  }

  @Test
  void claimingWorkNeverOverwritesTheOldCoupleHomeOrAnOccupiedSide() throws Exception {
    Building workplace = coupleWorkplace(Occupation.FARMER);
    BuildingInfo homeInfo = new BuildingInfo("couple_cottage_plains_1")
        .addBedLocation(1, 1, 1).addBedLocation(2, 1, 1);
    Buildings.reload(Map.of(workplace.getName(), workplace.getInfo(), homeInfo.getName(), homeInfo));
    Building home = new Building(homeInfo.getName(), Rotation.NONE);
    Village village = savedVillage(List.of(workplace, home), Map.of(
        SINGLE, new BedAssignment(SINGLE, workplace.getUUID(), 1)), List.of(A, B, SINGLE));
    // This resident remains entitled to their already occupied side while they staff the building.
    village.assignJob(SINGLE, new JobAssignment(null, Occupation.FARMER, workplace.getUUID(), 0));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    village.assignJob(A, new JobAssignment(null, Occupation.BUILDER, workplace.getUUID(), 1));
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(home.getUUID(), village.getBedAssignment(A).getBuildingUUID());
    assertEquals(1, village.getBedAssignment(SINGLE).getBedIndex());
    village.removePerson(SINGLE);
    reconcile(village);
    assertEquals(workplace.getUUID(), village.getBedAssignment(A).getBuildingUUID());
    assertTrue(village.sharesCoupleHome(A, B));
  }

  @Test
  void aPendingSpouseDoesNotQualifyTheWorkerForAPairedRoom() {
    Building workplace = coupleWorkplace(Occupation.FARMER);
    Village village = savedVillage(List.of(workplace), Map.of(), List.of(A));
    assertFalse(village.canHouseForJob(A, workplace.getUUID()));
    assertFalse(village.houseCouple(A, B, workplace.getUUID()));
    assertEquals(1, village.getFreeWorkerCoupleHomeCount());
    village.getPopulation().add(B);
    assertTrue(village.canHouseForJob(A, workplace.getUUID()));
  }

  @Test
  void theTavernCanReserveOneBedForItsKeeperAndOfferTheOtherAsGeneralHousing() throws Exception {
    BuildingInfo info = decode("""
        {"structure":"tavern_plains_1","beds":[[4,1,0],[0,1,0]],
         "worker_beds":[[0,1,0]],"work_stations":[{"pos":[3,1,1],"occupation":"INNKEEPER"}]}
        """);
    assertEquals(null, info.validate());
    Buildings.reload(Map.of(info.getName(), info));
    Building tavern = new Building(info.getName(), Rotation.NONE);
    Village village = savedVillage(List.of(tavern), Map.of(), List.of(SINGLE, A));
    assertEquals(1, village.getFreeGeneralBedCount());
    assertEquals(1, village.getFreeReservedBedCountIn(tavern.getUUID()));
    reconcile(village);
    assertEquals(0, village.getBedAssignment(SINGLE).getBedIndex());
    assertTrue(village.canHouseForJob(A, tavern.getUUID()));
    village.assignJob(A, new JobAssignment(null, Occupation.INNKEEPER, tavern.getUUID(), 0));
    assertEquals(1, village.getBedAssignment(A).getBedIndex());
    assertTrue(village.sleepsInReservedSingleBedAt(A, tavern.getUUID()));
    village.releaseJob(A);
    assertEquals(null, village.getBedAssignment(A));
    assertEquals(0, village.getBedAssignment(SINGLE).getBedIndex());
    assertEquals(1, village.getFreeReservedBedCountIn(tavern.getUUID()));
  }

  @Test
  void aKeeperAlreadySleepingInTheGeneralTavernRoomMovesToTheReservedRoom() {
    BuildingInfo info = decode("""
        {"structure":"tavern_plains_1","beds":[[4,1,0],[0,1,0]],
         "worker_beds":[[0,1,0]],"work_stations":[{"pos":[3,1,1],"occupation":"INNKEEPER"}]}
        """);
    Buildings.reload(Map.of(info.getName(), info));
    Building tavern = new Building(info.getName(), Rotation.NONE);
    Village village = savedVillage(List.of(tavern), Map.of(A, new BedAssignment(A, tavern.getUUID(), 0)), List.of(A));
    village.assignJob(A, new JobAssignment(null, Occupation.INNKEEPER, tavern.getUUID(), 0));
    assertEquals(1, village.getBedAssignment(A).getBedIndex());
    assertEquals(1, village.getFreeGeneralBedCount());
  }

  @Test
  void aReturningPairClaimsItsRoomOnlyAfterBothTravelersBecomeResidents() {
    Building home = onePairHome();
    Village village = savedVillage(List.of(home), Map.of(), List.of(A));
    assertEquals(0, MarriageService.awaitingHomeCount(village));
    assertFalse(village.houseCouple(A, B, home.getUUID()));
    assertTrue(village.getBedAssignmentsView().isEmpty());
    assertEquals(1, village.getFreeCoupleHomeCount());
    village.getPopulation().add(B);
    assertEquals(1, MarriageService.awaitingHomeCount(village));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(2, village.getTotalBeds());
  }

  @Test
  void twoCouplesAndASingleOccupyDifferentRoomsThroughSaveRoundTrip() {
    Building home = mixedHome();
    Village village = savedVillage(List.of(home), Map.of(), List.of(A, B, C, D, SINGLE));
    assertEquals(2, village.getFreeGeneralBedCount());
    assertEquals(2, village.getFreeCoupleHomeCount());
    assertEquals(2, MarriageService.awaitingHomeCount(village));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    assertTrue(village.houseCouple(C, D, home.getUUID()));
    village = Village.CODEC.parse(JsonOps.INSTANCE,
        Village.CODEC.encodeStart(JsonOps.INSTANCE, village).getOrThrow()).getOrThrow();
    assertTrue(village.sharesCoupleHome(A, B));
    assertTrue(village.sharesCoupleHome(C, D));
    assertFalse(village.sharesCoupleHome(A, C));
    assertEquals(4, village.getBedAssignmentsView().values().stream().map(BedAssignment::getBedIndex).distinct().count());
    assertEquals(0, village.getFreeCoupleHomeCount());
    assertEquals(0, MarriageService.awaitingHomeCount(village));
    assertEquals(2, village.getFreeGeneralBedCount());
  }

  @Test
  void singlesClaimOnlyUnpairedBedsDuringReconciliation() throws Exception {
    Building home = mixedHome();
    Village village = savedVillage(List.of(home), Map.of(), List.of(A, B, C, D, SINGLE));
    reconcile(village);
    assertEquals(2, village.getBedAssignmentsView().size());
    for (BedAssignment assignment : village.getBedAssignmentsView().values()) {
      assertFalse(home.getInfo().isCoupleBed(assignment.getBedIndex()));
    }
    assertEquals(2, village.getFreeCoupleHomeCount());
    assertFalse(village.hasFreeBedIn(home.getUUID()));
    assertFalse(village.canHouseForJob(SINGLE, home.getUUID()));
  }

  @Test
  void anUnavailableSecondBedLeavesBothSpousesAndTheUnrelatedResidentUntouched() {
    Building home = onePairHome();
    Building singles = new Building("singles_plains_1", Rotation.NONE);
    BuildingInfo singlesInfo = new BuildingInfo(singles.getName()).addBedLocation(0, 1, 0).addBedLocation(2, 1, 0);
    Buildings.reload(Map.of(home.getName(), home.getInfo(), singles.getName(), singlesInfo));
    Map<UUID, BedAssignment> original = Map.of(
        A, new BedAssignment(A, singles.getUUID(), 0),
        B, new BedAssignment(B, singles.getUUID(), 1),
        SINGLE, new BedAssignment(SINGLE, home.getUUID(), 1));
    Village village = savedVillage(List.of(home, singles), original, List.of(A, B, SINGLE));
    assertFalse(village.houseCouple(A, B, home.getUUID()));
    original.forEach((resident, bed) -> {
      assertEquals(bed.getBuildingUUID(), village.getBedAssignment(resident).getBuildingUUID());
      assertEquals(bed.getBedIndex(), village.getBedAssignment(resident).getBedIndex());
    });
    assertEquals(0, village.getFreeCoupleHomeCount());
  }

  @Test
  void spousesInDifferentRoomsMoveIntoAnActualPairWithinTheSameBuilding() {
    Building home = mixedHome();
    Village village = savedVillage(List.of(home), Map.of(
        A, new BedAssignment(A, home.getUUID(), 0),
        B, new BedAssignment(B, home.getUUID(), 2)), List.of(A, B));
    assertFalse(village.sharesCoupleHome(A, B));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(0, village.getBedAssignment(A).getBedIndex());
    assertEquals(1, village.getBedAssignment(B).getBedIndex());
    assertEquals(1, village.getFreeCoupleHomeCount());
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    assertEquals(1, village.getFreeCoupleHomeCount());
  }

  @Test
  void takingALiveInJobDoesNotSplitAnAlreadyHousedCouple() throws Exception {
    Building home = mixedHome();
    BuildingInfo workshop = new BuildingInfo("lumberjack_plains_1").addBedLocation(1, 1, 1)
        .addWorkLocation(3, 1, 1, Occupation.LUMBERJACK);
    Buildings.reload(Map.of(home.getName(), home.getInfo(), workshop.getName(), workshop));
    Building workplace = new Building(workshop.getName(), Rotation.NONE);
    Village village = savedVillage(List.of(home, workplace), Map.of(), List.of(A, B));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    village.assignJob(A, new JobAssignment(null, Occupation.LUMBERJACK, workplace.getUUID(), 0));
    reconcile(village);
    assertTrue(village.sharesCoupleHome(A, B));
    assertEquals(1, village.getFreeReservedBedCountIn(workplace.getUUID()));
    village.releaseJob(A);
    assertTrue(village.sharesCoupleHome(A, B));
  }

  @Test
  void vacatedCoupleBedsStayReservedAndAReplacementCoupleNeedsBothSides() {
    Building home = onePairHome();
    Village village = savedVillage(List.of(home), Map.of(), List.of(A, B, C, D));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    village.removePerson(A);
    assertEquals(0, village.getFreeCoupleHomeCount());
    assertEquals(0, village.getFreeGeneralBedCount());
    assertFalse(village.houseCouple(C, D, home.getUUID()));
    village.removePerson(B);
    assertEquals(1, village.getFreeCoupleHomeCount());
    assertTrue(village.houseCouple(C, D, home.getUUID()));
    assertTrue(village.sharesCoupleHome(C, D));
  }

  @Test
  void reorderedBedDefinitionsResolvePairsFromCoordinatesInsteadOfFrozenPairIndexes() {
    Building home = mixedHome();
    Village village = savedVillage(List.of(home), Map.of(), List.of(A, B));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    BuildingInfo reordered = decode("""
        {"structure":"house_plains_1","beds":[[0,1,0],[8,1,0],[1,1,0],[9,1,0],[4,1,0],[6,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]],[[8,1,0],[9,1,0]]]}
        """);
    Buildings.reload(Map.of(reordered.getName(), reordered));
    assertFalse(village.sharesCoupleHome(A, B));
    assertTrue(village.houseCouple(A, B, home.getUUID()));
    assertEquals(2, village.getBedAssignment(B).getBedIndex());
    assertTrue(village.sharesCoupleHome(A, B));
  }

  @Test
  void aStyleWithMixedHousingCanSaveForItWithoutADedicatedCottage() {
    Building home = mixedHome();
    Village village = new Village("Rooms");
    assertEquals(home.getInfo(), UrbanPlanner.coupleHomeGoal(village));
    BuildingInfo cottage = new BuildingInfo("couple_cottage_plains_1")
        .addBedLocation(1, 1, 1).addBedLocation(2, 1, 1);
    Buildings.reload(Map.of(home.getName(), home.getInfo(), cottage.getName(), cottage));
    assertEquals(cottage, UrbanPlanner.coupleHomeGoal(village));
  }

  private static Building mixedHome() {
    BuildingInfo info = decode("""
        {"structure":"house_plains_1","beds":[[0,1,0],[1,1,0],[8,1,0],[9,1,0],[4,1,0],[6,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]],[[8,1,0],[9,1,0]]]}
        """);
    Buildings.reload(Map.of(info.getName(), info));
    return new Building(info.getName(), Rotation.NONE);
  }

  private static Building coupleWorkplace(Occupation occupation) {
    BuildingInfo info = decode("""
        {"structure":"farm_plains_1","beds":[[0,1,0],[1,1,0]],
         "couple_beds":[[[0,1,0],[1,1,0]]],"worker_beds":[[0,1,0],[1,1,0]],
         "work_stations":[{"pos":[3,1,1],"occupation":"%s"},{"pos":[4,1,1],"occupation":"BUILDER"}]}
        """.formatted(occupation.name()));
    assertEquals(null, info.validate());
    Buildings.reload(Map.of(info.getName(), info));
    return new Building(info.getName(), Rotation.NONE);
  }

  private static Building onePairHome() {
    BuildingInfo info = new BuildingInfo("couple_cottage_plains_1")
        .addBedLocation(1, 1, 1).addBedLocation(2, 1, 1);
    Buildings.reload(Map.of(info.getName(), info));
    return new Building(info.getName(), Rotation.NONE);
  }

  private static BuildingInfo decode(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }

  private static Village savedVillage(List<Building> buildings, Map<UUID, BedAssignment> assignments,
      List<UUID> residents) {
    JsonObject saved = Village.CODEC.encodeStart(JsonOps.INSTANCE, new Village("Rooms")).getOrThrow().getAsJsonObject();
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
    Village village = Village.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    for (List<UUID> couple : List.of(List.of(A, B), List.of(C, D))) {
      village.putRelationship(RelationshipPair.create(couple.get(0), couple.get(1), 75, 0, 0, false, "", true));
    }
    return village;
  }

  private static void reconcile(Village village) throws Exception {
    Method method = Village.class.getDeclaredMethod("reconcileBeds");
    method.setAccessible(true);
    method.invoke(village);
  }
}
