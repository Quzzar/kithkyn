package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class LocationPreferenceTest {

  @Test
  void raisedDoorstepTemplatesFollowAHigherFrontBank() {
    assertEquals(64, LocationValidator.seatingPlane(62, -1, 64));
    assertEquals(62, LocationValidator.seatingPlane(62, -1, 61));
    assertEquals(62, LocationValidator.seatingPlane(62, 0, 64));
  }

  private static LocationValidator.Fit fit(int gap, int inward, int cost, Rotation rotation) {
    return new LocationValidator.Fit(rotation, BlockPos.ZERO,
        new SitePreparation.SiteCost(cost, 0, 0, false, false, ""),
        new TownLayout.Preference(gap, inward), new TownLayout.Relationship(1, 5), 0, 100, true);
  }

  @Test
  void aTwoBlockLaneWinsBeforeCostButOneBlockIsStillAFit() {
    var tight = fit(1, 1, 0, Rotation.NONE);
    var roomy = fit(2, 1, 20, Rotation.NONE);

    assertEquals(roomy, List.of(tight, roomy).stream().min(LocationValidator.FIT_ORDER).orElseThrow());
    assertEquals(tight, List.of(tight).stream().min(LocationValidator.FIT_ORDER).orElseThrow());
  }

  @Test
  void anInwardFrontWinsBeforeCostButTheOnlyAvailableTurnRemainsAFit() {
    var turned = fit(2, 0, 0, Rotation.CLOCKWISE_90);
    var inward = fit(2, 1, 20, Rotation.NONE);

    assertEquals(inward, List.of(turned, inward).stream().min(LocationValidator.FIT_ORDER).orElseThrow());
    assertEquals(turned, List.of(turned).stream().min(LocationValidator.FIT_ORDER).orElseThrow());
  }

  @Test
  void preferredCandidatesCannotSpendTheWholeScanBudgetBeforeFallbacksAreTried() {
    List<LocationValidator.PlannedCandidate> candidates = new ArrayList<>();
    for (TownLayout.Preference preference : TownLayout.PREFERENCES) {
      for (int distance = 200; distance >= 1; distance--) {
        candidates.add(new LocationValidator.PlannedCandidate(new TownLayout.Origin(distance, 0), Rotation.NONE,
            preference, new TownLayout.Relationship(1, 5), distance * distance));
      }
    }

    var selected = LocationValidator.plannedCandidatesWithinBudget(candidates, LocationValidator.MAX_PLANNED_CANDIDATES);

    assertEquals(160, selected.size());
    for (TownLayout.Preference preference : TownLayout.PREFERENCES) {
      var tier = selected.stream().filter(candidate -> candidate.preference().equals(preference)).toList();
      assertEquals(40, tier.size());
      assertEquals(1, tier.getFirst().distanceSqr());
      assertEquals(1600, tier.getLast().distanceSqr());
    }
    assertEquals(new TownLayout.Preference(2, 1), selected.getFirst().preference());
    assertEquals(new TownLayout.Preference(1, 0), selected.getLast().preference());
  }

  @Test
  void missingFallbackTiersLeaveTheirBudgetAvailableToActualCandidates() {
    List<LocationValidator.PlannedCandidate> candidates = new ArrayList<>();
    for (int distance = 1; distance <= 200; distance++) {
      candidates.add(new LocationValidator.PlannedCandidate(new TownLayout.Origin(distance, 0), Rotation.NONE,
          new TownLayout.Preference(2, 1), new TownLayout.Relationship(1, 5), distance * distance));
    }

    assertEquals(160,
        LocationValidator.plannedCandidatesWithinBudget(candidates, LocationValidator.MAX_PLANNED_CANDIDATES).size());
  }
}
