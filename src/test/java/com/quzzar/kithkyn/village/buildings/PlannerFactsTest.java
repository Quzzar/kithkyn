package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.Occupation;

class PlannerFactsTest {

  @Test
  void namesWhatAlreadyStandsAndWhichTradesAreStillOpen() {
    Map<String, Integer> buildings = new LinkedHashMap<>();
    buildings.put("farm", 1);
    buildings.put("fishery", 2);
    Map<String, Integer> posts = new LinkedHashMap<>();
    posts.put("farmer", 1);
    posts.put("fisher", 2);

    assertEquals("Already standing: farm x1, fishery x2. ",
        PlannerFacts.existingBuildings(buildings));
    assertEquals("Open work: farmer x1, fisher x2. ", PlannerFacts.openPosts(posts));
    assertEquals("Open wall guard posts: gate crossbow x2. ",
        PlannerFacts.openWallPosts(Map.of("gate crossbow", 2)));
  }

  @Test
  void explainsWhyHomelessResidentsCannotFillAnotherBedlessWorkshop() {
    assertEquals("2 adult residents are already unhoused. They cannot take open work unless that workplace "
            + "has a free live-in bed; another workshop without beds would not house them, while "
            + "a house would add general beds. ",
        PlannerFacts.housingConstraint(2, 0, 3));
  }

  @Test
  void fullBedsDoNotClaimThatPopulationGrowthIsImpossible() {
    assertEquals("No general bed is free. A newcomer could only wait unhoused at the campfire if "
            + "population limits allow; a workshop without beds would not add housing, while a "
            + "house would. ",
        PlannerFacts.housingConstraint(0, 0, 0));
  }

  @Test
  void vacantExistingProductionMakesAnotherFreshCopyRedundant() {
    Set<Occupation> open = Set.of(Occupation.LUMBERJACK);

    assertTrue(WorkplaceDemand.duplicatesVacantProduction(
        Set.of(Occupation.LUMBERJACK), List.of("LOGS", "PLANKS"), open,
        capability -> Set.of("LOGS", "PLANKS").contains(capability)));
    assertFalse(WorkplaceDemand.duplicatesVacantProduction(
        Set.of(Occupation.FARMER), List.of("FOOD", "CROPS"), open,
        capability -> Set.of("LOGS", "PLANKS").contains(capability)));
    assertFalse(WorkplaceDemand.duplicatesVacantProduction(
        Set.of(Occupation.LUMBERJACK), List.of("LOGS", "CHARCOAL"), open,
        capability -> Set.of("LOGS", "PLANKS").contains(capability)));
    assertFalse(WorkplaceDemand.duplicatesVacantProduction(
        Set.of(), List.of("STORAGE"), open, capability -> true));
    assertFalse(WorkplaceDemand.duplicatesVacantProduction(
        Set.of(Occupation.QUARTERMASTER), List.of("STORAGE"),
        Set.of(Occupation.QUARTERMASTER), capability -> true));
  }

  @Test
  void storedWheatExplainsTheMissingBakeryLinkInsteadOfCallingForAnotherField() {
    String fact = UrbanPlanner.wheatFoodFact(192, false, false);

    assertTrue(fact.contains("192 wheat"));
    assertTrue(fact.contains("not edible food"));
    assertTrue(fact.contains("bakery"));
    assertTrue(fact.contains("another wheat field will not solve hunger"));
  }
}
