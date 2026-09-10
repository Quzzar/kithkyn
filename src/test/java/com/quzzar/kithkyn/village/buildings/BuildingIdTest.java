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
    for (String id : new String[] {"house", "house_1", "house_birch_forest_", "house_plains_large",
        "house_plains_1__", "house_plains_1__large__house", "house_plains_1__large_"}) {
      assertFalse(new BuildingInfo(id).hasWellFormedId(), id);
      assertNotNull(new BuildingInfo(id).validate(), id);
    }
  }

  @Test
  void namedLayoutsPreserveTheCategoryStyleAndTierAcrossCodecRoundTrips() {
    BuildingInfo info = decode("""
        {"structure":"house_birch_forest_2__large_house_2", "category":"house", "variant":"birch_forest",
         "standalone":true}
        """);
    assertNull(info.validate());
    assertEquals("house", info.getCategory());
    assertEquals("birch_forest", info.getVariant());
    assertEquals(2, info.getLevel());
    assertEquals("large_house_2", info.getDesign());
    assertEquals("house (large house 2)", info.displayLabel());
    assertTrue(info.isStandalone());
    assertNull(info.getUpgradesFrom());
    BuildingInfo restored = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, info).getOrThrow()).getOrThrow();
    assertEquals(info.getDesign(), restored.getDesign());
    assertTrue(restored.isStandalone());
    assertNull(restored.validate());
  }

  @Test
  void standaloneIsExplicitAndCannotContradictAnUpgradePath() {
    assertNotNull(decode("""
        {"structure":"house_plains_2__courtyard"}
        """).validate());
    assertNotNull(decode("""
        {"structure":"house_plains_2", "standalone":true, "upgrades_from":"house_plains_1"}
        """).validate());
    BuildingInfo canonical = decode("""
        {"structure":"house_plains_2", "standalone":true}
        """);
    assertNull(canonical.validate());
    assertNull(canonical.getDesign());
  }

  private static BuildingInfo decode(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
