package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Catalog completeness includes every requested starter, even repeated copies of one design. */
class FoundingSetTest {
  @AfterEach
  void clearRegistry() { Buildings.reload(Map.of()); }

  private BuildingInfo center(String starters) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
        "{\"structure\":\"village_center_birch_forest_1\",\"starting_buildings\":" + starters + "}"))
        .getOrThrow();
  }

  private Map<String, BuildingInfo> definitions(BuildingInfo center) {
    Map<String, BuildingInfo> definitions = new HashMap<>();
    definitions.put(center.getName(), center);
    for (String id : List.of("mine_birch_forest_1", "storehouse_birch_forest_1", "house_birch_forest_1")) {
      definitions.put(id, new BuildingInfo(id));
    }
    return definitions;
  }

  @Test
  void repeatedHomesSurviveCodecAndRemainInAuthoredOrder() {
    BuildingInfo center = center("""
        ["mine_birch_forest_1", "storehouse_birch_forest_1", "house_birch_forest_1",
         "house_birch_forest_1", "house_birch_forest_1", "house_birch_forest_1"]
        """);
    Buildings.reload(definitions(center));
    assertTrue(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
    assertEquals(center.getStartingBuildings(), Buildings.foundingCompanions(center, VillageStyle.BIRCH_FOREST)
        .orElseThrow().stream().map(BuildingInfo::getName).toList());
    var encoded = BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, center).getOrThrow();
    assertEquals(center.getStartingBuildings(), BuildingInfo.CODEC.parse(JsonOps.INSTANCE, encoded)
        .getOrThrow().getStartingBuildings());
  }

  @Test
  void missingLastHomeRefusesEntireFoundingSet() {
    BuildingInfo center = center("""
        ["mine_birch_forest_1", "storehouse_birch_forest_1", "house_birch_forest_1", "house_birch_forest_2"]
        """);
    Buildings.reload(definitions(center));
    assertFalse(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
    assertTrue(Buildings.foundingCompanions(center, VillageStyle.BIRCH_FOREST).isEmpty());
  }

  @Test
  void authoredSetCannotOmitARequiredServiceOrContainASecondCenter() {
    for (String invalid : List.of("[\"mine_birch_forest_1\"]",
        "[\"mine_birch_forest_1\",\"storehouse_birch_forest_1\",\"village_center_birch_forest_1\"]")) {
      BuildingInfo center = center(invalid);
      Buildings.reload(definitions(center));
      assertFalse(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
    }
  }
}
