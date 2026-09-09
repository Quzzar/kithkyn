package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

class BuildingIdTest {
  @Test
  void multiwordKnownVariantKeepsTheWholeCategoryAndVariant() {
    for (String category : new String[] {"house", "village_center", "couple_cottage"}) {
      BuildingInfo info = new BuildingInfo(category + "_birch_forest_1");
      assertTrue(info.hasWellFormedId());
      assertEquals(category, info.getCategory());
      assertEquals("birch_forest", info.getVariant());
      assertEquals(1, info.getLevel());
      assertNull(info.validate());
    }
  }

  @Test
  void legacyAndCustomSingleTokenVariantsRemainCompatible() {
    BuildingInfo old = new BuildingInfo("hunting_lodge_taiga_1");
    assertEquals("hunting_lodge", old.getCategory());
    assertEquals("taiga", old.getVariant());
    BuildingInfo custom = new BuildingInfo("custom_large_home_copper_1");
    assertEquals("custom_large_home", custom.getCategory());
    assertEquals("copper", custom.getVariant());
    assertNull(custom.validate());
  }

  @Test
  void explicitBirchMetadataAndUpgradeChainValidate() {
    BuildingInfo info = decode("""
        {"structure":"house_birch_forest_2", "category":"house", "variant":"birch_forest",
         "upgrades_from":"house_birch_forest_1"}
        """);
    assertNull(info.validate());
    assertEquals(2, info.getLevel());
    assertEquals("house_birch_forest_1", info.getUpgradesFrom());
    assertEquals("house", info.displayLabel());
  }

  @Test
  void contradictingMetadataAndMissingUpgradeStillFail() {
    assertNotNull(decode("""
        {"structure":"house_birch_forest_1", "category":"house_birch"}
        """).validate());
    assertNotNull(decode("""
        {"structure":"house_birch_forest_1", "variant":"forest"}
        """).validate());
    assertNotNull(new BuildingInfo("house_birch_forest_2").validate());
  }

  @Test
  void incompleteOrNonnumericIdsStayMalformed() {
    for (String id : new String[] {"house", "house_1", "house_birch_forest_", "house_plains_large"}) {
      assertFalse(new BuildingInfo(id).hasWellFormedId(), id);
      assertNotNull(new BuildingInfo(id).validate(), id);
    }
  }

  private static BuildingInfo decode(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
