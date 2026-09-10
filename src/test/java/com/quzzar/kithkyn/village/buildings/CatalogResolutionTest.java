package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Every style is a strict catalog: a village raises only what its own family authored. */
class CatalogResolutionTest {
  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void noStyleBorrowsAnotherFamilysBuildings() {
    // Roles and tiers that only one family authored must stay invisible to the other two.
    Map<String, BuildingInfo> definitions = new HashMap<>();
    for (String id : new String[] {"house_desert_3", "mine_desert_2", "village_center_desert_2",
        "storehouse_badlands_2", "farm_badlands_3", "church_birch_forest_2"}) {
      definitions.put(id, new BuildingInfo(id));
    }
    Buildings.reload(definitions);
    for (BuildingInfo info : definitions.values()) {
      VillageStyle owner = VillageStyle.parse(info.getVariant());
      for (VillageStyle style : VillageStyle.values()) {
        if (style == owner) {
          assertSame(info, Buildings.resolve(info.getCategory(), info.getLevel(), style));
          assertTrue(Buildings.isRegionalChoice(info, style));
        } else {
          assertNull(Buildings.resolve(info.getCategory(), info.getLevel(), style), info.getName());
          assertTrue(Buildings.alternatives(info.getCategory(), info.getLevel(), style).isEmpty());
          assertFalse(Buildings.isRegionalChoice(info, style));
        }
      }
    }
    assertEquals(List.of("church_birch_forest_2"), names(Buildings.catalogue(VillageStyle.BIRCH_FOREST)));
    assertEquals(List.of("house_desert_3", "mine_desert_2", "village_center_desert_2"),
        names(Buildings.catalogue(VillageStyle.DESERT)));
    assertEquals(List.of("farm_badlands_3", "storehouse_badlands_2"), names(Buildings.catalogue(VillageStyle.BADLANDS)));
  }

  @Test
  void alternativesAreOneFamilysLayoutsCanonicalFirst() {
    BuildingInfo canonical = new BuildingInfo("house_desert_1");
    BuildingInfo alternative = new BuildingInfo("house_desert_1__courtyard");
    BuildingInfo birch = new BuildingInfo("house_birch_forest_1__timber");
    Buildings.reload(Map.of(canonical.getName(), canonical, alternative.getName(), alternative,
        birch.getName(), birch));
    assertEquals(List.of(canonical, alternative), Buildings.alternatives("house", 1, VillageStyle.DESERT));
    assertSame(canonical, Buildings.resolve("house", 1, VillageStyle.DESERT));
    // With no canonical design authored, the first named alternative is the family's answer.
    assertEquals(List.of(birch), Buildings.alternatives("house", 1, VillageStyle.BIRCH_FOREST));
    assertSame(birch, Buildings.resolve("house", 1, VillageStyle.BIRCH_FOREST));
    assertTrue(Buildings.alternatives("house", 1, VillageStyle.BADLANDS).isEmpty());
    assertFalse(Buildings.isRegionalChoice(alternative, VillageStyle.BIRCH_FOREST));
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
    assertFalse(Buildings.hasFoundingSet(VillageStyle.DESERT));
    assertSame(definitions.get("house_birch_forest_1"),
        Buildings.resolve("house", 1, VillageStyle.BIRCH_FOREST));
    assertSame(definitions.get("tavern_birch_forest_1"),
        Buildings.resolve("tavern", 1, VillageStyle.BIRCH_FOREST));
    definitions.remove("mine_birch_forest_1");
    Buildings.reload(definitions);
    assertFalse(Buildings.hasFoundingSet(VillageStyle.BIRCH_FOREST));
  }

  private static List<String> names(List<BuildingInfo> catalogue) {
    return catalogue.stream().map(BuildingInfo::getName).toList();
  }
}
