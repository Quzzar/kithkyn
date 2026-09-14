package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.PopulationOutlook;

class VillageContextSnapshotTest {

  @Test
  void reportsOrdinaryVacanciesSeparatelyFromWallDefense() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Rampart", "town", 12, 8, 4, 0, 0, 0, 0,
        16, 4, 0, 0, Map.of("village center", 1), Map.of("guard", 1),
        Map.of("gate crossbow", 3, "watchtower crossbow", 4), PopulationOutlook.CAN_GROW,
        new VillageContextSnapshot.RecruitmentStatus(60, 50, 8, 8, 0, 0, 0, 0),
        false, Optional.empty(), false, false, false, List.of(), Optional.empty(), Optional.empty(), List.of(),
        List.of(), Optional.empty(), 0, 0, 0, 0);

    for (String briefing : List.of(snapshot.plannerBriefing(), snapshot.chatBriefing())) {
      assertTrue(briefing.contains("Open work: guard x1"));
      assertTrue(briefing.contains("Open wall guard posts: gate crossbow x3, watchtower crossbow x4"));
      assertFalse(briefing.contains("Open work: guard x8"));
    }
  }

  @Test
  void bothBrainsDistinguishFreeCoupleRoomsFromSingleAndWorkplaceBeds() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Mesa", "hamlet", 8, 6, 2, 0, 0, 0, 0,
        14, 2, 0, 0, Map.of("house", 4), Map.of(), Map.of(), PopulationOutlook.CAN_GROW,
        new VillageContextSnapshot.RecruitmentStatus(60, 50, 8, 8, 0, 0, 0, 0),
        false, Optional.empty(), false, false, false, List.of(), Optional.empty(), Optional.empty(), List.of(),
        List.of(), Optional.empty(), 3, 1, 2, 1);
    for (String briefing : List.of(snapshot.plannerBriefing(), snapshot.chatBriefing())) {
      assertTrue(briefing.contains("2 general beds free"));
      assertTrue(briefing.contains("3 couple rooms contain two reserved beds each, with 1 complete pairs free"));
      assertTrue(briefing.contains("2 married couples await a shared room"));
      assertTrue(briefing.contains("1 of those free rooms require one spouse to work in that building"));
    }
  }

  @Test
  void aReportedMineFailureExplainsTheSeparateSiteOptionToBothBrains() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Meadowmere", "hamlet", 4, 4, 0,
        0, 0, 0, 0, 4, 0, 0, 0,
        Map.of("mine", 1), Map.of(), Map.of(), PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(50, 50, 8, 8, 0, 0, 0, 0),
        false, Optional.empty(), false, false, false, List.of(), Optional.empty(), Optional.empty(), List.of(),
        List.of(new VillageContextSnapshot.WorkerBlocker("miner", "Aaron",
            "I cannot get through the mine entrance to reach the work below.", 48000)), Optional.empty(), 0, 0, 0, 0);

    for (String briefing : List.of(snapshot.plannerBriefing(), snapshot.chatBriefing())) {
      assertTrue(briefing.contains("cannot get through the mine entrance"));
      assertTrue(briefing.contains("new mine on a separate site"));
      assertTrue(briefing.contains("Upgrading the existing mine retains its shaft location"));
      assertTrue(briefing.contains("still needs materials, space and a worker"));
    }
  }

  @Test
  void separatesAdultHomelessnessFromPreAdultsWithoutFamilyHomes() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Emberhollow", "hamlet", 11, 6, 2,
        4, 0, 4, 0,
        6, 0, 0, 1,
        Map.of("fishery", 3, "house", 1), Map.of("fisher", 2), Map.of(),
        PopulationOutlook.CAN_GROW,
        new VillageContextSnapshot.RecruitmentStatus(61.0D, 50.0D, 5.5D, 8.0D,
            0.0D, 0.0D, 0.0D, 0.0D),
        false, Optional.empty(), false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), 0, 0, 0, 0);

    String chat = snapshot.chatBriefing();
    assertTrue(chat.contains("4 are pre-adults; working teenagers also appear in the job counts"));
    assertTrue(chat.contains("1 adult resident needs independent housing"));
    assertTrue(chat.contains("None of the 4 pre-adults has a usable resident parent's home"));
    assertTrue(chat.contains("4 have no resident parent recorded"));
    assertTrue(chat.contains("Those children stay by the campfire at night"));
    assertFalse(chat.contains("4 pre-adults are dependently housed"));

    String planner = snapshot.plannerBriefing();
    assertTrue(planner.contains("1 adult resident is already unhoused"));
    assertFalse(planner.contains("5 people are already unhoused"));
  }

  @Test
  void reportsValidDependentHousingWithoutInventingSeparateBeds() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Emberhollow", "hamlet", 3, 2, 0,
        1, 1, 0, 0,
        2, 0, 0, 0,
        Map.of("house", 1), Map.of(), Map.of(), PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(45.0D, 50.0D, 4.0D, 8.0D,
            0.0D, 0.0D, 0.0D, 0.0D),
        false, Optional.empty(), false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), 0, 0, 0, 0);

    String chat = snapshot.chatBriefing();
    assertTrue(chat.contains("no adult residents need independent housing"));
    assertTrue(chat.contains("The pre-adult is housed with a resident parent"));
  }

  @Test
  void tellsBothBrainsWhenSharedStorageCannotAcceptMoreGoods() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Meadowmere", "hamlet", 4, 4, 0,
        0, 0, 0, 0,
        8, 0, 4, 0,
        Map.of("lumberjack", 4, "storehouse", 1), Map.of("lumberjack", 4), Map.of(),
        PopulationOutlook.HELD_AT_FLOOR,
        new VillageContextSnapshot.RecruitmentStatus(20.0D, 50.0D, 0.0D, 8.0D,
            0.0D, 0.0D, -4.0D, 0.0D),
        true, Optional.of(new com.quzzar.kithkyn.village.VillageBrain.StorageOccupancy(162, 162)),
        false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), 0, 0, 0, 0);

    assertTrue(snapshot.plannerBriefing().contains("Shared storage is backed up"));
    assertTrue(snapshot.plannerBriefing().contains("Restoring shelf access or adding central storage is urgent"));
    assertTrue(snapshot.chatBriefing().contains("Shared storage: backed up"));
  }

  @Test
  void connectsARecentlyBuiltVacantWorkplaceToRecruitmentConstraints() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Meadowmere", "hamlet", 4, 4, 0,
        0, 0, 0, 0,
        4, 0, 1, 0,
        Map.of("lumberjack", 1), Map.of("lumberjack", 1), Map.of(),
        PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(41.0D, 50.0D, 2.5D, 8.0D,
            0.0D, 0.0D, -3.0D, 0.0D),
        false, Optional.empty(), false, true, false,
        List.of(new VillageContextSnapshot.WorkplaceStatus(
            "lumberjack", 0L, Map.of(), Map.of("lumberjack", 1), 1)),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), 0, 0, 0, 0);

    String planner = snapshot.plannerBriefing();
    assertTrue(planner.contains("No newcomers are currently arriving"));
    assertTrue(planner.contains("Attractiveness is 41.0/100, below the newcomer threshold of 50.0"));
    assertTrue(planner.contains("Food is 2.5 items per person against a target of 8.0"));
    assertTrue(planner.contains("lumberjack completed today"));
    assertTrue(planner.contains("lumberjack: 0 staffed, 1 open"));
    assertTrue(planner.contains("1 free live-in bed"));
    assertTrue(planner.contains("A staffing decision is currently in progress"));
  }

  @Test
  void warnsTheBrainBeforeCentralShelvesBecomeCompletelyFull() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Shelfwatch", "town", 10, 9, 1, 0, 0, 0, 0,
        12, 1, 1, 0, Map.of("storehouse", 1), Map.of(), Map.of(), PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(50, 50, 4, 8, 0, 0, 0, 0),
        false, Optional.of(new com.quzzar.kithkyn.village.VillageBrain.StorageOccupancy(47, 54)),
        false, false, false, List.of(), Optional.empty(), Optional.empty(), List.of(),
        List.of(), Optional.empty(), 0, 0, 0, 0);

    assertTrue(snapshot.plannerBriefing().contains("87% occupied (47 of 54 slots)"));
    assertTrue(snapshot.chatBriefing().contains("adding storage deserves strong consideration"));
  }
}
