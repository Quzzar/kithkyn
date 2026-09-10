package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BirchCatalogResolutionTest {
  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void intentionalBirchOmissionsNeverBorrowPlainsBuildings() {
    Map<String, BuildingInfo> definitions = new HashMap<>();
    for (String id : new String[] {"house_plains_3", "mine_plains_2", "storehouse_plains_2",
        "farm_plains_3", "church_plains_2", "village_center_plains_2"}) {
      definitions.put(id, new BuildingInfo(id));
    }
    Buildings.reload(definitions);
    for (BuildingInfo info : definitions.values()) {
      assertNull(Buildings.resolve(info.getCategory(), info.getLevel(), VillageStyle.BIRCH_FOREST));
      assertSame(info, Buildings.resolve(info.getCategory(), info.getLevel(), VillageStyle.TAIGA));
    }
    assertTrue(Buildings.catalogue(VillageStyle.BIRCH_FOREST).isEmpty());
  }

  @Test
  void ownDefinitionsResolveAndRequireTheCompleteFoundingTrioForAutomaticSelection() {
    Map<String, BuildingInfo> definitions = new HashMap<>();
    for (String category : new String[] {"village_center", "mine", "storehouse", "house", "tavern"}) {
      BuildingInfo info = new BuildingInfo(category + "_birch_forest_1");
      definitions.put(info.getName(), info);
    }
    Buildings.reload(definitions);
    assertTrue(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
    assertFalse(Buildings.hasFoundingSet(VillageStyle.PLAINS));
    assertSame(definitions.get("house_birch_forest_1"),
        Buildings.resolve("house", 1, VillageStyle.BIRCH_FOREST));
    assertSame(definitions.get("tavern_birch_forest_1"),
        Buildings.resolve("tavern", 1, VillageStyle.BIRCH_FOREST));
    definitions.remove("mine_birch_forest_1");
    Buildings.reload(definitions);
    assertFalse(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
  }
}
