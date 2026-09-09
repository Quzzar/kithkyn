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
  void separatesAdultHomelessnessFromPreAdultsWithoutFamilyHomes() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Emberhollow", "hamlet", 11, 6, 2,
        4, 0, 4, 0,
        6, 0, 0, 1,
        Map.of("fishery", 3, "house", 1), Map.of("fisher", 2),
        PopulationOutlook.CAN_GROW,
        new VillageContextSnapshot.RecruitmentStatus(61.0D, 50.0D, 5.5D, 8.0D,
            0.0D, 0.0D, 0.0D, 0.0D),
        false, false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty());

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
        Map.of("house", 1), Map.of(), PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(45.0D, 50.0D, 4.0D, 8.0D,
            0.0D, 0.0D, 0.0D, 0.0D),
        false, false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty());

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
        Map.of("lumberjack", 4, "storehouse", 1), Map.of("lumberjack", 4),
        PopulationOutlook.HELD_AT_FLOOR,
        new VillageContextSnapshot.RecruitmentStatus(20.0D, 50.0D, 0.0D, 8.0D,
            0.0D, 0.0D, -4.0D, 0.0D),
        true, false, false, false, List.of(),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty());

    assertTrue(snapshot.plannerBriefing().contains("Shared storage is full"));
    assertTrue(snapshot.plannerBriefing().contains("More shared storage is urgent"));
    assertTrue(snapshot.chatBriefing().contains("Shared storage: full"));
  }

  @Test
  void connectsARecentlyBuiltVacantWorkplaceToRecruitmentConstraints() {
    VillageContextSnapshot snapshot = new VillageContextSnapshot(
        "Meadowmere", "hamlet", 4, 4, 0,
        0, 0, 0, 0,
        4, 0, 1, 0,
        Map.of("lumberjack", 1), Map.of("lumberjack", 1),
        PopulationOutlook.HOLDING,
        new VillageContextSnapshot.RecruitmentStatus(41.0D, 50.0D, 2.5D, 8.0D,
            0.0D, 0.0D, -3.0D, 0.0D),
        false, false, true, false,
        List.of(new VillageContextSnapshot.WorkplaceStatus(
            "lumberjack", 0L, Map.of(), Map.of("lumberjack", 1), 1)),
        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty());

    String planner = snapshot.plannerBriefing();
    assertTrue(planner.contains("No newcomers are currently arriving"));
    assertTrue(planner.contains("Attractiveness is 41.0/100, below the newcomer threshold of 50.0"));
    assertTrue(planner.contains("Food is 2.5 items per person against a target of 8.0"));
    assertTrue(planner.contains("lumberjack completed today"));
    assertTrue(planner.contains("lumberjack: 0 staffed, 1 open"));
    assertTrue(planner.contains("1 free live-in bed"));
    assertTrue(planner.contains("A staffing decision is currently in progress"));
  }
}
